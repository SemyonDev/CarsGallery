package com.test.carsgallery.data.remote

import com.test.carsgallery.data.datasource.RemoteDataSource
import com.test.carsgallery.data.remote.api.GalleryApi
import com.test.carsgallery.domain.model.ImageItem
import javax.inject.Inject

class RemoteDataSourceImpl @Inject constructor(private val api: GalleryApi) : RemoteDataSource {
    override suspend fun fetchImages(): List<ImageItem> =
        api.fetchImages().map { dto -> ImageItem(id = dto.id, imageUrl = dto.imageUrl) }
}
