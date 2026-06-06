package com.elderlyos.vieuxos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

data class SmsConversation(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val date: Long,
    val unread: Boolean
)

data class SmsMessage(
    val body: String,
    val date: Long,
    val isIncoming: Boolean
)

class MessagesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            showMessages()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), 300)
        }
    }

    private fun showMessages() {
        setContent { MessagesScreen() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 300 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            showMessages()
        }
    }
}

private fun loadConversations(context: android.content.Context): List<SmsConversation> {
    val conversations = mutableListOf<SmsConversation>()
    try {
        val cursor = context.contentResolver.query(
            Telephony.Sms.Conversations.CONTENT_URI,
            arrayOf(
                Telephony.Sms.Conversations.THREAD_ID,
                Telephony.Sms.Conversations.SNIPPET
            ),
            null, null,
            "${Telephony.Sms.Conversations.DEFAULT_SORT_ORDER}"
        )
        cursor?.use {
            val threadIdCol = it.getColumnIndex(Telephony.Sms.Conversations.THREAD_ID)
            val snippetCol = it.getColumnIndex(Telephony.Sms.Conversations.SNIPPET)
            while (it.moveToNext()) {
                val threadId = it.getLong(threadIdCol)
                val snippet = it.getString(snippetCol) ?: ""
                // Get address and date from the actual SMS table
                val smsCursor = context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.READ),
                    "${Telephony.Sms.THREAD_ID} = ?",
                    arrayOf(threadId.toString()),
                    "${Telephony.Sms.DATE} DESC"
                )
                smsCursor?.use { s ->
                    if (s.moveToFirst()) {
                        val address = s.getString(s.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: "Unknown"
                        val date = s.getLong(s.getColumnIndexOrThrow(Telephony.Sms.DATE))
                        val read = s.getInt(s.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
                        conversations.add(SmsConversation(threadId, address, snippet, date, !read))
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Return empty list if SMS reading fails
    }
    return conversations.sortedByDescending { it.date }
}

private fun loadMessages(context: android.content.Context, threadId: Long): List<SmsMessage> {
    val messages = mutableListOf<SmsMessage>()
    try {
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} ASC"
        )?.use { cursor ->
            val bodyCol = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateCol = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeCol = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            while (cursor.moveToNext()) {
                val body = cursor.getString(bodyCol) ?: ""
                val date = cursor.getLong(dateCol)
                val type = cursor.getInt(typeCol)
                messages.add(SmsMessage(body, date, type == Telephony.Sms.MESSAGE_TYPE_INBOX))
            }
        }
    } catch (e: Exception) { /* ignore */ }
    return messages
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 86_400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 604_800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun MessagesScreen() {
    val context = LocalContext.current
    val conversations = remember { loadConversations(context) }
    var selectedThread by remember { mutableStateOf<SmsConversation?>(null) }
    var fontSize by remember { mutableStateOf(18.sp) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header with font size controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF4527A0))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Chat, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (selectedThread != null) selectedThread!!.address else "Messages",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp)
                    )
                }
                // Font size controls
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontSizeButton(Icons.Filled.TextDecrease) {
                        fontSize = (fontSize.value - 2f).coerceAtLeast(12f).sp
                    }
                    FontSizeButton(Icons.Filled.TextIncrease) {
                        fontSize = (fontSize.value + 2f).coerceAtMost(32f).sp
                    }
                }
            }
        }

        if (selectedThread != null) {
            ConversationView(
                thread = selectedThread!!,
                fontSize = fontSize,
                onBack = { selectedThread = null },
                context = context
            )
        } else {
            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No messages found", color = Color(0xFF666666), fontSize = 20.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(conversations) { conv ->
                        ConversationRow(conversation = conv, fontSize = fontSize) {
                            selectedThread = conv
                        }
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    }
                }
            }
            BottomNavBar()
        }
    }
}

@Composable
fun ConversationRow(conversation: SmsConversation, fontSize: TextUnit, onClick: () -> Unit) {
    val initials = conversation.address
        .filter { it.isLetter() }
        .take(2)
        .uppercase()
        .ifEmpty { "#" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (conversation.unread) Color(0xFFF0EEF8) else Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF4527A0).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = Color(0xFF4527A0), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.address,
                color = Color(0xFF1A1A1A),
                fontSize = fontSize,
                fontWeight = if (conversation.unread) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = conversation.snippet,
                color = Color(0xFF666666),
                fontSize = (fontSize.value * 0.82f).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))
        Text(
            text = formatDate(conversation.date),
            color = if (conversation.unread) Color(0xFF4527A0) else Color(0xFF999999),
            fontSize = (fontSize.value * 0.75f).sp,
            fontWeight = if (conversation.unread) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun ConversationView(
    thread: SmsConversation,
    fontSize: TextUnit,
    onBack: () -> Unit,
    context: android.content.Context
) {
    val messages = remember(thread.threadId) { loadMessages(context, thread.threadId) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF8F8F8))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                MessageBubble(message = msg, fontSize = fontSize)
                Spacer(Modifier.height(6.dp))
            }
        }

        Surface(color = Color(0xFFEEEEEE)) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4527A0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Back to Messages", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun MessageBubble(message: SmsMessage, fontSize: TextUnit) {
    val bubbleColor = if (message.isIncoming) Color(0xFFEEEEEE) else Color(0xFF4527A0)
    val textColor = if (message.isIncoming) Color(0xFF1A1A1A) else Color.White
    val alignment = if (message.isIncoming) Alignment.Start else Alignment.End

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = message.body, color = textColor, fontSize = fontSize, lineHeight = (fontSize.value * 1.4f).sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatDate(message.date),
            color = Color(0xFF999999),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun FontSizeButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}
