package com.zipoapps.imageloader.engine

import android.graphics.Bitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

/**
 * Ensures only one network call is made for a given cache key regardless of how many
 * coroutines request it concurrently. Keys encode both URL and target dimensions, so two
 * views loading the same URL at different sizes are not deduplicated.
 *
 * ### Protocol
 * The first coroutine wins a [ConcurrentHashMap.putIfAbsent] race and becomes the
 * **primary requester**: it executes [load] and completes a shared [CompletableDeferred].
 * All subsequent coroutines for the same key await that deferred — zero extra network calls.
 *
 * ### Cancellation
 * When the primary is cancelled its deferred is cancelled, waking all waiters with a
 * [CancellationException]. Each waiter checks [coroutineContext.isActive]:
 * - `false` — own coroutine was cancelled; re-throw.
 * - `true` — only the deferred was cancelled; loop back and compete to become the new primary.
 *
 * [coroutineContext.ensureActive] at the top of each iteration makes cancellation prompt
 * even if a cancelled coroutine wins the putIfAbsent race.
 */
internal class RequestDeduplicator {

    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<Bitmap?>>()

    suspend fun getOrLoad(key: String, load: suspend () -> Bitmap?): Bitmap? {
        while (true) {
            coroutineContext.ensureActive()

            val existing = inFlight[key]
            if (existing != null) {
                return try {
                    existing.await()
                } catch (e: CancellationException) {
                    if (coroutineContext.isActive) continue else throw e
                } catch (_: Exception) {
                    if (coroutineContext.isActive) continue else return null
                }
            }

            val deferred = CompletableDeferred<Bitmap?>()
            if (inFlight.putIfAbsent(key, deferred) != null) continue

            return try {
                val result = load()
                deferred.complete(result)
                result
            } catch (e: CancellationException) {
                deferred.cancel(CancellationException("Primary requester cancelled", e))
                throw e
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                null
            } finally {
                inFlight.remove(key, deferred)
            }
        }
    }
}
