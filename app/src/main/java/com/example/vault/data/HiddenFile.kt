package com.example.vault.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_files")
data class HiddenFile(
    @PrimaryKey val id: String,
    val originalPath: String,
    val encryptedPath: String,
    val fileName: String,
    val fileType: String,
    val hiddenAt: Long
)
