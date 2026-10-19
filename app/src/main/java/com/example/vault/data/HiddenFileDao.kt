package com.example.vault.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HiddenFileDao {
    @Query("SELECT * FROM hidden_files")
    fun getAllHiddenFiles(): kotlinx.coroutines.flow.Flow<List<HiddenFile>>

    @Insert
    suspend fun insert(hiddenFile: HiddenFile)

    @Delete
    suspend fun delete(hiddenFile: HiddenFile)
}
