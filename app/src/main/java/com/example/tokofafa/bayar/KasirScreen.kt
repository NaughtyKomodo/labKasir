package com.example.tokofafa.bayar

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
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

    // Storage permission launcher
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            saveReceiptToGallery(context, receiptText, cartItems)
        } else {
            Toast.makeText(context, "Izin penyimpanan ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kasir",
                        color = AccentText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
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
            // Scan Button with improved design
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Button(
                    onClick = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = AccentText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = "https://cdn-icons-png.flaticon.com/512/2910/2910249.png",
                            contentDescription = "Scan Barcode",
                            modifier = Modifier.size(28.dp),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            error = painterResource(R.drawable.ic_error)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Scan Barcode",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cart Items Section
            if (cartItems.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = "https://cdn-icons-png.flaticon.com/512/11329/11329060.png",
                            contentDescription = "Empty Cart",
                            modifier = Modifier.size(80.dp),
                            placeholder = painterResource(R.drawable.ic_placeholder),
                            error = painterResource(R.drawable.ic_error)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Keranjang Kosong",
                            color = DarkText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Scan barcode untuk menambah produk",
                            color = DarkText.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cartItems) { item ->
                            ModernCartItemRow(
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

                        // Total Section
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkText
                                    )
                                    Text(
                                        text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(cartItems.sumOf { it.unitPrice * it.quantity })}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Checkout Button
            Button(
                onClick = {
                    if (cartItems.isNotEmpty()) {
                        val total = cartItems.sumOf { it.unitPrice * it.quantity }
                        val receipt = buildString {
                            appendLine("=== STRUK PEMBELIAN ===")
                            appendLine("TOKO")
                            appendLine("${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
                            appendLine("================================")
                            cartItems.forEach { item ->
                                appendLine("${item.name}")
                                appendLine("${item.barcode}")
                                appendLine("${item.quantity} x Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.unitPrice)} = Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.unitPrice * item.quantity)}")
                                appendLine("--------------------------------")
                            }
                            appendLine("TOTAL: Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(total)}")
                            appendLine("================================")
                            appendLine("Terima kasih atas kunjungan Anda!")
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
                                // Don't clear cart here, clear after dialog is dismissed
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = cartItems.isNotEmpty()
            ) {
                Text(
                    "Selesai Transaksi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Modern Receipt Dialog
        if (showReceiptDialog) {
            ModernReceiptDialog(
                receiptText = receiptText,
                cartItems = cartItems,
                onDismiss = {
                    showReceiptDialog = false
                    cartItems = emptyList() // Clear cart when dialog is dismissed
                },
                onPrint = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveReceiptToGallery(context, receiptText, cartItems)
                    } else {
                        storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
            )
        }
    }
}

@Composable
fun ModernCartItemRow(item: CartItem, onQuantityChange: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText
                )
                Text(
                    text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.unitPrice)}",
                    fontSize = 14.sp,
                    color = DarkText.copy(alpha = 0.7f)
                )
                Text(
                    text = item.barcode,
                    fontSize = 12.sp,
                    color = DarkText.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
            }

            // Quantity Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Minus Button
                IconButton(
                    onClick = { onQuantityChange(item.quantity - 1) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.quantity > 1) PrimaryBlue.copy(alpha = 0.1f)
                            else Color.Red.copy(alpha = 0.1f)
                        )
                ) {
                    Text(
                        text = "-",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.quantity > 1) PrimaryBlue else Color.Red
                    )
                }

                // Quantity Display
                Text(
                    text = item.quantity.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                    modifier = Modifier.widthIn(min = 24.dp),
                    textAlign = TextAlign.Center
                )

                // Plus Button
                IconButton(
                    onClick = { onQuantityChange(item.quantity + 1) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "+",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
fun ModernReceiptDialog(
    receiptText: String,
    cartItems: List<CartItem>,
    onDismiss: () -> Unit,
    onPrint: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryBlue)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Struk Pembelian",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        AsyncImage(
                            model = "https://cdn-icons-png.flaticon.com/512/1828/1828778.png",
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Receipt Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    state = rememberLazyListState()
                ) {
                    item {
                        // Store Header
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TOKO MAKMUR",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Jl. Raya No. 123, Malang",
                                fontSize = 12.sp,
                                color = DarkText.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Telp: (0341) 123456",
                                fontSize = 12.sp,
                                color = DarkText.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Divider(color = DarkText.copy(alpha = 0.3f))

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()),
                                fontSize = 12.sp,
                                color = DarkText.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Items
                    items(cartItems) { item ->
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkText,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity} x Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.unitPrice)}",
                                    fontSize = 12.sp,
                                    color = DarkText.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.unitPrice * item.quantity)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkText,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = DarkText.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )
                            Text(
                                text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(cartItems.sumOf { it.unitPrice * it.quantity })}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = DarkText.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Terima kasih atas kunjungan Anda!",
                            fontSize = 12.sp,
                            color = DarkText.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryBlue
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(Color.Transparent),
                            width = 1.dp
                        )

                    ) {
                        Text("Tutup")
                    }

                    Button(
                        onClick = onPrint,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = AccentText
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            AsyncImage(
                                model = "https://cdn-icons-png.flaticon.com/512/3022/3022381.png",
                                contentDescription = "Print",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}

fun saveReceiptToGallery(context: android.content.Context, receiptText: String, cartItems: List<CartItem>) {
    try {
        val bitmap = createReceiptBitmap(receiptText, cartItems)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "struk_$timestamp.jpg"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TokoFafa")
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val tokoFafaDir = File(picturesDir, "TokoFafa")
            if (!tokoFafaDir.exists()) {
                tokoFafaDir.mkdirs()
            }

            val file = File(tokoFafaDir, filename)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }

            // Scan file to make it visible in gallery
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = android.net.Uri.fromFile(file)
            context.sendBroadcast(intent)
        }

        Toast.makeText(context, "Struk berhasil disimpan ke galeri", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e("TokoFafa", "Error saving receipt", e)
        Toast.makeText(context, "Gagal menyimpan struk", Toast.LENGTH_SHORT).show()
    }
}

