package com.test.carsgallery.presentation.compose.mvvm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.test.carsgallery.domain.usecase.GetImagesUseCase
import com.test.carsgallery.presentation.compose.common.theme.CarsGalleryTheme
import com.test.carsgallery.presentation.compose.mvvm.navigation.MvvmNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Host for the compose_mvvm implementation: 100% Compose UI driven by Navigation 3 and MVVM
 * ViewModels.
 *
 * The use case is field-injected by Hilt and handed to [MvvmNavHost], which builds the per-screen
 * ViewModels. This keeps the data/domain layers shared with the View-system and MVI flows.
 */
@AndroidEntryPoint
class ComposeMvvmActivity : ComponentActivity() {

    @Inject
    lateinit var getImagesUseCase: GetImagesUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarsGalleryTheme {
                MvvmNavHost(getImagesUseCase = getImagesUseCase)
            }
        }
    }
}
