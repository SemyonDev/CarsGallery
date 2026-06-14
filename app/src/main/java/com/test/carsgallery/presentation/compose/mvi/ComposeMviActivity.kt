package com.test.carsgallery.presentation.compose.mvi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.test.carsgallery.domain.usecase.GetImagesUseCase
import com.test.carsgallery.presentation.compose.common.theme.CarsGalleryTheme
import com.test.carsgallery.presentation.compose.mvi.navigation.MviNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Host for the compose_mvi implementation: 100% Compose UI driven by Navigation 3 and an
 * intent/state/effect (MVI) loop. Shares the domain/data layers with the other two flows.
 */
@AndroidEntryPoint
class ComposeMviActivity : ComponentActivity() {

    @Inject
    lateinit var getImagesUseCase: GetImagesUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarsGalleryTheme {
                MviNavHost(getImagesUseCase = getImagesUseCase)
            }
        }
    }
}
