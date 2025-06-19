package com.example.tokofafa.produk

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tokofafa.R
import com.example.tokofafa.database.AppDatabase
import com.example.tokofafa.entities.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.tokofafa.dao.ProductWithSupplier
import com.example.tokofafa.ui.theme.*

class ProductViewModel(private val db: AppDatabase) : ViewModel() {
    val products: LiveData<List<ProductWithSupplier>> = liveData(Dispatchers.IO) {
        emitSource(db.productDao().getAllProductsWithSupplier())
    }

    fun addProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            db.productDao().insert(product)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch(Dispatchers.IO) {
            db.productDao().update(product)
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.productDao().delete(id)
        }
    }

    suspend fun getProductById(id: Long): Product? {
        return db.productDao().getProductById(id)
    }

    fun incrementStock(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.productDao().incrementStock(id)
        }
    }
}

class ProductViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    navController: NavController,
    viewModel: ProductViewModel = viewModel(
        factory = ProductViewModelFactory(AppDatabase.getDatabase(LocalContext.current))
    )
) {
    val context = LocalContext.current
    val products by viewModel.products.observeAsState(initial = emptyList())
    var showFilterDropdown by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var showMenu by remember { mutableStateOf<Long?>(null) }

    // Storage permission launcher for CSV export
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            exportToCsv(context, products)
        } else {
            Toast.makeText(context, "Izin penyimpanan ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("DAFTAR PRODUK")

    LaunchedEffect(products) {
        Log.d("TokoFafa", "Product list updated, size: ${products.size}")
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Daftar Produk", color = AccentText) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            AsyncImage(
                                model = "https://cdn-icons-png.flaticon.com/512/271/271220.png",
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp),
                                placeholder = painterResource(R.drawable.ic_placeholder),
                                error = painterResource(R.drawable.ic_error)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenu = if (showMenu == null) 0 else null }) {
                            Image(
                                painter = painterResource(id = R.drawable.excel),
                                contentDescription = "More",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu != null,
                            onDismissRequest = { showMenu = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = {
                                    showMenu = null
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        exportToCsv(context, products)
                                    } else {
                                        storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PrimaryBlue,
                        titleContentColor = AccentText
                    )
                )
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    contentColor = AccentText,
                    containerColor = PrimaryBlue
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    title,
                                    color = if (selectedTabIndex == index) AccentText else DarkText
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_product/-1") },
                containerColor = Color(0xFF1EB980),
                contentColor = Color.White
            ) {
                AsyncImage(
                    model = "https://cdn-icons-png.flaticon.com/512/992/992651.png",
                    contentDescription = "Add Product",
                    modifier = Modifier.size(24.dp),
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    error = painterResource(R.drawable.ic_error)
                )
            }
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedButton(
                    onClick = { showFilterDropdown = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedFilter)
                    Spacer(modifier = Modifier.width(8.dp))
                    AsyncImage(
                        model = "https://cdn-icons-png.flaticon.com/512/2985/2985150.png",
                        contentDescription = "Dropdown",
                        modifier = Modifier.size(24.dp),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        error = painterResource(R.drawable.ic_error)
                    )
                }
                DropdownMenu(
                    expanded = showFilterDropdown,
                    onDismissRequest = { showFilterDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Semua") },
                        onClick = {
                            selectedFilter = "Semua"
                            showFilterDropdown = false
                        }
                    )
                }
            }

            LazyColumn {
                items(products) { productWithSupplier ->
                    ProductItem(
                        productWithSupplier = productWithSupplier,
                        onAddStock = { viewModel.incrementStock(productWithSupplier.product.id) },
                        onEdit = { navController.navigate("add_product/${productWithSupplier.product.id}") },
                        onDelete = { viewModel.deleteProduct(productWithSupplier.product.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductItem(
    productWithSupplier: ProductWithSupplier,
    onAddStock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val product = productWithSupplier.product

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (product.photoUri != null && File(product.photoUri).exists()) {
            AsyncImage(
                model = product.photoUri,
                contentDescription = "Product Photo",
                modifier = Modifier
                    .size(50.dp)
                    .padding(end = 16.dp),
                placeholder = painterResource(R.drawable.ic_placeholder),
                error = painterResource(R.drawable.ic_error)
            )
        } else {
            AsyncImage(
                model = "https://cdn-icons-png.flaticon.com/512/679/679922.png",
                contentDescription = "Product Placeholder",
                modifier = Modifier
                    .size(50.dp)
                    .padding(end = 16.dp),
                placeholder = painterResource(R.drawable.ic_placeholder),
                error = painterResource(R.drawable.ic_error)
            )
            Log.d("TokoFafa", "Product photo does not exist: ${product.photoUri}")
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = product.name ?: "Unknown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Text(
                text = "barang",
                fontSize = 14.sp,
                color = DarkText.copy(alpha = 0.7f)
            )
            Text(
                text = "Supplier: ${productWithSupplier.supplierName ?: "Tidak ada"}",
                fontSize = 14.sp,
                color = DarkText.copy(alpha = 0.7f)
            )
            Text(
                text = "Rp ${
                    NumberFormat.getNumberInstance(Locale("id", "ID")).format(product.sellingPrice)
                }",
                fontSize = 14.sp,
                color = DarkText
            )
            Text(
                text = "Stok: ${product.stock}",
                fontSize = 14.sp,
                color = DarkText.copy(alpha = 0.7f)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Image(
                    painter = painterResource(id = R.drawable.edit),
                    contentDescription = "Edit",
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Tambah Stok") },
                    onClick = {
                        onAddStock()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        onEdit()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Hapus") },
                    onClick = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
        }
    }

    Divider(
        color = DarkText.copy(alpha = 0.1f),
        thickness = 1.dp
    )
}

fun exportToCsv(context: Context, products: List<ProductWithSupplier>) {
    try {
        // Generate CSV content
        val csvContent = StringBuilder()
        // CSV Header
        csvContent.append("ID,Name,SKU,Supplier,SellingPrice,Stock\n")

        // CSV Rows
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

        // Generate filename with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(java.util.Date())
        val filename = "products_$timestamp.csv"

        // Save to storage
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

            // Notify media scanner
            val intent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = android.net.Uri.fromFile(file)
            context.sendBroadcast(intent)
        }

        Toast.makeText(context, "CSV berhasil disimpan di Downloads", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e("TokoFafa", "Error exporting CSV", e)
        Toast.makeText(context, "Gagal mengekspor CSV", Toast.LENGTH_SHORT).show()
    }
}