package com.test.carsgallery.presentation.compose.screens.imagedetails

import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.test.carsgallery.R
import com.test.carsgallery.presentation.compose.components.ImageLoaderImage
import com.test.carsgallery.presentation.compose.theme.CarsGalleryTheme
import com.test.carsgallery.presentation.screens.imagedetails.ImageDetailViewModel
import com.test.carsgallery.presentation.screens.imagedetails.ImageLoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailScreen(
    viewModel: ImageDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onLoadStarted() }

    ImageDetailContent(
        imageId = viewModel.imageId,
        imageUrl = viewModel.imageUrl,
        loadState = loadState,
        onImageLoaded = { viewModel.onImageLoaded() },
        onImageError = { viewModel.onImageError() },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailContent(
    imageId: String,
    imageUrl: String,
    loadState: ImageLoadState,
    onImageLoaded: () -> Unit = {},
    onImageError: () -> Unit = {},
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.image_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ImageLoaderImage(
                    url = imageUrl,
                    scaleType = ImageView.ScaleType.FIT_CENTER,
                    onSuccess = onImageLoaded,
                    onError = onImageError,
                    modifier = Modifier.fillMaxSize(),
                )
                if (loadState is ImageLoadState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.image_id_label),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = imageId,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// region Previews

@Preview(showBackground = true, name = "Image Detail – Loading")
@Composable
private fun ImageDetailLoadingPreview() {
    CarsGalleryTheme {
        ImageDetailContent(
            imageId = "car_001",
            imageUrl = "https://example.com/car1.jpg",
            loadState = ImageLoadState.Loading,
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Image Detail – Loaded")
@Composable
private fun ImageDetailLoadedPreview() {
    CarsGalleryTheme {
        ImageDetailContent(
            imageId = "car_001",
            imageUrl = "https://example.com/car1.jpg",
            loadState = ImageLoadState.Success,
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Image Detail – Error")
@Composable
private fun ImageDetailErrorPreview() {
    CarsGalleryTheme {
        ImageDetailContent(
            imageId = "car_001",
            imageUrl = "https://example.com/car1.jpg",
            loadState = ImageLoadState.Error,
            onBack = {},
        )
    }
}

// endregion
