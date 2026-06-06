package com.elderlyos.vieuxos

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

class VieuxInCallService : InCallService() {

    companion object {
        // Référence statique à l'appel actif, accessible depuis InCallActivity
        var currentCall: Call? = null
            private set
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            when (state) {
                Call.STATE_DISCONNECTED -> {
                    // L'appel est terminé : ferme l'activité si elle est ouverte
                    currentCall = null
                    val intent = Intent(this@VieuxInCallService, InCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra("action", "finish")
                    }
                    startActivity(intent)
                }
                else -> {
                    // Met à jour l'état dans l'activité si besoin
                    InCallActivity.updateCallState(state)
                }
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        call.registerCallback(callCallback)

        // Lance l'activité d'appel custom
        val intent = Intent(this, InCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("caller_name", call.details?.handle?.schemeSpecificPart ?: "Inconnu")
            putExtra("call_state", call.state)
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        currentCall = null
    }
}