package com.elderlyos.vieuxos

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class GoHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            startApp()
        } else {
            ActivityCompat.requestPermissions(this, permissions, 200)
        }
    }

    private fun startApp() {
        setContent {
            GoHomeScreen()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) startApp()
    }
}

@Composable
fun GoHomeScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("vieuxos_prefs", Context.MODE_PRIVATE)
    var homeAddress by remember { mutableStateOf(prefs.getString("home_address", "") ?: "") }
    var showSettings by remember { mutableStateOf(homeAddress.isEmpty()) }
    var showMap by remember { mutableStateOf(false) }
    var travelMode by remember { mutableStateOf("foot") }
    var tempAddress by remember { mutableStateOf(homeAddress) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                // MAP VIEW
                showMap -> {
                    OSMMapView(
                        homeAddress = homeAddress,
                        travelMode = travelMode
                    )

                    Button(
                        onClick = { showMap = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("← Back", color = Color.White, fontSize = 16.sp)
                    }
                }

                // SETTINGS VIEW
                showSettings -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "⚙️ Set Home Address",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Ask a family member to set this once",
                            color = Color(0xFFAAAAAA),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(
                            value = tempAddress,
                            onValueChange = { tempAddress = it },
                            label = { Text("e.g. 123 Main Street, Paris", color = Color(0xFFAAAAAA)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF1565C0),
                                unfocusedBorderColor = Color(0xFF555555)
                            )
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                prefs.edit().putString("home_address", tempAddress).apply()
                                homeAddress = tempAddress
                                showSettings = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("💾  Save Address", fontSize = 22.sp, color = Color.White)
                        }
                        if (homeAddress.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { showSettings = false }) {
                                Text("Cancel", color = Color(0xFFAAAAAA), fontSize = 18.sp)
                            }
                        }
                    }
                }

                // MAIN VIEW
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("🏠", fontSize = 80.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Take Me Home",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                homeAddress,
                                color = Color(0xFFAAAAAA),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BigButton("🚗  By Car", Color(0xFF1565C0)) {
                                travelMode = "car"
                                showMap = true
                            }
                            BigButton("🚶  On Foot", Color(0xFF2E7D32)) {
                                travelMode = "foot"
                                showMap = true
                            }
                            BigButton("⚙️  Change Address", Color(0xFF424242)) {
                                showSettings = true
                            }
                        }
                    }
                }
            }
        }

        BottomNavBar()
    }
}

@Composable
fun OSMMapView(homeAddress: String, travelMode: String) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            val map = MapView(ctx)
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)
            map.controller.setZoom(15.0)

            // Show user location
            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), map)
            locationOverlay.enableMyLocation()
            locationOverlay.enableFollowLocation()
            map.overlays.add(locationOverlay)

            // Geocode address and add home marker
            Thread {
                try {
                    val geocoder = android.location.Geocoder(ctx)
                    val results = geocoder.getFromLocationName(homeAddress, 1)
                    if (!results.isNullOrEmpty()) {
                        val homePoint = GeoPoint(results[0].latitude, results[0].longitude)
                        (ctx as? android.app.Activity)?.runOnUiThread {
                            val marker = Marker(map)
                            marker.position = homePoint
                            marker.title = "🏠 Home"
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            map.overlays.add(marker)
                            map.controller.animateTo(homePoint)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

            map
        },
        modifier = Modifier.fillMaxSize()
    )
}