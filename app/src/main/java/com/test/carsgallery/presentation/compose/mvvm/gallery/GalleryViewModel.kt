package com.test.carsgallery.presentation.compose.mvvm.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.carsgallery.R
import com.test.carsgallery.domain.exception.NetworkException
import com.test.carsgallery.domain.usecase.GetImagesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVVM ViewModel for the Compose gallery.
 *
 * Classic MVVM: the View calls intent-named methods ([loadImages], [toggleGrid]) directly and
 * observes a single [uiState] stream. State transitions live here; the View only renders.
 *
 * Created by a Navigation 3 entry via a manual factory (see `MvvmNavigation`) so it can receive
 * the Hilt-provided [GetImagesUseCase] while staying scoped to its nav entry's ViewModelStore.
 */
class GalleryViewModel(
    private val getImagesUseCase: GetImagesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadImages()
    }

    fun loadImages() {
        loadJob?.cancel()
        _uiState.update { it.copy(content = GalleryUiState.Content.Loading) }
        loadJob = viewModelScope.launch {
            getImagesUseCase()
                .onSuccess { images ->
                    _uiState.update { it.copy(content = GalleryUiState.Content.Success(images)) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(content = GalleryUiState.Content.Error(mapError(error))) }
                }
        }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(isGrid = !it.isGrid) }
    }

    private fun mapError(error: Throwable): Int = when (error) {
        is NetworkException -> R.string.error_network
        else -> R.string.error_loading
    }
}
