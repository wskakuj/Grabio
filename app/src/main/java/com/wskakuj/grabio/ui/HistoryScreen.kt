package com.wskakuj.grabio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wskakuj.grabio.AppViewModel
import com.wskakuj.grabio.data.AppData
import com.wskakuj.grabio.data.DayRecord
import com.wskakuj.grabio.data.DayItem
import com.wskakuj.grabio.data.GROUP_BIKE
import com.wskakuj.grabio.data.GROUP_CUSTOM
import com.wskakuj.grabio.data.GROUP_ESSENTIAL
import com.wskakuj.grabio.data.GROUP_TRAINING
import com.wskakuj.grabio.data.TRANSPORT_BIKE
import com.wskakuj.grabio.data.TRANSPORT_CAR
import java.time.LocalDate
import java.time.YearMonth

private val polishMonths = listOf(
    "styczeń", "luty", "marzec", "kwiecień", "maj", "czerwiec",
    "lipiec", "sierpień", "wrzesień", "październik", "listopad", "grudzień"
)
private val polishWeekdays = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")
private val completedColor = Color(0xFF2EAD79)
private val incompleteColor = Color(0xFFE3A33B)
private val restColor = Color(0xFF8392AE)

private fun shortDay(day: Int): String = polishWeekdays.getOrElse(day - 1) { "?" }
private fun monthLabel(month: YearMonth): String =
    polishMonths[month.monthValue - 1].replaceFirstChar { it.uppercase() } + " " + month.year

