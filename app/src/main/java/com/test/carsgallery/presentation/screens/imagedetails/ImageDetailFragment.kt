package com.test.carsgallery.presentation.screens.imagedetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.test.carsgallery.R
import com.test.carsgallery.databinding.FragmentImageDetailBinding
import com.zipoapps.imageloader.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Full-screen detail view. Intentionally thin — all state lives in [ImageDetailViewModel].
 * [ImageLoader] is called here because it binds to an [android.widget.ImageView] within
 * the view's lifetime, which is a View-layer concern.
 */
@AndroidEntryPoint
class ImageDetailFragment : Fragment() {

    private var _binding: FragmentImageDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ImageDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentImageDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.textViewId.text = viewModel.imageId

        // Reset to Loading before each request so the spinner shows correctly after rotation.
        viewModel.onLoadStarted()

        ImageLoader.with(requireContext())
            .load(viewModel.imageUrl)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_error)
            .onSuccess { viewModel.onImageLoaded() }
            .onError { viewModel.onImageError() }
            .into(binding.imageViewDetail)

        observeLoadState()
    }

    private fun observeLoadState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadState.collect(::renderLoadState)
            }
        }
    }

    private fun renderLoadState(state: ImageLoadState) {
        binding.progressIndicator.isVisible = state is ImageLoadState.Loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Builds the [Bundle] for navigation to this fragment.
     * Keys are sourced from [ImageDetailViewModel] constants to match nav_graph.xml.
     */
    data class Args(val imageId: String, val imageUrl: String) {
        fun toBundle(): Bundle = Bundle().apply {
            putString(ImageDetailViewModel.ARG_IMAGE_ID, imageId)
            putString(ImageDetailViewModel.ARG_IMAGE_URL, imageUrl)
        }
    }
}
