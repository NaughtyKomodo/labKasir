package com.example.tokofafa.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "product",
    foreignKeys = [
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L, // Changed to autoGenerate = true, default to 0L
    val name: String?,
    val description: String?,
    val sku: String?,
    val barcode: String?,
    val supplierId: Int?,
    val basePrice: Double,
    val sellingPrice: Double,
    val stock: Int,
    val photoUri: String?
)