package com.stridewell.app.ui.onboarding.guided

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.data.OnboardingSessionStore
import com.stridewell.app.navigation.OnboardingFlow
import com.stridewell.app.navigation.OnboardingScreen
import com.stridewell.app.navigation.Route
import kotlinx.coroutines.flow.SharedFlow

/**
 * Advances past [screen] once its required fields are confirmed, skipping any later screens
 * already satisfied. [planBuilding] jumps straight to the plan-building screen. Fires at most
 * once so navigating back to the screen doesn't bounce forward.
 *
 * @param extraReady an extra condition that must also hold before advancing (the goal screen
 *   uses it to wait for race details).
 */
@Composable
fun GuidedAdvanceEffect(
    screen: OnboardingScreen,
    sessionStore: OnboardingSessionStore,
    planBuilding: SharedFlow<Unit>,
    onNavigate: (route: String) -> Unit,
    extraReady: Boolean = true
) {
    val confirmed by sessionStore.confirmedFields.collectAsStateWithLifecycle()
    val stravaConnected by sessionStore.stravaConnected.collectAsStateWithLifecycle()
    val historySummary by sessionStore.historySummary.collectAsStateWithLifecycle()
    var navigated by remember { mutableStateOf(false) }

    LaunchedEffect(confirmed, extraReady) {
        if (navigated) return@LaunchedEffect
        if (OnboardingFlow.isSatisfied(screen, confirmed) && extraReady) {
            navigated = true
            val branch = OnboardingFlow.baselineBranch(stravaConnected, historySummary)
            val next = OnboardingFlow.firstUnsatisfied(branch, confirmed)
            onNavigate(next?.let { OnboardingFlow.route(it) } ?: Route.PlanBuilding.path)
        }
    }

    LaunchedEffect(Unit) {
        planBuilding.collect {
            if (!navigated) {
                navigated = true
                onNavigate(Route.PlanBuilding.path)
            }
        }
    }
}
