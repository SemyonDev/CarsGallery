package com.test.carsgallery.presentation.compose.screens.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.test.carsgallery.R
import com.test.carsgallery.domain.model.ImageItem
import com.test.carsgallery.presentation.UiState
import com.test.carsgallery.presentation.compose.components.ImageLoaderImage
import com.test.carsgallery.presentation.compose.theme.CarsGalleryTheme
import com.test.carsgallery.presentation.screens.gallery.GalleryViewModel
import com.zipoapps.imageloader.ImageLoader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    onNavigateToDetail: (imageId: String, imageUrl: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isGridMode by viewModel.isGridMode.collectAsStateWithLifecycle()

    GalleryScreenContent(
        uiState = uiState,
        isGridMode = isGridMode,
        onToggleLayout = { viewModel.toggleLayoutMode() },
        onClearCache = {
            ImageLoader.invalidateAll()
            viewModel.loadImages()
        },
        onRetry = { viewModel.loadImages() },
        onRefresh = { viewModel.loadImages() },
        onNavigateToDetail = onNavigateToDetail,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreenContent(
    uiState: UiState<List<ImageItem>>,
    isGridMode: Boolean,
    onToggleLayout: () -> Unit,
    onClearCache: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToDetail: (imageId: String, imageUrl: String) -> Unit,
) {
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState !is UiState.Loading) isRefreshing = false
    }

    var menuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cacheClearedMessage = stringResource(R.string.cache_cleared)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isGridMode) stringResource(R.string.switch_to_list)
                                    else stringResource(R.string.switch_to_grid)
                                )
                            },
                            onClick = {
                                onToggleLayout()
                                menuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.clear_cache)) },
                            onClick = {
                                onClearCache()
                                menuExpanded = false
                                scope.launch { snackbarHostState.showSnackbar(cacheClearedMessage) }
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                onRefresh()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(state.messageResId),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                        Button(onClick = onRetry) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }

                is UiState.Success -> {
                    if (isGridMode) {
                        GalleryGrid(
                            items = state.data,
                            onItemClick = { onNavigateToDetail(it.id, it.imageUrl) },
                        )
                    } else {
                        GalleryList(
                            items = state.data,
                            onItemClick = { onNavigateToDetail(it.id, it.imageUrl) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryGrid(
    items: List<ImageItem>,
    onItemClick: (ImageItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.id }) { item ->
            GalleryItem(item = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun GalleryList(
    items: List<ImageItem>,
    onItemClick: (ImageItem) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.id }) { item ->
            GalleryItem(
                item = item,
                onClick = { onItemClick(item) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GalleryItem(
    item: ImageItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember { mutableStateOf(true) }

    Card(modifier = modifier.clickable(onClick = onClick)) {
        Box {
            ImageLoaderImage(
                url = item.imageUrl,
                onSuccess = { isLoading = false },
                onError = { isLoading = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            Text(
                text = item.id,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp),
            )
        }
    }
}

// region Previews

private val previewItems = listOf(
    ImageItem(id = "car_001", imageUrl = "https://example.com/car1.jpg"),
    ImageItem(id = "car_002", imageUrl = "https://example.com/car2.jpg"),
    ImageItem(id = "car_003", imageUrl = "https://example.com/car3.jpg"),
    ImageItem(id = "car_004", imageUrl = "https://example.com/car4.jpg"),
)

@Preview(showBackground = true, name = "Gallery – Loading")
@Composable
private fun GalleryScreenLoadingPreview() {
    CarsGalleryTheme {
        GalleryScreenContent(
            uiState = UiState.Loading,
            isGridMode = true,
            onToggleLayout = {},
            onClearCache = {},
            onRetry = {},
            onRefresh = {},
            onNavigateToDetail = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Gallery – Error")
@Composable
private fun GalleryScreenErrorPreview() {
    CarsGalleryTheme {
        GalleryScreenContent(
            uiState = UiState.Error(messageResId = R.string.error_loading),
            isGridMode = true,
            onToggleLayout = {},
            onClearCache = {},
            onRetry = {},
            onRefresh = {},
            onNavigateToDetail = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Gallery – Grid")
@Composable
private fun GalleryScreenGridPreview() {
    CarsGalleryTheme {
        GalleryScreenContent(
            uiState = UiState.Success(data = previewItems),
            isGridMode = true,
            onToggleLayout = {},
            onClearCache = {},
            onRetry = {},
            onRefresh = {},
            onNavigateToDetail = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Gallery – List")
@Composable
private fun GalleryScreenListPreview() {
    CarsGalleryTheme {
        GalleryScreenContent(
            uiState = UiState.Success(data = previewItems),
            isGridMode = false,
            onToggleLayout = {},
            onClearCache = {},
            onRetry = {},
            onRefresh = {},
            onNavigateToDetail = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "GalleryItem")
@Composable
private fun GalleryItemPreview() {
    CarsGalleryTheme {
        GalleryItem(
            item = ImageItem(id = "car_001", imageUrl = "https://example.com/car1.jpg"),
            onClick = {},
        )
    }
}

// endregion
