package com.zipoapps.imageloader.engine

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.annotation.VisibleForTesting
import com.zipoapps.imageloader.ImageLoaderConfig
import com.zipoapps.imageloader.ImageLoaderLogger
import com.zipoapps.imageloader.ImageRequest
import com.zipoapps.imageloader.Target
import com.zipoapps.imageloader.cache.CacheKey
import com.zipoapps.imageloader.cache.CacheManager
import com.zipoapps.imageloader.decode.BitmapDecoder
import com.zipoapps.imageloader.fetch.HttpFetcher

/**
 * Internal engine that wires together caching, fetching and decoding.
 *
 * ### Load pipeline
 * 1. Memory cache hit → return immediately.
 * 2. Disk cache hit → [BitmapDecoder.decodeFile] (no ByteArray heap copy) → promote to memory.
 *    Corrupt entries are evicted and the request falls through to the network.
 * 3. Network fetch → decode → write disk then memory → return.
 *
 * ### Memory pressure
 * Registers a [ComponentCallbacks2] listener; clears the memory cache on
 * [ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN] and [ComponentCallbacks2.onLowMemory].
 * Unregistered in [shutdown].
 *
 * ### Cache key
 * [CacheKey.fromRequest] encodes URL + target dimensions so that the same URL at different
 * sizes produces distinct entries, and the deduplication key matches the cache key exactly.
 *
 * The constructor is `internal` (not `private`) so integration tests can inject fakes
 * without going through [create].
 */
internal class ImageEngine(
    private val cacheManager: CacheManager,
    private val httpFetcher: HttpFetcher,
    private val bitmapDecoder: BitmapDecoder,
    private val requestManager: RequestManager,
) {

    private var appContext: Context? = null
    private var memoryCallbacks: ComponentCallbacks2? = null

    fun enqueue(
        request: ImageRequest,
        target: Target,
        onSuccess: (() -> Unit)? = null,
        onError: (() -> Unit)? = null,
    ) {
        requestManager.enqueue(request, target, ::loadBitmap, onSuccess, onError)
    }

    fun cancel(target: Target) = requestManager.cancel(target)

    fun cancel(imageView: ImageView) = requestManager.cancel(imageView)

    fun clearMemoryCache() = cacheManager.clearMemory()

    fun clearDiskCache() = cacheManager.clearDisk()

    fun invalidateAll() = cacheManager.invalidateAll()

    /**
     * Shuts down coroutines, background resources, and unregisters the memory callback.
     * Must be called before this engine is replaced.
     */
    fun shutdown() {
        requestManager.shutdown()
        cacheManager.shutdown()
        memoryCallbacks?.let { appContext?.unregisterComponentCallbacks(it) }
        memoryCallbacks = null
        appContext = null
    }

    @VisibleForTesting
    internal suspend fun loadBitmap(url: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        val key = CacheKey.fromRequest(url, targetWidth, targetHeight)

        cacheManager.getFromMemory(key)?.let { return it }

        cacheManager.getFromDiskFile(key)?.let { file ->
            val bitmap = bitmapDecoder.decodeFile(file, targetWidth, targetHeight)
            if (bitmap != null) {
                cacheManager.putToMemory(key, bitmap)
                return bitmap
            }
            // Corrupt or undecodable entry — evict and fall through to network.
            cacheManager.removeDisk(key)
        }

        val bytes = httpFetcher.fetch(url) ?: return null
        val bitmap = bitmapDecoder.decode(bytes, targetWidth, targetHeight) ?: return null

        // Write disk before memory: a process-death between the two writes leaves bytes on
        // disk (recoverable on next start) rather than only in volatile memory.
        cacheManager.putToDisk(key, bytes)
        cacheManager.putToMemory(key, bitmap)

        return bitmap
    }

    companion object {
        fun create(context: Context, config: ImageLoaderConfig = ImageLoaderConfig()): ImageEngine {
            val logger = ImageLoaderLogger(config.loggingEnabled)
            val memoryCacheSize = resolveMemoryCacheSize(context, config)
            val cacheManager = CacheManager(context.applicationContext, config, memoryCacheSize, logger)
            val httpFetcher = HttpFetcher(config.okHttpClient, config.networkTimeoutMs, logger)
            val bitmapDecoder = BitmapDecoder(config.preferredBitmapConfig, logger)
            val requestManager = RequestManager(logger)
            val engine = ImageEngine(cacheManager, httpFetcher, bitmapDecoder, requestManager)
            engine.registerMemoryCallbacks(context)
            return engine
        }

        /**
         * Uses [ActivityManager.getMemoryClass] (Android-recommended per-process budget)
         * rather than JVM max heap, which can be unreliable on some devices.
         */
        private fun resolveMemoryCacheSize(context: Context, config: ImageLoaderConfig): Long {
            if (config.memoryCacheSizeBytes > 0L) return config.memoryCacheSizeBytes
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return am.memoryClass * 1024L * 1024L / 4L
        }
    }

    private fun registerMemoryCallbacks(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        val callbacks = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() = clearMemoryCache()
            override fun onTrimMemory(level: Int) {
                // TRIM_MEMORY_UI_HIDDEN fires when all app UI goes to background.
                if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) clearMemoryCache()
            }
        }
        memoryCallbacks = callbacks
        ctx.registerComponentCallbacks(callbacks)
    }
}
