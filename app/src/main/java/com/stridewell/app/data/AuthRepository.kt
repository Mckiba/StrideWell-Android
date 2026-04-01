package com.stridewell.app.data

import com.stridewell.app.api.ApiResult
import com.stridewell.app.api.AuthApi
import com.stridewell.app.model.ForgotPasswordResponse
import com.stridewell.app.model.ForgotPasswordRequest
import com.stridewell.app.model.LoginRequest
import com.stridewell.app.model.LoginResponse
import com.stridewell.app.model.MeResponse
import com.stridewell.app.model.RegisterRequest
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore
) {

    suspend fun register(email: String, password: String): ApiResult<LoginResponse> =
        safeCall { api.register(RegisterRequest(email, password)) }

    suspend fun login(email: String, password: String): ApiResult<LoginResponse> =
        safeCall { api.login(LoginRequest(email, password)) }

    suspend fun me(): ApiResult<MeResponse> =
        safeCall { api.me() }

    suspend fun forgotPassword(email: String): ApiResult<ForgotPasswordResponse> =
        safeCall { api.forgotPassword(ForgotPasswordRequest(email)) }

    suspend fun deleteAccount(): ApiResult<Unit> =
        safeCallUnit { api.deleteAccount() }

    // MARK: - Helpers

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

    private suspend fun safeCallUnit(call: suspend () -> Response<Unit>): ApiResult<Unit> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
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

    // Extract "message" field from a JSON error body, fallback to raw string
    private fun String.extractMessage(): String {
        val match = Regex(""""message"\s*:\s*"([^"]+)"""").find(this)
        return match?.groupValues?.get(1) ?: this
    }
}
