package com.treningcheck.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.treningcheck.app.AppViewModel
import com.treningcheck.app.data.AppData
import com.treningcheck.app.data.ChecklistItem
import com.treningcheck.app.data.Template

@Composable
fun TemplatesScreen(vm: AppViewModel, data: AppData) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Template?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Szablony treningow", style = MaterialTheme.typography.titleLarge)
            Button(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Nowy")
            }
        }
        Spacer(Modifier.height(12.dp))

        if (data.templates.isEmpty()) {
            Text("Nie masz jeszcze zadnego szablonu. Dodaj pierwszy przyciskiem Nowy.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(data.templates, key = { it.id }) { t ->
                    TemplateCard(t, onClick = { editing = t }, onDelete = { vm.deleteTemplate(t.id) })
                }
            }
        }
    }

    if (creating) {
        TemplateEditor(vm, null) { creating = false }
    }
    editing?.let { t ->
        TemplateEditor(vm, t) { editing = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateCard(template: Template, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(template.icon, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, style = MaterialTheme.typography.titleMedium)
                Text("${template.items.size} pozycji", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Usun")
            }
        }
    }
}

@Composable
private fun TemplateEditor(vm: AppViewModel, template: Template?, onClose: () -> Unit) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var icon by remember { mutableStateOf(template?.icon ?: "\uD83C\uDFCB\uFE0F") }
    val itemList = remember { mutableStateListOf<ChecklistItem>().apply { addAll(template?.items ?: emptyList()) } }
    var newItem by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (template == null) "Nowy szablon" else "Edytuj szablon") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = icon,
                    onValueChange = { if (it.length <= 2) icon = it },
                    label = { Text("Emoji") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Pozycje", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(itemList, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { itemList.remove(item) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Usun")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
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
                        val trimmed = newItem.trim()
                        if (trimmed.isNotEmpty()) {
                            itemList.add(ChecklistItem(id = vm.newId(), name = trimmed))
                            newItem = ""
                        }
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Dodaj")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    vm.saveTemplate(
                        Template(
                            id = template?.id ?: vm.newId(),
                            name = name.trim(),
                            icon = icon.ifBlank { "\uD83C\uDFCB\uFE0F" },
                            items = itemList.toList()
                        )
                    )
                    onClose()
                }
            }) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Anuluj") }
        }
    )
}
