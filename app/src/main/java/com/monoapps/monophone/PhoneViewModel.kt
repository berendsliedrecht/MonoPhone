package com.monoapps.monophone

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.monoapps.monophone.data.ContactRow
import com.monoapps.monophone.data.PhoneRepository
import com.monoapps.monophone.data.RecentCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhoneViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PhoneRepository(application.contentResolver)
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var hasPermissions by mutableStateOf(allGranted())
        private set

    var contacts by mutableStateOf<List<ContactRow>>(emptyList())
        private set

    var recents by mutableStateOf<List<RecentCall>>(emptyList())
        private set

    /** User-entered voicemail number, used when the SIM does not provide one. */
    var voicemailNumber by mutableStateOf(prefs.getString("voicemail_number", "") ?: "")
        private set

    fun saveVoicemailNumber(number: String) {
        voicemailNumber = number.trim()
        prefs.edit().putString("voicemail_number", voicemailNumber).apply()
    }

    /** Carrier message-waiting indicator: true while voicemail is waiting. */
    var voicemailWaiting by mutableStateOf(false)
        private set

    // PhoneStateListener is deprecated, but its TelephonyCallback replacement
    // needs API 31 and minSdk is 28; the old listener still works everywhere.
    @Suppress("DEPRECATION")
    private val mwiListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onMessageWaitingIndicatorChanged(mwi: Boolean) {
            voicemailWaiting = mwi
        }
    }
    private var listeningMwi = false

    private val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) = refresh()
    }
    private var observing = false

    init {
        if (hasPermissions) start()
    }

    private fun allGranted(): Boolean {
        val app = getApplication<Application>()
        return listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG).all {
            ContextCompat.checkSelfPermission(app, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun onPermissionsGranted() {
        hasPermissions = allGranted()
        if (hasPermissions) start()
    }

    private fun start() {
        if (!observing) {
            observing = true
            val resolver = getApplication<Application>().contentResolver
            resolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
            resolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
        }
        startMwi()
        refresh()
    }

    @Suppress("DEPRECATION")
    private fun startMwi() {
        val app = getApplication<Application>()
        val granted = ContextCompat.checkSelfPermission(app, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
        if (listeningMwi || !granted) return
        listeningMwi = true
        app.getSystemService(TelephonyManager::class.java)
            ?.listen(mwiListener, PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR)
    }

    fun refresh() {
        if (!hasPermissions) return
        viewModelScope.launch(Dispatchers.IO) {
            val contactRows = repo.contacts()
            val recentRows = repo.recentCalls()
            withContext(Dispatchers.Main) {
                contacts = contactRows
                recents = recentRows
            }
        }
    }

    suspend fun primaryNumber(contactId: Long): String? =
        withContext(Dispatchers.IO) { repo.primaryNumber(contactId) }

    @Suppress("DEPRECATION")
    override fun onCleared() {
        val app = getApplication<Application>()
        if (observing) app.contentResolver.unregisterContentObserver(observer)
        if (listeningMwi) {
            app.getSystemService(TelephonyManager::class.java)
                ?.listen(mwiListener, PhoneStateListener.LISTEN_NONE)
        }
    }
}
