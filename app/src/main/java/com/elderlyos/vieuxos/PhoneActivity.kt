package com.elderlyos.vieuxos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Contact(
    val name: String,
    val phone: String,
    val avatarColor: Color
)

val sampleContacts = listOf(
    Contact("Maman",         "+33 6 12 34 56 78", Color(0xFF1B6E2E)),
    Contact("Papa",          "+33 6 98 76 54 32", Color(0xFF1560BD)),
    Contact("Marie",         "+33 6 11 22 33 44", Color(0xFF7B1FA2)),
    Contact("Pierre",        "+33 6 55 66 77 88", Color(0xFFE65100)),
    Contact("Docteur Brun",  "+33 1 42 00 11 22", Color(0xFF00695C)),
    Contact("Nathalie",      "+33 6 44 55 66 77", Color(0xFFAD1457)),
    Contact("Jean-Claude",   "+33 6 33 44 55 66", Color(0xFF4527A0)),
    Contact("Pharmacie",     "+33 1 45 67 89 00", Color(0xFF37474F)),
)

class PhoneActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhoneScreen()
        }
    }
}

@Composable
fun PhoneScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📞  Téléphone simple",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Divider(color = Color(0xFF222222), thickness = 0.5.dp)

        // Contacts list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(sampleContacts) { contact ->
                ContactRow(contact = contact) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                    context.startActivity(intent)
                }
                Divider(
                    color = Color(0xFF1A1A1A),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun ContactRow(contact: Contact, onClick: () -> Unit) {
    val initials = contact.name
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
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

        Spacer(modifier = Modifier.width(14.dp))

        // Name & number
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = contact.phone,
                color = Color(0xFF666666),
                fontSize = 14.sp
            )
        }

        // Call icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B2E1B)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📞", fontSize = 18.sp)
        }
    }
}
