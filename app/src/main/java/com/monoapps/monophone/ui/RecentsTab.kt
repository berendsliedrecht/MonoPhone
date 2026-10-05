@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.monoapps.monophone.ui

import android.provider.CallLog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.automirrored.outlined.CallMissed
import androidx.compose.material.icons.automirrored.outlined.CallReceived
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoapps.monophone.data.RecentCall
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecentsTab(
    recents: List<RecentCall>,
    onCall: (String) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBarMMD(
                title = { TextMMD("Recents", fontSize = 26.sp, fontWeight = FontWeight.Bold) },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (recents.isEmpty()) {
                TextMMD(
                    "We couldn't find any recent calls…",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                )
            } else {
                LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
                    items(recents, key = { it.id }) { call ->
                        val name = call.cachedName
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .calmClickable { if (call.number.isNotBlank()) onCall(call.number) },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            ) {
                                Icon(
                                    imageVector = when (call.type) {
                                        CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Outlined.CallReceived
                                        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Outlined.CallMade
                                        CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Outlined.CallMissed
                                        CallLog.Calls.VOICEMAIL_TYPE -> Icons.Outlined.Voicemail
                                        else -> Icons.Outlined.Block
                                    },
                                    contentDescription = callTypeLabel(call.type),
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp),
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    TextMMD(
                                        name ?: call.number.ifBlank { "Unknown" },
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    TextMMD(formatCallDate(call.dateMillis), fontSize = 18.sp)
                                }
                            }
                            DashedDivider(modifier = Modifier.padding(horizontal = 20.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun callTypeLabel(type: Int): String = when (type) {
    CallLog.Calls.INCOMING_TYPE -> "Incoming"
    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
    CallLog.Calls.MISSED_TYPE -> "Missed"
    CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
    else -> "Blocked"
}

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dayFormat = DateTimeFormatter.ofPattern("d MMM")

private fun formatCallDate(millis: Long): String {
    val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val today = LocalDate.now()
    return when (dateTime.toLocalDate()) {
        today -> "Today ${dateTime.format(timeFormat)}"
        today.minusDays(1) -> "Yesterday ${dateTime.format(timeFormat)}"
        else -> "${dateTime.format(dayFormat)} ${dateTime.format(timeFormat)}"
    }
}
