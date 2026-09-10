package com.stridewell.app.api

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The rule these cover: only a refresh the server rejects may clear the session.
 * Anything that merely prevented the refresh from completing must leave it alone.
 */
class SessionRefresherTest {

    private lateinit var server: MockWebServer
    private lateinit var tokens: FakeSessionTokens
    private val slept = mutableListOf<Long>()

    private fun refresher() = SessionRefresher(
        tokenStore = tokens,
        json = appJson,
        baseUrl = server.url("/").toString().trimEnd('/'),
        sleep = { slept += it },
    )

    private fun sessionBody(token: String = "access_v2", refresh: String = "refresh_v2") =
        """{"token":"$token","user_id":"user_abc","refresh_token":"$refresh","expires_at":${
            System.currentTimeMillis() / 1000 + 3600
        }}"""

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        tokens = FakeSessionTokens()
        slept.clear()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ── Transient: the session must survive ──────────────────────────────────

    @Test
    fun `503 is transient and retried without clearing the session`() {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(503)) }

        val outcome = refresher().refreshIfNeeded(force = true)

        assertEquals(SessionRefresher.Outcome.TRANSIENT_FAILED, outcome)
        assertEquals("initial attempt plus two retries", 3, server.requestCount)
        assertEquals(listOf(500L, 1000L), slept)
        assertFalse(tokens.cleared)
        assertEquals("refresh_v1", tokens.getRefreshToken())
    }

    @Test
    fun `an unreachable server is transient`() {
        repeat(3) {
            server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AT_START })
        }

        val outcome = refresher().refreshIfNeeded(force = true)

        assertEquals(SessionRefresher.Outcome.TRANSIENT_FAILED, outcome)
        assertFalse(tokens.cleared)
    }

    @Test
    fun `a rate limit is transient`() {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(429)) }

        assertEquals(SessionRefresher.Outcome.TRANSIENT_FAILED, refresher().refreshIfNeeded(force = true))
        assertFalse(tokens.cleared)
    }

    @Test
    fun `a transient failure that clears up mid-retry still succeeds`() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(200).setBody(sessionBody()))

        assertEquals(SessionRefresher.Outcome.REFRESHED, refresher().refreshIfNeeded(force = true))
        assertEquals("access_v2", tokens.getToken())
        assertFalse(tokens.cleared)
    }

    // ── Auth failure: the session is gone ────────────────────────────────────

    @Test
    fun `401 is an auth failure and is not retried`() {
        server.enqueue(MockResponse().setResponseCode(401))

        assertEquals(SessionRefresher.Outcome.AUTH_FAILED, refresher().refreshIfNeeded(force = true))
        assertEquals("a rejected token must not be presented again", 1, server.requestCount)
    }

    @Test
    fun `a missing refresh token cannot be refreshed`() {
        tokens = FakeSessionTokens(refreshToken = null)

        assertEquals(SessionRefresher.Outcome.NO_REFRESH_TOKEN, refresher().refreshIfNeeded(force = true))
        assertEquals(0, server.requestCount)
    }

    // ── Pre-flight behaviour ─────────────────────────────────────────────────

    @Test
    fun `a token far from expiry is left alone`() {
        assertEquals(SessionRefresher.Outcome.REFRESHED, refresher().refreshIfNeeded(force = false))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a token near expiry is refreshed up front`() {
        tokens.expireIn(30)
        server.enqueue(MockResponse().setResponseCode(200).setBody(sessionBody()))

        assertEquals(SessionRefresher.Outcome.REFRESHED, refresher().refreshIfNeeded(force = false))
        assertEquals(1, server.requestCount)
        assertEquals("access_v2", tokens.getToken())
    }

    @Test
    fun `an unknown expiry is treated as near expiry`() {
        tokens = FakeSessionTokens(expiresAt = -1).also { it.saveSession("access_v1", "refresh_v1", null) }
        server.enqueue(MockResponse().setResponseCode(200).setBody(sessionBody()))

        assertEquals(SessionRefresher.Outcome.REFRESHED, refresher().refreshIfNeeded(force = false))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `concurrent callers share a single refresh`() {
        tokens.expireIn(30)
        server.enqueue(MockResponse().setResponseCode(200).setBody(sessionBody()).setBodyDelay(300, java.util.concurrent.TimeUnit.MILLISECONDS))

        val refresher = refresher()
        val threads = (1..6).map { Thread { refresher.refreshIfNeeded(force = false) } }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals("refreshes must coalesce", 1, server.requestCount)
        assertTrue(tokens.saveCount >= 1)
    }
}
