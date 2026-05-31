package com.zipoapps.imageloader.cache

import java.security.MessageDigest

/**
 * Converts an arbitrary URL string (plus optional target size) into a filesystem-safe,
 * fixed-length cache key.
 *
 * Size is incorporated into the key so that the same URL loaded at different target dimensions
 * (e.g. thumbnail vs full-screen) produces distinct cache entries and never serves a
 * wrong-resolution bitmap from cache.
 *
 * MD5 is used for speed and uniform output length; this is not a security-sensitive operation.
 * MD5 is guaranteed to be present on Android (it is part of the platform's MessageDigest
 * implementation), so no fallback is needed.
 *
 * A [ThreadLocal] pools one [MessageDigest] per thread to avoid the security-provider
 * lookup cost (`MessageDigest.getInstance`) on every cache-key computation.
 */
internal object CacheKey {

    private val md5 = ThreadLocal.withInitial<MessageDigest> {
        MessageDigest.getInstance("MD5")
    }

    /** Key that ignores render size — useful when size is irrelevant (e.g. pre-fetching). */
    fun fromUrl(url: String): String = hash(url)

    /**
     * Key that incorporates the target render dimensions.
     * When both dimensions are ≤ 0 the result is identical to [fromUrl].
     */
    fun fromRequest(url: String, targetWidth: Int, targetHeight: Int): String {
        val sizeTag = if (targetWidth > 0 && targetHeight > 0) ":${targetWidth}x${targetHeight}" else ""
        return hash("$url$sizeTag")
    }

    private fun hash(input: String): String {
        val digest = md5.get()!!
        digest.reset()
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
