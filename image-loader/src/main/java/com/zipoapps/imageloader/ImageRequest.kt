package com.zipoapps.imageloader

import androidx.annotation.DrawableRes

/**
 * Immutable snapshot of a single image-load operation, captured at call-time by [RequestBuilder].
 * Keeping this separate from the builder lets the engine pass it to background coroutines safely.
 */
internal data class ImageRequest(
    val url: String,
    @param:DrawableRes val placeholderRes: Int?,
    @param:DrawableRes val errorRes: Int?,
    /** Non-positive values mean "decode at native size". */
    val targetWidth: Int,
    val targetHeight: Int,
)
