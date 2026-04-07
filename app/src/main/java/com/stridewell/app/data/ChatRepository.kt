package com.stridewell.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.stridewell.app.api.ApiResult
import com.stridewell.app.api.ChatApi
import com.stridewell.app.model.ChatHistoryResponse
import com.stridewell.app.model.ChatMessage
import com.stridewell.app.model.ChatMessageRequest
import com.stridewell.app.model.ChatMessageResponse
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.Response

@Singleton
class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    @Named("chat") private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val KEY_CONVERSATION_ID  = stringPreferencesKey("chat_conversation_id")
        private val KEY_CACHED_HISTORY   = stringPreferencesKey("chat_cached_history")
        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    }

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _conversationId = MutableStateFlow<String?>(null)
    val conversationId: StateFlow<String?> = _conversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _hasMoreHistory = MutableStateFlow(false)
    val hasMoreHistory: StateFlow<Boolean> = _hasMoreHistory.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    private var oldestCursor: String? = null

    suspend fun ensureConversationIdLoaded() {
        if (_conversationId.value != null) return
        _conversationId.value = dataStore.data.first()[KEY_CONVERSATION_ID]
    }

    suspend fun setConversationId(conversationId: String) {
        _conversationId.value = conversationId
        dataStore.edit { prefs ->
            prefs[KEY_CONVERSATION_ID] = conversationId
        }
    }

    suspend fun reset() = clearInMemoryState(clearPersistedConversationId = true)

    suspend fun clearInMemoryState(clearPersistedConversationId: Boolean = true) {
        _conversationId.value = null
        _messages.value = emptyList()
        _hasMoreHistory.value = false
        _isLoadingHistory.value = false
        _isOffline.value = false
        oldestCursor = null

        if (clearPersistedConversationId) {
            dataStore.edit { prefs ->
                prefs.remove(KEY_CONVERSATION_ID)
                prefs.remove(KEY_CACHED_HISTORY)
            }
        }
    }

    suspend fun loadInitialHistory(limit: Int = 50): ApiResult<Unit> {
        if (_isLoadingHistory.value) return ApiResult.Success(Unit)
        _isLoadingHistory.value = true

        return try {
            when (val result = safeCall { chatApi.history(limit = limit, before = null) }) {
                is ApiResult.Success -> {
                    _isOffline.value = false
                    applyInitialHistory(result.data)
                    dataStore.edit { it[KEY_CACHED_HISTORY] = json.encodeToString(result.data) }
                    ApiResult.Success(Unit)
                }
                is ApiResult.Error -> {
                    if (result.status == 0) {
                        val stored = dataStore.data.first()[KEY_CACHED_HISTORY]
                        val cached = stored?.let {
                            runCatching { json.decodeFromString<ChatHistoryResponse>(it) }.getOrNull()
                        }
                        if (cached != null) {
                            _isOffline.value = true
                            applyInitialHistory(cached)
                            ApiResult.Success(Unit)
                        } else {
                            _isOffline.value = false
                            result
                        }
                    } else {
                        _isOffline.value = false
                        result
                    }
                }
            }
        } finally {
            _isLoadingHistory.value = false
        }
    }

    suspend fun loadMoreHistory(limit: Int = 50): ApiResult<Unit> {
        val before = oldestCursor
        if (_isLoadingHistory.value || !_hasMoreHistory.value || before == null) {
            return ApiResult.Success(Unit)
        }
        _isLoadingHistory.value = true

        return try {
            when (val result = safeCall { chatApi.history(limit = limit, before = before) }) {
                is ApiResult.Success -> {
                    applyOlderHistory(result.data)
                    ApiResult.Success(Unit)
                }
                is ApiResult.Error -> result
            }
        } finally {
            _isLoadingHistory.value = false
        }
    }

    suspend fun sendMessage(message: String): ApiResult<ChatMessageResponse> {
        ensureConversationIdLoaded()
        return safeCall {
            chatApi.message(
                ChatMessageRequest(
                    message = message,
                    conversation_id = _conversationId.value
                )
            )
        }
    }

    fun appendLocalMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
        if (oldestCursor == null) {
            oldestCursor = message.created_at
        }
    }

    private fun applyInitialHistory(history: ChatHistoryResponse) {
        _messages.value = history.messages
        _hasMoreHistory.value = history.has_more
        oldestCursor = history.messages.firstOrNull()?.created_at
    }

    private fun applyOlderHistory(history: ChatHistoryResponse) {
        if (history.messages.isNotEmpty()) {
            _messages.value = history.messages + _messages.value
            oldestCursor = history.messages.first().created_at
        }
        _hasMoreHistory.value = history.has_more
    }

    private suspend fun <T> safeCall(call: suspend () -> Response<T>): ApiResult<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ApiResult.Success(body)
                } else {
                    ApiResult.Error(response.code(), "Empty response body")
                }
            } else {
                val errorMessage = response.errorBody()?.string()?.extractMessage()
                    ?: response.message()
                ApiResult.Error(response.code(), errorMessage)
            }
        } catch (e: IOException) {
            ApiResult.Error(
                0,
                if (e.isTimeoutLike()) {
                    "The response took too long. Please try again."
                } else {
                    "No internet connection. Please check your network."
                }
            )
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Unknown error")
        }
    }

    private fun Throwable.isTimeoutLike(): Boolean {
        if (this is SocketTimeoutException || this is TimeoutException) return true
        val message = message?.lowercase().orEmpty()
        return "timeout" in message || "timed out" in message
    }

    private fun String.extractMessage(): String {
        val match = Regex(""""(?:message|err|error)"\s*:\s*"([^"]+)"""").find(this)
        return match?.groupValues?.get(1) ?: this
    }
}
