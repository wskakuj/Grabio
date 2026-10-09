package com.treningcheck.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treningcheck.app.notify.Notifications
import com.treningcheck.app.ui.HistoryScreen
import com.treningcheck.app.ui.SettingsScreen
import com.treningcheck.app.ui.TemplatesScreen
import com.treningcheck.app.ui.TodayScreen
import com.treningcheck.app.ui.theme.TreningCheckTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifications.ensureChannel(this)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* wynik nie zmienia dzialania aplikacji */ }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            TreningCheckTheme {
                val vm: AppViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(vm: AppViewModel) {
    val data by vm.data.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        Tab("Dzis", Icons.Filled.CheckCircle),
        Tab("Szablony", Icons.Filled.List),
        Tab("Historia", Icons.Filled.DateRange),
        Tab("Ustawienia", Icons.Filled.Settings)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("TreningCheck") })
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> TodayScreen(vm, data)
                1 -> TemplatesScreen(vm, data)
                2 -> HistoryScreen(vm, data)
                else -> SettingsScreen(vm, data)
            }
        }
    }
}
