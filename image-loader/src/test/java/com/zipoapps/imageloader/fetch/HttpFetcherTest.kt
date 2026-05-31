package com.zipoapps.imageloader.fetch

import com.zipoapps.imageloader.ImageLoaderLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [HttpFetcher].
 *
 * ### Testing strategy
 * - **[MockWebServer]** for the success path: exercises the full OkHttp ↔ coroutine bridge
 *   with a real local HTTP server.
 * - **[Call.Factory] mock** for all failure paths: captures the [Callback] from
 *   [Call.enqueue] and invokes it synchronously. This avoids real networking (no OS-level
 *   TCP behaviour differences between platforms) and makes the tests deterministic.
 *
 * [HttpFetcher] accepts `Call.Factory` (an interface) which makes failure-path injection
 * straightforward without needing to mock `OkHttpClient` (a final class).
 *
 * Both styles use [runBlocking] because [kotlinx.coroutines.suspendCancellableCoroutine]
 * is resumed by a real OkHttp thread, not the virtual-time scheduler used by
 * [kotlinx.coroutines.test.runTest].
 */
class HttpFetcherTest {

    private val server = MockWebServer()
    private val logger = ImageLoaderLogger(enabled = false)

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    // ── Success path (real MockWebServer) ─────────────────────────────────────

    @Test
    fun `returns body bytes on 200 OK`() = runBlocking {
        val expectedBody = "fake image bytes".toByteArray()
        server.enqueue(MockResponse().setBody(String(expectedBody)).setResponseCode(200))
        val fetcher = HttpFetcher(okHttpClient = null, timeoutMs = 5_000L, logger = logger)

        val result = withTimeout(10_000) {
            fetcher.fetch(server.url("/image.jpg").toString())
        }

        assertArrayEquals("Body bytes must match", expectedBody, result)
    }

    @Test
    fun `returns empty bytes on 200 with empty body`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val fetcher = HttpFetcher(okHttpClient = null, timeoutMs = 5_000L, logger = logger)

        val result = withTimeout(10_000) {
            fetcher.fetch(server.url("/empty.jpg").toString())
        }

        assertTrue("Empty body must return empty byte array", result != null && result.isEmpty())
    }

    // ── Failure paths (Call.Factory mock — deterministic) ─────────────────────

    @Test
    fun `returns null on network failure (onFailure callback)`() = runBlocking {
        val fetcher = fetcherWithCallback { callback ->
            callback.onFailure(mockk(relaxed = true), IOException("no connection"))
        }
        assertNull("Network failure must return null", fetcher.fetch(FAKE_URL))
    }

    @Test
    fun `returns null on 404 response`() = runBlocking {
        val fetcher = fetcherWithCallback { callback ->
            callback.onResponse(mockk(relaxed = true), fakeResponse(404, "Not Found"))
        }
        assertNull("HTTP 404 must return null", fetcher.fetch(FAKE_URL))
    }

    @Test
    fun `returns null on 500 server error`() = runBlocking {
        val fetcher = fetcherWithCallback { callback ->
            callback.onResponse(mockk(relaxed = true), fakeResponse(500, "Internal Server Error"))
        }
        assertNull("HTTP 500 must return null", fetcher.fetch(FAKE_URL))
    }

    @Test
    fun `returns null when body read throws IOException`() = runBlocking {
        val brokenBody = mockk<okhttp3.ResponseBody> {
            every { bytes() } throws IOException("stream closed")
            every { close() } returns Unit
            every { contentType() } returns null
            every { contentLength() } returns -1L
        }
        val fetcher = fetcherWithCallback { callback ->
            callback.onResponse(mockk(relaxed = true), fakeResponse(200, body = brokenBody))
        }
        assertNull("Body read failure must return null", fetcher.fetch(FAKE_URL))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates an [HttpFetcher] backed by a [Call.Factory] that captures [Callback] from
     * [Call.enqueue] and immediately invokes [callbackBlock] with it. The callback fires
     * synchronously on the calling thread, keeping these tests deterministic.
     */
    private fun fetcherWithCallback(callbackBlock: (Callback) -> Unit): HttpFetcher {
        val callbackSlot = slot<Callback>()
        val mockCall: Call = mockk(relaxed = true) {
            every { enqueue(capture(callbackSlot)) } answers {
                callbackBlock(callbackSlot.captured)
            }
        }
        val factory = Call.Factory { mockCall }
        return HttpFetcher(callFactory = factory, logger = logger)
    }

    private fun fakeResponse(
        code: Int,
        bodyText: String = "",
        body: okhttp3.ResponseBody? = null,
    ): Response = Response.Builder()
        .request(Request.Builder().url(FAKE_URL).build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("fake")
        .body(body ?: bodyText.toResponseBody("application/octet-stream".toMediaType()))
        .build()

    private companion object {
        private const val FAKE_URL = "https://example.com/image.jpg"
    }
}
