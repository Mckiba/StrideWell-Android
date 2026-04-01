package com.stridewell.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stridewell.app.ui.components.PrimaryButton
import com.stridewell.app.ui.components.SecondaryButton
import com.stridewell.app.ui.theme.AccentLight
import com.stridewell.app.ui.theme.Spacing

/**
 * Landing screen for unauthenticated users.
 *
 * Matches iOS LandingScreen: dark gradient background, Stridewell branding,
 * and two primary auth paths — Get started (onboarding) and Sign in.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit
) {
    // Dark gradient — matches iOS: #0D1117 → black
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1117), Color.Black)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(1.dp)) // top anchor

            // Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text       = "Stridewell",
                    fontSize   = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Text(
                    text     = "Your AI running coach",
                    fontSize = 20.sp,
                    color    = Color.White.copy(alpha = 0.7f)
                )
            }

            // Auth buttons + footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary CTA — starts onboarding
                PrimaryButton(
                    text            = "Get Started",
                    onClick         = onGetStarted,
                    backgroundColor = AccentLight,
                    contentColor    = Color.White
                )

                // Secondary — existing users sign in
                SecondaryButton(
                    text         = "Sign In",
                    onClick      = onSignIn,
                    contentColor = Color.White
                )

                // Terms footer
                TextButton(onClick = {}) {
                    Text(
                        text     = "By continuing, you agree to our Terms & Privacy Policy",
                        fontSize = 11.sp,
                        color    = Color.White.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}
