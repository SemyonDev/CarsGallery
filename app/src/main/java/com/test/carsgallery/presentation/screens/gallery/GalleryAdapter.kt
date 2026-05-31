package com.test.carsgallery.presentation.screens.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.test.carsgallery.R
import com.test.carsgallery.databinding.ItemGalleryImageBinding
import com.test.carsgallery.domain.model.ImageItem
import com.zipoapps.imageloader.ImageLoader

class GalleryAdapter(
    private val onItemClick: (ImageItem) -> Unit,
) : ListAdapter<ImageItem, GalleryAdapter.GalleryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = ItemGalleryImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GalleryViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: GalleryViewHolder) {
        super.onViewRecycled(holder)
        // Cancel in-flight request AND hide the spinner. Without hiding the spinner here the
        // progressIndicator would stay visible permanently on fast scrolling, because neither
        // onSuccess nor onError fires when a request is cancelled by recycling.
        ImageLoader.cancel(holder.imageView)
        holder.clearLoadingState()
    }

    class GalleryViewHolder(
        private val binding: ItemGalleryImageBinding,
        private val onItemClick: (ImageItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        val imageView get() = binding.imageView

        fun bind(item: ImageItem) {
            binding.textViewId.text = item.id
            binding.root.setOnClickListener { onItemClick(item) }
            binding.progressIndicator.isVisible = true

            ImageLoader.with(binding.root.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .onSuccess { binding.progressIndicator.isVisible = false }
                .onError { binding.progressIndicator.isVisible = false }
                .into(binding.imageView)
        }

        /** Called from [onViewRecycled] to reset transient view state before re-use. */
        fun clearLoadingState() {
            binding.progressIndicator.isVisible = false
        }
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean =
            oldItem == newItem
    }
}