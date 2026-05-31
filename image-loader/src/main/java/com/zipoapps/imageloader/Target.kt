package com.zipoapps.imageloader

import android.graphics.Bitmap
import android.view.View

/**
 * Abstraction over the destination that receives a loaded bitmap.
 *
 * Implement this to load images into anything other than [android.widget.ImageView]:
 * notification RemoteViews, app-widget RemoteViews, custom views, etc.
 * The built-in implementation is [ImageViewTarget].
 *
 * All callbacks are invoked on the **main thread**.
 */
interface Target {

    /**
     * The [View] this target is bound to. Used by the engine for size measurement,
     * lifecycle tracking (cancel on detach), and stale-result detection after recycling.
     */
    val view: View

    /** Called on the main thread before the load starts. Show a placeholder here. */
    fun onPrepare(placeholderRes: Int?)

    /** Called on the main thread when the bitmap is ready to display. */
    fun onSuccess(bitmap: Bitmap)

    /**
     * Called on the main thread when the load fails.
     *
     * @param errorRes error drawable resource ID, or null if none was configured.
     * @param placeholderRes fallback used when [errorRes] is null.
     */
    fun onError(errorRes: Int?, placeholderRes: Int?)
}
