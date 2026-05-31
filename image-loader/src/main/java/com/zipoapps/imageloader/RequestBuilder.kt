package com.zipoapps.imageloader

import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.zipoapps.imageloader.engine.ImageEngine

/**
 * Fluent builder that captures a single image load request.
 *
 * Obtain an instance via [ImageLoader.with]. Call the configuration methods in any order,
 * then finalise with [into] to start loading.
 */
class RequestBuilder internal constructor(
    private val engine: ImageEngine,
) {
    private var url: String? = null
    private var placeholderRes: Int? = null
    private var errorRes: Int? = null
    private var onSuccess: (() -> Unit)? = null
    private var onError: (() -> Unit)? = null

    fun load(url: String?): RequestBuilder {
        this.url = url
        return this
    }

    fun placeholder(@DrawableRes res: Int): RequestBuilder {
        this.placeholderRes = res
        return this
    }

    fun error(@DrawableRes res: Int): RequestBuilder {
        this.errorRes = res
        return this
    }

    /**
     * Called on the main thread after the bitmap is successfully decoded and set on the view.
     * Not called when the result is served from the placeholder path (null/blank URL).
     */
    fun onSuccess(callback: () -> Unit): RequestBuilder {
        this.onSuccess = callback
        return this
    }

    /**
     * Called on the main thread when the load fails for any reason (network error, decode error,
     * server error). The error drawable (if set) will already be displayed.
     */
    fun onError(callback: () -> Unit): RequestBuilder {
        this.onError = callback
        return this
    }

    /**
     * Starts loading the image into [imageView] using the default [ImageViewTarget].
     *
     * - If the URL is null or blank the placeholder (if set) is displayed synchronously.
     * - If the view has not been laid out yet (width/height == 0), the load is deferred until
     *   the view is measured so that the decoder can compute the correct [inSampleSize].
     * - Any previously active request for this exact view instance is cancelled.
     */
    fun into(imageView: ImageView) {
        into(ImageViewTarget(imageView))
    }

    /**
     * Starts loading the image into a custom [Target].
     *
     * Use this overload to load into non-ImageView targets (e.g. notification icons,
     * widget RemoteViews, or custom views). The engine calls [Target.onPrepare],
     * [Target.onSuccess], and [Target.onError] on the main thread.
     */
    fun into(target: Target) {
        val resolvedUrl = url
        if (resolvedUrl.isNullOrBlank()) {
            target.onPrepare(placeholderRes)
            return
        }

        val request = ImageRequest(
            url = resolvedUrl,
            placeholderRes = placeholderRes,
            errorRes = errorRes,
            targetWidth = target.view.width.takeIf { it > 0 } ?: 0,
            targetHeight = target.view.height.takeIf { it > 0 } ?: 0,
        )

        engine.enqueue(request, target, onSuccess, onError)
    }
}
