# image-loader

A lightweight Android image loading library built with Kotlin Coroutines and OkHttp.

Runtime dependencies: `OkHttp` and `kotlinx-coroutines` only.

---

## Requirements

- Android **minSdk 26**
- Kotlin Coroutines

---

## Integration

The library is a local Gradle module. Add it to your app module:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":image-loader"))
}
```

---

## Quick Start

### Kotlin

```kotlin
ImageLoader.with(context)
    .load("https://example.com/photo.jpg")
    .placeholder(R.drawable.ic_placeholder)
    .error(R.drawable.ic_error)
    .into(imageView)
```

### Java

All public methods are `@JvmStatic`:

```java
ImageLoader.with(context)
    .load("https://example.com/photo.jpg")
    .placeholder(R.drawable.ic_placeholder)
    .error(R.drawable.ic_error)
    .into(imageView);
```

---

## Initialisation (optional)

Call `ImageLoader.initialize()` once in `Application.onCreate` to provide a custom configuration.
If you skip this step the library self-initialises on the first `with()` call using the defaults.

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ImageLoader.initialize(
            context = this,
            config = ImageLoaderConfig(
                okHttpClient      = myOkHttpClient,   // share your app's connection pool
                memoryCacheSizeBytes = 50L * 1024 * 1024,   // 50 MB (default: 1/4 of memoryClass)
                diskCacheSizeBytes   = 200L * 1024 * 1024,  // 200 MB (default: 100 MB)
                cacheExpirationMs    = 2L * 60 * 60 * 1000, // 2 h   (default: 4 h)
                preferredBitmapConfig = Bitmap.Config.RGB_565, // halves memory for opaque images
                loggingEnabled       = BuildConfig.DEBUG,
            )
        )
    }
}
```

### Configuration reference

| Property | Default | Description |
|---|---|---|
| `memoryCacheSizeBytes` | Auto (1/4 of `ActivityManager.memoryClass`) | In-process LRU bitmap cache size in bytes. Pass `0` for auto. |
| `diskCacheSizeBytes` | 100 MB | Maximum size of the on-disk cache directory. |
| `cacheExpirationMs` | 4 hours | How long a disk entry is considered fresh. Measured from write time. |
| `networkTimeoutMs` | 30 s | Connect / read / write timeout. Ignored when `okHttpClient` is non-null. |
| `okHttpClient` | `null` (library creates its own) | Supply your app's client to share its connection pool and interceptors. |
| `preferredBitmapConfig` | `ARGB_8888` | Pixel format. Use `RGB_565` to halve memory for images without transparency. |
| `loggingEnabled` | `false` | Sends cache-hit/miss and request events to Logcat when `true`. |

---

## Request Builder API

Chain any combination of these methods before calling `into()`:

```kotlin
ImageLoader.with(context)
    .load(url)                          // String? — null or blank shows placeholder
    .placeholder(R.drawable.loading)    // shown immediately while loading
    .error(R.drawable.broken)           // shown when the load fails
    .onSuccess { hideSpinner() }        // called on the main thread after bitmap is set
    .onError   { hideSpinner() }        // called on the main thread after error drawable is set
    .into(imageView)                    // starts the load; cancels any previous request on this view
```

All methods are optional. The only requirement is a `load()` + `into()` pair.

---

## Caching

### Two-tier hierarchy

| Tier | Storage | Survives process restart | Max size |
|---|---|---|---|
| Memory | In-process LRU (`LinkedHashMap`, access-ordered, size-bounded in bytes) | No | Configurable |
| Disk | Raw network bytes + `.meta` timestamp file | Yes | Configurable |

### Cache keys

Keys are MD5 hashes of `url + ":" + width + "x" + height`. The same URL loaded at different
view sizes produces distinct entries — a 120×120 thumbnail and a 1080×1920 full-screen image
are cached and served independently.

### Cache invalidation

```kotlin
ImageLoader.clearMemoryCache()   // evict in-process LRU only
ImageLoader.clearDiskCache()     // delete all files in the cache directory
ImageLoader.invalidateAll()      // both
```

---

## Cancellation

In-flight requests are cancelled automatically when the target view detaches from its window
(e.g. RecyclerView item scrolls off screen). You can also cancel explicitly:

```kotlin
// Cancel by ImageView reference
ImageLoader.cancel(imageView)

// Cancel by Target reference (see Custom Targets below)
ImageLoader.cancel(target)
```

In a `RecyclerView` adapter, call cancel in `onViewRecycled` to release resources immediately:

