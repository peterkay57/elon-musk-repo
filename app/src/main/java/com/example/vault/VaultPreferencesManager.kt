package com.example.vault

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class VaultPreferencesManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_vault_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("VaultPrefs", "EncryptedSharedPreferences failed, using fallback storage", e)
        context.getSharedPreferences("secure_vault_prefs_fallback", Context.MODE_PRIVATE)
    }

    fun saveVaultCredentials(pin: String, securityAnswer: String) {
        prefs.edit()
            .putString("vault_pin", pin)
            .putString("security_answer", securityAnswer)
            .putBoolean("vault_setup_done", true)
            .apply()
        Log.d("VaultPrefs", "Vault credentials saved successfully. PIN length: ${pin.length}")
    }

    fun getStoredPin(): String? {
        val pin = prefs.getString("vault_pin", null)
        Log.d("VaultPrefs", "Retrieved stored PIN: $pin")
        return pin
    }

    fun getSecurityAnswer(): String? {
        return prefs.getString("security_answer", null)
    }

    fun isSetupComplete(): Boolean {
        val complete = prefs.getBoolean("vault_setup_done", false)
        Log.d("VaultPrefs", "Is setup complete: $complete")
        return complete
    }

    fun verifyPin(inputPin: String): Boolean {
        val storedPin = getStoredPin()
        Log.d("VaultPrefs", "Verifying PIN. Input: '$inputPin', Stored: '$storedPin'")
        return storedPin != null && storedPin.trim() == inputPin.trim()
    }

    fun getVaultThemeMode(): String {
        return prefs.getString("vault_theme", "System") ?: "System"
    }

    fun setVaultThemeMode(mode: String) {
        prefs.edit().putString("vault_theme", mode).apply()
    }
}
