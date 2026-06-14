package com.test.carsgallery.presentation.compose.mvvm.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.test.carsgallery.domain.usecase.GetImagesUseCase
import com.test.carsgallery.presentation.compose.mvvm.detail.DetailScreen
import com.test.carsgallery.presentation.compose.mvvm.detail.DetailViewModel
import com.test.carsgallery.presentation.compose.mvvm.gallery.GalleryScreen
import com.test.carsgallery.presentation.compose.mvvm.gallery.GalleryViewModel

/** Navigation 3 keys for the MVVM flow. Each key carries the data its destination needs. */
sealed interface MvvmNavKey : NavKey

data object GalleryKey : MvvmNavKey

data class DetailKey(val imageId: String, val imageUrl: String) : MvvmNavKey

/**
 * Navigation 3 host for the compose_mvvm flow.
 *
 * The back stack is a plain observable list of keys; navigation is list mutation. Each entry is
 * scoped its own ViewModelStore via [rememberViewModelStoreNavEntryDecorator], so the per-screen
 * ViewModels are created lazily and cleared when their entry is popped.
 *
 * ViewModels are built with manual factories (`viewModel { ... }`) so they can receive the
 * Hilt-provided [getImagesUseCase] and the navigation-key arguments without coupling to Hilt's
 * Compose integration.
 */
@Composable
fun MvvmNavHost(
    getImagesUseCase: GetImagesUseCase,
    modifier: Modifier = Modifier,
) {
    val backStack = remember { mutableStateListOf<NavKey>(GalleryKey) }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            // Scene-setup decorator is applied by NavDisplay automatically; we add saveable-state
            // (rememberSaveable survives config changes) and ViewModelStore (per-entry, cleared on pop).
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<GalleryKey> {
                val viewModel: GalleryViewModel = viewModel { GalleryViewModel(getImagesUseCase) }
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                GalleryScreen(
                    uiState = uiState,
                    onToggleGrid = viewModel::toggleGrid,
                    onRetry = viewModel::loadImages,
                    onImageClick = { item -> backStack.add(DetailKey(item.id, item.imageUrl)) },
                )
            }
            entry<DetailKey> { key ->
                val viewModel: DetailViewModel = viewModel { DetailViewModel(key.imageId, key.imageUrl) }
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                DetailScreen(
                    uiState = uiState,
                    onBack = { backStack.removeLastOrNull() },
                    onImageLoading = viewModel::onImageLoading,
                    onImageSuccess = viewModel::onImageSuccess,
                    onImageError = viewModel::onImageError,
                )
            }
        },
    )
}
