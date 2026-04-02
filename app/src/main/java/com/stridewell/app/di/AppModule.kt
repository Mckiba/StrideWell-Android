package com.stridewell.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.stridewell.app.api.AuthApi
import com.stridewell.app.api.OnboardingApi
import com.stridewell.app.api.StravaApi
import com.stridewell.app.api.buildOkHttpClient
import com.stridewell.app.api.buildRetrofit
import com.stridewell.app.data.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

private val Context.onboardingDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "onboarding_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Core ──────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideUnauthorizedFlow(): MutableSharedFlow<Unit> =
        MutableSharedFlow(extraBufferCapacity = 1)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenStore: TokenStore,
        unauthorizedFlow: MutableSharedFlow<Unit>
    ): OkHttpClient = buildOkHttpClient(tokenStore, unauthorizedFlow)

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        buildRetrofit(okHttpClient)

    // ── Retrofit APIs ─────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideOnboardingApi(retrofit: Retrofit): OnboardingApi =
        retrofit.create(OnboardingApi::class.java)

    @Provides
    @Singleton
    fun provideStravaApi(retrofit: Retrofit): StravaApi =
        retrofit.create(StravaApi::class.java)

    // ── DataStore ─────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("onboarding")
    fun provideOnboardingDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.onboardingDataStore

    // ── OAuth ─────────────────────────────────────────────────────────────────

    /**
     * Shared OAuth authorization code channel.
     * MainActivity emits the code extracted from the Strava redirect deep link.
     * StravaConnectViewModel collects and exchanges it with the backend.
     */
    @Provides
    @Singleton
    @Named("oauthCode")
    fun provideOauthCodeFlow(): MutableStateFlow<String?> = MutableStateFlow(null)

    /**
     * Shared Apple ID-token channel.
     * MainActivity emits the id_token extracted from the Apple OAuth deep link.
     * SocialAuthViewModel collects it and exchanges with the backend.
     */
    @Provides
    @Singleton
    @Named("appleOAuthToken")
    fun provideAppleOAuthTokenFlow(): MutableStateFlow<String?> = MutableStateFlow(null)
}
