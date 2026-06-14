# CarsGallery

An Android image gallery app built on a **custom image loading library** — no Glide, Coil, Picasso, or Fresco.

The **same gallery is implemented three ways** so the patterns can be compared directly. On launch a
3-button chooser (`HomeFragment`) lets you pick an implementation:

| Button | UI toolkit | Navigation | State management |
|---|---|---|---|
| **XML** | View System (Fragments) | Navigation Component | MVVM |
| **Compose MVVM** | Jetpack Compose | **Navigation 3** | MVVM |
| **Compose MVI** | Jetpack Compose | **Navigation 2** (Navigation Compose, type-safe routes) | MVI (intent / state / effect) |

All three share the same `domain` + `data` layers and the same custom `:image-loader`.

---

## Modules

| Module | Purpose |
|---|---|
| `:image-loader` | Standalone image loading SDK. Zero app-level dependencies. |
| `:app` | Sample app that consumes the SDK. Demonstrates Clean Architecture + Hilt, with three interchangeable presentation implementations. |

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
│   │   ├── dto/ImageDto.kt               Network DTO (Moshi)
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
    ├── MainActivity.kt                   Hosts the NavHostFragment (XML world)
    ├── UiState.kt                        sealed class Loading / Success / Error
    │
    ├── screens/                          ── Implementation 1: XML + View System + MVVM ──
    │   ├── home/HomeFragment.kt          3-button chooser (nav-graph start destination)
    │   ├── gallery/
    │   │   ├── GalleryFragment.kt        repeatOnLifecycle; SwipeRefresh; Clear Cache
    │   │   ├── GalleryViewModel.kt       StateFlow<UiState>; layout mode in SavedStateHandle
    │   │   └── GalleryAdapter.kt         ListAdapter + DiffUtil; onViewRecycled cancel
    │   └── imagedetails/
    │       ├── ImageDetailFragment.kt    Observes ImageDetailViewModel.loadState
    │       ├── ImageDetailViewModel.kt   Args from SavedStateHandle; load state machine
    │       └── ImageLoadState.kt         sealed class Loading / Success / Error
    │
    └── compose/
        ├── common/                       Shared Compose code for both Compose flows
        │   ├── theme/Theme.kt            CarsGalleryTheme (Material 3, dynamic color)
        │   └── components/
        │       ├── NetworkImage.kt       Bridges :image-loader into Compose (AndroidView + ImageView)
        │       ├── GalleryGrid.kt        Grid/list rendering + per-item progress
        │       ├── DetailContent.kt      Shared stateless detail UI
        │       └── StateViews.kt         LoadingState / ErrorState
        │
        ├── mvvm/                         ── Implementation 2: Compose + Navigation 3 + MVVM ──
        │   ├── ComposeMvvmActivity.kt    @AndroidEntryPoint; setContent { MvvmNavHost }
        │   ├── navigation/MvvmNavigation.kt  NavDisplay + NavKey back stack + entry decorators
        │   ├── gallery/                  GalleryUiState / GalleryViewModel / GalleryScreen
        │   └── detail/                   DetailUiState / DetailViewModel / DetailScreen
        │
        └── mvi/                          ── Implementation 3: Compose + Navigation 2 + MVI ──
            ├── ComposeMviActivity.kt     @AndroidEntryPoint; setContent { MviNavHost }
            ├── navigation/MviNavigation.kt   NavHost + type-safe @Serializable routes
            ├── gallery/                  GalleryContract (State/Intent/Effect) / ViewModel / Screen
            └── detail/                   DetailContract (State/Intent/Effect) / ViewModel / Screen
```

> The Compose ViewModels are not Hilt ViewModels — they are created with `viewModel { ... }` factories
> fed the Hilt-field-injected `GetImagesUseCase` and the navigation arguments, so they stay scoped to
> their navigation entry while reusing the shared domain layer.

---

## The three implementations

Each implementation renders an identical gallery → detail flow with explicit **Loading / Success / Error**
UI states, but differs in toolkit, navigation, and state-management style:

- **XML (View System + MVVM).** Fragments hosted by a single `MainActivity` + Navigation Component
  (`nav_graph.xml`). The View observes a `StateFlow<UiState>` and calls intent-named ViewModel methods.

- **Compose MVVM (Navigation 3).** 100% Compose. `NavDisplay` renders a `mutableStateListOf<NavKey>`
  back stack that you mutate directly; per-entry ViewModel scoping uses the saveable-state +
  ViewModel-store nav-entry decorators. Each screen observes one `StateFlow` of an immutable UI-state.

- **Compose MVI (Navigation 2).** 100% Compose. A `NavHostController` owns the back stack with
  **type-safe `@Serializable` routes** (`composable<Route>`, `toRoute<Route>()`); ViewModels auto-scope
  to each `NavBackStackEntry`. The UI sends everything through a single `onIntent(...)` funnel, and
  navigation is emitted as one-shot **effects** that the host translates into controller calls.

The two Compose flows run as their own `@AndroidEntryPoint` activities to keep the Navigation 3 world
cleanly separated from the fragment / Navigation Component world.

---

## Data Flow

All three implementations share the same domain/data path below — only the leftmost node differs
(`GalleryFragment`, or the Compose MVVM / MVI gallery screen + ViewModel):

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
| UI toolkits | View System (XML) + Jetpack Compose (Material 3) |
| Navigation | Navigation Component (XML) · Navigation 3 (Compose MVVM) · Navigation Compose / Nav2 type-safe routes (Compose MVI) |
| Networking | Retrofit + OkHttp + Moshi |
| Async | Kotlin Coroutines + StateFlow |
| Image loading | Custom `:image-loader` module |
| Testing | JUnit 4, MockK, Turbine, Robolectric, Espresso, Hilt Testing |

> Compose uses AGP's built-in Kotlin with the `org.jetbrains.kotlin.plugin.compose` plugin; the
> Compose MVI routes use `kotlinx.serialization` for type-safe navigation.

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
