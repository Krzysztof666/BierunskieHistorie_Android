package pl.bierun.historie.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeveloperPinDialog(correctPin: String = "1387", onPinCorrect: () -> Unit, onDismiss: () -> Unit) {
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun validate() {
        if (pinInput == correctPin) { isError = false; onPinCorrect() } else { isError = true; pinInput = "" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔒 Tryb deweloperski", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Podaj 4-cyfrowy kod PIN:", fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) { pinInput = it; isError = false } },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { validate() }),
                    isError = isError,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 22.sp)
                )
                if (isError) Text("Nieprawidłowy PIN!", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        },
        confirmButton = { Button(onClick = { validate() }, enabled = pinInput.length == 4) { Text("Zatwierdź") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
        shape = RoundedCornerShape(16.dp)
    )
}