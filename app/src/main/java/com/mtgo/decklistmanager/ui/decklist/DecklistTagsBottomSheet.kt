package com.mtgo.decklistmanager.ui.decklist

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        _binding = BottomSheetDecklistTagsBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        // 展开到底部
        dialog.setOnShowListener {
            val bottomSheet = (dialog as BottomSheetDialog).findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
            }
        }

        setupViews()
        loadTags()
    }

    private fun setupViews() {
        binding.btnAddTag.setOnClickListener {
            showAddTagDialog()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun loadTags() {
        lifecycleScope.launch {
            currentTags = tagViewModel.getTagsForDecklist(decklistId)
            updateTagsChips()
        }
    }

    private fun updateTagsChips() {
        binding.chipGroupTags.removeAllViews()

        currentTags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag.name
                isCloseIconVisible = true
                setChipBackgroundColorResource(android.R.color.holo_blue_light)
                setTextColor(resources.getColor(android.R.color.white, null))

                setOnCloseIconClickListener {
                    removeTag(tag)
                }
            }
            binding.chipGroupTags.addView(chip)
        }

        if (currentTags.isEmpty()) {
            binding.tvNoTags.visibility = View.VISIBLE
        } else {
            binding.tvNoTags.visibility = View.GONE
        }
    }

    private fun showAddTagDialog() {
        // TODO: Show dialog to add new or existing tag
    }

    private fun removeTag(tag: Tag) {
        lifecycleScope.launch {
            tagViewModel.removeTagFromDecklist(decklistId, tag.id)
            loadTags()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
