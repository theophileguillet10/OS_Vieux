package com.elderlyos.vieuxos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            var flashVisible by remember { mutableStateOf(false) }
            var isRecording by remember { mutableStateOf(false) }
            var dotVisible by remember { mutableStateOf(true) }

            // Blinking dot effect while recording
            LaunchedEffect(isRecording) {
                while (isRecording) {
                    dotVisible = !dotVisible
                    kotlinx.coroutines.delay(500)
                }
                dotVisible = true
            }

            Box(modifier = Modifier.fillMaxSize()) {

                // Camera preview — tap to take photo (only when not recording)
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

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                this@CameraActivity,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture,
                                videoCapture
                            )
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
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (dotVisible) {
                            Canvas(modifier = Modifier.size(20.dp)) {
                                drawCircle(color = Color.Red)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = "RECORDING",
                            color = Color.Red,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Bottom section
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    // Hint text
                    if (!isRecording) {
                        Text(
                            text = "Tap anywhere to take a photo",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 8.dp)
                        )
                    }

                    // Video record button
                    Button(
                        onClick = {
                            if (isRecording) {
                                stopRecording()
                                isRecording = false
                            } else {
                                startRecording()
                                isRecording = true
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
                            text = if (isRecording) "⏹  Stop Recording" else "🎥  Record Video",
                            fontSize = 22.sp,
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
        val photoFile = File(
            getExternalFilesDir(null),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                .format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(this@CameraActivity, "📸 Photo saved!", Toast.LENGTH_SHORT).show()
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
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            recording = videoCapture.output
                .prepareRecording(this, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this)) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        if (!event.hasError()) {
                            Toast.makeText(this, "🎥 Video saved!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startCamera()
        }
    }
}