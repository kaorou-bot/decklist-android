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
                // v4.0.0: 优先显示中文名，回退到英文名
                btnCardName.text = card.cardNameZh ?: card.cardName

                // v4.0.0: 法术力值显示
                // 土地等没有法术力值的卡牌显示空字符串是正常的
                val manaDisplay = if (card.manaCost.isNullOrEmpty()) {
                    ""  // 土地卡没有法术力值
                } else {
                    ManaSymbolRenderer.renderManaCost(card.manaCost, btnCardName.context)
                }
                tvManaCost.text = manaDisplay

                btnCardName.setOnClickListener {
                    // 点击时使用原始卡名（英文）进行查询
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
