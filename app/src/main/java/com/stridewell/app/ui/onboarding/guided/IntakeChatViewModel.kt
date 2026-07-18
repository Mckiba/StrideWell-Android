package com.stridewell.app.ui.onboarding.guided

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.OnboardingRepository
import com.stridewell.app.data.OnboardingSessionStore
import com.stridewell.app.data.TokenStore
import com.stridewell.app.model.InterviewMessage
import com.stridewell.app.model.InterviewMessageRole
import com.stridewell.app.model.OnboardingMessageRequest
import com.stridewell.app.model.OnboardingStatus
import com.stridewell.app.model.StructuredFields
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * Drives the chat on a single guided onboarding screen. Each screen gets its own instance
 * carrying that screen's [screenContext] (via the assisted factory). Every turn sends the
 * topic and any picked values; each reply's confirmed fields go into the shared
 * [OnboardingSessionStore], and [planBuilding] fires when the backend starts building the plan.
 */
@HiltViewModel(assistedFactory = IntakeChatViewModel.Factory::class)
class IntakeChatViewModel @AssistedInject constructor(
    @Assisted val screenContext: String?,
    private val repository: OnboardingRepository,
    private val sessionStore: OnboardingSessionStore,
    private val tokenStore: TokenStore,
    private val unauthorizedFlow: MutableSharedFlow<Unit>
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(screenContext: String?): IntakeChatViewModel
    }

    /** Shared onboarding session state, for the screen's advancement and pre-fill. */
    val session: OnboardingSessionStore get() = sessionStore

    enum class Phase { Loading, Active, Waiting, Transitioning, Error }

    data class UiState(
        val messages: List<InterviewMessage> = emptyList(),
        val inputText: String = "",
        val phase: Phase = Phase.Loading,
        val errorMessage: String? = null
    ) {
        val canSend: Boolean get() = phase == Phase.Active && inputText.trim().isNotEmpty()
        val showTypingIndicator: Boolean get() = phase == Phase.Loading || phase == Phase.Waiting
        val showInputBar: Boolean get() = phase != Phase.Transitioning
        val inputEnabled: Boolean get() = phase != Phase.Loading && phase != Phase.Waiting
        /** True while a control-driven send should be blocked (mirrors [canSend] gating). */
        val canInteract: Boolean get() = phase == Phase.Active
    }

    private data class Pending(
        val message: InterviewMessage,
        val structured: StructuredFields?,
        val isInitialTrigger: Boolean
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Single-shot: emitted when the backend reports the plan is being built. */
    private val _planBuilding = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val planBuilding: SharedFlow<Unit> = _planBuilding.asSharedFlow()

    private var conversationId: String? = null
    private var pending: Pending? = null
    private var hasStarted = false

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(inputText = value) }
    }

    /** Sends the opener once so the coach speaks first on this screen's topic. The trigger
     *  message is not shown in the thread; the screen shows only its own turns. */
    fun startIfNeeded() {
        if (hasStarted) return
        hasStarted = true
        viewModelScope.launch {
            _uiState.update { it.copy(phase = Phase.Loading, errorMessage = null) }
            val cid = resolveConversationId()
            if (cid == null) {
                showFatalError("No active conversation. Please go back and try again.")
                return@launch
            }
            val trigger = makeMessage(InterviewMessageRole.user, "start")
            pending = Pending(trigger, structured = null, isInitialTrigger = true)
            send(cid, pending!!)
        }
    }

    fun onSendText() {
        val content = _uiState.value.inputText.trim()
        if (content.isEmpty() || _uiState.value.phase != Phase.Active) return
        submit(content, structured = null)
        _uiState.update { it.copy(inputText = "") }
    }

    /**
     * Dual-write: [content] is the sentence shown in the thread and [structured] holds the
     * exact values for the same selection. Callers use the fixed per-control templates.
     */
    fun submit(content: String, structured: StructuredFields?) {
        val trimmed = content.trim()
        if (trimmed.isEmpty() || _uiState.value.phase != Phase.Active) return
        viewModelScope.launch {
            val cid = resolveConversationId()
            if (cid == null) {
                showFatalError("No active conversation. Please go back and try again.")
                return@launch
            }
            val userMessage = makeMessage(InterviewMessageRole.user, trimmed)
            pending = Pending(userMessage, structured, isInitialTrigger = false)
            _uiState.update {
                it.copy(messages = it.messages + userMessage, phase = Phase.Waiting, errorMessage = null)
            }
            send(cid, pending!!)
        }
    }

    fun retry() {
        viewModelScope.launch {
            val p = pending
            if (p == null) {
                hasStarted = false
                startIfNeeded()
                return@launch
            }
            val cid = resolveConversationId()
            if (cid == null) {
                showFatalError("No active conversation. Please go back and try again.")
                return@launch
            }
            _uiState.update {
                it.copy(phase = if (p.isInitialTrigger) Phase.Loading else Phase.Waiting, errorMessage = null)
            }
            send(cid, p)
        }
    }

    private suspend fun send(conversationId: String, p: Pending) {
        val result = repository.message(
            OnboardingMessageRequest(
                conversation_id = conversationId,
                message = p.message,
                screen_context = screenContext,
                structured_fields = normalize(p.structured)
            )
        )
        when (result) {
            is ApiResult.Success -> handleSuccess(
                reply = result.data.reply,
                responseConversationId = result.data.conversation_id,
                planBuilding = result.data.onboarding_state.plan_building,
                confirmedFields = result.data.onboarding_state.confirmed_fields
            )
            is ApiResult.Error ->
                _uiState.update { it.copy(phase = Phase.Error, errorMessage = result.message) }
        }
    }

    private suspend fun handleSuccess(
        reply: InterviewMessage,
        responseConversationId: String,
        planBuilding: Boolean,
        confirmedFields: List<String>?
    ) {
        pending = null
        conversationId = responseConversationId
        repository.saveConversationId(responseConversationId)
        sessionStore.setConversationId(responseConversationId)
        sessionStore.applyConfirmedFields(confirmedFields)

        if (planBuilding) {
            _uiState.update {
                it.copy(messages = it.messages + reply, phase = Phase.Transitioning, errorMessage = null)
            }
            _planBuilding.tryEmit(Unit)
        } else {
            _uiState.update {
                it.copy(messages = it.messages + reply, phase = Phase.Active, errorMessage = null)
            }
        }
    }

    /** Drop an all-null structured payload so we never send an empty object. */
    private fun normalize(structured: StructuredFields?): StructuredFields? =
        if (structured == null || structured.isEmpty) null else structured

    private suspend fun resolveConversationId(): String? {
        conversationId?.let { return it }
        sessionStore.conversationId.value?.let { conversationId = it; return it }
        repository.getConversationId()?.let { conversationId = it; return it }
        return when (val result = repository.status()) {
            is ApiResult.Success -> {
                val data = result.data
                if (data.status == OnboardingStatus.interview && data.conversation_id != null) {
                    conversationId = data.conversation_id
                    repository.saveConversationId(data.conversation_id)
                    data.conversation_id
                } else null
            }
            is ApiResult.Error -> null
        }
    }

    private fun makeMessage(role: InterviewMessageRole, content: String): InterviewMessage =
        InterviewMessage(
            id = UUID.randomUUID().toString(),
            role = role,
            content = content,
            agent_used = null,
            created_at = Instant.now().toString()
        )

    private fun showFatalError(message: String) {
        _uiState.update { it.copy(phase = Phase.Error, errorMessage = message) }
    }

    fun onSignOut() {
        viewModelScope.launch {
            tokenStore.clearToken()
            unauthorizedFlow.tryEmit(Unit)
        }
    }
}
