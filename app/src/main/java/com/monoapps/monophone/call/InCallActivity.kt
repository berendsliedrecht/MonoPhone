package com.monoapps.monophone.call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monoapps.monophone.R
import com.monoapps.monophone.ui.calmClickable
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        // Fresh launches only; a replayed intent must not answer a later call.
        if (savedInstanceState == null) maybeAnswer(intent)
        setContent { ThemeMMD { InCallScreen(onFinished = { finish() }) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        maybeAnswer(intent)
    }

    /** The notification's Answer action routes through this activity. */
    private fun maybeAnswer(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_ANSWER, false) == true) {
            OngoingCall.call.value?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }
    }

    companion object {
        const val EXTRA_ANSWER = "answer"

        fun intent(context: Context): Intent =
            Intent(context, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

private val DTMF_KEYS = listOf(
    listOf('1', '2', '3'),
    listOf('4', '5', '6'),
    listOf('7', '8', '9'),
    listOf('*', '0', '#'),
)

@Composable
private fun InCallScreen(onFinished: () -> Unit) {
    val call by OngoingCall.call.collectAsState()
    val callState by OngoingCall.callState.collectAsState()
    val audio by OngoingCall.audioState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val current = call
    LaunchedEffect(current) { if (current == null) onFinished() }
    if (current == null) return

    // Swallow back while ringing; an accidental press must not hide the call.
    BackHandler(enabled = callState == Call.STATE_RINGING) {}

    val number = numberOf(current)
    val name by produceState(number, number) {
        value = withContext(Dispatchers.IO) { CallNotifier.displayName(context, number) }
    }

    var showKeypad by remember { mutableStateOf(false) }
    var dtmf by remember { mutableStateOf("") }

    // One small text repaint per second; acceptable on e-ink.
    var duration by remember { mutableStateOf("") }
    LaunchedEffect(callState, current) {
        if (callState != Call.STATE_ACTIVE) return@LaunchedEffect
        while (true) {
            val connected = current.details.connectTimeMillis
            if (connected > 0) {
                val secs = (System.currentTimeMillis() - connected) / 1000
                duration = if (secs >= 3600) {
                    "%d:%02d:%02d".format(secs / 3600, secs % 3600 / 60, secs % 60)
                } else {
                    "%d:%02d".format(secs / 60, secs % 60)
                }
            }
            delay(1000)
        }
    }

    val status = when (callState) {
        Call.STATE_RINGING -> "Incoming call"
        Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_NEW -> "Calling…"
        Call.STATE_ACTIVE -> duration.ifEmpty { "Connected" }
        Call.STATE_HOLDING -> "On hold"
        Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> "Call ended"
        else -> ""
    }

    val keypadOpen = showKeypad && callState == Call.STATE_ACTIVE

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        TextMMD(status, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(12.dp))
        TextMMD(
            name,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        // The typed DTMF digits take the number's line while the keypad is up.
        when {
            keypadOpen -> {
                Spacer(modifier = Modifier.height(4.dp))
                TextMMD(dtmf.ifEmpty { " " }, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            name != number && number.isNotBlank() -> {
                Spacer(modifier = Modifier.height(4.dp))
                TextMMD(number, fontSize = 20.sp)
            }
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (keypadOpen) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DTMF_KEYS.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                            row.forEach { key ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(width = 72.dp, height = 50.dp)
                                        .calmClickable {
                                            dtmf += key
                                            current.playDtmfTone(key)
                                            scope.launch {
                                                delay(120)
                                                current.stopDtmfTone()
                                            }
                                        },
                                ) {
                                    TextMMD(
                                        key.toString(),
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (callState == Call.STATE_RINGING) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RoundAction(
                    icon = { CallEndIcon() },
                    label = "Decline",
                    onClick = { current.reject(false, null) },
                )
                RoundAction(
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_call),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp),
                        )
                    },
                    label = "Answer",
                    onClick = { current.answer(VideoProfile.STATE_AUDIO_ONLY) },
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        } else {
            val muted = audio?.isMuted == true
            val speaker = audio?.route == CallAudioState.ROUTE_SPEAKER
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CallToggle(Icons.Outlined.MicOff, "Mute", active = muted) {
                    OngoingCall.service?.setMuted(!muted)
                }
                CallToggle(Icons.Outlined.VolumeUp, "Speaker", active = speaker) {
                    OngoingCall.service?.setAudioRoute(
                        if (speaker) CallAudioState.ROUTE_WIRED_OR_EARPIECE
                        else CallAudioState.ROUTE_SPEAKER
                    )
                }
                CallToggle(Icons.Outlined.Dialpad, "Keypad", active = showKeypad) {
                    showKeypad = !showKeypad
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // A wide bar instead of another circle: easy to hit and leaves
            // the vertical room the keypad needs on the small screen.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                    .calmClickable { current.disconnect() },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CallEndIcon()
                    Spacer(modifier = Modifier.width(10.dp))
                    TextMMD("End call", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CallEndIcon() {
    Icon(
        Icons.Outlined.CallEnd,
        contentDescription = null,
        tint = Color.Black,
        modifier = Modifier.size(36.dp),
    )
}

/** Outlined squircle button with a label, per the no-infill rule. */
@Composable
private fun RoundAction(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(2.dp, Color.Black, RoundedCornerShape(18.dp))
                .calmClickable(onClick),
        ) { icon() }
        Spacer(modifier = Modifier.height(6.dp))
        TextMMD(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** Active state reads as a thicker ring and bold label; never infilled. */
@Composable
private fun CallToggle(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(if (active) 4.dp else 1.dp, Color.Black, RoundedCornerShape(14.dp))
                .calmClickable(onClick),
        ) {
            Icon(icon, contentDescription = label, tint = Color.Black, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        TextMMD(
            label,
            fontSize = 16.sp,
            fontWeight = if (active) FontWeight.Black else FontWeight.Medium,
        )
    }
}
