package com.elderlyos.vieuxos

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class InCallActivity : ComponentActivity() {

    companion object {
        private var onStateChanged: ((Int) -> Unit)? = null

        fun updateCallState(state: Int) {
            onStateChanged?.invoke(state)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getStringExtra("action") == "finish") {
            finish()
            return
        }

        val callerName = intent.getStringExtra("caller_name") ?: "Unknown"
        val initialState = intent.getIntExtra("call_state", Call.STATE_CONNECTING)

        setContent {
            InCallScreen(
                callerName = callerName,
                initialState = initialState,
                onRegisterStateCallback = { cb -> onStateChanged = cb },
                onHangUp = {
                    VieuxInCallService.currentCall?.disconnect()
                    finish()
                },
                onFinish = { finish() }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        onStateChanged = null
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Intentionally empty — prevent accidental back press during a call
    }
}

@Composable
fun InCallScreen(
    callerName: String,
    initialState: Int,
    onRegisterStateCallback: ((Int) -> Unit) -> Unit,
    onHangUp: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var callState by remember { mutableStateOf(initialState) }
    var isSpeaker by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var callStarted by remember { mutableStateOf(callState == Call.STATE_ACTIVE) }

    LaunchedEffect(Unit) {
        onRegisterStateCallback { newState ->
            callState = newState
            if (newState == Call.STATE_ACTIVE) callStarted = true
            if (newState == Call.STATE_DISCONNECTED) onFinish()
        }
    }

    LaunchedEffect(callStarted) {
        if (callStarted) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    val formattedTime = remember(elapsedSeconds) {
        val m = elapsedSeconds / 60
        val s = elapsedSeconds % 60
        "%02d:%02d".format(m, s)
    }

    val statusLabel = when (callState) {
        Call.STATE_CONNECTING   -> "Connecting…"
        Call.STATE_DIALING      -> "Calling…"
        Call.STATE_RINGING      -> "Ringing…"
        Call.STATE_ACTIVE       -> formattedTime
        Call.STATE_HOLDING      -> "On hold"
        Call.STATE_DISCONNECTED -> "Call ended"
        else                    -> "…"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val avatarScale = if (callState == Call.STATE_ACTIVE) 1f else pulse

    val initials = callerName.split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    val speakerColor by animateColorAsState(
        targetValue = if (isSpeaker) Color(0xFF1560BD) else Color(0xFF2A2A2A),
        label = "speakerColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.6f))

        // ── Avatar ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(avatarScale)
                .clip(CircleShape)
                .background(Color(0xFF1B3A1B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.ifEmpty { "?" },
                color = Color(0xFF4CAF50),
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Caller name ──────────────────────────────────────────────────────
        Text(
            text = callerName,
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ── Status / Timer ───────────────────────────────────────────────────
        Text(
            text = statusLabel,
            color = Color(0xFF888888),
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── Speaker button ───────────────────────────────────────────────────
        Button(
            onClick = {
                isSpeaker = !isSpeaker
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.isSpeakerphoneOn = isSpeaker
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(88.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = speakerColor)
        ) {
            Text(
                text = if (isSpeaker) "🔊  Speaker ON" else "🔊  Speaker",
                fontSize = 26.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Hang up button ───────────────────────────────────────────────────
        Button(
            onClick = onHangUp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(96.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
        ) {
            Text(
                text = "📵  Hang Up",
                fontSize = 28.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── NavBar ───────────────────────────────────────────────────────────
        BottomNavBar()
    }
}