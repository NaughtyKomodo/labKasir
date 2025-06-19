package com.example.tokofafa

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.tokofafa.database.AppDatabase
import com.example.tokofafa.entities.Supplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SupplierViewModel(private val db: AppDatabase) : ViewModel() {
    val suppliers: LiveData<List<Supplier>> = liveData(Dispatchers.IO) {
        emitSource(db.supplierDao().getAll())
    }

    fun addSupplier(supplier: Supplier) {
        viewModelScope.launch(Dispatchers.IO) {
            db.supplierDao().insert(supplier)
        }
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch(Dispatchers.IO) {
            db.supplierDao().update(supplier)
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch(Dispatchers.IO) {
            db.supplierDao().delete(supplier)
        }
    }

    fun exportToCsv(context: Context, onComplete: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // Fetch suppliers synchronously in IO thread
            val suppliers = db.supplierDao().getAll().value ?: emptyList()
            val csv = CsvExporter.exportToCsv(suppliers)
            val file = CsvExporter.saveCsvFile(context, csv)
            onComplete(file)
        }
    }
}

class SupplierViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupplierViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SupplierViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}