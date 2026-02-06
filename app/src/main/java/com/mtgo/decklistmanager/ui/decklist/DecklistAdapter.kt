package com.mtgo.decklistmanager.ui.decklist

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ItemDecklistBinding
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.ui.decklist.MainViewModel
import kotlinx.coroutines.launch

/**
 * Decklist Adapter - 牌组列表适配器
 * v4.2.1: 支持左滑收藏功能和显示收藏状态
 */
class DecklistAdapter(
    private val onItemClick: (Decklist) -> Unit,
    private val viewModel: MainViewModel? = null,
    private val coroutineScope: LifecycleCoroutineScope? = null
) : ListAdapter<Decklist, DecklistAdapter.DecklistViewHolder>(DecklistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DecklistViewHolder {
        val binding = ItemDecklistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DecklistViewHolder(binding, onItemClick, viewModel, coroutineScope)
    }

    override fun onBindViewHolder(holder: DecklistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * 获取指定位置的套牌（用于滑动操作）
     */
    fun getItemAtPosition(position: Int): Decklist {
        return getItem(position)
    }

    class DecklistViewHolder(
        private val binding: ItemDecklistBinding,
        private val onItemClick: (Decklist) -> Unit,
        private val viewModel: MainViewModel? = null,
        private val coroutineScope: LifecycleCoroutineScope? = null
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(decklist: Decklist) {
            binding.apply {
                // 优先显示套牌名称，如果没有则显示赛事名称
                tvEventName.text = decklist.deckName ?: decklist.eventName
                tvFormat.text = "Format: ${decklist.format}"
                tvDate.text = decklist.date
                tvPlayer.text = decklist.playerName?.let { "Player: $it" } ?: "Player: N/A"
                tvRecord.text = decklist.record ?: "N/A"

                // 显示收藏状态
                coroutineScope?.launch {
                    val isFavorite = viewModel?.isFavorite(decklist.id) ?: false
                    ivFavorite.setImageResource(
                        if (isFavorite) R.drawable.ic_favorite_filled
                        else R.drawable.ic_favorite_border
                    )
                }

                root.setOnClickListener {
                    onItemClick(decklist)
                }
            }
        }
    }

    class DecklistDiffCallback : DiffUtil.ItemCallback<Decklist>() {
        override fun areItemsTheSame(oldItem: Decklist, newItem: Decklist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Decklist, newItem: Decklist): Boolean {
            return oldItem == newItem
        }
    }
}
