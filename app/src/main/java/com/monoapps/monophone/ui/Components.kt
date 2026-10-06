package com.monoapps.monophone.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.TelephonyManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/** Hairline dotted divider matching the stock Mudita list style. */
@Composable
fun DashedDivider(modifier: Modifier = Modifier, thickness: Dp = 1.dp) {
    Canvas(modifier = modifier.fillMaxWidth().height(thickness)) {
        drawLine(
            color = Color.Black,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = thickness.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx()), 0f),
        )
    }
}

/** Click without ripple; animations ghost on e-ink. */
fun Modifier.calmClickable(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )
)

/** "First **Last**" styling: every word regular except the last, which is bold. */
fun styledName(displayName: String) = buildAnnotatedString {
    val words = displayName.trim().split(" ")
    if (words.size > 1) append(words.dropLast(1).joinToString(" ") + " ")
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(words.last()) }
}

/** True when CALL_PHONE is already granted; callers request it otherwise. */
fun canCallDirectly(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
        PackageManager.PERMISSION_GRANTED

fun placeCall(context: Context, number: String) {
    startSafely(context, Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
}

/** Dial the voicemail box; telecom resolves the number from the SIM. */
fun placeVoicemailCall(context: Context) {
    startSafely(context, Intent(Intent.ACTION_CALL, Uri.parse("voicemail:")))
}

/** The voicemail number telephony knows, or null when the SIM has none. */
fun systemVoicemailNumber(context: Context): String? = try {
    context.getSystemService(TelephonyManager::class.java)
        ?.voiceMailNumber?.takeIf { it.isNotBlank() }
} catch (e: SecurityException) {
    // READ_PHONE_STATE denied; treat as unknown.
    null
}

/** Start an activity, ignoring the tap when no app can handle the intent. */
fun startSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // No handler installed; do nothing rather than crash.
    }
}
