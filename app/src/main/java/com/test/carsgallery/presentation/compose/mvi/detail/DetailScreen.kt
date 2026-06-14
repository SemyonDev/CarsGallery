package com.test.carsgallery.presentation.compose.mvi.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.test.carsgallery.presentation.compose.common.components.DetailContent
import com.test.carsgallery.presentation.compose.common.theme.CarsGalleryTheme

/** MVI detail screen — maps [DetailState] onto the shared [DetailContent], emitting [DetailIntent]s. */
@Composable
fun DetailScreen(
    state: DetailState,
    onIntent: (DetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailContent(
        imageId = state.imageId,
        imageUrl = state.imageUrl,
        isLoading = state.imageLoad is DetailState.ImageLoad.Loading,
        onBack = { onIntent(DetailIntent.BackClicked) },
        onImageLoading = { onIntent(DetailIntent.ImageLoading) },
        onImageSuccess = { onIntent(DetailIntent.ImageSucceeded) },
        onImageError = { onIntent(DetailIntent.ImageFailed) },
        modifier = modifier,
    )
}

@Preview(name = "Detail · Loaded", showBackground = true)
@Composable
private fun DetailScreenPreview() {
    CarsGalleryTheme {
        DetailScreen(
            state = DetailState(
                imageId = "image_042",
                imageUrl = "",
                imageLoad = DetailState.ImageLoad.Success,
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Detail · Loading", showBackground = true)
@Composable
private fun DetailScreenLoadingPreview() {
    CarsGalleryTheme {
        DetailScreen(
            state = DetailState(
                imageId = "image_042",
                imageUrl = "",
                imageLoad = DetailState.ImageLoad.Loading,
            ),
            onIntent = {},
        )
    }
}
