package com.example.transportsirius.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.transportsirius.R
import com.example.transportsirius.domain.entity.GeocoderResult

class SearchResultsAdapter(
    private val onItemClick: (GeocoderResult) -> Unit
) : ListAdapter<GeocoderResult, SearchResultsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.iconImageView)
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val subtitleTextView: TextView = itemView.findViewById(R.id.subtitleTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: GeocoderResult) {
            titleTextView.text = item.name
            subtitleTextView.text = item.formattedAddress
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<GeocoderResult>() {
        override fun areItemsTheSame(oldItem: GeocoderResult, newItem: GeocoderResult): Boolean {
            return oldItem.latLng == newItem.latLng
        }

        override fun areContentsTheSame(oldItem: GeocoderResult, newItem: GeocoderResult): Boolean {
            return oldItem == newItem
        }
    }
} 