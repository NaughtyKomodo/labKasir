package com.example.tokofafa.supplier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tokofafa.R
import com.example.tokofafa.entities.Supplier
import com.example.tokofafa.ui.theme.*

@Composable
fun SupplierItem(
    supplier: Supplier, // Diperbaiki dari Int ke Supplier
    onClick: () -> Unit,
    onEdit: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit,
    expandedId: Int?,
    onExpandChange: (Int?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        backgroundColor = LightCard,
        elevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = "https://cdn-icons-png.flaticon.com/512/3144/3144456.png",
                    contentDescription = "Supplier Icon",
                    modifier = Modifier
                        .size(56.dp)
                        .padding(end = 8.dp),
                    placeholder = painterResource(id = R.drawable.ic_placeholder),
                    error = painterResource(id = R.drawable.ic_error)
                )
                Column {
                    Text("ID: ${supplier.id}", fontSize = 12.sp, color = DarkText)
                    Text(
                        supplier.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkText
                    )
                    Text(
                        "${supplier.address}, ${supplier.city}, ${supplier.province}, ${supplier.postalCode}",
                        fontSize = 13.sp,
                        color = DarkText.copy(alpha = 0.7f)
                    )
                }
            }
            Box {
                IconButton(onClick = {
                    onExpandChange(if (expandedId == supplier.id) null else supplier.id)
                }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = DarkText
                    )
                }
                DropdownMenu(
                    expanded = expandedId == supplier.id,
                    onDismissRequest = { onExpandChange(null) }
                ) {
                    DropdownMenuItem(onClick = {
                        onExpandChange(null)
                        onEdit(supplier)
                    }) {
                        Text("Edit", color = DarkText)
                    }
                    DropdownMenuItem(onClick = {
                        onExpandChange(null)
                        onDelete(supplier)
                    }) {
                        Text("Hapus", color = DarkText)
                    }
                }
            }
        }
    }
}