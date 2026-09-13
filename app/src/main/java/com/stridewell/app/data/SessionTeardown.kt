package com.stridewell.app.data

import com.stridewell.app.ui.background.heatmap.HeatmapCache
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Clears every user-scoped cache. Explicit sign-out, account deletion and the
 * unrecoverable-401 path all run this, so they cannot drift apart and leave a
 * previous user's data behind for the next account on the device.
 */
@Singleton
class SessionTeardown @Inject constructor(
    private val tokenStore: TokenStore,
    private val settingsRepository: SettingsRepository,
    private val onboardingRepository: OnboardingRepository,
    private val planRepository: PlanRepository,
    private val chatRepository: ChatRepository,
    private val runsRepository: RunsRepository,
    private val activityRepository: ActivityRepository,
    private val heatmapCache: HeatmapCache
) {
    suspend fun clear() = coroutineScope {
        tokenStore.clearToken()
        heatmapCache.clearAll()
        listOf(
            async { settingsRepository.reset() },
            async { onboardingRepository.reset() },
            async { planRepository.reset() },
            async { chatRepository.reset() },
            async { runsRepository.reset() },
            async { activityRepository.reset() }
        ).awaitAll()
        Unit
    }
}
