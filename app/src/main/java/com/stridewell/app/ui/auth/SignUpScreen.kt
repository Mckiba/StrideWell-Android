package com.stridewell.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.model.OnboardingStatus
import com.stridewell.app.ui.components.PrimaryButton
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing
import com.stridewell.app.ui.theme.StridewellTheme

/**
 * Registration screen
 *
 * - "Let's get you started!" heading
 * - Email + Password + Confirm Password fields
 * - Inline password-mismatch and API error handling
 * - Terms footer
 * - "Already have an account? Sign In" link
 */
@Composable
fun SignUpScreen(
    onSignedUp: (onboardingStatus: OnboardingStatus?) -> Unit,
    onSignIn:   () -> Unit,
    viewModel:  SignUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate once registration succeeds
    LaunchedEffect(uiState.registeredWith) {
        if (uiState.registeredWith == true) {
            onSignedUp(uiState.onboardingStatus)
        }
    }

    SignUpContent(
        uiState   = uiState,
        onSignUp  = { email, password, confirm -> viewModel.signUp(email, password, confirm) },
        onSignIn  = onSignIn
    )
}

// ── Content (stateless — previewable) ─────────────────────────────────────────

@Composable
private fun SignUpContent(
    uiState:  SignUpViewModel.UiState,
    onSignUp: (email: String, password: String, confirm: String) -> Unit,
    onSignIn: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val canSubmit = email.isNotBlank() &&
        password.isNotEmpty() &&
        confirmPassword.isNotEmpty() &&
        !uiState.isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Spacing.lg)
    ) {
        Spacer(Modifier.height(Spacing.xxl))

        // Heading
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text  = "Let\u2019s get you started!",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text  = "It\u2019s quick and easy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        // Email
        OutlinedTextField(
            value         = email,
            onValueChange = { email = it },
            modifier      = Modifier.fillMaxWidth(),
            label         = { Text("Email Address") },
            leadingIcon   = { Icon(Icons.Default.Email, null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction    = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            shape      = RoundedCornerShape(CornerRadius.input),
            colors     = authFieldColors()
        )

        Spacer(Modifier.height(Spacing.sm))

        // Password
        OutlinedTextField(
            value         = password,
            onValueChange = { password = it },
            modifier      = Modifier.fillMaxWidth(),
            label         = { Text("Password") },
            leadingIcon   = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            shape      = RoundedCornerShape(CornerRadius.input),
            colors     = authFieldColors()
        )

        Spacer(Modifier.height(Spacing.sm))

        // Confirm password
        OutlinedTextField(
            value         = confirmPassword,
            onValueChange = { confirmPassword = it },
            modifier      = Modifier.fillMaxWidth(),
            label         = { Text("Confirm Password") },
            leadingIcon   = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (canSubmit) onSignUp(email, password, confirmPassword)
                }
            ),
            singleLine = true,
            shape      = RoundedCornerShape(CornerRadius.input),
            colors     = authFieldColors()
        )

        // Error box
        uiState.errorMessage?.let { message ->
            Spacer(Modifier.height(Spacing.sm))
            ErrorBox(message)
        }

        Spacer(Modifier.height(Spacing.md))

        PrimaryButton(
            text      = "Create Account",
            onClick   = { focusManager.clearFocus(); onSignUp(email, password, confirmPassword) },
            enabled   = canSubmit,
            isLoading = uiState.isLoading
        )

        Spacer(Modifier.height(Spacing.md))

        // Already have an account?
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = "Already have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick        = onSignIn,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text  = "Sign In",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Terms footer
        Text(
            text      = "By creating an account, you agree to our Terms and Privacy Policy.",
            style     = MaterialTheme.typography.labelMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier  = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.lg),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor    = MaterialTheme.colorScheme.primary,
    cursorColor          = MaterialTheme.colorScheme.primary
)

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Sign Up — Default", showBackground = true)
@Composable
private fun PreviewDefault() = StridewellTheme {
    SignUpContent(
        uiState  = SignUpViewModel.UiState(),
        onSignUp = { _, _, _ -> },
        onSignIn = {}
    )
}

@Preview(name = "Sign Up — Error", showBackground = true)
@Composable
private fun PreviewError() = StridewellTheme {
    SignUpContent(
        uiState  = SignUpViewModel.UiState(errorMessage = "An account with this email already exists."),
        onSignUp = { _, _, _ -> },
        onSignIn = {}
    )
}

@Preview(name = "Sign Up — Loading", showBackground = true)
@Composable
private fun PreviewLoading() = StridewellTheme {
    SignUpContent(
        uiState  = SignUpViewModel.UiState(isLoading = true),
        onSignUp = { _, _, _ -> },
        onSignIn = {}
    )
}
