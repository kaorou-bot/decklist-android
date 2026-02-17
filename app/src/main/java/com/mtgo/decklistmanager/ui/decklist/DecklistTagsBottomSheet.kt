package com.mtgo.decklistmanager.ui.decklist

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.BottomSheetDecklistTagsBinding
import com.mtgo.decklistmanager.domain.model.Tag
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * DecklistTagsBottomSheet - 套牌标签管理底部弹窗
 */
@AndroidEntryPoint
class DecklistTagsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDecklistTagsBinding? = null
    private val binding get() = _binding!!

    private val tagViewModel: com.mtgo.decklistmanager.ui.tag.TagViewModel by viewModels()

    private var decklistId: Long = 0
    private var currentTags: List<Tag> = emptyList()

    companion object {
        const val ARG_DECKLIST_ID = "decklist_id"

        fun newInstance(decklistId: Long): DecklistTagsBottomSheet {
            return DecklistTagsBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_DECKLIST_ID, decklistId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        decklistId = requireArguments().getLong(ARG_DECKLIST_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDecklistTagsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
        setupViews()
        loadTags()
    }

    override fun onResume() {
        super.onResume()
        // 每次恢复时重新加载标签，确保显示最新数据
        loadTags()
    }

    private fun setupDialog() {
        dialog?.setOnShowListener {
            val bottomSheet = (dialog as BottomSheetDialog).findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
            }
        }
    }

    private fun setupViews() {
        binding.btnAddTag.setOnClickListener {
            showAddTagDialog()
        }

        binding.btnClose.setOnClickListener {
            // 关闭前通知父页面刷新
            notifyParentRefresh()
            dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 弹窗销毁时也通知父页面刷新
        notifyParentRefresh()
    }

    private fun loadTags() {
        lifecycleScope.launch {
            val tags = tagViewModel.getTagsForDecklist(decklistId)
            currentTags = tags
            updateTagsChips(tags)
        }
    }

    private fun updateTagsChips(tags: List<Tag>) {
        binding.chipGroupTags.removeAllViews()

        if (tags.isEmpty()) {
            binding.tvNoTags.visibility = View.VISIBLE
        } else {
            binding.tvNoTags.visibility = View.GONE

            tags.forEach { tag ->
                val chip = Chip(requireContext()).apply {
                    text = tag.name
                    isCloseIconVisible = true
                    setChipBackgroundColorResource(android.R.color.holo_blue_light)
                    setTextColor(resources.getColor(android.R.color.white, null))
                    // 让芯片更小巧 (28dp)
                    chipMinHeight = 28f * resources.displayMetrics.density
                    textSize = 12f

                    setOnCloseIconClickListener {
                        removeTag(tag)
                    }
                }
                binding.chipGroupTags.addView(chip)
            }
        }
    }

    private fun showAddTagDialog() {
        // 获取所有可用标签
        lifecycleScope.launch {
            val allTags = tagViewModel.getAllTags()
            val currentTagIds = currentTags.map { it.id }.toSet()

            // 构建多选对话框
            val checkedItems = BooleanArray(allTags.size) { i ->
                allTags[i].id in currentTagIds
            }

            val tagNames = allTags.map { it.name }.toTypedArray()

            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择标签")
                .setMultiChoiceItems(tagNames, checkedItems) { _, which, isChecked ->
                    checkedItems[which] = isChecked
                }
                .setPositiveButton("确定") { _, _ ->
                    lifecycleScope.launch {
                        // 获取选中的标签ID
                        val selectedIds = mutableSetOf<Long>()
                        checkedItems.forEachIndexed { index, isChecked ->
                            if (isChecked) {
                                selectedIds.add(allTags[index].id)
                            }
                        }

                        // 找出需要删除的标签
                        val toRemove = currentTagIds - selectedIds
                        // 找出需要添加的标签
                        val toAdd = selectedIds - currentTagIds

                        // 先执行UI更新（删除）
                        toRemove.forEach { tagId ->
                            val tag = currentTags.find { it.id == tagId }
                            tag?.let { removeTagFromUI(it) }
                        }

                        // 执行数据库操作
                        toRemove.forEach { tagId ->
                            tagViewModel.removeTagFromDecklist(decklistId, tagId)
                        }

                        // 先添加到UI（即时反馈）
                        val addedTags = mutableListOf<Tag>()
                        toAdd.forEach { tagId ->
                            val tag = allTags.find { it.id == tagId }
                            tag?.let {
                                addedTags.add(it)
                                addTagToUI(it)
                            }
                        }

                        // 再执行数据库添加操作
                        toAdd.forEach { tagId ->
                            tagViewModel.addTagToDecklist(decklistId, tagId)
                        }

                        // 更新当前标签列表
                        currentTags = currentTags.filter { it.id !in toRemove } + addedTags

                        // 最后刷新 DeckDetailActivity
                        notifyParentRefresh()
                    }
                }
                .setNegativeButton("取消", null)
                .setNeutralButton("新建标签") { _, _ ->
                    showNewTagDialog()
                }
                .show()
        }
    }

    private fun showNewTagDialog() {
        val editText = android.widget.EditText(requireContext())
        editText.hint = "标签名称"

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("创建新标签")
            .setView(editText)
            .setPositiveButton("创建") { _, _ ->
                val tagName = editText.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    lifecycleScope.launch {
                        val newTag = tagViewModel.createTag(tagName)
                        // 立即添加到UI
                        addTagToUI(newTag)
                        currentTags = currentTags + newTag
                        // 执行数据库添加操作
                        tagViewModel.addTagToDecklist(decklistId, newTag.id)
                        // 通知父页面刷新
                        notifyParentRefresh()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addTagToUI(tag: Tag) {
        binding.tvNoTags.visibility = View.GONE

        val chip = Chip(requireContext()).apply {
            text = tag.name
            isCloseIconVisible = true
            setChipBackgroundColorResource(android.R.color.holo_blue_light)
            setTextColor(resources.getColor(android.R.color.white, null))
            // 让芯片更小巧 (28dp)
            chipMinHeight = 28f * resources.displayMetrics.density
            textSize = 12f

            setOnCloseIconClickListener {
                removeTag(tag)
            }
        }
        binding.chipGroupTags.addView(chip)
    }

    private fun removeTag(tag: Tag) {
        lifecycleScope.launch {
            // 先从UI中移除，提供即时反馈
            removeTagFromUI(tag)
            // 更新当前标签列表
            currentTags = currentTags.filter { it.id != tag.id }
            // 然后执行数据库删除操作
            tagViewModel.removeTagFromDecklist(decklistId, tag.id)
            // 通知父页面刷新
            notifyParentRefresh()
        }
    }

    private fun removeTagFromUI(tag: Tag) {
        // 遍历查找要删除的 chip
        for (i in 0 until binding.chipGroupTags.childCount) {
            val chip = binding.chipGroupTags.getChildAt(i) as? Chip
            if (chip?.text == tag.name) {
                binding.chipGroupTags.removeViewAt(i)
                break
            }
        }

        // 检查是否还有标签
        if (binding.chipGroupTags.childCount == 0) {
            binding.tvNoTags.visibility = View.VISIBLE
        }
    }

    private fun notifyParentRefresh() {
        // 通知父 Activity 刷新标签显示
        (activity as? com.mtgo.decklistmanager.ui.decklist.DeckDetailActivity)?.let {
            it.runOnUiThread {
                it.refreshTagsAndNotes()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

