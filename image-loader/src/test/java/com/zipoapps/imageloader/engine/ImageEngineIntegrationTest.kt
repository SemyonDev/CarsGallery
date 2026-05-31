package com.zipoapps.imageloader.engine

import android.content.Context
import android.graphics.Bitmap
import com.zipoapps.imageloader.ImageLoaderConfig
import com.zipoapps.imageloader.ImageLoaderLogger
import com.zipoapps.imageloader.cache.CacheManager
import com.zipoapps.imageloader.decode.BitmapDecoder
import com.zipoapps.imageloader.fetch.HttpFetcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Integration tests for [ImageEngine.loadBitmap] — the core cache + decode pipeline.
 *
 * [RequestManager] is mocked (relaxed) because it requires a real Android [Looper].
 * Everything else uses real implementations backed by a temp-folder disk cache, so the
 * full memory → disk → network → decode → cache-both path is exercised without
 * Android instrumentation.
 *
 * ### Disk-hit assertions (P3)
 * After the fix, the disk-hit path uses [BitmapDecoder.decodeFile] via [CacheManager.getFromDiskFile]
 * to avoid creating an intermediate [ByteArray]. Integration tests verify the end result (the
 * bitmap is returned and memory cache is warmed) and confirm the file is present on disk by
 * reading it through [CacheManager.getFromDisk] (kept for test-only byte verification).
 */
class ImageEngineIntegrationTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val logger = ImageLoaderLogger(enabled = false)
    private val fakeBitmap: Bitmap = mockk(relaxed = true) {
        every { allocationByteCount } returns 1024
    }
    private val fakeBytes = ByteArray(16) { it.toByte() }

    private lateinit var cacheManager: CacheManager
    private lateinit var httpFetcher: HttpFetcher
    private lateinit var bitmapDecoder: BitmapDecoder
    private lateinit var engine: ImageEngine

    @Before
    fun setUp() {
        val context = mockk<Context> {
            every { cacheDir } returns tmpFolder.newFolder("cache")
        }
        val config = ImageLoaderConfig(
            memoryCacheSizeBytes = 1L * 1024 * 1024,
            diskCacheSizeBytes = 10L * 1024 * 1024,
            cacheExpirationMs = 4L * 60 * 60 * 1000,
        )
        cacheManager = CacheManager(context, config, config.memoryCacheSizeBytes, logger)
        httpFetcher = mockk()
        bitmapDecoder = mockk()
        engine = ImageEngine(
            cacheManager = cacheManager,
            httpFetcher = httpFetcher,
            bitmapDecoder = bitmapDecoder,
            requestManager = mockk(relaxed = true),
        )
    }

    @Test
    fun `network fetch populates both memory and disk caches`() = runTest {
        coEvery { httpFetcher.fetch(URL) } returns fakeBytes
        every { bitmapDecoder.decode(fakeBytes, W, H) } returns fakeBitmap

        val result = engine.loadBitmap(URL, W, H)

        assertEquals("Should return decoded bitmap", fakeBitmap, result)
        // Memory cache must be warm after first load.
        assertEquals("Bitmap must be in memory cache", fakeBitmap, cacheManager.getFromMemory(cacheKey(URL, W, H)))
        // Disk cache must contain the raw bytes (verified via getFromDisk for test purposes).
        val diskBytes = cacheManager.getFromDisk(cacheKey(URL, W, H))
        assertEquals("Raw bytes must be on disk", fakeBytes.size, diskBytes?.size)
    }

    @Test
    fun `second call returns from memory without touching network or disk`() = runTest {
        coEvery { httpFetcher.fetch(URL) } returns fakeBytes
        every { bitmapDecoder.decode(fakeBytes, W, H) } returns fakeBitmap

        engine.loadBitmap(URL, W, H)   // first populates memory
        val second = engine.loadBitmap(URL, W, H)  // second must hit memory

        assertEquals(fakeBitmap, second)
        coVerify(exactly = 1) { httpFetcher.fetch(URL) }
    }

    @Test
    fun `disk hit promotes bitmap to memory and skips network`() = runTest {
        // Seed the disk cache directly with raw bytes.
        val key = cacheKey(URL, W, H)
        cacheManager.putToDisk(key, fakeBytes)

        // Engine uses decodeFile for disk hits — stub both decode paths to be safe.
        every { bitmapDecoder.decodeFile(any(), W, H) } returns fakeBitmap
        every { bitmapDecoder.decode(fakeBytes, W, H) } returns fakeBitmap

        val result = engine.loadBitmap(URL, W, H)

        assertEquals("Disk-hit should return decoded bitmap", fakeBitmap, result)
        coVerify(exactly = 0) { httpFetcher.fetch(any()) }
        assertEquals(fakeBitmap, cacheManager.getFromMemory(key))
    }

    @Test
    fun `disk file is present after network fetch`() = runTest {
        coEvery { httpFetcher.fetch(URL) } returns fakeBytes
        every { bitmapDecoder.decode(fakeBytes, W, H) } returns fakeBitmap

        engine.loadBitmap(URL, W, H)

        val diskFile = cacheManager.getFromDiskFile(cacheKey(URL, W, H))
        assertNotNull("Disk file must exist after network fetch", diskFile)
        assertEquals("Disk file size must match fetched bytes", fakeBytes.size.toLong(), diskFile?.length())
    }

    @Test
    fun `corrupt disk entry is evicted and network is attempted on the same call`() = runTest {
        val key = cacheKey(URL, W, H)
        cacheManager.putToDisk(key, fakeBytes)

        // Decoder fails (corrupt bytes) for both decodeFile and decode paths.
        every { bitmapDecoder.decodeFile(any(), W, H) } returns null
        every { bitmapDecoder.decode(fakeBytes, W, H) } returns null
        coEvery { httpFetcher.fetch(URL) } returns null

        val result = engine.loadBitmap(URL, W, H)

        assertNull("Network failure should propagate as null", result)
        assertNull("Corrupt disk entry must be evicted", cacheManager.getFromDiskFile(key))
        coVerify(exactly = 1) { httpFetcher.fetch(URL) }
    }

    @Test
    fun `network failure returns null`() = runTest {
        coEvery { httpFetcher.fetch(URL) } returns null

        val result = engine.loadBitmap(URL, W, H)

        assertNull("Network failure should return null", result)
        assertNull("Memory must be empty on network failure", cacheManager.getFromMemory(cacheKey(URL, W, H)))
    }

    @Test
    fun `same URL different sizes use distinct cache entries`() = runTest {
        val smallBytes = ByteArray(8) { 0 }
        val largeBytes = ByteArray(16) { 1 }
        val smallBitmap: Bitmap = mockk(relaxed = true) { every { allocationByteCount } returns 100 }
        val largeBitmap: Bitmap = mockk(relaxed = true) { every { allocationByteCount } returns 400 }

        coEvery { httpFetcher.fetch(URL) } returns smallBytes andThen largeBytes
        every { bitmapDecoder.decode(smallBytes, 100, 100) } returns smallBitmap
        every { bitmapDecoder.decode(largeBytes, 400, 400) } returns largeBitmap

        val small = engine.loadBitmap(URL, 100, 100)
        val large = engine.loadBitmap(URL, 400, 400)

        assertEquals("100×100 request must get smallBitmap", smallBitmap, small)
        assertEquals("400×400 request must get largeBitmap", largeBitmap, large)
        assertEquals(smallBitmap, cacheManager.getFromMemory(cacheKey(URL, 100, 100)))
        assertEquals(largeBitmap, cacheManager.getFromMemory(cacheKey(URL, 400, 400)))
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun cacheKey(url: String, w: Int, h: Int): String =
        com.zipoapps.imageloader.cache.CacheKey.fromRequest(url, w, h)

    private companion object {
        private const val URL = "https://example.com/image.jpg"
        private const val W = 200
        private const val H = 200
    }
}
