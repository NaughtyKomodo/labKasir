package com.example.tokofafa.produk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(navController: NavController) {
    val context = LocalContext.current
    val localStorage = remember { LocalStorage(context) }
    val products by remember { mutableStateOf(localStorage.getProducts()) }
    var showFilterDropdown by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Produk") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        AsyncImage(
                            model = "https://cdn-icons-png.flaticon.com/512/271/271220.png", // Back arrow icon
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle search */ }) {
                        AsyncImage(
                            model = "https://cdn-icons-png.flaticon.com/512/954/954591.png", // Search icon
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        AsyncImage(
                            model = "https://cdn-icons-png.flaticon.com/512/2089/2089627.png", // More icon
                            contentDescription = "More",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("CSV") },
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
                    model = "https://cdn-icons-png.flaticon.com/512/992/992651.png", // Add icon
                    contentDescription = "Add Product",
                    modifier = Modifier.size(24.dp)
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
            // Filter Dropdown
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
                        model = "https://cdn-icons-png.flaticon.com/512/2985/2985150.png", // Dropdown arrow icon
                        contentDescription = "Dropdown",
                        modifier = Modifier.size(24.dp)
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
                    // Add more filter options if needed
                }
            }

            // Product List
            LazyColumn {
                items(products) { product ->
                    ProductItem(
                        product = product,
                        onAddStock = {
                            val updatedProduct = product.copy(stock = product.stock + 1)
                            localStorage.updateProduct(updatedProduct)
                        },
                        onEdit = {
                            navController.navigate("add_product/${product.id}")
                        },
                        onDelete = {
                            localStorage.deleteProduct(product.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    onAddStock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product Photo or Placeholder
        if (product.photoUri != null) {
            AsyncImage(
                model = product.photoUri,
                contentDescription = "Product Photo",
                modifier = Modifier
                    .size(50.dp)
                    .padding(end = 16.dp)
            )
        } else {
            AsyncImage(
                model = "https://cdn-icons-png.flaticon.com/512/679/679922.png", // Placeholder icon
                contentDescription = "Product Placeholder",
                modifier = Modifier
                    .size(50.dp)
                    .padding(end = 16.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = product.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Minuman",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(product.sellingPrice)}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tambah Stok ${product.stock}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                AsyncImage(
                    model = "https://cdn-icons-png.flaticon.com/512/2089/2089627.png", // More icon
                    contentDescription = "More",
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
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        thickness = 1.dp
    )
}