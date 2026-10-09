package com.wskakuj.grabio.ui

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wskakuj.grabio.AppViewModel
import com.wskakuj.grabio.data.AppData
import com.wskakuj.grabio.data.BAG_BACKPACK
import com.wskakuj.grabio.data.BAG_TOTE
import com.wskakuj.grabio.data.DayItem
import com.wskakuj.grabio.data.DayRecord
import com.wskakuj.grabio.data.GROUP_BIKE
import com.wskakuj.grabio.data.GROUP_CUSTOM
import com.wskakuj.grabio.data.GROUP_ESSENTIAL
import com.wskakuj.grabio.data.GROUP_TRAINING
import com.wskakuj.grabio.data.TRANSPORT_BIKE
import com.wskakuj.grabio.data.TRANSPORT_CAR
import com.wskakuj.grabio.weather.Weather

fun weekdayNamePl(dow: Int): String = listOf(
    "poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela"
).getOrElse(dow - 1) { "?" }

private fun capitalized(s: String) = s.replaceFirstChar { it.uppercase() }

@Composable
fun TodayScreen(
    vm: AppViewModel,
    data: AppData,
    weather: Weather.Result?,
    listState: LazyListState
) {
    val key = vm.todayKey()
    val context = LocalContext.current
    val record = data.days.find { it.date == key }
    val restDay = record?.restDay == true
    var askTransport by remember {
        val r = record
        mutableStateOf(r == null || (!r.restDay && r.transport.isEmpty()))
    }
    var editing by remember { mutableStateOf<DayItem?>(null) }

    if (askTransport && !restDay) {
        AlertDialog(
            onDismissRequest = {
                if (record != null && record.transport.isNotEmpty()) askTransport = false
            },
            icon = { Text("🚲", style = MaterialTheme.typography.headlineMedium) },
            title = { Text("Czym jedziesz na trening?") },
            text = {
                Column {
                    Text(
                        "Jeśli wybierzesz rower, dopiszę do listy kluczyk od łańcucha " +
                            "i światełka do roweru."
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = {
                        vm.markRestDay(key)
                        askTransport = false
                    }) {
                        Text("🛋️  Dziś mam wolne")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.setTransport(TRANSPORT_CAR)
                    askTransport = false
                }) { Text("Samochodem") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    vm.setTransport(TRANSPORT_BIKE)
                    askTransport = false
                }) { Text("Rowerem") }
            }
        )
    }

    if (record == null || restDay) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            WeatherPanel(weather = weather, cityName = data.cityName, bikeNudge = false)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (restDay) {
                        Text("🛋️", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(12.dp))
                        Text("Dziś masz wolne", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Miłego odpoczynku!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            vm.clearRestDay(key)
                            askTransport = true
                        }) {
                            Text("Wróć do treningu")
                        }
                    } else {
                        Text("🎒", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Wybierz środek transportu, żeby zobaczyć listę na dziś.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "weather") {
                WeatherPanel(
                    weather = weather,
                    cityName = data.cityName,
                    bikeNudge = record.transport == TRANSPORT_BIKE &&
                        (weather?.hourly?.take(8)?.maxOfOrNull { it.precipProb } ?: 0) >= 40
                )
            }

            item(key = "hero") { HeroCard(record, vm) }

            val groups = listOf(
                Triple(GROUP_TRAINING, "🏋️  Trening", "Rzeczy na dzisiejszy trening"),
                Triple(GROUP_ESSENTIAL, "✅  Codziennie", "Zawsze zabierz ze sobą"),
                Triple(GROUP_BIKE, "🚲  Rower", "Bo jedziesz rowerem"),
                Triple(GROUP_CUSTOM, "➕  Własne", "Przytrzymaj i przeciągnij, żeby zmienić kolejność")
            )
            groups.forEach { (group, label, sub) ->
                val groupItems = record.items.filter { it.group == group }
                if (groupItems.isNotEmpty()) {
                    item(key = "h_$group") {
                        SectionCard(group, label, sub, groupItems, key, vm) { editing = it }
                    }
                }
            }

            item { AddItemCard(key, vm) }
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { vm.clearDay(key) }) {
                        Text("Wyczyść listę na dziś")
                    }
                }
            }
        }

        CelebrationOverlay(visible = record.complete, modifier = Modifier.fillMaxSize())
    }

    editing?.let { item ->
        EditItemDialog(
            initial = item.name,
            onSave = {
                vm.renameDayItem(key, item.id, it)
                editing = null
            },
            onClose = { editing = null },
            onDelete = {
                vm.removeItemFromDay(key, item.id)
                editing = null
            },
            onMakePermanent = if (item.group == GROUP_CUSTOM) {
                {
                    vm.addItemPermanently(key, item.name)
                    Toast.makeText(
                        context,
                        "Dodano na stałe do planu tego dnia",
                        Toast.LENGTH_SHORT
                    ).show()
                    editing = null
                }
            } else {
                null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeroCard(record: DayRecord, vm: AppViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Dziś",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                capitalized(weekdayNamePl(vm.weekday())),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { record.progress },
                        modifier = Modifier.size(88.dp),
                        strokeWidth = 9.dp,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                    )
                    Text(
                        "${record.done}/${record.total}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(20.dp))
                Column {
                    Text(
                        "${(record.progress * 100).toInt()}% spakowane",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val remaining = record.items.filter { !it.checked }
                    Text(
                        when {
                            remaining.isEmpty() -> "Wszystko gotowe! 🎉"
                            else -> "Zostało: " +
                                remaining.take(3).joinToString(", ") { it.name } +
                                if (remaining.size > 3) "…" else ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = record.transport == TRANSPORT_CAR,
                    onClick = { vm.setTransport(TRANSPORT_CAR) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("🚗  Samochód") }
                )
                SegmentedButton(
                    selected = record.transport == TRANSPORT_BIKE,
                    onClick = { vm.setTransport(TRANSPORT_BIKE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("🚲  Rower") }
                )
            }
            Spacer(Modifier.height(10.dp))
            // Plecak czy torba — domyślnie torba w poniedziałek i czwartek.
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = vm.todayBag() != BAG_TOTE,
                    onClick = { vm.setBag(BAG_BACKPACK) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("🎒  Plecak") }
                )
                SegmentedButton(
                    selected = vm.todayBag() == BAG_TOTE,
                    onClick = { vm.setBag(BAG_TOTE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("👜  Torba") }
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    group: String,
    title: String,
    subtitle: String,
    items: List<DayItem>,
    dateKey: String,
    vm: AppViewModel,
    onEdit: (DayItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ReorderableColumn(
                count = items.size,
                onMove = { from, to -> vm.moveDayItem(dateKey, group, from, to) },
                rowHeight = 56.dp
            ) { index, handle ->
                ItemRow(items[index], dateKey, vm, handle) { onEdit(items[index]) }
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: DayItem,
    dateKey: String,
    vm: AppViewModel,
    dragHandle: Modifier,
    onEdit: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val textColor by animateColorAsState(
        targetValue = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurface,
        label = "itemColor"
    )

    Row(
        modifier = dragHandle
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.toggleItem(dateKey, item.id)
            }
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.toggleItem(dateKey, item.id)
            }
        )
        Text(
            item.name,
            modifier = Modifier.weight(1f),
            color = textColor,
            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
        )
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Edytuj",
                tint = MaterialTheme.colorScheme.outline
            )
        }
        IconButton(onClick = { vm.removeItemFromDay(dateKey, item.id) }) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Usuń",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun AddItemCard(dateKey: String, vm: AppViewModel) {
    var text by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Dodaj własną pozycję") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                vm.addItemToDay(dateKey, text)
                text = ""
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj")
            }
        }
    }
}
