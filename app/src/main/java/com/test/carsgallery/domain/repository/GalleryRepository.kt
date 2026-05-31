package com.test.carsgallery.domain.repository

import com.test.carsgallery.domain.model.ImageItem

interface GalleryRepository {
    suspend fun getImages(): Result<List<ImageItem>>
}
