package com.elderlyos.vieuxos

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class TorchActivity : ComponentActivity() {

    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        // Find the first camera that has a physical flash unit
        cameraId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) { null }

        setContent {
            TorchScreen(
                hasTorch = cameraId != null,
                onTorchChange = { on ->
                    try { cameraId?.let { cameraManager.setTorchMode(it, on) } }
                    catch (_: Exception) { }
                }
            )
        }
    }

    // Always turn the torch off when the user leaves — safety measure
    override fun onPause() {
        super.onPause()
        try { cameraId?.let { cameraManager.setTorchMode(it, false) } } catch (_: Exception) { }
    }
}

@Composable
fun TorchScreen(hasTorch: Boolean, onTorchChange: (Boolean) -> Unit) {
    var isOn by remember { mutableStateOf(false) }

    // Ensure torch is off when the composable is removed from the tree
    DisposableEffect(Unit) {
        onDispose { onTorchChange(false) }
    }

    val bgColor by animateColorAsState(
        targetValue = if (isOn) Color(0xFFFFFDE7) else Color(0xFF1A1A1A),
        animationSpec = tween(250), label = "bg"
    )
    val btnColor by animateColorAsState(
        targetValue = if (isOn) Color(0xFFFFD600) else Color(0xFF424242),
        animationSpec = tween(250), label = "btn"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isOn) Color(0xFF1A1A1A) else Color(0xFFFFD600),
        animationSpec = tween(250), label = "icon"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isOn) Color(0xFF212121) else Color(0xFFEEEEEE),
        animationSpec = tween(250), label = "label"
    )

    Column(
        modifier = Modifier.fillMaxSize().background(bgColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF37474F))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bolt, null, tint = Color(0xFFFFD600), modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Text("Torch", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
        }

        // ── Toggle area ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (!hasTorch) {
                Text(
                    "No flashlight available on this device",
                    color = Color(0xFFAAAAAA),
                    fontSize = 22.sp,
                    modifier = Modifier.padding(32.dp)
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    // Large circular button — easy to press for seniors
                    Button(
                        onClick = {
                            val newState = !isOn
                            isOn = newState
                            onTorchChange(newState)
                        },
                        modifier = Modifier.size(220.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (isOn) 20.dp else 4.dp
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(130.dp)
                        )
                    }

                    Text(
                        text = if (isOn) "ON" else "OFF",
                        color = labelColor,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 8.sp
                    )
                }
            }
        }

        BottomNavBar()
    }
}
