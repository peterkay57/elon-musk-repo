package com.example.ui

import com.example.vault.VaultViewModel

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ActiveCallState
import com.example.ui.components.ActiveCallScreen
import com.example.ui.components.AddContactDialog
import com.example.ui.components.ContactDetailSheet
import com.example.ui.components.PermissionBanner
import com.example.ui.tabs.ContactsTab
import com.example.ui.tabs.FavoritesTab
import com.example.ui.tabs.KeypadTab
import com.example.ui.tabs.RecentsTab
import com.example.ui.theme.CallGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EndCallRed

@Composable
fun DialerMainScreen(
    viewModel: DialerViewModel,
    vaultViewModel: VaultViewModel,
    vaultManager: com.example.vault.VaultManager,
    onBiometricUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val dialedNumber by viewModel.dialedNumber.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val recentsFilter by viewModel.recentsFilter.collectAsStateWithLifecycle()
    val favoriteContacts by viewModel.favoriteContacts.collectAsStateWithLifecycle()
    val filteredContacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
    val filteredRecents by viewModel.filteredRecents.collectAsStateWithLifecycle()
    val t9Matches by viewModel.t9Matches.collectAsStateWithLifecycle()
    val selectedContact by viewModel.selectedContact.collectAsStateWithLifecycle()
    val isAddContactOpen by viewModel.isAddContactOpen.collectAsStateWithLifecycle()
    val contactToEdit by viewModel.contactToEdit.collectAsStateWithLifecycle()
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    val isCallMinimized by viewModel.isCallMinimized.collectAsStateWithLifecycle()
    val hasPermissions by viewModel.hasPermissions.collectAsStateWithLifecycle()
    val isBannerDismissed by viewModel.isPermissionBannerDismissed.collectAsStateWithLifecycle()

    // Handle back button gracefully
    BackHandler(
        enabled = activeCall != null && !isCallMinimized ||
                selectedContact != null ||
                isAddContactOpen ||
                searchQuery.isNotEmpty() ||
                (selectedTab == DialerTab.KEYPAD && dialedNumber.isNotEmpty()) ||
                selectedTab != DialerTab.KEYPAD
    ) {
        when {
            activeCall != null && !isCallMinimized -> viewModel.minimizeCall()
            selectedContact != null -> viewModel.closeContactDetail()
            isAddContactOpen -> viewModel.closeAddContact()
            searchQuery.isNotEmpty() -> viewModel.setSearchQuery("")
            selectedTab == DialerTab.KEYPAD && dialedNumber.isNotEmpty() -> viewModel.onClearAllDialed()
            selectedTab != DialerTab.KEYPAD -> viewModel.selectTab(DialerTab.KEYPAD)
        }
    }

    val vaultThemeMode by viewModel.vaultThemeMode.collectAsStateWithLifecycle()
    com.example.ui.theme.MyApplicationTheme(
        forceDark = when (vaultThemeMode) {
            "Dark" -> true
            "Light" -> false
            else -> null
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("dialer_bottom_navigation")
                ) {
                // Favorites
                NavigationBarItem(
                    selected = selectedTab == DialerTab.FAVORITES,
                    onClick = { viewModel.selectTab(DialerTab.FAVORITES) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == DialerTab.FAVORITES) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorites"
                        )
                    },
                    label = { Text("Favorites") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricCyan,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_favorites")
                )

                // Recents
                NavigationBarItem(
                    selected = selectedTab == DialerTab.RECENTS,
                    onClick = { viewModel.selectTab(DialerTab.RECENTS) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == DialerTab.RECENTS) Icons.Default.History else Icons.Outlined.History,
                            contentDescription = "Recents"
                        )
                    },
                    label = { Text("Recents") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricCyan,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_recents")
                )

                // Contacts
                NavigationBarItem(
                    selected = selectedTab == DialerTab.CONTACTS,
                    onClick = { viewModel.selectTab(DialerTab.CONTACTS) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == DialerTab.CONTACTS) Icons.Default.People else Icons.Outlined.People,
                            contentDescription = "Contacts"
                        )
                    },
                    label = { Text("Contacts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricCyan,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_contacts")
                )

                // Keypad
                NavigationBarItem(
                    selected = selectedTab == DialerTab.KEYPAD,
                    onClick = { viewModel.selectTab(DialerTab.KEYPAD) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == DialerTab.KEYPAD) Icons.Default.Dialpad else Icons.Outlined.Dialpad,
                            contentDescription = "Keypad"
                        )
                    },
                    label = { Text("Keypad") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricCyan,
                        selectedTextColor = ElectricCyan,
                        indicatorColor = ElectricCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_keypad")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Minimized Call Floating Banner
            if (activeCall != null && isCallMinimized) {
                MinimizedCallBanner(
                    callState = activeCall!!,
                    onExpand = { viewModel.expandCall() },
                    onEnd = { viewModel.endActiveCall() }
                )
            }

            // Permission banner (requests CALL_PHONE, READ_CONTACTS, READ_CALL_LOG)
            PermissionBanner(
                hasPermissions = hasPermissions,
                isDismissed = isBannerDismissed,
                onPermissionsResult = { granted -> viewModel.onPermissionsResult(granted) },
                onDismiss = { viewModel.dismissPermissionBanner() }
            )

            // Search Bar (shown on Favorites, Recents, and Contacts)
            if (selectedTab != DialerTab.KEYPAD) {
                SearchBarHeader(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    placeholder = when (selectedTab) {
                        DialerTab.FAVORITES -> "Search favorites..."
                        DialerTab.RECENTS -> "Search call history..."
                        DialerTab.CONTACTS -> "Search ${filteredContacts.size} contacts..."
                        else -> "Search..."
                    }
                )
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    DialerTab.FAVORITES -> {
                        FavoritesTab(
                            favoriteContacts = favoriteContacts,
                            onContactClick = { viewModel.openContactDetail(it) },
                            onCallClick = { number, contact ->
                                viewModel.initiateCall(number, contact, context)
                            },
                            onMessageClick = { number -> viewModel.sendSms(number, context) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onNavigateToContacts = { viewModel.selectTab(DialerTab.CONTACTS) }
                        )
                    }

                    DialerTab.RECENTS -> {
                        RecentsTab(
                            recents = filteredRecents,
                            selectedFilter = recentsFilter,
                            onFilterChange = { viewModel.setRecentsFilter(it) },
                            onCallClick = { number ->
                                viewModel.initiateCall(number, null, context)
                            },
                            onDeleteCall = { viewModel.deleteCallRecord(it) },
                            onClearAll = { viewModel.clearAllCallLogs() },
                            onItemClick = { record ->
                                val contact = viewModel.contacts.value.firstOrNull {
                                    it.phoneNumber == record.phoneNumber
                                }
                                if (contact != null) {
                                    viewModel.openContactDetail(contact)
                                } else {
                                    viewModel.initiateCall(record.phoneNumber, null, context)
                                }
                            }
                        )
                    }

                    DialerTab.CONTACTS -> {
                        ContactsTab(
                            contacts = filteredContacts,
                            onContactClick = { viewModel.openContactDetail(it) },
                            onCallClick = { number, contact ->
                                viewModel.initiateCall(number, contact, context)
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddContactClick = { viewModel.openAddContact() }
                        )
                    }

                    DialerTab.KEYPAD -> {
                        KeypadTab(
                            dialedNumber = dialedNumber,
                            t9Matches = t9Matches,
                            onDigitPress = { digit -> viewModel.onKeypadDigitPress(digit, context) },
                            onBackspacePress = { viewModel.onBackspacePress(context) },
                            onClearAllPress = { viewModel.onClearAllDialed() },
                            onCallPress = { number ->
                                viewModel.initiateCall(number, null, context)
                            },
                            onAddContactPress = { number -> viewModel.openAddContact(number) },
                            onContactSelect = { contact -> viewModel.openContactDetail(contact) },
                            onBiometricUnlock = onBiometricUnlock
                        )
                    }
                }
            }
        }
    }

    // Active Full Screen Call Overlay
    if (activeCall != null && !isCallMinimized) {
        ActiveCallScreen(
            callState = activeCall!!,
            onEndCall = { viewModel.endActiveCall() },
            onToggleMute = { viewModel.toggleMute() },
            onToggleSpeaker = { viewModel.toggleSpeaker() },
            onToggleHold = { viewModel.toggleHold() },
            onToggleKeypad = { viewModel.toggleCallKeypad() },
            onSendDtmf = { digit -> viewModel.sendDtmfDigit(digit) },
            onMinimize = { viewModel.minimizeCall() }
        )
    }

    // Vault Overlay
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    if (isVaultUnlocked) {
        VaultScreen(
            viewModel = vaultViewModel,
            vaultManager = vaultManager,
            onLock = { viewModel.lockVault() }
        )
    }
    }

    // Contact Details Modal Sheet
    if (selectedContact != null) {
        ContactDetailSheet(
            contact = selectedContact!!,
            onDismiss = { viewModel.closeContactDetail() },
            onCall = { number ->
                viewModel.closeContactDetail()
                viewModel.initiateCall(number, selectedContact, context)
            },
            onMessage = { number -> viewModel.sendSms(number, context) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onEdit = { contact ->
                viewModel.closeContactDetail()
                viewModel.openEditContact(contact)
            },
            onDelete = { contact ->
                viewModel.deleteContact(contact)
                viewModel.closeContactDetail()
            }
        )
    }

    // Add or Edit Contact Dialog
    if (isAddContactOpen) {
        AddContactDialog(
            contactToEdit = contactToEdit,
            prefillNumber = dialedNumber,
            onDismiss = { viewModel.closeAddContact() },
            onSave = { name, phone, email, label, isFavorite ->
                viewModel.saveContact(name, phone, email, label, isFavorite)
            }
        )
    }
}

@Composable
private fun SearchBarHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("main_search_input")
        )
    }
}

@Composable
private fun MinimizedCallBanner(
    callState: ActiveCallState,
    onExpand: () -> Unit,
    onEnd: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CallGreen
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onExpand() }
            .testTag("minimized_call_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = callState.displayTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Call in progress • ${callState.formattedDuration}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EndCallRed)
                    .clickable { onEnd() }
                    .testTag("minimized_end_call_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
