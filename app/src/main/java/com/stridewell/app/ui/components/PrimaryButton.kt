package com.stridewell.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing

/**
 * Filled primary action button.
 *
 * Default colours: accent background, white text. Override via [backgroundColor]/[contentColor].
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(CornerRadius.bubble),
        colors = ButtonDefaults.buttonColors(
            containerColor         = backgroundColor,
            contentColor           = contentColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.35f),
            disabledContentColor   = contentColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color  = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.height(20.dp)
            )
        } else {
            Text(
                text  = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Outlined secondary button — used for lower-priority actions (e.g., Sign In on WelcomeScreen).
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentColor: Color = Color.White
) {
    OutlinedButton(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(CornerRadius.bubble),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor           = contentColor,
            disabledContentColor   = contentColor.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (enabled) contentColor.copy(alpha = 0.6f) else contentColor.copy(alpha = 0.2f)
        ),
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color  = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.height(20.dp)
            )
        } else {
            Text(
                text  = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
