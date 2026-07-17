package com.stridewell.app.ui.onboarding

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.ui.onboarding.components.StravaConnectContent

/**
 * Connect Screen controller. Delegates state to [StravaConnectViewModel], forwards the connect click with
 * an Activity context (for the Custom Tab), and navigates when the ViewModel emits a target.
 */
@Composable
fun StravaConnectScreen(
    onNavigate: (route: String) -> Unit,
    viewModel: StravaConnectViewModel = hiltViewModel()
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val navTarget by viewModel.navTarget.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSkipDialog by remember { mutableStateOf(false) }

    LaunchedEffect(navTarget) {
        navTarget?.let { route ->
            viewModel.onNavConsumed()
            onNavigate(route)
        }
    }

    StravaConnectContent(
        screenState             = screenState,
        onConnect               = { viewModel.onConnectClicked(context) },
        onContinueWithoutStrava = viewModel::onContinueWithoutStrava,
        onSkipOnboarding        = { showSkipDialog = true },
        onContinueForward       = viewModel::onContinueForward,
        onRetrySession          = viewModel::onRetrySession,
        onSignOut               = viewModel::onSignOut
    )

    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("Skip onboarding?") },
            text = { Text("You'll get a generic starter plan. You can still connect Strava and personalize things later.") },
            confirmButton = {
                TextButton(onClick = {
                    showSkipDialog = false
                    viewModel.onSkipOnboarding()
                }) { Text("Skip") }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) { Text("Cancel") }
            }
        )
    }
}
