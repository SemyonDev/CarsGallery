package com.test.carsgallery.presentation.screens.imagedetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Holds display data and image load state for the detail screen.
 *
 * [imageId] and [imageUrl] are read from [SavedStateHandle], which the Navigation component
 * populates automatically from the `<argument>` declarations in nav_graph.xml.
 *
 * [onLoadStarted] is called from [ImageDetailFragment.onViewCreated] before each new load
 * request, resetting [loadState] to [ImageLoadState.Loading] after screen rotation.
 * [ARG_IMAGE_ID] and [ARG_IMAGE_URL] must match the argument names in nav_graph.xml.
 */
@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val imageId: String = checkNotNull(savedStateHandle[ARG_IMAGE_ID]) {
        "Required navigation argument '$ARG_IMAGE_ID' is missing. Check nav_graph.xml."
    }

    val imageUrl: String = checkNotNull(savedStateHandle[ARG_IMAGE_URL]) {
        "Required navigation argument '$ARG_IMAGE_URL' is missing. Check nav_graph.xml."
    }

    private val _loadState = MutableStateFlow<ImageLoadState>(ImageLoadState.Loading)

    /** Observed by the fragment to drive the progress indicator visibility. */
    val loadState: StateFlow<ImageLoadState> = _loadState.asStateFlow()

    fun onLoadStarted() { _loadState.value = ImageLoadState.Loading }
    fun onImageLoaded() { _loadState.value = ImageLoadState.Success }
    fun onImageError()  { _loadState.value = ImageLoadState.Error }

    companion object {
        const val ARG_IMAGE_ID  = "imageId"   // must match nav_graph.xml <argument android:name>
        const val ARG_IMAGE_URL = "imageUrl"  // must match nav_graph.xml <argument android:name>
    }
}
