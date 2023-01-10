package com.example.photogallery

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.photogallery.api.GalleryItem
import com.example.photogallery.databinding.ListItemGalleryBinding


class PhotoViewHolder(
    private val binding: ListItemGalleryBinding
) : RecyclerView.ViewHolder(binding.root){

    fun bind(item: GalleryItem, onItemClicked: (Uri) -> Unit){
        binding.photoView.load(item.url)
        binding.root.setOnClickListener { onItemClicked(item.photoPageUri) }
    }




}

class PhotoListAdapter(
    private val onItemClicked: (Uri) -> Unit
) : PagingDataAdapter<GalleryItem, PhotoViewHolder>(PhotoDiffCallback())
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemGalleryBinding.inflate(inflater, parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        getItem(position)?.let {  holder.bind(it, onItemClicked) }
    }

}

class PhotoDiffCallback : DiffUtil.ItemCallback<GalleryItem>() {
    override fun areItemsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
        return oldItem == newItem
    }
}