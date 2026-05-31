package com.zipoapps.imageloader.cache

import com.zipoapps.imageloader.ImageLoaderLogger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Tests [DiskCache] using real filesystem I/O via [TemporaryFolder].
 *
 * The cache stores raw bytes (not recompressed bitmaps), so round-trip fidelity can be
 * verified directly without mocking [android.graphics.BitmapFactory].
 */
class DiskCacheTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var cacheDir: File
    private val maxSize = 10L * 1024 * 1024  // 10 MB
    private val fourHoursMs = 4L * 60 * 60 * 1000
    private val logger = ImageLoaderLogger(enabled = false)

    @Before
    fun setUp() {
        cacheDir = tmpFolder.newFolder("image_cache")
    }

    @Test
    fun `get returns null for missing entry`() {
        val cache = DiskCache(cacheDir, maxSize, fourHoursMs, logger)
        assertNull(cache.get("missing_key"))
    }

    @Test
    fun `containsValid returns false before any write`() {
        val cache = DiskCache(cacheDir, maxSize, fourHoursMs, logger)
        assertFalse(cache.containsValid("any_key"))
    }

    @Test
    fun `put writes image file and meta file`() {
        val cache = DiskCache(cacheDir, maxSize, fourHoursMs, logger)
        cache.put("abc123", byteArrayOf(1, 2, 3))
        assertTrue("Image file should exist", File(cacheDir, "abc123.cache").exists())
        assertTrue("Meta file should exist", File(cacheDir, "abc123.meta").exists())
    }

    @Test
    fun `get returns the exact bytes that were put`() {
        val cache = DiskCache(cacheDir, maxSize, fourHoursMs, logger)
        val data = ByteArray(32) { it.toByte() }
        cache.put("round_trip", data)
        assertArrayEquals("Round-trip bytes must be identical", data, cache.get("round_trip"))
    }

    @Test
    fun `meta file contains parseable timestamp`() {
        val cache = DiskCache(cacheDir, maxSize, fourHoursMs, logger)
        val before = System.currentTimeMillis()

        cache.put("ts_key", byteArrayOf(0))

        val metaContent = File(cacheDir, "ts_key.meta").readText().trim()
        // Meta format: "<written_epoch_ms>|<accessed_epoch_ms>"
        val writtenAt = metaContent.substringBefore('|').toLong()
        val after = System.currentTimeMillis()

        assertTrue(writtenAt >= before)
        assertTrue(writtenAt <= after)
    }

    @Test
    fun `containsValid returns false for expired entry`() {
        val zeroTtl = 0L
        val cache = DiskCache(cacheDir, maxSize, zeroTtl, logger)
        cache.put("expired_key", byteArrayOf(1))
        Thread.sleep(1)
        assertFalse("Expired entry should not be valid", cache.containsValid("expired_key"))
    }

    @Test
    fun `get returns null for expired entry and deletes both files`() {
        val zeroTtl = 0L
        val cache = DiskCache(cacheDir, maxSize, zeroTtl, logger)
        cache.put("expired", byteArrayOf(1))
        Thread.sleep(1)

        assertNull("Expired entry must return null", cache.get("expired"))
        // Expired entries are deleted eagerly so they don't accumulate on disk.
        assertFalse("Expired .cache file must be deleted", File(cacheDir, "expired.cache").exists())
        assertFalse("Expired .meta file must be deleted", File(cacheDir, "expired.meta").exists())
    }

    @Test
    fun `containsValid does not delete expired files only get does`() {
        val zeroTtl = 0L
        val cache = DiskCache(cacheDir, maxSize, zeroTtl, logger)
        cache.put("stale", byteArrayOf(1))
        Thread.sleep(1)

        assertFalse("containsValid should report false", cache.containsValid("stale"))
        // containsValid is a query it must not delete files as a side effect.
        assertTrue("Files must still exist after containsValid", File(cacheDir, "stale.cache").exists())
    }

    @Test
    fun `containsValid returns true for fresh entry`() {
        val cache = DiskCache(cacheDir, maxSize, fourHoursMs, logger)
        cache.put("fresh_key", byteArrayOf(1))
        assertTrue("Fresh entry should be valid", cache.containsValid("fresh_key"))
    }

    @Test
    fun `remove deletes both files`() {
        val cache = DiskCache(cacheDir, maxSize, fourHoursMs, logger)
        cache.put("del_key", byteArrayOf(1))

        cache.remove("del_key")

        assertFalse(File(cacheDir, "del_key.cache").exists())
        assertFalse(File(cacheDir, "del_key.meta").exists())
    }

    @Test
    fun `clear removes all cache files`() {
        val cache = DiskCache(cacheDir, maxSize, fourHoursMs, logger)
        cache.put("k1", byteArrayOf(1))
        cache.put("k2", byteArrayOf(2))

        cache.clear()

        assertTrue(
            "Cache dir should be empty after clear",
            cacheDir.listFiles().isNullOrEmpty(),
        )
    }

    @Test
    fun `orphaned meta files are removed on construction`() {
        // Write a .meta file with no corresponding .cache file (simulates a mid-write crash).
        File(cacheDir, "orphan.meta").writeText(System.currentTimeMillis().toString())
        assertFalse("Orphan has no .cache file", File(cacheDir, "orphan.cache").exists())

        // DiskCache runs orphan cleanup on a background daemon thread.
        // awaitInit() blocks until the latch is counted down so the assertion below is safe.
        val diskCache = DiskCache(cacheDir, maxSize, fourHoursMs, logger)
        diskCache.awaitInit()

        assertFalse("Orphaned .meta file must be removed on init", File(cacheDir, "orphan.meta").exists())
    }
}
