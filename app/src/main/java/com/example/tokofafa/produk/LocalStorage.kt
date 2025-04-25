package com.example.tokofafa.produk

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalStorage(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("TokoFafaPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PRODUCTS_KEY = "products"
    }

    fun saveProducts(products: List<Product>) {
        val json = gson.toJson(products)
        sharedPreferences.edit().putString(PRODUCTS_KEY, json).apply()
    }

    fun getProducts(): List<Product> {
        val json = sharedPreferences.getString(PRODUCTS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<Product>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun addProduct(product: Product) {
        val products = getProducts().toMutableList()
        products.add(product)
        saveProducts(products)
    }

    fun updateProduct(updatedProduct: Product) {
        val products = getProducts().toMutableList()
        val index = products.indexOfFirst { it.id == updatedProduct.id }
        if (index != -1) {
            products[index] = updatedProduct
            saveProducts(products)
        }
    }

    fun deleteProduct(productId: Long) {
        val products = getProducts().toMutableList()
        products.removeAll { it.id == productId }
        saveProducts(products)
    }
}