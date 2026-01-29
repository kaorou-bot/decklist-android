package com.mtgo.decklistmanager.ui.decklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ItemEventBinding
import com.mtgo.decklistmanager.domain.model.Event

/**
 * 日期分组赛事列表适配器
 * 支持两种类型：日期标题 和 赛事项
 */
class EventSectionAdapter(
    private val onItemClick: (Event) -> Unit
) : ListAdapter<EventListItem, RecyclerView.ViewHolder>(EventDiffCallback()) {

    /**
     * 获取指定位置的 EventListItem（用于滑动删除）
     */
    fun getItemAtPosition(position: Int): EventListItem {
        return getItem(position)
    }

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_EVENT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EventListItem.DateHeader -> TYPE_DATE_HEADER
            is EventListItem.EventItem -> TYPE_EVENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_event_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            TYPE_EVENT -> {
                val binding = ItemEventBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                EventViewHolder(binding, onItemClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is EventListItem.DateHeader -> {
                (holder as DateHeaderViewHolder).bind(item)
            }
            is EventListItem.EventItem -> {
                (holder as EventViewHolder).bind(item.event)
            }
        }
    }

    class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDateHeader: MaterialTextView = itemView.findViewById(R.id.tvDateHeader)

        fun bind(header: EventListItem.DateHeader) {
            tvDateHeader.text = header.date
        }
    }

    class EventViewHolder(
        private val binding: ItemEventBinding,
        private val onItemClick: (Event) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {
                tvEventName.text = event.eventName

                // Setup format chip
                formatChip.text = event.format
                formatChip.isClickable = false

                tvDate.text = event.date
                tvDeckCount.text = "${event.deckCount} Decks"

                root.setOnClickListener {
                    onItemClick(event)
                }
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<EventListItem>() {
        override fun areItemsTheSame(oldItem: EventListItem, newItem: EventListItem): Boolean {
            return when {
                oldItem is EventListItem.DateHeader && newItem is EventListItem.DateHeader ->
                    oldItem.date == newItem.date
                oldItem is EventListItem.EventItem && newItem is EventListItem.EventItem ->
                    oldItem.event.id == newItem.event.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: EventListItem, newItem: EventListItem): Boolean {
            return oldItem == newItem
        }
    }
}
