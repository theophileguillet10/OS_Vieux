package com.elderlyos.vieuxos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.telephony.SmsManager
import android.view.WindowInsetsController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide the system navigation bar (immersive mode)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        setContent { HomeScreen() }
    }
}

// Pages: list of tile lists. Add more pages by adding more lists here.
private val PAGE_COUNT = 2

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var currentPage by remember { mutableIntStateOf(0) }
    var slideDirection by remember { mutableIntStateOf(1) }

    var batteryPct by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = context.registerReceiver(null, filter)
        receiver?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) batteryPct = (level * 100 / scale)
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL
        }
        val br = object : BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) batteryPct = (level * 100 / scale)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(br, filter)
        onDispose { context.unregisterReceiver(br) }
    }

    var isWifiConnected by remember { mutableStateOf(false) }
    var isBluetoothEnabled by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        fun checkWifi() {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            isWifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
        fun checkBt() {
            isBluetoothEnabled = try {
                context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
            } catch (e: Exception) { false }
        }
        checkWifi(); checkBt()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: Intent) {
                when (intent.action) {
                    ConnectivityManager.CONNECTIVITY_ACTION -> checkWifi()
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        isBluetoothEnabled = state == BluetoothAdapter.STATE_ON
                    }
                }
            }
        }
        val combined = IntentFilter().apply {
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(receiver, combined)
        onDispose { context.unregisterReceiver(receiver) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEEEEE))
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                slideInHorizontally { it * slideDirection } togetherWith
                        slideOutHorizontally { -it * slideDirection }
            },
            modifier = Modifier.weight(1f),
            label = "page"
        ) { page ->
            when (page) {
                0 -> PageOne(context)
                1 -> PageTwo(context)
                else -> PageOne(context)
            }
        }

        // Page number + battery indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFDDDDDD))
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${currentPage + 1} / $PAGE_COUNT",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.align(Alignment.Center)
            )
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isWifiConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = if (isWifiConnected) Color(0xFF2E7D32) else Color(0xFF9E9E9E),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (isBluetoothEnabled) Icons.Filled.Bluetooth else Icons.Filled.BluetoothDisabled,
                    contentDescription = null,
                    tint = if (isBluetoothEnabled) Color(0xFF1565C0) else Color(0xFF9E9E9E),
                    modifier = Modifier.size(36.dp)
                )
            }
            val batteryIcon = when {
                isCharging -> Icons.Filled.BatteryChargingFull
                batteryPct >= 80 -> Icons.Filled.BatteryFull
                batteryPct >= 50 -> Icons.Filled.Battery5Bar
                batteryPct >= 20 -> Icons.Filled.Battery3Bar
                else -> Icons.Filled.Battery1Bar
            }
            val batteryColor = when {
                batteryPct <= 20 -> Color(0xFFE53935)
                batteryPct <= 50 -> Color(0xFFF57C00)
                else -> Color(0xFF2E7D32)
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(batteryIcon, null, tint = batteryColor, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$batteryPct%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = batteryColor
                )
            }
        }

        BottomNavBar(
            onLeft = {
                if (currentPage > 0) {
                    slideDirection = -1
                    currentPage--
                }
            },
            onRight = {
                if (currentPage < PAGE_COUNT - 1) {
                    slideDirection = 1
                    currentPage++
                }
            },
            onHome = {
                if (currentPage != 0) {
                    slideDirection = -1
                    currentPage = 0
                }
            }
        )
    }
}

@Composable
fun PageOne(context: android.content.Context) {
    val s = getStrings(context)
    var showSOSDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            AppTile(s.callFamily, Icons.Filled.Phone, Color(0xFF2E7D32)) {
                context.startActivity(Intent(context, PhoneActivity::class.java))
            }
        }
        item {
            AppTile(s.takeHome, Icons.Filled.Home, Color(0xFF00838F)) {
                context.startActivity(Intent(context, GoHomeActivity::class.java))
            }
        }
        item {
            AppTile(s.camera, Icons.Filled.PhotoCamera, Color(0xFF6A1B9A)) {
                context.startActivity(Intent(context, CameraActivity::class.java))
            }
        }
        item {
            AppTile(s.gallery, Icons.Filled.PhotoLibrary, Color(0xFFF57C00)) {
                context.startActivity(Intent(context, GalleryActivity::class.java))
            }
        }
        item {
            AppTile(s.messages, Icons.Filled.Chat, Color(0xFF4527A0)) {
                context.startActivity(Intent(context, MessagesActivity::class.java))
            }
        }
        item {
            AppTile(
                label = s.sos,
                icon = Icons.Filled.Warning,
                color = Color(0xFFC62828),
                onLongClick = {
                    context.startActivity(Intent(context, FamilySetupActivity::class.java))
                }
            ) {
                showSOSDialog = true
            }
        }
        }

        MedicationBanner(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    // ── SOS confirmation dialog ───────────────────────────────────────────────
    if (showSOSDialog) {
        SOSConfirmDialog(
            context   = context,
            onDismiss = { showSOSDialog = false }
        )
    }
}

