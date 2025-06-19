package com.example.tokofafa.inventori

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.tokofafa.R
import com.example.tokofafa.dao.ProductWithSupplier
import com.example.tokofafa.database.AppDatabase
import com.example.tokofafa.produk.ProductViewModel
import com.example.tokofafa.produk.ProductViewModelFactory
import com.example.tokofafa.ui.theme.*
import java.io.File
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoriScreen(navController: NavController) {
    val context = LocalContext.current
    var showFilterDropdown by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var showMenu by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("KARTU STOK", "PEMBELIAN")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Inventori", color = AccentText) },
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
                        val context = LocalContext.current

                        IconButton(onClick = { showMenu = true }) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(R.drawable.excel)
                                    .build(),
                                contentDescription = "More",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = { /* Handle CSV export */ }
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
                    contentColor = AccentText, // Selected tab text color
                    containerColor = PrimaryBlue // Background color
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
        containerColor = LightBackground
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTabIndex) {
                0 -> KartuStokScreen()
                1 -> PembelianScreen()
            }
        }
    }
}

@Composable
fun KartuStokScreen() {
    val context = LocalContext.current
    val viewModel: ProductViewModel = viewModel(
        factory = ProductViewModelFactory(AppDatabase.getDatabase(context))
    )
    val products by viewModel.products.observeAsState(initial = emptyList())
    var showFilterDropdown by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Semua") }

    LaunchedEffect(products) {
        Log.d("TokoFafa", "Kartu Stok products updated, size: ${products.size}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            OutlinedButton(
                onClick = { showFilterDropdown = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DarkText
                )
            ) {
                Text(selectedFilter, color = DarkText)
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
                ProductItem(productWithSupplier = productWithSupplier)
            }
        }
    }
}

@Composable
fun ProductItem(productWithSupplier: ProductWithSupplier) {
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
                text = "Minuman",
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
    }
    Divider(
        color = DarkText.copy(alpha = 0.1f),
        thickness = 1.dp
    )
}

@Composable
fun PembelianScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Pembelian (To be implemented)",
            fontSize = 16.sp,
            color = DarkText
        )
    }
}