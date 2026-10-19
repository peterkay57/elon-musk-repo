package com.example.vault

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class VaultManager(private val context: Context) {
    private val keyAlias = "vault_key"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    init {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey = keyStore.getKey(keyAlias, null) as SecretKey

    fun encryptFile(inputFile: File, outputFile: File) {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        
        FileOutputStream(outputFile).use { fos ->
            fos.write(iv) // Store IV at start of file
            CipherOutputStream(fos, cipher).use { cos ->
                FileInputStream(inputFile).use { fis ->
                    fis.copyTo(cos)
                }
            }
        }
    }

    fun decryptFile(inputFile: File, outputFile: File) {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        
        FileInputStream(inputFile).use { fis ->
            val iv = ByteArray(16)
            fis.read(iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))
            
            CipherInputStream(fis, cipher).use { cis ->
                FileOutputStream(outputFile).use { fos ->
                    cis.copyTo(fos)
                }
            }
        }
    }
}
