package com.test.carsgallery.presentation.compose.mvi.detail

/**
 * MVI contract for the detail screen.
 *
 * [DetailState] is seeded with the navigation-key arguments and tracks the image-load lifecycle.
 * The UI sends [DetailIntent]s; [DetailEffect] carries the one-shot back navigation.
 */
data class DetailState(
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

sealed interface DetailIntent {
    data object ImageLoading : DetailIntent
    data object ImageSucceeded : DetailIntent
    data object ImageFailed : DetailIntent
    data object BackClicked : DetailIntent
}

sealed interface DetailEffect {
    data object NavigateBack : DetailEffect
}
