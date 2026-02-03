package com.mtgo.decklistmanager.ui.decklist

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mtgo.decklistmanager.databinding.DialogCardDetailBinding
import com.mtgo.decklistmanager.domain.model.CardInfo

/**
 * Card Info Dialog - 卡牌信息弹窗
 * 使用搜索风格的详情显示（文本形式）
 */
class CardInfoFragment : DialogFragment() {

    private var _binding: DialogCardDetailBinding? = null
    private val binding get() = _binding!!

    private var currentCardInfo: CardInfo? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCardDetailBinding.inflate(layoutInflater)

        val cardInfo = arguments?.getParcelable(ARG_CARD_INFO, CardInfo::class.java)
        cardInfo?.let {
            currentCardInfo = it
            displayCardInfo(it)
        }

        // 使用搜索风格的标题
        val title = cardInfo?.name ?: "卡牌详情"

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton("关闭") { _, _ ->
                dismiss()
            }
            .create()
    }

    /**
     * 将文本中的换行符转换为实际的换行
     * 处理多种可能的换行符格式
     */
    private fun formatText(text: String?): String {
        if (text == null) return ""

        // 处理多种换行符格式
        return text
            .replace("\\n", "\n")      // 字面量 \n 转换为换行符
            .replace("\\r\\n", "\n")   // Windows 风格
            .replace("\\r", "\n")      // 旧 Mac 风格
            .replace("\n", "\n")       // 确保实际的换行符保留
    }

    private fun displayCardInfo(cardInfo: CardInfo) {
        binding.apply {
            // 加载卡牌图片
            val imageUrl = cardInfo.imageUriNormal
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this@CardInfoFragment)
                    .load(imageUrl)
                    .placeholder(com.google.android.material.R.drawable.mtrl_ic_cancel)
                    .error(com.google.android.material.R.drawable.mtrl_ic_error)
                    .into(imageViewCard)
            } else {
                imageViewCard.visibility = View.GONE
            }

            // 构建详细信息文本（搜索风格）
            val details = buildString {
                // 卡牌名称
                appendLine("卡牌名称：${cardInfo.name}")
                appendLine()
                appendLine("类型：${cardInfo.typeLine ?: ""}")
                appendLine("法术力：${cardInfo.manaCost ?: "N/A"}")

                // 规则文本
                cardInfo.oracleText?.let {
                    val text = formatText(it)
                    if (text.isNotEmpty()) {
                        appendLine("规则文本：\n$text")
                    }
                }

                // 力量/防御力
                cardInfo.power?.let { power ->
                    cardInfo.toughness?.let { toughness ->
                        appendLine("攻防：$power/$toughness")
                    }
                }

                // 忠诚度（鹏洛客）
                cardInfo.loyalty?.let { appendLine("忠诚：$it") }

                appendLine()
                appendLine("系列：${cardInfo.setName ?: "N/A"}")
                cardInfo.artist?.let { appendLine("画家：$it") }
                appendLine()
                appendLine("稀有度：${cardInfo.rarity ?: "N/A"}")
                cardInfo.colorIdentity?.let { colors ->
                    if (colors.isNotEmpty()) {
                        appendLine("颜色标识：${colors.joinToString()}")
                    }
                }

                // 双面卡信息
                if (cardInfo.isDualFaced) {
                    appendLine()
                    appendLine("注：此卡为双面卡")
                    cardInfo.backFaceName?.let {
                        appendLine("反面名称：$it")
                    }
                }
            }

            textViewCardDetails.text = details
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CARD_INFO = "card_info"

        fun newInstance(cardInfo: CardInfo): CardInfoFragment {
            return CardInfoFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CARD_INFO, cardInfo)
                }
            }
        }
    }
}
