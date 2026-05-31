package com.test.carsgallery.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.test.carsgallery.R
import com.test.carsgallery.domain.exception.NetworkException
import com.test.carsgallery.domain.model.ImageItem
import com.test.carsgallery.domain.usecase.GetImagesUseCase
import com.test.carsgallery.presentation.screens.gallery.GalleryViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getImagesUseCase: GetImagesUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = GalleryViewModel(
        getImagesUseCase = getImagesUseCase,
        savedStateHandle = SavedStateHandle(),
    )

    @Test
    fun `initial state is Loading`() = runTest {
        coEvery { getImagesUseCase() } returns Result.success(emptyList())
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            assertTrue("First emission must be Loading", awaitItem() is UiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Success with images after loading`() = runTest {
        val images = listOf(
            ImageItem("1", "https://example.com/1.jpg"),
            ImageItem("2", "https://example.com/2.jpg"),
        )
        coEvery { getImagesUseCase() } returns Result.success(images)
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()
            val success = awaitItem() as UiState.Success<*>
            assertEquals(images, success.data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `NetworkException maps to error_network resource`() = runTest {
        coEvery { getImagesUseCase() } returns Result.failure(NetworkException(IOException("timeout")))
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()
            val error = awaitItem() as UiState.Error
            assertEquals(R.string.error_network, error.messageResId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `non-network exception maps to generic error_loading resource`() = runTest {
        coEvery { getImagesUseCase() } returns Result.failure(RuntimeException("unexpected"))
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()
            val error = awaitItem() as UiState.Error
            assertEquals(R.string.error_loading, error.messageResId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadImages resets to Loading then Success`() = runTest {
        val images = listOf(ImageItem("1", "https://example.com/1.jpg"))
        coEvery { getImagesUseCase() } returns Result.success(images)
        val viewModel = buildViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // current Success state replayed by StateFlow

            viewModel.loadImages()
            assertTrue(awaitItem() is UiState.Loading)

            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(awaitItem() is UiState.Success<*>)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleLayoutMode persists state across ViewModel re-creation`() = runTest {
        coEvery { getImagesUseCase() } returns Result.success(emptyList())
        val handle = SavedStateHandle()
        val viewModel = GalleryViewModel(getImagesUseCase, handle)

        assertTrue(viewModel.isGridMode.value)

        viewModel.toggleLayoutMode()
        assertTrue(!viewModel.isGridMode.value)

        // Simulate process death / re-creation using the same SavedStateHandle
        val recreated = GalleryViewModel(getImagesUseCase, handle)
        assertTrue("Grid mode must survive re-creation", !recreated.isGridMode.value)
    }
}
