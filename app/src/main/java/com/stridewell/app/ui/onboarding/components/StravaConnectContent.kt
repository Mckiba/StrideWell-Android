package com.stridewell.app.ui.onboarding.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stridewell.R
import com.stridewell.app.ui.components.PrimaryButton
import com.stridewell.app.ui.onboarding.StravaConnectViewModel.ScreenState
import com.stridewell.app.ui.onboarding.guided.ExpandableSheetScaffold
import com.stridewell.app.ui.theme.AccentLight
import com.stridewell.app.ui.theme.InterFamily
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

// ── Composable ────────────────────────────────────────────────────────────────

/**
 * Stateless presentation layer for the Strava Connect screen.
 * All state is passed in as parameters so this composable is fully previewable.
 */
@Composable
fun StravaConnectContent(
    screenState: ScreenState,
    onConnect: () -> Unit                = {},
    onContinueWithoutStrava: () -> Unit  = {},
    onSkipOnboarding: () -> Unit         = {},
    onContinueForward: () -> Unit        = {},
    onRetrySession: () -> Unit           = {},
    onSignOut: () -> Unit                = {},
) {
    val showContinueWithoutStrava = screenState is ScreenState.Idle ||
        screenState is ScreenState.OAuthError ||
        screenState is ScreenState.Starting

    val collapsedFraction = 0.40f

    ExpandableSheetScaffold(
        collapsedFraction = collapsedFraction,
        expandedFraction  = 0.92f,
        sheetColor        = Color.Black,
        handleColor       = Color.White.copy(alpha = 0.4f),
        background = {
            // ── Background image ──────────────────────────────────────────────
            Image(
                painter            = painterResource(R.drawable.onboarding_background),
                contentDescription = null,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop
            )
            // Dark scrim so text stays legible over any photo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )

            // Title + Strava circle occupy the area above the collapsed sheet; the circle
            // is centered within that visible area so it stays clear of the resting sheet.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(1f - collapsedFraction),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text       = "You, in Context",
                    color      = Color.White,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 50.sp,
                    lineHeight = 54.sp,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = Spacing.sm)
                        .padding(top = Spacing.lg)
                )

                Spacer(modifier = Modifier.weight(1f))

                // ── Circular modal card ───────────────────────────────────────
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation    = 24.dp,
                            shape        = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.25f)
                        )
                        .size(300.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.59f))
                        .border(3.dp, Color(0xFFF9F0F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Image(
                            painter            = painterResource(R.drawable.strava_logo),
                            contentDescription = "Strava",
                            modifier           = Modifier.width(160.dp).height(65.dp),
                            contentScale       = ContentScale.Fit
                        )

                        Box(
                            modifier         = Modifier.height(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            StatusRow(screenState)
                        }

                        when (screenState) {
                            ScreenState.Starting,
                            ScreenState.Connecting,
                            ScreenState.Analyzing  -> Unit

                            ScreenState.Connected  ->
                                PrimaryButton(text = "Continue", onClick = onContinueForward, modifier = Modifier.padding(horizontal = 32.dp))

                            is ScreenState.SessionError ->
                                PrimaryButton(text = "Try again", onClick = onRetrySession, modifier = Modifier.padding(horizontal = 32.dp))

                            ScreenState.Idle,
                            is ScreenState.OAuthError ->
                                PrimaryButton(text = "Connect", onClick = onConnect, modifier = Modifier.padding(horizontal = 32.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        },
        sheetContent = { _ ->
            // ── Bottom info panel (scrollable) ────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text  = "Let's make this plan about ",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text       = "You.",
                        color      = Color.White,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp
                    )
                }

                Text(
                    text       = "Together.",
                    color      = Color.White,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 32.sp
                )

                Text(
                    text  = "Connect your Strava to help us build a plan tailored to your history and goals",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Continue without Strava — the interview still happens, we just ask more.
                if (showContinueWithoutStrava) {
                    PrimaryButton(
                        text     = "Continue without Strava",
                        onClick  = onContinueWithoutStrava,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text  = "You can still build a personal plan — we'll just ask a few more questions.",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text     = "Skipping the onboarding will result in a default plan. For a more personal experience the early onboarding and Strava integration is recommended.",
                    color    = Color.White.copy(alpha = 0.75f),
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.xs)
                )

                TextButton(
                    onClick        = onSkipOnboarding,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(
                        text       = "Skip onboarding",
                        color      = Color.White.copy(alpha = 0.85f),
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                TextButton(
                    onClick        = onSignOut,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    modifier       = Modifier.padding(top = Spacing.xs)
                ) {
                    Text(
                        text  = "Sign out",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}

// ── Status row ────────────────────────────────────────────────────────────────

@Composable
private fun StatusRow(screenState: ScreenState) {
    when (screenState) {
        ScreenState.Starting ->
            SpinnerLabel("Setting up your session\u2026")

        ScreenState.Connecting ->
            SpinnerLabel("Opening Strava\u2026")

        ScreenState.Analyzing ->
            SpinnerLabel("Analyzing your run history\u2026")

        ScreenState.Connected ->
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint     = Color(0xFF34C759),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text  = "Strava connected",
                    color = Color(0xFF34C759),
                    style = MaterialTheme.typography.bodySmall
                )
            }

        is ScreenState.SessionError ->
            ErrorLabel(screenState.message)

        is ScreenState.OAuthError ->
            ErrorLabel(screenState.message)

        ScreenState.Idle -> Unit
    }
}

@Composable
private fun SpinnerLabel(label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            modifier  = Modifier.size(16.dp),
            color     = AccentLight,
            strokeWidth = 2.dp
        )
        Text(
            text  = label,
            color = Color.DarkGray,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ErrorLabel(message: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector        = Icons.Filled.Warning,
            contentDescription = null,
            tint               = Color(0xFFFF3B30),
            modifier           = Modifier.size(16.dp)
        )
        Text(
            text      = message,
            color     = Color(0xFFFF3B30),
            style     = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Idle", showBackground = true)
@Composable
private fun PreviewIdle() = StridewellTheme {
    StravaConnectContent(screenState = ScreenState.Idle)
}

@Preview(name = "Connecting", showBackground = true)
@Composable
private fun PreviewConnecting() = StridewellTheme {
    StravaConnectContent(screenState = ScreenState.Connecting)
}

@Preview(name = "Connected", showBackground = true)
@Composable
private fun PreviewConnected() = StridewellTheme {
    StravaConnectContent(screenState = ScreenState.Connected)
}

@Preview(name = "Analyzing", showBackground = true)
@Composable
private fun PreviewAnalyzing() = StridewellTheme {
    StravaConnectContent(screenState = ScreenState.Analyzing)
}

@Preview(name = "Session Error", showBackground = true)
@Composable
private fun PreviewSessionError() = StridewellTheme {
    StravaConnectContent(
        screenState = ScreenState.SessionError("Unable to start session. Please try again.")
    )
}

@Preview(name = "OAuth Error", showBackground = true)
@Composable
private fun PreviewOAuthError() = StridewellTheme {
    StravaConnectContent(
        screenState = ScreenState.OAuthError("Could not connect to Strava.")
    )
}
