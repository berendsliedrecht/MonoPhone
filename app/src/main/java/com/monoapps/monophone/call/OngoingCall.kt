package com.monoapps.monophone.call

import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import kotlinx.coroutines.flow.MutableStateFlow

/** Telecom state shared between [CallService] and the in-call UI. */
object OngoingCall {
    /** The call the UI shows; a ringing call takes precedence. */
    val call = MutableStateFlow<Call?>(null)

    /** Mirrors the current call's state; Call itself is not observable. */
    val callState = MutableStateFlow(Call.STATE_DISCONNECTED)

    val audioState = MutableStateFlow<CallAudioState?>(null)

    /** The bound service, for audio routing; null outside a call. */
    var service: CallService? = null
}

/** Call.getState moved to Call.Details in API 31. */
fun stateOf(call: Call): Int =
    if (Build.VERSION.SDK_INT >= 31) call.details.state
    else @Suppress("DEPRECATION") call.state

fun numberOf(call: Call): String =
    call.details.handle?.schemeSpecificPart.orEmpty()
