package com.stridewell.app.data

import com.stridewell.app.api.ApiResult
import com.stridewell.app.api.NotificationsApi
import com.stridewell.app.model.RegisterTokenRequest
import com.stridewell.app.model.RegisterTokenResponse
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class NotificationsRepository @Inject constructor(
    private val api: NotificationsApi,
) {
    suspend fun registerToken(token: String): ApiResult<RegisterTokenResponse> =
        safeCall { api.registerToken(RegisterTokenRequest(deviceToken = token, platform = "android")) }

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
                ApiResult.Error(response.code(), response.message())
            }
        } catch (e: IOException) {
            ApiResult.Error(0, "No internet connection.")
        } catch (e: Exception) {
            ApiResult.Error(-1, e.message ?: "Unknown error")
        }
    }
}
