package com.stridewell.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stridewell.app.ui.components.PrimaryButton
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.InterFamily
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

// ── Bottom sheet host ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordSheet(
    sheetState: SheetState,
    viewModel: ForgotPasswordViewModel,
    uiState: ForgotPasswordViewModel.UiState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.background
    ) {
        ForgotPasswordContent(
            uiState       = uiState,
            onSendReset   = { email -> viewModel.sendResetLink(email) },
            modifier      = Modifier.padding(bottom = Spacing.xxl)
        )
    }
}

// ── Content (stateless — previewable) ─────────────────────────────────────────

@Composable
fun ForgotPasswordContent(
    uiState: ForgotPasswordViewModel.UiState,
    onSendReset: (email: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }
    val canSubmit = email.isNotBlank() && !uiState.isLoading

    Column(
        modifier            = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (uiState.didSend) {
            // ── Success state ─────────────────────────────────────────────────
            Spacer(Modifier.height(Spacing.lg))

            Icon(
                imageVector        = Icons.Default.Email,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(48.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text  = "Check your email",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text      = buildAnnotatedString {
                        append("If ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(email) }
                        append(" has an account, you\u2019ll receive a password reset link shortly.")
                    },
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(Spacing.lg))
        } else {
            // ── Request form ──────────────────────────────────────────────────
            Spacer(Modifier.height(Spacing.sm))

            Icon(
                imageVector        = Icons.Default.Email,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(44.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text  = "Reset your password",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text      = "Enter your email and we\u2019ll send you a link to reset your password.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            OutlinedTextField(
                value         = email,
                onValueChange = { email = it },
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text("Email Address") },
                leadingIcon   = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (canSubmit) onSendReset(email)
                    }
                ),
                singleLine = true,
                shape      = RoundedCornerShape(CornerRadius.input),
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor    = MaterialTheme.colorScheme.primary,
                    cursorColor          = MaterialTheme.colorScheme.primary
                )
            )

            uiState.errorMessage?.let { ErrorBox(it) }

            PrimaryButton(
                text      = "Send Reset Link",
                onClick   = { focusManager.clearFocus(); onSendReset(email) },
                enabled   = canSubmit,
                isLoading = uiState.isLoading
            )

            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Forgot Password — Form", showBackground = true)
@Composable
private fun PreviewForm() = StridewellTheme {
    ForgotPasswordContent(
        uiState     = ForgotPasswordViewModel.UiState(),
        onSendReset = {}
    )
}

@Preview(name = "Forgot Password — Sent", showBackground = true)
@Composable
private fun PreviewSent() = StridewellTheme {
    ForgotPasswordContent(
        uiState     = ForgotPasswordViewModel.UiState(didSend = true),
        onSendReset = {}
    )
}

@Preview(name = "Forgot Password — Error", showBackground = true)
@Composable
private fun PreviewError() = StridewellTheme {
    ForgotPasswordContent(
        uiState     = ForgotPasswordViewModel.UiState(errorMessage = "No account found with this email."),
        onSendReset = {}
    )
}
