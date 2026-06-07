package com.elderlyos.vieuxos

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraActivity : ComponentActivity() {

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, permissions, 100)
        }
    }

    private fun startCamera() {
        setContent {
            var flashVisible   by remember { mutableStateOf(false) }
            var isRecording    by remember { mutableStateOf(false) }
            var dotVisible     by remember { mutableStateOf(true) }
            var useFrontCamera by remember { mutableStateOf(false) }
            var zoomLevel      by remember { mutableStateOf(0f) }  // 0f = wide, 1f = max
            var torchEnabled   by remember { mutableStateOf(false) }

            // Blinking dot while recording
            LaunchedEffect(isRecording) {
                while (isRecording) {
                    dotVisible = !dotVisible
                    kotlinx.coroutines.delay(500)
                }
                dotVisible = true
            }

            Box(modifier = Modifier.fillMaxSize()) {

                // Camera preview — rebuilds when lens changes
                key(useFrontCamera) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                imageCapture = ImageCapture.Builder().build()
                                val recorder = Recorder.Builder()
                                    .setQualitySelector(QualitySelector.from(Quality.HD))
                                    .build()
                                videoCapture = VideoCapture.withOutput(recorder)

                                val selector = if (useFrontCamera)
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                else
                                    CameraSelector.DEFAULT_BACK_CAMERA

                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(
                                    this@CameraActivity,
                                    selector,
                                    preview,
                                    imageCapture,
                                    videoCapture
                                )
                                // Restore zoom after lens switch
                                camera?.cameraControl?.setLinearZoom(zoomLevel)
                                // Front camera has no torch — reset state
                                if (useFrontCamera) {
                                    torchEnabled = false
                                    camera?.cameraControl?.enableTorch(false)
                                }

                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (!isRecording) {
                                    takePhoto()
                                    flashVisible = true
                                }
                            }
                    )
                }

                // White flash feedback
                if (flashVisible) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(200)
                        flashVisible = false
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = Color.White.copy(alpha = 0.7f))
                    }
                }

                // RECORDING indicator
                if (isRecording) {
                    Row(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dotVisible) {
                            Canvas(modifier = Modifier.size(20.dp)) { drawCircle(color = Color.Red) }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text("RECORDING", color = Color.Red, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                // ── Top-right: flip + flash buttons ───────────────────────────
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Flip camera
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { useFrontCamera = !useFrontCamera },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Cameraswitch,
                            contentDescription = "Flip camera",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    // Flash / torch (back camera only)
                    if (!useFrontCamera) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    if (torchEnabled) Color(0xFFFFC107).copy(alpha = 0.9f)
                                    else Color.Black.copy(alpha = 0.5f)
                                )
                                .clickable {
                                    torchEnabled = !torchEnabled
                                    camera?.cameraControl?.enableTorch(torchEnabled)
                                    imageCapture?.flashMode = if (torchEnabled)
                                        ImageCapture.FLASH_MODE_ON
                                    else
                                        ImageCapture.FLASH_MODE_OFF
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = "Flash",
                                tint = if (torchEnabled) Color.Black else Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // ── Right side: zoom controls ─────────────────────────────────
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Zoom in
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                zoomLevel = (zoomLevel + 0.1f).coerceAtMost(1f)
                                camera?.cameraControl?.setLinearZoom(zoomLevel)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    // Zoom level indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${"%.1f".format(1f + zoomLevel * 9f)}×",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Zoom out
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                zoomLevel = (zoomLevel - 0.1f).coerceAtLeast(0f)
                                camera?.cameraControl?.setLinearZoom(zoomLevel)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Remove, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // ── Bottom: hint + record button + nav ────────────────────────
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    if (!isRecording) {
                        Text(
                            text = "Tap screen to take a photo",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (isRecording) {
                                stopRecording(); isRecording = false
                            } else {
                                startRecording(); isRecording = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(70.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFF8B0000) else Color(0xFFB71C1C)
                        )
                    ) {
                        Text(
                            text = if (isRecording) "Stop Recording" else "Record Video",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    BottomNavBar()
                }
            }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val fileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
            .format(System.currentTimeMillis()) + ".jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VieuxOS")
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        ).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(this@CameraActivity, "Photo saved!", Toast.LENGTH_SHORT).show()
                }
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity, "Failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun startRecording() {
        val videoCapture = videoCapture ?: return
        val videoFile = File(
            getExternalFilesDir(null),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                .format(System.currentTimeMillis()) + ".mp4"
        )
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            recording = videoCapture.output
                .prepareRecording(this, FileOutputOptions.Builder(videoFile).build())
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this)) { event ->
                    if (event is VideoRecordEvent.Finalize && !event.hasError())
                        Toast.makeText(this, "Video saved!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED })
            startCamera()
    }
}
