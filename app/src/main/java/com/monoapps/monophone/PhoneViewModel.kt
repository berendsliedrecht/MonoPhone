package com.monoapps.monophone

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.provider.CallLog
import android.provider.ContactsContract
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

    var hasPermissions by mutableStateOf(allGranted())
        private set

    var contacts by mutableStateOf<List<ContactRow>>(emptyList())
        private set

    var recents by mutableStateOf<List<RecentCall>>(emptyList())
        private set

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
        refresh()
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

    override fun onCleared() {
        if (observing) getApplication<Application>().contentResolver.unregisterContentObserver(observer)
    }
}
