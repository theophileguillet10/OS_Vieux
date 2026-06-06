package com.elderlyos.vieuxos

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class Contact(
    val name: String,
    val phone: String,
    val avatarColor: Color
)

private val avatarColors = listOf(
    Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A), Color(0xFFE65100),
    Color(0xFF00695C), Color(0xFFAD1457), Color(0xFF4527A0), Color(0xFF37474F),
    Color(0xFF1B6E2E), Color(0xFF1560BD)
)

internal fun loadDeviceContacts(resolver: ContentResolver): List<Contact> {
    val contacts = mutableListOf<Contact>()
    val seen = mutableSetOf<String>()
    resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null, null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
    )?.use { cursor ->
        val nameCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val phoneCol = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        var colorIndex = 0
        while (cursor.moveToNext()) {
            val name = cursor.getString(nameCol) ?: continue
            val phone = cursor.getString(phoneCol) ?: continue
            val key = name + phone
            if (key !in seen) {
                seen.add(key)
                contacts.add(Contact(name, phone, avatarColors[colorIndex % avatarColors.size]))
                colorIndex++
            }
        }
    }
    return contacts
}

class PhoneActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            showScreen()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 400)
        }
    }

    private fun showScreen() {
        setContent { PhoneScreen() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 400 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            showScreen()
        }
    }
}

@Composable
fun PhoneScreen() {
    val context = LocalContext.current
    val contacts = remember { loadDeviceContacts(context.contentResolver) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1565C0))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Contacts",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        HorizontalDivider(color = Color(0xFFDDDDDD), thickness = 1.dp)

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No contacts found", color = Color(0xFF666666), fontSize = 18.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(contacts) { contact ->
                    ContactRow(
                        contact = contact,
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                            context.startActivity(intent)
                        },
                        onMessage = {
                            val intent = Intent(context, MessagesActivity::class.java).apply {
                                putExtra("contact_name", contact.name)
                                putExtra("contact_phone", contact.phone)
                            }
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(
                        color = Color(0xFFEEEEEE),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        BottomNavBar()
    }
}

@Composable
fun ContactRow(contact: Contact, onCall: () -> Unit, onMessage: () -> Unit) {
    val initials = contact.name
        .split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(contact.avatarColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = contact.avatarColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                color = Color(0xFF1A1A1A),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = contact.phone,
                color = Color(0xFF666666),
                fontSize = 14.sp
            )
        }

        Button(
            onClick = onMessage,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4527A0).copy(alpha = 0.12f)),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Chat,
                contentDescription = "Message",
                tint = Color(0xFF4527A0),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onCall,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = contact.avatarColor.copy(alpha = 0.12f)),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = "Call",
                tint = contact.avatarColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
