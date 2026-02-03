package com.mtgo.decklistmanager.ui.decklist

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mtgo.decklistmanager.databinding.DialogCardDetailBinding
import com.mtgo.decklistmanager.domain.model.CardInfo
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Card Info Dialog - 卡牌信息弹窗
 * 支持双面牌切换和版本切换
 */
@AndroidEntryPoint
class CardInfoFragment : DialogFragment() {

    @Inject
    lateinit var repository: com.mtgo.decklistmanager.data.repository.DecklistRepository

    private var _binding: DialogCardDetailBinding? = null
    private val binding get() = _binding!!

    private var currentCardInfo: CardInfo? = null
    private var isShowingFront = true

    // 卡牌的所有版本（用于版本切换）
    private var allVersions: List<CardInfo> = emptyList()
    private var currentVersionIndex = 0

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
            // 设置双面牌切换按钮
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

            // 设置版本切换按钮
            btnChangeVersion.visibility = View.VISIBLE
            btnChangeVersion.setOnClickListener {
                showVersionSelector()
            }

            updateCardDisplay()
        }
    }

    /**
     * 显示版本选择对话框
     */
    private fun showVersionSelector() {
        val currentInfo = currentCardInfo ?: return

        lifecycleScope.launch {
            try {
                // 显示加载提示
                android.widget.Toast.makeText(requireContext(), "正在加载版本列表...", android.widget.Toast.LENGTH_SHORT).show()

                // 获取所有版本
                allVersions = repository.getCardAllVersions(currentInfo.name)

                if (allVersions.isEmpty()) {
                    android.widget.Toast.makeText(requireContext(), "未找到其他版本", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 找到当前版本索引
                currentVersionIndex = allVersions.indexOfFirst {
                    it.id == currentInfo.id
                }

                // 构建版本列表显示
                val versionNames = allVersions.mapIndexed { index, card ->
                    val prefix = if (index == currentVersionIndex) "✓ " else ""
                    "$prefix${card.setName ?: "未知系列"} (${card.setCode ?: "?"})"
                }.toTypedArray()

                // 显示选择对话框
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("选择版本")
                    .setSingleChoiceItems(versionNames, currentVersionIndex) { dialog, which ->
                        currentVersionIndex = which
                        val selectedCard = allVersions[which]
                        currentCardInfo = selectedCard
                        updateCardDisplay()
                        dialog.dismiss()
                    }
                    .setNegativeButton("取消", null)
                    .show()

            } catch (e: Exception) {
                AppLogger.e("CardInfoFragment", "Error loading versions: ${e.message}")
                android.widget.Toast.makeText(requireContext(), "加载版本失败", android.widget.Toast.LENGTH_SHORT).show()
            }
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

            // 更新版本按钮文本
            if (allVersions.isNotEmpty()) {
                btnChangeVersion.text = "${cardInfo.setName} (${cardInfo.setCode})"
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

                // 力量/防御力
                cardInfo.power?.let { power ->
                    cardInfo.toughness?.let { toughness ->
                        appendLine("攻防：$power/$toughness")
                    }
                }

                // 忠诚度
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
