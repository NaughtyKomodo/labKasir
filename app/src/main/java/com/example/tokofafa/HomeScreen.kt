package com.example.tokofafa

import android.Manifest
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tokofafa.database.AppDatabase
import com.example.tokofafa.entities.Store
import com.example.tokofafa.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateStore: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val store by db.storeDao().getStore().collectAsStateWithLifecycle(initialValue = null)

    // State for form fields
    var namaToko by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }
    var kota by remember { mutableStateOf("") }
    var provinsi by remember { mutableStateOf("") }
    var telepon by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf<String?>(null) }

    // Load existing store data
    LaunchedEffect(store) {
        store?.let { storeData ->
            Log.d("TokoFafa", "Loading store data: ${storeData.name}")
            namaToko = storeData.name ?: ""
            alamat = storeData.address ?: ""
            kota = storeData.city ?: ""
            provinsi = storeData.province ?: ""
            telepon = storeData.phone ?: ""
            logoUri = storeData.logoUri
        }
    }

    // Launcher for picking an image from the gallery
    val logoFile = remember { File(context.filesDir, "logo_${System.currentTimeMillis()}.png") }
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    FileOutputStream(logoFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                logoUri = logoFile.absolutePath
                Log.d("TokoFafa", "Logo saved at: $logoUri")
            } catch (e: Exception) {
                Log.e("TokoFafa", "Error saving logo", e)
                Toast.makeText(context, "Gagal menyimpan logo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission launcher for READ_MEDIA_IMAGES (API 33+) or READ_EXTERNAL_STORAGE (API < 33)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            Log.w("TokoFafa", "Storage permission denied")
            Toast.makeText(context, "Izin akses media ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Toko Saya", color = AccentText) },
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
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = logoUri ?: "https://cdn-icons-png.flaticon.com/512/2331/2331970.png",
                contentDescription = "Store Icon",
                modifier = Modifier.size(64.dp),
                placeholder = painterResource(R.drawable.ic_placeholder),
                error = painterResource(R.drawable.ic_error)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Toko Saya",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileTextField(
                label = "Nama Toko",
                value = namaToko,
                onValueChange = { namaToko = it },
                onDone = { keyboardController?.hide() }
            )
            ProfileTextField(
                label = "Alamat",
                value = alamat,
                onValueChange = { alamat = it },
                onDone = { keyboardController?.hide() }
            )
            ProfileTextField(
                label = "Kota",
                value = kota,
                onValueChange = { kota = it },
                onDone = { keyboardController?.hide() }
            )
            ProfileTextField(
                label = "Provinsi",
                value = provinsi,
                onValueChange = { provinsi = it },
                onDone = { keyboardController?.hide() }
            )
            ProfileTextField(
                label = "Telepon",
                value = telepon,
                onValueChange = { telepon = it },
                onDone = { keyboardController?.hide() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = AccentText
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pilih Logo")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Maksimal 1 Mb", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (namaToko.isBlank()) {
                        Log.w("TokoFafa", "Nama toko harus diisi")
                        Toast.makeText(context, "Nama toko harus diisi", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val storeData = Store(
                        id = store?.id ?: 0L,
                        name = namaToko,
                        address = alamat,
                        city = kota,
                        province = provinsi,
                        phone = telepon,
                        logoUri = logoUri
                    )
                    try {
                        coroutineScope.launch(Dispatchers.IO) {
                            if (store == null) {
                                db.storeDao().insert(storeData)
                                Log.d("TokoFafa", "Inserted new store: ${storeData.name}")
                            } else {
                                db.storeDao().update(storeData)
                                Log.d("TokoFafa", "Updated store: ${storeData.name}")
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Toko disimpan", Toast.LENGTH_SHORT).show()
                                onCreateStore()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TokoFafa", "Error saving store", e)
                        Toast.makeText(context, "Gagal menyimpan toko: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = AccentText
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Toko")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = DarkText
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(label, color = DarkText.copy(alpha = 0.5f)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = LightBackground,
                unfocusedContainerColor = LightBackground,
                focusedIndicatorColor = PrimaryBlue,
                unfocusedIndicatorColor = DarkText.copy(alpha = 0.5f),
                focusedTextColor = DarkText,
                unfocusedTextColor = DarkText,
                focusedPlaceholderColor = DarkText.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = DarkText.copy(alpha = 0.5f)
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() })
        )
    }
}