package com.monoapps.monophone.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.PhoneLookup

data class ContactRow(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val starred: Boolean,
)

data class RecentCall(
    val id: Long,
    val number: String,
    val cachedName: String?,
    val type: Int,
    val dateMillis: Long,
)

class PhoneRepository(private val resolver: ContentResolver) {

    fun contacts(): List<ContactRow> {
        val rows = mutableListOf<ContactRow>()
        resolver.query(
            Contacts.CONTENT_URI,
            arrayOf(Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME_PRIMARY, Contacts.STARRED),
            null,
            null,
            Contacts.SORT_KEY_PRIMARY,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(2) ?: continue
                rows += ContactRow(c.getLong(0), c.getString(1), name, c.getInt(3) == 1)
            }
        }
        return rows
    }

    /** First phone number of a contact, or null if it has none. */
    fun primaryNumber(contactId: Long): String? {
        resolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.NUMBER),
            "${Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null,
        )?.use { c -> if (c.moveToFirst()) return c.getString(0) }
        return null
    }

    fun recentCalls(limit: Int = 100): List<RecentCall> {
        val rows = mutableListOf<RecentCall>()
        resolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
            ),
            null,
            null,
            "${CallLog.Calls.DATE} DESC",
        )?.use { c ->
            val nameCache = mutableMapOf<String, String?>()
            while (c.moveToNext() && rows.size < limit) {
                val number = c.getString(1).orEmpty()
                val cached = c.getString(2)?.takeIf { it.isNotBlank() }
                rows += RecentCall(
                    id = c.getLong(0),
                    number = number,
                    cachedName = cached ?: nameCache.getOrPut(number) { lookupName(number) },
                    type = c.getInt(3),
                    dateMillis = c.getLong(4),
                )
            }
        }
        return rows
    }

    private fun lookupName(number: String): String? {
        if (number.isBlank()) return null
        resolver.query(
            Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
            arrayOf(PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { c -> if (c.moveToFirst()) return c.getString(0) }
        return null
    }
}
