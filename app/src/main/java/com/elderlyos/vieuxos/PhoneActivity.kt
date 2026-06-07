package com.elderlyos.vieuxos

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Light theme colors ────────────────────────────────────────────────────────
private val PH_BG           = Color(0xFFF5F5F5)
private val PH_SURFACE      = Color(0xFFFFFFFF)
private val PH_TEXT_PRI     = Color(0xFF111111)
private val PH_TEXT_SEC     = Color(0xFF666666)
private val PH_BORDER       = Color(0xFFDDDDDD)
private val PH_BLUE         = Color(0xFF1565C0)
private val PH_BLUE_LIGHT   = Color(0xFFE3F2FD)
private val PH_ORANGE       = Color(0xFFE65100)
private val PH_ORANGE_LIGHT = Color(0xFFFFF3E0)
private val PH_BTN_DARK     = Color(0xFF424242)

private val avatarColors = listOf(
    Color(0xFF1B6E2E), Color(0xFF1560BD), Color(0xFF7B1FA2),
    Color(0xFFE65100), Color(0xFF00695C), Color(0xFFAD1457),
    Color(0xFF4527A0), Color(0xFF37474F), Color(0xFF00838F),
    Color(0xFF558B2F),
)

class PhoneActivity : ComponentActivity() {

    private val defaultDialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* result ignored — ON_RESUME recheck via DisposableEffect */ }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled at use site */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
            )
        )

        setContent {
            PhoneScreen(onRequestDefaultDialer = { requestDefaultDialer() })
        }
    }

    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)
                && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            ) {
                defaultDialerLauncher.launch(
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                )
            }
        } else {
            defaultDialerLauncher.launch(
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    packageName
                )
            )
        }
    }
}

private fun isDefaultDialer(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.getSystemService(RoleManager::class.java)
            ?.isRoleHeld(RoleManager.ROLE_DIALER) == true
    } else {
        context.getSystemService(TelecomManager::class.java)
            ?.defaultDialerPackage == context.packageName
    }
}

@Composable
private fun rememberIsDefaultDialer(): Boolean {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(isDefaultDialer(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            isDefault = isDefaultDialer(context)
            delay(1000)
        }
    }

    return isDefault
}

@Composable
fun PhoneScreen(onRequestDefaultDialer: () -> Unit) {
    val context = LocalContext.current
    val isDefault = rememberIsDefaultDialer()

    val contacts = remember { mutableStateListOf<Contact>().also { it.addAll(loadContacts(context)) } }

    var showAddDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val visibleCount = 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PH_BG)
    ) {
        // ── Default dialer banner ─────────────────────────────────────────────
        if (!isDefault) {
            DefaultDialerBanner(onRequestDefaultDialer = onRequestDefaultDialer)
        }

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E7D32))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Phone",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // ── Contact list ──────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(PH_SURFACE)
        ) {
            items(contacts) { contact ->
                val colorIndex = contacts.indexOf(contact) % avatarColors.size
                ContactRow(contact = contact, avatarColor = avatarColors[colorIndex]) {
                    val number = contact.phone.replace(" ", "")
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
                    } else {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                    }
                }
                HorizontalDivider(
                    color = PH_BORDER, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        HorizontalDivider(color = PH_BORDER, thickness = 1.dp)

        // ── Up / Down / Add bar ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PH_SURFACE)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(maxOf(0, listState.firstVisibleItemIndex - visibleCount))
                    }
                },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PH_BTN_DARK)
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(minOf(contacts.size - 1, listState.firstVisibleItemIndex + visibleCount))
                    }
                },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PH_BTN_DARK)
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PH_BLUE)
            ) { Text("＋  Add", fontSize = 18.sp, color = Color.White) }
        }

        BottomNavBar()
    }

    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone ->
                if (name.isNotBlank() && phone.isNotBlank()) {
                    contacts.add(Contact(name.trim(), phone.trim()))
                    saveContacts(context, contacts)
                }
                showAddDialog = false
            }
        )
    }
}

// ── Default dialer banner ─────────────────────────────────────────────────────
@Composable
fun DefaultDialerBanner(onRequestDefaultDialer: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PH_ORANGE_LIGHT)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("⚠️", fontSize = 22.sp)
        Text(
            text = "VieuxOS is not the default phone app.\nTap Activate to use the custom call screen.",
            color = PH_ORANGE,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onRequestDefaultDialer,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PH_ORANGE)
        ) { Text("Activate", color = Color.White, fontSize = 14.sp) }
    }
}

// ── Contact row ───────────────────────────────────────────────────────────────
@Composable
fun ContactRow(contact: Contact, avatarColor: Color, onClick: () -> Unit) {
    val initials = contact.name
        .split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(avatarColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, color = avatarColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = contact.name, color = PH_TEXT_PRI, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = contact.phone, color = PH_TEXT_SEC, fontSize = 22.sp)
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) { Text(text = "📞", fontSize = 24.sp) }
    }
}

// ── Add contact dialog ────────────────────────────────────────────────────────
@Composable
fun AddContactDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PH_SURFACE,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "New contact",
                    color = PH_TEXT_PRI, fontSize = 24.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name", fontSize = 18.sp) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, color = PH_TEXT_PRI),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PH_BLUE,
                        unfocusedBorderColor = PH_BORDER,
                        focusedLabelColor = PH_BLUE,
                        unfocusedLabelColor = PH_TEXT_SEC,
                        cursorColor = PH_TEXT_PRI,
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Phone number", fontSize = 18.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, color = PH_TEXT_PRI),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PH_BLUE,
                        unfocusedBorderColor = PH_BORDER,
                        focusedLabelColor = PH_BLUE,
                        unfocusedLabelColor = PH_TEXT_SEC,
                        cursorColor = PH_TEXT_PRI,
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PH_TEXT_SEC)
                    ) { Text("Cancel", fontSize = 18.sp) }

                    Button(
                        onClick = { onConfirm(name, phone) },
                        enabled = name.isNotBlank() && phone.isNotBlank(),
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PH_BLUE)
                    ) { Text("Add", fontSize = 18.sp, color = Color.White) }
                }
            }
        }
    }
}