package com.example.tokofafa.produk

import android.Manifest
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tokofafa.CustomCaptureActivity
import com.example.tokofafa.R
import com.example.tokofafa.database.AppDatabase
import com.example.tokofafa.entities.Product
import com.example.tokofafa.entities.Supplier
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    productId: Long
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val coroutineScope = rememberCoroutineScope()

    // State for form fields
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var supplierId by remember { mutableStateOf<Int?>(null) }
    var basePrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var showSupplierDropdown by remember { mutableStateOf(false) }

    // State for suppliers list
    val suppliers by db.supplierDao().getAll().observeAsState(initial = emptyList())

    // Log suppliers for debugging
    LaunchedEffect(suppliers) {
        Log.d("TokoFafa", "Suppliers loaded: ${suppliers.size} - ${suppliers.map { it.name }}")
    }

    // Load existing product if editing
    LaunchedEffect(productId) {
        if (productId != -1L) {
            try {
                coroutineScope.launch(Dispatchers.IO) {
                    db.productDao().getProductById(productId)?.let { product ->
                        Log.d("TokoFafa", "Loading product: ${product.name}")
                        withContext(Dispatchers.Main) {
                            name = product.name ?: ""
                            description = product.description ?: ""
                            sku = product.sku ?: ""
                            barcode = product.barcode ?: ""
                            supplierId = product.supplierId
                            basePrice = product.basePrice.toString()
                            sellingPrice = product.sellingPrice.toString()
                            stock = product.stock.toString()
                            photoUri = product.photoUri
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat produk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher for picking a photo from the gallery
    val photoFile = remember { File(context.filesDir, "product_${productId}_${System.currentTimeMillis()}.png") }
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    FileOutputStream(photoFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                photoUri = photoFile.absolutePath
                Log.d("TokoFafa", "Product photo saved at: $photoUri")
            } catch (e: Exception) {
                Log.e("TokoFafa", "Error saving product photo", e)
                Toast.makeText(context, "Gagal menyimpan foto: ${e.message}", Toast.LENGTH_SHORT).show()
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
            Log.w("TokoFafa", "READ_EXTERNAL_STORAGE permission denied")
            Toast.makeText(context, "Izin akses media ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    // Custom contract for launching the CustomCaptureActivity
    val customScanContract = object : androidx.activity.result.contract.ActivityResultContract<ScanOptions, String?>() {
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

    // Barcode scanner launcher using the custom contract
    val scanLauncher = rememberLauncherForActivityResult(customScanContract) { result ->
        if (result != null) {
            barcode = result
            Log.d("TokoFafa", "Barcode scanned: $result")
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
            Log.w("TokoFafa", "CAMERA permission denied")
            Toast.makeText(context, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == -1L) "Tambah Produk" else "Edit Produk") },
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
                    if (productId != -1L) {
                        TextButton(onClick = { navController.navigateUp() }) {
                            Text("BATAL", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
            AsyncImage(
                model = "https://cdn-icons-png.flaticon.com/512/679/679922.png",
                contentDescription = "Product Icon",
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.CenterHorizontally),
                placeholder = painterResource(R.drawable.ic_placeholder),
                error = painterResource(R.drawable.ic_error)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (photoUri != null && File(photoUri).exists()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Product Photo",
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp),
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    error = painterResource(R.drawable.ic_error)
                )
            } else {
                Log.d("TokoFafa", "Product photo does not exist: $photoUri")
            }

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        photoPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    } else {
                        pickPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
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

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Produk") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("SKU") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
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
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                IconButton(
                    onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    AsyncImage(
                        model = "https://cdn-icons-png.flaticon.com/512/2910/2910249.png",
                        contentDescription = "Scan Barcode",
                        modifier = Modifier.size(24.dp),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        error = painterResource(R.drawable.ic_error)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Supplier Dropdown
            if (suppliers.isEmpty()) {
                Text(
                    text = "Tidak ada supplier. Tambah supplier terlebih dahulu.",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = { navController.navigate("supplier") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text("Tambah Supplier")
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = suppliers.find { it.id == supplierId }?.name ?: "Pilih Supplier",
                        onValueChange = { },
                        label = { Text("Supplier") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showSupplierDropdown = true }) {
                                AsyncImage(
                                    model = "https://cdn-icons-png.flaticon.com/512/2985/2985150.png",
                                    contentDescription = "Dropdown",
                                    modifier = Modifier.size(24.dp),
                                    placeholder = painterResource(R.drawable.ic_placeholder),
                                    error = painterResource(R.drawable.ic_error)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    DropdownMenu(
                        expanded = showSupplierDropdown,
                        onDismissRequest = { showSupplierDropdown = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suppliers.forEach { supplier ->
                            DropdownMenuItem(
                                text = { Text(supplier.name) },
                                onClick = {
                                    supplierId = supplier.id
                                    showSupplierDropdown = false
                                    Log.d("TokoFafa", "Selected supplier: ${supplier.name} (ID: ${supplier.id})")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = basePrice,
                onValueChange = { basePrice = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Harga Pokok") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = sellingPrice,
                onValueChange = { sellingPrice = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Harga Jual") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = stock,
                onValueChange = { stock = it.filter { char -> char.isDigit() } },
                label = { Text("Jumlah") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ex: PCS") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Nama produk harus diisi", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (suppliers.isNotEmpty() && supplierId == null) {
                        Toast.makeText(context, "Pilih supplier terlebih dahulu", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val product = Product(
                        id = if (productId == -1L) 0L else productId, // Use 0L for new products to let Room auto-generate
                        name = name,
                        description = description,
                        sku = sku,
                        barcode = barcode,
                        supplierId = supplierId,
                        basePrice = basePrice.toDoubleOrNull() ?: 0.0,
                        sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                        stock = stock.toIntOrNull() ?: 0,
                        photoUri = photoUri
                    )
                    try {
                        coroutineScope.launch(Dispatchers.IO) {
                            if (productId == -1L) {
                                db.productDao().insert(product)
                                Log.d("TokoFafa", "Inserted new product: ${product.name}")
                            } else {
                                db.productDao().update(product)
                                Log.d("TokoFafa", "Updated product: ${product.name}")
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Produk disimpan", Toast.LENGTH_SHORT).show()
                                navController.navigateUp()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Gagal menyimpan produk: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SIMPAN")
            }
        }
    }
}