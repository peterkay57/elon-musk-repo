package com.example.vault.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HiddenFile::class], version = 1)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun hiddenFileDao(): HiddenFileDao
}
