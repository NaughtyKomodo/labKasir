package com.example.tokofafa.dao

import androidx.room.Dao
import androidx.room.Insert
import com.example.tokofafa.entities.Transaction

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)
}