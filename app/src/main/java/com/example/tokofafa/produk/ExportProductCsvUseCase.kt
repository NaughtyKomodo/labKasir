package com.example.tokofafa.usecase

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.example.tokofafa.dao.ProductWithSupplier
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ExportProductCsvUseCase {
    fun execute(context: Context, products: List<ProductWithSupplier>) {
        try {
            val csvContent = StringBuilder()
            csvContent.append("ID,Name,SKU,Supplier,SellingPrice,Stock\n")

            products.forEach { productWithSupplier ->
                val product = productWithSupplier.product
                val id = product.id.toString()
                val name = product.name?.replace(",", "") ?: "-"
                val sku = product.sku?.replace(",", "") ?: "-"
                val supplier = productWithSupplier.supplierName?.replace(",", "") ?: "-"
                val sellingPrice = NumberFormat.getNumberInstance(Locale("id", "ID")).format(product.sellingPrice)
                val stock = product.stock.toString()

                csvContent.append("$id,$name,$sku,$supplier,$sellingPrice,$stock\n")
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(java.util.Date())
            val filename = "products_$timestamp.csv"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvContent.toString().toByteArray())
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, filename)
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(csvContent.toString().toByteArray())
                }

                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = android.net.Uri.fromFile(file)
                context.sendBroadcast(intent)
            }

            Toast.makeText(context, "CSV berhasil disimpan di Downloads", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("TokoFafa", "Error exporting CSV", e)
            Toast.makeText(context, "Gagal mengekspor CSV", Toast.LENGTH_SHORT).show()
        }
    }
}