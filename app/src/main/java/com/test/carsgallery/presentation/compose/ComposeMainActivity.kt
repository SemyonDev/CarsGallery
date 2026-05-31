package com.test.carsgallery.presentation.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.test.carsgallery.presentation.compose.navigation.ComposeNavGraph
import com.test.carsgallery.presentation.compose.theme.CarsGalleryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComposeMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarsGalleryTheme {
                ComposeNavGraph()
            }
        }
    }
}
