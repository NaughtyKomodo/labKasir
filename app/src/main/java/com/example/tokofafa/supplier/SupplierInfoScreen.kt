package com.example.tokofafa

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tokofafa.database.AppDatabase
import com.example.tokofafa.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SupplierInfoScreen(
    navController: NavController,
    supplierId: Int,
    viewModel: SupplierViewModel = viewModel(factory = SupplierViewModelFactory(AppDatabase.getDatabase(LocalContext.current)))
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val suppliers by viewModel.suppliers.observeAsState(initial = emptyList())
    val supplier = suppliers.find { it.id == supplierId }

    BackHandler {
        navController.navigateUp()
    }

    if (supplier == null) {
        Text("Supplier tidak ditemukan", modifier = Modifier.padding(16.dp), color = DarkText)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informasi Supplier", color = AccentText) },
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
                backgroundColor = PrimaryBlue
            )
        },
        backgroundColor = LightBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = LightCard,
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = "https://cdn-icons-png.flaticon.com/512/3144/3144456.png",
                        contentDescription = "Supplier Icon",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(bottom = 16.dp),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        error = painterResource(R.drawable.ic_error)
                    )
                    Text(
                        supplier.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = DarkText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "${supplier.address}, ${supplier.city}, ${supplier.province}, ${supplier.postalCode}",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = DarkText.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .fillMaxWidth()
                    )
                    Text(
                        "Telp: ${supplier.phone}",
                        fontSize = 14.sp,
                        color = DarkText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Email: ${supplier.email}",
                        fontSize = 14.sp,
                        color = DarkText,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${supplier.phone}")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Text("PANGGIL", color = AccentText)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:${supplier.email}")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Text("EMAIL", color = AccentText)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val phone = supplier.phone.replace("+", "").replace(" ", "")
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://wa.me/$phone")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Text("WHATSAPP", color = AccentText)
                        }
                    }
                }
            }
        }
    }
}