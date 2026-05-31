package com.test.carsgallery.presentation.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun CarsGalleryTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    MaterialTheme(
        colorScheme = dynamicLightColorScheme(context),
        content = content,
    )
}
