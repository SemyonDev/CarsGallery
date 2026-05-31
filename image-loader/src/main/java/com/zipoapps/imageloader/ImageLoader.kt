package com.zipoapps.imageloader

import android.content.Context
import android.widget.ImageView
import com.zipoapps.imageloader.engine.ImageEngine

/**
 * Public entry point for the image loading library.
 *
 * ```kotlin
 * ImageLoader.with(context)
 *     .load(url)
 *     .placeholder(R.drawable.placeholder)
 *     .error(R.drawable.error)
 *     .into(imageView)
 * ```
 *
 * Call [initialize] once in `Application.onCreate` to supply a custom config. If skipped,
 * the engine is created lazily on the first [with] call using [ImageLoaderConfig] defaults.
 * All methods are `@JvmStatic` for Java interoperability.
 */
object ImageLoader {

    @Volatile
    private var engine: ImageEngine? = null
    private val lock = Any()

    /**
     * Initialises (or re-initialises) the engine with [config].
     * Any existing engine is shut down first, terminating orphaned coroutines.
     */
    @JvmStatic
    fun initialize(context: Context, config: ImageLoaderConfig) {
        synchronized(lock) {
            engine?.shutdown()
            engine = ImageEngine.create(context.applicationContext, config)
        }
    }

    @JvmStatic
    fun with(context: Context): RequestBuilder =
        RequestBuilder(getOrCreateEngine(context))

    /** Cancels any in-flight request bound to [imageView]. */
    @JvmStatic
    fun cancel(imageView: ImageView) {
        engine?.cancel(imageView)
    }

    /** Cancels any in-flight request bound to [target]. */
    @JvmStatic
    fun cancel(target: Target) {
        engine?.cancel(target)
    }

    @JvmStatic
    fun clearMemoryCache() {
        engine?.clearMemoryCache()
    }

    @JvmStatic
    fun clearDiskCache() {
        engine?.clearDiskCache()
    }

    @JvmStatic
    fun invalidateAll() {
        engine?.invalidateAll()
    }

    private fun getOrCreateEngine(context: Context): ImageEngine =
        engine ?: synchronized(lock) {
            engine ?: ImageEngine.create(context.applicationContext).also { engine = it }
        }
}
