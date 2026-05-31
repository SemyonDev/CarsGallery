package com.test.carsgallery.data.remote.api

import com.test.carsgallery.data.remote.dto.ImageDto
import retrofit2.http.GET

interface GalleryApi {

    @GET("image_list.json")
    suspend fun fetchImages(): List<ImageDto>
}
