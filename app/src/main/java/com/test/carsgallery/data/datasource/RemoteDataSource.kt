package com.test.carsgallery.data.datasource

import com.test.carsgallery.domain.model.ImageItem

interface RemoteDataSource {
    suspend fun fetchImages(): List<ImageItem>
}
