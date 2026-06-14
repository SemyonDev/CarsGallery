package com.test.carsgallery.presentation.compose.common

import com.test.carsgallery.domain.model.ImageItem

/**
 * Sample data for `@Preview` functions across both Compose implementations.
 *
 * The URLs are intentionally blank: previews render without a network, so [NetworkImage] shows its
 * progress overlay/placeholder rather than a real bitmap — which is exactly what we want to inspect.
 */
val previewImageItems: List<ImageItem> = List(6) { index ->
    ImageItem(id = "image_%03d".format(index), imageUrl = "")
}
