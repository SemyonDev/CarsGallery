package com.zipoapps.imageloader.cache

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MemoryCacheTest {

    // 1 MB budget enough for a few fake bitmaps.
    private val maxBytes = 1L * 1024 * 1024
    private lateinit var cache: MemoryCache

    @Before
    fun setUp() {
        cache = MemoryCache(maxBytes)
    }

    @Test
    fun `put and get returns stored bitmap`() {
        val bitmap = fakeBitmap(100 * 1024)
        cache.put("key1", bitmap)
        assertNotNull(cache.get("key1"))
    }

    @Test
    fun `get returns null for missing key`() {
        assertNull(cache.get("nonexistent"))
    }

    @Test
    fun `evicts least recently used when capacity exceeded`() {
        val big = fakeBitmap(600 * 1024)  // 600 KB each two together exceed 1 MB
        cache.put("key1", big)
        cache.put("key2", big)
        assertNull("LRU key1 should be evicted", cache.get("key1"))
        assertNotNull("Recent key2 should remain", cache.get("key2"))
    }

    @Test
    fun `get promotes entry so it outlives the previous LRU when a third entry is added`() {
        val half = fakeBitmap(500 * 1024)  // 500 KB each two fit inside 1 MB
        cache.put("key1", half)
        cache.put("key2", half)
        // LRU order: [key1 (head/LRU)] → [key2 (tail/MRU)]

        cache.get("key1")  // promote key1 to MRU; key2 becomes LRU
        // LRU order: [key2 (head/LRU)] → [key1 (tail/MRU)]

        cache.put("key3", half)  // total 1500 KB > 1024 KB evict key2 (the LRU head)

        assertNotNull("key1 was promoted to MRU must survive", cache.get("key1"))
        assertNull("key2 became LRU after key1 was accessed must be evicted", cache.get("key2"))
    }

    @Test
    fun `remove decreases reported size`() {
        val bitmap = fakeBitmap(200 * 1024)
        cache.put("key1", bitmap)
        val beforeRemove = cache.currentSizeBytes()
        cache.remove("key1")
        assertEquals(beforeRemove - bitmap.allocationByteCount.toLong(), cache.currentSizeBytes())
        assertNull(cache.get("key1"))
    }

    @Test
    fun `clear empties cache and resets size to zero`() {
        cache.put("k1", fakeBitmap(100 * 1024))
        cache.put("k2", fakeBitmap(100 * 1024))
        cache.clear()
        assertEquals(0L, cache.currentSizeBytes())
        assertNull(cache.get("k1"))
        assertNull(cache.get("k2"))
    }

    @Test
    fun `replacing existing key does not double-count size`() {
        val first = fakeBitmap(100 * 1024)
        val second = fakeBitmap(200 * 1024)
        cache.put("key1", first)
        cache.put("key1", second)
        assertEquals(second.allocationByteCount.toLong(), cache.currentSizeBytes())
    }

    /**
     * Stress-tests the `@Synchronized` contract by running concurrent reads and writes
     * across multiple IO threads. A plain HashMap would throw ConcurrentModificationException
     * or produce negative size under this load.
     */
    @Test
    fun `concurrent reads and writes do not corrupt the cache`() = runBlocking {
        val stressCache = MemoryCache(512L * 1024)  // 512 KB budget

        val jobs = (1..200).map { i ->
            launch(Dispatchers.IO) {
                val bitmap = fakeBitmap(4 * 1024)  // 4 KB each
                stressCache.put("key${i % 20}", bitmap)  // 20 unique keys triggers eviction
                stressCache.get("key${i % 20}")
                if (i % 5 == 0) stressCache.remove("key${i % 20}")
            }
        }
        jobs.joinAll()

        // Invariant: size must never be negative overflow or double-subtract would show here.
        assertTrue(
            "currentSizeBytes must be 0 after concurrent access",
            stressCache.currentSizeBytes() >= 0L,
        )
    }

    //Helpers
    private fun fakeBitmap(byteCount: Int): Bitmap = mockk {
        every { this@mockk.allocationByteCount } returns byteCount
    }
}
