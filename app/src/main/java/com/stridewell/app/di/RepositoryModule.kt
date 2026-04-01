package com.stridewell.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds repository interfaces to their implementations.
 * Populated incrementally as repositories are added in later milestones.
 * AuthRepository uses @Inject constructor directly — no binding needed.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule
