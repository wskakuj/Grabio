package com.wskakuj.grabio.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Wspólne okno: zmiana nazwy pozycji + (opcjonalnie) dodanie na stałe i usunięcie. */
@Composable
fun EditItemDialog(
    initial: String,
    onSave: (String) -> Unit,
    onClose: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onMakePermanent: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Edytuj pozycję") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Nazwa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (onMakePermanent != null) {
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onMakePermanent) {
                        Text("➕  Dodaj na stałe do planu tego dnia")
                    }
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onDelete) {
                        Text("🗑  Usuń", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text("Zapisz") }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Anuluj") }
        }
    )
}
