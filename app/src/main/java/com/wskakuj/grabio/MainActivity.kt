package com.wskakuj.grabio

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wskakuj.grabio.data.AppData
import com.wskakuj.grabio.notify.Notifications
import com.wskakuj.grabio.ui.HistoryScreen
import com.wskakuj.grabio.ui.SettingsScreen
import com.wskakuj.grabio.ui.TodayScreen
import com.wskakuj.grabio.ui.WeekPlanScreen
import com.wskakuj.grabio.ui.GrabioWordmark
import com.wskakuj.grabio.ui.theme.GrabioTheme
import com.wskakuj.grabio.widget.WidgetRefresh
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifications.ensureChannel(this)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val data by vm.data.collectAsState()
            GrabioTheme(mode = data.themeMode) {
                AppRoot(vm, data)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // widget albo powiadomienie mogły coś zmienić, gdy aplikacja była w tle
        vm.reload()
    }
}

private data class Tab(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(vm: AppViewModel, data: AppData) {
    val updateState by vm.updateState.collectAsState()
    val weather by vm.weather.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // Licznik + pasek w górnym pasku, gdy karta „Dziś” zniknie przy przewijaniu.
    val todayListState = rememberLazyListState()
    val heroVisible by remember {
        derivedStateOf {
            todayListState.layoutInfo.visibleItemsInfo.any { it.key == "hero" }
        }
    }
    val showCounter = selectedTab == 0 &&
        todayListState.layoutInfo.totalItemsCount > 0 &&
        !heroVisible

    LaunchedEffect(Unit) {
        vm.checkForUpdate()
        vm.refreshWeather()
    }

    // odśwież widget, gdy zmienią się dane
    LaunchedEffect(data) { WidgetRefresh.update(context) }

    val tabs = listOf(
        Tab("Dziś", Icons.Filled.CheckCircle),
        Tab("Plan", Icons.AutoMirrored.Filled.List),
        Tab("Historia", Icons.Filled.DateRange),
        Tab("Ustawienia", Icons.Filled.Settings)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val rec = data.days.find { it.date == LocalDate.now().toString() }
                    if (showCounter && rec != null && !rec.restDay) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { rec.progress },
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(6.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                "${rec.done}/${rec.total}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        GrabioWordmark(fontSize = 22.sp)
                    }
                },
                navigationIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(32.dp)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> TodayScreen(vm, data, weather, todayListState)
                1 -> WeekPlanScreen(vm, data)
                2 -> HistoryScreen(vm, data)
                else -> SettingsScreen(vm, data)
            }
        }
    }

    UpdateDialogs(vm, updateState)
}

@Composable
private fun UpdateDialogs(vm: AppViewModel, state: UpdateState) {
    when (state) {
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = { vm.dismissUpdate() },
            title = { Text("Nowa wersja Grabio") },
            text = {
                Text(
                    "Dostępna jest wersja ${state.info.version}, a masz " +
                        "${BuildConfig.VERSION_NAME}.\n\nPobrać i zainstalować?"
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.downloadUpdate() }) { Text("Pobierz i zainstaluj") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissUpdate() }) { Text("Później") }
            }
        )

        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = { },
            title = { Text("Pobieram aktualizację…") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { state.percent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${state.percent}%")
                }
            },
            confirmButton = { }
        )

        is UpdateState.Ready -> AlertDialog(
            onDismissRequest = { vm.dismissUpdate() },
            title = { Text("Pobrano aktualizację") },
            text = { Text("Otworzyłem instalator — potwierdź instalację w systemowym oknie.") },
            confirmButton = {
                TextButton(onClick = { vm.dismissUpdate() }) { Text("OK") }
            }
        )

        is UpdateState.Failed -> AlertDialog(
            onDismissRequest = { vm.dismissUpdate() },
            title = { Text("Aktualizacja nie udała się") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = { vm.dismissUpdate() }) { Text("OK") }
            }
        )

        else -> Unit
    }
}
