package com.test.carsgallery.presentation.compose.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.test.carsgallery.domain.model.ImageItem

private val GridSpacing = 8.dp
private const val GridColumns = 2

/**
 * Renders the loaded image list either as a 2-column grid or a single-column list.
 *
 * Shared by both the compose_mvvm and compose_mvi gallery screens — the loader integration,
 * per-item progress overlay, and click handling are identical regardless of state-management style.
 */
@Composable
fun GalleryGrid(
    images: List<ImageItem>,
    isGrid: Boolean,
    onImageClick: (ImageItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isGrid) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(GridColumns),
            modifier = modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(GridSpacing),
            horizontalArrangement = Arrangement.spacedBy(GridSpacing),
            verticalArrangement = Arrangement.spacedBy(GridSpacing),
        ) {
            items(items = images, key = { it.id }) { item ->
                GalleryImageCard(
                    item = item,
                    onClick = { onImageClick(item) },
                    modifier = Modifier.aspectRatio(1f),
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(GridSpacing),
            verticalArrangement = Arrangement.spacedBy(GridSpacing),
        ) {
            items(items = images, key = { it.id }) { item ->
                GalleryImageRow(item = item, onClick = { onImageClick(item) })
            }
        }
    }
}

@Composable
private fun GalleryImageCard(
    item: ImageItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Box(modifier = Modifier.fillMaxSize()) {
            LoadingNetworkImage(
                url = item.imageUrl,
                contentDescription = item.id,
                modifier = Modifier.fillMaxSize(),
            )
            IdBadge(
                id = item.id,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
            )
        }
    }
}

@Composable
private fun GalleryImageRow(
    item: ImageItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LoadingNetworkImage(
                url = item.imageUrl,
                contentDescription = item.id,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
            )
            Text(
                text = item.id,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/** [NetworkImage] with a centered spinner shown until the load completes (success or error). */
@Composable
private fun LoadingNetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    // In previews the loader never reports back, so start "loaded" to show the placeholder cleanly.
    val startLoading = !LocalInspectionMode.current
    var loading by remember(url) { mutableStateOf(startLoading) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        NetworkImage(
            url = url,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            onLoading = { loading = true },
            onSuccess = { loading = false },
            onError = { loading = false },
        )
        if (loading) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun IdBadge(id: String, modifier: Modifier = Modifier) {
    Text(
        text = id,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
