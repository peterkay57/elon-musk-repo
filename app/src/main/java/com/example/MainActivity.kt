package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.DialerRepository
import com.example.ui.DialerMainScreen
import com.example.ui.DialerTab
import com.example.ui.DialerViewModel
import com.example.ui.VaultSetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.vault.VaultManager
import com.example.vault.VaultViewModel
import com.example.vault.data.VaultDatabase

class MainActivity : androidx.fragment.app.FragmentActivity() {

    private lateinit var repository: DialerRepository
    private lateinit var vaultDatabase: VaultDatabase
    private lateinit var vaultManager: VaultManager

    private val viewModel: DialerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DialerViewModel(repository) as T
            }
        }
    }

    private val vaultViewModel: VaultViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VaultViewModel(applicationContext, vaultDatabase.hiddenFileDao(), vaultManager) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = DialerRepository(applicationContext)
        vaultDatabase = Room.databaseBuilder(
            applicationContext,
            VaultDatabase::class.java,
            "vault_db"
        ).build()
        vaultManager = VaultManager(applicationContext)

        handleDialIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val vaultPrefsManager = remember { com.example.vault.VaultPreferencesManager(applicationContext) }
                    var isSetupDone by remember {
                        mutableStateOf(vaultPrefsManager.isSetupComplete())
                    }

                    if (!isSetupDone) {
                        VaultSetupScreen(
                            onSetupComplete = { _, _ ->
                                isSetupDone = true
                            }
                        )
                    } else {
                        DialerMainScreen(
                            viewModel = viewModel,
                            vaultViewModel = vaultViewModel,
                            vaultManager = vaultManager,
                            onBiometricUnlock = {
                                com.example.vault.BiometricAuthHelper.promptBiometric(
                                    this,
                                    onSuccess = {
                                        viewModel.unlockVault("biometric", this)
                                    },
                                    onError = { _ -> }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDialIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }

    override fun onStop() {
        super.onStop()
        viewModel.lockVault()
    }

    private fun handleDialIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "tel") {
            val number = data.schemeSpecificPart
            if (!number.isNullOrBlank()) {
                viewModel.setDialedNumber(number)
                viewModel.selectTab(DialerTab.KEYPAD)
            }
        }
    }
}
