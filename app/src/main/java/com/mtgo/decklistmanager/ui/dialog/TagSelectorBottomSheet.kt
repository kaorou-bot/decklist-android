package com.mtgo.decklistmanager.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.checkbox.MaterialCheckBox
import com.mtgo.decklistmanager.databinding.BottomSheetTagSelectorBinding
import com.mtgo.decklistmanager.domain.model.Tag
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * TagSelectorBottomSheet - 标签选择底部弹窗
 */
@AndroidEntryPoint
class TagSelectorBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTagSelectorBinding? = null
    private val binding get() = _binding!!

    private val tagViewModel: com.mtgo.decklistmanager.ui.tag.TagViewModel by viewModels()

    private var decklistId: Long = 0
    private var allTags: List<Tag> = emptyList()
    private var selectedTagIds: Set<Long> = emptySet()
    private var onTagsSelected: ((List<Long>) -> Unit)? = null

    companion object {
        const val ARG_DECKLIST_ID = "decklist_id"

        fun newInstance(decklistId: Long, onTagsSelected: (List<Long>) -> Unit): TagSelectorBottomSheet {
            return TagSelectorBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_DECKLIST_ID, decklistId)
                }
                this.onTagsSelected = onTagsSelected
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
        _binding = BottomSheetTagSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
        setupViews()
        loadData()
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
        binding.btnConfirm.setOnClickListener {
            confirmSelection()
        }

        binding.btnNewTag.setOnClickListener {
            showNewTagDialog()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            // 加载所有标签
            allTags = tagViewModel.getAllTags()
            // 加载当前套牌已选择的标签
            val currentTags = tagViewModel.getTagsForDecklist(decklistId)
            selectedTagIds = currentTags.map { it.id }.toSet()

            updateTagList()
        }
    }

    private fun updateTagList() {
        binding.tagListContainer.removeAllViews()

        if (allTags.isEmpty()) {
            binding.tvNoTags.visibility = View.VISIBLE
            binding.tagListContainer.visibility = View.GONE
        } else {
            binding.tvNoTags.visibility = View.GONE
            binding.tagListContainer.visibility = View.VISIBLE

            allTags.forEach { tag ->
                val checkBox = MaterialCheckBox(requireContext()).apply {
                    text = tag.name
                    isChecked = selectedTagIds.contains(tag.id)
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            selectedTagIds = selectedTagIds + tag.id
                        } else {
                            selectedTagIds = selectedTagIds - tag.id
                        }
                    }
                }
                binding.tagListContainer.addView(checkBox)
            }
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
                        selectedTagIds = selectedTagIds + newTag.id
                        loadData() // 重新加载
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmSelection() {
        onTagsSelected?.invoke(selectedTagIds.toList())
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
