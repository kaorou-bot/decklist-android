package com.mtgo.decklistmanager.ui.decklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mtgo.decklistmanager.databinding.ItemCardBinding
import com.mtgo.decklistmanager.domain.model.Card
import com.mtgo.decklistmanager.util.ManaSymbolRenderer
import com.mtgo.decklistmanager.util.AppLogger

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
                // 对于 Split 牌（名称包含 //），如果 manaCost 不包含 //，说明是旧数据
                // 这种情况下，尝试从名称推断（这是一个临时解决方案）
                val manaDisplay = if (card.manaCost.isNullOrEmpty()) {
                    ""  // 土地卡没有法术力值
                } else {
                    var manaCost = card.manaCost

                    // 记录所有包含 // 的卡牌（包括 split 卡）
                    if (card.cardName?.contains(" // ") == true || card.cardNameZh?.contains(" // ") == true || manaCost.contains(" // ")) {
                        AppLogger.d("CardAdapter", "Split card - name: ${card.cardNameZh}, manaCost: $manaCost")
                    }

                    ManaSymbolRenderer.renderManaCost(manaCost, btnCardName.context)
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
