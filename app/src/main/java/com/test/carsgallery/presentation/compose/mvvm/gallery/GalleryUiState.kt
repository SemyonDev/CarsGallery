package com.test.carsgallery.presentation.compose.mvvm.gallery

import androidx.annotation.StringRes
import com.test.carsgallery.domain.model.ImageItem

/**
 * Immutable UI-state for the MVVM gallery screen.
 *
 * The load lifecycle ([Content]) and the layout preference ([isGrid]) are modelled as separate
 * axes so toggling grid/list never disturbs the loaded data, and a sealed [Content] keeps the
 * three mutually-exclusive load states explicit.
 */
data class GalleryUiState(
    val content: Content = Content.Loading,
    val isGrid: Boolean = true,
) {
    sealed interface Content {
        data object Loading : Content
        data class Success(val images: List<ImageItem>) : Content
        data class Error(@param:StringRes val messageResId: Int) : Content
    }
}
