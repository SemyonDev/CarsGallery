package com.zipoapps.imageloader

import android.util.Log

/**
 * Logging gate shared by all engine components.
 * [d] is suppressed unless [enabled]; [w] is always emitted.
 */
internal class ImageLoaderLogger(private val enabled: Boolean) {

    fun d(tag: String, message: String) {
        if (enabled) Log.d(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.w(tag, message, throwable)
        else Log.w(tag, message)
    }
}
