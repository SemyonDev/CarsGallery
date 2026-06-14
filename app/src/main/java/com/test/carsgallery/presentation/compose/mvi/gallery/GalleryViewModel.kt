package com.test.carsgallery.presentation.compose.mvi.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.carsgallery.R
import com.test.carsgallery.domain.exception.NetworkException
import com.test.carsgallery.domain.usecase.GetImagesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI store for the gallery.
 *
 * Unlike the MVVM ViewModel (which exposes intent-named methods), the UI here interacts through a
 * single [onIntent] entry point. Intents are reduced into an immutable [state]; navigation is
 * surfaced as a one-shot [effects] stream so it fires exactly once.
 */
class GalleryViewModel(
    private val getImagesUseCase: GetImagesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GalleryState())
    val state: StateFlow<GalleryState> = _state.asStateFlow()

    private val _effects = Channel<GalleryEffect>(Channel.BUFFERED)
    val effects: Flow<GalleryEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        onIntent(GalleryIntent.Load)
    }

    /** Single funnel for all UI events. */
    fun onIntent(intent: GalleryIntent) {
        when (intent) {
            GalleryIntent.Load, GalleryIntent.Retry -> loadImages()
            GalleryIntent.ToggleGrid -> _state.update { it.copy(isGrid = !it.isGrid) }
            is GalleryIntent.ImageClicked -> viewModelScope.launch {
                _effects.send(GalleryEffect.NavigateToDetail(intent.item.id, intent.item.imageUrl))
            }
        }
    }

    private fun loadImages() {
        loadJob?.cancel()
        _state.update { it.copy(content = GalleryState.Content.Loading) }
        loadJob = viewModelScope.launch {
            getImagesUseCase()
                .onSuccess { images ->
                    _state.update { it.copy(content = GalleryState.Content.Success(images)) }
                }
                .onFailure { error ->
                    _state.update { it.copy(content = GalleryState.Content.Error(mapError(error))) }
                }
        }
    }

    private fun mapError(error: Throwable): Int = when (error) {
        is NetworkException -> R.string.error_network
        else -> R.string.error_loading
    }
}
