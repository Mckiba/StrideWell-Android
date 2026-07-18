package com.stridewell.app.ui.onboarding.guided

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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

/** Captures whether the athlete has done structured speed work. Skipped when answered earlier. */
@Composable
fun SpeedworkScreen(
    onNavigate: (route: String) -> Unit,
    viewModel: IntakeChatViewModel = intakeChatViewModel(OnboardingFlow.screenContext(OnboardingScreen.Speedwork)),
    sessionStore: OnboardingSessionStore = viewModel.session
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val partial by sessionStore.partialIntake.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.startIfNeeded() }
    GuidedAdvanceEffect(OnboardingScreen.Speedwork, sessionStore, viewModel.planBuilding, onNavigate)

    SpeedworkContent(
        uiState = uiState,
        initialSelection = partial?.has_done_speedwork,
        onSelect = { value ->
            val text = if (value) "I've done structured speed workouts before."
            else "I haven't done structured speed workouts."
            viewModel.submit(text, StructuredFields(has_done_speedwork = value))
        },
        onInputChanged = viewModel::onInputChanged,
        onSend = viewModel::onSendText,
        onRetry = viewModel::retry
    )
}

@Composable
private fun SpeedworkContent(
    uiState: IntakeChatViewModel.UiState,
    initialSelection: Boolean?,
    onSelect: (Boolean) -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit
) {
    var selection by remember(initialSelection) { mutableStateOf(initialSelection) }

    GuidedScreenScaffold(
        title = "The Work",
        imageRes = R.drawable.onboarding_speedwork,
        autoExpand = uiState.phase == IntakeChatViewModel.Phase.Waiting,
        structuredInputs = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OnboardingChip(
                    label = "I've done speed work",
                    isSelected = selection == true,
                    onClick = { selection = true; onSelect(true) },
                    modifier = Modifier.weight(1f)
                )
                OnboardingChip(
                    label = "I haven't",
                    isSelected = selection == false,
                    onClick = { selection = false; onSelect(false) },
                    modifier = Modifier.weight(1f)
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
private fun SpeedworkPreview() = StridewellTheme {
    SpeedworkContent(
        uiState = sampleChatState("Have you done structured speed work before?"),
        initialSelection = null,
        onSelect = {},
        onInputChanged = {},
        onSend = {},
        onRetry = {}
    )
}
