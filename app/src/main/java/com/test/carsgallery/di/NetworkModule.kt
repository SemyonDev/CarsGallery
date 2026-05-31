package com.test.carsgallery.di

import android.content.Context
import com.test.carsgallery.BuildConfig
import com.test.carsgallery.data.remote.api.GalleryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://zipoapps-storage-test.nyc3.digitaloceanspaces.com/"
    private const val HTTP_CACHE_BYTES = 10L * 1024 * 1024   // 10 MB

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient =
        OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http_cache"), HTTP_CACHE_BYTES))
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                    )
                }
            }
            // Rewrite responses that lack (or disable) caching headers so that OkHttp's HTTP
            // cache stores them. A request-side Cache-Control header only controls *acceptance*
            // of a cached response; the response-side header controls *storage*. Without this
            // interceptor, a static asset served without Cache-Control would never be cached.
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                val cacheControl = response.header("Cache-Control")
                if (cacheControl.isNullOrEmpty()
                    || cacheControl.contains("no-cache", ignoreCase = true)
                    || cacheControl.contains("no-store", ignoreCase = true)
                ) {
                    response.newBuilder()
                        .header("Cache-Control", "public, max-age=3600")
                        .build()
                } else {
                    response
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideGalleryApi(retrofit: Retrofit): GalleryApi =
        retrofit.create(GalleryApi::class.java)
}
