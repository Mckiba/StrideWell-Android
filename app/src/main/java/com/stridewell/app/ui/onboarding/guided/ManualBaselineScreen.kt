package com.stridewell.app.ui.onboarding.guided

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.R
import com.stridewell.app.data.OnboardingSessionStore
import com.stridewell.app.model.StructuredFields
import com.stridewell.app.navigation.OnboardingFlow
import com.stridewell.app.navigation.OnboardingScreen
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import kotlin.math.roundToInt

/**
 * Baseline screen shown when Strava isn't connected. Captures weekly volume, training phase,
 * and injury status via a slider, phase chips, and chat, then advances once all three confirm.
 */
@Composable
fun ManualBaselineScreen(
    onNavigate: (route: String) -> Unit,
    viewModel: IntakeChatViewModel = intakeChatViewModel(OnboardingFlow.screenContext(OnboardingScreen.ManualBaseline)),
    sessionStore: OnboardingSessionStore = viewModel.session
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val partial by sessionStore.partialIntake.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.startIfNeeded() }
    GuidedAdvanceEffect(OnboardingScreen.ManualBaseline, sessionStore, viewModel.planBuilding, onNavigate)

    ManualBaselineContent(
        uiState = uiState,
        initialVolumeKm = partial?.current_weekly_volume_km,
        initialPhase = partial?.training_phase,
        onInputChanged = viewModel::onInputChanged,
        onSend = viewModel::onSendText,
        onRetry = viewModel::retry,
        onCommitVolume = { text, km -> viewModel.submit(text, StructuredFields(current_weekly_volume_km = km)) },
        onCommitPhase = { value, label -> viewModel.submit("I'd say I'm in a ${label.lowercase()} phase.", StructuredFields(training_phase = value)) }
    )
}

@Composable
private fun ManualBaselineContent(
    uiState: IntakeChatViewModel.UiState,
    initialVolumeKm: Double?,
    initialPhase: String?,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onCommitVolume: (text: String, km: Double) -> Unit,
    onCommitPhase: (value: String, label: String) -> Unit
) {
    var volumeDisplay by remember(initialVolumeKm) {
        mutableStateOf(initialVolumeKm?.let { OnboardingUnits.displayValueFromKm(it).roundToInt().toDouble() } ?: 30.0)
    }
    var selectedPhase by remember(initialPhase) { mutableStateOf(initialPhase) }

    GuidedScreenScaffold(
        title = "You, In Context",
        imageRes = R.drawable.onboarding_history_confirm,
        autoExpand = uiState.phase == IntakeChatViewModel.Phase.Waiting,
        structuredInputs = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                VolumeSlider(
                    displayValue = volumeDisplay,
                    onValueChange = { volumeDisplay = it },
                    onCommit = {
                        val km = OnboardingUnits.kmFromDisplay(volumeDisplay)
                        val text = if (volumeDisplay < 1) "I'm not currently running."
                        else "I'm running about ${volumeDisplay.roundToInt()} ${OnboardingUnits.unitLabel} per week right now."
                        onCommitVolume(text, km)
                    }
                )
                PhaseChips(
                    selected = selectedPhase,
                    onSelect = { value, label -> selectedPhase = value; onCommitPhase(value, label) }
                )
            }
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
private fun ManualBaselinePreview() = StridewellTheme {
    ManualBaselineContent(
        uiState = sampleChatState("How much are you running these days, and how's your body feeling?"),
        initialVolumeKm = null,
        initialPhase = null,
        onInputChanged = {},
        onSend = {},
        onRetry = {},
        onCommitVolume = { _, _ -> },
        onCommitPhase = { _, _ -> }
    )
}
