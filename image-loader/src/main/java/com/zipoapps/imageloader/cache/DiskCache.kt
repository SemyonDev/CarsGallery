package com.zipoapps.imageloader.cache

import com.zipoapps.imageloader.ImageLoaderLogger
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * File-based LRU cache with timestamp-driven expiration.
 *
 * ### Storage format
 * ```
 * <cacheDir>/
 *   <key>.cache   — raw network bytes
 *   <key>.meta    — "<written_epoch_ms>|<accessed_epoch_ms>"
 * ```
 *
 * ### Atomic writes
 * [put] writes to `.cache.tmp` / `.meta.tmp` first, then renames both atomically via
 * `rename(2)`. A mid-write crash leaves only `.tmp` orphans, cleaned up on next construction.
 *
 * ### LRU eviction
 * Access timestamps are stored in `.meta` and updated on every [get]/[getFile] hit.
 * [trimIfNeeded] sorts by this value, which is reliable across all Android filesystems
 * (unlike [File.setLastModified], which can silently fail).
 *
 * ### Expiration
 * Measured from **write time** so frequently-accessed entries still expire and re-fetch,
 * preventing permanently stale content.
 *
 * ### Async construction
 * Orphan cleanup runs on a background daemon thread to avoid blocking `Application.onCreate`.
 * A [CountDownLatch] ensures the first I/O call waits for cleanup to complete.
 *
 * ### Thread safety
 * [ReentrantReadWriteLock] allows concurrent reads while serialising writes.
 */
