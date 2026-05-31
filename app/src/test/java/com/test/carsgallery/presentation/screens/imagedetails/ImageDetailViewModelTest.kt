package com.test.carsgallery.presentation.screens.imagedetails

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [ImageDetailViewModel].
 *
 * The ViewModel has no coroutines or Android framework dependencies beyond [SavedStateHandle],
 * so these tests run on the JVM without any special test dispatcher or Robolectric setup.
 * State assertions use [kotlinx.coroutines.flow.StateFlow.value] directly — no Turbine needed
 * because all state transitions are synchronous.
 */
class ImageDetailViewModelTest {

    // ── Argument access ──────────────────────────────────────────────────────

    @Test
    fun `imageId is read from SavedStateHandle`() {
        val vm = buildViewModel(imageId = "car_001")
        assertEquals("car_001", vm.imageId)
    }

    @Test
    fun `imageUrl is read from SavedStateHandle`() {
        val vm = buildViewModel(imageUrl = "https://cdn.example.com/car.jpg")
        assertEquals("https://cdn.example.com/car.jpg", vm.imageUrl)
    }

    @Test(expected = IllegalStateException::class)
    fun `missing imageId argument throws IllegalStateException`() {
        ImageDetailViewModel(
            SavedStateHandle(mapOf(ImageDetailViewModel.ARG_IMAGE_URL to "https://cdn.example.com/car.jpg"))
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `missing imageUrl argument throws IllegalStateException`() {
        ImageDetailViewModel(
            SavedStateHandle(mapOf(ImageDetailViewModel.ARG_IMAGE_ID to "car_001"))
        )
    }

    // ── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial loadState is Loading`() {
        val vm = buildViewModel()
        assertEquals(ImageLoadState.Loading, vm.loadState.value)
    }

    // ── State transitions ────────────────────────────────────────────────────

    @Test
    fun `onImageLoaded transitions state to Success`() {
        val vm = buildViewModel()

        vm.onImageLoaded()

        assertEquals(ImageLoadState.Success, vm.loadState.value)
    }

    @Test
    fun `onImageError transitions state to Error`() {
        val vm = buildViewModel()

        vm.onImageError()

        assertEquals(ImageLoadState.Error, vm.loadState.value)
    }

    @Test
    fun `onLoadStarted resets state to Loading from Success`() {
        val vm = buildViewModel()
        vm.onImageLoaded()
        assertEquals(ImageLoadState.Success, vm.loadState.value)

        vm.onLoadStarted()

        assertEquals(ImageLoadState.Loading, vm.loadState.value)
    }

    @Test
    fun `onLoadStarted resets state to Loading from Error`() {
        val vm = buildViewModel()
        vm.onImageError()
        assertEquals(ImageLoadState.Error, vm.loadState.value)

        vm.onLoadStarted()

        assertEquals(ImageLoadState.Loading, vm.loadState.value)
    }

    @Test
    fun `full lifecycle Loading to Success to Loading to Error`() {
        val vm = buildViewModel()

        assertEquals(ImageLoadState.Loading, vm.loadState.value)
        vm.onImageLoaded()
        assertEquals(ImageLoadState.Success, vm.loadState.value)
        // Simulate screen rotation: view recreated, new load started.
        vm.onLoadStarted()
        assertEquals(ImageLoadState.Loading, vm.loadState.value)
        vm.onImageError()
        assertEquals(ImageLoadState.Error, vm.loadState.value)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun buildViewModel(
        imageId: String = "default_id",
        imageUrl: String = "https://example.com/default.jpg",
    ) = ImageDetailViewModel(
        SavedStateHandle(
            mapOf(
                ImageDetailViewModel.ARG_IMAGE_ID to imageId,
                ImageDetailViewModel.ARG_IMAGE_URL to imageUrl,
            )
        )
    )
}
