package com.mtgo.decklistmanager.ui.decklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.mtgo.decklistmanager.databinding.ItemEventBinding
import com.mtgo.decklistmanager.domain.model.Event

/**
 * Event Adapter - 赛事列表适配器
 */
class EventAdapter(
    private val onItemClick: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(
        private val binding: ItemEventBinding,
        private val onItemClick: (Event) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {
                tvEventName.text = event.eventName
                tvFormat.text = "Format: ${event.format}"
                tvDate.text = event.date
                tvEventType.text = event.eventType ?: "N/A"
                tvSource.text = "Source: ${event.source}"
                tvDeckCount.text = "Decks: ${event.deckCount}"

                root.setOnClickListener {
                    onItemClick(event)
                }
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}
