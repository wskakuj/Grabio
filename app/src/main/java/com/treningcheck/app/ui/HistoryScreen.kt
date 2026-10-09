package com.treningcheck.app.ui

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
import com.treningcheck.app.AppViewModel
import com.treningcheck.app.data.AppData

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
            Text("Brak zapisanych dni. Zacznij od zakladki Dziś.")
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
            .take(5)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Treningow zapisanych: $totalDays", style = MaterialTheme.typography.bodyLarge)
                Text("Ukonczonych w 100%: $completed", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Srednie spakowanie: ${(avg * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (forgotten.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Najczesciej zapominane:", style = MaterialTheme.typography.titleMedium)
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
                ListItem(
                    headlineContent = { Text(d.date) },
                    supportingContent = {
                        Text("${d.templateName.ifBlank { "Wlasna lista" }} • ${d.done}/${d.total}")
                    },
                    trailingContent = { Text("${(d.progress * 100).toInt()}%") }
                )
                HorizontalDivider()
            }
        }
    }
}
