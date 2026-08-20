package com.stridewell.app.ui.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.stridewell.BuildConfig
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.ActivityRepository
import com.stridewell.app.data.AuthRepository
import com.stridewell.app.data.ChatRepository
import com.stridewell.app.data.PlanRepository
import com.stridewell.app.data.TokenStore
import com.stridewell.app.model.OnboardingStatus
import com.stridewell.app.util.AppleOAuthHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SocialAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore,
    private val chatRepository: ChatRepository,
    private val planRepository: PlanRepository,
    private val activityRepository: ActivityRepository,
    @Named("appleOAuthToken") private val appleTokenFlow: MutableStateFlow<String?>
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val signedIn: Boolean? = null,
        val onboardingStatus: OnboardingStatus? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Emits (rawNonce, hashedNonce) when the View should launch the Apple Chrome Custom Tab.
     * The View calls AppleOAuthHelper.launch(context, hashedNonce).
     */
    private val _launchAppleOAuth = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 1)
    val launchAppleOAuth: SharedFlow<Pair<String, String>> = _launchAppleOAuth.asSharedFlow()

    // Raw nonce stored while waiting for Apple's deep link response
    private var pendingAppleNonce: String? = null

    init {
        // Collect Apple id_token emitted by MainActivity when the deep link arrives
        viewModelScope.launch {
            appleTokenFlow.collect { idToken ->
                val nonce = pendingAppleNonce ?: return@collect
                if (idToken != null) {
                    pendingAppleNonce = null
                    appleTokenFlow.value = null   // consume
                    finalize(provider = "apple") { authRepository.appleSignIn(idToken, nonce) }
                }
            }
        }
    }

    // ── Google ────────────────────────────────────────────────────────────────

    fun signInWithGoogle(context: Context) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val rawNonce    = generateNonce()
            val hashedNonce = sha256(rawNonce)
            val credentialManager = CredentialManager.create(context)

            try {
                val idToken = try {
                    fetchGoogleIdToken(
                        credentialManager, context,
                        GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .setNonce(hashedNonce)
                            .build(),
                    )
                } catch (e: NoCredentialException) {
                    // Bottom sheet has nothing to offer (no eligible account, or One Tap
                    // cooldown after repeated dismissals). Fall back to the full picker,
                    // which also offers adding an account.
                    Log.w(TAG, "Google bottom sheet had no credential, falling back to account picker", e)
                    fetchGoogleIdToken(
                        credentialManager, context,
                        GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .setNonce(hashedNonce)
                            .build(),
                    )
                }

                // Backend expects the raw nonce; Google embeds the SHA-256 hash in the token
                finalize(provider = "google") { authRepository.googleSignIn(idToken, rawNonce) }
            } catch (e: GetCredentialCancellationException) {
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: NoCredentialException) {
                Log.w(TAG, "Google sign-in: no credential after fallback", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No Google account available. Add one in device settings and try again.",
                    )
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google sign-in failed: type=${e.type} message=${e.errorMessage}", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Google sign-in failed. Please try again.")
                }
            }
        }
    }

    private suspend fun fetchGoogleIdToken(
        credentialManager: CredentialManager,
        context: Context,
        option: CredentialOption,
    ): String {
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = credentialManager.getCredential(context, request)
        return GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    }

    // ── Apple ─────────────────────────────────────────────────────────────────

    fun onAppleSignIn() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val rawNonce    = generateNonce()
        val hashedNonce = sha256(rawNonce)
        pendingAppleNonce = rawNonce

        viewModelScope.launch {
            _launchAppleOAuth.emit(rawNonce to hashedNonce)
        }
    }

    // Called by the View when the Apple Custom Tab is dismissed without completing
    fun onAppleOAuthAborted() {
        pendingAppleNonce = null
        _uiState.update { it.copy(isLoading = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── Shared finalize ───────────────────────────────────────────────────────

    private suspend fun finalize(
        @Suppress("UNUSED_PARAMETER") provider: String,
        call: suspend () -> ApiResult<com.stridewell.app.model.LoginResponse>
    ) {
        when (val result = call()) {
            is ApiResult.Success -> {
                planRepository.clearInMemoryState(clearSeenVersion = true)
                chatRepository.clearInMemoryState(clearPersistedConversationId = true)
                activityRepository.reset()
                tokenStore.saveSession(
                    result.data.token,
                    result.data.refresh_token,
                    result.data.expires_at,
                )
                tokenStore.saveUserId(result.data.user_id)
                val meResult = authRepository.me()
                val onboardingStatus = when (meResult) {
                    is ApiResult.Success -> {
                        meResult.data.onboarding_status ?: OnboardingStatus.pending
                    }
                    else -> OnboardingStatus.pending
                }
                _uiState.update {
                    it.copy(isLoading = false, signedIn = true, onboardingStatus = onboardingStatus)
                }
            }
            is ApiResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.message ?: "Sign-in failed. Please try again."
                    )
                }
            }
        }
    }

    // ── Crypto helpers ────────────────────────────────────────────────────────

    private fun generateNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        private const val TAG = "SocialAuthViewModel"
    }
}
