package com.mtgo.decklistmanager.ui.comparison.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mtgo.decklistmanager.databinding.ItemCardComparisonBinding
import com.mtgo.decklistmanager.ui.comparison.model.CardComparison

/**
 * CardComparisonAdapter - 卡牌对比适配器
 */
class CardComparisonAdapter(
    private val deckName1: String,
    private val deckName2: String
) : ListAdapter<CardComparison, CardComparisonAdapter.ViewHolder>(CardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCardComparisonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), deckName1, deckName2)
    }

    class ViewHolder(private val binding: ItemCardComparisonBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comparison: CardComparison, deckName1: String, deckName2: String) {
            binding.apply {
                tvCardName.text = comparison.cardName
                tvCount1.text = comparison.count1.toString()
                tvCount2.text = comparison.count2.toString()
                tvDeck1.text = deckName1
                tvDeck2.text = deckName2

                // 根据对比结果设置背景色
                when {
                    comparison.onlyInDeck1 -> {
                        root.setBackgroundColor(
                            ContextCompat.getColor(root.context, android.R.color.holo_red_light)
                        )
                    }
                    comparison.onlyInDeck2 -> {
                        root.setBackgroundColor(
                            ContextCompat.getColor(root.context, android.R.color.holo_blue_light)
                        )
                    }
                    comparison.differentCount -> {
                        root.setBackgroundColor(
                            ContextCompat.getColor(root.context, android.R.color.holo_orange_light)
                        )
                    }
                    else -> {
                        root.setBackgroundColor(
                            ContextCompat.getColor(root.context, android.R.color.transparent)
                        )
                    }
                }
            }
        }
    }

    class CardDiffCallback : DiffUtil.ItemCallback<CardComparison>() {
        override fun areItemsTheSame(oldItem: CardComparison, newItem: CardComparison): Boolean {
            return oldItem.cardName == newItem.cardName
        }

        override fun areContentsTheSame(oldItem: CardComparison, newItem: CardComparison): Boolean {
            return oldItem == newItem
        }
    }
}
