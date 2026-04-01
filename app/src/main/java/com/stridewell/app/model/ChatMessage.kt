package com.stridewell.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class MessageRole {
    user, assistant
}

@Serializable
enum class AgentUsed {
    coach, explainer, adjuster
}

@Serializable
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val agent_used: AgentUsed? = null,
    val created_at: String
)

@Serializable
data class ChatMessageRequest(
    val message: String,
    val conversation_id: String? = null
)

@Serializable
data class ChatMessageResponse(
    val conversation_id: String,
    val message: ChatMessage
)

@Serializable
data class ChatHistoryResponse(
    val messages: List<ChatMessage>,
    val has_more: Boolean
)
