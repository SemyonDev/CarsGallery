package com.test.carsgallery.presentation.compose.mvi.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.test.carsgallery.presentation.compose.common.components.DetailContent

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
