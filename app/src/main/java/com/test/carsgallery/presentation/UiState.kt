package com.test.carsgallery.presentation

import androidx.annotation.StringRes

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(@param:StringRes val messageResId: Int) : UiState<Nothing>()
}
