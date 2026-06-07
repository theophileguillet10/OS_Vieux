package com.elderlyos.vieuxos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat

// ── Light theme colors ────────────────────────────────────────────────────────
private val BG          = Color(0xFFF5F5F5)
private val SURFACE     = Color(0xFFFFFFFF)
private val BORDER      = Color(0xFFDDDDDD)
private val TEXT_PRI    = Color(0xFF111111)
private val TEXT_SEC    = Color(0xFF666666)
private val RED         = Color(0xFFD32F2F)
private val RED_LIGHT   = Color(0xFFFFEBEE)
private val GREEN       = Color(0xFF2E7D32)
private val GREEN_LIGHT = Color(0xFFE8F5E9)
private val BLUE        = Color(0xFF1565C0)
private val BLUE_LIGHT  = Color(0xFFE3F2FD)

private val sosAvatarColors = listOf(
    Color(0xFF1B6E2E), Color(0xFF1560BD), Color(0xFF7B1FA2),
    Color(0xFFE65100), Color(0xFF00695C), Color(0xFFAD1457),
    Color(0xFF4527A0), Color(0xFF37474F), Color(0xFF00838F),
    Color(0xFF558B2F),
)

class SOSActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled at use site */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init contacts synchronously before Compose renders
        ContactRepository.init(this)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,
            )
        )

        setContent { SOSScreen() }
    }
}

@Composable
fun SOSScreen() {
    val context = LocalContext.current

    // ── Read Meet link from main prefs (configured in developer mode) ─────────
    val prefs = remember { context.getSharedPreferences("vieuxos_prefs", Context.MODE_PRIVATE) }
    val meetLink = prefs.getString("meet_link", "") ?: ""
    var showContactPicker by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var statusMessage by remember { mutableStateOf("") }

    val contacts = ContactRepository.contacts

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SURFACE)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🆘  Call for Help",
                color = TEXT_PRI,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        HorizontalDivider(color = BORDER, thickness = 1.dp)

        Spacer(modifier = Modifier.height(24.dp))

        // ── Big SOS button ────────────────────────────────────────────────────
        Button(
            onClick = { showContactPicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(130.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RED)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🆘", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "CALL FOR HELP",
                    fontSize = 26.sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Status feedback ───────────────────────────────────────────────────
        if (statusMessage.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(16.dp),
                color = GREEN_LIGHT,
                tonalElevation = 0.dp
            ) {
                Text(
                    text = statusMessage,
                    color = GREEN,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Info card ─────────────────────────────────────────────────────────
        val s = getStrings(context)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            shape = RoundedCornerShape(20.dp),
            color = BLUE_LIGHT,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(s.howItWorks, color = BLUE, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(s.howItWorksBody, color = Color(0xFF1A3A6B), fontSize = 15.sp, lineHeight = 22.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider(color = BORDER, thickness = 1.dp)
        BottomNavBar()
    }

    // ── Contact picker ────────────────────────────────────────────────────────
    if (showContactPicker) {
        ContactPickerDialog(
            contacts = contacts,
            onDismiss = { showContactPicker = false },
            onSelect = { contact ->
                selectedContact = contact
                showContactPicker = false
                showConfirm = true
            }
        )
    }

    // ── Confirmation dialog ───────────────────────────────────────────────────
    if (showConfirm && selectedContact != null) {
        ConfirmSOSDialog(
            contact = selectedContact!!,
            meetLink = meetLink,
            onDismiss = {
                showConfirm = false
                selectedContact = null
            },
            onConfirm = {
                showConfirm = false
                val contact = selectedContact!!
                selectedContact = null

                // 1. Send SMS with Meet link
                sendSOSSms(context, contact.phone, meetLink)

                // 2. Open Meet on this device
                openMeetLink(context, meetLink)

                statusMessage = "✅ SMS sent to ${contact.name} — opening Meet…"
            }
        )
    }
}

// ── Send SMS ──────────────────────────────────────────────────────────────────
private fun sendSOSSms(context: Context, phone: String, meetLink: String) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
        != PackageManager.PERMISSION_GRANTED) return

    val number = phone.replace(" ", "")
    val message = "🆘 I need help! Please join me on Google Meet:\n$meetLink"

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(number, null, message, null, null)
        } else {
            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, message, null, null)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// ── Open Meet link ────────────────────────────────────────────────────────────
private fun openMeetLink(context: Context, meetLink: String) {
    val url = if (meetLink.startsWith("http")) meetLink else "https://$meetLink"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

// ── Dialog: pick a contact ────────────────────────────────────────────────────
@Composable
fun ContactPickerDialog(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onSelect: (Contact) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SURFACE,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                Text(
                    "Who do you want to call?",
                    color = TEXT_PRI, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BORDER)

                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(contacts) { contact ->
                        val colorIndex = contacts.indexOf(contact) % sosAvatarColors.size
                        val avatarColor = sosAvatarColors[colorIndex]
                        val initials = contact.name.split(" ").take(2)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .joinToString("")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(contact) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    color = avatarColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.name, color = TEXT_PRI, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                Text(contact.phone, color = TEXT_SEC, fontSize = 15.sp)
                            }
                            Text("›", color = TEXT_SEC, fontSize = 22.sp)
                        }
                        HorizontalDivider(
                            color = BORDER, thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TEXT_SEC)
                ) { Text("Cancel", fontSize = 16.sp) }
            }
        }
    }
}

// ── Dialog: confirm SOS ───────────────────────────────────────────────────────
@Composable
fun ConfirmSOSDialog(
    contact: Contact,
    meetLink: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SURFACE,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("🆘  Confirm", color = RED, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = RED_LIGHT,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("An SMS will be sent to:", color = TEXT_SEC, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(contact.name, color = TEXT_PRI, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(contact.phone, color = TEXT_SEC, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Message:", color = TEXT_SEC, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🆘 I need help! Please join me on Google Meet:\n$meetLink",
                            color = TEXT_PRI, fontSize = 14.sp, lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TEXT_SEC)
                    ) { Text("Cancel", fontSize = 16.sp) }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RED)
                    ) { Text("Send & Join", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}