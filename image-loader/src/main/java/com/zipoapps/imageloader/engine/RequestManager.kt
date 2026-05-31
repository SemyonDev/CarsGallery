package com.zipoapps.imageloader.engine

import android.graphics.Bitmap
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.annotation.MainThread
import com.zipoapps.imageloader.ImageLoaderLogger
import com.zipoapps.imageloader.ImageRequest
import com.zipoapps.imageloader.R
import com.zipoapps.imageloader.Target
import com.zipoapps.imageloader.cache.CacheKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Orchestrates per-view request lifecycle and cross-view deduplication.
 *
 * ### Threading
 * [enqueue] and [cancel] are main-thread-only. [shutdown] may be called from any thread.
 * [ConcurrentHashMap] is used for [activeJobs] so main-thread mutations and the off-thread
 * [shutdown] call do not race.
 *
 * ### Deferred loading
 * When [enqueue] is called before the view is measured (width/height == 0), an
 * [View.OnLayoutChangeListener] is attached and the job starts once the view has dimensions.
 * The listener is cleaned up on rebind, detach, and explicit [cancel].
 *
 * ### Listener accumulation
 * Both the lifecycle listener and the layout listener are stored as view tags and removed
 * before new ones are added on every rebind, preventing accumulation across RecyclerView recycling.
 *
 * ### Stale-result prevention
 * The current URL is stored in [R.id.image_loader_request_tag]. On result delivery a mismatch
 * means the view was rebound while the load was in flight; the result is discarded.
 */
internal class RequestManager(private val logger: ImageLoaderLogger) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val deduplicator = RequestDeduplicator()
    private val activeJobs = ConcurrentHashMap<Int, Job>()
    private var viewIdCounter = 0

    @MainThread
    fun enqueue(
        request: ImageRequest,
        target: Target,
        loader: suspend (url: String, targetWidth: Int, targetHeight: Int) -> Bitmap?,
        onSuccess: (() -> Unit)? = null,
        onError: (() -> Unit)? = null,
    ) {
        assertMainThread()

        val view = target.view
        val viewId = getOrAssignViewId(view)

        activeJobs.remove(viewId)?.cancel()
        clearLayoutListener(view)

        target.onPrepare(request.placeholderRes)

        // Track current URL for stale-result detection.
        view.setTag(R.id.image_loader_request_tag, request.url)

        if (view.width > 0 && view.height > 0) {
            startJob(target, viewId, request, loader, onSuccess, onError)
        } else {
            scheduleOnLayout(target, viewId, request, loader, onSuccess, onError)
        }
    }

    @MainThread
    fun cancel(target: Target) = cancelByView(target.view)

    @MainThread
    fun cancel(imageView: ImageView) = cancelByView(imageView)

    fun shutdown() {
        scope.cancel()
        activeJobs.clear()
    }

    @MainThread
    private fun cancelByView(view: View) {
        assertMainThread()
        val viewId = view.getTag(R.id.image_loader_view_id_tag) as? Int ?: return
        activeJobs.remove(viewId)?.cancel()
        clearLayoutListener(view)
        view.setTag(R.id.image_loader_request_tag, null)
    }

    private fun startJob(
        target: Target,
        viewId: Int,
        request: ImageRequest,
        loader: suspend (String, Int, Int) -> Bitmap?,
        onSuccess: (() -> Unit)?,
        onError: (() -> Unit)?,
    ) {
        val view = target.view
        val job = scope.launch {
            val dedupKey = CacheKey.fromRequest(request.url, request.targetWidth, request.targetHeight)
            val bitmap = deduplicator.getOrLoad(dedupKey) {
                loader(request.url, request.targetWidth, request.targetHeight)
            }

            withContext(Dispatchers.Main) {
                if (view.getTag(R.id.image_loader_request_tag) != request.url) return@withContext

                if (bitmap != null) {
                    target.onSuccess(bitmap)
                    onSuccess?.invoke()
                } else {
                    target.onError(request.errorRes, request.placeholderRes)
                    onError?.invoke()
                }
            }
        }

        activeJobs[viewId] = job
        attachLifecycleObserver(view, viewId, activeJobs, logger)
    }

    private fun scheduleOnLayout(
        target: Target,
        viewId: Int,
        request: ImageRequest,
        loader: suspend (String, Int, Int) -> Bitmap?,
        onSuccess: (() -> Unit)?,
        onError: (() -> Unit)?,
    ) {
        val view = target.view
        val listener = object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View, l: Int, t: Int, r: Int, b: Int, ol: Int, ot: Int, or_: Int, ob: Int,
            ) {
                if (v.width <= 0 || v.height <= 0) return
                v.removeOnLayoutChangeListener(this)
                v.setTag(R.id.image_loader_layout_listener_tag, null)

                // Discard if the view was rebound to a different URL while waiting for layout.
                if (v.getTag(R.id.image_loader_request_tag) != request.url) return

                val sizedRequest = request.copy(targetWidth = v.width, targetHeight = v.height)
                startJob(target, viewId, sizedRequest, loader, onSuccess, onError)
            }
        }
        view.setTag(R.id.image_loader_layout_listener_tag, listener)
        view.addOnLayoutChangeListener(listener)

        // Attach lifecycle observer so view detach cancels the layout wait too.
        attachLifecycleObserver(view, viewId, activeJobs, logger)
    }

    private fun clearLayoutListener(view: View) {
        val old = view.getTag(R.id.image_loader_layout_listener_tag) as? View.OnLayoutChangeListener
        if (old != null) {
            view.removeOnLayoutChangeListener(old)
            view.setTag(R.id.image_loader_layout_listener_tag, null)
        }
    }

    private fun attachLifecycleObserver(
        view: View,
        viewId: Int,
        jobs: ConcurrentHashMap<Int, Job>,
        log: ImageLoaderLogger,
    ) {
        val oldListener = view.getTag(R.id.image_loader_lifecycle_tag)
                as? View.OnAttachStateChangeListener
        oldListener?.let { view.removeOnAttachStateChangeListener(it) }

        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit

            override fun onViewDetachedFromWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.setTag(R.id.image_loader_lifecycle_tag, null)

                // Cancel any pending layout wait.
                val ll = v.getTag(R.id.image_loader_layout_listener_tag) as? View.OnLayoutChangeListener
                if (ll != null) {
                    v.removeOnLayoutChangeListener(ll)
                    v.setTag(R.id.image_loader_layout_listener_tag, null)
                }

                jobs.remove(viewId)?.cancel()
                log.d(TAG, "Cancelled job for detached view id=$viewId")
            }
        }

        view.setTag(R.id.image_loader_lifecycle_tag, listener)
        view.addOnAttachStateChangeListener(listener)
    }

    private fun getOrAssignViewId(view: View): Int {
        val existing = view.getTag(R.id.image_loader_view_id_tag) as? Int
        if (existing != null) return existing
        val newId = ++viewIdCounter
        view.setTag(R.id.image_loader_view_id_tag, newId)
        return newId
    }

    private fun assertMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "ImageLoader must be called from the main thread, but was called on '${Thread.currentThread().name}'"
        }
    }

    private companion object {
        private const val TAG = "RequestManager"
    }
}
