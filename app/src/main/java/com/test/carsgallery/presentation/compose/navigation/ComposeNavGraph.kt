package com.test.carsgallery.presentation.compose.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.test.carsgallery.presentation.compose.screens.gallery.GalleryScreen
import com.test.carsgallery.presentation.compose.screens.imagedetails.ImageDetailScreen
import com.test.carsgallery.presentation.screens.imagedetails.ImageDetailViewModel.Companion.ARG_IMAGE_ID
import com.test.carsgallery.presentation.screens.imagedetails.ImageDetailViewModel.Companion.ARG_IMAGE_URL

private const val ROUTE_GALLERY = "gallery"
private const val ROUTE_IMAGE_DETAIL = "image_detail/{$ARG_IMAGE_ID}/{$ARG_IMAGE_URL}"

@Composable
fun ComposeNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_GALLERY) {

        composable(ROUTE_GALLERY) {
            GalleryScreen(
                onNavigateToDetail = { imageId, imageUrl ->
                    navController.navigate(
                        "image_detail/$imageId/${Uri.encode(imageUrl)}"
                    )
                }
            )
        }

        composable(
            route = ROUTE_IMAGE_DETAIL,
            arguments = listOf(
                navArgument(ARG_IMAGE_ID) { type = NavType.StringType },
                navArgument(ARG_IMAGE_URL) { type = NavType.StringType },
            ),
        ) {
            ImageDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
