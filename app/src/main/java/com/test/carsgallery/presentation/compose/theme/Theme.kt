package com.test.carsgallery.presentation.compose.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CarsGalleryTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context)
    } else {
        lightColorScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

// region Previews

@Preview(showBackground = true, name = "CarsGalleryTheme")
@Composable
private fun CarsGalleryThemePreview() {
    CarsGalleryTheme {
        Surface {
            Text(
                text = "Cars Gallery",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

// endregion
