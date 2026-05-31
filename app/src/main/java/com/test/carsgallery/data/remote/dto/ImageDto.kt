package com.test.carsgallery.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImageDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "imageUrl") val imageUrl: String,
)
