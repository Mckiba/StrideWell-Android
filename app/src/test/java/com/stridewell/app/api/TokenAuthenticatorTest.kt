package com.stridewell.app.api

import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
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
 * Exercises the whole client chain — pre-flight refresh, 401 authenticator and the
 * unauthorized interceptor — because the sign-out decision is split across all
 * three and only their combination decides whether the user keeps their session.
 */
class TokenAuthenticatorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokens: FakeSessionTokens
    private lateinit var unauthorized: MutableSharedFlow<Unit>

    private fun buildClient(): OkHttpClient {
        val refresher = SessionRefresher(
            tokenStore = tokens,
            json = appJson,
            baseUrl = server.url("/").toString().trimEnd('/'),
            sleep = { },
        )
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokens, refresher))
            .addInterceptor(UnauthorizedInterceptor(tokens, unauthorized))
            .authenticator(TokenAuthenticator(tokens, unauthorized, refresher))
            .build()
    }

    private fun protectedRequest() =
        Request.Builder().url(server.url("/plan/today")).build()

    private fun sessionBody() =
        """{"token":"access_v2","user_id":"user_abc","refresh_token":"refresh_v2","expires_at":${
            System.currentTimeMillis() / 1000 + 3600
        }}"""

    private fun signedOut() = unauthorized.replayCache.isNotEmpty()

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        tokens = FakeSessionTokens()
        unauthorized = MutableSharedFlow(replay = 1, extraBufferCapacity = 1)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `a refresh that cannot complete does not sign the user out`() {
        // The regression: a 503 from /auth/refresh used to be indistinguishable
        // from a rejected token, so a momentary outage wiped the session.
        server.enqueue(MockResponse().setResponseCode(401))
        repeat(3) { server.enqueue(MockResponse().setResponseCode(503)) }

        val response = buildClient().newCall(protectedRequest()).execute()

        assertEquals(401, response.code)
        assertFalse("session must survive", tokens.cleared)
        assertFalse("must not navigate to sign-in", signedOut())
        assertEquals("refresh_v1", tokens.getRefreshToken())
        response.close()
    }

    @Test
    fun `an unreachable server does not sign the user out`() {
        server.enqueue(MockResponse().setResponseCode(401))
        repeat(3) {
            server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AT_START })
        }

        val response = buildClient().newCall(protectedRequest()).execute()

        assertEquals(401, response.code)
        assertFalse(tokens.cleared)
        assertFalse(signedOut())
        response.close()
    }

    @Test
    fun `a rejected refresh token signs the user out`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        val response = buildClient().newCall(protectedRequest()).execute()

        assertEquals(401, response.code)
        assertTrue("session must be cleared", tokens.cleared)
        assertTrue("must navigate to sign-in", signedOut())
        response.close()
    }

    @Test
    fun `a successful refresh replays the original request`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody(sessionBody()))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))

        val response = buildClient().newCall(protectedRequest()).execute()

        assertEquals(200, response.code)
        assertFalse(tokens.cleared)
        assertFalse(signedOut())
        assertEquals("access_v2", tokens.getToken())

        val first = server.takeRequest()
        val refresh = server.takeRequest()
        val retry = server.takeRequest()
        assertEquals("Bearer access_v1", first.getHeader("Authorization"))
        assertEquals("/auth/refresh", refresh.path)
        assertEquals("Bearer access_v2", retry.getHeader("Authorization"))
        response.close()
    }

    @Test
    fun `a near-expiry token is refreshed before the request is sent`() {
        tokens.expireIn(30)
        server.enqueue(MockResponse().setResponseCode(200).setBody(sessionBody()))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))

        val response = buildClient().newCall(protectedRequest()).execute()

        assertEquals(200, response.code)
        assertEquals("no 401 round trip should be needed", 2, server.requestCount)
        assertEquals("/auth/refresh", server.takeRequest().path)
        assertEquals("Bearer access_v2", server.takeRequest().getHeader("Authorization"))
        response.close()
    }

    @Test
    fun `a 401 with no refresh token signs the user out`() {
        tokens = FakeSessionTokens(refreshToken = null)
        server.enqueue(MockResponse().setResponseCode(401))

        val response = buildClient().newCall(protectedRequest()).execute()

        assertEquals(401, response.code)
        assertTrue(signedOut())
        response.close()
    }
}
