package com.elderlyos.vieuxos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        ContactRepository.init(this)
        val contactName = intent.getStringExtra("contact_name")
        val contactPhone = intent.getStringExtra("contact_phone")
        val needed = arrayOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS)
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            showMessages(contactName, contactPhone)
        } else {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 300)
        }
    }

    private fun showMessages(contactName: String? = null, contactPhone: String? = null) {
        setContent { MessagesScreen(initialName = contactName, initialPhone = contactPhone) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 300) showMessages()
    }
}

private fun resolveContactName(phone: String): String {
    val normalized = phone.replace(" ", "").replace("-", "")
    return ContactRepository.contacts.find { contact ->
        contact.phone.replace(" ", "").replace("-", "").endsWith(normalized.takeLast(9))
                || normalized.endsWith(contact.phone.replace(" ", "").replace("-", "").takeLast(9))
    }?.name ?: phone
}

// ── Fake test data ────────────────────────────────────────────────────────────
private const val FAKE_THREAD_ID = -99L

private val fakeConversations = listOf(
    SmsConversation(
        threadId = FAKE_THREAD_ID,
        address  = "+33 6 12 34 56 78",
        snippet  = "Don't forget your doctor's appointment tomorrow!",
        date     = System.currentTimeMillis() - 3_600_000,
        unread   = true
    )
)

private val fakeMessages = listOf(
    SmsMessage("Hi dear, how are you feeling today? 😊", System.currentTimeMillis() - 7_200_000, isIncoming = true),
    SmsMessage("I'm fine thanks! A bit tired but good.", System.currentTimeMillis() - 6_900_000, isIncoming = false),
    SmsMessage("Make sure you eat well and drink water!", System.currentTimeMillis() - 6_600_000, isIncoming = true),
    SmsMessage("Yes Mum, I will 😄", System.currentTimeMillis() - 6_300_000, isIncoming = false),
    SmsMessage("Don't forget your doctor's appointment tomorrow!", System.currentTimeMillis() - 3_600_000, isIncoming = true),
)

private fun loadConversations(context: android.content.Context): List<SmsConversation> {
    val conversations = mutableListOf<SmsConversation>()
    try {
        val cursor = context.contentResolver.query(
            Telephony.Sms.Conversations.CONTENT_URI,
            arrayOf(Telephony.Sms.Conversations.THREAD_ID, Telephony.Sms.Conversations.SNIPPET),
            null, null,
            "${Telephony.Sms.Conversations.DEFAULT_SORT_ORDER}"
        )
        cursor?.use {
            val threadIdCol = it.getColumnIndex(Telephony.Sms.Conversations.THREAD_ID)
            val snippetCol = it.getColumnIndex(Telephony.Sms.Conversations.SNIPPET)
            while (it.moveToNext()) {
                val threadId = it.getLong(threadIdCol)
                val snippet = it.getString(snippetCol) ?: ""
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
    } catch (e: Exception) { /* ignore */ }
    val real = conversations.sortedByDescending { it.date }
    return if (real.isEmpty()) fakeConversations else real
}

private fun loadMessages(context: android.content.Context, threadId: Long): List<SmsMessage> {
    if (threadId == FAKE_THREAD_ID) return fakeMessages
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
                messages.add(SmsMessage(
                    cursor.getString(bodyCol) ?: "",
                    cursor.getLong(dateCol),
                    cursor.getInt(typeCol) == Telephony.Sms.MESSAGE_TYPE_INBOX
                ))
            }
        }
    } catch (e: Exception) { /* ignore */ }
    return messages
}

private fun formatDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 86_400_000  -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 604_800_000 -> SimpleDateFormat("EEE",   Locale.getDefault()).format(Date(timestamp))
        else               -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}

// ── Shared scroll button bar ──────────────────────────────────────────────────

