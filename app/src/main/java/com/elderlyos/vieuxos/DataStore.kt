package com.elderlyos.vieuxos

import android.content.Context

private const val PREFS = "vieuxos_prefs"

// ── Contacts ──────────────────────────────────────────────────────────────────

private val defaultContacts = listOf(
    Contact("Maman",        "+33 6 12 34 56 78"),
    Contact("Papa",         "+33 6 98 76 54 32"),
    Contact("Marie",        "+33 6 11 22 33 44"),
    Contact("Pierre",       "+33 6 55 66 77 88"),
    Contact("Docteur Brun", "+33 1 42 00 11 22"),
    Contact("Pharmacie",    "+33 1 45 67 89 00"),
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
    Medication("Metformine 500mg", "Lunch"),
    Medication("Amlodipine 5mg",   "Dinner"),
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
    CalEvent(6,  8,  "Birthday Maman"),
    CalEvent(6,  9,  "Pharmacy"),
    CalEvent(6,  10, "Family lunch"),
    CalEvent(6,  12, "Cardiology checkup"),
    CalEvent(6,  14, "Birthday Pierre"),
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
