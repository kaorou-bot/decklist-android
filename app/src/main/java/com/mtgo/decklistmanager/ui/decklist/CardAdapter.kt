package com.mtgo.decklistmanager.ui.decklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mtgo.decklistmanager.databinding.ItemCardBinding
import com.mtgo.decklistmanager.domain.model.Card
import com.mtgo.decklistmanager.util.ManaSymbolRenderer

/**
 * Card Adapter - 卡牌列表适配器
 */
class CardAdapter(
    private val onCardClick: (String) -> Unit
) : ListAdapter<Card, CardAdapter.CardViewHolder>(CardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CardViewHolder(binding, onCardClick)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CardViewHolder(
        private val binding: ItemCardBinding,
        private val onCardClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: Card) {
            binding.apply {
                tvQuantity.text = card.quantity.toString()
                btnCardName.text = card.cardName
                tvManaCost.text = ManaSymbolRenderer.renderManaCost(card.manaCost, btnCardName.context)

                btnCardName.setOnClickListener {
                    onCardClick(card.cardName)
                }
            }
        }
    }

    class CardDiffCallback : DiffUtil.ItemCallback<Card>() {
        override fun areItemsTheSame(oldItem: Card, newItem: Card): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Card, newItem: Card): Boolean {
            return oldItem == newItem
        }
    }
}
