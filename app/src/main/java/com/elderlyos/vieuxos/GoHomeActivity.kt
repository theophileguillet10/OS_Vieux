package com.elderlyos.vieuxos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private val HEADER_COLOR = Color(0xFF00695C)
private val ACCENT_COLOR  = Color(0xFF004D40)

class GoHomeActivity : ComponentActivity() {

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* WebView reloads automatically via onGeolocationPermissionsShowPrompt */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val ctrl = WindowInsetsControllerCompat(window, window.decorView)
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
        ctrl.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        setContent { GoHomeScreen() }
    }
}

// ── Build an OpenStreetMap / Leaflet page with OSRM routing ──────────────────
private fun buildMapHtml(address: String, vehicleMode: String): String {
    // Safely escape the address for embedding in a JS single-quoted string
    val esc = address
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\r", "")
        .replace("\n", " ")

    // OSRM profile: "driving" for car, "foot" for walking
    val osrmProfile = if (vehicleMode == "d") "driving" else "foot"

    return """<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=4.0,user-scalable=yes">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<style>
*{margin:0;padding:0;box-sizing:border-box}
html,body{width:100%;height:100%;font-family:sans-serif;background:#fff}
#map{position:absolute;top:0;left:0;right:0;background:#d0d8e0}
#status{
  position:absolute;top:12px;left:50%;transform:translateX(-50%);
  background:rgba(0,0,0,.72);color:#fff;padding:10px 20px;
  border-radius:24px;font-size:17px;z-index:1000;
  white-space:nowrap;pointer-events:none;
}
#steps-panel{
  position:absolute;left:0;right:0;bottom:0;
  overflow-y:auto;background:#fff;
  border-top:3px solid #1565C0;
}
#steps-header{
  background:#1565C0;color:#fff;
  padding:10px 16px;font-size:16px;font-weight:bold;
  position:sticky;top:0;z-index:10;
}
.step{
  display:flex;align-items:flex-start;
  padding:12px 16px;border-bottom:1px solid #e0e0e0;gap:12px;
}
.step-icon{font-size:26px;min-width:34px;text-align:center;margin-top:2px}
.step-info{flex:1}
.step-name{font-size:16px;color:#212121;font-weight:500;line-height:1.35}
.step-dist{font-size:14px;color:#757575;margin-top:3px}
</style>
</head>
<body>
<div id="map"></div>
<div id="status">Finding route…</div>
<div id="steps-panel">
  <div id="steps-header">📋 Instructions</div>
  <div id="steps-list"><div class="step"><div class="step-name" style="color:#999">Waiting for route…</div></div></div>
</div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
// ── Layout: map 55%, steps panel 45% ──────────────────────────────────────
function applyLayout(){
  var h = window.innerHeight;
  var mapH = Math.floor(h * 0.55);
  var panelH = h - mapH;
  document.getElementById('map').style.height = mapH + 'px';
  document.getElementById('steps-panel').style.height = panelH + 'px';
  document.getElementById('steps-panel').style.top = mapH + 'px';
  if(window._leafletMap) window._leafletMap.invalidateSize();
}
applyLayout();
window.addEventListener('resize', applyLayout);

var map = L.map('map',{zoomControl:true});
window._leafletMap = map;
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{
  maxZoom:19, attribution:'© OpenStreetMap'
}).addTo(map);

var statusEl = document.getElementById('status');
function setStatus(msg){statusEl.textContent=msg;statusEl.style.display='block';}
function hideStatus(){statusEl.style.display='none';}

var TURN_ICON = {
  'turn-right':'↱','turn-left':'↰',
  'turn-sharp-right':'↪','turn-sharp-left':'↩',
  'turn-slight-right':'↗','turn-slight-left':'↖',
  'uturn':'⟲','roundabout':'🔄','rotary':'🔄',
  'continue':'⬆','merge':'⬆','fork':'↗',
  'arrive':'🏠','depart':'📍','notification':'ℹ'
};
function stepIcon(m){
  if(!m) return '⬆';
  if(m.type==='arrive') return '🏠';
  if(m.type==='depart') return '📍';
  var k = m.type + (m.modifier?'-'+m.modifier:'');
  return TURN_ICON[k] || TURN_ICON[m.type] || '⬆';
}
function fmtDist(m){
  return m>=1000?(m/1000).toFixed(1)+' km':Math.round(m)+' m';
}
function capitalize(s){ return s ? s.charAt(0).toUpperCase()+s.slice(1) : ''; }

// Step 1 — geocode home address
fetch('https://nominatim.openstreetmap.org/search?q='+encodeURIComponent('$esc')+'&format=json&limit=1',
  {headers:{'Accept-Language':'fr','User-Agent':'VieuxOS/1.0'}})
.then(function(r){return r.json();})
.then(function(data){
  if(!data||data.length===0){setStatus('Adresse introuvable ❌');return;}
  var dLat=parseFloat(data[0].lat), dLon=parseFloat(data[0].lon);

  var homeIcon=L.divIcon({html:'<div style="font-size:34px;line-height:1">🏠</div>',
    iconSize:[34,34],iconAnchor:[17,34],className:''});
  L.marker([dLat,dLon],{icon:homeIcon}).addTo(map).bindPopup('<b>Domicile</b><br>$esc');
  map.setView([dLat,dLon],13);
  setStatus('Localisation en cours…');

  navigator.geolocation.getCurrentPosition(function(pos){
    var oLat=pos.coords.latitude, oLon=pos.coords.longitude;

    L.circleMarker([oLat,oLon],{
      radius:10,color:'#fff',weight:3,fillColor:'#1565C0',fillOpacity:1
    }).addTo(map).bindPopup('Vous êtes ici');

    setStatus('Calcul de l\'itinéraire…');

    var url='https://router.project-osrm.org/route/v1/$osrmProfile/'
      +oLon+','+oLat+';'+dLon+','+dLat
      +'?overview=full&geometries=geojson&steps=true';

    fetch(url)
    .then(function(r){return r.json();})
    .then(function(json){
      if(json.code!=='Ok'||!json.routes||!json.routes[0]){
        setStatus('Itinéraire introuvable');
        return;
      }
      var route=json.routes[0];

      // Draw route line
      var coords=route.geometry.coordinates.map(function(c){return[c[1],c[0]];});
      var line=L.polyline(coords,{color:'#1565C0',weight:6,opacity:.85}).addTo(map);
      map.fitBounds(line.getBounds(),{padding:[40,40]});

      var km=(route.distance/1000).toFixed(1);
      var mins=Math.round(route.duration/60);
      setStatus(km+' km  ·  ~'+mins+' min');
      setTimeout(hideStatus,6000);

      // Collect steps from all legs
      var steps=[];
      (route.legs||[]).forEach(function(leg){
        (leg.steps||[]).forEach(function(s){ steps.push(s); });
      });

      if(steps.length===0){
        document.getElementById('steps-list').innerHTML=
          '<div class="step"><div class="step-name" style="color:#999">Aucune instruction disponible</div></div>';
        return;
      }

      var html='';
      steps.forEach(function(s){
        var roadName = (s.name && s.name.trim()) ? '<b>'+s.name+'</b>' : '';
        var type = (s.maneuver && s.maneuver.type) ? s.maneuver.type.replace(/-/g,' ') : '';
        var mod  = (s.maneuver && s.maneuver.modifier) ? s.maneuver.modifier.replace(/-/g,' ') : '';
        var instr = capitalize(mod ? type+' '+mod : type);
        var label = roadName ? (instr ? instr+' — '+roadName : roadName) : instr;
        html += '<div class="step">'
          +'<div class="step-icon">'+stepIcon(s.maneuver)+'</div>'
          +'<div class="step-info">'
          +'<div class="step-name">'+label+'</div>'
          +'<div class="step-dist">'+fmtDist(s.distance)+'</div>'
          +'</div></div>';
      });
      document.getElementById('steps-list').innerHTML = html;
    })
    .catch(function(e){setStatus('Erreur réseau');});

  },function(){
    setStatus('Activez la localisation');
    setTimeout(hideStatus,4000);
  },{enableHighAccuracy:true,timeout:15000});
})
.catch(function(){setStatus('Erreur réseau');});
</script>
</body>
</html>"""
}

