package com.test.carsgallery.presentation.compose.components

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.test.carsgallery.R
import com.test.carsgallery.presentation.compose.theme.CarsGalleryTheme
import com.zipoapps.imageloader.ImageLoader

@Composable
fun ImageLoaderImage(
    url: String,
    modifier: Modifier = Modifier,
    scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
    onSuccess: () -> Unit = {},
    onError: () -> Unit = {},
) {
    if (LocalInspectionMode.current) {
        androidx.compose.foundation.layout.Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        )
        return
    }

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

// region Previews

@Preview(showBackground = true, name = "ImageLoaderImage")
@Composable
private fun ImageLoaderImagePreview() {
    CarsGalleryTheme {
        ImageLoaderImage(
            url = "https://example.com/car1.jpg",
            modifier = Modifier.size(200.dp),
        )
    }
}

// endregion
