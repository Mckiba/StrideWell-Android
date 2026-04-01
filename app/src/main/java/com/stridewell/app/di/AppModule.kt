package com.stridewell.app.di

import com.stridewell.app.api.AuthApi
import com.stridewell.app.api.buildOkHttpClient
import com.stridewell.app.api.buildRetrofit
import com.stridewell.app.data.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

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

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)
}
