package com.stridewell.app.ui.onboarding.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    onConnect: () -> Unit       = {},
    onSkip: () -> Unit          = {},
    onContinue: () -> Unit      = {},
    onRetrySession: () -> Unit  = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Background image ──────────────────────────────────────────────────
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

        // ── Content column ────────────────────────────────────────────────────
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // ── Circular modal card ───────────────────────────────────────────
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
                    // Strava logo
                    Image(
                        painter            = painterResource(R.drawable.strava_logo),
                        contentDescription = "Strava",
                        modifier           = Modifier
                            .width(160.dp)
                            .height(65.dp),
                        contentScale       = ContentScale.Fit
                    )

                    // Status indicator
                    Box(
                        modifier         = Modifier.height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        StatusRow(screenState)
                    }

                    // Conditional action button
                    when (screenState) {
                        ScreenState.Starting,
                        ScreenState.Connecting,
                        ScreenState.Analyzing  -> Unit

                        ScreenState.Connected  ->
                            PrimaryButton(
                                text      = "Continue",
                                onClick   = onContinue,
                                modifier  = Modifier.padding(horizontal = 32.dp)
                            )

                        is ScreenState.SessionError ->
                            PrimaryButton(
                                text      = "Try again",
                                onClick   = onRetrySession,
                                modifier  = Modifier.padding(horizontal = 32.dp)
                            )

                        ScreenState.Idle,
                        is ScreenState.OAuthError ->
                            PrimaryButton(
                                text      = "Connect",
                                onClick   = onConnect,
                                modifier  = Modifier.padding(horizontal = 32.dp)
                            )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Bottom info panel ─────────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Black,
                        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                    )
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text  = "Let's make this plan about You. Together.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text  = "Connect your Strava to help us build a plan tailored to your history and goals.",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text  = "Skipping the onboarding will result in a default plan. For a more personal experience the early onboarding and Strava integration is recommended.",
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                TextButton(
                    onClick  = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text  = "Skip for now",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
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
