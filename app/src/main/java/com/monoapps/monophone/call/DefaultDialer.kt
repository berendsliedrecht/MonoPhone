package com.monoapps.monophone.call

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager

fun isDefaultDialer(context: Context): Boolean =
    context.getSystemService(TelecomManager::class.java)
        ?.defaultDialerPackage == context.packageName

/** System dialog asking to make MonoPhone the phone app, or null if it already is. */
fun defaultDialerRequest(context: Context): Intent? {
    if (isDefaultDialer(context)) return null
    return if (Build.VERSION.SDK_INT >= 29) {
        val roles = context.getSystemService(RoleManager::class.java) ?: return null
        if (!roles.isRoleAvailable(RoleManager.ROLE_DIALER)) return null
        roles.createRequestRoleIntent(RoleManager.ROLE_DIALER)
    } else {
        @Suppress("DEPRECATION")
        Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
            .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
    }
}
