package com.stridewell.app.ui.onboarding.guided

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.R
import com.stridewell.app.data.OnboardingSessionStore
import com.stridewell.app.navigation.OnboardingFlow
import com.stridewell.app.navigation.OnboardingScreen
import com.stridewell.app.ui.theme.StridewellTheme

/** Captures what hasn't worked in past training, as free text. Advances when the backend signals plan building. */
@Composable
fun LessonsScreen(
    onNavigate: (route: String) -> Unit,
    viewModel: IntakeChatViewModel = intakeChatViewModel(OnboardingFlow.screenContext(OnboardingScreen.Lessons)),
    sessionStore: OnboardingSessionStore = viewModel.session
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.startIfNeeded() }
    GuidedAdvanceEffect(OnboardingScreen.Lessons, sessionStore, viewModel.planBuilding, onNavigate)

    LessonsContent(
        uiState = uiState,
        onNothingComesToMind = { viewModel.submit("Nothing really comes to mind.", null) },
        onInputChanged = viewModel::onInputChanged,
        onSend = viewModel::onSendText,
        onRetry = viewModel::retry
    )
}

@Composable
private fun LessonsContent(
    uiState: IntakeChatViewModel.UiState,
    onNothingComesToMind: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit
) {
    GuidedScreenScaffold(
        title = "The Lessons",
        imageRes = R.drawable.onboarding_lessons,
        autoExpand = uiState.phase == IntakeChatViewModel.Phase.Waiting,
        structuredInputs = {
            TextButton(
                onClick = onNothingComesToMind,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Nothing comes to mind", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
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
private fun LessonsPreview() = StridewellTheme {
    LessonsContent(
        uiState = sampleChatState("What hasn't worked for you in past training?"),
        onNothingComesToMind = {},
        onInputChanged = {},
        onSend = {},
        onRetry = {}
    )
}
