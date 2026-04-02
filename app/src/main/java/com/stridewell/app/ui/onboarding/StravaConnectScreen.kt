package com.stridewell.app.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.ui.onboarding.components.StravaConnectContent

/**
 * Controller for the Strava Connect onboarding step.
 *
 * Responsibilities:
 *  - Delegates all state to [StravaConnectViewModel]
 *  - Passes [LocalContext] to connect clicks (needed for Custom Tab launch)
 *  - Observes the single-shot [navigateToInterview] event and calls [onNavigateToInterview]
 *  - Renders the stateless [StravaConnectContent] composable
 *
 */
@Composable
fun StravaConnectScreen(
    onNavigateToInterview: () -> Unit,
    viewModel: StravaConnectViewModel = hiltViewModel()
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val navigateToInterview by viewModel.navigateToInterview.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Single-shot navigation event
    LaunchedEffect(navigateToInterview) {
        if (navigateToInterview) {
            viewModel.onNavigatedToInterview()
            onNavigateToInterview()
        }
    }

    StravaConnectContent(
        screenState    = screenState,
        onConnect      = { viewModel.onConnectClicked(context) },
        onSkip         = viewModel::onSkip,
        onContinue     = viewModel::onContinue,
        onRetrySession = viewModel::onRetrySession
    )
}
