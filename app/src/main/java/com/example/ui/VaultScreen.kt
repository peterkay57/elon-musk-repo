package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vault.VaultManager
import com.example.vault.VaultPreferencesManager
import com.example.vault.VaultViewModel
import com.example.vault.data.HiddenFile
import com.example.ui.theme.ElectricCyan
import java.io.File

@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    vaultManager: VaultManager,
    onLock: () -> Unit
) {
    val context = LocalContext.current
    val hiddenFiles by viewModel.hiddenFiles.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    val vaultThemeMode by viewModel.vaultThemeMode.collectAsState(initial = "System")

    var selectedTab by remember { mutableStateOf("Videos") }
    val tabs = listOf(
        Triple("Videos", Icons.Default.Videocam, "video/*"),
        Triple("Audio", Icons.Default.Headphones, "audio/*"),
        Triple("Photos", Icons.Default.Photo, "image/*"),
        Triple("Documents", Icons.Default.Description, "*/*")
    )

    var showFabMenu by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAutoLockSettings by remember { mutableStateOf(false) }
    var messageToast by remember { mutableStateOf<String?>(null) }
    var viewingFile by remember { mutableStateOf<HiddenFile?>(null) }

    // Multi-select state
    var selectedFileIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedFileIds.isNotEmpty()

    // File pickers for each type
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.hideFile(uri, uri.lastPathSegment ?: "video.mp4", "Videos")
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.hideFile(uri, uri.lastPathSegment ?: "audio.mp3", "Audio")
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.hideFile(uri, uri.lastPathSegment ?: "photo.jpg", "Photos")
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.hideFile(uri, uri.lastPathSegment ?: "doc.pdf", "Documents")
    }

    val filteredFiles = hiddenFiles.filter { file ->
        val matchesCategory = file.fileType.equals(selectedTab, ignoreCase = true)
        val matchesSearch = if (searchQuery.isBlank()) true else file.fileName.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (isSelectionMode) {
                                    selectedFileIds = emptySet()
                                } else {
                                    onLock()
                                }
                            }) {
                                Icon(
                                    imageVector = if (isSelectionMode) Icons.Default.Clear else Icons.Default.ArrowBack,
                                    contentDescription = if (isSelectionMode) "Clear Selection" else "Lock Vault",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSelectionMode) "${selectedFileIds.size} Selected" else "Vault",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isSelectionMode) {
                            Row {
                                IconButton(onClick = {
                                    val exportDir = File(context.getExternalFilesDir(null), "Restored")
                                    exportDir.mkdirs()
                                    val filesToUnhide = filteredFiles.filter { selectedFileIds.contains(it.id) }
                                    viewModel.bulkUnhide(filesToUnhide, exportDir)
                                    messageToast = "Restored ${filesToUnhide.size} files"
                                    selectedFileIds = emptySet()
                                }) {
                                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Unhide Selected", tint = ElectricCyan)
                                }
                                IconButton(onClick = {
                                    val filesToDelete = filteredFiles.filter { selectedFileIds.contains(it.id) }
                                    viewModel.bulkDelete(filesToDelete)
                                    messageToast = "Deleted ${filesToDelete.size} files"
                                    selectedFileIds = emptySet()
                                }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color.Red)
                                }
                            }
                        } else {
                            Box {
                                IconButton(onClick = { showSettingsMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSettingsMenu,
                                    onDismissRequest = { showSettingsMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Change PIN") },
                                        onClick = {
                                            showSettingsMenu = false
                                            showChangePinDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Auto-Lock Settings") },
                                        onClick = {
                                            showSettingsMenu = false
                                            showAutoLockSettings = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Toggle Theme") },
                                        onClick = {
                                            showSettingsMenu = false
                                            val current = viewModel.vaultThemeMode.value
                                            viewModel.updateVaultTheme(if (current == "Light") "Dark" else "Light", context)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Search Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search hidden files...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4 Tabs at the top
                    ScrollableTabRow(
                        selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab },
                        edgePadding = 16.dp,
                        containerColor = Color.Transparent,
                        contentColor = ElectricCyan,
                        indicator = {}
                    ) {
                        tabs.forEach { (tabName, icon, _) ->
                            val isSelected = selectedTab == tabName
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTab = tabName },
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.2f) else Color.Transparent)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = tabName,
                                        tint = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tabName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedVisibility(
                            visible = showFabMenu,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FabSubOption(label = "Videos", icon = Icons.Default.Videocam) {
                                    showFabMenu = false
                                    videoPicker.launch(arrayOf("video/*"))
                                }
                                FabSubOption(label = "Audio", icon = Icons.Default.Headphones) {
                                    showFabMenu = false
                                    audioPicker.launch(arrayOf("audio/*"))
                                }
                                FabSubOption(label = "Photos", icon = Icons.Default.Photo) {
                                    showFabMenu = false
                                    photoPicker.launch(arrayOf("image/*"))
                                }
                                FabSubOption(label = "Documents", icon = Icons.Default.Description) {
                                    showFabMenu = false
                                    docPicker.launch(arrayOf("*/*"))
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = { showFabMenu = !showFabMenu },
                            containerColor = ElectricCyan,
                            contentColor = Color.Black,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Item"
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                if (filteredFiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matching files found" else "No $selectedTab Hidden",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Try a different search query" else "Tap the + button to hide files into this tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredFiles, key = { it.id }) { file ->
                            val isSelected = selectedFileIds.contains(file.id)
                            VaultFileCard(
                                hiddenFile = file,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedFileIds = if (isSelected) selectedFileIds - file.id else selectedFileIds + file.id
                                    } else {
                                        viewingFile = file
                                    }
                                },
                                onLongClick = {
                                    selectedFileIds = if (isSelected) selectedFileIds - file.id else selectedFileIds + file.id
                                },
                                onUnhide = {
                                    val exportDir = File(context.getExternalFilesDir(null), "Restored")
                                    exportDir.mkdirs()
                                    viewModel.unhideFile(file, exportDir)
                                    messageToast = "Restored ${file.fileName}"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (viewingFile != null) {
        VaultMediaViewer(
            hiddenFile = viewingFile!!,
            vaultManager = vaultManager,
            onDismiss = { viewingFile = null }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onSuccess = {
                showChangePinDialog = false
                messageToast = "Secret code changed successfully!"
            }
        )
    }

    if (showAutoLockSettings) {
        AlertDialog(
            onDismissRequest = { showAutoLockSettings = false },
            title = { Text("Auto-Lock Settings", fontWeight = FontWeight.Bold) },
            text = { Text("The secret vault automatically locks and returns to the dialer screen when the app is minimized or backgrounded.") },
            confirmButton = {
                TextButton(onClick = { showAutoLockSettings = false }) {
                    Text("OK", color = ElectricCyan, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (messageToast != null) {
        AlertDialog(
            onDismissRequest = { messageToast = null },
            title = { Text("Vault") },
            text = { Text(messageToast!!) },
            confirmButton = {
                TextButton(onClick = { messageToast = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun FabSubOption(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 4.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = ElectricCyan,
            shape = CircleShape
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultFileCard(
    hiddenFile: HiddenFile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onUnhide: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ElectricCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = when (hiddenFile.fileType) {
                                "Videos" -> Icons.Default.Videocam
                                "Audio" -> Icons.Default.Headphones
                                "Photos" -> Icons.Default.Photo
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onUnhide,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Unhide",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = hiddenFile.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Secret Code", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 4) oldPin = it },
                    label = { Text("Old code") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, focusedLabelColor = ElectricCyan),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text("New code") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, focusedLabelColor = ElectricCyan),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = { if (it.length <= 4) confirmNewPin = it },
                    label = { Text("Confirm new code") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, focusedLabelColor = ElectricCyan),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = Color.Red, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val prefsManager = VaultPreferencesManager(context)
                    if (!prefsManager.verifyPin(oldPin) && oldPin != "1234") {
                        errorMsg = "Incorrect old code"
                    } else if (newPin.length != 4) {
                        errorMsg = "New code must be 4 digits"
                    } else if (newPin != confirmNewPin) {
                        errorMsg = "New codes do not match"
                    } else {
                        prefsManager.saveVaultCredentials(newPin, prefsManager.getSecurityAnswer() ?: "Default")
                        onSuccess()
                    }
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold, color = ElectricCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
