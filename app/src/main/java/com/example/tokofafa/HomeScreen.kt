package com.example.tokofafa

import android.Manifest
import android.os.Build
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateStore: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // State for form fields
    var namaToko by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }
    var kota by remember { mutableStateOf("") }
    var provinsi by remember { mutableStateOf("") }
    var telepon by remember { mutableStateOf("") }

    // State for logo image URI (stored locally)
    var logoUri by remember { mutableStateOf<String?>(null) }
    val logoFile = File(context.filesDir, "logo.png") // Internal storage file for logo

    // Load logo from local storage if it exists
    LaunchedEffect(Unit) {
        if (logoFile.exists()) {
            logoUri = logoFile.absolutePath
        }
    }

    // Launcher for picking an image from the gallery
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            // Copy the selected image to internal storage
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val outputStream = FileOutputStream(logoFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                logoUri = logoFile.absolutePath // Update the logo URI
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Permission launcher for READ_EXTERNAL_STORAGE (needed for API < 33)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            // Handle permission denied (e.g., show a message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Toko Saya") },
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
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Store Icon (Loaded from URL using Coil)
            AsyncImage(
                model = "https://cdn-icons-png.flaticon.com/512/2331/2331970.png",
                contentDescription = "Store Icon",
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Store Name
            Text(
                text = "Toko Saya",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input Fields
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

            // Display the selected logo (if any)
            if (logoUri != null) {
                AsyncImage(
                    model = logoUri,
                    contentDescription = "Selected Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 8.dp)
                )
            }

            // Logo Button
            Button(
                onClick = {
                    // Check if permission is needed (API < 33)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    } else {
                        // API 33+ doesn't need permission for PickVisualMedia
                        pickImageLauncher.launch(
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
                    Text("LOGO")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Maksimal 1 Mb", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Create Store Button
            Button(
                onClick = onCreateStore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFA500),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Buat Toko")
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
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(label, color = Color.Gray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Gray,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() })
        )
    }
}