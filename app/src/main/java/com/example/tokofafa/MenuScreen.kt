package com.example.tokofafa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tokofafa.produk.ProductListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sistem Kasir Mobile") },
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
        ) {
            // Supplier
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/3144/3144456.png",
                title = "Supplier",
                subtitle = "Mengelola supplier",
                onClick = { /* To be implemented */ }
            )

            // Produk
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/679/679922.png",
                title = "Produk",
                subtitle = "Mengelola produk",
                onClick = { navController.navigate("product_list") }
            )

            // Inventori
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/3082/3082383.png",
                title = "Inventori",
                subtitle = "Mengelola persediaan",
                onClick = { /* To be implemented */ }
            )

            // Kasir
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/2919/2919592.png",
                title = "Kasir",
                subtitle = "Melakukan penjualan produk dan kasir",
                onClick = { /* To be implemented */ }
            )

            // Laporan
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/2231/2231610.png",
                title = "Laporan",
                subtitle = "Melihat laporan penjualan dan keuangan",
                onClick = { /* To be implemented */ }
            )

            // Pengaturan
            MenuItem(
                iconUrl = "https://cdn-icons-png.flaticon.com/512/126/126794.png",
                title = "Pengaturan",
                subtitle = "Melakukan Pengaturan",
                onClick = { /* To be implemented */ }
            )

            // Profil Toko
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
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        AsyncImage(
            model = iconUrl,
            contentDescription = title,
            modifier = Modifier
                .size(40.dp)
                .padding(end = 16.dp)
        )

        // Title and Subtitle
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        // Arrow Icon
        AsyncImage(
            model = "https://cdn-icons-png.flaticon.com/512/271/271228.png", // Right arrow icon
            contentDescription = "Navigate",
            modifier = Modifier.size(24.dp)
        )
    }

    // Divider between items
    Divider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        thickness = 1.dp
    )
}