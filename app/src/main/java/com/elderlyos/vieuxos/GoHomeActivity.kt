package com.elderlyos.vieuxos

import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val ctrl = WindowInsetsControllerCompat(window, window.decorView)
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
        ctrl.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
html,body{width:100%;height:100%;overflow:hidden;background:#d0d8e0}
#map{width:100%;height:100%}
#status{
  position:absolute;top:12px;left:50%;transform:translateX(-50%);
  background:rgba(0,0,0,.72);color:#fff;padding:10px 20px;
  border-radius:24px;font-size:17px;z-index:1000;
  font-family:sans-serif;white-space:nowrap;pointer-events:none;
}
</style>
</head>
<body>
<div id="map"></div>
<div id="status">Finding route…</div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
var map = L.map('map',{zoomControl:true});
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{
  maxZoom:19, attribution:'© OpenStreetMap'
}).addTo(map);

var statusEl = document.getElementById('status');
function setStatus(msg){statusEl.textContent=msg;statusEl.style.display='block';}
function hideStatus(){statusEl.style.display='none';}

// Step 1 — geocode home address via Nominatim
fetch('https://nominatim.openstreetmap.org/search?q='+encodeURIComponent('$esc')+'&format=json&limit=1',
  {headers:{'Accept-Language':'en','User-Agent':'VieuxOS/1.0'}})
.then(function(r){return r.json();})
.then(function(data){
  if(!data||data.length===0){setStatus('Address not found ❌');return;}
  var dLat=parseFloat(data[0].lat), dLon=parseFloat(data[0].lon);

  // Home marker
  var homeIcon=L.divIcon({html:'<div style="font-size:34px;line-height:1">🏠</div>',
    iconSize:[34,34],iconAnchor:[17,34],className:''});
  L.marker([dLat,dLon],{icon:homeIcon}).addTo(map)
   .bindPopup('<b>Home</b><br>$esc');
  map.setView([dLat,dLon],13);
  setStatus('Getting your location…');

  // Step 2 — get device GPS
  navigator.geolocation.getCurrentPosition(function(pos){
    var oLat=pos.coords.latitude, oLon=pos.coords.longitude;

    // Blue dot for current position
    L.circleMarker([oLat,oLon],{
      radius:10,color:'#fff',weight:3,
      fillColor:'#1565C0',fillOpacity:1
    }).addTo(map).bindPopup('You are here');

    setStatus('Calculating route…');

    // Step 3 — fetch route from OSRM
    var osrmUrl='https://router.project-osrm.org/route/v1/$osrmProfile/'
      +oLon+','+oLat+';'+dLon+','+dLat
      +'?overview=full&geometries=geojson';

    fetch(osrmUrl)
    .then(function(r){return r.json();})
    .then(function(json){
      if(json.code!=='Ok'||!json.routes[0]){setStatus('No route found');return;}
      var coords=json.routes[0].geometry.coordinates.map(function(c){return[c[1],c[0]];});
      var line=L.polyline(coords,{color:'#1565C0',weight:6,opacity:.85}).addTo(map);
      map.fitBounds(line.getBounds(),{padding:[50,50]});
      var km=(json.routes[0].distance/1000).toFixed(1);
      var mins=Math.round(json.routes[0].duration/60);
      setStatus(km+' km  ·  ~'+mins+' min');
      setTimeout(hideStatus,6000);
    })
    .catch(function(){setStatus('Route unavailable');});

  },function(){
    // GPS denied — show destination only
    setStatus('Enable location for routing');
    setTimeout(hideStatus,4000);
  },{enableHighAccuracy:true,timeout:12000});
})
.catch(function(){setStatus('Network error');});
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
