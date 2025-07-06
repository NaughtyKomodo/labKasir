package com.example.tokofafa.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tokofafa.dao.ProductDao
import com.example.tokofafa.dao.StoreDao
import com.example.tokofafa.dao.SupplierDao
import com.example.tokofafa.dao.TransactionDao
import com.example.tokofafa.entities.Product
import com.example.tokofafa.entities.Store
import com.example.tokofafa.entities.Supplier
import com.example.tokofafa.entities.Transaction

@Database(entities = [Product::class, Supplier::class, Transaction::class, Store::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun supplierDao(): SupplierDao
    abstract fun transactionDao(): TransactionDao
    abstract fun storeDao(): StoreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tokofafa_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .fallbackToDestructiveMigration() // For development only; remove for production
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS suppliers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        contact TEXT,
                        address TEXT
                    )
                """)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN supplierId INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE products_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT,
                        description TEXT,
                        sku TEXT,
                        barcode TEXT,
                        supplierId INTEGER,
                        basePrice REAL NOT NULL,
                        sellingPrice REAL NOT NULL,
                        stock INTEGER NOT NULL,
                        photoUri TEXT,
                        FOREIGN KEY(supplierId) REFERENCES suppliers(id) ON DELETE SET NULL
                    )
                """)
                database.execSQL("""
                    INSERT INTO products_new (id, name, description, sku, barcode, supplierId, basePrice, sellingPrice, stock, photoUri)
                    SELECT id, name, description, sku, barcode, supplierId, basePrice, sellingPrice, stock, photoUri
                    FROM products
                """)
                database.execSQL("DROP TABLE products")
                database.execSQL("ALTER TABLE products_new RENAME TO products")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        barcode TEXT NOT NULL,
                        productName TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        unitPrice REAL NOT NULL,
                        totalPrice REAL NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS stores (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT,
                        address TEXT,
                        city TEXT,
                        province TEXT,
                        phone TEXT,
                        logoUri TEXT
                    )
                """)
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes needed for autoGenerate = true
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Check if products table exists
                val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='products'")
                val productsTableExists = cursor.moveToFirst()
                cursor.close()

                // Create products table if it doesn't exist
                if (!productsTableExists) {
                    database.execSQL("""
                        CREATE TABLE products (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT,
                            description TEXT,
                            sku TEXT,
                            barcode TEXT,
                            supplierId INTEGER,
                            basePrice REAL NOT NULL,
                            sellingPrice REAL NOT NULL,
                            stock INTEGER NOT NULL,
                            photoUri TEXT,
                            FOREIGN KEY(supplierId) REFERENCES suppliers(id) ON DELETE SET NULL
                        )
                    """)
                } else {
                    // Create temporary table
                    database.execSQL("""
                        CREATE TABLE products_temp (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT,
                            description TEXT,
                            sku TEXT,
                            barcode TEXT,
                            supplierId INTEGER,
                            basePrice REAL NOT NULL,
                            sellingPrice REAL NOT NULL,
                            stock INTEGER NOT NULL,
                            photoUri TEXT,
                            FOREIGN KEY(supplierId) REFERENCES suppliers(id) ON DELETE SET NULL
                        )
                    """)
                    // Copy data from existing products table
                    database.execSQL("""
                        INSERT INTO products_temp (id, name, description, sku, barcode, supplierId, basePrice, sellingPrice, stock, photoUri)
                        SELECT id, name, description, sku, barcode, supplierId, basePrice, sellingPrice, stock, photoUri
                        FROM products
                    """)
                    // Drop old table and rename temp table
                    database.execSQL("DROP TABLE products")
                    database.execSQL("ALTER TABLE products_temp RENAME TO products")
                }
            }
        }
    }
}