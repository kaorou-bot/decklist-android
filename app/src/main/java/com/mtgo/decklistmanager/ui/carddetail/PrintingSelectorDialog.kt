package com.mtgo.decklistmanager.ui.carddetail

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto
import com.mtgo.decklistmanager.ui.search.SearchViewModel
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 印刷版本选择弹窗
 *
 * 显示卡牌的所有印刷版本，允许用户选择
 * 使用 ViewModel 共享数据而不是通过 Bundle 传递
 */
@AndroidEntryPoint
class PrintingSelectorDialog : DialogFragment() {

    private var oracleId: String? = null
    private var currentIndex: Int = 0
    private var onPrintingSelected: ((Int) -> Unit)? = null

    // 使用共享的 SearchViewModel 来获取印刷版本
    private val searchViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            defaultViewModelProviderFactory
        )[SearchViewModel::class.java]
    }

    private var printings: List<MtgchCardDto> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // 获取参数
        oracleId = arguments?.getString(ARG_ORACLE_ID)
        currentIndex = arguments?.getInt(ARG_CURRENT_INDEX, 0) ?: 0

        // 先显示一个空对话框，然后异步加载数据
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择印刷版本")
            .setNegativeButton("取消", null)
            .create()

        // 异步加载印刷版本
        oracleId?.let { loadPrintingsAndUpdateDialog(it, dialog) }

        return dialog
    }

    /**
     * 加载印刷版本并更新对话框
     */
    private fun loadPrintingsAndUpdateDialog(oracleId: String, dialog: Dialog) {
        lifecycleScope.launch {
            try {
                AppLogger.d("PrintingSelectorDialog", "Loading printings for: $oracleId")

                val result = searchViewModel.getCardPrintings(oracleId, limit = 100)
                result?.let { (cards, total) ->
                    printings = cards
                    AppLogger.d("PrintingSelectorDialog", "Loaded ${cards.size} printings")

                    // 构建显示列表
                    val displayItems = cards.mapIndexed { index, card ->
                        val setName = card.setName ?: card.setCode ?: "Unknown"
                        val collectorNumber = card.collectorNumber ?: "?"
                        val rarity = getRaritySymbol(card.rarity)
                        val isCurrent = if (index == currentIndex) " ✓" else ""
                        "$setName #$collectorNumber $rarity$isCurrent"
                    }

                    // 更新对话框内容
                    updateDialogItems(dialog, displayItems)
                } ?: run {
                    AppLogger.w("PrintingSelectorDialog", "No printings found")
                    dismiss()
                }
            } catch (e: Exception) {
                AppLogger.e("PrintingSelectorDialog", "Error loading printings", e)
                dismiss()
            }
        }
    }

    /**
     * 更新对话框的单选列表项
     */
    private fun updateDialogItems(dialog: Dialog, items: List<String>) {
        // 需要重建对话框以更新列表
        val alertDialog = dialog as? androidx.appcompat.app.AlertDialog
        alertDialog?.apply {
            // 清除原有按钮
            setButton(Dialog.BUTTON_NEGATIVE, "取消") { _, _ -> dismiss() }

            // 设置单选列表（需要重新创建对话框）
            // 由于 AlertDialog 已经创建，我们需要使用不同的方式

            // 关闭当前对话框并重新创建
            dismiss()

            // 创建新的对话框
            val newDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择印刷版本")
                .setSingleChoiceItems(items.toTypedArray(), currentIndex) { _, which ->
                    onPrintingSelected?.invoke(which)
                    // 关闭新对话框
                    // 注意：这里 dismiss() 会关闭新对话框，不是原来的
                }
                .setNegativeButton("取消") { _, _ ->
                    // 关闭新对话框
                }
                .create()

            // 立即显示新对话框
            newDialog.show()
        }
    }

    /**
     * 获取稀有度符号
     */
    private fun getRaritySymbol(rarity: String?): String {
        return when (rarity?.lowercase()) {
            "common" -> "C"
            "uncommon" -> "U"
            "rare" -> "R"
            "mythic" -> "M"
            else -> ""
        }
    }

    companion object {
        private const val ARG_ORACLE_ID = "oracle_id"
        private const val ARG_CURRENT_INDEX = "current_index"

        fun newInstance(
            oracleId: String,
            currentIndex: Int,
            onPrintingSelected: (Int) -> Unit
        ): PrintingSelectorDialog {
            return PrintingSelectorDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORACLE_ID, oracleId)
                    putInt(ARG_CURRENT_INDEX, currentIndex)
                }
                this.onPrintingSelected = onPrintingSelected
            }
        }
    }
}
