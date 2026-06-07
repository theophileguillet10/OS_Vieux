package com.elderlyos.vieuxos

import android.content.Context
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.os.Build
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

// ── Light theme colors ────────────────────────────────────────────────────────
private val IC_BG           = Color(0xFFF5F5F5)
private val IC_SURFACE      = Color(0xFFFFFFFF)
private val IC_TEXT_PRI     = Color(0xFF111111)
private val IC_TEXT_SEC     = Color(0xFF666666)
private val IC_BORDER       = Color(0xFFDDDDDD)
private val IC_GREEN        = Color(0xFF2E7D32)
private val IC_GREEN_LIGHT  = Color(0xFFE8F5E9)
private val IC_BLUE         = Color(0xFF1565C0)
private val IC_BLUE_LIGHT   = Color(0xFFE3F2FD)
private val IC_RED          = Color(0xFFD32F2F)

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
    var isSpeaker by remember { mutableStateOf(true) } // auto-enable speaker for seniors
    var elapsedSeconds by remember { mutableStateOf(0) }
    var callStarted by remember { mutableStateOf(callState == Call.STATE_ACTIVE) }

    // Route audio to speaker or back to earpiece
    fun setSpeaker(context: Context, on: Boolean) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (on) {
                val speaker = am.availableCommunicationDevices
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                if (speaker != null) am.setCommunicationDevice(speaker)
            } else {
                // Route back to earpiece when speaker is turned off
                val earpiece = am.availableCommunicationDevices
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                if (earpiece != null) am.setCommunicationDevice(earpiece)
                else am.clearCommunicationDevice()
            }
        } else {
            @Suppress("DEPRECATION")
            am.mode = AudioManager.MODE_IN_CALL   // required before routing on older APIs
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = on
        }
    }

    LaunchedEffect(Unit) {
        setSpeaker(context, true) // turn on speaker right away
        onRegisterStateCallback { newState ->
            callState = newState
            if (newState == Call.STATE_ACTIVE) {
                callStarted = true
                setSpeaker(context, isSpeaker)
            }
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

    // Pulse animation on avatar while connecting
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

    val speakerContainerColor by animateColorAsState(
        targetValue = if (isSpeaker) IC_BLUE else IC_SURFACE,
        label = "speakerColor"
    )
    val speakerTextColor by animateColorAsState(
        targetValue = if (isSpeaker) Color.White else IC_TEXT_PRI,
        label = "speakerTextColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IC_BG),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.6f))

        // ── Avatar ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(avatarScale)
                .clip(CircleShape)
                .background(IC_GREEN_LIGHT),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.ifEmpty { "?" },
                color = IC_GREEN,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Caller name ───────────────────────────────────────────────────────
        Text(
            text = callerName,
            color = IC_TEXT_PRI,
            fontSize = 36.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ── Status / Timer ────────────────────────────────────────────────────
        Text(
            text = statusLabel,
            color = IC_TEXT_SEC,
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── Speaker button ────────────────────────────────────────────────────
        Button(
            onClick = {
                val newValue = !isSpeaker
                isSpeaker = newValue
                setSpeaker(context, newValue)   // use local var to avoid stale state read
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(88.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = speakerContainerColor),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = if (isSpeaker) "🔊  Speaker ON" else "🔊  Speaker",
                fontSize = 26.sp,
                color = speakerTextColor,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Hang up button ────────────────────────────────────────────────────
        Button(
            onClick = onHangUp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(96.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IC_RED)
        ) {
            Text(
                text = "📵  Hang Up",
                fontSize = 28.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── NavBar ────────────────────────────────────────────────────────────
        BottomNavBar()
    }
}