@Composable
fun HistoryScreen(vm: AppViewModel, data: AppData) {
    val currentDate = LocalDate.now()
    val today = currentDate.toString()
    val byDate = data.days.associateBy { it.date }
    val allPast = data.days.filter { it.date < today }.sortedByDescending { it.date }
    val statDays = allPast.filter { it.done > 0 }
    val latest = data.days
        .filter { it.date <= today && (it.restDay || it.items.isNotEmpty()) }
        .maxByOrNull { it.date }
        ?.date

    var selectedDate by remember { mutableStateOf(latest ?: today) }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(LocalDate.parse(latest ?: today))) }

    val selected = byDate[selectedDate]
    val totalDays = statDays.size
    val completed = statDays.count { it.complete }
    val avg = if (statDays.isEmpty()) 0.0 else statDays.map { it.progress.toDouble() }.average()

    // Dni bez rekordu są neutralne. Dni odpoczynku także nie liczą się jako porażka.
    var streak = 0
    var cursor = currentDate
    if (byDate[cursor.toString()]?.complete != true) cursor = cursor.minusDays(1)
    while (true) {
        val rec = byDate[cursor.toString()]
        when {
            rec?.restDay == true -> cursor = cursor.minusDays(1)
            rec?.complete == true -> {
                streak++
                cursor = cursor.minusDays(1)
            }
            else -> break
        }
    }

    val omissions = statDays
        .flatMap { day -> day.items.map { item -> Triple(item.id, item.name, item.checked) } }
        .groupBy { it.first }
        .map { (_, observations) ->
            val missed = observations.count { !it.third }
            Triple(observations.last().second, missed, observations.size)
        }
        .filter { it.second > 0 }
        .sortedWith(
            compareByDescending<Triple<String, Int, Int>> { it.second }
                .thenByDescending { it.second.toDouble() / it.third }
        )
        .take(6)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item {
            Text(
                "Historia i statystyki",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            CalendarCard(
                month = displayedMonth,
                today = currentDate,
                selectedDate = selectedDate,
                records = byDate,
                onSelect = { selectedDate = it },
                onMonthChange = { displayedMonth = it }
            )
        }

        item {
            SelectedDayCard(date = selectedDate, day = selected)
        }

        item {
            Text(
                "Podsumowanie",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Treningów", "$totalDays", Modifier.weight(1f))
                StatCard("Ukończone", "$completed", Modifier.weight(1f))
                StatCard("Średnio", "${(avg * 100).toInt()}%", Modifier.weight(1f))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "🔥 Seria: $streak ${if (streak == 1) "dzień" else "dni"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Zapisane dni odpoczynku nie przerywają serii.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Ostatnie 7 dni",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(104.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        (6 downTo 0).forEach { back ->
                            val date = currentDate.minusDays(back.toLong())
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
                                            if (rec?.complete == true) MaterialTheme.colorScheme.primary
                                            else if (rec?.restDay == true) restColor
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
        }

        if (omissions.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Najczęściej nieodhaczone",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Stan zapisów z poprzednich dni; nie oznacza na pewno zapomnienia.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        omissions.forEach { (name, missed, total) ->
                            Text(
                                "• $name — $missed/$total dni (${missed * 100 / total}%)",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    today: LocalDate,
    selectedDate: String,
    records: Map<String, DayRecord>,
    onSelect: (String) -> Unit,
    onMonthChange: (YearMonth) -> Unit
) {
    val currentMonth = YearMonth.from(today)
    val cellsOffset = month.atDay(1).dayOfWeek.value - 1
    val totalSlots = ((cellsOffset + month.lengthOfMonth() + 6) / 7) * 7

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Kalendarz", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        monthLabel(month),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Poprzedni miesiąc")
                }
                IconButton(
                    onClick = { onMonthChange(month.plusMonths(1)) },
                    enabled = month < currentMonth
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Następny miesiąc")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                polishWeekdays.forEach { weekday ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            weekday,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            repeat(totalSlots / 7) { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(7) { weekday ->
                        val number = week * 7 + weekday - cellsOffset + 1
                        if (number !in 1..month.lengthOfMonth()) {
                            Spacer(Modifier.weight(1f).height(49.dp))
                        } else {
                            val date = month.atDay(number)
                            val rec = records[date.toString()]
                            val color = when {
                                rec?.restDay == true -> restColor
                                rec?.complete == true -> completedColor
                                rec != null && rec.items.isNotEmpty() -> incompleteColor
                                else -> Color.Transparent
                            }
                            val isSelected = selectedDate == date.toString()
                            val isToday = date == today
                            val disabled = date.isAfter(today)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(49.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (isSelected) Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(10.dp)
                                        ) else Modifier
                                    )
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable(enabled = !disabled) { onSelect(date.toString()) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$number",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (disabled) MaterialTheme.colorScheme.onSurface.copy(alpha = .3f)
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                                }
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendDot(completedColor, "Gotowe", Modifier.weight(1f))
                LegendDot(incompleteColor, "Niepełne", Modifier.weight(1f))
                LegendDot(restColor, "Wolne", Modifier.weight(1f))
            }
            if (month != currentMonth) {
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = { onMonthChange(currentMonth); onSelect(today.toString()) }
                ) { Text("Wróć do dzisiaj") }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun SelectedDayCard(date: String, day: DayRecord?) {
    val parsed = LocalDate.parse(date)
    val status = when {
        day == null -> "Brak zapisu"
        day.restDay -> "Dzień wolny"
        day.complete -> "Wszystko spakowane"
        day.items.isEmpty() -> "Pusta lista"
        else -> "Lista niepełna"
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(
                "Wybrany dzień",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${shortDay(parsed.dayOfWeek.value)}, ${parsed.dayOfMonth} ${polishMonths[parsed.monthValue - 1]} ${parsed.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

            if (day != null && !day.restDay && day.items.isNotEmpty()) {
                val transport = when (day.transport) {
                    TRANSPORT_CAR -> "🚗 Samochód"
                    TRANSPORT_BIKE -> "🚲 Rower"
                    else -> "Nie wybrano"
                }
                Text(
                    "$transport · ${if (day.bag == "torba") "👜 Torba" else if (day.bag == "plecak") "🎒 Plecak" else "Bagaż: nie zapisano"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Spakowane ${day.done} z ${day.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LinearProgressIndicator(
                    progress = { day.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                // Obie sekcje są zwinięte po wejściu na ekran oraz zmianie daty.
                HistoryPackingSection(
                    selectedDate = date,
                    title = "Niespakowane",
                    items = day.items.filterNot { it.checked },
                    checked = false
                )
                HistoryPackingSection(
                    selectedDate = date,
                    title = "Spakowane",
                    items = day.items.filter { it.checked },
                    checked = true
                )
            } else if (day == null) {
                Text(
                    "Nie ma zapisanych informacji o pakowaniu tego dnia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (day.restDay) {
                Text("Ten dzień został oznaczony jako odpoczynek.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Rozwijana lista jednej kategorii; bez zmian w historycznym zapisie. */
@Composable
private fun HistoryPackingSection(
    selectedDate: String,
    title: String,
    items: List<DayItem>,
    checked: Boolean
) {
    var expanded by remember(selectedDate) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (checked) "✓" else "○",
                    color = if (checked) completedColor else incompleteColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    "$title (${items.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (items.isEmpty()) {
                        Text(
                            if (checked) "Nic jeszcze nie jest odhaczone." else "Wszystko spakowane!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (item.checked) "✓" else "○",
                                color = if (item.checked) completedColor else incompleteColor,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                val groupName = when (item.group) {
                                    GROUP_TRAINING -> "Trening"
                                    GROUP_ESSENTIAL -> "Codziennie"
                                    GROUP_BIKE -> "Rower"
                                    GROUP_CUSTOM -> "Własne"
                                    else -> "Inne"
                                }
                                Text(
                                    groupName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (item.priority > 0) {
                                Text(
                                    if (item.priority == 2) "‼" else "★",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 7.dp),
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
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
    }
}
