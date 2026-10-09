package com.wskakuj.grabio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wskakuj.grabio.AppViewModel
import com.wskakuj.grabio.data.AppData
import com.wskakuj.grabio.data.DayRecord
import com.wskakuj.grabio.data.TRANSPORT_BIKE
import com.wskakuj.grabio.data.TRANSPORT_CAR
import java.time.LocalDate

private fun shortDay(dow: Int): String =
    listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd").getOrElse(dow - 1) { "?" }

@Composable
fun HistoryScreen(vm: AppViewModel, data: AppData) {
    val today = LocalDate.now().toString()
    // Historia pokazuje tylko minione dni; do statystyk liczymy te, w których
    // faktycznie coś spakowano (odhaczono), żeby pusta lista nie zafałszowała liczb.
    val allPast = data.days.filter { it.date < today }.sortedByDescending { it.date }
    val statDays = allPast.filter { it.done > 0 }
    val byDate = data.days.associateBy { it.date }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Historia i statystyki",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        if (allPast.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Brak minionych dni.\nStatystyki pojawią się po pierwszym treningu.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        val totalDays = statDays.size
        val completed = statDays.count { it.total > 0 && it.done == it.total }
        val avg = if (statDays.isEmpty()) 0.0 else statDays.map { it.progress.toDouble() }.average()

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Treningów", "$totalDays", Modifier.weight(1f))
            StatCard("Ukończone", "$completed", Modifier.weight(1f))
            StatCard("Średnio", "${(avg * 100).toInt()}%", Modifier.weight(1f))
        }

        // --- seria ---
        var streak = 0
        var cursor = LocalDate.now()
        if (byDate[cursor.toString()]?.complete != true) cursor = cursor.minusDays(1)
        while (true) {
            val rec = byDate[cursor.toString()]
            if (rec != null && rec.complete) {
                streak++
                cursor = cursor.minusDays(1)
            } else {
                break
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "🔥 Seria: $streak ${if (streak == 1) "dzień" else "dni"} z rzędu",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Ostatnie 7 dni",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    (6 downTo 0).forEach { back ->
                        val date = LocalDate.now().minusDays(back.toLong())
                        val rec = byDate[date.toString()]
                        val p = rec?.progress ?: 0f
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((76f * p).coerceAtLeast(4f).dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (p >= 1f) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primaryContainer
                                    )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                shortDay(date.dayOfWeek.value),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        val forgotten = statDays
            .flatMap { day -> day.items.filter { !it.checked }.map { it.name } }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(6)

        if (forgotten.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Najczęściej zapominane",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    forgotten.forEach { (name, count) ->
                        Text(
                            "• $name — $count razy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Minione dni",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allPast, key = { it.date }) { d -> DayRow(d) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun DayRow(d: DayRecord) {
    val transport = when (d.transport) {
        TRANSPORT_CAR -> "🚗"
        TRANSPORT_BIKE -> "🚲"
        else -> "•"
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$transport  ${d.date}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${(d.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { d.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Spakowane: ${d.done} / ${d.total}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
