package com.elderlyos.vieuxos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
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

class GoHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoHomeScreen()
        }
    }
}

@Composable
fun GoHomeScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("vieuxos_prefs", Context.MODE_PRIVATE)
    var homeAddress by remember { mutableStateOf(prefs.getString("home_address", "") ?: "") }
    var showSettings by remember { mutableStateOf(false) }
    var tempAddress by remember { mutableStateOf(homeAddress) }

    fun openGoogleMaps(mode: String) {
        val uri = Uri.parse("google.navigation:q=${Uri.encode(homeAddress)}&mode=$mode")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        context.startActivity(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E7D32))
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Take Me Home",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (showSettings) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Enter Home Address",
                    color = Color(0xFF1A1A1A),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = tempAddress,
                    onValueChange = { tempAddress = it },
                    label = { Text("Home Address") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1A1A1A),
                        unfocusedTextColor = Color(0xFF1A1A1A),
                        focusedBorderColor = Color(0xFF2E7D32),
                        unfocusedBorderColor = Color(0xFFAAAAAA)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save", fontSize = 22.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { showSettings = false }) {
                    Text("Cancel", color = Color(0xFF666666), fontSize = 18.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (homeAddress.isNotEmpty()) {
                        Text(
                            text = homeAddress,
                            color = Color(0xFF444444),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "No address saved yet",
                            color = Color(0xFFCC0000),
                            fontSize = 16.sp
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (homeAddress.isNotEmpty()) {
                        BigButton("By Car", Color(0xFF1565C0), Icons.Filled.DirectionsCar) { openGoogleMaps("d") }
                        BigButton("On Foot", Color(0xFF2E7D32), Icons.Filled.DirectionsWalk) { openGoogleMaps("w") }
                    }
                    BigButton("Set Home Address", Color(0xFF757575), Icons.Filled.Settings) { showSettings = true }
                }
            }
        }

        BottomNavBar()
    }
}
