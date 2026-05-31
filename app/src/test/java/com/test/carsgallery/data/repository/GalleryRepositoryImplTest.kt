package com.test.carsgallery.data.repository

import com.test.carsgallery.data.datasource.RemoteDataSource
import com.test.carsgallery.domain.exception.NetworkException
import com.test.carsgallery.domain.model.ImageItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class GalleryRepositoryImplTest {

    private val remoteDataSource: RemoteDataSource = mockk()
    private lateinit var repository: GalleryRepositoryImpl

    @Before
    fun setUp() {
        repository = GalleryRepositoryImpl(remoteDataSource)
    }

    @Test
    fun `maps domain items from data source`() = runTest {
        val items = listOf(
            ImageItem(id = "img_1", imageUrl = "https://cdn.example.com/1.jpg"),
            ImageItem(id = "img_2", imageUrl = "https://cdn.example.com/2.jpg"),
        )
        coEvery { remoteDataSource.fetchImages() } returns items

        val result = repository.getImages()

        assertTrue(result.isSuccess)
        assertEquals(items, result.getOrThrow())
    }

    @Test
    fun `wraps IOException in NetworkException`() = runTest {
        coEvery { remoteDataSource.fetchImages() } throws IOException("Connection reset")

        val result = repository.getImages()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun `wraps HttpException in NetworkException`() = runTest {
        val httpException = HttpException(
            Response.error<Any>(404, "Not Found".toResponseBody())
        )
        coEvery { remoteDataSource.fetchImages() } throws httpException

        val result = repository.getImages()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }

    @Test
    fun `passes through non-network exceptions as-is`() = runTest {
        coEvery { remoteDataSource.fetchImages() } throws RuntimeException("Unexpected")

        val result = repository.getImages()

        assertTrue(result.isFailure)
        assertEquals("Unexpected", result.exceptionOrNull()?.message)
    }

    @Test
    fun `returns empty list when source returns empty`() = runTest {
        coEvery { remoteDataSource.fetchImages() } returns emptyList()
        assertTrue(repository.getImages().getOrThrow().isEmpty())
    }

    @Test
    fun `preserves field values from data source`() = runTest {
        val item = ImageItem(id = "special_id_!@#", imageUrl = "https://example.com/path?q=1&r=2")
        coEvery { remoteDataSource.fetchImages() } returns listOf(item)

        val fetched = repository.getImages().getOrThrow().single()

        assertEquals(item.id, fetched.id)
        assertEquals(item.imageUrl, fetched.imageUrl)
    }

    @Test
    fun `CancellationException propagates and is not wrapped in Result`() = runTest {
        coEvery { remoteDataSource.fetchImages() } throws CancellationException("cancelled")

        try {
            repository.getImages()
            fail("Expected CancellationException to propagate")
        } catch (e: CancellationException) {
            // expected
        }
    }
}
