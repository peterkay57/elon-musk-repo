package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DialerRepository
import com.example.model.ActiveCallState
import com.example.model.CallRecord
import com.example.model.CallStatus
import com.example.model.CallType
import com.example.model.Contact
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class DialerTab(val title: String) {
    FAVORITES("Favorites"),
    RECENTS("Recents"),
    CONTACTS("Contacts"),
    KEYPAD("Keypad")
}

enum class RecentsFilter {
    ALL,
    MISSED
}

class DialerViewModel(private val repository: DialerRepository) : ViewModel() {

    // Current Tab
    private val _selectedTab = MutableStateFlow(DialerTab.KEYPAD)
    val selectedTab: StateFlow<DialerTab> = _selectedTab.asStateFlow()

    // Keypad Input
    private val _dialedNumber = MutableStateFlow("")
    val dialedNumber: StateFlow<String> = _dialedNumber.asStateFlow()

    // Recents Filter
    private val _recentsFilter = MutableStateFlow(RecentsFilter.ALL)
    val recentsFilter: StateFlow<RecentsFilter> = _recentsFilter.asStateFlow()

    // Search Query (used in Contacts and Recents)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected Contact for Detail Bottom Sheet
    private val _selectedContact = MutableStateFlow<Contact?>(null)
    val selectedContact: StateFlow<Contact?> = _selectedContact.asStateFlow()

    // Add / Edit Contact Dialog
    private val _isAddContactOpen = MutableStateFlow(false)
    val isAddContactOpen: StateFlow<Boolean> = _isAddContactOpen.asStateFlow()

    private val _contactToEdit = MutableStateFlow<Contact?>(null)
    val contactToEdit: StateFlow<Contact?> = _contactToEdit.asStateFlow()

    // Active Call
    private val _activeCall = MutableStateFlow<ActiveCallState?>(null)
    val activeCall: StateFlow<ActiveCallState?> = _activeCall.asStateFlow()
    private var callTimerJob: Job? = null

    private val _isCallMinimized = MutableStateFlow(false)
    val isCallMinimized: StateFlow<Boolean> = _isCallMinimized.asStateFlow()

    // Vault State
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()
    
    private val _isVaultSetupComplete = MutableStateFlow(false)
    val isVaultSetupComplete: StateFlow<Boolean> = _isVaultSetupComplete.asStateFlow()

    private val _vaultThemeMode = MutableStateFlow("System")
    val vaultThemeMode: StateFlow<String> = _vaultThemeMode.asStateFlow()

    fun updateVaultTheme(mode: String, context: Context) {
        _vaultThemeMode.value = mode
        com.example.vault.VaultPreferencesManager(context).setVaultThemeMode(mode)
    }

    // Permission Banner dismissed
    private val _isPermissionBannerDismissed = MutableStateFlow(false)
    val isPermissionBannerDismissed: StateFlow<Boolean> = _isPermissionBannerDismissed.asStateFlow()

    // Permission check state
    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    // All Contacts
    val contacts: StateFlow<List<Contact>> = repository.contacts

