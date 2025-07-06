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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tokofafa.CustomCaptureActivity
import com.example.tokofafa.R
import com.example.tokofafa.database.AppDatabase
import com.example.tokofafa.entities.Product
import com.example.tokofafa.entities.Supplier
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ProductFormState(
    val name: String = "",
    val description: String = "",
    val sku: String = "",
    val barcode: String = "",
    val supplierId: Int? = null,
    val basePrice: String = "",
    val sellingPrice: String = "",
    val stock: String = "",
    val photoUri: String? = null
)

class AddProductViewModel(private val db: AppDatabase) : ViewModel() {
    private val _suppliers = MutableStateFlow<List<Supplier>>(emptyList())
    val suppliers: StateFlow<List<Supplier>> = _suppliers.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _suppliers.value = db.supplierDao().getAllList()
        }
    }

    suspend fun getProductById(id: Long): Product? {
        return db.productDao().getProductById(id)
    }

    fun insertOrUpdate(product: Product, isNew: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isNew) {
                db.productDao().insert(product)
                Log.d("TokoFafa", "Inserted new product: ${product.name}")
            } else {
                db.productDao().update(product)
                Log.d("TokoFafa", "Updated product: ${product.name}")
            }
        }
    }
}

class AddProductViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddProductViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    productId: Long
) {
    val context = LocalContext.current
    val viewModel: AddProductViewModel = viewModel(
        factory = AddProductViewModelFactory(AppDatabase.getDatabase(context))
    )
    val coroutineScope = rememberCoroutineScope()
    var formState by remember { mutableStateOf(ProductFormState()) }
    var showSupplierDropdown by remember { mutableStateOf(false) }
    val suppliers by viewModel.suppliers.collectAsState()

    LaunchedEffect(suppliers) {
        Log.d("TokoFafa", "Suppliers loaded: ${suppliers.size} - ${suppliers.map { it.name }}")
    }

    LaunchedEffect(productId) {
        if (productId != -1L) {
            try {
                coroutineScope.launch(Dispatchers.IO) {
                    viewModel.getProductById(productId)?.let { product ->
                        Log.d("TokoFafa", "Loading product: ${product.name}")
                        withContext(Dispatchers.Main) {
                            formState = formState.copy(
                                name = product.name ?: "",
                                description = product.description ?: "",
                                sku = product.sku ?: "",
                                barcode = product.barcode ?: "",
                                supplierId = product.supplierId,
                                basePrice = product.basePrice.toString(),
                                sellingPrice = product.sellingPrice.toString(),
                                stock = product.stock.toString(),
                                photoUri = product.photoUri
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat produk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                formState = formState.copy(photoUri = photoFile.absolutePath)
                Log.d("TokoFafa", "Product photo saved at: ${formState.photoUri}")
            } catch (e: Exception) {
                Log.e("TokoFafa", "Error saving product photo", e)
                Toast.makeText(context, "Gagal menyimpan foto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

    val scanLauncher = rememberLauncherForActivityResult(customScanContract) { result ->
        if (result != null) {
            formState = formState.copy(barcode = result)
            Log.d("TokoFafa", "Barcode scanned: $result")
        }
    }

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
                    .align( Alignment.CenterHorizontally),
                placeholder = painterResource(R.drawable.ic_placeholder),
                error = painterResource(R.drawable.ic_error)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (formState.photoUri != null && File(formState.photoUri).exists()) {
                AsyncImage(
                    model = formState.photoUri,
                    contentDescription = "Product Photo",
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp),
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    error = painterResource(R.drawable.ic_error)
                )
            } else {
                Log.d("TokoFafa", "Product photo does not exist: ${formState.photoUri}")
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
                value = formState.name,
                onValueChange = { formState = formState.copy(name = it) },
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
                value = formState.description,
                onValueChange = { formState = formState.copy(description = it) },
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
                value = formState.sku,
                onValueChange = { formState = formState.copy(sku = it) },
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
                    value = formState.barcode,
                    onValueChange = { formState = formState.copy(barcode = it) },
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
                        value = suppliers.find { it.id == formState.supplierId }?.name ?: "Pilih Supplier",
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
                                    formState = formState.copy(supplierId = supplier.id)
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
                value = formState.basePrice,
                onValueChange = { formState = formState.copy(basePrice = it.filter { char -> char.isDigit() || char == '.' }) },
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
                value = formState.sellingPrice,
                onValueChange = { formState = formState.copy(sellingPrice = it.filter { char -> char.isDigit() || char == '.' }) },
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
                value = formState.stock,
                onValueChange = { formState = formState.copy(stock = it.filter { char -> char.isDigit() }) },
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
                    if (formState.name.isBlank()) {
                        Toast.makeText(context, "Nama produk harus diisi", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (suppliers.isNotEmpty() && formState.supplierId == null) {
                        Toast.makeText(context, "Pilih supplier terlebih dahulu", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val product = Product(
                        id = if (productId == -1L) 0L else productId,
                        name = formState.name,
                        description = formState.description,
                        sku = formState.sku,
                        barcode = formState.barcode,
                        supplierId = formState.supplierId,
                        basePrice = formState.basePrice.toDoubleOrNull() ?: 0.0,
                        sellingPrice = formState.sellingPrice.toDoubleOrNull() ?: 0.0,
                        stock = formState.stock.toIntOrNull() ?: 0,
                        photoUri = formState.photoUri
                    )
                    try {
                        viewModel.insertOrUpdate(product, productId == -1L)
                        Toast.makeText(context, "Produk disimpan", Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
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