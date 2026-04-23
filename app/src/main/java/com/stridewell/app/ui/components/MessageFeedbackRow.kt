package com.stridewell.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.stridewell.app.model.FeedbackVote
import com.stridewell.app.model.MessageFeedback
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Thumbs up/down affordance shown beneath each assistant chat bubble.
 * On thumbs-down, slides in an inline comment field. Submits on IME Send,
 * focus loss (keyboard dismiss), or after 10s idle. X button discards.
 *
 * Parent handles the actual PUT via [onVote] / [onCommentSubmit].
 */
@Composable
fun MessageFeedbackRow(
    feedback: MessageFeedback?,
    onVote: (FeedbackVote) -> Unit,
    onCommentSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCommentField by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun submit() {
        val trimmed = commentText.trim()
        commentText = ""
        showCommentField = false
        if (trimmed.isNotEmpty()) onCommentSubmit(trimmed)
    }

    fun dismiss() {
        commentText = ""
        showCommentField = false
    }

    Column(
        modifier = modifier.padding(start = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            ThumbIconButton(
                filled = feedback?.vote == FeedbackVote.up,
                active = feedback?.vote == FeedbackVote.up,
                isUp = true,
                onClick = {
                    dismiss()
                    onVote(FeedbackVote.up)
                }
            )
            ThumbIconButton(
                filled = feedback?.vote == FeedbackVote.down,
                active = feedback?.vote == FeedbackVote.down,
                isUp = false,
                onClick = {
                    if (feedback?.vote == FeedbackVote.down) {
                        // Already down → toggle comment field
                        showCommentField = !showCommentField
                    } else {
                        onVote(FeedbackVote.down)
                        showCommentField = true
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = showCommentField && feedback?.vote == FeedbackVote.down,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 }
        ) {
            CommentField(
                value = commentText,
                onValueChange = { commentText = it },
                onSubmit = { submit() },
                onDismiss = { dismiss() },
                focusRequester = focusRequester
            )
        }

        // Auto-focus when the field appears
        LaunchedEffect(showCommentField) {
            if (showCommentField) {
                // Small delay to ensure the field is composed before requesting focus
                delay(50)
                runCatching { focusRequester.requestFocus() }
            }
        }

        // 10s idle auto-submit — restarts on each keystroke
        LaunchedEffect(commentText, showCommentField) {
            if (!showCommentField) return@LaunchedEffect
            delay(10_000)
            if (commentText.isNotBlank()) submit()
        }
    }
}

@Composable
private fun ThumbIconButton(
    filled: Boolean,
    active: Boolean,
    isUp: Boolean,
    onClick: () -> Unit
) {
    val icon = when {
        isUp && filled -> Icons.Filled.ThumbUp
        isUp -> Icons.Outlined.ThumbUp
        !isUp && filled -> Icons.Filled.ThumbDown
        else -> Icons.Outlined.ThumbDown
    }
    val tint = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (isUp) "Thumbs up" else "Thumbs down",
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun CommentField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    focusRequester: FocusRequester
) {
    var hasHadFocus by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                androidx.compose.material3.Text(
                    "What went wrong? (optional)",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            shape = RoundedCornerShape(CornerRadius.sm),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        hasHadFocus = true
                    } else if (hasHadFocus) {
                        // Keyboard dismissed (tap outside, back gesture) → submit whatever's there
                        onSubmit()
                    }
                }
        )

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close comment field",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
