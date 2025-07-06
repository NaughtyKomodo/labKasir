package com.example.tokofafa

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tokofafa.ui.theme.*

@Composable
fun SupplierFormDialog(
    isEditing: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    province: String,
    onProvinceChange: (String) -> Unit,
    postalCode: String,
    onPostalCodeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isEditing) "Edit Supplier" else "Tambah Supplier",
                    fontWeight = FontWeight.Bold,
                    color = AccentText,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Nama Supplier", color = AccentText) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = AccentText),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email", color = AccentText) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = AccentText),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Telepon", color = AccentText) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = AccentText),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    label = { Text("Alamat", color = AccentText) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = AccentText),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("Kota", color = AccentText) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = AccentText),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = province,
                    onValueChange = onProvinceChange,
                    label = { Text("Provinsi", color = AccentText) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = AccentText),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = onPostalCodeChange,
                    label = { Text("Kode Pos", color = AccentText) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = AccentText),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        backgroundColor = DarkCard,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = AccentGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan", color = AccentText)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(backgroundColor = BorderGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Batal", color = AccentText)
            }
        }
    )
}