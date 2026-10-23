package com.monoapps.monophone.call

import android.annotation.SuppressLint
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService

/**
 * Bound by telecom while MonoPhone is the default dialer; owns the call
 * notifications and publishes call state to [OngoingCall] for the UI.
 */
class CallService : InCallService() {

    private val calls = mutableListOf<Call>()
    private var rendered: Pair<Call?, Int>? = null

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = publish()
    }

    private var proximityLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        OngoingCall.service = this
        val power = getSystemService(PowerManager::class.java)
        if (power?.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) == true) {
            proximityLock =
                power.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "monophone:call")
        }
    }

    override fun onDestroy() {
        releaseProximityLock()
        OngoingCall.service = null
        // Never leave a dead Call binder for notification actions to poke.
        OngoingCall.call.value = null
        OngoingCall.callState.value = Call.STATE_DISCONNECTED
        OngoingCall.audioState.value = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        calls += call
        call.registerCallback(callback)
        publish()
        // Outgoing call placed from the app, which is in the foreground;
        // incoming calls arrive through the notification's full-screen intent.
        if (stateOf(call) != Call.STATE_RINGING) startActivity(InCallActivity.intent(this))
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callback)
        calls -= call
        publish()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        OngoingCall.audioState.value = audioState
        updateProximityLock()
    }

    /**
     * Screen and touch off while the phone is against the face: in a call
     * with the earpiece, not while ringing or on speaker or a headset.
     */
    @SuppressLint("WakelockTimeout") // Held for the length of the call.
    private fun updateProximityLock() {
        val lock = proximityLock ?: return
        val inCall = OngoingCall.callState.value in
            intArrayOf(Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING)
        val route = OngoingCall.audioState.value?.route ?: CallAudioState.ROUTE_EARPIECE
        if (inCall && route == CallAudioState.ROUTE_EARPIECE) {
            if (!lock.isHeld) lock.acquire()
        } else {
            releaseProximityLock()
        }
    }

    private fun releaseProximityLock() {
        val lock = proximityLock ?: return
        // Keeps the screen off until the face moves away, not the instant
        // the call drops; prevents a cheek tap on whatever is below.
        if (lock.isHeld) lock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
    }

    /**
     * A ringing call takes over the UI; answering it holds the other one.
     * The single call notification always mirrors whichever call that is, so
     * state changes on any call re-render it rather than leaving stale actions.
     */
    private fun publish() {
        val current = calls.firstOrNull { stateOf(it) == Call.STATE_RINGING } ?: calls.firstOrNull()
        val state = current?.let(::stateOf) ?: Call.STATE_DISCONNECTED
        OngoingCall.call.value = current
        OngoingCall.callState.value = state
        updateProximityLock()
        if (rendered == current to state) return
        rendered = current to state
        when {
            current == null || state == Call.STATE_DISCONNECTING || state == Call.STATE_DISCONNECTED ->
                CallNotifier.cancelCall(this)
            // Full-screen intent opens the call screen; telecom rings.
            state == Call.STATE_RINGING -> CallNotifier.showIncoming(this, current)
            else -> CallNotifier.showOngoing(this, current)
        }
    }
}
