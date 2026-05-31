package com.zipoapps.imageloader.cache

import android.graphics.Bitmap

/**
 * Thread-safe, size-bounded LRU cache backed by an access-ordered [LinkedHashMap].
 *
 * Eviction is size-based (bytes), not entry-count-based, which is correct for bitmaps whose
 * byte footprints vary wildly.
 *
 * ### Why @Synchronized on get()
 * [LinkedHashMap] with `accessOrder = true` performs a structural modification on every [get]
 * (moves the accessed entry to the tail). Structural modifications are not thread-safe, so
 * [get] must be fully synchronised — a read-write lock would not help here because [get] is
 * effectively a write to the internal map structure.
 *
 * ### Why evicted bitmaps are not recycled
 * Calling [Bitmap.recycle] on an evicted bitmap would crash any view still drawing it.
 * Hardware-backed bitmaps (API 26+) also reject [Bitmap.recycle]. Memory is reclaimed by the
 * GC once all references are dropped, which is the correct modern approach.
 *
 * Size tracking uses [Long] to prevent overflow when many large bitmaps rotate through the
 * cache — [Int] would silently wrap at ~2 GB and disable eviction.
 *
 * @param maxSizeBytes maximum number of bytes the cache may hold before evicting.
 */
internal class MemoryCache(private val maxSizeBytes: Long) {

    private val store = LinkedHashMap<String, Bitmap>(16, 0.75f, true)
    private var currentSizeBytes = 0L

    @Synchronized
    fun get(key: String): Bitmap? = store[key]

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        removeInternal(key)
        store[key] = bitmap
        currentSizeBytes += bitmap.allocationByteCount.toLong()
        trimToSize()
    }

    @Synchronized
    fun remove(key: String) = removeInternal(key)

    private fun removeInternal(key: String) {
        store.remove(key)?.let { evicted ->
            currentSizeBytes -= evicted.allocationByteCount.toLong()
        }
    }

    @Synchronized
    fun clear() {
        store.clear()
        currentSizeBytes = 0L
    }

    @Synchronized
    fun currentSizeBytes(): Long = currentSizeBytes

    @Synchronized
    fun maxSizeBytes(): Long = maxSizeBytes

    // Called only from put(), which already holds the monitor — direct field access avoids
    // a redundant re-entrant lock acquisition on maxSizeBytes().
    private fun trimToSize() {
        val iterator = store.entries.iterator()
        while (currentSizeBytes > maxSizeBytes && iterator.hasNext()) {
            val evicted = iterator.next()
            currentSizeBytes -= evicted.value.allocationByteCount.toLong()
            iterator.remove()
        }
    }
}
