package com.example.vault

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vault.data.HiddenFile
import com.example.vault.data.HiddenFileDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class VaultViewModel(
    private val context: Context,
    private val dao: HiddenFileDao,
    private val vaultManager: VaultManager
) : ViewModel() {

    private val prefsManager = VaultPreferencesManager(context)

    private val _vaultThemeMode = MutableStateFlow(prefsManager.getVaultThemeMode())
    val vaultThemeMode: StateFlow<String> = _vaultThemeMode.asStateFlow()

    fun updateVaultTheme(theme: String, context: Context) {
        prefsManager.setVaultThemeMode(theme)
        _vaultThemeMode.value = theme
    }

    val hiddenFiles: StateFlow<List<HiddenFile>> = dao.getAllHiddenFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun hideFile(uri: Uri, fileName: String, fileType: String) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val vaultDir = File(context.filesDir, "vault_secure").apply { mkdirs() }
                val fileId = UUID.randomUUID().toString()
                val encryptedFile = File(vaultDir, "$fileId.enc")
                val tempInput = File(context.cacheDir, "temp_input")

                tempInput.outputStream().use { fos ->
                    inputStream.copyTo(fos)
                }
                inputStream.close()

                vaultManager.encryptFile(tempInput, encryptedFile)
                tempInput.delete()

                val hiddenRecord = HiddenFile(
                    id = fileId,
                    originalPath = uri.toString(),
                    encryptedPath = encryptedFile.absolutePath,
                    fileName = fileName,
                    fileType = fileType,
                    hiddenAt = System.currentTimeMillis()
                )

                dao.insert(hiddenRecord)

                // Try to delete original if content provider allows or if it's a file URI
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (_: Exception) {}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unhideFile(hiddenFile: HiddenFile, exportDir: File) {
        viewModelScope.launch {
            try {
                val encryptedFile = File(hiddenFile.encryptedPath)
                if (!encryptedFile.exists()) return@launch

                val restoredFile = File(exportDir, hiddenFile.fileName)
                vaultManager.decryptFile(encryptedFile, restoredFile)

                encryptedFile.delete()
                dao.delete(hiddenFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun bulkUnhide(files: List<HiddenFile>, exportDir: File) {
        viewModelScope.launch {
            for (file in files) {
                unhideFile(file, exportDir)
            }
        }
    }

    fun bulkDelete(files: List<HiddenFile>) {
        viewModelScope.launch {
            for (file in files) {
                try {
                    val enc = File(file.encryptedPath)
                    if (enc.exists()) enc.delete()
                    dao.delete(file)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
