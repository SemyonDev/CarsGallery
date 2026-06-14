package com.test.carsgallery.presentation.compose.mvi.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.test.carsgallery.presentation.compose.mvi.detail.DetailEffect
import com.test.carsgallery.presentation.compose.mvi.detail.DetailScreen
import com.test.carsgallery.presentation.compose.mvi.detail.DetailViewModel
import com.test.carsgallery.presentation.compose.mvi.gallery.GalleryEffect
import com.test.carsgallery.presentation.compose.mvi.gallery.GalleryScreen
import com.test.carsgallery.presentation.compose.mvi.gallery.GalleryViewModel

/** Navigation 3 keys for the MVI flow. */
sealed interface MviNavKey : NavKey

data object GalleryKey : MviNavKey

data class DetailKey(val imageId: String, val imageUrl: String) : MviNavKey

/**
 * Navigation 3 host for the compose_mvi flow.
 *
 * Mirrors the MVVM host's structure, but navigation is driven by the ViewModels' one-shot effect
 * streams: each entry collects its store's effects and translates them into back-stack mutations,
 * keeping the screens free of any direct navigation calls.
 */
@Composable
fun MviNavHost(
    getImagesUseCase: GetImagesUseCase,
    modifier: Modifier = Modifier,
) {
    val backStack = remember { mutableStateListOf<NavKey>(GalleryKey) }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<GalleryKey> {
                val viewModel: GalleryViewModel = viewModel { GalleryViewModel(getImagesUseCase) }
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(viewModel) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is GalleryEffect.NavigateToDetail ->
                                backStack.add(DetailKey(effect.imageId, effect.imageUrl))
                        }
                    }
                }
                GalleryScreen(state = state, onIntent = viewModel::onIntent)
            }
            entry<DetailKey> { key ->
                val viewModel: DetailViewModel = viewModel { DetailViewModel(key.imageId, key.imageUrl) }
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(viewModel) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            DetailEffect.NavigateBack -> backStack.removeLastOrNull()
                        }
                    }
                }
                DetailScreen(state = state, onIntent = viewModel::onIntent)
            }
        },
    )
}
