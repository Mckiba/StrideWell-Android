package com.stridewell.app.api

import com.stridewell.app.model.ChatHistoryResponse
import com.stridewell.app.model.ChatMessageRequest
import com.stridewell.app.model.ChatMessageResponse
import com.stridewell.app.model.FeedbackRequest
import com.stridewell.app.model.FeedbackResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    @POST("chat/message")
    suspend fun message(
        @Body body: ChatMessageRequest
    ): Response<ChatMessageResponse>

    @GET("chat/history")
    suspend fun history(
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null
    ): Response<ChatHistoryResponse>

    @PUT("chat/messages/{messageId}/feedback")
    suspend fun sendFeedback(
        @Path("messageId") messageId: String,
        @Body body: FeedbackRequest
    ): Response<FeedbackResponse>
}
