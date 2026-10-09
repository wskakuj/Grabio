package com.wskakuj.grabio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wskakuj.grabio.AppViewModel
import com.wskakuj.grabio.data.AppData
import com.wskakuj.grabio.data.ChecklistItem

@Composable
fun WeekPlanScreen(vm: AppViewModel, data: AppData) {
    var editingDay by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Plan tygodnia",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Rzeczy na każdy dzień treningu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
        }

        items((1..7).toList(), key = { it }) { day ->
            DayCard(
                title = weekdayNamePl(day),
                names = data.weekPlan[day].orEmpty().map { it.name }
            ) { editingDay = day }
        }

        item {
            ListCard(
                title = "✅  Codzienne dodatki",
                subtitle = "Przypominane każdego dnia.",
                items = data.essentials,
                onRemove = { vm.removeEssential(it) },
                onAdd = { vm.addEssential(it) },
                addLabel = "Dodaj codzienny dodatek"
            )
        }
        item {
            ListCard(
                title = "🚲  Rowerowe dodatki",
                subtitle = "Dopisane, gdy jedziesz rowerem.",
                items = data.bikeExtras,
                onRemove = { vm.removeBikeExtra(it) },
                onAdd = { vm.addBikeExtra(it) },
                addLabel = "Dodaj rowerowy dodatek"
            )
        }
    }

    editingDay?.let { day ->
        DayEditor(vm = vm, day = day, items = data.weekPlan[day].orEmpty()) {
            editingDay = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCard(title: String, names: List<String>, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                if (names.isEmpty()) {
                    Text(
                        "Dzień wolny",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        names.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Edytuj",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ListCard(
    title: String,
    subtitle: String,
    items: List<ChecklistItem>,
    onRemove: (String) -> Unit,
    onAdd: (String) -> Unit,
    addLabel: String
) {
    var text by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.name, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemove(item.id) }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Usuń",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(addLabel) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    onAdd(text)
                    text = ""
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Dodaj")
                }
            }
        }
    }
}

@Composable
private fun DayEditor(
    vm: AppViewModel,
    day: Int,
    items: List<ChecklistItem>,
    onClose: () -> Unit
) {
    var newItem by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Plan — ${weekdayNamePl(day)}") },
        text = {
            Column {
                if (items.isEmpty()) {
                    Text("Brak pozycji. Dodaj pierwszą poniżej.")
                }
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(items, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.removeWeekItem(day, item.id) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Usuń")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItem,
                        onValueChange = { newItem = it },
                        label = { Text("Dodaj pozycję") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        vm.addWeekItem(day, newItem)
                        newItem = ""
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "Dodaj")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Gotowe") }
        }
    )
}
