package com.test.carsgallery.presentation.screens.imagedetails

/** Lifecycle of a single image load request in [ImageDetailViewModel]. */
sealed class ImageLoadState {
    /** The request has been dispatched; the decoder has not yet returned. */
    data object Loading : ImageLoadState()
    /** The bitmap was decoded and set on the ImageView successfully. */
    data object Success : ImageLoadState()
    /** The load failed — network, HTTP, or decode error. */
    data object Error : ImageLoadState()
}
