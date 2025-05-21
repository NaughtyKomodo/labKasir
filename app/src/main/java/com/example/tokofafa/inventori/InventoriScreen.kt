package com.example.tokofafa.inventori

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.tokofafa.produk.LocalStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoriScreen(navController: NavController) {
    val context = LocalContext.current
    var showFilterDropdown by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var showMenu by remember { mutableStateOf(false) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("KARTU STOK", "PEMBELIAN")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Inventori") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            AsyncImage(
                                model = "https://cdn-icons-png.flaticon.com/512/271/271220.png",
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Handle search */ }) {
                            AsyncImage(
                                model = "https://cdn-icons-png.flaticon.com/512/954/954591.png",
                                contentDescription = "Search",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = { showMenu = true }) {
                            AsyncImage(
                                model = "https://cdn-icons-png.flaticon.com/512/2089/2089627.png",
                                contentDescription = "More",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("CSV") },
                                onClick = { /* Handle CSV export */ }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                // Tambahkan TabRow di bawah TopAppBar
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    )

    { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTabIndex) {
                0 -> KartuStokScreen()
                1 -> PembelianScreen()
            }
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Filter Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

            }
        }
    }
}
@Composable
fun KartuStokScreen() {
    val context = LocalContext.current
    val localStorage = remember { LocalStorage(context) }
    var products by remember { mutableStateOf(localStorage.getProducts()) }
    var showFilterDropdown by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Semua") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Filter Dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            OutlinedButton(
                onClick = { showFilterDropdown = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedFilter)
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = "https://cdn-icons-png.flaticon.com/512/2985/2985150.png", // Dropdown icon
                    contentDescription = "Dropdown",
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = showFilterDropdown,
                onDismissRequest = { showFilterDropdown = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Semua") },
                    onClick = {
                        selectedFilter = "Semua"
                        showFilterDropdown = false
                        products = localStorage.getProducts() // Reset to all
                    }
                )
                // Tambahkan filter lainnya jika diperlukan
            }
        }
    }
}

@Composable
fun PembelianScreen() {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("Hari ini") }
    val options = listOf("Hari ini", "Kemarin", "Bulan ini", "Bulan lalu", "Selama ini", "Khusus")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tampilkan")

            // Anchor dropdown
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                // Surface yang bisa diklik untuk memunculkan dropdown
                Surface(
                    shape = MaterialTheme.shapes.small,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .clickable { expanded = true }
                ) {
                    Text(
                        text = selectedOption,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // Dropdown menu
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedOption = option
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun TableRow(items: List<String>, isHeader: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { item ->
            Text(
                text = item,
                fontSize = 14.sp,
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f))
        }
    }
}