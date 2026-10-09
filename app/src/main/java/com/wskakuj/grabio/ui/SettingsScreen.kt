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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.wskakuj.grabio.AppViewModel
import com.wskakuj.grabio.BuildConfig
import com.wskakuj.grabio.data.AppData

@Composable
fun SettingsScreen(vm: AppViewModel, data: AppData) {
    var enabled by remember { mutableStateOf(data.reminderEnabled) }
    var wdHour by remember { mutableIntStateOf(data.weekdayHour) }
    var wdMinute by remember { mutableIntStateOf(data.weekdayMinute) }
    var weHour by remember { mutableIntStateOf(data.weekendHour) }
    var weMinute by remember { mutableIntStateOf(data.weekendMinute) }

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
                Text("Przypomnienia", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Powiadomienie o wybranej godzinie, żeby nie zapomnieć się spakować.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    vm.setReminder(it, wdHour, wdMinute, weHour, weMinute)
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Poniedziałek–piątek", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        TimeRow(wdHour, wdMinute, { wdHour = it }, { wdMinute = it })

        Spacer(Modifier.height(12.dp))
        Text("Sobota–niedziela", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        TimeRow(weHour, weMinute, { weHour = it }, { weMinute = it })

        Spacer(Modifier.height(12.dp))
        Button(onClick = { vm.setReminder(enabled, wdHour, wdMinute, weHour, weMinute) }) {
            Text("Zapisz godziny")
        }

        Spacer(Modifier.height(28.dp))
        Text("Aktualizacje", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Wersja: ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.checkForUpdate() }) {
            Text("Sprawdź aktualizacje")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Grabio — lista pakowania na trening.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun TimeRow(
    hour: Int,
    minute: Int,
    onHour: (Int) -> Unit,
    onMinute: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Godzina", modifier = Modifier.width(90.dp))
        OutlinedTextField(
            value = hour.toString().padStart(2, '0'),
            onValueChange = { v ->
                v.filter { it.isDigit() }.take(2).toIntOrNull()?.let { if (it in 0..23) onHour(it) }
            },
            label = { Text("HH") },
            singleLine = true,
            modifier = Modifier.width(96.dp)
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = minute.toString().padStart(2, '0'),
            onValueChange = { v ->
                v.filter { it.isDigit() }.take(2).toIntOrNull()?.let { if (it in 0..59) onMinute(it) }
            },
            label = { Text("MM") },
            singleLine = true,
            modifier = Modifier.width(96.dp)
        )
    }
}
