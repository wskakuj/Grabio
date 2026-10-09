package com.wskakuj.grabio.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wskakuj.grabio.AppViewModel
import com.wskakuj.grabio.data.AppData
import com.wskakuj.grabio.data.ChecklistItem

@Composable
fun WeekPlanScreen(vm: AppViewModel, data: AppData) {
    var editingDay by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("Plan tygodnia", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
        }
        items((1..7).toList(), key = { it }) { day ->
            val list = data.weekPlan[day].orEmpty()
            WeekDayCard(
                title = weekdayNamePl(day),
                count = list.size,
                onClick = { editingDay = day }
            )
        }

        item {
            Spacer(Modifier.height(20.dp))
            Text("Codzienne dodatki", style = MaterialTheme.typography.titleLarge)
            Text(
                "Przypominane każdego dnia, niezależnie od treningu.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }
        items(data.essentials, key = { it.id }) { item ->
            SimpleItemRow(item.name) { vm.removeEssential(item.id) }
        }
        item {
            AddRow("Dodaj codzienny dodatek") { vm.addEssential(it) }
        }

        item {
            Spacer(Modifier.height(20.dp))
            Text("Rowerowe dodatki", style = MaterialTheme.typography.titleLarge)
            Text(
                "Dopisywane, gdy wybierzesz, że jedziesz rowerem.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }
        items(data.bikeExtras, key = { it.id }) { item ->
            SimpleItemRow(item.name) { vm.removeBikeExtra(item.id) }
        }
        item {
            AddRow("Dodaj rowerowy dodatek") { vm.addBikeExtra(it) }
            Spacer(Modifier.height(24.dp))
        }
    }

    editingDay?.let { day ->
        DayEditor(
            vm = vm,
            day = day,
            items = data.weekPlan[day].orEmpty()
        ) { editingDay = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekDayCard(title: String, count: Int, onClick: () -> Unit) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("$count pozycji", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.Edit, contentDescription = "Edytuj")
        }
    }
}

@Composable
private fun SimpleItemRow(name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Usuń")
        }
    }
}

@Composable
private fun AddRow(label: String, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(label) },
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = {
            onAdd(text)
            text = ""
        }) {
            Icon(Icons.Filled.Add, contentDescription = "Dodaj")
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
