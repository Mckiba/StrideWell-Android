package com.stridewell.app.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Shared exponential-backoff polling helper.
 *
 * Used by onboarding screens that wait for server-side state transitions
 * (e.g. Strava analysis complete, plan built).
 */
object Polling {

    /**
     * Polls [until] with exponential back-off until it returns true
     * or the calling coroutine is cancelled.
     *
     * @param initialMs First sleep duration in milliseconds (default 3 s).
     * @param maxMs     Maximum sleep duration in milliseconds (default 15 s).
     * @param stepMs    Amount added to the delay after each failed check (default 3 s).
     * @param until     Suspend function; return true to stop polling.
     */
    suspend fun exponentialBackoff(
        initialMs: Long = 3_000L,
        maxMs: Long     = 15_000L,
        stepMs: Long    = 3_000L,
        until: suspend () -> Boolean
    ) {
        var delayMs = initialMs
        while (true) {
            try {
                delay(delayMs)
            } catch (_: CancellationException) {
                return
            }
            if (until()) return
            delayMs = minOf(delayMs + stepMs, maxMs)
        }
    }
}
