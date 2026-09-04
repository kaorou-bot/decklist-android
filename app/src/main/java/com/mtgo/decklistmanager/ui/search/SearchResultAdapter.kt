package com.mtgo.decklistmanager.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mtgo.decklistmanager.databinding.ItemSearchResultBinding
import com.mtgo.decklistmanager.util.CardImageFallbackLoader

/**
 * 搜索结果适配器
 */
class SearchResultAdapter(
    private val onItemClick: (SearchResultItem) -> Unit,
    private val imageFallbackLoader: CardImageFallbackLoader? = null
) : ListAdapter<SearchResultItem, SearchResultAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick, imageFallbackLoader)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemSearchResultBinding,
        private val onItemClick: (SearchResultItem) -> Unit,
        private val imageFallbackLoader: CardImageFallbackLoader?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(result: SearchResultItem) {
            binding.apply {
                // 显示中文名或英文名
                textViewName.text = result.displayName ?: result.name

                // 显示类型
                textViewType.text = result.typeLine ?: result.type ?: ""

                // 显示法术力值（规范化 "no cost"）
                textViewManaCost.text =
                    com.mtgo.decklistmanager.util.ManaCosts.normalize(result.manaCost) ?: ""

                // 加载卡牌图片（主图缺失/失败时按 printings 顺序回退，最后 Scryfall）
                if (imageFallbackLoader != null) {
                    imageFallbackLoader.load(
                        imageView = imageViewCard,
                        primaryUrl = result.imageUrl,
                        cardId = result.mtgchCard?.oracleId,
                        isBack = false,
                        setCode = result.setCode,
                        collectorNumber = result.collectorNumber,
                        placeholderRes = com.google.android.material.R.drawable.mtrl_ic_cancel,
                        errorRes = com.google.android.material.R.drawable.mtrl_ic_error
                    )
                } else if (!result.imageUrl.isNullOrEmpty()) {
                    imageViewCard.visibility = android.view.View.VISIBLE
                    Glide.with(imageViewCard.context)
                        .load(result.imageUrl)
                        .placeholder(com.google.android.material.R.drawable.mtrl_ic_cancel)
                        .error(com.google.android.material.R.drawable.mtrl_ic_error)
                        .into(imageViewCard)
                } else {
                    imageViewCard.visibility = android.view.View.GONE
                }
            }

            binding.root.setOnClickListener {
                onItemClick(result)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<SearchResultItem>() {
        override fun areItemsTheSame(
            oldItem: SearchResultItem,
            newItem: SearchResultItem
        ): Boolean {
            return oldItem.cardInfoId == newItem.cardInfoId
        }

        override fun areContentsTheSame(
            oldItem: SearchResultItem,
            newItem: SearchResultItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
