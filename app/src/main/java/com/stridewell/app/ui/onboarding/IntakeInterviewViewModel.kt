package com.stridewell.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.OnboardingRepository
import com.stridewell.app.model.InterviewMessage
import com.stridewell.app.model.InterviewMessageRole
import com.stridewell.app.model.OnboardingMessageRequest
import com.stridewell.app.model.OnboardingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class IntakeInterviewViewModel @Inject constructor(
    private val repository: OnboardingRepository
) : ViewModel() {

    enum class Phase {
        Loading,
        Active,
        Waiting,
        Transitioning,
        Error,
    }

    data class UiState(
        val messages: List<InterviewMessage> = emptyList(),
        val inputText: String = "",
        val phase: Phase = Phase.Loading,
        val errorMessage: String? = null,
    ) {
        val canSend: Boolean
            get() = phase == Phase.Active && inputText.trim().isNotEmpty()

        val showTypingIndicator: Boolean
            get() = phase == Phase.Loading || phase == Phase.Waiting

        val showInputBar: Boolean
            get() = phase != Phase.Transitioning

        val inputEnabled: Boolean
            get() = phase != Phase.Loading && phase != Phase.Waiting
    }

    private data class PendingRequest(
        val message: InterviewMessage,
        val isInitialTrigger: Boolean,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _navigateToPlanBuilding = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToPlanBuilding: SharedFlow<Unit> = _navigateToPlanBuilding.asSharedFlow()

    private var conversationId: String? = null
    private var pendingRequest: PendingRequest? = null

    init {
        viewModelScope.launch { bootstrapConversation() }
    }

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(inputText = value) }
    }

    fun sendMessage() {
        val content = _uiState.value.inputText.trim()
        if (content.isEmpty() || _uiState.value.phase != Phase.Active) return

        viewModelScope.launch {
            val resolvedConversationId = resolveConversationId()
            if (resolvedConversationId == null) {
                showFatalError("No active conversation. Please go back and try again.")
                return@launch
            }

            val userMessage = makeMessage(role = InterviewMessageRole.user, content = content)
            pendingRequest = PendingRequest(message = userMessage, isInitialTrigger = false)

            _uiState.update {
                it.copy(
                    messages = it.messages + userMessage,
                    inputText = "",
                    phase = Phase.Waiting,
                    errorMessage = null
                )
            }

            sendPendingRequest(resolvedConversationId, requireNotNull(pendingRequest))
        }
    }

    fun retry() {
        viewModelScope.launch {
            val pending = pendingRequest
            if (pending == null && _uiState.value.messages.isEmpty()) {
                bootstrapConversation()
                return@launch
            }

            val resolvedConversationId = resolveConversationId()
            if (resolvedConversationId == null) {
                showFatalError("No active conversation. Please go back and try again.")
                return@launch
            }

            if (pending != null) {
                _uiState.update {
                    it.copy(
                        phase = if (pending.isInitialTrigger) Phase.Loading else Phase.Waiting,
                        errorMessage = null
                    )
                }
                sendPendingRequest(resolvedConversationId, pending)
            }
        }
    }

    private suspend fun bootstrapConversation() {
        _uiState.update { it.copy(phase = Phase.Loading, errorMessage = null) }

        val resolvedConversationId = resolveConversationId()
        if (resolvedConversationId == null) {
            showFatalError("No active conversation. Please go back and try again.")
            return
        }

        when (val historyResult = repository.history(conversationId = resolvedConversationId, limit = 50)) {
            is ApiResult.Success -> {
                if (historyResult.data.messages.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            messages = historyResult.data.messages,
                            phase = Phase.Active,
                            errorMessage = null
                        )
                    }
                } else {
                    triggerInitialMessage(resolvedConversationId)
                }
            }
            is ApiResult.Error -> {
                _uiState.update {
                    it.copy(
                        phase = Phase.Error,
                        errorMessage = historyResult.message
                    )
                }
            }
        }
    }

    private suspend fun triggerInitialMessage(conversationId: String) {
        val triggerMessage = makeMessage(role = InterviewMessageRole.user, content = "start")
        val pending = PendingRequest(message = triggerMessage, isInitialTrigger = true)
        pendingRequest = pending

        _uiState.update {
            it.copy(
                phase = Phase.Loading,
                errorMessage = null
            )
        }

        sendPendingRequest(conversationId, pending)
    }

    private suspend fun sendPendingRequest(
        conversationId: String,
        pending: PendingRequest,
    ) {
        when (
            val result = repository.message(
                OnboardingMessageRequest(
                    conversation_id = conversationId,
                    message = pending.message
                )
            )
        ) {
            is ApiResult.Success -> handleSuccess(result.data.reply, result.data.conversation_id, result.data.onboarding_state.plan_building)
            is ApiResult.Error -> {
                _uiState.update {
                    it.copy(
                        phase = Phase.Error,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    private suspend fun handleSuccess(
        reply: InterviewMessage,
        responseConversationId: String,
        planBuilding: Boolean,
    ) {
        pendingRequest = null
        conversationId = responseConversationId
        repository.saveConversationId(responseConversationId)

        if (planBuilding) {
            val buildingMessage = makeMessage(
                role = InterviewMessageRole.assistant,
                content = "Building your plan..."
            )
            _uiState.update {
                it.copy(
                    messages = it.messages + reply + buildingMessage,
                    phase = Phase.Transitioning,
                    errorMessage = null
                )
            }
            delay(1500)
            _navigateToPlanBuilding.tryEmit(Unit)
        } else {
            _uiState.update {
                it.copy(
                    messages = it.messages + reply,
                    phase = Phase.Active,
                    errorMessage = null
                )
            }
        }
    }

    private suspend fun resolveConversationId(): String? {
        conversationId?.let { return it }

        repository.getConversationId()?.let {
            conversationId = it
            return it
        }

        return when (val result = repository.status()) {
            is ApiResult.Success -> {
                val status = result.data.status
                if (status == OnboardingStatus.interview && result.data.conversation_id != null) {
                    conversationId = result.data.conversation_id
                    repository.saveConversationId(result.data.conversation_id)
                    result.data.conversation_id
                } else {
                    null
                }
            }
            is ApiResult.Error -> null
        }
    }

    private fun makeMessage(
        role: InterviewMessageRole,
        content: String,
    ): InterviewMessage = InterviewMessage(
        id = UUID.randomUUID().toString(),
        role = role,
        content = content,
        agent_used = null,
        created_at = Instant.now().toString()
    )

    private fun showFatalError(message: String) {
        _uiState.update {
            it.copy(
                phase = Phase.Error,
                errorMessage = message
            )
        }
    }
}
