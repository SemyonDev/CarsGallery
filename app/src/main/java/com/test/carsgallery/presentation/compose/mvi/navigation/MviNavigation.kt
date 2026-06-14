package com.test.carsgallery.presentation.compose.mvi.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.test.carsgallery.domain.usecase.GetImagesUseCase
import com.test.carsgallery.presentation.compose.mvi.detail.DetailEffect
import com.test.carsgallery.presentation.compose.mvi.detail.DetailScreen
import com.test.carsgallery.presentation.compose.mvi.detail.DetailViewModel
import com.test.carsgallery.presentation.compose.mvi.gallery.GalleryEffect
import com.test.carsgallery.presentation.compose.mvi.gallery.GalleryScreen
import com.test.carsgallery.presentation.compose.mvi.gallery.GalleryViewModel
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation 2 (Navigation Compose) routes for the MVI flow. Each route is a
 * `@Serializable` destination; the data class carries its arguments and the library handles
 * encoding, so the (slash-bearing) image URL needs no manual escaping.
 */
@Serializable
data object GalleryRoute

@Serializable
data class DetailRoute(val imageId: String, val imageUrl: String)

/**
 * Navigation Compose (Navigation 2) host for the compose_mvi flow.
 *
 * Deliberately uses Navigation 2 — not Navigation 3 like compose_mvvm — to contrast the two
 * systems. Here a [androidx.navigation.NavHostController] owns the back stack and ViewModels are
 * scoped to each `NavBackStackEntry` automatically (no decorators needed). Navigation stays out of
 * the screens: each destination collects its ViewModel's one-shot effects and drives the controller.
 */
@Composable
fun MviNavHost(
    getImagesUseCase: GetImagesUseCase,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = GalleryRoute,
        modifier = modifier,
    ) {
        composable<GalleryRoute> {
            val viewModel: GalleryViewModel = viewModel { GalleryViewModel(getImagesUseCase) }
            val state by viewModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is GalleryEffect.NavigateToDetail ->
                            navController.navigate(DetailRoute(effect.imageId, effect.imageUrl))
                    }
                }
            }
            GalleryScreen(state = state, onIntent = viewModel::onIntent)
        }
        composable<DetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DetailRoute>()
            val viewModel: DetailViewModel = viewModel { DetailViewModel(route.imageId, route.imageUrl) }
            val state by viewModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(viewModel) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        DetailEffect.NavigateBack -> navController.popBackStack()
                    }
                }
            }
            DetailScreen(state = state, onIntent = viewModel::onIntent)
        }
    }
}
