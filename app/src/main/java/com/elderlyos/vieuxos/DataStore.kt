package com.elderlyos.vieuxos

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    val network = cm?.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable
fun NoInternetGuard(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var online by remember { mutableStateOf(isOnline(context)) }

    if (!online) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(24.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Icon(Icons.Filled.WifiOff, null, tint = Color(0xFFE53935), modifier = Modifier.size(80.dp))
                    Text(
                        "No Internet",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Please connect to Wi-Fi or mobile data and try again.",
                        color = Color(0xFFAAAAAA),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(Color(0xFF1565C0), RoundedCornerShape(16.dp))
                            .clickable { online = isOnline(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Try Again", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            BottomNavBar()
        }
    } else {
        content()
    }
}

private const val PREFS = "vieuxos_prefs"

// ── Contacts ──────────────────────────────────────────────────────────────────

private val defaultContacts = listOf(
    Contact("Pauline",        "+41 78 699 52 05"),
    Contact("Axel",         "+41 77 422 67 08"),
    Contact("Martin",        "+41 79 582 26 63"),
    Contact("Théo",       "+33 7 68 88 91 50"),
)

fun loadContacts(context: Context): List<Contact> {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString("contacts", null) ?: return defaultContacts
    return raw.trim().split("\n").mapNotNull { line ->
        val parts = line.split("|")
        if (parts.size == 2) Contact(parts[0], parts[1]) else null
    }.ifEmpty { defaultContacts }
}

fun saveContacts(context: Context, contacts: List<Contact>) {
    val value = contacts.joinToString("\n") { "${it.name}|${it.phone}" }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("contacts", value).apply()
}

// ── Medications ───────────────────────────────────────────────────────────────

data class Medication(val name: String, val meal: String)

private val defaultMedications = listOf(
    Medication("Doliprane 1000mg", "Breakfast"),
)

fun loadMedications(context: Context): List<Medication> {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString("medications", null) ?: return defaultMedications
    return raw.trim().split("\n").mapNotNull { line ->
        val parts = line.split("|")
        if (parts.size == 2) Medication(parts[0], parts[1]) else null
    }.ifEmpty { defaultMedications }
}

fun saveMedications(context: Context, list: List<Medication>) {
    val value = list.joinToString("\n") { "${it.name}|${it.meal}" }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("medications", value).apply()
}

// ── Calendar Events ───────────────────────────────────────────────────────────

data class CalEvent(val month: Int, val day: Int, val title: String)

private val defaultEvents = listOf(
    CalEvent(6,  6,  "Doctor appointment"),
    CalEvent(6,  6,  "Take medication at breakfast"),
)

fun loadEvents(context: Context): List<CalEvent> {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString("events", null) ?: return defaultEvents
    return raw.trim().split("\n").mapNotNull { line ->
        val parts = line.split("|")
        if (parts.size == 3) CalEvent(parts[0].toIntOrNull() ?: return@mapNotNull null, parts[1].toIntOrNull() ?: return@mapNotNull null, parts[2]) else null
    }.ifEmpty { defaultEvents }
}

fun saveEvents(context: Context, list: List<CalEvent>) {
    val value = list.joinToString("\n") { "${it.month}|${it.day}|${it.title}" }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("events", value).apply()
}

fun eventsForDay(events: List<CalEvent>, month: Int, day: Int): List<CalEvent> =
    events.filter { it.month == month && it.day == day }
