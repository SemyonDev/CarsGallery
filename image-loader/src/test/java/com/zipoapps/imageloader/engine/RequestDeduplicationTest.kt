package com.zipoapps.imageloader.engine

import android.graphics.Bitmap
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests [RequestDeduplicator] in isolation — no Android dependencies required.
 *
 * ### Why UnconfinedTestDispatcher for the cancellation test
 * The primary-cancellation test requires precise interleaving: primary must suspend *before*
 * secondary registers, and secondary must be awaiting the deferred *before* primary is
 * cancelled. [UnconfinedTestDispatcher] runs coroutines eagerly to the next suspension point
 * (like [Dispatchers.Unconfined]) so each coroutine predictably advances until it suspends,
 * giving us the ordering we need without sleep or countdown latches.
 */
class RequestDeduplicationTest {

    @Test
    fun `concurrent requests for same URL invoke loader exactly once`() = runTest {
        val callCount = AtomicInteger(0)
        val fakeBitmap: Bitmap = mockk()
        val deduplicator = RequestDeduplicator()
        val url = "https://example.com/image.jpg"

        val results = (1..10).map {
            async {
                deduplicator.getOrLoad(url) {
                    callCount.incrementAndGet()
                    delay(50)  // virtual delay — all 10 coroutines register before this resumes
                    fakeBitmap
                }
            }
        }.awaitAll()

        assertEquals("All 10 callers should receive the bitmap", 10, results.filterNotNull().size)
        assertEquals("Loader must be invoked exactly once", 1, callCount.get())
    }

    @Test
    fun `sequential requests each invoke the loader independently`() = runTest {
        val callCount = AtomicInteger(0)
        val fakeBitmap: Bitmap = mockk()
        val deduplicator = RequestDeduplicator()
        val url = "https://example.com/image.jpg"

        deduplicator.getOrLoad(url) { callCount.incrementAndGet(); fakeBitmap }
        deduplicator.getOrLoad(url) { callCount.incrementAndGet(); fakeBitmap }

        assertEquals("Sequential requests are independent — loader called twice", 2, callCount.get())
    }

    @Test
    fun `concurrent requests for different URLs each invoke the loader`() = runTest {
        val callCount = AtomicInteger(0)
        val fakeBitmap: Bitmap = mockk()
        val deduplicator = RequestDeduplicator()

        val results = (1..5).map { i ->
            async {
                deduplicator.getOrLoad("https://example.com/$i.jpg") {
                    callCount.incrementAndGet()
                    delay(50)
                    fakeBitmap
                }
            }
        }.awaitAll()

        assertEquals(5, results.filterNotNull().size)
        assertEquals("Each distinct URL must have its own loader call", 5, callCount.get())
    }

    @Test
    fun `loader returning null propagates null to all waiters`() = runTest {
        val deduplicator = RequestDeduplicator()
        val url = "https://example.com/broken.jpg"

        val results = (1..4).map {
            async {
                deduplicator.getOrLoad(url) {
                    delay(10)
                    null  // simulate decode failure
                }
            }
        }.awaitAll()

        results.forEach { assertNull("Every waiter should receive null on loader failure", it) }
    }

    @Test
    fun `failed loader allows next sequential request to retry`() = runTest {
        val callCount = AtomicInteger(0)
        val fakeBitmap: Bitmap = mockk()
        val deduplicator = RequestDeduplicator()
        val url = "https://example.com/retry.jpg"

        val first = deduplicator.getOrLoad(url) {
            callCount.incrementAndGet()
            throw RuntimeException("transient error")
        }

        val second = deduplicator.getOrLoad(url) {
            callCount.incrementAndGet()
            fakeBitmap
        }

        assertNull("Failed load should return null", first)
        assertEquals("Successful retry should return bitmap", fakeBitmap, second)
        assertEquals("Loader invoked once per attempt", 2, callCount.get())
    }

    /**
     * Regression test for the deduplication-key/cache-key mismatch bug.
     *
     * Before the fix, [RequestManager] keyed deduplication by raw URL while the cache used a
     * size-aware key. Two concurrent requests for the same URL at different sizes would share
     * one deduplication slot: the second caller received the first caller's bitmap.
     *
     * The fix aligns both keys via [com.zipoapps.imageloader.cache.CacheKey.fromRequest].
     * This test exercises [RequestDeduplicator] directly: requests with different keys must
     * each invoke the loader independently even when they arrive concurrently.
     */
    @Test
    fun `concurrent requests for same URL with different sizes each invoke the loader`() = runTest {
        val callCount = AtomicInteger(0)
        val smallBitmap: Bitmap = mockk()
        val largeBitmap: Bitmap = mockk()
        val deduplicator = RequestDeduplicator()
        val url = "https://example.com/image.jpg"

        // Simulate what RequestManager now does: key includes size component.
        val keySmall = "$url:100x100"
        val keyLarge = "$url:400x400"

        val small = async {
            deduplicator.getOrLoad(keySmall) {
                callCount.incrementAndGet()
                delay(50)
                smallBitmap
            }
        }
        val large = async {
            deduplicator.getOrLoad(keyLarge) {
                callCount.incrementAndGet()
                delay(50)
                largeBitmap
            }
        }

        val results = awaitAll(small, large)

        assertEquals("Each size must invoke its own loader", 2, callCount.get())
        assertEquals("100×100 caller must receive smallBitmap", smallBitmap, results[0])
        assertEquals("400×400 caller must receive largeBitmap", largeBitmap, results[1])
    }

    /**
     * Regression test for the CancellationException-handling bug.
     *
     * When the **primary** coroutine is cancelled its [CompletableDeferred] is cancelled,
     * which causes all waiters to throw [CancellationException] from `await()`.
     *
     * The fixed code distinguishes two cases by checking [kotlinx.coroutines.isActive]:
     * - Own coroutine cancelled → re-throw (abort the waiter).
     * - Deferred cancelled but own coroutine alive → retry the loop and become new primary.
     *
     * Without the fix the waiter would always re-throw, silently dropping the load even though
     * the calling view is still on screen.
     */
    @Test
    fun `waiter retries and succeeds when primary coroutine is cancelled`() =
        runTest(UnconfinedTestDispatcher()) {
            val callCount = AtomicInteger(0)
            val fakeBitmap: Bitmap = mockk()
            val deduplicator = RequestDeduplicator()
            val url = "https://example.com/cancellation.jpg"

            // Gate that keeps the primary suspended until we cancel it.
            val primaryGate = CompletableDeferred<Bitmap?>()

            // Primary: becomes the in-flight requester and suspends at the gate.
            // With UnconfinedTestDispatcher this coroutine runs eagerly to primaryGate.await().
            val primary = async {
                deduplicator.getOrLoad(url) {
                    callCount.incrementAndGet()
                    primaryGate.await()
                }
            }

            // Secondary: finds primary in the in-flight map and suspends on existing.await().
            // With UnconfinedTestDispatcher this runs eagerly to that suspension point.
            val secondary = async {
                deduplicator.getOrLoad(url) {
                    callCount.incrementAndGet()
                    fakeBitmap
                }
            }

            // Cancel the primary. Its deferred is cancelled → secondary wakes with
            // CancellationException → secondary sees isActive==true → retries loop →
            // becomes new primary → calls loader → returns fakeBitmap.
            primary.cancel()

            val result = secondary.await()
            assertEquals("Secondary should succeed after retrying", fakeBitmap, result)
            assertEquals("Primary and retrying secondary each invoke the loader once", 2, callCount.get())
        }
}
