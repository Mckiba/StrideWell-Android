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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stridewell.app.model.OnboardingStatus
import com.stridewell.app.ui.components.PrimaryButton
import com.stridewell.app.ui.theme.CornerRadius
import com.stridewell.app.ui.theme.Spacing

/**
 * Sign-in screen.
 *
 * - "Welcome Back" heading + subtitle
 * - Email + Password fields with keyboard actions
 * - "Forgot Password?" link → ModalBottomSheet
 * - PrimaryButton with loading state
 * - Inline error box — stays on screen, does NOT navigate away on failure
 * - "Don't have an account? Sign Up" footer → [onSignUp]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onSignedIn: (onboardingStatus: OnboardingStatus?) -> Unit,
    onBack:     () -> Unit,
    onSignUp:   () -> Unit,
    signInViewModel:         SignInViewModel         = hiltViewModel(),
    forgotPasswordViewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState       by signInViewModel.uiState.collectAsStateWithLifecycle()
    val forgotUiState by forgotPasswordViewModel.uiState.collectAsStateWithLifecycle()
    val focusManager   = LocalFocusManager.current
    val sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var email              by remember { mutableStateOf("") }
    var password           by remember { mutableStateOf("") }
    var showForgotPassword by remember { mutableStateOf(false) }

    // Navigate once sign-in succeeds
    LaunchedEffect(uiState.signedIn) {
        if (uiState.signedIn == true) {
            onSignedIn(uiState.onboardingStatus)
        }
    }

    val canSignIn = email.isNotBlank() && password.isNotEmpty() && !uiState.isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Spacing.lg)
    ) {
        // Back button
        IconButton(
            onClick  = onBack,
            modifier = Modifier.padding(top = Spacing.sm)
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint               = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        // Heading
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text  = "Welcome Back",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text  = "Please sign in to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        // Email field
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
            colors     = authTextFieldColors()
        )

        Spacer(Modifier.height(Spacing.sm))

        // Password field
        OutlinedTextField(
            value         = password,
            onValueChange = { password = it },
            modifier      = Modifier.fillMaxWidth(),
            label         = { Text("Password") },
            leadingIcon   = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (canSignIn) signInViewModel.signIn(email, password)
                }
            ),
            singleLine = true,
            shape      = RoundedCornerShape(CornerRadius.input),
            colors     = authTextFieldColors()
        )

        // Forgot password — trailing link, opens bottom sheet
        TextButton(
            onClick  = { showForgotPassword = true },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text  = "Forgot Password?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Sign-in button
        PrimaryButton(
            text      = "Sign In",
            onClick   = { focusManager.clearFocus(); signInViewModel.signIn(email, password) },
            enabled   = canSignIn,
            isLoading = uiState.isLoading
        )

        // Error box — inline, screen stays put on failure
        uiState.errorMessage?.let { message ->
            Spacer(Modifier.height(Spacing.sm))
            ErrorBox(message)
        }

        Spacer(Modifier.height(Spacing.md))

        // Sign-up footer
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = "Don\u2019t have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick        = onSignUp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text  = "Sign Up",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
    }

    // ── Forgot Password bottom sheet ──────────────────────────────────────────
    if (showForgotPassword) {
        ForgotPasswordSheet(
            sheetState = sheetState,
            viewModel  = forgotPasswordViewModel,
            uiState    = forgotUiState,
            onDismiss  = { showForgotPassword = false }
        )
    }
}

// ── Shared auth UI helpers (also used by SignUpScreen + ForgotPasswordScreen) ─

@Composable
internal fun ErrorBox(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                shape = RoundedCornerShape(CornerRadius.md)
            )
            .padding(Spacing.sm),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Icon(
            imageVector        = Icons.Default.Warning,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.error
        )
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
internal fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor    = MaterialTheme.colorScheme.primary,
    cursorColor          = MaterialTheme.colorScheme.primary
)
