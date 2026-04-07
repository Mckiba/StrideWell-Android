package com.stridewell.app.ui.main.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stridewell.app.api.ApiResult
import com.stridewell.app.data.ChatRepository
import com.stridewell.app.data.PlanRepository
import com.stridewell.app.model.AgentUsed
import com.stridewell.app.model.ChatMessage
import com.stridewell.app.model.MessageRole
import com.stridewell.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val planRepository: PlanRepository
) : ViewModel() {

    data class UiState(
        val messages: List<ChatMessage> = emptyList(),
        val inputText: String = "",
        val isInitialLoading: Boolean = true,
        val isWaitingReply: Boolean = false,
        val errorMessage: String? = null,
        val hasMoreHistory: Boolean = false,
        val isLoadingHistory: Boolean = false,
        val isOffline: Boolean = false
    ) {
        val canSend: Boolean
            get() = inputText.trim().isNotEmpty() && !isWaitingReply && !isOffline
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pendingMessage: String? = null
    private var initialHistoryLoaded = false

    init {
        viewModelScope.launch {
            combine(
                chatRepository.messages,
                chatRepository.hasMoreHistory,
                chatRepository.isLoadingHistory
            ) { messages, hasMore, isLoadingHistory ->
                Triple(messages, hasMore, isLoadingHistory)
            }.collect { (messages, hasMore, isLoadingHistory) ->
                _uiState.update {
                    it.copy(
                        messages = messages,
                        hasMoreHistory = hasMore,
                        isLoadingHistory = isLoadingHistory
                    )
                }
            }
        }

        viewModelScope.launch {
            chatRepository.isOffline.collect { offline ->
                _uiState.update { it.copy(isOffline = offline) }
            }
        }

        loadInitialHistory(force = false)
    }

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(inputText = value) }
    }

    fun sendFromInput() {
        sendMessage(_uiState.value.inputText, clearInput = true, appendOptimisticUserMessage = true)
    }

    fun sendPrompt(prompt: String) {
        sendMessage(prompt, clearInput = false, appendOptimisticUserMessage = true)
    }

    fun retry() {
        val message = pendingMessage ?: return
        sendMessage(message, clearInput = false, appendOptimisticUserMessage = false)
    }

    fun retryError() {
        if (pendingMessage != null) {
            retry()
        } else {
            loadInitialHistory(force = true)
        }
    }

    fun loadMoreHistory() {
        if (_uiState.value.isLoadingHistory || !_uiState.value.hasMoreHistory) return
        viewModelScope.launch {
            when (chatRepository.loadMoreHistory()) {
                is ApiResult.Success -> Unit
                is ApiResult.Error -> Unit
            }
        }
    }

    fun refreshInitialHistory() {
        loadInitialHistory(force = false)
    }

    private fun loadInitialHistory(force: Boolean) {
        if (!force && initialHistoryLoaded) return
        if (!force) initialHistoryLoaded = true
        viewModelScope.launch {
            _uiState.update { it.copy(isInitialLoading = true, errorMessage = null) }
            chatRepository.ensureConversationIdLoaded()
            when (val result = chatRepository.loadInitialHistory()) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isInitialLoading = false) }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isInitialLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun sendMessage(
        rawContent: String,
        clearInput: Boolean,
        appendOptimisticUserMessage: Boolean
    ) {
        val content = rawContent.trim()
        if (content.isEmpty() || _uiState.value.isWaitingReply) return

        if (appendOptimisticUserMessage) {
            val optimistic = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = MessageRole.user,
                content = content,
                agent_used = null,
                created_at = Instant.now().toString()
            )
            chatRepository.appendLocalMessage(optimistic)
        }

        pendingMessage = content
        _uiState.update {
            it.copy(
                inputText = if (clearInput) "" else it.inputText,
                isWaitingReply = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = chatRepository.sendMessage(content)) {
                is ApiResult.Success -> {
                    pendingMessage = null
                    chatRepository.setConversationId(result.data.conversation_id)
                    chatRepository.appendLocalMessage(result.data.message)
                    _uiState.update { it.copy(isWaitingReply = false, errorMessage = null) }
                    maybeRefreshPlanAfterAdjuster(result.data.message.agent_used)
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isWaitingReply = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun maybeRefreshPlanAfterAdjuster(agentUsed: AgentUsed?) {
        if (agentUsed != AgentUsed.adjuster) return
        viewModelScope.launch {
            delay(5000)
            val mondayStart = DateUtils.mondayString(containing = Date())
            val todayDeferred = async { planRepository.today() }
            val weekDeferred = async { planRepository.week(mondayStart) }

            when (val result = todayDeferred.await()) {
                is ApiResult.Success -> planRepository.setTodayPlanDay(result.data)
                is ApiResult.Error -> Unit
            }

            when (val result = weekDeferred.await()) {
                is ApiResult.Success -> planRepository.setWeekData(result.data)
                is ApiResult.Error -> Unit
            }
        }
    }
}
