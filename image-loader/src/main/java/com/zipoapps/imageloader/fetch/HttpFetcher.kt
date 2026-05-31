package com.zipoapps.imageloader.fetch

import com.zipoapps.imageloader.ImageLoaderLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Downloads raw image bytes over HTTP using OkHttp.
 *
 * Uses [suspendCancellableCoroutine] + [Call.enqueue] so that coroutine cancellation
 * immediately aborts the OkHttp call via [Call.cancel] rather than waiting for the timeout.
 * `cont.resume()` is a no-op after cancellation, so the callbacks need no `isActive` guard.
 *
 * Depends on [Call.Factory] rather than [OkHttpClient] directly so tests can inject a
 * lightweight lambda without mocking a final class.
 */
internal class HttpFetcher(
    private val callFactory: Call.Factory,
    private val logger: ImageLoaderLogger,
) {

    constructor(
        okHttpClient: OkHttpClient? = null,
        timeoutMs: Long = 30_000L,
        logger: ImageLoaderLogger,
    ) : this(
        callFactory = okHttpClient ?: OkHttpClient.Builder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build(),
        logger = logger,
    )

    suspend fun fetch(url: String): ByteArray? = suspendCancellableCoroutine { cont ->
        val request = Request.Builder().url(url).build()
        val call = callFactory.newCall(request)

        cont.invokeOnCancellation {
            logger.d(TAG, "Cancelling in-flight call for $url")
            call.cancel()
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logger.w(TAG, "Network error fetching $url", e)
                cont.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { r ->
                    if (!r.isSuccessful) {
                        logger.w(TAG, "HTTP ${r.code} for $url")
                        cont.resume(null)
                        return
                    }
                    val bytes = try {
                        r.body?.bytes()
                    } catch (e: Exception) {
                        logger.w(TAG, "Failed to read body for $url", e)
                        null
                    }
                    logger.d(TAG, "Downloaded ${bytes?.size ?: 0} bytes from $url")
                    cont.resume(bytes)
                }
            }
        })
    }

    private companion object {
        private const val TAG = "HttpFetcher"
    }
}
