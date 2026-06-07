package com.elderlyos.vieuxos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

class FamilySetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FamilySetupScreen() }
    }
}

@Composable
fun FamilySetupScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("vieuxos_prefs", Context.MODE_PRIVATE)
    val s = getStrings(context)

    var pinEntered by remember { mutableStateOf(false) }
    var pinInput  by remember { mutableStateOf("") }
    var pinError  by remember { mutableStateOf(false) }
    val correctPin = prefs.getString("setup_pin", "1234") ?: "1234"

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).imePadding()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (!pinEntered) {
                PinScreen(
                    s = s,
                    pinInput = pinInput,
                    pinError = pinError,
                    onPinChange = { pinInput = it },
                    onUnlock = {
                        if (pinInput == correctPin) { pinEntered = true; pinError = false }
                        else { pinError = true; pinInput = "" }
                    }
                )
            } else {
                SetupContent(context = context, prefs = prefs)
            }
        }
        BottomNavBar()
    }
}

@Composable
fun PinScreen(
    s: AppStrings,
    pinInput: String,
    pinError: Boolean,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(s.familySetup, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(s.enterPin, color = Color(0xFFAAAAAA), fontSize = 16.sp)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = pinInput, onValueChange = onPinChange,
            label = { Text("PIN", color = Color(0xFFAAAAAA)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF1565C0), unfocusedBorderColor = Color(0xFF555555)
            )
        )
        if (pinError) {
            Spacer(Modifier.height(8.dp))
            Text(s.wrongPin, color = Color.Red, fontSize = 14.sp)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onUnlock,
            modifier = Modifier.fillMaxWidth().height(70.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
            shape = RoundedCornerShape(16.dp)
        ) { Text(s.unlock, fontSize = 22.sp, color = Color.White) }
    }
}

