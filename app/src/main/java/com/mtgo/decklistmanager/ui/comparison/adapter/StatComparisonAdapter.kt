package com.mtgo.decklistmanager.ui.comparison.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mtgo.decklistmanager.databinding.ItemStatComparisonBinding
import com.mtgo.decklistmanager.ui.comparison.model.StatComparison

/**
 * StatComparisonAdapter - 统计对比适配器
 */
class StatComparisonAdapter : ListAdapter<StatComparison, StatComparisonAdapter.ViewHolder>(StatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatComparisonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemStatComparisonBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stat: StatComparison) {
            binding.apply {
                tvStatName.text = stat.statName
                tvValue1.text = stat.value1.toString()
                tvValue2.text = stat.value2.toString()

                // 设置差异颜色
                val diff = stat.difference
                tvDifference.text = if (diff > 0) "+$diff" else diff.toString()
                tvDifference.setTextColor(
                    when {
                        diff > 0 -> root.resources.getColor(android.R.color.holo_green_dark, null)
                        diff < 0 -> root.resources.getColor(android.R.color.holo_red_dark, null)
                        else -> root.resources.getColor(android.R.color.darker_gray, null)
                    }
                )
            }
        }
    }

    class StatDiffCallback : DiffUtil.ItemCallback<StatComparison>() {
        override fun areItemsTheSame(oldItem: StatComparison, newItem: StatComparison): Boolean {
            return oldItem.statName == newItem.statName
        }

        override fun areContentsTheSame(oldItem: StatComparison, newItem: StatComparison): Boolean {
            return oldItem == newItem
        }
    }
}
