package com.elderlyos.vieuxos

/**
 * Shared data classes used across activities.
 * Single source of truth — do NOT redeclare Contact in other files.
 */
data class Contact(val name: String, val phone: String)