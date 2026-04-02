package com.stridewell.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stridewell.app.ui.auth.LaunchViewModel
import com.stridewell.app.ui.auth.SignInScreen
import com.stridewell.app.ui.auth.SignUpScreen
import com.stridewell.app.ui.auth.WelcomeScreen
import com.stridewell.app.ui.onboarding.StravaConnectScreen
import com.stridewell.app.ui.stub.MainStubScreen
import com.stridewell.app.ui.stub.OnboardingStubScreen
import kotlinx.coroutines.flow.Flow

@Composable
fun StridewellNavHost(
    launchState: LaunchViewModel.LaunchState,
    unauthorizedFlow: Flow<Unit>,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    // Route to the correct destination once the launch auth check completes
    LaunchedEffect(launchState) {
        when (launchState) {
            LaunchViewModel.LaunchState.Loading         -> Unit
            LaunchViewModel.LaunchState.Unauthenticated -> navController.navigate(Route.Welcome.path) {
                popUpTo(0) { inclusive = true }
            }
            LaunchViewModel.LaunchState.NeedsOnboarding -> navController.navigate(Route.StravaConnect.path) {
                popUpTo(0) { inclusive = true }
            }
            LaunchViewModel.LaunchState.Authenticated   -> navController.navigate(Route.Main.path) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // 401 on a protected endpoint → clear back stack → Welcome
    LaunchedEffect(Unit) {
        unauthorizedFlow.collect {
            navController.navigate(Route.Welcome.path) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController    = navController,
        startDestination = Route.Welcome.path,
        modifier         = modifier,
        enterTransition  = { fadeIn(tween(200)) },
        exitTransition   = { fadeOut(tween(200)) }
    ) {
        // ── Auth ──────────────────────────────────────────────────────────────

        composable(Route.Welcome.path) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Route.SignUp.path) },
                onSignIn     = { navController.navigate(Route.SignIn.path) },
                onSignedIn   = { needsOnboarding ->
                    val dest = if (needsOnboarding) Route.StravaConnect.path else Route.Main.path
                    navController.navigate(dest) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Route.SignUp.path) {
            SignUpScreen(
                onSignedUp = { needsOnboarding ->
                    val dest = if (needsOnboarding) Route.StravaConnect.path else Route.Main.path
                    navController.navigate(dest) { popUpTo(0) { inclusive = true } }
                },
                onSignIn = {
                    // Navigate to SignIn, removing SignUp from the back stack
                    navController.navigate(Route.SignIn.path) {
                        popUpTo(Route.SignUp.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.SignIn.path) {
            SignInScreen(
                onSignedIn = { needsOnboarding ->
                    val dest = if (needsOnboarding) Route.StravaConnect.path else Route.Main.path
                    navController.navigate(dest) { popUpTo(0) { inclusive = true } }
                },
                onBack   = { navController.popBackStack() },
                onSignUp = {
                    // Navigate to SignUp, removing SignIn from the back stack
                    navController.navigate(Route.SignUp.path) {
                        popUpTo(Route.SignIn.path) { inclusive = true }
                    }
                }
            )
        }

        // ── Onboarding ────────────────────────────────────────────────────────

        composable(Route.StravaConnect.path) {
            StravaConnectScreen(
                onNavigateToInterview = {
                    navController.navigate(Route.IntakeInterview.path) {
                        popUpTo(Route.StravaConnect.path) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.IntakeInterview.path) { OnboardingStubScreen("Intake Interview (M4)") }
        composable(Route.PlanBuilding.path)    { OnboardingStubScreen("Plan Building (M5)") }
        composable(Route.PlanReveal.path)      { OnboardingStubScreen("Plan Reveal (M6)") }

        // ── Main (stub until M7) ──────────────────────────────────────────────

        composable(Route.Main.path) { MainStubScreen() }
    }
}
