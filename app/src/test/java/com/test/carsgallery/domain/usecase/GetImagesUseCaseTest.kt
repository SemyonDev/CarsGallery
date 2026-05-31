package com.test.carsgallery.domain.usecase

import com.test.carsgallery.domain.model.ImageItem
import com.test.carsgallery.domain.repository.GalleryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetImagesUseCaseTest {

    private val repository: GalleryRepository = mockk()
    private lateinit var useCase: GetImagesUseCase

    @Before
    fun setUp() {
        useCase = GetImagesUseCase(repository)
    }

    @Test
    fun `returns success with items when repository succeeds`() = runTest {
        val expected = listOf(
            ImageItem(id = "1", imageUrl = "https://example.com/1.jpg"),
            ImageItem(id = "2", imageUrl = "https://example.com/2.jpg"),
        )
        coEvery { repository.getImages() } returns Result.success(expected)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun `returns failure when repository returns failure`() = runTest {
        val error = RuntimeException("Network unavailable")
        coEvery { repository.getImages() } returns Result.failure(error)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("Network unavailable", result.exceptionOrNull()?.message)
    }

    @Test
    fun `delegates to repository exactly once`() = runTest {
        coEvery { repository.getImages() } returns Result.success(emptyList())

        useCase()

        coVerify(exactly = 1) { repository.getImages() }
    }

    @Test
    fun `returns empty list when repository returns empty`() = runTest {
        coEvery { repository.getImages() } returns Result.success(emptyList())

        val result = useCase()

        assertEquals(emptyList<ImageItem>(), result.getOrNull())
    }
}
