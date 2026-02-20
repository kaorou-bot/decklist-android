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
import com.mtgo.decklistmanager.data.remote.api.dto.CardInfoDto
import com.mtgo.decklistmanager.data.remote.api.dto.toCardInfo
import com.mtgo.decklistmanager.ui.search.SearchViewModel
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

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

    // 保存原始卡牌的中文规则文本，切换版本时继续使用
    private var originalChineseOracleText: String? = null
    private var originalChineseTypeLine: String? = null
    private var originalChineseSetName: String? = null
    private var originalBackFaceOracleText: String? = null
    private var originalBackFaceTypeLine: String? = null

    private var oracleId: String? = null
    private var printings: List<CardInfoDto> = emptyList()
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
            // 保存原始中文规则文本和类型行
            originalChineseOracleText = it.oracleText
            originalChineseTypeLine = it.typeLine
            originalChineseSetName = it.setName
            originalBackFaceOracleText = it.backFaceOracleText
            originalBackFaceTypeLine = it.backFaceTypeLine
            displayCardInfo(it)
        }

        oracleId = arguments?.getString(ARG_ORACLE_ID)
        val receivedOracleId = oracleId
        AppLogger.d("CardInfoFragment", "Received oracleId: $receivedOracleId")
        AppLogger.d("CardInfoFragment", "Card info oracleId: ${cardInfo?.oracleId}")

        // 优先使用传入的 oracleId，否则使用 cardInfo.oracleId
        val effectiveOracleId = receivedOracleId ?: cardInfo?.oracleId

        if (effectiveOracleId != null) {
            AppLogger.d("CardInfoFragment", "Loading printings for oracleId: $effectiveOracleId")
            loadPrintings(effectiveOracleId)
        } else {
            // 没有 oracleId 时，尝试使用卡牌名称搜索获取印刷版本
            // 但只对英文名称进行搜索（中文名称搜索会失败）
            AppLogger.w("CardInfoFragment", "No oracleId available, trying to load printings by card name")
            val cardName = cardInfo?.enName ?: cardInfo?.name
            if (!cardName.isNullOrEmpty()) {
                // 只使用英文名称搜索
                loadPrintingsByName(cardName)
            } else {
                AppLogger.w("CardInfoFragment", "No card name available, cannot load printings")
                binding.btnSelectVersion.visibility = View.GONE
            }
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
                if (which in printings.indices) {
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
                val result = searchViewModel.getCardPrintings(oracleId, limit = 2000)
                result?.let { (cards, total) ->
                    // 验证返回的印刷版本是否匹配当前卡牌
                    val expectedCardName = currentCardInfo?.name
                    val expectedEnName = currentCardInfo?.enName
                    val isValid = cards.isEmpty() || cards.any { card ->
                        val cardNameMatches = card.name.equals(expectedCardName, ignoreCase = true)
                        val zhNameMatches = card.nameZh?.equals(expectedCardName, ignoreCase = true) == true
                        val enNameMatches = card.name.equals(expectedEnName, ignoreCase = true)
                        cardNameMatches || zhNameMatches || enNameMatches
                    }

                    if (isValid) {
                        printings = cards
                        AppLogger.d("CardInfoFragment", "Loaded ${cards.size} printings (validated)")
                        if (_binding != null) {
                            updateVersionSelectorItems()
                        } else {
                            AppLogger.e("CardInfoFragment", "_binding is null, cannot update button")
                        }
                    } else {
                        // oracleId 指向了错误的卡牌，回退到按名称搜索
                        AppLogger.w("CardInfoFragment", "Oracle ID mismatch: expected '$expectedCardName', got cards with names: ${cards.map { it.name }.take(3).joinToString()}")
                        AppLogger.w("CardInfoFragment", "Falling back to name-based search")
                        expectedCardName?.let { loadPrintingsByName(it) }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("CardInfoFragment", "Error loading printings", e)
            }
        }
    }

    private fun loadPrintingsByName(cardName: String) {
        // 使用卡牌名称搜索获取印刷版本
        loadingJob = lifecycleScope.launch {
            try {
                AppLogger.d("CardInfoFragment", "Searching printings by name: $cardName")
                AppLogger.d("CardInfoFragment", "Current card info - name: ${currentCardInfo?.name}, enName: ${currentCardInfo?.enName}, oracleId: ${currentCardInfo?.oracleId}")

                var oracleId: String? = currentCardInfo?.oracleId
                var englishName: String? = currentCardInfo?.enName

                // 如果 oracleId 为 null，先搜索获取 oracleId
                if (oracleId.isNullOrEmpty()) {
                    AppLogger.d("CardInfoFragment", "oracleId is null, fetching from API")

                    try {
                        // 添加超时控制，避免长时间卡住
                        withTimeout(5000) {
                            // 尝试多个搜索词
                            val searchTerms = mutableListOf<String>()

                            // 1. 优先使用英文名（如果存在）
                            val engName = englishName
                            if (engName != null) {
                                searchTerms.add(engName)
                            }

                            // 2. 对于双面牌（名称包含 //），只使用第一部分搜索
                            val firstHalfName = if (cardName.contains(" // ")) {
                                cardName.split(" // ")[0].trim()
                            } else null
                            if (firstHalfName != null) {
                                searchTerms.add(firstHalfName)
                            }

                            // 3. 使用原始卡名
                            searchTerms.add(cardName)

                            AppLogger.d("CardInfoFragment", "Will try searching with: ${searchTerms.joinToString(", ")}")

                            // 依次尝试每个搜索词，直到找到结果
                            for (searchTerm in searchTerms) {
                                val cards = searchViewModel.searchCardPrintingsByName(searchTerm, limit = 5)
                                if (cards.isNotEmpty()) {
                                    // 找到匹配的卡牌，获取其 oracleId
                                    val matchedCard = cards.find { card ->
                                        val nameMatch = card.name.equals(searchTerm, ignoreCase = true)
                                        val zhNameMatch = card.nameZh?.equals(cardName, ignoreCase = true) == true ||
                                                        card.nameZh?.equals(firstHalfName, ignoreCase = true) == true
                                        nameMatch || zhNameMatch
                                    } ?: cards.firstOrNull()

                                    if (matchedCard != null) {
                                        oracleId = matchedCard.oracleId
                                        englishName = matchedCard.name

                                        AppLogger.d("CardInfoFragment", "Found oracleId: $oracleId, English name: $englishName")

                                        // 更新 currentCardInfo
                                        currentCardInfo = currentCardInfo?.copy(
                                            oracleId = oracleId,
                                            enName = englishName
                                        )

                                        break
                                    }
                                }
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        AppLogger.w("CardInfoFragment", "Search oracleId timeout after 5 seconds")
                        // 超时后不再继续查询印刷版本
                        return@launch
                    } catch (e: Exception) {
                        AppLogger.e("CardInfoFragment", "Failed to fetch oracleId", e)
                        return@launch
                    }
                }

                // 现在使用 oracleId 获取所有印刷版本
                val oId = oracleId
                if (oId != null) {
                    AppLogger.d("CardInfoFragment", "Loading printings using oracleId: $oId")
                    val result = searchViewModel.getCardPrintings(oId, limit = 100)
                    result?.let { (cards, total) ->
                        printings = cards
                        AppLogger.d("CardInfoFragment", "Loaded ${cards.size} printings using oracleId")
                        if (_binding != null) {
                            updateVersionSelectorItems()
                        } else {
                            AppLogger.e("CardInfoFragment", "_binding is null, cannot update button")
                        }
                        return@launch
                    }
                }

                // 如果没有 oracleId，回退到直接搜索（可能只有1个结果）
                // 只有在英文名的情况下才回退，中文名搜索通常会失败
                val engName = englishName
                if (engName != null && oracleId == null) {
                    AppLogger.w("CardInfoFragment", "No oracleId found, falling back to direct search")
                    val cards = searchViewModel.searchCardPrintingsByName(engName)
                    printings = cards
                    if (_binding != null) {
                        updateVersionSelectorItems()
                    } else {
                        AppLogger.e("CardInfoFragment", "_binding is null, cannot update button")
                    }
                } else {
                    AppLogger.d("CardInfoFragment", "Skipping printings load - no english name available")
                }
            } catch (e: Exception) {
                AppLogger.e("CardInfoFragment", "Error loading printings by name", e)
            }
        }
    }

    private fun updateVersionSelectorItems() {
        AppLogger.d("CardInfoFragment", "updateVersionSelectorItems - printings size: ${printings.size}")
        // 只要有印刷版本数据就显示选择按钮
        val shouldShow = printings.isNotEmpty()
        binding.btnSelectVersion.visibility = if (shouldShow) View.VISIBLE else View.GONE
        AppLogger.d("CardInfoFragment", "Button visibility set to: ${if (shouldShow) "VISIBLE" else "GONE"}")
        AppLogger.d("CardInfoFragment", "Button height: ${binding.btnSelectVersion.height}, width: ${binding.btnSelectVersion.width}")
    }

    private fun switchToPrinting(index: Int) {
        AppLogger.d("CardInfoFragment", "switchToPrinting called with index: $index, printings size: ${printings.size}")
        if (index in printings.indices) {
            currentPrintingIndex = index

            val newCard = printings[index]
            AppLogger.d("CardInfoFragment", "Switching to printing $index: ${newCard.name}")
            AppLogger.d("CardInfoFragment", "  setCode: ${newCard.setCode}, collectorNumber: ${newCard.collectorNumber}")
            AppLogger.d("CardInfoFragment", "  setNameZh: ${newCard.setNameZh}")
            AppLogger.d("CardInfoFragment", "  frontImageUri: ${newCard.imageUris?.normal}")

            // 打印 cardFaces 信息
            newCard.cardFaces?.let { faces ->
                AppLogger.d("CardInfoFragment", "  cardFaces size: ${faces.size}")
                if (faces.size >= 2) {
                    AppLogger.d("CardInfoFragment", "  cardFaces[0].imageUris?.normal: ${faces[0].imageUris?.normal}")
                    AppLogger.d("CardInfoFragment", "  cardFaces[1].imageUris?.normal: ${faces[1].imageUris?.normal}")
                }
            }

            // 使用 ServerMapper 将 CardInfoDto 转换为 CardInfo
            val tempCardInfo = newCard.toCardInfo()

            // 如果原始有中文文本，优先使用原始的（保留用户选择的语言）
            // 但是系列名称应该跟随切换的版本变化
            val newCardInfo = tempCardInfo.copy(
                oracleText = originalChineseOracleText ?: tempCardInfo.oracleText,
                typeLine = originalChineseTypeLine ?: tempCardInfo.typeLine,
                setName = tempCardInfo.setName,  // 使用新版本的系列名称
                backFaceOracleText = if (originalBackFaceOracleText != null && tempCardInfo.backFaceOracleText != null) {
                    originalBackFaceOracleText
                } else {
                    tempCardInfo.backFaceOracleText
                },
                backFaceTypeLine = if (originalBackFaceTypeLine != null && tempCardInfo.backFaceTypeLine != null) {
                    originalBackFaceTypeLine
                } else {
                    tempCardInfo.backFaceTypeLine
                }
            )

            AppLogger.d("CardInfoFragment", "Built new CardInfo: ${newCardInfo.name}")
            AppLogger.d("CardInfoFragment", "  new imageUriNormal: ${newCardInfo.imageUriNormal}")
            AppLogger.d("CardInfoFragment", "  new frontImageUri: ${newCardInfo.frontImageUri}")

            currentCardInfo = newCardInfo
            isShowingFront = true

            // 更新系列名称显示
            binding.textViewSetName.text = newCardInfo.setName ?: "N/A"

            AppLogger.d("CardInfoFragment", "Calling displayCardInfo with new CardInfo")
            displayCardInfo(newCardInfo)
        } else {
            AppLogger.w("CardInfoFragment", "Invalid index $index, printings size: ${printings.size}")
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

            // 使用 Glide 加载图片
            if (!imageUrl.isNullOrEmpty()) {
                AppLogger.d("CardInfoFragment", "Loading image: $imageUrl")
                Glide.with(this@CardInfoFragment)
                    .load(imageUrl)
                    .placeholder(com.google.android.material.R.drawable.mtrl_ic_cancel)
                    .error(com.google.android.material.R.drawable.mtrl_ic_error)
                    .into(imageViewCard)
                imageViewCard.visibility = View.VISIBLE
            } else {
                AppLogger.w("CardInfoFragment", "No image URL available")
                imageViewCard.visibility = View.GONE
            }

            if (cardInfo.isDualFaced) {
                btnFlipCard.text = "查看其他部分"
            }

            // 更新系列名称显示（使用中文）
            binding.textViewSetName.text = cardInfo.setName ?: "N/A"

            val details = buildString {
                if (cardInfo.isDualFaced) {
                    // 真正的双面牌，根据当前显示哪一面来决定显示内容
                    if (!isShowingFront) {
                        // 显示背面
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
                        // 显示正面
                        appendLine("卡牌名称：${cardInfo.frontFaceName ?: cardInfo.name}")
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
                } else if (cardInfo.backFaceName != null) {
                    // Split型等多面卡牌（不是真正的双面牌），同时显示所有面
                    appendLine("=== 第一部分 ===")
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

                    appendLine()
                    appendLine("=== 第二部分 ===")
                    appendLine("卡牌名称：${cardInfo.backFaceName}")
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
