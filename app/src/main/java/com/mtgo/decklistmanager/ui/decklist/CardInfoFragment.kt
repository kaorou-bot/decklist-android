package com.mtgo.decklistmanager.ui.decklist

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mtgo.decklistmanager.databinding.DialogCardDetailBinding
import com.mtgo.decklistmanager.domain.model.CardInfo
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto
import com.mtgo.decklistmanager.ui.search.SearchViewModel
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Card Info Dialog - 卡牌信息弹窗
 * 支持双面牌切换和印刷版本下拉菜单
 */
@AndroidEntryPoint
class CardInfoFragment : DialogFragment() {

    private var _binding: DialogCardDetailBinding? = null
    private val binding get() = _binding!!

    private var currentCardInfo: CardInfo? = null
    private var isShowingFront = true

    private var oracleId: String? = null
    private var printings: List<MtgchCardDto> = emptyList()
    private var currentPrintingIndex: Int = 0
    private var loadingJob: Job? = null

    private val searchViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            defaultViewModelProviderFactory
        )[SearchViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCardDetailBinding.inflate(layoutInflater)

        setupVersionSelector()

        val cardInfo = arguments?.getParcelable(ARG_CARD_INFO, CardInfo::class.java)
        cardInfo?.let {
            currentCardInfo = it
            isShowingFront = true
            displayCardInfo(it)
        }

        oracleId = arguments?.getString(ARG_ORACLE_ID)
        oracleId?.let {
            loadPrintings(it)
        }

        val title = cardInfo?.name ?: "卡牌详情"

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton("关闭") { _, _ ->
                dismiss()
            }
            .create()
    }

    private fun setupVersionSelector() {
        // 点击版本选择按钮时弹出对话框
        binding.btnSelectVersion.setOnClickListener {
            AppLogger.d("CardInfoFragment", "Version selector clicked, printings size: ${printings.size}")
            if (printings.isNotEmpty()) {
                showVersionSelectorDialog()
            } else {
                AppLogger.d("CardInfoFragment", "No printings available to show")
            }
        }

        // 初始时隐藏按钮，等加载完版本后再显示
        binding.btnSelectVersion.visibility = View.GONE
    }

    private fun showVersionSelectorDialog() {
        val items = printings.mapIndexed { index, card ->
            val setName = card.setNameZh ?: card.setName ?: card.setCode ?: "Unknown"
            val collectorNumber = card.collectorNumber ?: "?"
            val rarity = getRaritySymbol(card.rarity)
            "$setName — #$collectorNumber $rarity"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择印刷版本")
            .setItems(items) { _, which ->
                if (which != currentPrintingIndex && which in printings.indices) {
                    switchToPrinting(which)
                }
            }
            .show()
    }

    private fun loadPrintings(oracleId: String) {
        // 不在 loadPrintings 中添加 lifecycle observer，而是直接启动协程
        loadingJob = lifecycleScope.launch {
            try {
                AppLogger.d("CardInfoFragment", "Loading printings for: $oracleId")
                val result = searchViewModel.getCardPrintings(oracleId, limit = 100)
                result?.let { (cards, total) ->
                    printings = cards
                    AppLogger.d("CardInfoFragment", "Loaded ${cards.size} printings")
                    AppLogger.d("CardInfoFragment", "_binding is null: ${_binding == null}")
                    if (_binding != null) {
                        updateVersionSelectorItems()
                    } else {
                        AppLogger.e("CardInfoFragment", "_binding is null, cannot update button")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("CardInfoFragment", "Error loading printings", e)
            }
        }
    }

    private fun updateVersionSelectorItems() {
        AppLogger.d("CardInfoFragment", "updateVersionSelectorItems - printings size: ${printings.size}")
        // 如果有多个版本，显示选择按钮
        val shouldShow = printings.size > 1
        binding.btnSelectVersion.visibility = if (shouldShow) View.VISIBLE else View.GONE
        AppLogger.d("CardInfoFragment", "Button visibility set to: ${if (shouldShow) "VISIBLE" else "GONE"}")
        AppLogger.d("CardInfoFragment", "Button height: ${binding.btnSelectVersion.height}, width: ${binding.btnSelectVersion.width}")
    }

    private fun switchToPrinting(index: Int) {
        if (index in printings.indices) {
            currentPrintingIndex = index

            val newCard = printings[index]
            AppLogger.d("CardInfoFragment", "Switching to printing $index: ${newCard.name}")
            AppLogger.d("CardInfoFragment", "  setCode: ${newCard.setCode}, collectorNumber: ${newCard.collectorNumber}")
            AppLogger.d("CardInfoFragment", "  frontImageUri: ${newCard.imageUris?.normal}")

            // 打印 cardFaces 信息
            newCard.cardFaces?.let { faces ->
                AppLogger.d("CardInfoFragment", "  cardFaces size: ${faces.size}")
                if (faces.size >= 2) {
                    AppLogger.d("CardInfoFragment", "  cardFaces[0].imageUris?.normal: ${faces[0].imageUris?.normal}")
                    AppLogger.d("CardInfoFragment", "  cardFaces[1].imageUris?.normal: ${faces[1].imageUris?.normal}")
                }
            }

            val newCardInfo = com.mtgo.decklistmanager.util.CardDetailHelper.buildCardInfo(
                mtgchCard = newCard,
                cardInfoId = newCard.idString ?: newCard.oracleId ?: ""
            )

            currentCardInfo = newCardInfo
            isShowingFront = true

            // 更新系列名称显示
            binding.textViewSetName.text = newCardInfo.setName ?: "N/A"

            displayCardInfo(newCardInfo)
        }
    }

    private fun getRaritySymbol(rarity: String?): String {
        return when (rarity?.lowercase()) {
            "common" -> "C"
            "uncommon" -> "U"
            "rare" -> "R"
            "mythic" -> "M"
            else -> ""
        }
    }

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
            if (cardInfo.isDualFaced) {
                btnFlipCard.visibility = View.VISIBLE
                btnFlipCard.text = "查看其他部分"
                btnFlipCard.setOnClickListener {
                    isShowingFront = !isShowingFront
                    updateCardDisplay()
                }
            } else {
                btnFlipCard.visibility = View.GONE
            }

            updateCardDisplay()
        }
    }

    private fun updateCardDisplay() {
        val cardInfo = currentCardInfo ?: return

        AppLogger.d("CardInfoFragment", "updateCardDisplay - isDualFaced: ${cardInfo.isDualFaced}, isShowingFront: $isShowingFront")
        AppLogger.d("CardInfoFragment", "frontImageUri: ${cardInfo.frontImageUri}")
        AppLogger.d("CardInfoFragment", "backImageUri: ${cardInfo.backImageUri}")
        AppLogger.d("CardInfoFragment", "imageUriNormal: ${cardInfo.imageUriNormal}")

        binding.apply {
            val imageUrl = if (cardInfo.isDualFaced) {
                if (isShowingFront) {
                    AppLogger.d("CardInfoFragment", "Showing front: ${cardInfo.frontImageUri ?: cardInfo.imageUriNormal}")
                    cardInfo.frontImageUri ?: cardInfo.imageUriNormal
                } else {
                    AppLogger.d("CardInfoFragment", "Showing back: ${cardInfo.backImageUri}")
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

            if (cardInfo.isDualFaced) {
                btnFlipCard.text = "查看其他部分"
            }

            // 更新系列名称显示（使用中文）
            binding.textViewSetName.text = cardInfo.setName ?: "N/A"

            val details = buildString {
                if (cardInfo.isDualFaced && !isShowingFront) {
                    appendLine("卡牌名称：${cardInfo.backFaceName ?: cardInfo.name}")
                    appendLine()
                    appendLine("类别：${cardInfo.backFaceTypeLine ?: ""}")
                    // 法术力不为空才显示
                    cardInfo.backFaceManaCost?.let { manaCost ->
                        if (manaCost.isNotEmpty() && manaCost != "N/A") {
                            appendLine("法术力：$manaCost")
                        }
                    }
                    cardInfo.backFaceOracleText?.let {
                        val text = formatText(it)
                        if (text.isNotEmpty()) {
                            appendLine("规则文本：\n$text")
                        }
                    }
                } else {
                    appendLine("卡牌名称：${cardInfo.name}")
                    appendLine()
                    appendLine("类别：${cardInfo.typeLine ?: ""}")
                    // 法术力不为空才显示
                    cardInfo.manaCost?.let { manaCost ->
                        if (manaCost.isNotEmpty() && manaCost != "N/A") {
                            appendLine("法术力：$manaCost")
                        }
                    }
                    cardInfo.oracleText?.let {
                        val text = formatText(it)
                        if (text.isNotEmpty()) {
                            appendLine("规则文本：\n$text")
                        }
                    }
                }

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

                val loyaltyToUse = if (cardInfo.isDualFaced && !isShowingFront) {
                    cardInfo.backFaceLoyalty
                } else {
                    cardInfo.loyalty
                }

                loyaltyToUse?.let { appendLine("忠诚：$it") }

                appendLine()
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
        loadingJob?.cancel()
        _binding = null
    }

    companion object {
        private const val ARG_CARD_INFO = "card_info"
        private const val ARG_ORACLE_ID = "oracle_id"

        fun newInstance(cardInfo: CardInfo, oracleId: String? = null): CardInfoFragment {
            return CardInfoFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CARD_INFO, cardInfo)
                    oracleId?.let { putString(ARG_ORACLE_ID, it) }
                }
            }
        }
    }
}
