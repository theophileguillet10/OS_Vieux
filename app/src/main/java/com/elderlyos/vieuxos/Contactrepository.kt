package com.elderlyos.vieuxos

import android.content.Context
import androidx.compose.runtime.mutableStateListOf

/**
 * Singleton repository for contacts.
 * Uses the SAME SharedPreferences store as DataStore.kt (vieuxos_prefs / contacts)
 * so that changes in FamilySetupActivity are immediately visible everywhere.
 */
object ContactRepository {

    val contacts = mutableStateListOf<Contact>()

    /** Call in every Activity.onCreate() that shows contacts — always reloads from prefs. */
    fun init(context: Context) {
        contacts.clear()
        contacts.addAll(loadContacts(context))   // from DataStore.kt
    }

    fun add(context: Context, contact: Contact) {
        contacts.add(contact)
        saveContacts(context, contacts)           // from DataStore.kt
    }

    fun remove(context: Context, contact: Contact) {
        contacts.remove(contact)
        saveContacts(context, contacts)           // from DataStore.kt
    }
}