@Composable
fun SetupContent(context: Context, prefs: android.content.SharedPreferences) {
    // Reload strings whenever language changes
    var langKey by remember { mutableStateOf(prefs.getString("language", "en") ?: "en") }
    val s = remember(langKey) { getStrings(context) }

    var elderlyName  by remember { mutableStateOf(prefs.getString("elderly_name",  "") ?: "") }
    var homeAddress  by remember { mutableStateOf(prefs.getString("home_address",  "") ?: "") }
    var newPin       by remember { mutableStateOf("") }
    var meetLink     by remember { mutableStateOf(prefs.getString("meet_link", "") ?: "") }
    var saved        by remember { mutableStateOf(false) }

    val contacts    = remember { mutableStateListOf<Contact>().also { it.addAll(loadContacts(context)) } }
    val medications = remember { mutableStateListOf<Medication>().also { it.addAll(loadMedications(context)) } }
    val events      = remember { mutableStateListOf<CalEvent>().also { it.addAll(loadEvents(context)) } }

    var showAddContact  by remember { mutableStateOf(false) }
    var showAddMed      by remember { mutableStateOf(false) }
    var showAddEvent    by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(s.setupTitle, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)

        // ── Language picker ───────────────────────────────────────────────────
        SectionHeader(s.language, Icons.Filled.Language)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("en" to "🇬🇧 EN", "fr" to "🇫🇷 FR", "nl" to "🇳🇱 NL").forEach { (code, label) ->
                val selected = langKey == code
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .background(
                            if (selected) Color(0xFF1565C0) else Color(0xFF1E1E1E),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) Color(0xFF4a9eff) else Color(0xFF444444),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            langKey = code
                            prefs.edit().putString("language", code).apply()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (selected) Color.White else Color(0xFFAAAAAA),
                        fontSize = 18.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // ── General ───────────────────────────────────────────────────────────
        SectionHeader(s.general, Icons.Filled.Settings)
        SetupField(s.personName,  elderlyName,  "e.g. Georges")        { elderlyName = it }
        SetupField(s.homeAddress, homeAddress,  "e.g. 12 Rue de Paris") { homeAddress = it }
        SetupField(s.changePin,   newPin, "Leave empty to keep", isPassword = true) { newPin = it }
        SetupField(s.meetLink,    meetLink, "meet.google.com/xxx-xxxx") { meetLink = it }

        // ── Contacts ──────────────────────────────────────────────────────────
        SectionHeader(s.contacts, Icons.Filled.Phone)
        contacts.forEachIndexed { i, c ->
            ItemRow(primary = c.name, secondary = c.phone) {
                contacts.removeAt(i); saveContacts(context, contacts)
            }
        }
        AddButton(s.addContact) { showAddContact = true }

        // ── Medications ───────────────────────────────────────────────────────
        SectionHeader(s.medications, Icons.Filled.Medication)
        medications.forEachIndexed { i, m ->
            ItemRow(primary = m.name, secondary = m.meal) {
                medications.removeAt(i); saveMedications(context, medications)
            }
        }
        AddButton(s.addMedication) { showAddMed = true }

        // ── Events ────────────────────────────────────────────────────────────
        SectionHeader(s.events, Icons.Filled.CalendarMonth)
        val months = listOf("","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        events.forEachIndexed { i, e ->
            ItemRow(primary = e.title, secondary = "${months.getOrElse(e.month){"?"}} ${e.day}") {
                events.removeAt(i); saveEvents(context, events)
            }
        }
        AddButton(s.addEvent) { showAddEvent = true }

        // ── Wallet ────────────────────────────────────────────────────────────
        SectionHeader("Wallet", Icons.Filled.CreditCard)
        val foundApps = remember { installedPaymentApps(context) }
        var selectedWalletPkg  by remember { mutableStateOf(prefs.getString("wallet_package", "") ?: "") }
        var selectedWalletName by remember { mutableStateOf(prefs.getString("wallet_name",    "") ?: "") }

        if (foundApps.isEmpty()) {
            Text(
                "No payment app found on this device.\nInstall Google Wallet from the Play Store.",
                color = Color(0xFFAAAAAA), fontSize = 15.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            )
        } else {
            // One selectable row per detected payment app
            foundApps.forEach { (pkg, name) ->
                val selected = selectedWalletPkg == pkg
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) Color(0xFF1B5E20) else Color(0xFF1E1E1E),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) Color(0xFF4CAF50) else Color(0xFF444444),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedWalletPkg  = pkg
                            selectedWalletName = name
                            prefs.edit()
                                .putString("wallet_package", pkg)
                                .putString("wallet_name",    name)
                                .apply()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        Icons.Filled.CreditCard,
                        null,
                        tint = if (selected) Color(0xFF4CAF50) else Color(0xFF888888),
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        name,
                        color = if (selected) Color.White else Color(0xFFBBBBBB),
                        fontSize = 18.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Button to open Android's tap-and-pay settings (so family can set it as system default)
        if (selectedWalletPkg.isNotEmpty()) {
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(android.provider.Settings.ACTION_NFC_PAYMENT_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Filled.Nfc, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Set $selectedWalletName as default payment\n(opens Android NFC settings)",
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        // ── Save ──────────────────────────────────────────────────────────────
        HorizontalDivider(color = Color(0xFF333333))
        Button(
            onClick = {
                prefs.edit()
                    .putString("elderly_name", elderlyName)
                    .putString("home_address", homeAddress)
                    .putString("meet_link", meetLink)
                    .apply { if (newPin.isNotEmpty()) putString("setup_pin", newPin) }
                    .apply()
                saveContacts(context, contacts)
                saveMedications(context, medications)
                saveEvents(context, events)
                saved = true
            },
            modifier = Modifier.fillMaxWidth().height(70.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(16.dp)
        ) { Text(s.saveAll, fontSize = 22.sp, color = Color.White) }

        if (saved) {
            Text(s.savedOk, color = Color(0xFF4CAF50), fontSize = 18.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        Spacer(Modifier.height(8.dp))
    }

    // Dialogs
    if (showAddContact) {
        TwoFieldDialog(
            title = s.addContact, label1 = "Name", label2 = "Phone number",
            keyboard2 = KeyboardType.Phone,
            cancelLabel = s.cancel, addLabel = s.add,
            onDismiss = { showAddContact = false },
            onConfirm = { a, b -> contacts.add(Contact(a, b)); saveContacts(context, contacts); showAddContact = false }
        )
    }
    if (showAddMed) {
        MealPickerDialog(
            s = s,
            onDismiss = { showAddMed = false },
            onConfirm = { name, meal -> medications.add(Medication(name, meal)); saveMedications(context, medications); showAddMed = false }
        )
    }
    if (showAddEvent) {
        EventDialog(
            addLabel = s.add, cancelLabel = s.cancel,
            onDismiss = { showAddEvent = false },
            onConfirm = { m, d, t -> events.add(CalEvent(m, d, t)); saveEvents(context, events); showAddEvent = false }
        )
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(icon, null, tint = Color(0xFF4a9eff), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color(0xFF4a9eff), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = Color(0xFF333333), thickness = 0.5.dp)
}

@Composable
fun ItemRow(primary: String, secondary: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(primary, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(secondary, color = Color(0xFF888888), fontSize = 13.sp)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, null, tint = Color(0xFFE53935))
        }
    }
}

@Composable
fun AddButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4a9eff))
    ) {
        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 16.sp)
    }
}

