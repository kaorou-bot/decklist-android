package com.mtgo.decklistmanager.ui.decklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.mtgo.decklistmanager.databinding.ItemDecklistTableBinding
import com.mtgo.decklistmanager.domain.model.Decklist

/**
 * DecklistTableAdapter - Table-style adapter for displaying decklists
 */
class DecklistTableAdapter(
    private val onItemClick: (Decklist) -> Unit
) : ListAdapter<Decklist, DecklistTableAdapter.DecklistTableViewHolder>(DecklistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DecklistTableViewHolder {
        val binding = ItemDecklistTableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DecklistTableViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: DecklistTableViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DecklistTableViewHolder(
        private val binding: ItemDecklistTableBinding,
        private val onItemClick: (Decklist) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(decklist: Decklist) {
            // 优先显示套牌名称，如果没有则显示玩家名称
            binding.tvPlayerName.text = decklist.deckName ?: decklist.playerName ?: "Unknown Deck"
            binding.tvRecord.text = decklist.record ?: "N/A"

            // Show/hide loading indicator
            binding.progressBar.visibility = if (decklist.isLoading) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(decklist)
            }
        }
    }

    private class DecklistDiffCallback : DiffUtil.ItemCallback<Decklist>() {
        override fun areItemsTheSame(oldItem: Decklist, newItem: Decklist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Decklist, newItem: Decklist): Boolean {
            return oldItem == newItem
        }
    }
}
