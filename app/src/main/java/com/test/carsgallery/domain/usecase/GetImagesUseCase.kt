package com.test.carsgallery.domain.usecase

import com.test.carsgallery.domain.model.ImageItem
import com.test.carsgallery.domain.repository.GalleryRepository
import javax.inject.Inject

class GetImagesUseCase @Inject constructor(private val repository: GalleryRepository) {

    suspend operator fun invoke(): Result<List<ImageItem>> = repository.getImages()
}
