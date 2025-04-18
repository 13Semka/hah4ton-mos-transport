package com.example.transportsirius.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.transportsirius.R
import com.example.transportsirius.domain.entity.RouteOption

class RouteAdapter(
    private val onRouteClick: (RouteOption) -> Unit
) : ListAdapter<RouteOption, RouteAdapter.ViewHolder>(DiffCallback()) {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position, position == selectedPosition)
        
        holder.itemView.setOnClickListener {
            val wasSelected = selectedPosition == position
            
            // Обновляем выделенную позицию
            val oldSelectedPosition = selectedPosition
            selectedPosition = if (wasSelected) RecyclerView.NO_POSITION else position
            
            // Перерисовываем старый и новый выделенные элементы
            if (oldSelectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldSelectedPosition)
            }
            if (selectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(selectedPosition)
            }
            
            // Вызываем коллбэк только если была выбрана новая позиция
            if (!wasSelected) {
                onRouteClick(item)
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val routeNameTextView: TextView = itemView.findViewById(R.id.routeNameTextView)
        private val transportTypeTextView: TextView = itemView.findViewById(R.id.transportTypeTextView)
        private val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)
        private val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        private val routeCardView: CardView = itemView.findViewById(R.id.routeCardView)

        fun bind(item: RouteOption, position: Int, isSelected: Boolean) {
            routeNameTextView.text = "Маршрут ${position + 1}"
            transportTypeTextView.text = "Транспорт: ${item.transportType}"
            
            // Форматирование времени
            val hours = item.duration / 3600
            val minutes = (item.duration % 3600) / 60
            val timeString = when {
                hours > 0 && minutes > 0 -> "$hours ч $minutes мин"
                hours > 0 -> "$hours ч"
                else -> "$minutes мин"
            }
            durationTextView.text = "Время в пути: $timeString"
            
            // Форматирование стоимости
            priceTextView.text = "Стоимость: ${item.price.toInt()} руб."
            
            descriptionTextView.text = item.description
            
            // Установка фона для выделенного элемента
            routeCardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    itemView.context, 
                    if (isSelected) R.color.selected_route_background else R.color.white
                )
            )
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RouteOption>() {
        override fun areItemsTheSame(oldItem: RouteOption, newItem: RouteOption): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RouteOption, newItem: RouteOption): Boolean {
            return oldItem == newItem
        }
    }
} 