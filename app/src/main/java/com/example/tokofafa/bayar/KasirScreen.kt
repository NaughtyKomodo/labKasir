package com.example.tokofafa.bayar

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tokofafa.CustomCaptureActivity
import com.example.tokofafa.R
import com.example.tokofafa.database.AppDatabase
import com.example.tokofafa.entities.Transaction
import com.example.tokofafa.ui.theme.*
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

data class CartItem(
    val barcode: String,
    val name: String,
    val unitPrice: Double,
    var quantity: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val coroutineScope = rememberCoroutineScope()
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var receiptText by remember { mutableStateOf("") }

    // Barcode scanner launcher
    val scanContract = object : androidx.activity.result.contract.ActivityResultContract<ScanOptions, String?>() {
        override fun createIntent(context: android.content.Context, input: ScanOptions): Intent {
            val intent = Intent(context, CustomCaptureActivity::class.java)
            intent.action = "com.google.zxing.client.android.SCAN"
            intent.putExtra("SCAN_FORMATS", "EAN_13,EAN_8")
            intent.putExtra("PROMPT_MESSAGE", "Scan a barcode")
            intent.putExtra("BEEP_ENABLED", true)
            intent.putExtra("SCAN_CAMERA_ID", 0)
            return intent
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            if (resultCode == android.app.Activity.RESULT_OK && intent != null) {
                return intent.getStringExtra("SCAN_RESULT")
            }
            return null
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(scanContract) { result ->
        if (result != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val product = db.productDao().getProductByBarcode(result)
                withContext(Dispatchers.Main) {
                    if (product != null) {
                        val existingItem = cartItems.find { it.barcode == result }
                        if (existingItem != null) {
                            cartItems = cartItems.map {
                                if (it.barcode == result) it.copy(quantity = it.quantity + 1) else it
                            }
                        } else {
                            cartItems = cartItems + CartItem(
                                barcode = result,
                                name = product.name ?: "Unknown",
                                unitPrice = product.sellingPrice,
                                quantity = 1
                            )
                        }
                        Log.d("TokoFafa", "Added product to cart: ${product.name}")
                    } else {
                        Toast.makeText(context, "Produk tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.EAN_13, ScanOptions.EAN_8)
                setPrompt("Scan a barcode")
                setBeepEnabled(true)
                setCameraId(0)
                setOrientationLocked(true)
            }
            scanLauncher.launch(options)
        } else {
            Toast.makeText(context, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasir", color = AccentText) },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = AccentText
                )
            )
        },
        containerColor = LightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Button(
                onClick = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = AccentText
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = "https://cdn-icons-png.flaticon.com/512/2910/2910249.png",
                        contentDescription = "Scan Barcode",
                        modifier = Modifier.size(24.dp),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        error = painterResource(R.drawable.ic_error)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Barcode")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (cartItems.isEmpty()) {
                Text(
                    text = "Belum ada produk di keranjang",
                    color = DarkText,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn {
                    items(cartItems) { item ->
                        CartItemRow(
                            item = item,
                            onQuantityChange = { newQuantity ->
                                if (newQuantity > 0) {
                                    cartItems = cartItems.map {
                                        if (it.barcode == item.barcode) it.copy(quantity = newQuantity) else it
                                    }
                                } else {
                                    cartItems = cartItems.filter { it.barcode != item.barcode }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (cartItems.isNotEmpty()) {
                        val total = cartItems.sumOf { it.unitPrice * it.quantity }
                        val receipt = buildString {
                            appendLine("=== Struk Pembelian ===")
                            cartItems.forEach { item ->
                                appendLine("${item.barcode} - ${item.name} - ${item.quantity} - Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.unitPrice)}")
                            }
                            appendLine("Total Dibayarkan: Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(total)}")
                        }
                        receiptText = receipt
                        showReceiptDialog = true

                        // Save transactions
                        coroutineScope.launch(Dispatchers.IO) {
                            cartItems.forEach { item ->
                                db.transactionDao().insert(
                                    Transaction(
                                        barcode = item.barcode,
                                        productName = item.name,
                                        quantity = item.quantity,
                                        unitPrice = item.unitPrice,
                                        totalPrice = item.unitPrice * item.quantity,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                            withContext(Dispatchers.Main) {
                                cartItems = emptyList() // Clear cart
                            }
                        }
                    } else {
                        Toast.makeText(context, "Keranjang kosong", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = AccentText
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Selesai")
            }
        }

        if (showReceiptDialog) {
            AlertDialog(
                onDismissRequest = { showReceiptDialog = false },
                title = { Text("Struk Pembelian", color = DarkText) },
                text = {
                    Text(
                        text = receiptText,
                        color = DarkText,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { showReceiptDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = AccentText)
                    ) {
                        Text("Tutup")
                    }
                },
                containerColor = LightBackground,
                titleContentColor = DarkText
            )
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, onQuantityChange: (Int) -> Unit) {
    var quantityText by remember { mutableStateOf(item.quantity.toString()) }

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
                text = item.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Text(
                text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.unitPrice)}",
                fontSize = 14.sp,
                color = DarkText
            )
        }
        OutlinedTextField(
            value = quantityText,
            onValueChange = { newValue ->
                quantityText = newValue.filter { it.isDigit() }
                val newQuantity = newValue.toIntOrNull() ?: 0
                onQuantityChange(newQuantity)
            },
            label = { Text("Jumlah", color = DarkText) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(100.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DarkText,
                unfocusedTextColor = DarkText,
                focusedLabelColor = DarkText,
                unfocusedLabelColor = DarkText,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = DarkText.copy(alpha = 0.5f)
            )
        )
    }
    Divider(
        color = DarkText.copy(alpha = 0.1f),
        thickness = 1.dp
    )
}