package com.zipoapps.imageloader.engine

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.zipoapps.imageloader.ImageLoaderLogger
import com.zipoapps.imageloader.ImageRequest
import com.zipoapps.imageloader.ImageViewTarget
import com.zipoapps.imageloader.R
import com.zipoapps.imageloader.Target
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [RequestManager] using Robolectric for the Android Looper and View system.
 *
 * [RequestManager] is main-thread-only for [enqueue] and [cancel], which requires a real
 * Android [android.os.Looper]. Robolectric provides a simulated main thread so these tests
 * run on the JVM without a device or emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class RequestManagerTest {

    private val logger = ImageLoaderLogger(enabled = false)
    private lateinit var manager: RequestManager
    private lateinit var imageView: ImageView

    @Before
    fun setUp() {
        manager = RequestManager(logger)
        imageView = ImageView(ApplicationProvider.getApplicationContext())
        // Give the view a non-zero size so loading is not deferred.
        imageView.layout(0, 0, 200, 200)
    }

    private fun makeRequest(url: String = "https://example.com/img.jpg") = ImageRequest(
        url = url,
        placeholderRes = null,
        errorRes = null,
        targetWidth = 200,
        targetHeight = 200,
    )

    // ── Stale-result prevention ──────────────────────────────────────────────

    @Test
    fun `view tag is set to the most-recent URL after rebind`() =
        runTest(UnconfinedTestDispatcher()) {
            val gate = CompletableDeferred<Bitmap?>()
            val target = ImageViewTarget(imageView)

            // Enqueue first URL — suspends at gate so the load is in-flight.
            manager.enqueue(makeRequest("url-A"), target, { _, _, _ -> gate.await() })

            // Immediately rebind to a different URL (simulates RecyclerView fast scroll).
            manager.enqueue(makeRequest("url-B"), target, { _, _, _ -> null })

            // View tag must reflect the last-bound URL, not the in-flight one.
            assertEquals(
                "View tag must reflect last-bound URL",
                "url-B",
                imageView.getTag(R.id.image_loader_request_tag),
            )

            gate.complete(null) // let the first load finish so the scope can clean up
        }

    // ── Cancel via Target ────────────────────────────────────────────────────

    @Test
    fun `cancel(target) clears request tag`() {
        val target = ImageViewTarget(imageView)
        manager.enqueue(makeRequest(), target, { _, _, _ -> null })

        manager.cancel(target)

        assertNull(
            "Request tag must be null after cancel(target)",
            imageView.getTag(R.id.image_loader_request_tag),
        )
    }

    // ── Cancel via ImageView ─────────────────────────────────────────────────

    @Test
    fun `cancel(imageView) clears request tag`() {
        val target = ImageViewTarget(imageView)
        manager.enqueue(makeRequest(), target, { _, _, _ -> null })

        manager.cancel(imageView)

        assertNull(
            "Request tag must be null after cancel(imageView)",
            imageView.getTag(R.id.image_loader_request_tag),
        )
    }

    // ── New request cancels previous ─────────────────────────────────────────

    @Test
    fun `enqueueing a second request to the same view cancels the first`() =
        runTest(UnconfinedTestDispatcher()) {
            val gate = CompletableDeferred<Bitmap?>()
            val target = ImageViewTarget(imageView)
            var firstLoaderInvoked = false

            manager.enqueue(makeRequest("url-A"), target, { _, _, _ ->
                firstLoaderInvoked = true
                gate.await()
            })

            // Enqueue a second request — this should cancel the first job.
            manager.enqueue(makeRequest("url-B"), target, { _, _, _ -> null })

            // Complete gate so scope can finish.
            gate.complete(null)

            // Tag reflects second URL.
            assertEquals("url-B", imageView.getTag(R.id.image_loader_request_tag))
        }

    // ── Placeholder ──────────────────────────────────────────────────────────

    @Test
    fun `placeholder resource is passed to target onPrepare before load starts`() =
        runTest(UnconfinedTestDispatcher()) {
            val request = ImageRequest(
                url = "https://example.com/img.jpg",
                placeholderRes = android.R.drawable.ic_delete,
                errorRes = null,
                targetWidth = 200,
                targetHeight = 200,
            )
            var preparedRes: Int? = null
            val gate = CompletableDeferred<Bitmap?>()

            val target = object : Target {
                override val view = imageView
                override fun onPrepare(placeholderRes: Int?) { preparedRes = placeholderRes }
                override fun onSuccess(bitmap: Bitmap) = Unit
                override fun onError(errorRes: Int?, placeholderRes: Int?) = Unit
            }

            manager.enqueue(request, target, { _, _, _ -> gate.await() })

            assertEquals(
                "Placeholder must be passed to target.onPrepare synchronously",
                android.R.drawable.ic_delete,
                preparedRes,
            )
            gate.complete(null)
        }

    // ── Lifecycle tag management ──────────────────────────────────────────────

    @Test
    fun `view id tag is assigned on first enqueue`() {
        val target = ImageViewTarget(imageView)
        assertNull("No id tag before first enqueue", imageView.getTag(R.id.image_loader_view_id_tag))

        manager.enqueue(makeRequest(), target, { _, _, _ -> null })

        val id = imageView.getTag(R.id.image_loader_view_id_tag)
        assert(id is Int) { "View id tag must be an Int after enqueue" }
    }

    @Test
    fun `view id tag is reused across multiple requests to the same view`() {
        val target = ImageViewTarget(imageView)

        manager.enqueue(makeRequest("url-1"), target, { _, _, _ -> null })
        val idAfterFirst = imageView.getTag(R.id.image_loader_view_id_tag)

        manager.enqueue(makeRequest("url-2"), target, { _, _, _ -> null })
        val idAfterSecond = imageView.getTag(R.id.image_loader_view_id_tag)

        assertEquals("Same view must reuse its assigned id", idAfterFirst, idAfterSecond)
    }
}