@Composable
fun MessagesScreen(initialName: String? = null, initialPhone: String? = null) {
    val context = LocalContext.current
    var conversations by remember { mutableStateOf(loadConversations(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3_000)
            conversations = loadConversations(context)
        }
    }

    val initialThread = remember(initialPhone) {
        if (initialPhone != null) {
            conversations.find { it.address == initialPhone }
                ?: SmsConversation(-1L, initialPhone, "", System.currentTimeMillis(), false)
        } else null
    }
    var selectedThread by remember { mutableStateOf<SmsConversation?>(initialThread) }
    var fontSize by remember { mutableStateOf(22.sp) }
    var showContactPicker by remember { mutableStateOf(false) }

    // Scroll state for the conversation list
    val convListState = rememberLazyListState()
    val convScope = rememberCoroutineScope()
    val visibleCount = 4

    fun displayName(thread: SmsConversation): String {
        val fromRepo = resolveContactName(thread.address)
        return when {
            fromRepo != thread.address -> fromRepo
            thread == initialThread && initialName != null -> initialName
            else -> thread.address
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────────
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
                    if (selectedThread != null) {
                        IconButton(onClick = { selectedThread = null }) {
                            Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = if (selectedThread != null) displayName(selectedThread!!) else "Messages",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontSizeButton(Icons.Filled.TextDecrease) {
                        if (fontSize.value > 14f) fontSize = (fontSize.value - 2).sp
                    }
                    FontSizeButton(Icons.Filled.TextIncrease) {
                        if (fontSize.value < 30f) fontSize = (fontSize.value + 2).sp
                    }
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        if (selectedThread == null) {
            // ── New Message button ────────────────────────────────────────────
            Button(
                onClick = { showContactPicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4527A0))
            ) {
                Text("✉️  New Message", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = Color(0xFFEEEEEE))

            if (conversations.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No messages yet", color = Color(0xFF999999), fontSize = fontSize)
                }
            } else {
                LazyColumn(
                    state = convListState,
                    userScrollEnabled = false,
                    modifier = Modifier.weight(1f)
                ) {
                    items(conversations) { conv ->
                        ConversationRow(
                            conversation = conv,
                            displayName = displayName(conv),
                            fontSize = fontSize
                        ) { selectedThread = conv }
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                    }
                }
            }

            // ── ▲ ▼ for conversation list ─────────────────────────────────────
            ScrollButtons(
                onUp = {
                    convScope.launch {
                        convListState.animateScrollToItem(
                            maxOf(0, convListState.firstVisibleItemIndex - visibleCount)
                        )
                    }
                },
                onDown = {
                    convScope.launch {
                        convListState.animateScrollToItem(
                            minOf(conversations.size - 1, convListState.firstVisibleItemIndex + visibleCount)
                        )
                    }
                }
            )
        } else {
            // ConversationView fills the rest; BottomNavBar is below it
            ConversationView(
                thread = selectedThread!!,
                fontSize = fontSize,
                context = context,
                modifier = Modifier.weight(1f)
            )
        }

        // ── NavBar always visible ─────────────────────────────────────────────
        BottomNavBar(
            onBack = { if (selectedThread != null) selectedThread = null }
        )
    }

    if (showContactPicker) {
        ContactPickerDialog(
            contacts = ContactRepository.contacts,
            onDismiss = { showContactPicker = false },
            onSelect = { contact ->
                showContactPicker = false
                val existing = conversations.find { conv ->
                    resolveContactName(conv.address) == contact.name ||
                            conv.address.replace(" ", "") == contact.phone.replace(" ", "")
                }
                selectedThread = existing ?: SmsConversation(
                    threadId = -1L,
                    address  = contact.phone,
                    snippet  = "",
                    date     = System.currentTimeMillis(),
                    unread   = false
                )
            }
        )
    }
}

@Composable
fun ConversationRow(
    conversation: SmsConversation,
    displayName: String,
    fontSize: TextUnit,
    onClick: () -> Unit
) {
    val initials = displayName
        .split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
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
                text = displayName,
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
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    var messages by remember(thread.threadId) { mutableStateOf(loadMessages(context, thread.threadId)) }
    var messageText by remember { mutableStateOf("") }
    val msgListState = rememberLazyListState()
    val msgScope = rememberCoroutineScope()
    val visibleCount = 3

    LaunchedEffect(thread.threadId) {
        while (true) {
            delay(3_000)
            messages = loadMessages(context, thread.threadId)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Message bubbles ───────────────────────────────────────────────────
        LazyColumn(
            state = msgListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF8F8F8))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            reverseLayout = true,
            userScrollEnabled = false
        ) {
            items(messages.reversed()) { msg ->
                MessageBubble(message = msg, fontSize = fontSize)
                Spacer(Modifier.height(6.dp))
            }
        }

        // ── ▲ ▼ for message list ──────────────────────────────────────────────
        ScrollButtons(
            onUp = {
                msgScope.launch {
                    msgListState.animateScrollToItem(
                        maxOf(0, msgListState.firstVisibleItemIndex - visibleCount)
                    )
                }
            },
            onDown = {
                msgScope.launch {
                    msgListState.animateScrollToItem(
                        minOf(messages.size - 1, msgListState.firstVisibleItemIndex + visibleCount)
                    )
                }
            }
        )

        // ── Text field + Send ─────────────────────────────────────────────────
        Surface(color = Color(0xFFEEEEEE)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Write a message…", fontSize = fontSize) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1A1A1A),
                        unfocusedTextColor = Color(0xFF1A1A1A),
                        focusedBorderColor = Color(0xFF4527A0),
                        unfocusedBorderColor = Color(0xFFCCCCCC),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = fontSize)
                )
                Button(
                    onClick = {
                        val text = messageText.trim()
                        if (text.isNotEmpty()) {
                            try {
                                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    context.getSystemService(SmsManager::class.java)
                                } else {
                                    @Suppress("DEPRECATION")
                                    SmsManager.getDefault()
                                }
                                if (smsManager == null) {
                                    android.widget.Toast.makeText(context, "SMS not available on this device", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    // Add message instantly to the list for immediate feedback
                                    messages = messages + SmsMessage(
                                        body = text,
                                        date = System.currentTimeMillis(),
                                        isIncoming = false
                                    )
                                    messageText = ""
                                    // Send in background
                                    smsManager.sendTextMessage(thread.address, null, text, null, null)
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Failed to send: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4527A0)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Filled.Send, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: SmsMessage, fontSize: TextUnit) {
    val bubbleColor = if (message.isIncoming) Color(0xFFEEEEEE) else Color(0xFF4527A0)
    val textColor   = if (message.isIncoming) Color(0xFF1A1A1A) else Color.White
    val alignment   = if (message.isIncoming) Alignment.Start   else Alignment.End

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