```kotlin
override fun onViewRecycled(holder: MyViewHolder) {
    super.onViewRecycled(holder)
    ImageLoader.cancel(holder.imageView)
}
```

---

## Deferred loading

If `into()` is called before the view has been measured (width or height is 0), the load is
automatically deferred until the view's next layout pass. No extra code is needed — the correct
`inSampleSize` is computed from the actual view dimensions once they are available.

---

## Custom Targets

Implement `Target` to load images into anything other than a plain `ImageView` — notification
`RemoteViews`, app-widget `RemoteViews`, or custom views:

```kotlin
class NotificationTarget(
    private val builder: NotificationCompat.Builder,
    private val notificationId: Int,
) : Target {

    // Must be a View for lifecycle tracking (attach/detach cancellation).
    // Use a dummy view if your target is not view-backed.
    override val view: View = View(context)

    override fun onPrepare(placeholderRes: Int?) = Unit  // no placeholder in a notification

    override fun onSuccess(bitmap: Bitmap) {
        builder.setLargeIcon(bitmap)
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    override fun onError(errorRes: Int?, placeholderRes: Int?) = Unit
}

// Usage
ImageLoader.with(context)
    .load("https://example.com/photo.jpg")
    .into(NotificationTarget(builder, NOTIFICATION_ID))
```

`ImageViewTarget` is the built-in `Target` implementation created automatically by
`into(imageView)`. Instantiate it directly only when you need a `Target` reference:

```kotlin
val target = ImageViewTarget(imageView)
ImageLoader.cancel(target)
```

---

## Memory pressure

The library registers a `ComponentCallbacks2` listener on the application context.
When the OS signals `TRIM_MEMORY_UI_HIDDEN` (all app UI goes to background) or
`onLowMemory`, the in-process bitmap cache is cleared automatically to reduce RSS
and lower the chance of process kill.

---

## How it works

### Load pipeline

```
ImageLoader.with(ctx).load(url).into(view)
  │
  └── RequestManager.enqueue()          (main thread)
        ├── show placeholder immediately
        ├── cancel previous request on this view
        └── scope.launch(Dispatchers.IO)
              └── RequestDeduplicator.getOrLoad(key)
                    ├── MemoryCache.get(key)      → hit: deliver bitmap
                    ├── DiskCache.getFile(key)    → hit: BitmapDecoder.decodeFile → promote to memory → deliver
                    └── HttpFetcher.fetch(url)    → BitmapDecoder.decode → write disk then memory → deliver
              └── withContext(Main): stale-URL guard → Target.onSuccess / Target.onError
```

### Request deduplication

When multiple views request the same URL + size simultaneously, only **one** network call is
made. The first coroutine performs the fetch; all others await a shared `CompletableDeferred`.
If the primary coroutine is cancelled, the next waiter automatically becomes the new primary
rather than silently dropping the load.

### Bitmap downsampling

`BitmapDecoder` uses a two-pass approach:
1. `inJustDecodeBounds = true` — reads image dimensions without allocating pixel memory.
2. Compute `inSampleSize` as the largest power-of-two that keeps the decoded bitmap at least
   as large as the target view.
3. Decode again at the computed sample size.

Disk-cache hits are decoded directly from the file path via `BitmapFactory.decodeFile`,
avoiding the intermediate `ByteArray` heap spike that would occur if the file were read
into memory first.

### Disk cache atomicity

`DiskCache.put` writes to `.cache.tmp` and `.meta.tmp` temp files first, then renames
both atomically via `rename(2)`. A process crash during a write leaves only `.tmp` orphans,
which are cleaned up on the next construction of `DiskCache`.

---

## Testing

The library includes unit tests and integration tests that run on the JVM (no device required):

```bash
./gradlew :image-loader:testDebugUnitTest
```

| Test class | Coverage |
|---|---|
| `CacheKeyTest` | Key determinism, size encoding, filesystem safety |
| `MemoryCacheTest` | LRU eviction, byte-size tracking, concurrent access |
| `DiskCacheTest` | File I/O, expiration, orphan cleanup, meta parsing |
| `CacheManagerTest` | Two-tier coordination, invalidation ordering |
| `BitmapDecoderTest` | Sample size calculation |
| `RequestDeduplicationTest` | Concurrent dedup, cancellation recovery, retry |
| `ImageEngineIntegrationTest` | Full memory → disk → network pipeline |
| `HttpFetcherTest` | 200 OK, 404, 500, network failure, body-read exception |
| `RequestManagerTest` | Stale-result detection, cancel, placeholder delivery, view-id tagging |