@Composable
fun PageTwo(context: android.content.Context) {
    val s = getStrings(context)
    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppTile(s.youtube, Icons.Filled.PlayCircle, Color(0xFFCC0000)) {
                    context.startActivity(Intent(context, YoutubeActivity::class.java))
                }
            }
            item {
                AppTile(s.internet, Icons.Filled.Language, Color(0xFF1A73E8)) {
                    context.startActivity(Intent(context, ChromeActivity::class.java))
                }
            }
            item {
                AppTile(s.weather, Icons.Filled.WbSunny, Color(0xFFFFA000)) {
                    context.startActivity(Intent(context, MeteoActivity::class.java))
                }
            }
            item {
                AppTile(s.wallet, Icons.Filled.CreditCard, Color(0xFF00695C)) {
                    context.startActivity(Intent(context, WalletActivity::class.java))
                }
            }
            item {
                AppTile(s.torch, Icons.Filled.Bolt, Color(0xFF455A64)) {
                    context.startActivity(Intent(context, TorchActivity::class.java))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        BirthdayBanner(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    label: String,
    icon: ImageVector,
    color: Color,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(color, RoundedCornerShape(8.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = label,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val bannerColors = listOf(
    Color(0xFFE53935),
    Color(0xFF8E24AA),
    Color(0xFF1E88E5),
    Color(0xFF00897B),
    Color(0xFFF4511E),
    Color(0xFF3949AB),
)

@Composable
fun MedicationBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val medications = remember { loadMedications(context) }
    var currentIndex by remember { mutableIntStateOf(0) }

    // Static color — changes only when the displayed medication changes (no flashing)
    val bannerColor = bannerColors[currentIndex % bannerColors.size]

    LaunchedEffect(medications.size) {
        while (medications.isNotEmpty()) {
            delay(3000)
            currentIndex = (currentIndex + 1) % medications.size
        }
    }

    Box(
        modifier = modifier.background(bannerColor, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (medications.isEmpty()) {
            Text("No medications", color = Color.White, fontSize = 20.sp)
        } else {
            val med = medications[currentIndex]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Medication, null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(6.dp))
                Text(med.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(med.meal, color = Color.White.copy(alpha = 0.9f), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun BirthdayBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val allEvents = remember { loadEvents(context) }
    val todaysEvents = remember {
        val cal = java.util.Calendar.getInstance()
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        eventsForDay(allEvents, month, day)
    }

    Box(
        modifier = modifier
            .background(Color(0xFF3949AB), RoundedCornerShape(16.dp))
            .clickable { context.startActivity(Intent(context, CalendarActivity::class.java)) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text("Today's Events", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            if (todaysEvents.isEmpty()) {
                Text("No events today", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            } else {
                todaysEvents.forEach { event ->
                    Text(
                        text = event.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Tap to see the week →", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        }
    }
}

@Composable
fun BigButton(label: String, color: Color, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

// ── SOS: send SMS to all contacts ─────────────────────────────────────────────
private fun sendSOSMessages(context: android.content.Context) {
    val prefs    = context.getSharedPreferences("vieuxos_prefs", android.content.Context.MODE_PRIVATE)
    val meetLink = prefs.getString("meet_link", "") ?: ""
    val contacts = loadContacts(context)
    val message  = if (meetLink.isNotBlank())
        "I need help! Please join me: $meetLink"
    else
        "I need help! Please contact me as soon as possible."

    try {
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        contacts.forEach { contact ->
            val number = contact.phone.replace(" ", "")
            smsManager?.sendTextMessage(number, null, message, null, null)
        }
    } catch (e: Exception) { e.printStackTrace() }

    // Open Meet link if set
    if (meetLink.isNotBlank()) {
        val url = if (meetLink.startsWith("http")) meetLink else "https://$meetLink"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}

// ── SOS confirmation dialog ───────────────────────────────────────────────────
@Composable
fun SOSConfirmDialog(context: android.content.Context, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .background(Color(0xFFB71C1C), RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = androidx.compose.ui.Modifier.size(64.dp)
                )
                Spacer(androidx.compose.ui.Modifier.height(16.dp))
                Text(
                    text = "Send SOS?",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(androidx.compose.ui.Modifier.height(8.dp))
                Text(
                    text = "An emergency message will be sent to all your contacts.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                Spacer(androidx.compose.ui.Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Cancel
                    Button(
                        onClick = onDismiss,
                        modifier = androidx.compose.ui.Modifier.weight(1f).height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555))
                    ) {
                        Text("Cancel", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    // Confirm
                    Button(
                        onClick = {
                            onDismiss()
                            sendSOSMessages(context)
                        },
                        modifier = androidx.compose.ui.Modifier.weight(1f).height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("SEND", fontSize = 20.sp, color = Color(0xFFB71C1C), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
