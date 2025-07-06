package com.example.tokofafa.inventori

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.example.tokofafa.entities.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoriScreen(navController: NavController) {
    val context = LocalContext.current
    var showFilterDropdown by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var showMenu by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("KARTU STOK", "PEMBELIAN")
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }

    // Storage permission launcher for CSV export
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            exportToCsv(context, transactions)
        } else {
            Toast.makeText(context, "Izin penyimpanan ditolak", Toast.LENGTH_SHORT).show()
        }
    }

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
                                onClick = {
                                    showMenu = false
                                    if (selectedTabIndex == 1 && transactions.isEmpty()) {
                                        Toast.makeText(context, "Tidak ada transaksi untuk diekspor", Toast.LENGTH_SHORT).show()
                                    } else if (selectedTabIndex == 1) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            exportToCsv(context, transactions)
                                        } else {
                                            storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        }
                                    } else {
                                        Toast.makeText(context, "Ekspor CSV hanya tersedia di tab Pembelian", Toast.LENGTH_SHORT).show()
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
        containerColor = LightBackground
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTabIndex) {
                0 -> KartuStokScreen()
                1 -> PembelianScreen(onTransactionsUpdated = { transactions = it })
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
fun PembelianScreen(onTransactionsUpdated: (List<Transaction>) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val fetchedTransactions = db.transactionDao().getAllTransactionsSync()
            withContext(Dispatchers.Main) {
                transactions = fetchedTransactions
                onTransactionsUpdated(fetchedTransactions)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada riwayat pembelian",
                    fontSize = 16.sp,
                    color = DarkText.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn {
                items(transactions) { transaction ->
                    TransactionItem(transaction = transaction)
                    Divider(
                        color = DarkText.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(transaction.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = transaction.productName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Text(
                text = "Jumlah: ${transaction.quantity}",
                fontSize = 14.sp,
                color = DarkText.copy(alpha = 0.7f)
            )
            Text(
                text = "Total: Rp ${
                    NumberFormat.getNumberInstance(Locale("id", "ID")).format(transaction.totalPrice)
                }",
                fontSize = 14.sp,
                color = DarkText.copy(alpha = 0.7f)
            )
            Text(
                text = "Waktu: $formattedDate",
                fontSize = 14.sp,
                color = DarkText.copy(alpha = 0.7f)
            )
        }
    }
}

fun exportToCsv(context: Context, transactions: List<Transaction>) {
    try {
        // Generate CSV content
        val csvContent = StringBuilder()
        // CSV Header
        csvContent.append("Barcode,ProductName,Quantity,UnitPrice,TotalPrice,Timestamp\n")

        // CSV Rows
        transactions.forEach { transaction ->
            val barcode = transaction.barcode.replace(",", "")
            val productName = transaction.productName.replace(",", "")
            val quantity = transaction.quantity.toString()
            val unitPrice = NumberFormat.getNumberInstance(Locale("id", "ID")).format(transaction.unitPrice)
            val totalPrice = NumberFormat.getNumberInstance(Locale("id", "ID")).format(transaction.totalPrice)
            val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(transaction.timestamp))

            csvContent.append("$barcode,$productName,$quantity,$unitPrice,$totalPrice,$timestamp\n")
        }

        // Generate filename with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "transactions_$timestamp.csv"

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