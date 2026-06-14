package com.test.carsgallery.presentation.compose.mvi.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI store for the detail screen. All UI events flow through [onIntent]; back navigation is
 * emitted once via [effects].
 */
class DetailViewModel(
    imageId: String,
    imageUrl: String,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailState(imageId = imageId, imageUrl = imageUrl))
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private val _effects = Channel<DetailEffect>(Channel.BUFFERED)
    val effects: Flow<DetailEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: DetailIntent) {
        when (intent) {
            DetailIntent.ImageLoading ->
                _state.update { it.copy(imageLoad = DetailState.ImageLoad.Loading) }
            DetailIntent.ImageSucceeded ->
                _state.update { it.copy(imageLoad = DetailState.ImageLoad.Success) }
            DetailIntent.ImageFailed ->
                _state.update { it.copy(imageLoad = DetailState.ImageLoad.Error) }
            DetailIntent.BackClicked -> viewModelScope.launch {
                _effects.send(DetailEffect.NavigateBack)
            }
        }
    }
}
