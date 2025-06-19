package com.example.tokofafa.produk

import java.io.Serializable

data class Product(
    val id: Long = System.currentTimeMillis(), // Unique ID based on timestamp
    val name: String,
    val description: String,
    val sku: String,
    val barcode: String,
    val supplier: String,
    val basePrice: Double, // Harga Pokok
    val sellingPrice: Double, // Harga Jual
    val stock: Int, // Jumlah
    val photoUri: String? = null // URI of the product photo (nullable)
) : Serializable