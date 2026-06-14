package com.test.carsgallery.presentation.compose.common.components

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
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
 *
 * In `@Preview`/inspection mode the [AndroidView] + [ImageLoader] path is skipped (the loader engine
 * can't run in the preview renderer) and a static placeholder is drawn instead, so screen previews
 * render cleanly.
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
    if (LocalInspectionMode.current) {
        PreviewPlaceholder(modifier = modifier)
        return
    }

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

/** Static stand-in for the loaded image, shown only in `@Preview`/inspection mode. */
@Composable
private fun PreviewPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(0.4f),
        )
    }
}
