package com.test.carsgallery.domain.model

/**
 * Domain entity representing a single gallery image.
 *
 * This class is purposely free of framework annotations and JSON field names.
 * Mapping from the network DTO happens at the repository boundary.
 */
data class ImageItem(
    val id: String,
    val imageUrl: String,
)
