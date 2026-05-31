package com.test.carsgallery.presentation.screens.gallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.carsgallery.R
import com.test.carsgallery.domain.exception.NetworkException
import com.test.carsgallery.domain.model.ImageItem
import com.test.carsgallery.domain.usecase.GetImagesUseCase
import com.test.carsgallery.presentation.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val getImagesUseCase: GetImagesUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ImageItem>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ImageItem>>> = _uiState.asStateFlow()

    val isGridMode: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_GRID_MODE, true)

    private var loadJob: Job? = null

    init {
        loadImages()
    }

    fun loadImages() {
        loadJob?.cancel()
        _uiState.value = UiState.Loading
        loadJob = viewModelScope.launch {
            getImagesUseCase()
                .onSuccess { images -> _uiState.value = UiState.Success(images) }
                .onFailure { error -> _uiState.value = UiState.Error(mapError(error)) }
        }
    }

    fun toggleLayoutMode() {
        savedStateHandle[KEY_GRID_MODE] = !isGridMode.value
    }

    private fun mapError(error: Throwable): Int = when (error) {
        is NetworkException -> R.string.error_network
        else -> R.string.error_loading
    }

    private companion object {
        private const val KEY_GRID_MODE = "is_grid_mode"
    }
}