fun createReceiptBitmap(receiptText: String, cartItems: List<CartItem>): Bitmap {
    val width = 600
    val padding = 40
    val lineHeight = 40
    val headerHeight = 200 // Initial estimate for header
    val footerHeight = 100 // Initial estimate for footer
    val extraBuffer = 100 // Add extra buffer to prevent cutoff

    // Paint objects
    val titlePaint = Paint().apply {
        color = Color.Black.toArgb()
        textSize = 48f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val headerPaint = Paint().apply {
        color = Color.Black.toArgb()
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    val normalPaint = Paint().apply {
        color = Color.Black.toArgb()
        textSize = 28f
        textAlign = Paint.Align.LEFT
    }

    val pricePaint = Paint().apply {
        color = Color.Black.toArgb()
        textSize = 28f
        textAlign = Paint.Align.RIGHT
    }

    val totalPaint = Paint().apply {
        color = Color.Black.toArgb()
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.RIGHT
    }

    // Calculate height dynamically
    var totalHeight = padding.toFloat() // Start with top padding

    // Header height
    totalHeight += 60f // "TOKO FAFA"
    totalHeight += 50f // Address
    totalHeight += 40f // Phone
    totalHeight += 60f // Date
    totalHeight += 40f // Separator line
    totalHeight += 40f // Space after separator

    // Calculate item height dynamically
    val maxTextWidth = width - 2 * padding - 20 // Account for padding and small margin
    cartItems.forEach { item ->
        // Measure product name height (handle wrapping)
        val bounds = android.graphics.Rect()
        normalPaint.getTextBounds(item.name, 0, item.name.length, bounds)
        val lines = calculateTextLines(item.name, normalPaint, maxTextWidth)
        totalHeight += lines * 40f // Height for product name (40 per line)

        // Quantity and price lines
        totalHeight += 40f // Quantity line
        totalHeight += 50f // Space after item
    }

    // Footer height
    totalHeight += 20f // Space before total separator
    totalHeight += 40f // Total separator line
    totalHeight += 50f // Total line
    totalHeight += 60f // Space after total
    totalHeight += 40f // Bottom separator line
    totalHeight += 50f // Thank you message
    totalHeight += padding.toFloat() // Bottom padding
    totalHeight += extraBuffer.toFloat() // Extra buffer to prevent cutoff

    // Create bitmap with calculated height
    val bitmap = Bitmap.createBitmap(width, totalHeight.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background
    canvas.drawColor(Color.White.toArgb())

    var yPosition = padding.toFloat()

    // Store Header
    yPosition += 60f
    canvas.drawText("TOKO MAKMUR", width / 2f, yPosition, titlePaint)
    yPosition += 50f
    canvas.drawText("Jl. Raya No. 123, Malang", width / 2f, yPosition, headerPaint)
    yPosition += 40f
    canvas.drawText("Telp: (0341) 123456", width / 2f, yPosition, headerPaint)
    yPosition += 60f

    // Date
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    canvas.drawText(dateFormat.format(Date()), width / 2f, yPosition, headerPaint)
    yPosition += 60f

    // Separator line
    canvas.drawLine(padding.toFloat(), yPosition, (width - padding).toFloat(), yPosition, normalPaint)
    yPosition += 40f

    // Items
    cartItems.forEach { item ->
        // Product name (handle wrapping)
        val lines = splitTextIntoLines(item.name, normalPaint, maxTextWidth)
        lines.forEach { line ->
            canvas.drawText(line, (padding + 10).toFloat(), yPosition, normalPaint)
            yPosition += 40f
        }

        // Quantity and price calculation
        val qtyPriceText = "${item.quantity} x Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.unitPrice)}"
        canvas.drawText(qtyPriceText, (padding + 10).toFloat(), yPosition, normalPaint)

        // Item total
        val itemTotal = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.unitPrice * item.quantity)}"
        canvas.drawText(itemTotal, (width - padding - 10).toFloat(), yPosition, pricePaint)
        yPosition += 50f
    }

    yPosition += 20f
    // Total separator line
    canvas.drawLine(padding.toFloat(), yPosition, (width - padding).toFloat(), yPosition, normalPaint)
    yPosition += 50f

    // Total
    canvas.drawText("TOTAL:", (padding + 120).toFloat(), yPosition, totalPaint)
    val grandTotal = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(cartItems.sumOf { it.unitPrice * it.quantity })}"
    canvas.drawText(grandTotal, (width - padding - 10).toFloat(), yPosition, totalPaint)
    yPosition += 60f

    // Bottom separator line
    canvas.drawLine(padding.toFloat(), yPosition, (width - padding).toFloat(), yPosition, normalPaint)
    yPosition += 50f

    // Thank you message
    canvas.drawText("Terima kasih atas kunjungan Anda!", width / 2f, yPosition, headerPaint)

    return bitmap
}

