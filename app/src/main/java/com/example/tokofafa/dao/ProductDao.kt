package com.example.tokofafa.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.tokofafa.entities.Product

data class ProductWithSupplier(
    @Embedded val product: Product,
    @ColumnInfo(name = "supplierName") val supplierName: String?
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM product")
    fun getAll(): LiveData<List<Product>>

    @Query("UPDATE product SET stock = stock - :quantity WHERE barcode = :barcode")
    fun decreaseStock(barcode: String, quantity: Int)

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM product WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product)

    @Update
    suspend fun update(product: Product)

    @Query("DELETE FROM product WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE product SET stock = stock + 1 WHERE id = :id")
    suspend fun incrementStock(id: Long)

    @Query("""
        SELECT product.*, suppliers.name as supplierName
        FROM product
        LEFT JOIN suppliers ON product.supplierId = suppliers.id
    """)
    fun getAllProductsWithSupplier(): LiveData<List<ProductWithSupplier>>

}