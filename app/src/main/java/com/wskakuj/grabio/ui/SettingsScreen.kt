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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wskakuj.grabio.AppViewModel
import com.wskakuj.grabio.BuildConfig
import com.wskakuj.grabio.data.AppData
import com.wskakuj.grabio.data.THEME_DARK
import com.wskakuj.grabio.data.THEME_LIGHT
import com.wskakuj.grabio.data.THEME_SYSTEM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, data: AppData) {
    var enabled by remember { mutableStateOf(data.reminderEnabled) }
    var wdHour by remember { mutableIntStateOf(data.weekdayHour) }
    var wdMinute by remember { mutableIntStateOf(data.weekdayMinute) }
    var weHour by remember { mutableIntStateOf(data.weekendHour) }
    var weMinute by remember { mutableIntStateOf(data.weekendMinute) }
    var city by remember { mutableStateOf(data.cityName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Ustawienia",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // --- wygląd ---
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Wygląd",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                val tryby = listOf(
                    THEME_SYSTEM to "Systemowy",
                    THEME_LIGHT to "Jasny",
                    THEME_DARK to "Ciemny"
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    tryby.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = data.themeMode == mode,
                            onClick = { vm.setThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = tryby.size),
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // --- przypomnienia ---
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Przypomnienia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Powiadomienie, żeby nie zapomnieć się spakować.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                HorizontalDivider(Modifier.padding(vertical = 14.dp))

                Text("Poniedziałek–piątek", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                TimeRow(wdHour, wdMinute, { wdHour = it }, { wdMinute = it })

                Spacer(Modifier.height(12.dp))
                Text("Sobota–niedziela", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                TimeRow(weHour, weMinute, { weHour = it }, { weMinute = it })

                Spacer(Modifier.height(14.dp))
                Button(onClick = { vm.setReminder(enabled, wdHour, wdMinute, weHour, weMinute) }) {
                    Text("Zapisz godziny")
                }
            }
        }

        // --- pogoda ---
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Pogoda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Podaj miasto — w dni rowerowe podpowiem, gdy ma padać.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Miasto") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { vm.setCity(city) }) { Text("Zapisz") }
                }
                if (data.cityLat != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Ustawiono: ${data.cityName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // --- aktualizacje ---
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Aktualizacje",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Wersja: ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { vm.checkForUpdate() }) {
                    Text("Sprawdź aktualizacje")
                }
            }
        }

        GrabioCredit()
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
        Text("Godzina", modifier = Modifier.width(80.dp))
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
