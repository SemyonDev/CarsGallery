# CarsGallery

An Android image gallery app built on a **custom image loading library** — no Glide, Coil, Picasso, or Fresco.

---

## Modules

| Module | Purpose |
|---|---|
| `:image-loader` | Standalone image loading SDK. Zero app-level dependencies. |
| `:app` | Sample app that consumes the SDK. Demonstrates Clean Architecture + MVVM + Hilt. |

---

## App Architecture

The app follows **Clean Architecture** with three layers and strict dependency rules.

```
presentation  →  domain  ←  data
```

```
app/src/main/java/com/test/carsgallery/
│
├── CarsGalleryApp.kt                     Application — Hilt entry point, ImageLoader init
│
├── data/
│   ├── datasource/RemoteDataSource.kt    Data-layer interface
│   ├── remote/
│   │   ├── api/GalleryApi.kt             Retrofit suspend interface
│   │   ├── dto/ImageDto.kt               Network DTO (Gson)
│   │   └── RemoteDataSourceImpl.kt       DTO → domain mapping
│   └── repository/GalleryRepositoryImpl.kt  IOException/HttpException → NetworkException
│
├── domain/
│   ├── model/ImageItem.kt                Domain entity (no framework annotations)
│   ├── exception/NetworkException.kt
│   ├── repository/GalleryRepository.kt   Interface returning Result<List<ImageItem>>
│   └── usecase/GetImagesUseCase.kt       Single-responsibility delegate
│
├── di/
│   ├── DataModule.kt                     Hilt bindings for repository + data source
│   └── NetworkModule.kt                  OkHttpClient, Retrofit, GalleryApi
│
└── presentation/
    ├── UiState.kt                        sealed class Loading / Success / Error
    ├── GalleryViewModel.kt               StateFlow<UiState>; layout mode in SavedStateHandle
    └── screens/
        ├── gallery/
        │   ├── GalleryFragment.kt        repeatOnLifecycle; SwipeRefresh; Clear Cache
        │   └── GalleryAdapter.kt         ListAdapter + DiffUtil; onViewRecycled cancel
        └── imagedetails/
            ├── ImageDetailFragment.kt    Observes ImageDetailViewModel.loadState
            ├── ImageDetailViewModel.kt   Args from SavedStateHandle; load state machine
            └── ImageLoadState.kt         sealed class Loading / Success / Error
```

---

## Data Flow

```
GalleryFragment  →  GalleryViewModel  →  GetImagesUseCase  →  GalleryRepository
                                                                      ↓
                                                           RemoteDataSourceImpl
                                                                      ↓
                                                              Retrofit / OkHttp
                                                                      ↓
                                                           image_list.json (remote)
```

Image loading per list item:

```
GalleryAdapter.bind(item)
  └── ImageLoader.with(ctx).load(url).into(imageView)
        └── ImageEngine.loadBitmap(url, w, h)
              ├── MemoryCache hit  →  return immediately
              ├── DiskCache hit    →  BitmapDecoder.decodeFile  →  promote to memory
              └── HttpFetcher.fetch(url)  →  BitmapDecoder.decode  →  write disk + memory
```

---

## Tech Stack

| Concern | Solution |
|---|---|
| Dependency injection | Hilt |
| Networking | Retrofit + OkHttp + Gson |
| Async | Kotlin Coroutines + StateFlow |
| Navigation | Navigation Component (fragment-ktx) |
| Image loading | Custom `:image-loader` module |
| Testing | JUnit 4, MockK, Turbine, Robolectric, Espresso, Hilt Testing |

---

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Unit tests (app + image-loader)
./gradlew testDebugUnitTest

# Instrumented tests (requires connected device or emulator)
./gradlew connectedDebugAndroidTest

# Install on connected device
./gradlew installDebug
```

**Minimum SDK:** 26  
**Target SDK:** 36  
**Remote endpoint:** `https://zipoapps-storage-test.nyc3.digitaloceanspaces.com/image_list.json`
