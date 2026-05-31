package com.test.carsgallery.presentation.compose.components

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.test.carsgallery.R
import com.zipoapps.imageloader.ImageLoader

@Composable
fun ImageLoaderImage(
    url: String,
    modifier: Modifier = Modifier,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
    onSuccess: () -> Unit = {},
    onError: () -> Unit = {},
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply { this.scaleType = scaleType }
        },
        update = { imageView ->
            ImageLoader.with(context)
                .load(url)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .onSuccess(onSuccess)
                .onError(onError)
                .into(imageView)
        },
        modifier = modifier,
    )
}
