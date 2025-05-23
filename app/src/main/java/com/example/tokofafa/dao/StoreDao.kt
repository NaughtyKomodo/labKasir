package com.example.tokofafa.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.tokofafa.entities.Store
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores LIMIT 1")
    fun getStore(): Flow<Store?>

    @Insert
    suspend fun insert(store: Store)

    @Update
    suspend fun update(store: Store)
}