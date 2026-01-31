package com.mtgo.decklistmanager.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.DialogExportFormatBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 导出格式选择对话框
 *
 * 使用 BottomSheetDialogFragment 显示导出选项
 */
class ExportFormatDialog : BottomSheetDialogFragment() {

    private var _binding: DialogExportFormatBinding? = null
    private val binding get() = _binding!!

    private var listener: ExportFormatListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NORMAL, R.style.Theme_MaterialComponents_Light_BottomSheetDialog)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogExportFormatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // MTGO 格式导出
        binding.btnExportMtgo.setOnClickListener {
            listener?.onExportFormatSelected(ExportFormat.MTGO)
            dismiss()
        }

        // Arena 格式导出
        binding.btnExportArena.setOnClickListener {
            listener?.onExportFormatSelected(ExportFormat.ARENA)
            dismiss()
        }

        // 文本格式导出
        binding.btnExportText.setOnClickListener {
            listener?.onExportFormatSelected(ExportFormat.TEXT)
            dismiss()
        }

        // Moxfield 分享
        binding.btnShareMoxfield.setOnClickListener {
            listener?.onShareMoxfield()
            dismiss()
        }

        // 复制到剪贴板
        binding.btnCopyClipboard.setOnClickListener {
            listener?.onCopyToClipboard()
            dismiss()
        }
    }

    /**
     * 设置监听器
     */
    fun setExportFormatListener(listener: ExportFormatListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ExportFormatDialog"

        fun newInstance(): ExportFormatDialog {
            return ExportFormatDialog()
        }
    }

    /**
     * 导出格式枚举
     */
    enum class ExportFormat {
        MTGO,
        ARENA,
        TEXT
    }

    /**
     * 导出格式监听器
     */
    interface ExportFormatListener {
        fun onExportFormatSelected(format: ExportFormat)
        fun onShareMoxfield()
        fun onCopyToClipboard()
    }
}
