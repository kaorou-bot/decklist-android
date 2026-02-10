package com.mtgo.decklistmanager.ui.folder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mtgo.decklistmanager.databinding.ItemFolderBinding
import com.mtgo.decklistmanager.domain.model.Folder
import javax.inject.Inject

/**
 * FolderAdapter - 文件夹列表适配器
 */
class FolderAdapter(
    private val onFolderClick: (Folder) -> Unit,
    private val onFolderLongClick: (Folder) -> Unit,
    private val onFolderDelete: (Folder) -> Unit
) : ListAdapter<Folder, FolderAdapter.FolderViewHolder>(FolderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FolderViewHolder(
        private val binding: ItemFolderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(folder: Folder) {
            binding.apply {
                nameText.text = folder.name
                countText.text = root.context.getString(
                    com.mtgo.decklistmanager.R.string.folder_deck_count,
                    folder.decklistCount
                )

                // Set folder icon color
                folderIcon.setColorFilter(folder.color)

                root.setOnClickListener {
                    onFolderClick(folder)
                }

                root.setOnLongClickListener {
                    onFolderLongClick(folder)
                    true
                }

                deleteButton.setOnClickListener {
                    onFolderDelete(folder)
                }
            }
        }
    }

    class FolderDiffCallback : DiffUtil.ItemCallback<Folder>() {
        override fun areItemsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Folder, newItem: Folder): Boolean {
            return oldItem == newItem
        }
    }
}
