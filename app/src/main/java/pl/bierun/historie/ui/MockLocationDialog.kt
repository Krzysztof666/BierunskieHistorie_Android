package pl.bierun.historie.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng

@Composable
fun MockLocationDialog(
    onLocationSet: (LatLng) -> Unit,
    onDismiss: () -> Unit
) {
    var latInput by remember { mutableStateOf("50.09115") }
    var lngInput by remember { mutableStateOf("19.08812") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🛰️ Ustaw ręczny GPS", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Wprowadź współrzędne, aby zasymulować pozycję:", modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = latInput,
                    onValueChange = { latInput = it },
                    label = { Text("Szerokość (Latitude)") },
                    placeholder = { Text("np. 50.09115") },
                    supportingText = { Text("Format dziesiętny (kropka)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = lngInput,
                    onValueChange = { lngInput = it },
                    label = { Text("Długość (Longitude)") },
                    placeholder = { Text("np. 19.08812") },
                    supportingText = { Text("Format dziesiętny (kropka)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val lat = latInput.toDoubleOrNull() ?: 50.09115
                val lng = lngInput.toDoubleOrNull() ?: 19.08812
                onLocationSet(LatLng(lat, lng))
            }) {
                Text("Ustaw lokalizację")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
