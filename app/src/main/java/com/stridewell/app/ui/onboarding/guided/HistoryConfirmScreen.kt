package com.stridewell.app.ui.onboarding.guided

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.R
import com.stridewell.app.data.OnboardingSessionStore
import com.stridewell.app.model.HistoryWeekVolume
import com.stridewell.app.model.StravaHistorySummary
import com.stridewell.app.navigation.OnboardingFlow
import com.stridewell.app.navigation.OnboardingScreen
import com.stridewell.app.ui.theme.StridewellTheme

/**
 * Baseline screen shown when Strava is connected. Displays the computed history and confirms
 * weekly volume, training phase, and injury status through chat.
 */
@Composable
fun HistoryConfirmScreen(
    onNavigate: (route: String) -> Unit,
    viewModel: IntakeChatViewModel = intakeChatViewModel(OnboardingFlow.screenContext(OnboardingScreen.HistoryConfirm)),
    sessionStore: OnboardingSessionStore = viewModel.session
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historySummary by sessionStore.historySummary.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.startIfNeeded() }
    GuidedAdvanceEffect(OnboardingScreen.HistoryConfirm, sessionStore, viewModel.planBuilding, onNavigate)

    HistoryConfirmContent(
        uiState = uiState,
        summary = historySummary,
        onInputChanged = viewModel::onInputChanged,
        onSend = viewModel::onSendText,
        onRetry = viewModel::retry
    )
}

@Composable
private fun HistoryConfirmContent(
    uiState: IntakeChatViewModel.UiState,
    summary: StravaHistorySummary?,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit
) {
    GuidedScreenScaffold(
        title = "You in Focus",
        imageRes = R.drawable.onboarding_history_confirm,
        autoExpand = uiState.phase == IntakeChatViewModel.Phase.Waiting,
        structuredInputs = {
            summary?.let { HistorySummaryCard(summary = it) }
        },
        chat = { onInteract ->
            IntakeChatSurface(
                uiState = uiState,
                onInputChanged = onInputChanged,
                onSend = onSend,
                onRetry = onRetry,
                onInteract = onInteract,
                modifier = Modifier.weight(1f)
            )
        }
    )
}

@Preview
@Composable
private fun HistoryConfirmPreview() = StridewellTheme {
    val volumes = listOf(0.0, 30.0, 55.0, 28.0, 32.0, 22.0, 18.0, 15.0, 32.0, 4.0, 20.0, 11.0)
    val summary = StravaHistorySummary(
        avg_weekly_volume_km_4wk = 40.0,
        peak_weekly_volume_km_12wk = 55.0,
        avg_runs_per_week_4wk = 4.0,
        inferred_training_phase = "base",
        weekly_volumes = volumes.mapIndexed { i, v -> HistoryWeekVolume("2026-0${(i % 9) + 1}-01", v) }
    )
    HistoryConfirmContent(
        uiState = sampleChatState("You've averaged about 40 km a week with a solid base. Does that match how you feel?"),
        summary = summary,
        onInputChanged = {},
        onSend = {},
        onRetry = {}
    )
}
