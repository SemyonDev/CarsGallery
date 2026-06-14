package com.test.carsgallery.presentation.compose.mvi.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.test.carsgallery.R
import com.test.carsgallery.presentation.compose.common.components.ErrorState
import com.test.carsgallery.presentation.compose.common.components.GalleryGrid
import com.test.carsgallery.presentation.compose.common.components.LoadingState
import com.test.carsgallery.presentation.compose.common.previewImageItems
import com.test.carsgallery.presentation.compose.common.theme.CarsGalleryTheme

/**
 * MVI gallery screen. Renders [state] and emits user actions through the single [onIntent] funnel —
 * it never mutates state or navigates directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    state: GalleryState,
    onIntent: (GalleryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { onIntent(GalleryIntent.ToggleGrid) }) {
                        if (state.isGrid) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = stringResource(R.string.switch_to_list),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.GridView,
                                contentDescription = stringResource(R.string.switch_to_grid),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val content = state.content) {
                is GalleryState.Content.Loading -> LoadingState()
                is GalleryState.Content.Error -> ErrorState(
                    message = stringResource(content.messageResId),
                    onRetry = { onIntent(GalleryIntent.Retry) },
                )
                is GalleryState.Content.Success -> GalleryGrid(
                    images = content.images,
                    isGrid = state.isGrid,
                    onImageClick = { item -> onIntent(GalleryIntent.ImageClicked(item)) },
                )
            }
        }
    }
}

@Preview(name = "Gallery · Success (grid)", showBackground = true)
@Composable
private fun GalleryScreenSuccessPreview() {
    CarsGalleryTheme {
        GalleryScreen(
            state = GalleryState(
                content = GalleryState.Content.Success(previewImageItems),
                isGrid = true,
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Gallery · Loading", showBackground = true)
@Composable
private fun GalleryScreenLoadingPreview() {
    CarsGalleryTheme {
        GalleryScreen(
            state = GalleryState(content = GalleryState.Content.Loading),
            onIntent = {},
        )
    }
}

@Preview(name = "Gallery · Error", showBackground = true)
@Composable
private fun GalleryScreenErrorPreview() {
    CarsGalleryTheme {
        GalleryScreen(
            state = GalleryState(content = GalleryState.Content.Error(R.string.error_network)),
            onIntent = {},
        )
    }
}
