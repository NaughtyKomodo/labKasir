package com.example.tokofafa.produk

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tokofafa.CustomCaptureActivity
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    productId: Long
) {
    val context = LocalContext.current
    val localStorage = remember { LocalStorage(context) }

    // State for form fields
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var basePrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) } // State for the photo URI

    // Load existing product if editing
    LaunchedEffect(productId) {
        if (productId != -1L) {
            val product = localStorage.getProducts().find { it.id == productId }
            product?.let {
                name = it.name
                description = it.description
                sku = it.sku
                barcode = it.barcode
                supplier = it.supplier
                basePrice = it.basePrice.toString()
                sellingPrice = it.sellingPrice.toString()
                stock = it.stock.toString()
                photoUri = it.photoUri // Load the existing photo URI
            }
        }
    }

    // Launcher for picking a photo from the gallery
    val photoFile = File(context.filesDir, "product_${productId}_${System.currentTimeMillis()}.png") // Unique file for the photo
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            // Copy the selected photo to internal storage
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val outputStream = FileOutputStream(photoFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                photoUri = photoFile.absolutePath // Update the photo URI
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Permission launcher for READ_EXTERNAL_STORAGE (needed for API < 33)
    val photoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            // Handle permission denied (e.g., show a message)
        }
    }

    // Custom contract for launching the CustomCaptureActivity
    val customScanContract = object : ActivityResultContract<ScanOptions, String?>() {
        override fun createIntent(context: android.content.Context, input: ScanOptions): Intent {
            val intent = Intent(context, CustomCaptureActivity::class.java)
            // Configure the intent with ZXing extras
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

    // Barcode scanner launcher using the custom contract
    val scanLauncher = rememberLauncherForActivityResult(customScanContract) { result ->
        if (result != null) {
            barcode = result // Auto-fill barcode field
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.EAN_13, ScanOptions.EAN_8) // ISBN formats
                setPrompt("Scan a barcode")
                setBeepEnabled(true)
                setCameraId(0) // Use the back camera
            }
            scanLauncher.launch(options)
        } else {
            // Handle permission denied
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == -1L) "Tambah Produk" else "Edit Produk") },
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
                    if (productId != -1L) {
                        TextButton(onClick = { navController.navigateUp() }) {
                            Text("BATAL", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Product Icon
            AsyncImage(
                model = "https://cdn-icons-png.flaticon.com/512/679/679922.png", // Product icon
                contentDescription = "Product Icon",
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display the selected photo (if any)
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Product Photo",
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                )
            }

            // Photo Upload Button
            Button(
                onClick = {
                    // Check if permission is needed (API < 33)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        photoPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    } else {
                        // API 33+ doesn't need permission for PickVisualMedia
                        pickPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1EB980),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("UPLOAD FOTO BARANG")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Maksimal 1 Mb", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Produk") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("SKU") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    AsyncImage(
                        model = "https://cdn-icons-png.flaticon.com/512/2910/2910249.png", // Barcode scanner icon
                        contentDescription = "Scan Barcode",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = supplier,
                onValueChange = { supplier = it },
                label = { Text("Supplier") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    AsyncImage(
                        model = "https://cdn-icons-png.flaticon.com/512/992/992651.png", // Add icon for supplier
                        contentDescription = "Add Supplier",
                        modifier = Modifier.size(24.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = basePrice,
                onValueChange = { basePrice = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Harga Pokok") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = sellingPrice,
                onValueChange = { sellingPrice = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Harga Jual") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = stock,
                onValueChange = { stock = it.filter { char -> char.isDigit() } },
                label = { Text("Jumlah") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ex: PCS") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    val product = Product(
                        id = if (productId == -1L) System.currentTimeMillis() else productId,
                        name = name,
                        description = description,
                        sku = sku,
                        barcode = barcode,
                        supplier = supplier,
                        basePrice = basePrice.toDoubleOrNull() ?: 0.0,
                        sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                        stock = stock.toIntOrNull() ?: 0,
                        photoUri = photoUri // Include the photo URI
                    )
                    if (productId == -1L) {
                        localStorage.addProduct(product)
                    } else {
                        localStorage.updateProduct(product)
                    }
                    navController.navigateUp()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1EB980),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SIMPAN")
            }
        }
    }
}