package com.mtgo.decklistmanager.ui.dialog

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
import com.mtgo.decklistmanager.databinding.BottomSheetNoteEditBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * NoteEditBottomSheet - 备注编辑底部弹窗
 */
@AndroidEntryPoint
class NoteEditBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNoteEditBinding? = null
    private val binding get() = _binding!!

    private val noteViewModel: com.mtgo.decklistmanager.ui.note.DecklistNoteViewModel by viewModels()

    private var decklistId: Long = 0

    companion object {
        const val ARG_DECKLIST_ID = "decklist_id"

        fun newInstance(decklistId: Long): NoteEditBottomSheet {
            return NoteEditBottomSheet().apply {
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
        _binding = BottomSheetNoteEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
        setupViews()
        loadNote()
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
        binding.btnSave.setOnClickListener {
            lifecycleScope.launch {
                saveNote()
            }
        }

        binding.btnDelete.setOnClickListener {
            deleteNote()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 弹窗关闭时通知父页面刷新（即使没有保存也刷新，保持同步）
        (activity as? com.mtgo.decklistmanager.ui.decklist.DeckDetailActivity)?.refreshTagsAndNotes()
    }

    private fun loadNote() {
        lifecycleScope.launch {
            noteViewModel.loadNote(decklistId)
            // 等待加载完成后获取值
            kotlinx.coroutines.delay(50)
            noteViewModel.currentNote.value?.let { note ->
                binding.etNote.setText(note.note ?: "")
            }
        }
    }

    private suspend fun saveNote() {
        val noteText = binding.etNote.text.toString().trim()
        noteViewModel.saveNote(decklistId, noteText)
        // 通知父页面刷新
        (activity as? com.mtgo.decklistmanager.ui.decklist.DeckDetailActivity)?.refreshTagsAndNotes()
        dismiss()
    }

    private fun deleteNote() {
        if (binding.etNote.text.toString().trim().isEmpty()) {
            dismiss()
            return
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除备注")
            .setMessage("确定要删除这条备注吗？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    noteViewModel.deleteNote(decklistId)
                    // 通知父页面刷新
                    (activity as? com.mtgo.decklistmanager.ui.decklist.DeckDetailActivity)?.refreshTagsAndNotes()
                    dismiss()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
