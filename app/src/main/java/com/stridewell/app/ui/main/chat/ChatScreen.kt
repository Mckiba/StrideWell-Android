package com.stridewell.app.ui.main.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.model.ChatMessage
import com.stridewell.app.model.InterviewMessage
import com.stridewell.app.model.InterviewMessageRole
import com.stridewell.app.model.MessageRole
import com.stridewell.app.ui.components.MessageBubble
import com.stridewell.app.ui.components.TypingIndicator
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import kotlinx.coroutines.flow.distinctUntilChanged

private val SuggestedPrompts = listOf(
    "Why did my plan change?",
    "I missed today's run",
    "How am I progressing?"
)

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.refreshInitialHistory()
    }

    LaunchedEffect(
        uiState.messages.lastOrNull()?.id,
        uiState.isWaitingReply,
        uiState.errorMessage
    ) {
        if (!uiState.isInitialLoading && (uiState.messages.isNotEmpty() || uiState.isWaitingReply)) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(
        listState,
        uiState.hasMoreHistory,
        uiState.isLoadingHistory,
        uiState.messages.size
    ) {
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo
            val maxVisibleIndex = visible.maxOfOrNull { it.index } ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && maxVisibleIndex >= (total - 1)
        }
            .distinctUntilChanged()
            .collect { reachedOldest ->
                if (reachedOldest && uiState.hasMoreHistory && !uiState.isLoadingHistory) {
                    viewModel.loadMoreHistory()
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        when {
            uiState.isInitialLoading && uiState.messages.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.messages.isEmpty() -> {
                EmptyState(
                    errorMessage = uiState.errorMessage,
                    onRetry = viewModel::retryError,
                    onSendPrompt = viewModel::sendPrompt,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }

            else -> {
                ConversationThread(
                    uiState = uiState,
                    listState = listState,
                    onRetry = viewModel::retryError,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }

        HorizontalDivider()
        ChatInputBar(
            value = uiState.inputText,
            onValueChange = viewModel::onInputChanged,
            onSend = viewModel::sendFromInput,
            canSend = uiState.canSend
        )
    }
}

@Composable
private fun EmptyState(
    errorMessage: String?,
    onRetry: () -> Unit,
    onSendPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                Text(
                    text = "Ask your coach anything",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                errorMessage?.let {
                    InlineErrorMessage(
                        message = it,
                        onRetry = onRetry
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            SuggestedPrompts.forEach { prompt ->
                Surface(
                    onClick = { onSendPrompt(prompt) },
                    shape = RoundedCornerShape(CornerRadius.md),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ConversationThread(
    uiState: ChatViewModel.UiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        uiState.errorMessage?.let { error ->
            item(key = "error") {
                InlineErrorMessage(
                    message = error,
                    onRetry = onRetry
                )
            }
        }

        if (uiState.isWaitingReply) {
            item(key = "typing") {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TypingIndicator()
                }
            }
        }

        items(
            items = uiState.messages.asReversed(),
            key = { message -> message.id }
        ) { message ->
            ChatMessageRow(message = message)
        }

        if (uiState.isLoadingHistory) {
            item(key = "history-loading") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ChatMessageRow(message: ChatMessage) {
    val role = if (message.role == MessageRole.user) {
        InterviewMessageRole.user
    } else {
        InterviewMessageRole.assistant
    }
    MessageBubble(
        message = InterviewMessage(
            id = message.id,
            role = role,
            content = message.content,
            agent_used = message.agent_used?.name,
            created_at = message.created_at
        ),
        subtitle = if (message.role == MessageRole.assistant) message.agent_used?.name else null
    )
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    canSend: Boolean
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
            enabled = true,
            minLines = 1,
            maxLines = 5,
            placeholder = { Text("Message…") },
            shape = RoundedCornerShape(CornerRadius.input),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
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
