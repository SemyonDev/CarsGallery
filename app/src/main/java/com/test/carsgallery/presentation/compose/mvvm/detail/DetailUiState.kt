package com.test.carsgallery.presentation.compose.mvvm.detail

/**
 * UI-state for the MVVM detail screen.
 *
 * [imageId]/[imageUrl] arrive from the navigation key, while [imageLoad] tracks the lifecycle of
 * the full-size image load so the screen can show a spinner or an error indicator.
 */
data class DetailUiState(
    val imageId: String,
    val imageUrl: String,
    val imageLoad: ImageLoad = ImageLoad.Loading,
) {
    sealed interface ImageLoad {
        data object Loading : ImageLoad
        data object Success : ImageLoad
        data object Error : ImageLoad
    }
}
