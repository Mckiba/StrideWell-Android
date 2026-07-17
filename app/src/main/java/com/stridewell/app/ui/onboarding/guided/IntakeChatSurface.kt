package com.stridewell.app.ui.onboarding.guided

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stridewell.app.ui.components.ChatInputBar
import com.stridewell.app.ui.components.MessageBubble
import com.stridewell.app.ui.components.TypingIndicator
import com.stridewell.app.ui.theme.Spacing

/**
 * The chat thread + input bar embedded in every guided onboarding screen's sheet.
 */
@Composable
fun IntakeChatSurface(
    uiState: IntakeChatViewModel.UiState,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Keep the newest content in view as the thread grows or the coach starts typing.
    LaunchedEffect(uiState.messages.size, uiState.showTypingIndicator, uiState.errorMessage) {
        val count = uiState.messages.size +
            (if (uiState.showTypingIndicator) 1 else 0) +
            (if (uiState.errorMessage != null) 1 else 0)
        if (count > 0) listState.animateScrollToItem((count - 1).coerceAtLeast(0))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
            if (uiState.showTypingIndicator) {
                item(key = "typing") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        TypingIndicator()
                    }
                }
            }
            if (uiState.phase == IntakeChatViewModel.Phase.Error && uiState.errorMessage != null) {
                item(key = "error") {
                    InlineChatError(message = uiState.errorMessage, onRetry = onRetry)
                }
            }
        }

        if (uiState.showInputBar) {
            ChatInputBar(
                value = uiState.inputText,
                onValueChange = onInputChanged,
                onSend = onSend,
                canSend = uiState.canSend,
                enabled = uiState.inputEnabled,
                onFocus = onInteract,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.sm)
            )
        }
    }
}

@Composable
private fun InlineChatError(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
            Text("Retry")
        }
    }
}
