package com.treningcheck.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.treningcheck.app.AppViewModel
import com.treningcheck.app.data.AppData

@Composable
fun TodayScreen(vm: AppViewModel, data: AppData) {
    val key = vm.todayKey()
    val day = data.days.find { it.date == key }
    var newItem by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (day == null) {
            Text("Nie masz jeszcze listy na dzis.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Wybierz szablon treningu, zeby zaczac pakowanie.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Wybierz szablon")
            }
            if (data.templates.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Brak szablonow. Dodaj je w zakladce Szablony.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Text(
                "Trening: ${day.templateName.ifBlank { "Wlasna lista" }}",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { day.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Spakowane: ${day.done} / ${day.total}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(day.items, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.checked,
                            onCheckedChange = { vm.toggleItem(key, item.id) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.name,
                                textDecoration = if (item.checked) TextDecoration.LineThrough
                                else TextDecoration.None
                            )
                            if (item.note.isNotBlank()) {
                                Text(item.note, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { vm.removeItemFromDay(key, item.id) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Usun")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    label = { Text("Dodaj pozycje") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    vm.addItemToDay(key, newItem)
                    newItem = ""
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Dodaj")
                }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { vm.clearDay(key) }) {
                Text("Wyczysc liste na dzis")
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Wybierz szablon") },
            text = {
                Column {
                    if (data.templates.isEmpty()) {
                        Text("Nie masz jeszcze zadnego szablonu.")
                    } else {
                        data.templates.forEach { t ->
                            TextButton(
                                onClick = {
                                    vm.startDayFromTemplate(t.id)
                                    showPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${t.icon}  ${t.name}")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("Anuluj") }
            }
        )
    }
}
