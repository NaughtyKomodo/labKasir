package com.example.tokofafa.produk

import android.util.Log
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
import java.text.NumberFormat
import java.util.Locale
import com.example.tokofafa.dao.ProductWithSupplier



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

    LaunchedEffect(products) {
        Log.d("TokoFafa", "Product list updated, size: ${products.size}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Produk") },
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
                    IconButton(onClick = { /* Handle search */ }) {
                        AsyncImage(
                            model = "https://cdn-icons-png.flaticon.com/512/954/954591.png",
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            error = painterResource(R.drawable.ic_error)
                        )
                    }
                    IconButton(onClick = { showMenu = if (showMenu == null) 0 else null }) {
                        AsyncImage(
                            model = "https://cdn-icons-png.flaticon.com/512/2089/2089627.png",
                            contentDescription = "More",
                            modifier = Modifier.size(24.dp),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            error = painterResource(R.drawable.ic_error)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu != null,
                        onDismissRequest = { showMenu = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export CSV") },
                            onClick = { /* Handle CSV export */ }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
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
        containerColor = MaterialTheme.colorScheme.background
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
            .background(MaterialTheme.colorScheme.surface)
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "barang",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Supplier: ${productWithSupplier.supplierName ?: "Tidak ada"}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Rp ${
                    NumberFormat.getNumberInstance(Locale("id", "ID")).format(product.sellingPrice)
                }",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Stok: ${product.stock}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                AsyncImage(
                    model = "https://cdn-icons-png.flaticon.com/512/2089/2089627.png",
                    contentDescription = "More",
                    modifier = Modifier.size(24.dp),
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    error = painterResource(R.drawable.ic_error)
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
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        thickness = 1.dp
    )
}