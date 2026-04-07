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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.vector.ImageVector
import com.stridewell.app.model.DecisionRecord
import com.stridewell.app.model.Run
import com.stridewell.app.ui.main.activities.ActivitiesScreen
import com.stridewell.app.ui.main.chat.ChatViewModel
import com.stridewell.app.ui.main.home.HomeScreen
import com.stridewell.app.ui.main.chat.ChatScreen
import com.stridewell.app.ui.main.plan.PlanScreen
import com.stridewell.app.ui.main.settings.SettingsScreen
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow

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
    onOpenPlanChange: (DecisionRecord?) -> Unit,
    onNavigateToActivityDetail: (Run) -> Unit,
    chatEntryMessageFlow: MutableStateFlow<String?>,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }

    LaunchedEffect(Unit) {
        chatEntryMessageFlow.collect { message ->
            if (message.isNullOrBlank()) return@collect
            chatViewModel.sendInitialMessage(message)
            selectedTab = MainTab.Chat
            chatEntryMessageFlow.value = null
        }
    }

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
                onOpenChatWithMessage = { message ->
                    chatViewModel.sendInitialMessage(message)
                    selectedTab = MainTab.Chat
                },
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.Plan -> PlanScreen(
                onOpenPlanChange = { onOpenPlanChange(null) },
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.Chat -> ChatScreen(
                viewModel = chatViewModel,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.Activities -> ActivitiesScreen(
                onNavigateToDetail = onNavigateToActivityDetail,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.Settings -> SettingsScreen(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