internal class DiskCache(
    private val cacheDir: File,
    private val maxSizeBytes: Long,
    private val expirationMs: Long,
    private val logger: ImageLoaderLogger,
) {
    private val lock = ReentrantReadWriteLock()
    private val initLatch = CountDownLatch(1)

    init {
        cacheDir.mkdirs()
        Thread({
            lock.write { cleanOrphanedFiles() }
            initLatch.countDown()
        }, "image-loader-disk-init").apply { isDaemon = true }.start()
    }

    fun get(key: String): ByteArray? {
        awaitInit()
        return when (val result = readEntry(key)) {
            ReadResult.Miss -> null
            ReadResult.Expired -> { remove(key); null }
            is ReadResult.Hit -> {
                logger.d(TAG, "Cache hit (disk): $key")
                updateAccessTime(key, result.writtenAt)
                try {
                    result.imageFile.readBytes()
                } catch (e: Exception) {
                    logger.w(TAG, "Failed to read cached file for key $key", e)
                    null
                }
            }
        }
    }

    /**
     * Returns the cache [File] for [key] if valid, or null.
     *
     * The caller decodes directly from the file path via [android.graphics.BitmapFactory.decodeFile],
     * avoiding the [ByteArray] heap spike of [get]. If the file is evicted between this call
     * and the caller's read, [BitmapFactory.decodeFile] returns null and the engine falls
     * through to the network.
     */
    fun getFile(key: String): File? {
        awaitInit()
        return when (val result = readEntry(key)) {
            ReadResult.Miss -> null
            ReadResult.Expired -> { remove(key); null }
            is ReadResult.Hit -> {
                logger.d(TAG, "Cache file hit (disk): $key")
                updateAccessTime(key, result.writtenAt)
                result.imageFile
            }
        }
    }

    /** Writes [bytes] atomically using temp-file rename. Triggers LRU eviction if needed. */
    fun put(key: String, bytes: ByteArray) {
        awaitInit()
        lock.write {
            val imageFile = imageFile(key)
            val metaFile = metaFile(key)
            val tmpImageFile = File(cacheDir, "$key.cache.tmp")
            val tmpMetaFile = File(cacheDir, "$key.meta.tmp")

            try {
                tmpImageFile.writeBytes(bytes)
                val now = System.currentTimeMillis()
                tmpMetaFile.writeText(metaContent(now, now))

                // rename(2) is atomic on Linux/Android.
                if (!tmpImageFile.renameTo(imageFile)) throw IOException("Image rename failed")
                if (!tmpMetaFile.renameTo(metaFile)) throw IOException("Meta rename failed")

                logger.d(TAG, "Wrote to disk cache: $key (${bytes.size} bytes)")
                trimIfNeeded()
            } catch (e: Exception) {
                logger.w(TAG, "Failed to write cache entry for key $key", e)
                tmpImageFile.delete()
                tmpMetaFile.delete()
                // imageFile may have been renamed before meta failed — delete for consistency.
                imageFile.delete()
                metaFile.delete()
            }
        }
    }

    fun remove(key: String): Unit = lock.write {
        imageFile(key).delete()
        metaFile(key).delete()
    }

    fun clear(): Unit = lock.write {
        cacheDir.listFiles()?.forEach { file ->
            if (!file.delete()) logger.w(TAG, "Failed to delete cache file: ${file.name}")
        }
        logger.d(TAG, "Disk cache cleared")
    }

    fun shutdown() {
        initLatch.await(5, TimeUnit.SECONDS)
    }

    fun containsValid(key: String): Boolean {
        awaitInit()
        return lock.read {
            val meta = metaFile(key)
            if (!imageFile(key).exists() || !meta.exists()) return@read false
            val parsed = parseMeta(meta) ?: return@read false
            !isExpired(parsed.writtenAt)
        }
    }

    /** Blocks until background orphan cleanup has finished. `internal` for test use. */
    internal fun awaitInit() {
        if (initLatch.count == 0L) return   // fast path — already done
        initLatch.await(5, TimeUnit.SECONDS)
    }

    private fun readEntry(key: String): ReadResult = lock.read {
        val imageFile = imageFile(key)
        val metaFile = metaFile(key)
        if (!imageFile.exists() || !metaFile.exists()) return@read ReadResult.Miss
        val meta = parseMeta(metaFile) ?: return@read ReadResult.Miss
        if (isExpired(meta.writtenAt)) {
            logger.d(TAG, "Cache miss (expired): $key")
            return@read ReadResult.Expired
        }
        ReadResult.Hit(imageFile, meta.writtenAt)
    }

    private fun updateAccessTime(key: String, writtenAt: Long) {
        lock.write {
            val mf = metaFile(key)
            if (mf.exists()) mf.writeText(metaContent(writtenAt, System.currentTimeMillis()))
        }
    }

    private fun isExpired(writtenAtMs: Long): Boolean =
        System.currentTimeMillis() - writtenAtMs > expirationMs

    private fun imageFile(key: String) = File(cacheDir, "$key.cache")
    private fun metaFile(key: String) = File(cacheDir, "$key.meta")
    private fun metaContent(writtenAt: Long, accessedAt: Long) = "$writtenAt|$accessedAt"

    private data class MetaInfo(val writtenAt: Long, val accessedAt: Long)

    private fun parseMeta(file: File): MetaInfo? {
        val text = runCatching { file.readText().trim() }.getOrNull() ?: return null
        return if ('|' in text) {
            val idx = text.indexOf('|')
            val w = text.substring(0, idx).toLongOrNull() ?: return null
            val a = text.substring(idx + 1).toLongOrNull() ?: return null
            MetaInfo(w, a)
        } else {
            // Legacy single-timestamp format: treat written == accessed.
            val ts = text.toLongOrNull() ?: return null
            MetaInfo(ts, ts)
        }
    }

    private fun cleanOrphanedFiles() {
        cacheDir.listFiles { f -> f.extension == "tmp" }?.forEach { it.delete() }

        val cacheFiles = cacheDir.listFiles { f -> f.extension == "cache" }
            ?.associateBy { it.nameWithoutExtension } ?: return
        val metaFiles = cacheDir.listFiles { f -> f.extension == "meta" }
            ?.associateBy { it.nameWithoutExtension } ?: emptyMap()

        metaFiles.forEach { (key, metaFile) ->
            if (key !in cacheFiles) {
                metaFile.delete()
                logger.d(TAG, "Removed orphaned meta: ${metaFile.name}")
            }
        }
        cacheFiles.forEach { (key, cacheFile) ->
            if (key !in metaFiles) {
                cacheFile.delete()
                logger.d(TAG, "Removed orphaned cache: ${cacheFile.name}")
            }
        }
    }

    private fun trimIfNeeded() {
        val imageFiles = cacheDir.listFiles { f -> f.extension == "cache" } ?: return
        var totalSize = imageFiles.sumOf { it.length() }
        if (totalSize <= maxSizeBytes) return

        imageFiles
            .sortedBy { file ->
                parseMeta(File(file.parent, "${file.nameWithoutExtension}.meta"))?.accessedAt
                    ?: file.lastModified()
            }
            .forEach { file ->
                if (totalSize <= maxSizeBytes) return
                val meta = File(file.parent, "${file.nameWithoutExtension}.meta")
                totalSize -= file.length()
                if (!file.delete()) logger.w(TAG, "Failed to evict: ${file.name}")
                meta.delete()
                logger.d(TAG, "Evicted from disk cache: ${file.name}")
            }
    }

    private sealed class ReadResult {
        object Miss : ReadResult()
        object Expired : ReadResult()
        class Hit(val imageFile: File, val writtenAt: Long) : ReadResult()
    }

    private companion object {
        private const val TAG = "DiskCache"
    }
}
