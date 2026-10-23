package com.monoapps.monophone.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.Call

/** Handles the Decline / Hang up notification actions. */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_END) return
        val call = OngoingCall.call.value ?: return
        if (stateOf(call) == Call.STATE_RINGING) call.reject(false, null) else call.disconnect()
    }

    companion object {
        const val ACTION_END = "com.monoapps.monophone.END_CALL"
    }
}
