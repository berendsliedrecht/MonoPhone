package com.monoapps.monophone.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager

/**
 * Telecom delegates missed-call notifications to the default dialer through
 * this protected broadcast; count 0 means the user has seen the call log.
 */
class MissedCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION) return
        val count = intent.getIntExtra(TelecomManager.EXTRA_NOTIFICATION_COUNT, 0)
        val number = intent.getStringExtra(TelecomManager.EXTRA_NOTIFICATION_PHONE_NUMBER)
        if (count == 0) {
            CallNotifier.cancelMissed(context)
        } else {
            CallNotifier.showMissed(context, count, number)
        }
    }
}
