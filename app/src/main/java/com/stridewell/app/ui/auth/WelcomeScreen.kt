package com.stridewell.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.R
import com.stridewell.app.ui.components.PrimaryButton
import com.stridewell.app.ui.theme.AccentLight
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.AppleOAuthHelper

/**
 * Landing screen for unauthenticated users.
 *
 * Auth paths:
 *   • Continue with Apple  → SocialAuthViewModel (Chrome Custom Tab OAuth)
 *   • Continue with Google → SocialAuthViewModel (Credential Manager)
 *   • Sign up with Email   → [onGetStarted] → SignUpScreen
 *   • Sign In              → [onSignIn] → SignInScreen
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSignIn:     () -> Unit,
    onSignedIn:   (needsOnboarding: Boolean) -> Unit,
    viewModel: SocialAuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Navigate once social sign-in succeeds
    LaunchedEffect(uiState.signedIn) {
        if (uiState.signedIn == true) onSignedIn(uiState.needsOnboarding)
    }

    // Launch Apple Chrome Custom Tab when the ViewModel requests it
    LaunchedEffect(Unit) {
        viewModel.launchAppleOAuth.collect { (_, hashedNonce) ->
            AppleOAuthHelper.launch(context, hashedNonce)
        }
    }

    WelcomeContent(
        isLoading    = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onApple      = { viewModel.onAppleSignIn() },
        onGoogle     = { viewModel.signInWithGoogle(context) },
        onEmail      = onGetStarted,
        onSignIn     = onSignIn
    )
}

// ── Stateless content ─────────────────────────────────────────────────────────

@Composable
private fun WelcomeContent(
    isLoading: Boolean    = false,
    errorMessage: String? = null,
    onApple:  () -> Unit  = {},
    onGoogle: () -> Unit  = {},
    onEmail:  () -> Unit  = {},
    onSignIn: () -> Unit  = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D1117), Color.Black)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.md),
            verticalArrangement   = Arrangement.SpaceBetween,
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(1.dp))

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
                    text      = "Your AI running coach",
                    fontSize  = 20.sp,
                    color     = Color.White.copy(alpha = 0.7f)
                )
            }

            // Auth buttons
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error
                errorMessage?.let { ErrorBox(it) }

                // Continue with Apple
                SocialButton(
                    text            = "Continue with Apple",
                    iconRes         = R.drawable.ic_apple,
                    iconTint        = Color.Black,
                    backgroundColor = Color.White,
                    contentColor    = Color.Black,
                    isLoading       = isLoading,
                    onClick         = onApple
                )

                // Continue with Google
                SocialButton(
                    text            = "Continue with Google",
                    iconRes         = R.drawable.ic_google,
                    iconTint        = null,            // multicolour — don't tint
                    backgroundColor = Color(0xFF1C1C1E),
                    contentColor    = Color.White,
                    border          = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    isLoading       = isLoading,
                    onClick         = onGoogle
                )

                // Sign up with Email
                PrimaryButton(
                    text            = "Sign up with Email",
                    onClick         = onEmail,
                    enabled         = !isLoading,
                    backgroundColor = AccentLight,
                    contentColor    = Color.White
                )

                Spacer(Modifier.height(Spacing.xs))

                // Already have an account?
                TextButton(onClick = onSignIn) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.6f))) {
                                append("Already have an account? ")
                            }
                            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold)) {
                                append("Sign In")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Terms
                Text(
                    text      = "By continuing, you agree to our Terms & Privacy Policy",
                    fontSize  = 11.sp,
                    color     = Color.White.copy(alpha = 0.35f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(horizontal = Spacing.sm)
                )
            }
        }
    }
}

// ── Social button ─────────────────────────────────────────────────────────────

@Composable
private fun SocialButton(
    text: String,
    iconRes: Int,
    iconTint: Color?,
    backgroundColor: Color,
    contentColor: Color,
    isLoading: Boolean,
    onClick: () -> Unit,
    border: BorderStroke? = null
) {
    OutlinedButton(
        onClick  = { if (!isLoading) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled  = !isLoading,
        shape    = RoundedCornerShape(CornerRadius.bubble),
        colors   = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor   = contentColor
        ),
        border          = border ?: BorderStroke(0.dp, Color.Transparent),
        contentPadding  = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (iconTint != null) {
                Image(
                    painter           = painterResource(iconRes),
                    contentDescription = null,
                    modifier          = Modifier.size(20.dp),
                    colorFilter       = androidx.compose.ui.graphics.ColorFilter.tint(iconTint)
                )
            } else {
                Image(
                    painter            = painterResource(iconRes),
                    contentDescription = null,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Text(
                text  = text,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Default", showBackground = true)
@Composable
private fun PreviewDefault() = StridewellTheme {
    WelcomeContent()
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun PreviewError() = StridewellTheme {
    WelcomeContent(errorMessage = "Google sign-in failed. Please try again.")
}

@Preview(name = "Loading", showBackground = true)
@Composable
private fun PreviewLoading() = StridewellTheme {
    WelcomeContent(isLoading = true)
}
