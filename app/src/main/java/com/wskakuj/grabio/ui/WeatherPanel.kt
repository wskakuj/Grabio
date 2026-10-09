package com.wskakuj.grabio.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.wskakuj.grabio.weather.Weather
import com.wskakuj.grabio.weather.weatherEmoji
import com.wskakuj.grabio.weather.weatherLabel
import kotlin.math.roundToInt

private fun fmt(v: Double): String {
    val r = (v * 10).roundToInt() / 10.0
    return if (r == r.toInt().toDouble()) r.toInt().toString() else r.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherPanel(
    weather: Weather.Result?,
    cityName: String,
    bikeNudge: Boolean,
    modifier: Modifier = Modifier
) {
    if (cityName.isBlank() || weather == null) return
    var showHourly by remember { mutableStateOf(false) }
    val cur = weather.current

    Card(
        onClick = { showHourly = true },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(weatherEmoji(cur?.code), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            cur?.tempC?.let { "${it.roundToInt()}°C" } ?: "—",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            (cur?.place ?: cityName).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val details = buildList {
                        cur?.code?.let { add(weatherLabel(it)) }
                        cur?.precipMm?.let { add("opad ${fmt(it)} mm") }
                        cur?.windMs?.let { add("wiatr ${fmt(it)} m/s") }
                    }.joinToString(" · ")
                    if (details.isNotBlank()) {
                        Text(
                            details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Prognoza godzinowa",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            if (cur != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "źródło: ${cur.source}${if (cur.stamp.isNotBlank()) " · ${cur.stamp}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (bikeNudge) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "☔ Jedziesz rowerem, a ma padać — może jednak samochód?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showHourly) {
        HourlyDialog(weather, cityName) { showHourly = false }
    }
}

@Composable
private fun HourlyDialog(weather: Weather.Result, cityName: String, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Prognoza godzinowa") },
        text = {
            Column {
                Text(
                    "${cityName.replaceFirstChar { it.uppercase() }} · Open-Meteo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                if (weather.hourly.isEmpty()) {
                    Text("Brak danych prognozy.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(weather.hourly) { h ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    h.time,
                                    modifier = Modifier.width(56.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    weatherEmoji(h.code),
                                    modifier = Modifier.width(34.dp)
                                )
                                Text(
                                    "${h.tempC.roundToInt()}°C",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${h.precipProb}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (h.precipProb >= 40) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Zamknij") }
        }
    )
}