@Composable
fun TwoFieldDialog(
    title: String, label1: String, label2: String,
    keyboard2: KeyboardType = KeyboardType.Text,
    cancelLabel: String = "Cancel", addLabel: String = "Add",
    onDismiss: () -> Unit, onConfirm: (String, String) -> Unit
) {
    var f1 by remember { mutableStateOf("") }
    var f2 by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1E1E1E), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                DarkField(f1, label1) { f1 = it }
                DarkField(f2, label2, keyboard2) { f2 = it }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(cancelLabel, color = Color(0xFF888888))
                    }
                    Button(
                        onClick = { if (f1.isNotBlank() && f2.isNotBlank()) onConfirm(f1.trim(), f2.trim()) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) { Text(addLabel, color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun MealPickerDialog(s: AppStrings, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    // Internal keys stay English so saved data is language-agnostic
    var mealKey by remember { mutableStateOf("Breakfast") }
    val mealOptions = listOf(
        "Breakfast" to s.breakfast,
        "Lunch"     to s.lunch,
        "Dinner"    to s.dinner,
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1E1E1E), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(s.addMedication, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                DarkField(name, "Medication name") { name = it }
                Text("When to take:", color = Color(0xFF888888), fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mealOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = mealKey == key,
                            onClick = { mealKey = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1565C0),
                                selectedLabelColor = Color.White,
                                labelColor = Color(0xFFAAAAAA)
                            )
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(s.cancel, color = Color(0xFF888888))
                    }
                    Button(
                        onClick = { if (name.isNotBlank()) onConfirm(name.trim(), mealKey) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) { Text(s.add, color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun EventDialog(
    addLabel: String = "Add", cancelLabel: String = "Cancel",
    onDismiss: () -> Unit, onConfirm: (Int, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var day   by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1E1E1E), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Add Event", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                DarkField(title, "Event title") { title = it }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DarkField(month, "Month (1-12)", KeyboardType.Number, Modifier.weight(1f)) { month = it }
                    DarkField(day,   "Day",          KeyboardType.Number, Modifier.weight(1f)) { day = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(cancelLabel, color = Color(0xFF888888))
                    }
                    Button(
                        onClick = {
                            val m = month.toIntOrNull(); val d = day.toIntOrNull()
                            if (title.isNotBlank() && m != null && d != null) onConfirm(m, d, title.trim())
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) { Text(addLabel, color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun DarkField(
    value: String, label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        singleLine = true, modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color.White),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF1565C0), unfocusedBorderColor = Color(0xFF444444),
            focusedLabelColor = Color(0xFF4a9eff), unfocusedLabelColor = Color(0xFF888888),
            cursorColor = Color.White
        )
    )
}

@Composable
fun SetupField(
    label: String, value: String, placeholder: String,
    isPassword: Boolean = false, onValueChange: (String) -> Unit
) {
    Column {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF666666)) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF1565C0), unfocusedBorderColor = Color(0xFF555555)
            )
        )
    }
}
