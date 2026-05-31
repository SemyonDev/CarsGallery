package com.test.carsgallery

import com.test.carsgallery.domain.model.ImageItem
import com.test.carsgallery.domain.repository.GalleryRepository

/**
 * Deterministic test double for [GalleryRepository] used in instrumented tests.
 *
 * ### Why not MockK
 * MockK's inline mocking requires a JVMTI native agent (`libmockkjvmtiagent.so`) that is not
 * available on all Android runtime configurations. Even though [GalleryRepository] is an
 * interface (not a final class), MockK attempts to bootstrap its agent at class-initialization
 * time and throws [io.mockk.proxy.MockKAgentException] before any test code runs.
 *
 * A hand-written fake avoids this entirely and is simpler to reason about in instrumented tests.
 *
 * ### Usage
 * ```kotlin
 * fake.enqueueSuccess(items)         // next call returns Success
 * fake.enqueueFailure(NetworkException(IOException()))  // next call returns Failure
 * ```
 * Results are consumed in FIFO order. If the queue is empty, [getImages] returns an empty
 * success list so tests that don't care about the result still pass.
 */
class FakeGalleryRepository : GalleryRepository {

    private val queue = ArrayDeque<Result<List<ImageItem>>>()

    fun enqueueSuccess(items: List<ImageItem> = emptyList()) {
        queue.addLast(Result.success(items))
    }

    fun enqueueFailure(error: Throwable) {
        queue.addLast(Result.failure(error))
    }

    override suspend fun getImages(): Result<List<ImageItem>> =
        if (queue.isNotEmpty()) queue.removeFirst() else Result.success(emptyList())
}
