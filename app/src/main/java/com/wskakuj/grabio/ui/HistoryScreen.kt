package com.wskakuj.grabio.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wskakuj.grabio.AppViewModel
import com.wskakuj.grabio.data.AppData
import com.wskakuj.grabio.data.TRANSPORT_BIKE
import com.wskakuj.grabio.data.TRANSPORT_CAR

@Composable
fun HistoryScreen(vm: AppViewModel, data: AppData) {
    val days = data.days.sortedByDescending { it.date }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Historia i statystyki", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        if (days.isEmpty()) {
            Text("Brak zapisanych dni. Zacznij od zakładki Dziś.")
            return@Column
        }

        val totalDays = days.size
        val completed = days.count { it.total > 0 && it.done == it.total }
        val withItems = days.filter { it.total > 0 }
        val avg = if (withItems.isEmpty()) 0.0 else withItems.map { it.progress.toDouble() }.average()

        val forgotten = days
            .flatMap { day -> day.items.filter { !it.checked }.map { it.name } }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(6)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Treningów zapisanych: $totalDays", style = MaterialTheme.typography.bodyLarge)
                Text("Ukończonych w 100%: $completed", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Średnie spakowanie: ${(avg * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (forgotten.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Najczęściej zapominane:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            forgotten.forEach { (name, count) ->
                Text("• $name — $count razy", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Minione dni", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(days, key = { it.date }) { d ->
                val transport = when (d.transport) {
                    TRANSPORT_CAR -> " • samochód"
                    TRANSPORT_BIKE -> " • rower"
                    else -> ""
                }
                ListItem(
                    headlineContent = { Text(d.date) },
                    supportingContent = { Text("${d.done}/${d.total}$transport") },
                    trailingContent = { Text("${(d.progress * 100).toInt()}%") }
                )
                HorizontalDivider()
            }
        }
    }
}
