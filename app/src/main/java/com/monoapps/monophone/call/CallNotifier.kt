package com.monoapps.monophone.call

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import com.monoapps.monophone.MainActivity
import com.monoapps.monophone.R

/** Builds and posts the incoming, ongoing, and missed call notifications. */
object CallNotifier {

    private const val CHANNEL_INCOMING = "incoming_calls"
    private const val CHANNEL_ONGOING = "ongoing_calls"
    private const val CHANNEL_MISSED = "missed_calls"
    private const val NOTIFICATION_CALL = 1
    private const val NOTIFICATION_MISSED = 2

    fun showIncoming(context: Context, call: Call) {
        val number = numberOf(call)
        val open = InCallActivity.intent(context)
        val answer = InCallActivity.intent(context).putExtra(InCallActivity.EXTRA_ANSWER, true)
        val notification = Notification.Builder(context, CHANNEL_INCOMING)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle(displayName(context, number))
            .setContentText("Incoming call")
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(activityIntent(context, 0, open))
            // Launches the call screen directly when the phone is locked.
            .setFullScreenIntent(activityIntent(context, 0, open), true)
            .addAction(action(context, "Decline", endCallIntent(context)))
            .addAction(action(context, "Answer", activityIntent(context, 1, answer)))
            .build()
        notify(context, NOTIFICATION_CALL, notification)
    }

    fun showOngoing(context: Context, call: Call) {
        val number = numberOf(call)
        val notification = Notification.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle(displayName(context, number))
            .setContentText("Ongoing call")
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(activityIntent(context, 0, InCallActivity.intent(context)))
            .addAction(action(context, "Hang up", endCallIntent(context)))
            .build()
        notify(context, NOTIFICATION_CALL, notification)
    }

    fun cancelCall(context: Context) = manager(context).cancel(NOTIFICATION_CALL)

    fun showMissed(context: Context, count: Int, number: String?) {
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_RECENTS)
        val notification = Notification.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_call_missed)
            .setContentTitle(if (count == 1) "Missed call" else "$count missed calls")
            .setContentText(if (count == 1) displayName(context, number.orEmpty()) else "")
            .setCategory(Notification.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(activityIntent(context, 2, open))
            .build()
        notify(context, NOTIFICATION_MISSED, notification)
    }

    fun cancelMissed(context: Context) = manager(context).cancel(NOTIFICATION_MISSED)

    /**
     * Contact name for a number, falling back to the number itself. A single
     * indexed PhoneLookup row; cheap enough for the main-thread callers here.
     */
    fun displayName(context: Context, number: String): String {
        if (number.isBlank()) return "Unknown"
        val granted = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return number
        context.contentResolver.query(
            Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)
            ),
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { c -> if (c.moveToFirst()) return c.getString(0) ?: number }
        return number
    }

    private fun endCallIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_END),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun activityIntent(context: Context, request: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(
            context, request, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun action(context: Context, title: String, intent: PendingIntent) =
        Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_call), title, intent
        ).build()

    private fun notify(context: Context, id: Int, notification: Notification) {
        val denied = Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (!denied) manager(context).notify(id, notification)
    }

    private fun manager(context: Context): NotificationManager {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_INCOMING, "Incoming calls", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    // Telecom already plays the ringtone; no second sound.
                    setSound(null, null)
                },
                NotificationChannel(
                    CHANNEL_ONGOING, "Ongoing calls", NotificationManager.IMPORTANCE_LOW
                ),
                NotificationChannel(
                    CHANNEL_MISSED, "Missed calls", NotificationManager.IMPORTANCE_DEFAULT
                ),
            )
        )
        return manager
    }
}
