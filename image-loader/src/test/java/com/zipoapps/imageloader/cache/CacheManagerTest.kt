package com.zipoapps.imageloader.cache

import android.content.Context
import android.graphics.Bitmap
import com.zipoapps.imageloader.ImageLoaderConfig
import com.zipoapps.imageloader.ImageLoaderLogger
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tests the [CacheManager] coordination layer specifically that the two cache tiers
 * operate independently and that [invalidateAll] clears both.
 *
 * [Context] is mocked so the disk cache writes to a temp directory rather than a real
 * Android file system.
 */
class CacheManagerTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var cacheManager: CacheManager

    @Before
    fun setUp() {
        val context = mockk<Context> {
            every { cacheDir } returns tmpFolder.newFolder("cache")
        }
        val config = ImageLoaderConfig(
            diskCacheSizeBytes = 10L * 1024 * 1024,
            cacheExpirationMs = 4L * 60 * 60 * 1000,
        )
        cacheManager = CacheManager(
            context = context,
            config = config,
            memoryCacheSizeBytes = 1L * 1024 * 1024,
            logger = ImageLoaderLogger(enabled = false),
        )
    }

    @Test
    fun `putToMemory and getFromMemory round-trips bitmap`() {
        val key = "mem_key"
        cacheManager.putToMemory(key, fakeBitmap(100))
        assertNotNull(cacheManager.getFromMemory(key))
    }

    @Test
    fun `putToDisk and getFromDisk round-trips raw bytes`() {
        val key = "disk_key"
        cacheManager.putToDisk(key, ByteArray(16) { it.toByte() })
        assertNotNull(cacheManager.getFromDisk(key))
    }

    @Test
    fun `disk entry is not automatically visible in memory`() {
        val key = "key"
        cacheManager.putToDisk(key, ByteArray(10))
        assertNull("Disk put must not populate memory", cacheManager.getFromMemory(key))
    }

    @Test
    fun `clearMemory removes memory entries but leaves disk intact`() {
        val key = "key"
        cacheManager.putToMemory(key, fakeBitmap(100))
        cacheManager.putToDisk(key, ByteArray(10))

        cacheManager.clearMemory()

        assertNull("Memory should be cleared", cacheManager.getFromMemory(key))
        assertNotNull("Disk should be untouched", cacheManager.getFromDisk(key))
    }

    @Test
    fun `clearDisk removes disk entries but leaves memory intact`() {
        val key = "key"
        cacheManager.putToMemory(key, fakeBitmap(100))
        cacheManager.putToDisk(key, ByteArray(10))

        cacheManager.clearDisk()

        assertNotNull("Memory should be untouched", cacheManager.getFromMemory(key))
        assertNull("Disk should be cleared", cacheManager.getFromDisk(key))
    }

    @Test
    fun `invalidateAll removes entries from both caches`() {
        val key = "key"
        cacheManager.putToMemory(key, fakeBitmap(100))
        cacheManager.putToDisk(key, ByteArray(10))

        cacheManager.invalidateAll()

        assertNull("Memory must be empty after invalidateAll", cacheManager.getFromMemory(key))
        assertNull("Disk must be empty after invalidateAll", cacheManager.getFromDisk(key))
    }

    // Helpers
    private fun fakeBitmap(byteCount: Int): Bitmap = mockk {
        every { this@mockk.allocationByteCount } returns byteCount
    }
}
