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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.launch

private val avatarColors = listOf(
    Color(0xFF1B6E2E), Color(0xFF1560BD), Color(0xFF7B1FA2),
    Color(0xFFE65100), Color(0xFF00695C), Color(0xFFAD1457),
    Color(0xFF4527A0), Color(0xFF37474F), Color(0xFF00838F),
    Color(0xFF558B2F),
)

data class Contact(val name: String, val phone: String)

class PhoneActivity : ComponentActivity() {

    private val defaultDialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* résultat ignoré — onResume recheck via DisposableEffect */ }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* géré via checkSelfPermission à l'usage */ }

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

    // Utilise les imports en haut — plus de fully-qualified
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefault = isDefaultDialer(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            .background(Color(0xFF121212))
    ) {
        if (!isDefault) {
            DefaultDialerBanner(onRequestDefaultDialer = onRequestDefaultDialer)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📞  Phone",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        HorizontalDivider(color = Color(0xFF222222), thickness = 0.5.dp)

        LazyColumn(
            state = listState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxWidth().weight(1f)
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
                    color = Color(0xFF1A1A1A), thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF181818))
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))
            ) { Text("▲  Up", fontSize = 18.sp, color = Color.White) }

            Button(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(minOf(contacts.size - 1, listState.firstVisibleItemIndex + visibleCount))
                    }
                },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))
            ) { Text("▼  Down", fontSize = 18.sp, color = Color.White) }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1560BD))
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

@Composable
fun DefaultDialerBanner(onRequestDefaultDialer: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF7B3300))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("⚠️", fontSize = 22.sp)
        Text(
            text = "VieuxOS is not the default phone app.\nTap Activate to use the custom call screen.",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onRequestDefaultDialer,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
        ) { Text("Activate", color = Color.White, fontSize = 14.sp) }
    }
}

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
            modifier = Modifier.size(64.dp).clip(CircleShape)
                .background(avatarColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, color = avatarColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = contact.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = contact.phone, color = Color(0xFF888888), fontSize = 18.sp)
        }
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1B3A1B)),
            contentAlignment = Alignment.Center
        ) { Text(text = "📞", fontSize = 24.sp) }
    }
}

@Composable
fun AddContactDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("New contact", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name", fontSize = 18.sp) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1560BD), unfocusedBorderColor = Color(0xFF444444),
                        focusedLabelColor = Color(0xFF4a9eff), unfocusedLabelColor = Color(0xFF888888),
                        cursorColor = Color.White,
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Phone number", fontSize = 18.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1560BD), unfocusedBorderColor = Color(0xFF444444),
                        focusedLabelColor = Color(0xFF4a9eff), unfocusedLabelColor = Color(0xFF888888),
                        cursorColor = Color.White,
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF888888))
                    ) { Text("Cancel", fontSize = 18.sp) }
                    Button(
                        onClick = { onConfirm(name, phone) },
                        enabled = name.isNotBlank() && phone.isNotBlank(),
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1560BD))
                    ) { Text("Add", fontSize = 18.sp, color = Color.White) }
                }
            }
        }
    }
}