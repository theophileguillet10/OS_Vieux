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
    var showSettings by remember { mutableStateOf(homeAddress.isEmpty()) }
    var tempAddress by remember { mutableStateOf(homeAddress) }

    fun openNavigation(mode: String) {
        val uri = Uri.parse("google.navigation:q=${Uri.encode(homeAddress)}&mode=$mode")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        context.startActivity(intent)
    }

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
            if (showSettings) {
                // SETTINGS VIEW
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
            } else {
                // MAIN VIEW
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
                            openNavigation("d")
                        }
                        BigButton("🚶  On Foot", Color(0xFF2E7D32)) {
                            openNavigation("w")
                        }
                        BigButton("⚙️  Change Address", Color(0xFF424242)) {
                            showSettings = true
                        }
                    }
                }
            }
        }

        BottomNavBar()
    }
}