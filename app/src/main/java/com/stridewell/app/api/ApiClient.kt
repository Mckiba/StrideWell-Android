package com.stridewell.app.api

import com.stridewell.BuildConfig
import com.stridewell.app.data.TokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi

/** Attaches the JWT Bearer token to every outgoing request. */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

/**
 * Watches for 401 responses on PROTECTED endpoints and emits on [unauthorizedFlow]
 * so MainActivity can navigate to WelcomeScreen and clear auth state.
 *
 * Auth endpoints (login, register, forgot-password) are explicitly excluded —
 * a 401 there means wrong credentials, not an expired session. Those errors
 * are handled by the ViewModel via ApiResult.Error.
 */
class UnauthorizedInterceptor(
    private val unauthorizedFlow: MutableSharedFlow<Unit>
) : Interceptor {

    companion object {
        /** Paths that legitimately return 401 for bad credentials, not bad tokens. */
        private val AUTH_PATHS = setOf(
            "auth/login",
            "auth/register",
            "auth/forgot-password",
            "auth/google",
            "auth/apple"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            val path = chain.request().url.encodedPath.trimStart('/')
            val isAuthEndpoint = AUTH_PATHS.any { path.startsWith(it) }
            if (!isAuthEndpoint) {
                unauthorizedFlow.tryEmit(Unit)
            }
        }
        return response
    }
}

val appJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

fun buildOkHttpClient(
    tokenStore: TokenStore,
    unauthorizedFlow: MutableSharedFlow<Unit>
): OkHttpClient {
    val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    return OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .addInterceptor(UnauthorizedInterceptor(unauthorizedFlow))
        .addInterceptor(logging)
        .build()
}

@OptIn(ExperimentalSerializationApi::class)
fun buildRetrofit(okHttpClient: OkHttpClient): Retrofit {
    val contentType = "application/json".toMediaType()
    return Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL + "/")
        .client(okHttpClient)
        .addConverterFactory(appJson.asConverterFactory(contentType))
        .build()
}
