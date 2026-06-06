package com.elderlyos.vieuxos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class FamilySetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilySetupScreen()
        }
    }
}

@Composable
fun FamilySetupScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("vieuxos_prefs", Context.MODE_PRIVATE)

    var pinEntered by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    val correctPin = prefs.getString("setup_pin", "1234") ?: "1234"

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
            if (!pinEntered) {
                // PIN SCREEN
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔒 Family Setup", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Enter PIN to continue", color = Color(0xFFAAAAAA), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("PIN", color = Color(0xFFAAAAAA)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF1565C0),
                            unfocusedBorderColor = Color(0xFF555555)
                        )
                    )

                    if (pinError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("❌ Wrong PIN", color = Color.Red, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (pinInput == correctPin) {
                                pinEntered = true
                                pinError = false
                            } else {
                                pinError = true
                                pinInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(70.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Unlock", fontSize = 22.sp, color = Color.White)
                    }
                }
            } else {
                // SETTINGS SCREEN
                var elderlyName by remember { mutableStateOf(prefs.getString("elderly_name", "") ?: "") }
                var homeAddress by remember { mutableStateOf(prefs.getString("home_address", "") ?: "") }
                var familyPhone by remember { mutableStateOf(prefs.getString("family_phone", "") ?: "") }
                var sosPhone by remember { mutableStateOf(prefs.getString("sos_phone", "") ?: "") }
                var newPin by remember { mutableStateOf("") }
                var saved by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("⚙️ Family Setup", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Configure VieuxOS for your family member", color = Color(0xFFAAAAAA), fontSize = 14.sp)

                    HorizontalDivider(color = Color(0xFF333333))

                    SetupField("👤 Person's Name", elderlyName, "e.g. Georges") { elderlyName = it }
                    SetupField("🏠 Home Address", homeAddress, "e.g. 12 Rue de Paris") { homeAddress = it }
                    SetupField("📞 Family Phone Number", familyPhone, "e.g. +33612345678") { familyPhone = it }
                    SetupField("🆘 SOS Phone Number", sosPhone, "e.g. +33612345678") { sosPhone = it }
                    SetupField("🔒 Change PIN", newPin, "Leave empty to keep current PIN", isPassword = true) { newPin = it }

                    HorizontalDivider(color = Color(0xFF333333))

                    Button(
                        onClick = {
                            prefs.edit()
                                .putString("elderly_name", elderlyName)
                                .putString("home_address", homeAddress)
                                .putString("family_phone", familyPhone)
                                .putString("sos_phone", sosPhone)
                                .apply {
                                    if (newPin.isNotEmpty()) putString("setup_pin", newPin)
                                }
                                .apply()
                            saved = true
                        },
                        modifier = Modifier.fillMaxWidth().height(70.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("💾  Save All Settings", fontSize = 22.sp, color = Color.White)
                    }

                    if (saved) {
                        Text(
                            "✅ Settings saved!",
                            color = Color(0xFF4CAF50),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        BottomNavBar()
    }
}

@Composable
fun SetupField(
    label: String,
    value: String,
    placeholder: String,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF666666)) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF1565C0),
                unfocusedBorderColor = Color(0xFF555555)
            )
        )
    }
}