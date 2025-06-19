package com.example.tokofafa.supplier

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tokofafa.R
import com.example.tokofafa.SupplierFormDialog
import com.example.tokofafa.SupplierViewModel
import com.example.tokofafa.SupplierViewModelFactory
import com.example.tokofafa.database.AppDatabase
import com.example.tokofafa.entities.Supplier
import com.example.tokofafa.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierScreen(
    navController: NavController,
    viewModel: SupplierViewModel = viewModel(factory = SupplierViewModelFactory(AppDatabase.getDatabase(LocalContext.current)))
) {
    val context = LocalContext.current
    val supplierList by viewModel.suppliers.observeAsState(initial = emptyList())
    var showForm by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    var dropdownExpandedId by remember { mutableStateOf<Int?>(null) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var exportCsvTrigger by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("SUPPLIER")

    val onEdit: (Supplier) -> Unit = { supplier ->
        editingSupplier = supplier
        name = supplier.name
        email = supplier.email
        phone = supplier.phone
        address = supplier.address
        city = supplier.city
        province = supplier.province
        postalCode = supplier.postalCode
        showForm = true
    }

    val onDelete: (Supplier) -> Unit = { supplier ->
        viewModel.deleteSupplier(supplier)
        Toast.makeText(context, "Supplier dihapus", Toast.LENGTH_SHORT).show()
    }

    val filteredSuppliers = supplierList
        .sortedBy { it.id }
        .filter { supplier ->
            supplier.name.contains(searchText, ignoreCase = true) ||
                    supplier.address.contains(searchText, ignoreCase = true)
        }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                placeholder = { Text("Cari supplier...", color = AccentText) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = AccentText,
                                    unfocusedTextColor = AccentText,
                                    cursorColor = AccentGreen,
                                    focusedPlaceholderColor = AccentText,
                                    unfocusedPlaceholderColor = AccentText
                                )
                            )
                        } else {
                            Text("Supplier", color = AccentText)
                        }
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
                    actions = {
                        if (isSearchActive) {
                            IconButton(onClick = {
                                isSearchActive = false
                                searchText = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = AccentText)
                            }
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
                                    color = if (selectedTabIndex == index) AccentText else DarkText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSupplier = null
                    name = ""
                    email = ""
                    phone = ""
                    address = ""
                    city = ""
                    province = ""
                    postalCode = ""
                    showForm = true
                },
                containerColor = AccentGreen
            ) {
                Text("+", color = AccentText, fontSize = 24.sp)
            }
        },
        containerColor = LightBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(filteredSuppliers) { supplier ->
                    SupplierItem(
                        supplier = supplier,
                        onClick = { navController.navigate("supplier_info/${supplier.id}") },
                        onEdit = onEdit,
                        onDelete = onDelete,
                        expandedId = dropdownExpandedId,
                        onExpandChange = { dropdownExpandedId = it }
                    )
                }
            }
        }
    }

    if (exportCsvTrigger) {
        LaunchedEffect(exportCsvTrigger) {
            viewModel.exportToCsv(context) { file ->
                Toast.makeText(context, "CSV disimpan di:\n${file.name}", Toast.LENGTH_SHORT).show()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Bagikan file CSV ke:"))
                exportCsvTrigger = false
            }
        }
    }

    if (showForm) {
        SupplierFormDialog(
            isEditing = editingSupplier != null,
            name = name,
            onNameChange = { name = it },
            email = email,
            onEmailChange = { email = it },
            phone = phone,
            onPhoneChange = { phone = it },
            address = address,
            onAddressChange = { address = it },
            city = city,
            onCityChange = { city = it },
            province = province,
            onProvinceChange = { province = it },
            postalCode = postalCode,
            onPostalCodeChange = { postalCode = it },
            onDismiss = { showForm = false },
            onConfirm = {
                if (name.isBlank()) {
                    Toast.makeText(context, "Nama Supplier harus diisi", Toast.LENGTH_SHORT).show()
                    return@SupplierFormDialog
                }
                val supplier = Supplier(
                    id = editingSupplier?.id ?: 0,
                    name = name,
                    email = email,
                    phone = phone,
                    address = address,
                    city = city,
                    province = province,
                    postalCode = postalCode
                )
                if (editingSupplier != null) {
                    viewModel.updateSupplier(supplier)
                } else {
                    viewModel.addSupplier(supplier)
                }
                showForm = false
            }
        )
    }
}