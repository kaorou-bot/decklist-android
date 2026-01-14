package com.mtgo.decklistmanager.ui.carddetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ItemLegalityBinding

/**
 * Legalities Adapter - 合法性列表适配器
 */
class LegalitiesAdapter : ListAdapter<CardDetailActivity.LegalityItem, LegalitiesAdapter.LegalityViewHolder>(LegalityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LegalityViewHolder {
        val binding = ItemLegalityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LegalityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LegalityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LegalityViewHolder(
        private val binding: ItemLegalityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CardDetailActivity.LegalityItem) {
            binding.apply {
                tvFormat.text = item.format
                tvLegality.text = item.legality

                // Set color based on legality
                val colorRes = when (item.legality.lowercase()) {
                    "legal" -> R.color.success
                    "restricted" -> R.color.warning
                    "banned" -> R.color.error
                    else -> R.color.text_secondary
                }
                tvLegality.setTextColor(root.context.getColor(colorRes))
            }
        }
    }

    class LegalityDiffCallback : DiffUtil.ItemCallback<CardDetailActivity.LegalityItem>() {
        override fun areItemsTheSame(oldItem: CardDetailActivity.LegalityItem, newItem: CardDetailActivity.LegalityItem): Boolean {
            return oldItem.format == newItem.format
        }

        override fun areContentsTheSame(oldItem: CardDetailActivity.LegalityItem, newItem: CardDetailActivity.LegalityItem): Boolean {
            return oldItem == newItem
        }
    }
}
