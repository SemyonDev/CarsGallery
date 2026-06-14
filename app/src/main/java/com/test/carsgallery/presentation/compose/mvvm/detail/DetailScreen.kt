package com.test.carsgallery.presentation.compose.mvvm.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.test.carsgallery.presentation.compose.common.components.DetailContent

/** MVVM detail screen — maps [DetailUiState] onto the shared, stateless [DetailContent]. */
@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onBack: () -> Unit,
    onImageLoading: () -> Unit,
    onImageSuccess: () -> Unit,
    onImageError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailContent(
        imageId = uiState.imageId,
        imageUrl = uiState.imageUrl,
        isLoading = uiState.imageLoad is DetailUiState.ImageLoad.Loading,
        onBack = onBack,
        onImageLoading = onImageLoading,
        onImageSuccess = onImageSuccess,
        onImageError = onImageError,
        modifier = modifier,
    )
}
