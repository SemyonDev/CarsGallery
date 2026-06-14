package com.test.carsgallery.presentation.compose.common.components

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.test.carsgallery.R
import com.zipoapps.imageloader.ImageLoader

/**
 * Bridges the project's custom [ImageLoader] (which loads into an Android [ImageView]) into Compose.
 *
 * Rather than re-implement bitmap fetching/caching/decoding for Compose, we reuse the existing,
 * fully-tested loader by hosting a real [ImageView] via [AndroidView]. This keeps a single image
 * pipeline across the View-system, compose_mvvm, and compose_mvi implementations.
 *
 * Load state is surfaced through [onLoading]/[onSuccess]/[onError] so callers can drive their own
 * UI-state (e.g. a progress overlay). The load is (re)issued only when [url] actually changes —
 * the last-loaded URL is stashed on the view's tag to avoid reloading on every recomposition.
 */
@Composable
fun NetworkImage(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
    @DrawableRes placeholder: Int = R.drawable.ic_placeholder,
    @DrawableRes error: Int = R.drawable.ic_error,
    onLoading: () -> Unit = {},
    onSuccess: () -> Unit = {},
    onError: () -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                this.scaleType = scaleType
                this.contentDescription = contentDescription
            }
        },
        update = { imageView ->
            imageView.scaleType = scaleType
            if (imageView.tag != url) {
                imageView.tag = url
                onLoading()
                ImageLoader.with(imageView.context)
                    .load(url)
                    .placeholder(placeholder)
                    .error(error)
                    .onSuccess(onSuccess)
                    .onError(onError)
                    .into(imageView)
            }
        },
        onReset = { imageView ->
            // AndroidView may recycle the node; cancel and clear so a reused view reloads cleanly.
            ImageLoader.cancel(imageView)
            imageView.tag = null
            imageView.setImageDrawable(null)
        },
        onRelease = { imageView ->
            ImageLoader.cancel(imageView)
            imageView.tag = null
        },
    )
}
