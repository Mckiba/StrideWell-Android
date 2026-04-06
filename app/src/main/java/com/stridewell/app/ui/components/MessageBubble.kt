package com.stridewell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stridewell.app.model.InterviewMessage
import com.stridewell.app.model.InterviewMessageRole
import com.stridewell.app.ui.theme.AccentLight
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme
import com.stridewell.app.util.DateUtils

@Composable
fun MessageBubble(
    message: InterviewMessage,
    modifier: Modifier = Modifier,
    showTimestamp: Boolean = false,
    subtitle: String? = null
) {
    val isUser = message.role == InterviewMessageRole.user
    val bubbleColor = if (isUser) {
        AccentLight
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
    val horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.82f),
            horizontalAlignment = if (isUser) androidx.compose.ui.Alignment.End else androidx.compose.ui.Alignment.Start
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier
                    .background(
                        color = bubbleColor,
                        shape = RoundedCornerShape(
                            topStart = CornerRadius.bubble,
                            topEnd = CornerRadius.bubble,
                            bottomStart = if (isUser) CornerRadius.bubble else 6.dp,
                            bottomEnd = if (isUser) 6.dp else CornerRadius.bubble
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (isUser) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.padding(
                        start = Spacing.xs,
                        top = 2.dp,
                        end = Spacing.xs
                    )
                )
            }

            if (showTimestamp) {
                Text(
                    text = DateUtils.displayDateTime(message.created_at),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (isUser) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.padding(
                        start = Spacing.xs,
                        top = Spacing.xs,
                        end = Spacing.xs
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageBubblePreview() {
    StridewellTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            MessageBubble(
                message = InterviewMessage(
                    id = "assistant",
                    role = InterviewMessageRole.assistant,
                    content = "Welcome. Let's build your first plan.",
                    created_at = "2026-04-01T18:00:00Z"
                )
            )
            MessageBubble(
                message = InterviewMessage(
                    id = "user",
                    role = InterviewMessageRole.user,
                    content = "I want to get back to consistent running.",
                    created_at = "2026-04-01T18:01:00Z"
                )
            )
        }
    }
}
