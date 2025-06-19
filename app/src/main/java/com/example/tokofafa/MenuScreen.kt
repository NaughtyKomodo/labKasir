package com.example.tokofafa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tokofafa.ui.theme.*

@Composable
fun MenuScreen(navController: NavController) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    modifier = Modifier.height(64.dp), // Increase height to accommodate downward shift
                    backgroundColor = PrimaryBlue,
                    contentColor = AccentText,
                    content = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 30.dp), // Move content downward
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { /* No action needed */ }) {
                                AsyncImage(
                                    model = "https://cdn-icons-png.flaticon.com/512/271/271220.png",
                                    contentDescription = "Back",
                                    modifier = Modifier.size(24.dp),
                                    placeholder = painterResource(R.drawable.ic_placeholder),
                                    error = painterResource(R.drawable.ic_error)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sistem Kasir Mobile",
                                color = AccentText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryBlue)
                        .height(32.dp) // Maintain consistency with existing layout
                ) {
                    // Empty Row to maintain header height consistency
                }
            }
        },
        backgroundColor = LightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/3144/3144456.png",
                title = "Supplier",
                subtitle = "Mengelola supplier",
                onClick = { navController.navigate("supplier") }
            )
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/679/679922.png",
                title = "Produk",
                subtitle = "Mengelola produk",
                onClick = { navController.navigate("product_list") }
            )
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/3082/3082383.png",
                title = "Inventori",
                subtitle = "Mengelola persediaan",
                onClick = { navController.navigate("inventori") }
            )
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/2919/2919592.png",
                title = "Kasir",
                subtitle = "Melakukan penjualan produk dan kasir",
                onClick = { navController.navigate("kasir") }
            )
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/2231/2231610.png",
                title = "Laporan",
                subtitle = "Melihat laporan penjualan dan keuangan",
                onClick = { /* To be implemented */ }
            )
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/126/126794.png",
                title = "Pengaturan",
                subtitle = "Melakukan Pengaturan",
                onClick = { /* To be implemented */ }
            )
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/2331/2331970.png",
                title = "Profil Toko",
                subtitle = "Mengelola informasi toko",
                onClick = { /* To be implemented */ }
            )
        }
    }
}

@Composable
fun MenuItem(
    iconUrl: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(LightCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = iconUrl,
            contentDescription = title,
            modifier = Modifier
                .size(40.dp)
                .padding(end = 16.dp),
            placeholder = painterResource(id = R.drawable.ic_placeholder),
            error = painterResource(id = R.drawable.ic_error)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = DarkText.copy(alpha = 0.7f)
            )
        }
        AsyncImage(
            model = "https://cdn-icons-png.flaticon.com/512/271/271228.png",
            contentDescription = "Navigate",
            modifier = Modifier.size(24.dp),
            placeholder = painterResource(id = R.drawable.ic_placeholder),
            error = painterResource(id = R.drawable.ic_error)
        )
    }
    Divider(
        color = DarkText.copy(alpha = 0.1f),
        thickness = 1.dp
    )
}