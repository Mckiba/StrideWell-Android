package com.stridewell.app.api

import com.stridewell.BuildConfig
import com.stridewell.app.data.SessionTokens
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
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Attaches the JWT Bearer token to every outgoing request, refreshing it first
 * when it is close to expiring.
 *
 * Refreshing up front means a burst of screen loads after a cold start does not
 * each spend a wasted 401 round trip discovering the same expiry. A refresh that
 * fails here is not fatal: the stored token may still be good, and if it is not
 * the 401 path takes over.
 */
class AuthInterceptor(
    private val tokenStore: SessionTokens,
    private val refresher: SessionRefresher,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (tokenStore.getToken() != null && refresher.isAccessTokenNearExpiry()) {
            refresher.refreshIfNeeded(force = false)
        }

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
 * Attaches the device's IANA timezone to every outgoing request so backend
 * code can resolve "today" in the user's local calendar instead of UTC.
 * Without this, west-of-UTC users at late evening lose a day of plan
 * windowing (e.g. adjuster's firstDay jumps two calendar days ahead locally).
 */
class TimezoneInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-Timezone", TimeZone.getDefault().id)
            .build()
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
    private val tokenStore: SessionTokens,
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
            // While a refresh token is stored, TokenAuthenticator has already seen
            // this 401 and decided what it means. It clears the session and emits
            // itself when the token is dead, and deliberately leaves the session
            // alone when the refresh merely could not be completed — so emitting
            // here as well would sign the user out on a network blip.
            val recoverable = !tokenStore.getRefreshToken().isNullOrEmpty()
            if (!isAuthEndpoint && !recoverable) {
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
    // One refresher shared by the pre-flight interceptor and the 401 authenticator,
    // so the two paths cannot refresh concurrently.
    val refresher = SessionRefresher(tokenStore, appJson)

    return OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore, refresher))
        .addInterceptor(TimezoneInterceptor())
        .addInterceptor(UnauthorizedInterceptor(tokenStore, unauthorizedFlow))
        .addInterceptor(logging)
        // Refreshes the access token on 401 and retries the request transparently.
        .authenticator(TokenAuthenticator(tokenStore, unauthorizedFlow, refresher))
        .callTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
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
