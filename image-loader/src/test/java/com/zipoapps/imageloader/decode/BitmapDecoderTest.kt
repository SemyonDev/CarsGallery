package com.zipoapps.imageloader.decode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests [calculateSampleSize] — the pure downsampling logic extracted from [BitmapDecoder].
 *
 * No Android instrumentation is required because the function has no Android dependencies.
 */
class BitmapDecoderTest {

    @Test
    fun `sampleSize is 1 when image fits within target`() {
        assertEquals(1, calculateSampleSize(rawWidth = 100, rawHeight = 100, reqWidth = 200, reqHeight = 200))
    }

    @Test
    fun `sampleSize is 1 when image matches target exactly`() {
        assertEquals(1, calculateSampleSize(rawWidth = 100, rawHeight = 100, reqWidth = 100, reqHeight = 100))
    }

    @Test
    fun `sampleSize is 2 when image is twice the target in both dimensions`() {
        assertEquals(2, calculateSampleSize(rawWidth = 200, rawHeight = 200, reqWidth = 100, reqHeight = 100))
    }

    @Test
    fun `sampleSize is 4 when image is four times the target`() {
        assertEquals(4, calculateSampleSize(rawWidth = 400, rawHeight = 400, reqWidth = 100, reqHeight = 100))
    }

    @Test
    fun `sampleSize is always a power of two`() {
        // 300×300 into 100×100: half=150, 150/1>=100 → size=2; 150/2=75<100 → stop. Result: 2.
        val result = calculateSampleSize(rawWidth = 300, rawHeight = 300, reqWidth = 100, reqHeight = 100)
        assertTrue("Expected power-of-two but was $result", result > 0 && result and (result - 1) == 0)
    }

    @Test
    fun `sampleSize uses the more conservative dimension to avoid undersampling`() {
        // Wide image 400×100 into 100×100: height (100) is not > reqHeight (100),
        // so the while-loop condition on height fails immediately. Result: 1.
        // This ensures we never produce a bitmap smaller than the target in any dimension.
        assertEquals(1, calculateSampleSize(rawWidth = 400, rawHeight = 100, reqWidth = 100, reqHeight = 100))
    }

    @Test
    fun `sampleSize is 1 for zero raw dimensions`() {
        assertEquals(1, calculateSampleSize(rawWidth = 0, rawHeight = 0, reqWidth = 100, reqHeight = 100))
    }

    @Test
    fun `sampleSize is at least 1 regardless of inputs`() {
        // Sanity: result must never be zero or negative.
        val cases = listOf(
            intArrayOf(1, 1, 1000, 1000),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(1000, 1000, 1, 1),
        )
        cases.forEach { (rw, rh, qw, qh) ->
            assertTrue(calculateSampleSize(rw, rh, qw, qh) >= 1)
        }
    }
}
