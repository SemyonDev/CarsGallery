package com.zipoapps.imageloader.cache

import android.content.Context
import android.graphics.Bitmap
import com.zipoapps.imageloader.ImageLoaderConfig
import com.zipoapps.imageloader.ImageLoaderLogger
import java.io.File

/**
 * Single point of truth for all caching operations.
 *
 * Exposes the two cache tiers separately so the engine can implement the lookup hierarchy
 * (memory → disk → network) and promotion logic in one place. The disk tier stores raw network
 * bytes; decoding is the engine's responsibility after a disk hit.
 *
 * [invalidateAll] clears disk before memory so a concurrent promotion cannot re-insert a stale
 * disk entry into the already-empty memory cache.
 */
internal class CacheManager(
    context: Context,
    config: ImageLoaderConfig,
    memoryCacheSizeBytes: Long,
    logger: ImageLoaderLogger,
) {

    private val memoryCache = MemoryCache(memoryCacheSizeBytes)
    private val diskCache = DiskCache(
        cacheDir = File(context.cacheDir, CACHE_DIR_NAME),
        maxSizeBytes = config.diskCacheSizeBytes,
        expirationMs = config.cacheExpirationMs,
        logger = logger,
    )

    fun getFromMemory(key: String): Bitmap? = memoryCache.get(key)

    /**
     * Returns the raw cached bytes for [key], or null on miss or expiry.
     *
     * Prefer [getFromDiskFile] in the image-loading hot path to avoid allocating a
     * full-size [ByteArray] for each disk hit.
     */
    fun getFromDisk(key: String): ByteArray? = diskCache.get(key)

    /**
     * Returns the cache [File] for [key] if valid, or null on miss or expiry.
     *
     * Use this in the load pipeline so that [android.graphics.BitmapFactory.decodeFile]
     * can read the file directly without an intermediate [ByteArray] heap allocation.
     */
    fun getFromDiskFile(key: String): File? = diskCache.getFile(key)

    fun putToMemory(key: String, bitmap: Bitmap) = memoryCache.put(key, bitmap)

    /** Stores raw network bytes. The engine is responsible for decoding before display. */
    fun putToDisk(key: String, bytes: ByteArray) = diskCache.put(key, bytes)

    /** Removes a specific key from the disk cache (e.g. after a failed decode). */
    fun removeDisk(key: String) = diskCache.remove(key)

    fun clearMemory() = memoryCache.clear()

    fun clearDisk() = diskCache.clear()

    fun invalidateAll() {
        // Disk first: a concurrent disk-hit promotion cannot insert a stale entry into the
        // already-cleared memory cache while disk still has data.
        diskCache.clear()
        memoryCache.clear()
    }

    fun shutdown() = diskCache.shutdown()

    private companion object {
        private const val CACHE_DIR_NAME = "image_loader_cache"
    }
}
