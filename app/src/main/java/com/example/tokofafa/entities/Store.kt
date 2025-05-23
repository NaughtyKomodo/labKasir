package com.example.tokofafa.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class Store(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val address: String?,
    val city: String?,
    val province: String?,
    val phone: String?,
    val logoUri: String?
)