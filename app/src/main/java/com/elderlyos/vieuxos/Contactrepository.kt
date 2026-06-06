package com.elderlyos.vieuxos

import android.content.Context
import androidx.compose.runtime.mutableStateListOf

/**
 * Singleton repository for contacts.
 * Shared across PhoneActivity and SOSActivity.
 * Persists contacts to SharedPreferences as a simple CSV.
 */
object ContactRepository {

    private const val PREFS_NAME = "contacts_prefs"
    private const val KEY_CONTACTS = "contacts_list"

    val contacts = mutableStateListOf<Contact>()

    private val defaults = listOf(
        Contact("Mum",         "+33 6 12 34 56 78"),
        Contact("Dad",         "+33 6 98 76 54 32"),
        Contact("Marie",       "+33 6 11 22 33 44"),
        Contact("Pierre",      "+33 6 55 66 77 88"),
        Contact("Doctor Brun", "+33 1 42 00 11 22"),
        Contact("Nathalie",    "+33 6 44 55 66 77"),
        Contact("Jean-Claude", "+33 6 33 44 55 66"),
        Contact("Pharmacy",    "+33 1 45 67 89 00"),
    )

    /** Call once in Application.onCreate() or in each Activity before first use. */
    fun init(context: Context) {
        if (contacts.isNotEmpty()) return   // already loaded

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_CONTACTS, null)

        if (saved.isNullOrBlank()) {
            contacts.addAll(defaults)
            persist(context)
        } else {
            contacts.addAll(deserialize(saved))
        }
    }

    fun add(context: Context, contact: Contact) {
        contacts.add(contact)
        persist(context)
    }

    fun remove(context: Context, contact: Contact) {
        contacts.remove(contact)
        persist(context)
    }

    // ── Serialization ─────────────────────────────────────────────────────────
    // Format: one contact per line → "Name||Phone"

    private fun persist(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CONTACTS, serialize(contacts)).apply()
    }

    private fun serialize(list: List<Contact>): String =
        list.joinToString("\n") { "${it.name}||${it.phone}" }

    private fun deserialize(raw: String): List<Contact> =
        raw.lines()
            .filter { it.contains("||") }
            .map {
                val parts = it.split("||", limit = 2)
                Contact(parts[0], parts[1])
            }
}