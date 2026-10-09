package com.treningcheck.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.treningcheck.app.AppViewModel
import com.treningcheck.app.data.AppData

@Composable
fun SettingsScreen(vm: AppViewModel, data: AppData) {
    var enabled by remember { mutableStateOf(data.reminderEnabled) }
    var hour by remember { mutableIntStateOf(data.reminderHour) }
    var minute by remember { mutableIntStateOf(data.reminderMinute) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Ustawienia", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Codzienne przypomnienie", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Powiadomienie o wybranej godzinie, zeby nie zapomniec sie spakowac.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    vm.setReminder(it, hour, minute)
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Godzina", modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = hour.toString().padStart(2, '0'),
                onValueChange = { v ->
                    v.filter { it.isDigit() }.toIntOrNull()?.let { if (it in 0..23) hour = it }
                },
                label = { Text("HH") },
                singleLine = true,
                modifier = Modifier.width(90.dp)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = minute.toString().padStart(2, '0'),
                onValueChange = { v ->
                    v.filter { it.isDigit() }.toIntOrNull()?.let { if (it in 0..59) minute = it }
                },
                label = { Text("MM") },
                singleLine = true,
                modifier = Modifier.width(90.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = { vm.setReminder(enabled, hour, minute) }) {
            Text("Zapisz godzine")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "TreningCheck — Twoja lista pakowania na trening.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
