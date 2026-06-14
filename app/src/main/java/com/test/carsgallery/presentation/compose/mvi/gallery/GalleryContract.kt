package com.test.carsgallery.presentation.compose.mvi.gallery

import androidx.annotation.StringRes
import com.test.carsgallery.domain.model.ImageItem

/**
 * MVI contract for the gallery screen: the single [GalleryState], the [GalleryIntent]s the UI can
 * send, and the one-shot [GalleryEffect]s the ViewModel can emit. Keeping all three in one file
 * makes the screen's full interaction surface readable at a glance.
 */
data class GalleryState(
    val content: Content = Content.Loading,
    val isGrid: Boolean = true,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val images: List<ImageItem>) : Content
        data class Error(@param:StringRes val messageResId: Int) : Content
    }
}

/** Everything the user can do on the gallery screen. */
sealed interface GalleryIntent {
    data object Load : GalleryIntent
    data object Retry : GalleryIntent
    data object ToggleGrid : GalleryIntent
    data class ImageClicked(val item: ImageItem) : GalleryIntent
}

/** One-shot events that should happen once and not be replayed on recomposition. */
sealed interface GalleryEffect {
    data class NavigateToDetail(val imageId: String, val imageUrl: String) : GalleryEffect
}
