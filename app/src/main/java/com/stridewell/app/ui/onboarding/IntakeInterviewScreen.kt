package com.stridewell.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.model.InterviewMessage
import com.stridewell.app.ui.components.MessageBubble
import com.stridewell.app.ui.components.TypingIndicator
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing

private sealed interface ThreadItem {
    data class Message(val message: InterviewMessage) : ThreadItem
    data object Typing : ThreadItem
    data class Error(val message: String) : ThreadItem
}

@Composable
fun IntakeInterviewScreen(
    onNavigateToPlanBuilding: () -> Unit,
    viewModel: IntakeInterviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val threadItems = remember(uiState.messages, uiState.phase, uiState.errorMessage) {
        buildList {
            uiState.errorMessage?.let { add(ThreadItem.Error(it)) }
            if (uiState.showTypingIndicator) add(ThreadItem.Typing)
            uiState.messages.asReversed().forEach { add(ThreadItem.Message(it)) }
        }
    }

    // Spec: no back navigation during interview — empty handler consumes the event.
    BackHandler(enabled = true) {}

    LaunchedEffect(Unit) {
        viewModel.navigateToPlanBuilding.collect {
            onNavigateToPlanBuilding()
        }
    }

    LaunchedEffect(threadItems.size, uiState.phase) {
        if (threadItems.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        IntakeInterviewTopBar()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            reverseLayout = true,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = Spacing.md,
                vertical = Spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(
                items = threadItems,
                key = { item ->
                    when (item) {
                        is ThreadItem.Message -> item.message.id
                        ThreadItem.Typing -> "typing"
                        is ThreadItem.Error -> "error:${item.message}"
                    }
                }
            ) { item ->
                when (item) {
                    is ThreadItem.Message -> MessageBubble(message = item.message)
                    ThreadItem.Typing -> Row(modifier = Modifier.fillMaxWidth()) {
                        TypingIndicator()
                    }
                    is ThreadItem.Error -> InlineErrorMessage(
                        message = item.message,
                        onRetry = viewModel::retry
                    )
                }
            }
        }

        if (uiState.showInputBar) {
            HorizontalDivider()
            IntakeInputBar(
                value = uiState.inputText,
                onValueChange = viewModel::onInputChanged,
                onSend = viewModel::sendMessage,
                canSend = uiState.canSend,
                inputEnabled = uiState.inputEnabled
            )
        }
    }
}

@Composable
private fun IntakeInterviewTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Text(
            text = "Intake Interview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun IntakeInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    canSend: Boolean,
    inputEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = inputEnabled,
            minLines = 1,
            maxLines = 5,
            placeholder = { Text("Message…") },
            shape = RoundedCornerShape(CornerRadius.input),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )

        Surface(
            shape = CircleShape,
            color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ) {
            IconButton(
                onClick = onSend,
                enabled = canSend
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InlineErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                shape = RoundedCornerShape(CornerRadius.md)
            )
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("Retry")
            }
        }
    }
}
