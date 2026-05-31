package com.zipoapps.imageloader

import android.graphics.Bitmap
import okhttp3.OkHttpClient

/**
 * Immutable configuration for the image loading engine.
 *
 * Pass a configured instance to [ImageLoader.initialize] once in your Application subclass.
 * All fields have sensible defaults so partial overrides are straightforward.
 *
 * ### Memory cache size
 * Leave [memoryCacheSizeBytes] at [AUTO_SIZE] (the default) to let the engine compute 1/4 of
 * the per-process memory class reported by ActivityManager — the Android-recommended approach.
 * Set it to a positive value to override.
 *
 * ### Network timeout and custom OkHttpClient
 * [networkTimeoutMs] applies only when the library creates its own OkHttpClient (i.e. when
 * [okHttpClient] is null). When you supply a custom client its built-in timeouts are used
 * as-is; set them on the client's builder directly.
 */
data class ImageLoaderConfig(
    /**
     * Maximum bytes the in-process LRU cache may occupy.
     * [AUTO_SIZE] (0, the default) = auto-compute from ActivityManager at engine creation.
     * Must be ≥ 0.
     */
    val memoryCacheSizeBytes: Long = AUTO_SIZE,
    /** Maximum bytes the disk cache directory may grow to before LRU eviction kicks in. Must be > 0. */
    val diskCacheSizeBytes: Long = DEFAULT_DISK_CACHE_SIZE_BYTES,
    /** How long a disk-cached entry is considered fresh. After this the network is hit again. Must be > 0. */
    val cacheExpirationMs: Long = DEFAULT_CACHE_EXPIRATION_MS,
    /**
     * Connect / read / write timeout for network requests, in milliseconds.
     * Ignored when [okHttpClient] is non-null — set timeouts on that client's builder instead.
     * Must be > 0.
     */
    val networkTimeoutMs: Long = DEFAULT_NETWORK_TIMEOUT_MS,
    /**
     * Optional pre-configured OkHttpClient. Provide one to share connection pools, add
     * authentication interceptors, or tune timeouts. When null the library creates its own client
     * using [networkTimeoutMs].
     */
    val okHttpClient: OkHttpClient? = null,
    /**
     * Preferred Bitmap pixel format. Defaults to ARGB_8888 (full colour + alpha).
     * Use RGB_565 to halve memory usage for images without transparency.
     * Use HARDWARE (API 26+) for images that are only ever drawn to a Canvas.
     */
    val preferredBitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888,
    /** When true, verbose cache-hit/miss and request lifecycle events are sent to Logcat. */
    val loggingEnabled: Boolean = false,
) {
    init {
        require(memoryCacheSizeBytes >= 0) {
            "memoryCacheSizeBytes must be ≥ 0 (use AUTO_SIZE = 0 for auto-compute)"
        }
        require(diskCacheSizeBytes > 0) { "diskCacheSizeBytes must be > 0" }
        require(cacheExpirationMs > 0) { "cacheExpirationMs must be > 0" }
        require(networkTimeoutMs > 0) { "networkTimeoutMs must be > 0" }
    }

    companion object {
        /** Sentinel: let the engine compute the memory cache size from ActivityManager. */
        const val AUTO_SIZE: Long = 0L
        const val DEFAULT_DISK_CACHE_SIZE_BYTES: Long = 100L * 1024 * 1024  // 100 MB
        const val DEFAULT_CACHE_EXPIRATION_MS: Long = 4L * 60 * 60 * 1000   // 4 hours
        const val DEFAULT_NETWORK_TIMEOUT_MS: Long = 30_000L                 // 30 seconds
    }
}
