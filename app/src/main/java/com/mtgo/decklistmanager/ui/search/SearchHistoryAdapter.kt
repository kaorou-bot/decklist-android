package com.mtgo.decklistmanager.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mtgo.decklistmanager.databinding.ItemSearchHistoryBinding

/**
 * 搜索历史适配器
 */
class SearchHistoryAdapter(
    private val onItemClick: (String) -> Unit,
    private val onDeleteClick: (Long) -> Unit
) : ListAdapter<com.mtgo.decklistmanager.domain.model.SearchHistory, SearchHistoryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemSearchHistoryBinding,
        private val onItemClick: (String) -> Unit,
        private val onDeleteClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())

        fun bind(history: com.mtgo.decklistmanager.domain.model.SearchHistory) {
            binding.textViewQuery.text = history.query
            binding.textViewTime.text = dateFormat.format(java.util.Date(history.searchTime))
            binding.textViewCount.text = "${history.resultCount} 个结果"

            binding.root.setOnClickListener {
                onItemClick(history.query)
            }

            binding.buttonDelete.setOnClickListener {
                onDeleteClick(history.id)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<com.mtgo.decklistmanager.domain.model.SearchHistory>() {
        override fun areItemsTheSame(
            oldItem: com.mtgo.decklistmanager.domain.model.SearchHistory,
            newItem: com.mtgo.decklistmanager.domain.model.SearchHistory
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: com.mtgo.decklistmanager.domain.model.SearchHistory,
            newItem: com.mtgo.decklistmanager.domain.model.SearchHistory
        ): Boolean {
            return oldItem == newItem
        }
    }
}
