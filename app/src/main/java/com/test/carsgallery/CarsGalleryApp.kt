package com.test.carsgallery

import android.app.Application
import com.zipoapps.imageloader.ImageLoader
import com.zipoapps.imageloader.ImageLoaderConfig
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class CarsGalleryApp : Application() {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        initImageLoader()
    }

    private fun initImageLoader() {
        ImageLoader.initialize(
            context = this,
            config = ImageLoaderConfig(
                // Share the app's OkHttpClient so all HTTP traffic uses one connection pool.
                okHttpClient = okHttpClient,
                cacheExpirationMs = ImageLoaderConfig.DEFAULT_CACHE_EXPIRATION_MS,
                diskCacheSizeBytes = ImageLoaderConfig.DEFAULT_DISK_CACHE_SIZE_BYTES,
                loggingEnabled = BuildConfig.DEBUG,
            ),
        )
    }
}
