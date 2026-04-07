package com.stridewell.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.stridewell.app.api.AuthApi
import com.stridewell.app.api.ChatApi
import com.stridewell.app.api.NotificationsApi
import com.stridewell.app.api.OnboardingApi
import com.stridewell.app.api.PlanApi
import com.stridewell.app.api.ReflectionApi
import com.stridewell.app.api.RunsApi
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

private val Context.settingsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "settings_prefs")

private val Context.planDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "plan_prefs")

private val Context.chatDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "chat_prefs")

private val Context.runsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "runs_prefs")

private val Context.activityDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "activity_prefs")

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
    fun provideChatApi(retrofit: Retrofit): ChatApi =
        retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun provideStravaApi(retrofit: Retrofit): StravaApi =
        retrofit.create(StravaApi::class.java)

    @Provides
    @Singleton
    fun providePlanApi(retrofit: Retrofit): PlanApi =
        retrofit.create(PlanApi::class.java)

    @Provides
    @Singleton
    fun provideRunsApi(retrofit: Retrofit): RunsApi =
        retrofit.create(RunsApi::class.java)

    @Provides
    @Singleton
    fun provideReflectionApi(retrofit: Retrofit): ReflectionApi =
        retrofit.create(ReflectionApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationsApi(retrofit: Retrofit): NotificationsApi =
        retrofit.create(NotificationsApi::class.java)

    // ── DataStore ─────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("onboarding")
    fun provideOnboardingDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.onboardingDataStore

    @Provides
    @Singleton
    @Named("settings")
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.settingsDataStore

    @Provides
    @Singleton
    @Named("plan")
    fun providePlanDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.planDataStore

    @Provides
    @Singleton
    @Named("chat")
    fun provideChatDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.chatDataStore

    @Provides
    @Singleton
    @Named("runs")
    fun provideRunsDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.runsDataStore

    @Provides
    @Singleton
    @Named("activity")
    fun provideActivityDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.activityDataStore

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

    /**
     * Shared notification deep link channel.
     * MainActivity emits the deep_link value from a notification tap intent extra.
     * StridewellNavHost collects it and navigates to the appropriate screen.
     */
    @Provides
    @Singleton
    @Named("notificationDeepLink")
    fun provideNotificationDeepLinkFlow(): MutableStateFlow<String?> = MutableStateFlow(null)

    /**
     * Shared message channel for opening Chat from Home/PlanChange with a pre-sent prompt.
     * MainContainer consumes the message, switches to Chat tab, sends it once, then clears it.
     */
    @Provides
    @Singleton
    @Named("chatEntryMessage")
    fun provideChatEntryMessageFlow(): MutableStateFlow<String?> = MutableStateFlow(null)
}
