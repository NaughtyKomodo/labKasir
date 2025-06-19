package com.example.tokofafa

import android.content.Context
import com.example.tokofafa.entities.Supplier
import java.io.File
import java.io.FileWriter

object CsvExporter {
    fun exportToCsv(suppliers: List<Supplier>): String {
        val header = "ID,Nama,Email,Telepon,Alamat,Kota,Provinsi,Kode Pos\n"
        val data = suppliers.joinToString("\n") { supplier ->
            "${supplier.id},${supplier.name},${supplier.email},${supplier.phone},${supplier.address},${supplier.city},${supplier.province},${supplier.postalCode}"
        }
        return header + data
    }

    fun saveCsvFile(context: Context, csvContent: String): File {
        val file = File(context.filesDir, "suppliers_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.write(csvContent)
        }
        return file
    }
}