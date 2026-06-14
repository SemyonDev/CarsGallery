package com.test.carsgallery.presentation.compose.mvvm.gallery

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
import com.test.carsgallery.domain.model.ImageItem
import com.test.carsgallery.presentation.compose.common.components.ErrorState
import com.test.carsgallery.presentation.compose.common.components.GalleryGrid
import com.test.carsgallery.presentation.compose.common.components.LoadingState
import com.test.carsgallery.presentation.compose.common.previewImageItems
import com.test.carsgallery.presentation.compose.common.theme.CarsGalleryTheme

/**
 * MVVM gallery screen. Stateless: it renders [uiState] and forwards user actions as callbacks.
 * The hosting nav entry wires these callbacks to [GalleryViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    uiState: GalleryUiState,
    onToggleGrid: () -> Unit,
    onRetry: () -> Unit,
    onImageClick: (ImageItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onToggleGrid) {
                        if (uiState.isGrid) {
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
            when (val content = uiState.content) {
                is GalleryUiState.Content.Loading -> LoadingState()
                is GalleryUiState.Content.Error -> ErrorState(
                    message = stringResource(content.messageResId),
                    onRetry = onRetry,
                )
                is GalleryUiState.Content.Success -> GalleryGrid(
                    images = content.images,
                    isGrid = uiState.isGrid,
                    onImageClick = onImageClick,
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
            uiState = GalleryUiState(
                content = GalleryUiState.Content.Success(previewImageItems),
                isGrid = true,
            ),
            onToggleGrid = {},
            onRetry = {},
            onImageClick = {},
        )
    }
}

@Preview(name = "Gallery · Loading", showBackground = true)
@Composable
private fun GalleryScreenLoadingPreview() {
    CarsGalleryTheme {
        GalleryScreen(
            uiState = GalleryUiState(content = GalleryUiState.Content.Loading),
            onToggleGrid = {},
            onRetry = {},
            onImageClick = {},
        )
    }
}

@Preview(name = "Gallery · Error", showBackground = true)
@Composable
private fun GalleryScreenErrorPreview() {
    CarsGalleryTheme {
        GalleryScreen(
            uiState = GalleryUiState(content = GalleryUiState.Content.Error(R.string.error_network)),
            onToggleGrid = {},
            onRetry = {},
            onImageClick = {},
        )
    }
}
