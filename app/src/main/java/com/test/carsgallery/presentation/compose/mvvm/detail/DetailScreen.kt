package com.test.carsgallery.presentation.compose.mvvm.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.test.carsgallery.presentation.compose.common.components.DetailContent
import com.test.carsgallery.presentation.compose.common.theme.CarsGalleryTheme

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

@Preview(name = "Detail · Loaded", showBackground = true)
@Composable
private fun DetailScreenPreview() {
    CarsGalleryTheme {
        DetailScreen(
            uiState = DetailUiState(
                imageId = "image_042",
                imageUrl = "",
                imageLoad = DetailUiState.ImageLoad.Success,
            ),
            onBack = {},
            onImageLoading = {},
            onImageSuccess = {},
            onImageError = {},
        )
    }
}

@Preview(name = "Detail · Loading", showBackground = true)
@Composable
private fun DetailScreenLoadingPreview() {
    CarsGalleryTheme {
        DetailScreen(
            uiState = DetailUiState(
                imageId = "image_042",
                imageUrl = "",
                imageLoad = DetailUiState.ImageLoad.Loading,
            ),
            onBack = {},
            onImageLoading = {},
            onImageSuccess = {},
            onImageError = {},
        )
    }
}
