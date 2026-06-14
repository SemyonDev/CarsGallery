package com.test.carsgallery.presentation.compose.mvvm.detail

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * MVVM ViewModel for the detail screen.
 *
 * Receives the image identity from the navigation key (constructor args, supplied by the nav
 * entry factory) and exposes a [uiState] the screen renders. The image-load callbacks come from
 * the loader bridge in the View layer and are funnelled back into state here.
 */
class DetailViewModel(
    imageId: String,
    imageUrl: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState(imageId = imageId, imageUrl = imageUrl))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun onImageLoading() = _uiState.update { it.copy(imageLoad = DetailUiState.ImageLoad.Loading) }
    fun onImageSuccess() = _uiState.update { it.copy(imageLoad = DetailUiState.ImageLoad.Success) }
    fun onImageError() = _uiState.update { it.copy(imageLoad = DetailUiState.ImageLoad.Error) }
}