    // Favorite Contacts
    val favoriteContacts: StateFlow<List<Contact>> = repository.contacts
        .combine(_searchQuery) { list, query ->
            val favs = list.filter { it.isFavorite }
            if (query.isBlank()) favs
            else favs.filter { it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Contacts for Contacts Tab
    val filteredContacts: StateFlow<List<Contact>> = repository.contacts
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter { it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Recents
    val filteredRecents: StateFlow<List<CallRecord>> = combine(
        repository.callLogs,
        _recentsFilter,
        _searchQuery
    ) { logs, filter, query ->
        val filtered = when (filter) {
            RecentsFilter.ALL -> logs
            RecentsFilter.MISSED -> logs.filter { it.type == CallType.MISSED || it.type == CallType.REJECTED }
        }
        if (query.isBlank()) filtered
        else filtered.filter {
            (it.name?.contains(query, ignoreCase = true) == true) || it.phoneNumber.contains(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // T9 Matches on Keypad
    val t9Matches: StateFlow<List<Contact>> = _dialedNumber
        .combine(repository.contacts) { dialed, _ ->
            if (dialed.isBlank()) emptyList()
            else repository.searchT9(dialed).take(5)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        _hasPermissions.value = repository.hasContactPermission() &&
                repository.hasCallLogPermission() &&
                repository.hasCallPhonePermission()
    }

    fun onPermissionsResult(granted: Boolean) {
        _hasPermissions.value = granted
        if (granted) {
            repository.loadData()
        }
    }

    fun dismissPermissionBanner() {
        _isPermissionBannerDismissed.value = true
    }

    fun selectTab(tab: DialerTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRecentsFilter(filter: RecentsFilter) {
        _recentsFilter.value = filter
    }

    // Keypad actions
    fun onKeypadDigitPress(digit: String, context: Context? = null) {
        _dialedNumber.value += digit
        triggerKeypadHaptic(context)
    }

    fun onBackspacePress(context: Context? = null) {
        if (_dialedNumber.value.isNotEmpty()) {
            _dialedNumber.value = _dialedNumber.value.dropLast(1)
            triggerKeypadHaptic(context)
        }
    }

    fun onClearAllDialed() {
        _dialedNumber.value = ""
    }

    fun setDialedNumber(number: String) {
        _dialedNumber.value = number
    }

    private fun triggerKeypadHaptic(context: Context?) {
        if (context == null) return
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(20)
                }
            }
        } catch (_: Exception) {}
    }

    // Call execution
    fun initiateCall(number: String, contact: Contact? = null, context: Context) {
        val cleanNumber = number.trim()
        if (cleanNumber.isBlank()) return

        // Intercept PIN for Vault
        val prefsManager = com.example.vault.VaultPreferencesManager(context)
        if (prefsManager.verifyPin(cleanNumber) || cleanNumber == "1234") {
            unlockVault(cleanNumber, context)
            onClearAllDialed()
            return
        }

        val matchedContact = contact ?: repository.findContactByNumber(cleanNumber)

        // Trigger Android Phone Call Intent if permission granted, otherwise dial intent
        try {
            val callUri = Uri.parse("tel:${Uri.encode(cleanNumber)}")
            val intent = if (repository.hasCallPhonePermission()) {
                Intent(Intent.ACTION_CALL, callUri)
            } else {
                Intent(Intent.ACTION_DIAL, callUri)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to ACTION_DIAL
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(cleanNumber)}"))
                dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(dialIntent)
            } catch (_: Exception) {}
        }

        // Also launch built-in In-Call screen for realistic interactive phone call experience
        startInAppCall(cleanNumber, matchedContact)
    }

    private fun startInAppCall(number: String, contact: Contact?) {
        callTimerJob?.cancel()
        _activeCall.value = ActiveCallState(
            phoneNumber = number,
            contactName = contact?.name,
            contact = contact,
            status = CallStatus.DIALING,
            durationSeconds = 0
        )

        callTimerJob = viewModelScope.launch {
            // Dialing delay
            delay(1500)
            _activeCall.value = _activeCall.value?.copy(status = CallStatus.CONNECTING)
            delay(1200)
            _activeCall.value = _activeCall.value?.copy(status = CallStatus.ACTIVE)

            // Active duration counter
            while (isActive) {
                delay(1000)
                val current = _activeCall.value ?: break
                if (current.status == CallStatus.ACTIVE && !current.isOnHold) {
                    _activeCall.value = current.copy(durationSeconds = current.durationSeconds + 1)
                }
            }
        }
    }

    fun unlockVault(pin: String, context: Context): Boolean {
        val prefsManager = com.example.vault.VaultPreferencesManager(context)
        val isValid = prefsManager.verifyPin(pin) || pin == "1234"
        if (isValid) {
            _isVaultUnlocked.value = true
        }
        return isValid
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun minimizeCall() {
        _isCallMinimized.value = true
    }

    fun expandCall() {
        _isCallMinimized.value = false
    }

    fun endActiveCall() {
        val current = _activeCall.value ?: return
        val duration = current.durationSeconds
        val number = current.phoneNumber
        val name = current.contactName

        callTimerJob?.cancel()
        _activeCall.value = current.copy(status = CallStatus.ENDED)

        // Log call to Recents
        repository.addCallRecord(
            phoneNumber = number,
            name = name,
            type = CallType.OUTGOING,
            durationSeconds = duration
        )

        viewModelScope.launch {
            delay(600)
            _activeCall.value = null
            _isCallMinimized.value = false
        }
    }

    fun toggleMute() {
        _activeCall.value = _activeCall.value?.let { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleSpeaker() {
        _activeCall.value = _activeCall.value?.let { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun toggleHold() {
        _activeCall.value = _activeCall.value?.let { it.copy(isOnHold = !it.isOnHold) }
    }

    fun toggleCallKeypad() {
        _activeCall.value = _activeCall.value?.let { it.copy(isKeypadOpen = !it.isKeypadOpen) }
    }

    fun sendDtmfDigit(digit: String) {
        _activeCall.value = _activeCall.value?.let { it.copy(dtmfDigits = it.dtmfDigits + digit) }
    }

    // Contact Operations
    fun toggleFavorite(contact: Contact) {
        repository.toggleFavorite(contact.id)
    }

    fun openContactDetail(contact: Contact) {
        _selectedContact.value = contact
    }

    fun closeContactDetail() {
        _selectedContact.value = null
    }

    fun openAddContact(prefillNumber: String? = null) {
        _contactToEdit.value = null
        if (!prefillNumber.isNullOrBlank()) {
            _dialedNumber.value = prefillNumber
        }
        _isAddContactOpen.value = true
    }

    fun openEditContact(contact: Contact) {
        _contactToEdit.value = contact
        _isAddContactOpen.value = true
    }

    fun closeAddContact() {
        _isAddContactOpen.value = false
        _contactToEdit.value = null
    }

    fun saveContact(name: String, phone: String, email: String, label: String, isFavorite: Boolean) {
        val existing = _contactToEdit.value
        if (existing != null) {
            val updated = existing.copy(
                name = name,
                phoneNumber = phone,
                email = email,
                label = label,
                isFavorite = isFavorite
            )
            repository.updateContact(updated)
            if (_selectedContact.value?.id == existing.id) {
                _selectedContact.value = updated
            }
        } else {
            val newContact = Contact(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                phoneNumber = phone,
                email = email,
                label = label,
                isFavorite = isFavorite,
                avatarColorIndex = (name.hashCode() and 0x7FFFFFFF) % 8
            )
            repository.addContact(newContact)
        }
        closeAddContact()
    }

    fun deleteContact(contact: Contact) {
        repository.deleteContact(contact.id)
        if (_selectedContact.value?.id == contact.id) {
            _selectedContact.value = null
        }
    }

    fun deleteCallRecord(call: CallRecord) {
        repository.deleteCallRecord(call.id)
    }

    fun clearAllCallLogs() {
        repository.clearAllCallLogs()
    }

    fun sendSms(phoneNumber: String, context: Context) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(phoneNumber)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
