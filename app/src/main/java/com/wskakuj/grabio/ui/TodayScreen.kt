package com.wskakuj.grabio.ui

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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wskakuj.grabio.AppViewModel
import com.wskakuj.grabio.data.AppData
import com.wskakuj.grabio.data.GROUP_BIKE
import com.wskakuj.grabio.data.GROUP_CUSTOM
import com.wskakuj.grabio.data.GROUP_ESSENTIAL
import com.wskakuj.grabio.data.GROUP_TRAINING
import com.wskakuj.grabio.data.TRANSPORT_BIKE
import com.wskakuj.grabio.data.TRANSPORT_CAR

fun weekdayNamePl(dow: Int): String = listOf(
    "poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela"
).getOrElse(dow - 1) { "?" }

@Composable
fun TodayScreen(vm: AppViewModel, data: AppData) {
    val key = vm.todayKey()
    val record = data.days.find { it.date == key }
    // Za każdym wejściem na zakładkę pytamy o środek transportu.
    var askTransport by remember { mutableStateOf(true) }
    var newItem by remember { mutableStateOf("") }

    if (askTransport) {
        AlertDialog(
            onDismissRequest = { if (record != null) askTransport = false },
            title = { Text("Czym jedziesz na trening?") },
            text = {
                Text(
                    "Jeśli wybierzesz rower, dopiszę do listy kluczyk od łańcucha " +
                        "i światełka do roweru."
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.setTransport(TRANSPORT_CAR)
                    askTransport = false
                }) { Text("Samochodem") }
            },
            dismissButton = {
                Button(onClick = {
                    vm.setTransport(TRANSPORT_BIKE)
                    askTransport = false
                }) { Text("Rowerem") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (record == null) {
            Text("Wybierz środek transportu, żeby zobaczyć listę na dziś.")
        } else {
            Text(
                "Trening — ${weekdayNamePl(vm.weekday())}",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { record.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text("Spakowane: ${record.done} / ${record.total}")

            Spacer(Modifier.height(12.dp))
            Text("Transport", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = record.transport == TRANSPORT_CAR,
                    onClick = { vm.setTransport(TRANSPORT_CAR) },
                    label = { Text("Samochód") }
                )
                FilterChip(
                    selected = record.transport == TRANSPORT_BIKE,
                    onClick = { vm.setTransport(TRANSPORT_BIKE) },
                    label = { Text("Rower") }
                )
            }

            Spacer(Modifier.height(12.dp))

            val groups = listOf(
                GROUP_TRAINING to "Trening",
                GROUP_ESSENTIAL to "Codziennie",
                GROUP_BIKE to "Rower",
                GROUP_CUSTOM to "Własne"
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                groups.forEach { (group, label) ->
                    val groupItems = record.items.filter { it.group == group }
                    if (groupItems.isNotEmpty()) {
                        item(key = "h_$group") {
                            Text(
                                label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(groupItems, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.checked,
                                    onCheckedChange = { vm.toggleItem(key, item.id) }
                                )
                                Text(
                                    item.name,
                                    modifier = Modifier.weight(1f),
                                    textDecoration = if (item.checked) TextDecoration.LineThrough
                                    else TextDecoration.None
                                )
                                IconButton(onClick = { vm.removeItemFromDay(key, item.id) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Usuń")
                                }
                            }
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
                    label = { Text("Dodaj pozycję") },
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
                Text("Wyczyść listę na dziś")
            }
        }
    }
}
