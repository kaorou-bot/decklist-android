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
import dagger.hilt.android.AndroidEntryPoint

/**
 * Card Info Dialog - 卡牌信息弹窗
 * 支持双面牌切换
 */
@AndroidEntryPoint
class CardInfoFragment : DialogFragment() {

    private var _binding: DialogCardDetailBinding? = null
    private val binding get() = _binding!!

    private var currentCardInfo: CardInfo? = null
    private var isShowingFront = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCardDetailBinding.inflate(layoutInflater)

        val cardInfo = arguments?.getParcelable(ARG_CARD_INFO, CardInfo::class.java)
        cardInfo?.let {
            currentCardInfo = it
            isShowingFront = true
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
     */
    private fun formatText(text: String?): String {
        if (text == null) return ""
        return text
            .replace("\\n", "\n")
            .replace("\\r\\n", "\n")
            .replace("\\r", "\n")
            .replace("\n", "\n")
    }

    private fun displayCardInfo(cardInfo: CardInfo) {
        binding.apply {
            // 设置双面牌切换按钮（仅双面牌显示）
            if (cardInfo.isDualFaced) {
                btnFlipCard.visibility = View.VISIBLE
                btnFlipCard.text = if (isShowingFront) "查看反面" else "查看正面"
                btnFlipCard.setOnClickListener {
                    isShowingFront = !isShowingFront
                    updateCardDisplay()
                }
            } else {
                btnFlipCard.visibility = View.GONE
            }

            // 隐藏版本切换按钮
            btnChangeVersion.visibility = View.GONE

            updateCardDisplay()
        }
    }

    private fun updateCardDisplay() {
        val cardInfo = currentCardInfo ?: return

        binding.apply {
            // 加载卡牌图片
            val imageUrl = if (cardInfo.isDualFaced) {
                if (isShowingFront) {
                    cardInfo.frontImageUri ?: cardInfo.imageUriNormal
                } else {
                    cardInfo.backImageUri
                }
            } else {
                cardInfo.imageUriNormal
            }

            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this@CardInfoFragment)
                    .load(imageUrl)
                    .placeholder(com.google.android.material.R.drawable.mtrl_ic_cancel)
                    .error(com.google.android.material.R.drawable.mtrl_ic_error)
                    .into(imageViewCard)
            } else {
                imageViewCard.visibility = View.GONE
            }

            // 更新切换按钮文本
            if (cardInfo.isDualFaced) {
                btnFlipCard.text = if (isShowingFront) "查看反面" else "查看正面"
            }

            // 构建详细信息文本
            val details = buildString {
                if (cardInfo.isDualFaced && !isShowingFront) {
                    // 显示反面信息
                    appendLine("卡牌名称：${cardInfo.backFaceName ?: cardInfo.name}")
                    appendLine()
                    appendLine("类型：${cardInfo.backFaceTypeLine ?: ""}")
                    appendLine("法术力：${cardInfo.backFaceManaCost ?: "N/A"}")
                    cardInfo.backFaceOracleText?.let {
                        val text = formatText(it)
                        if (text.isNotEmpty()) {
                            appendLine("规则文本：\n$text")
                        }
                    }
                } else {
                    // 显示正面信息
                    appendLine("卡牌名称：${cardInfo.name}")
                    appendLine()
                    appendLine("类型：${cardInfo.typeLine ?: ""}")
                    appendLine("法术力：${cardInfo.manaCost ?: "N/A"}")
                    cardInfo.oracleText?.let {
                        val text = formatText(it)
                        if (text.isNotEmpty()) {
                            appendLine("规则文本：\n$text")
                        }
                    }
                }

                // 力量/防御力 - 根据当前显示的面来选择数据
                val powerToUse = if (cardInfo.isDualFaced && !isShowingFront) {
                    cardInfo.backFacePower
                } else {
                    cardInfo.power
                }
                val toughnessToUse = if (cardInfo.isDualFaced && !isShowingFront) {
                    cardInfo.backFaceToughness
                } else {
                    cardInfo.toughness
                }

                powerToUse?.let { power ->
                    toughnessToUse?.let { toughness ->
                        appendLine("攻防：$power/$toughness")
                    }
                }

                // 忠诚度 - 根据当前显示的面来选择数据
                val loyaltyToUse = if (cardInfo.isDualFaced && !isShowingFront) {
                    cardInfo.backFaceLoyalty
                } else {
                    cardInfo.loyalty
                }

                loyaltyToUse?.let { appendLine("忠诚：$it") }

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
