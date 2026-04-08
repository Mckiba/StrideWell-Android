package com.stridewell.app.ui.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.vector.ImageVector
import com.stridewell.app.model.DecisionRecord
import com.stridewell.app.model.Run
import com.stridewell.app.ui.background.heatmap.HeatmapViewModel
import com.stridewell.app.ui.background.weather.ResidueView
import com.stridewell.app.ui.background.weather.StormContents
import com.stridewell.app.ui.background.weather.WeatherViewModel
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
    val context = LocalContext.current
    val heatmapViewModel: HeatmapViewModel = hiltViewModel()
    val weatherViewModel: WeatherViewModel = hiltViewModel()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val weatherState by weatherViewModel.uiState.collectAsStateWithLifecycle()
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        chatEntryMessageFlow.collect { message ->
            if (message.isNullOrBlank()) return@collect
            chatViewModel.sendInitialMessage(message)
            selectedTab = MainTab.Chat
            chatEntryMessageFlow.value = null
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            heatmapViewModel.setLocationPermissionGranted(true)
            weatherViewModel.fetchIfNeeded(true)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        heatmapViewModel.setLocationPermissionGranted(hasLocationPermission)
        weatherViewModel.fetchIfNeeded(hasLocationPermission)
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
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                MainTab.Home -> HomeScreen(
                    onOpenPlanChange = onOpenPlanChange,
                    onOpenChatWithMessage = { message ->
                        chatViewModel.sendInitialMessage(message)
                        selectedTab = MainTab.Chat
                    },
                    hasLocationPermission = hasLocationPermission,
                    heatmapViewModel = heatmapViewModel,
                    weatherViewModel = weatherViewModel,
                    modifier = Modifier
                )
                MainTab.Plan -> PlanScreen(
                    onOpenPlanChange = { onOpenPlanChange(null) },
                    hasLocationPermission = hasLocationPermission,
                    heatmapViewModel = heatmapViewModel,
                    modifier = Modifier
                )
                MainTab.Chat -> ChatScreen(
                    viewModel = chatViewModel,
                    modifier = Modifier
                )
                MainTab.Activities -> ActivitiesScreen(
                    onNavigateToDetail = onNavigateToActivityDetail,
                    hasLocationPermission = hasLocationPermission,
                    heatmapViewModel = heatmapViewModel,
                    weatherViewModel = weatherViewModel,
                    modifier = Modifier
                )
                MainTab.Settings -> SettingsScreen(
                    modifier = Modifier
                )
            }

            if (weatherState.activeCondition != com.stridewell.app.model.StormCondition.CLEAR) {
                val type = if (weatherState.activeCondition == com.stridewell.app.model.StormCondition.RAIN) {
                    StormContents.RAIN
                } else {
                    StormContents.SNOW
                }
                val strength = if (type == StormContents.RAIN) 250f else 150f
                ResidueView(
                    type = type,
                    strength = strength,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }
    }
}
