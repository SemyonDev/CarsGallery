package com.zipoapps.imageloader.decode

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.zipoapps.imageloader.ImageLoaderLogger
import java.io.File

/**
 * Decodes raw bytes (or a [File]) into a [Bitmap] while avoiding OOM on large images.
 *
 * When target dimensions are provided the decoder uses the two-pass `inJustDecodeBounds` →
 * `inSampleSize` technique to load a downsampled version just large enough to fill the target.
 *
 * [decodeFile] uses [BitmapFactory.decodeFile] which reads the file path directly, avoiding
 * the [ByteArray] heap spike that would occur if the entire file were loaded first.
 *
 * Use [Bitmap.Config.RGB_565] to halve memory for images without transparency;
 * use [Bitmap.Config.HARDWARE] (API 26+) for images only ever drawn to a Canvas.
 */
internal class BitmapDecoder(
    private val preferredConfig: Bitmap.Config = Bitmap.Config.ARGB_8888,
    private val logger: ImageLoaderLogger,
) {

    /** Decodes a bitmap from raw [bytes], e.g. freshly downloaded from the network. */
    fun decode(bytes: ByteArray, targetWidth: Int = 0, targetHeight: Int = 0): Bitmap? {
        if (bytes.isEmpty()) return null
        return try {
            if (targetWidth > 0 && targetHeight > 0) {
                decodeByteArrayWithDownsampling(bytes, targetWidth, targetHeight)
            } else {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, baseOptions())
            }
        } catch (e: Exception) {
            logger.w(TAG, "Failed to decode bitmap (${bytes.size} bytes)", e)
            null
        }
    }

    /**
     * Decodes a bitmap from [file] without loading its contents into a [ByteArray].
     * Returns null if the file does not exist, is corrupt, or cannot be decoded.
     */
    fun decodeFile(file: File, targetWidth: Int = 0, targetHeight: Int = 0): Bitmap? {
        if (!file.exists()) return null
        return try {
            val path = file.absolutePath
            if (targetWidth > 0 && targetHeight > 0) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                val decode = baseOptions().apply {
                    inSampleSize = calculateSampleSize(
                        bounds.outWidth, bounds.outHeight, targetWidth, targetHeight
                    )
                }
                BitmapFactory.decodeFile(path, decode)
            } else {
                BitmapFactory.decodeFile(path, baseOptions())
            }
        } catch (e: Exception) {
            logger.w(TAG, "Failed to decode bitmap from file ${file.name}", e)
            null
        }
    }

    private fun decodeByteArrayWithDownsampling(
        bytes: ByteArray,
        targetWidth: Int,
        targetHeight: Int,
    ): Bitmap? {
        // Pass 1: read only the dimensions — no pixel memory allocated.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        // Pass 2: decode at the computed sample size.
        val decode = baseOptions().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, targetWidth, targetHeight)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decode)
    }

    private fun baseOptions() = BitmapFactory.Options().apply {
        inPreferredConfig = preferredConfig
    }

    private companion object {
        private const val TAG = "BitmapDecoder"
    }
}

/**
 * Returns the largest power-of-two sample size that still produces a bitmap at least as
 * large as [reqWidth] × [reqHeight]. Result is always ≥ 1.
 *
 * Package-level so it can be unit-tested without Android instrumentation.
 * Guards against [Int] overflow by stopping once [sampleSize] would exceed [Int.MAX_VALUE] / 2.
 */
internal fun calculateSampleSize(rawWidth: Int, rawHeight: Int, reqWidth: Int, reqHeight: Int): Int {
    var sampleSize = 1

    if (rawHeight > reqHeight || rawWidth > reqWidth) {
        val halfHeight = rawHeight / 2
        val halfWidth = rawWidth / 2
        while (halfHeight / sampleSize >= reqHeight && halfWidth / sampleSize >= reqWidth) {
            if (sampleSize > Int.MAX_VALUE / 2) break
            sampleSize *= 2
        }
    }
    return sampleSize
}
