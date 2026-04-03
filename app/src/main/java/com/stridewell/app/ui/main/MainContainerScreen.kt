package com.stridewell.app.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.vector.ImageVector
import com.stridewell.app.ui.main.home.HomeScreen
import com.stridewell.app.ui.main.plan.PlanScreen
import com.stridewell.app.ui.stub.MainStubScreen

private enum class MainTab(
    val label: String,
    val icon: ImageVector
) {
    Home("Home", Icons.Default.Home),
    Plan("Plan", Icons.Default.DateRange),
    Chat("Chat", Icons.Default.Edit),
    Activities("Activities", Icons.AutoMirrored.Filled.List),
    Settings("Settings", Icons.Default.Settings)
}

@Composable
fun MainContainerScreen(
    onOpenPlanChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.Home -> HomeScreen(
                onOpenPlanChange = onOpenPlanChange,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.Plan -> PlanScreen(
                onOpenPlanChange = onOpenPlanChange,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.Chat -> MainStubScreen(
                title = "Chat",
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.Activities -> MainStubScreen(
                title = "Activities",
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.Settings -> MainStubScreen(
                title = "Settings",
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