@Composable
fun GoHomeScreen() {
    val context     = LocalContext.current
    val prefs       = context.getSharedPreferences("vieuxos_prefs", Context.MODE_PRIVATE)
    val homeAddress = remember { prefs.getString("home_address", "") ?: "" }

    var travelMode by remember { mutableStateOf("d") }
    var modeChosen by remember { mutableStateOf(false) }
    var webView    by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HEADER_COLOR)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Home, null, tint = Color.White, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Take Me Home",
                        color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold
                    )
                    if (homeAddress.isNotEmpty()) {
                        Text(
                            homeAddress,
                            color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp
                        )
                    }
                }
            }
        }

        if (homeAddress.isEmpty()) {
            // ── No address configured ─────────────────────────────────────────
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Filled.LocationOff, null, tint = Color(0xFFCC0000), modifier = Modifier.size(72.dp))
                    Text(
                        "No home address configured.",
                        color = Color(0xFF222222), fontSize = 22.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                    Text(
                        "Ask a family member to enter the home address\nin the app settings (long-press SOS).",
                        color = Color(0xFF666666), fontSize = 18.sp,
                        textAlign = TextAlign.Center, lineHeight = 26.sp
                    )
                }
            }
        } else {
            // ── Mode selector — disappears once a mode is chosen ──────────────
            if (!modeChosen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ACCENT_COLOR)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeButton(
                        label = "By Car", icon = Icons.Filled.DirectionsCar,
                        selected = travelMode == "d", modifier = Modifier.weight(1f)
                    ) {
                        travelMode = "d"; modeChosen = true
                        webView?.loadDataWithBaseURL(
                            "https://localhost/",
                            buildMapHtml(homeAddress, "d"),
                            "text/html", "UTF-8", null
                        )
                    }
                    ModeButton(
                        label = "On Foot", icon = Icons.Filled.DirectionsWalk,
                        selected = travelMode == "w", modifier = Modifier.weight(1f)
                    ) {
                        travelMode = "w"; modeChosen = true
                        webView?.loadDataWithBaseURL(
                            "https://localhost/",
                            buildMapHtml(homeAddress, "w"),
                            "text/html", "UTF-8", null
                        )
                    }
                }
            }

            // ── Embedded OpenStreetMap with Leaflet + OSRM routing ────────────
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        settings.apply {
                            javaScriptEnabled                = true
                            domStorageEnabled                = true
                            useWideViewPort                  = true
                            loadWithOverviewMode             = true
                            setSupportZoom(true)
                            builtInZoomControls              = true
                            displayZoomControls              = false
                            allowContentAccess               = true
                            allowFileAccess                  = true
                            setGeolocationEnabled(true)
                            @Suppress("DEPRECATION")
                            allowUniversalAccessFromFileURLs = true   // needed for fetch() in loadDataWithBaseURL
                            @Suppress("DEPRECATION")
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString =
                                "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/124.0.0.0 Mobile Safari/537.36"
                        }

                        webViewClient = object : WebViewClient() { }

                        webChromeClient = object : WebChromeClient() {
                            // Auto-grant location to the map page
                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String,
                                callback: GeolocationPermissions.Callback
                            ) {
                                callback.invoke(origin, true, false)
                            }
                        }

                        // Load initial map (driving is the default)
                        loadDataWithBaseURL(
                            "https://localhost/",
                            buildMapHtml(homeAddress, travelMode),
                            "text/html", "UTF-8", null
                        )
                        webView = this
                    }
                }
            )
        }

        // ── Nav bar always visible ────────────────────────────────────────────
        BottomNavBar()
    }
}

@Composable
private fun ModeButton(
    label:    String,
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit
) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(56.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF2E7D32) else Color(0xFF546E7A)
        )
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}
