package com.elderlyos.vieuxos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

// ── Shared event list ─────────────────────────────────────────────────────────
// Format: CalEvent(month 1-12, day, title)
data class CalEvent(val month: Int, val day: Int, val title: String)

val calendarEvents = listOf(
    CalEvent(6,  6,  "Doctor appointment"),
    CalEvent(6,  8,  "Birthday Maman"),
    CalEvent(6,  9,  "Pharmacy"),
    CalEvent(6,  10, "Family lunch"),
    CalEvent(6,  12, "Cardiology checkup"),
    CalEvent(6,  14, "Birthday Pierre"),
)

fun eventsForDay(month: Int, day: Int): List<CalEvent> =
    calendarEvents.filter { it.month == month && it.day == day }

// ── Activity ──────────────────────────────────────────────────────────────────
class CalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalendarScreen() }
    }
}

@Composable
fun CalendarScreen() {
    val today = remember { Calendar.getInstance() }

    // Build the 7 days of the current week (Mon → Sun)
    val weekDays = remember {
        val week = mutableListOf<Calendar>()
        val cal = today.clone() as Calendar
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        repeat(7) {
            week.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        week
    }

    val dayNameFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dayNumFmt  = remember { SimpleDateFormat("d",   Locale.getDefault()) }
    val monthFmt   = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF3949AB))
                .padding(horizontal = 24.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    monthFmt.format(today.time).replaceFirstChar { it.uppercaseChar() },
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            weekDays.forEach { day ->
                val isToday = day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                    && day.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                val month = day.get(Calendar.MONTH) + 1
                val dayNum = day.get(Calendar.DAY_OF_MONTH)
                val events = eventsForDay(month, dayNum)

                WeekDayCard(
                    dayName = dayNameFmt.format(day.time).replaceFirstChar { it.uppercaseChar() },
                    dayNum = dayNumFmt.format(day.time),
                    isToday = isToday,
                    events = events
                )
            }
        }

        BottomNavBar()
    }
}

@Composable
fun WeekDayCard(dayName: String, dayNum: String, isToday: Boolean, events: List<CalEvent>) {
    val borderColor = if (isToday) Color(0xFF3949AB) else Color.Transparent
    val headerBg   = if (isToday) Color(0xFF3949AB) else Color(0xFFDDDDDD)
    val headerText = if (isToday) Color.White else Color(0xFF444444)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = if (isToday) 6.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isToday) 2.dp else 0.dp, borderColor, RoundedCornerShape(14.dp))
    ) {
        Column {
            // Day header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dayName $dayNum",
                    color = headerText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isToday) {
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Text(
                            "Today",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Events
            if (events.isEmpty()) {
                Text(
                    "No events",
                    color = Color(0xFFAAAAAA),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            } else {
                events.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Event, null, tint = Color(0xFF3949AB), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(event.title, color = Color(0xFF222222), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}
