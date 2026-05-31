package com.test.carsgallery.presentation.screens.gallery

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.test.carsgallery.R
import com.test.carsgallery.databinding.FragmentGalleryBinding
import com.test.carsgallery.domain.model.ImageItem
import com.test.carsgallery.presentation.screens.gallery.GalleryViewModel
import com.test.carsgallery.presentation.UiState
import com.test.carsgallery.presentation.screens.imagedetails.ImageDetailFragment
import com.zipoapps.imageloader.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GalleryViewModel by viewModels()

    private lateinit var adapter: GalleryAdapter
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = GalleryAdapter(onItemClick = ::navigateToDetail)
        gridLayoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
        linearLayoutManager = LinearLayoutManager(requireContext())
        setupRecyclerView()
        setupSwipeRefresh()
        setupToolbarMenu()
        setupRetryButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            adapter = this@GalleryFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadImages()
        }
    }

    private fun setupToolbarMenu() {
        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_toggle_layout -> {
                    viewModel.toggleLayoutMode()
                    true
                }
                R.id.action_clear_cache -> {
                    ImageLoader.invalidateAll()
                    Snackbar.make(binding.root, R.string.cache_cleared, Snackbar.LENGTH_SHORT).show()
                    viewModel.loadImages()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRetryButton() {
        binding.buttonRetry.setOnClickListener { viewModel.loadImages() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isGridMode.collect(::applyLayoutMode) }
                launch { viewModel.uiState.collect(::renderState) }
            }
        }
    }

    private fun applyLayoutMode(isGrid: Boolean) {
        val newManager = if (isGrid) gridLayoutManager else linearLayoutManager
        if (binding.recyclerView.layoutManager !== newManager) {
            binding.recyclerView.layoutManager = newManager
        }
        val menuItem = binding.toolbar.menu.findItem(R.id.action_toggle_layout) ?: return
        menuItem.setTitle(if (isGrid) R.string.switch_to_list else R.string.switch_to_grid)
        menuItem.setIcon(if (isGrid) R.drawable.ic_view_list else R.drawable.ic_view_grid)
    }

    private fun renderState(state: UiState<List<ImageItem>>) {
        when (state) {
            is UiState.Loading -> {
                val isPullRefresh = binding.swipeRefreshLayout.isRefreshing
                binding.progressBar.isVisible = !isPullRefresh
                if (!isPullRefresh) binding.recyclerView.isVisible = false
                binding.errorContainer.isVisible = false
            }
            is UiState.Success -> {
                binding.swipeRefreshLayout.isRefreshing = false
                binding.progressBar.isVisible = false
                binding.recyclerView.isVisible = true
                binding.errorContainer.isVisible = false
                adapter.submitList(state.data)
            }
            is UiState.Error -> {
                binding.swipeRefreshLayout.isRefreshing = false
                binding.progressBar.isVisible = false
                binding.recyclerView.isVisible = false
                binding.errorContainer.isVisible = true
                binding.viewError.text = getString(state.messageResId)
            }
        }
    }

    private fun navigateToDetail(item: ImageItem) {
        findNavController().navigate(
            R.id.action_gallery_to_detail,
            ImageDetailFragment.Args(imageId = item.id, imageUrl = item.imageUrl).toBundle(),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        binding.recyclerView.layoutManager = null
        _binding = null
    }

    private companion object {
        private const val GRID_SPAN_COUNT = 2
    }
}