package com.zipoapps.imageloader

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView

/**
 * Default [Target] implementation that loads bitmaps into an [ImageView].
 *
 * Created automatically by [RequestBuilder.into]. Instantiate directly only when
 * you need a [Target] reference for [ImageLoader.cancel]:
 * ```kotlin
 * val target = ImageViewTarget(imageView)
 * ImageLoader.cancel(target)
 * ```
 */
class ImageViewTarget(val imageView: ImageView) : Target {

    override val view: View get() = imageView

    override fun onPrepare(placeholderRes: Int?) {
        placeholderRes?.let { imageView.setImageResource(it) }
    }

    override fun onSuccess(bitmap: Bitmap) {
        imageView.setImageBitmap(bitmap)
    }

    override fun onError(errorRes: Int?, placeholderRes: Int?) {
        val res = errorRes ?: placeholderRes
        res?.let { imageView.setImageResource(it) }
    }
}
