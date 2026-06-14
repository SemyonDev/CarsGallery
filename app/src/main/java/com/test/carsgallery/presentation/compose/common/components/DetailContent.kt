package com.test.carsgallery.presentation.compose.common.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.test.carsgallery.R

/**
 * Shared, stateless detail UI used by both Compose implementations.
 *
 * The caller maps its own UI-state onto these plain parameters, so the visual layout and the
 * loader integration are defined once. [isLoading] drives the progress overlay; the loader
 * callbacks are forwarded back to the caller's state holder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(
    imageId: String,
    imageUrl: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onImageLoading: () -> Unit,
    onImageSuccess: () -> Unit,
    onImageError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.image_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                NetworkImage(
                    url = imageUrl,
                    contentDescription = stringResource(R.string.image_description),
                    scaleType = ImageView.ScaleType.FIT_CENTER,
                    modifier = Modifier.fillMaxSize(),
                    onLoading = onImageLoading,
                    onSuccess = onImageSuccess,
                    onError = onImageError,
                )
                if (isLoading) {
                    CircularProgressIndicator()
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.image_id_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = imageId,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
