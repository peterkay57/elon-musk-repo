package com.example.data

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class DialerRepository(private val context: Context) {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _callLogs = MutableStateFlow<List<CallRecord>>(emptyList())
    val callLogs: StateFlow<List<CallRecord>> = _callLogs.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        refreshContacts()
        refreshCallLogs()
    }

    fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCallPhonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun refreshContacts() {
        val loaded = mutableListOf<Contact>()
        if (hasContactPermission()) {
            try {
                val resolver: ContentResolver = context.contentResolver
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.STARRED,
                    ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
                )
                val cursor: Cursor? = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )

                cursor?.use {
                    val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val starredCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                    val lookupCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)

                    var colorIdx = 0
                    while (it.moveToNext()) {
                        val id = if (idCol >= 0) it.getString(idCol) else UUID.randomUUID().toString()
                        val name = if (nameCol >= 0) it.getString(nameCol) ?: "Unknown" else "Unknown"
                        val number = if (numCol >= 0) it.getString(numCol) ?: "" else ""
                        val isStarred = if (starredCol >= 0) it.getInt(starredCol) == 1 else false
                        val lookup = if (lookupCol >= 0) it.getString(lookupCol) else null

                        if (number.isNotBlank() && loaded.none { existing -> existing.phoneNumber == number }) {
                            loaded.add(
                                Contact(
                                    id = id,
                                    name = name,
                                    phoneNumber = number,
                                    isFavorite = isStarred,
                                    avatarColorIndex = (colorIdx++) % 8,
                                    lookupKey = lookup
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Graceful fallback on permission or query failure
            }
        }

        if (loaded.isEmpty()) {
            // Provide realistic sample contacts for rich out-of-the-box demo in emulator
            _contacts.value = getSampleContacts()
        } else {
            _contacts.value = loaded
        }
    }

    fun refreshCallLogs() {
        val loaded = mutableListOf<CallRecord>()
        if (hasCallLogPermission()) {
            try {
                val resolver: ContentResolver = context.contentResolver
                val projection = arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                )
                val cursor: Cursor? = resolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )

                cursor?.use {
                    val idCol = it.getColumnIndex(CallLog.Calls._ID)
                    val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val numCol = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
                    val dateCol = it.getColumnIndex(CallLog.Calls.DATE)
                    val durCol = it.getColumnIndex(CallLog.Calls.DURATION)

                    while (it.moveToNext()) {
                        val id = if (idCol >= 0) it.getString(idCol) else UUID.randomUUID().toString()
                        val name = if (nameCol >= 0) it.getString(nameCol) else null
                        val number = if (numCol >= 0) it.getString(numCol) ?: "" else ""
                        val rawType = if (typeCol >= 0) it.getInt(typeCol) else CallLog.Calls.INCOMING_TYPE
                        val date = if (dateCol >= 0) it.getLong(dateCol) else System.currentTimeMillis()
                        val duration = if (durCol >= 0) it.getLong(durCol) else 0L

                        val type = when (rawType) {
                            CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                            CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                            else -> CallType.INCOMING
                        }

                        loaded.add(
                            CallRecord(
                                id = id,
                                name = name,
                                phoneNumber = number,
                                type = type,
                                timestamp = date,
                                durationSeconds = duration
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Graceful fallback
            }
        }

        if (loaded.isEmpty()) {
            _callLogs.value = getSampleCallLogs()
        } else {
            _callLogs.value = loaded
        }
    }

    fun toggleFavorite(contactId: String) {
        val currentList = _contacts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == contactId }
        if (index != -1) {
            val contact = currentList[index]
            val updated = contact.copy(isFavorite = !contact.isFavorite)
            currentList[index] = updated
            _contacts.value = currentList
        }
    }

    fun addContact(contact: Contact) {
        val currentList = _contacts.value.toMutableList()
        currentList.add(0, contact)
        _contacts.value = currentList.sortedBy { it.name }

        // Attempt system insertion if WRITE_CONTACTS permission granted
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val ops = ArrayList<ContentProviderOperation>()
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build()
                )
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                        .build()
                )
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phoneNumber)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build()
                )
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            } catch (e: Exception) {
                // Saved in app state
            }
        }
    }

    fun updateContact(updated: Contact) {
        val currentList = _contacts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            currentList[index] = updated
            _contacts.value = currentList.sortedBy { it.name }
        }
    }

    fun deleteContact(contactId: String) {
        val currentList = _contacts.value.filter { it.id != contactId }
        _contacts.value = currentList
    }

    fun addCallRecord(phoneNumber: String, name: String?, type: CallType, durationSeconds: Long) {
        val newRecord = CallRecord(
            id = UUID.randomUUID().toString(),
            name = name ?: findContactByNumber(phoneNumber)?.name,
            phoneNumber = phoneNumber,
            type = type,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds
        )
        val current = _callLogs.value.toMutableList()
        current.add(0, newRecord)
        _callLogs.value = current

        // Attempt system insertion if WRITE_CALL_LOG is granted
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            try {
                val values = android.content.ContentValues().apply {
                    put(CallLog.Calls.NUMBER, phoneNumber)
                    put(CallLog.Calls.DATE, System.currentTimeMillis())
                    put(CallLog.Calls.DURATION, durationSeconds)
                    put(
                        CallLog.Calls.TYPE,
                        when (type) {
                            CallType.INCOMING -> CallLog.Calls.INCOMING_TYPE
                            CallType.OUTGOING -> CallLog.Calls.OUTGOING_TYPE
                            CallType.MISSED -> CallLog.Calls.MISSED_TYPE
                            CallType.REJECTED -> CallLog.Calls.REJECTED_TYPE
                        }
                    )
                    put(CallLog.Calls.NEW, 1)
                    if (name != null) put(CallLog.Calls.CACHED_NAME, name)
                }
                context.contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
            } catch (e: Exception) {
                // Logged in repository state
            }
        }
    }

    fun deleteCallRecord(callId: String) {
        val current = _callLogs.value.filter { it.id != callId }
        _callLogs.value = current
    }

    fun clearAllCallLogs() {
        _callLogs.value = emptyList()
    }

    fun findContactByNumber(number: String): Contact? {
        val cleanNumber = normalizeNumber(number)
        return _contacts.value.firstOrNull {
            normalizeNumber(it.phoneNumber).endsWith(cleanNumber) ||
                    cleanNumber.endsWith(normalizeNumber(it.phoneNumber))
        }
    }

    // T9 Search Matching
    fun searchT9(query: String): List<Contact> {
        if (query.isBlank()) return emptyList()
        val cleanDigits = query.filter { it.isDigit() }
        if (cleanDigits.isBlank()) {
            // Text search
            return _contacts.value.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.phoneNumber.contains(query)
            }
        }

        return _contacts.value.filter { contact ->
            // Match phone digits directly
            val numDigits = contact.phoneNumber.filter { it.isDigit() }
            if (numDigits.contains(cleanDigits)) return@filter true

            // Match T9 text conversion
            val nameT9 = nameToT9(contact.name)
            nameT9.contains(cleanDigits)
        }
    }

    private fun nameToT9(name: String): String {
        return buildString {
            for (ch in name.lowercase()) {
                val digit = when (ch) {
                    'a', 'b', 'c' -> '2'
                    'd', 'e', 'f' -> '3'
                    'g', 'h', 'i' -> '4'
                    'j', 'k', 'l' -> '5'
                    'm', 'n', 'o' -> '6'
                    'p', 'q', 'r', 's' -> '7'
                    't', 'u', 'v' -> '8'
                    'w', 'x', 'y', 'z' -> '9'
                    '0' -> '0'
                    '1' -> '1'
                    else -> ' '
                }
                append(digit)
            }
        }
    }

    private fun normalizeNumber(num: String): String {
        return num.filter { it.isDigit() }
    }

    private fun getSampleContacts(): List<Contact> {
        return listOf(
            Contact("1", "Alexander Wright", "+1 (555) 234-8901", "alex.wright@example.com", "Mobile", true, 0),
            Contact("2", "Bella Thorne", "+1 (555) 345-6789", "bella.t@example.com", "Mobile", true, 1),
            Contact("3", "Carlos Mendes", "+1 (555) 987-6543", "carlos.m@example.com", "Work", false, 2),
            Contact("4", "Doctor Evans", "+1 (555) 456-7890", "office@drevans.clinic", "Work", true, 3),
            Contact("5", "Elena Rostova", "+1 (555) 678-9012", "elena.rost@example.com", "Mobile", true, 4),
            Contact("6", "Home Delivery", "+1 (555) 123-4567", "orders@localfresh.com", "Other", false, 5),
            Contact("7", "Liam O'Connor", "+1 (555) 890-1234", "liam.oc@example.com", "Mobile", false, 6),
            Contact("8", "Maya Lin", "+1 (555) 567-8901", "maya.lin@design.studio", "Work", true, 7),
            Contact("9", "Mom", "+1 (555) 234-5678", "mom@family.net", "Family", true, 0),
            Contact("10", "Noah Patel", "+1 (555) 789-0123", "noah.patel@techlab.io", "Mobile", false, 1),
            Contact("11", "Sophia Martinez", "+1 (555) 890-5678", "sophia.m@example.com", "Mobile", true, 2),
            Contact("12", "William Turner", "+1 (555) 654-3210", "william.t@example.com", "Work", false, 3)
        )
    }

    private fun getSampleCallLogs(): List<CallRecord> {
        val now = System.currentTimeMillis()
        val hour = 3600 * 1000L
        val min = 60 * 1000L

        return listOf(
            CallRecord("c1", "Mom", "+1 (555) 234-5678", CallType.INCOMING, now - 15 * min, 184),
            CallRecord("c2", "Alexander Wright", "+1 (555) 234-8901", CallType.OUTGOING, now - 52 * min, 45),
            CallRecord("c3", null, "+1 (555) 902-1144", CallType.MISSED, now - 2 * hour, 0),
            CallRecord("c4", "Doctor Evans", "+1 (555) 456-7890", CallType.INCOMING, now - 5 * hour, 312),
            CallRecord("c5", "Bella Thorne", "+1 (555) 345-6789", CallType.OUTGOING, now - 18 * hour, 92),
            CallRecord("c6", null, "+1 (800) 444-2200", CallType.REJECTED, now - 26 * hour, 0),
            CallRecord("c7", "Elena Rostova", "+1 (555) 678-9012", CallType.MISSED, now - 32 * hour, 0),
            CallRecord("c8", "Maya Lin", "+1 (555) 567-8901", CallType.INCOMING, now - 48 * hour, 540)
        )
    }
}