// Helper function to calculate the number of lines needed for text
fun calculateTextLines(text: String, paint: Paint, maxWidth: Int): Int {
    if (text.isEmpty()) return 1
    val words = text.split(" ")
    var currentLineWidth = 0f
    var lineCount = 1

    words.forEach { word ->
        val bounds = android.graphics.Rect()
        paint.getTextBounds(word, 0, word.length, bounds)
        val wordWidth = bounds.width()

        if (currentLineWidth + wordWidth > maxWidth) {
            lineCount++
            currentLineWidth = wordWidth + paint.measureText(" ") // Account for space
        } else {
            currentLineWidth += wordWidth + paint.measureText(" ")
        }
    }
    return lineCount
}

// Helper function to split text into lines for rendering
fun splitTextIntoLines(text: String, paint: Paint, maxWidth: Int): List<String> {
    val lines = mutableListOf<String>()
    if (text.isEmpty()) return listOf("")

    val words = text.split(" ")
    var currentLine = StringBuilder()
    var currentLineWidth = 0f

    words.forEach { word ->
        val bounds = android.graphics.Rect()
        paint.getTextBounds(word, 0, word.length, bounds)
        val wordWidth = bounds.width()

        if (currentLineWidth + wordWidth > maxWidth) {
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder()
                currentLineWidth = 0f
            }
            currentLine.append(word)
            currentLineWidth = wordWidth + paint.measureText(" ")
        } else {
            if (currentLine.isNotEmpty()) {
                currentLine.append(" ")
                currentLineWidth += paint.measureText(" ")
            }
            currentLine.append(word)
            currentLineWidth += wordWidth
        }
    }

    if (currentLine.isNotEmpty()) {
        lines.add(currentLine.toString())
    }

    return lines
}