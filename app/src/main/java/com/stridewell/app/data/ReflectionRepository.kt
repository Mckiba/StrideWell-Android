package com.stridewell.app.data

import com.stridewell.app.api.ApiResult
import com.stridewell.app.api.ReflectionApi
import com.stridewell.app.model.ReflectionResponse
import com.stridewell.app.model.ReflectionSubmission
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class ReflectionRepository @Inject constructor(
    private val api: ReflectionApi
) {

    suspend fun submit(submission: ReflectionSubmission): ApiResult<ReflectionResponse> =
        safeCall { api.submit(submission) }

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
            ApiResult.Error(0, "No internet connection. Please check your network.")
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Unknown error")
        }
    }

    private fun String.extractMessage(): String {
        val match = Regex(""""(?:message|err|error)"\s*:\s*"([^"]+)"""").find(this)
        return match?.groupValues?.get(1) ?: this
    }
}
