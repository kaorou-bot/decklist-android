package com.mtgo.decklistmanager.data.analyzer

import com.mtgo.decklistmanager.data.local.dao.CardDao
import com.mtgo.decklistmanager.data.local.dao.DecklistDao
import com.mtgo.decklistmanager.data.local.entity.CardEntity
import com.mtgo.decklistmanager.domain.model.*
import com.mtgo.decklistmanager.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 套牌分析器
 * v4.2.0: 用于分析套牌的法术力曲线、颜色分布、类型分布等
 */
@Singleton
class DeckAnalyzer @Inject constructor(
    private val cardDao: CardDao,
    private val decklistDao: DecklistDao
) {

    /**
     * 分析套牌
     * @param decklistId 套牌ID
     * @return 套牌分析结果
     */
    suspend fun analyze(decklistId: Long): DeckAnalysis = withContext(Dispatchers.IO) {
        try {
            AppLogger.d("DeckAnalyzer", "Analyzing decklist: $decklistId")

            val cards = cardDao.getCardsByDecklistId(decklistId)
            AppLogger.d("DeckAnalyzer", "Total cards found: ${cards.size}")

            val mainDeck = cards.filter { it.location == "main" }
            val sideboard = cards.filter { it.location == "sideboard" }

            AppLogger.d("DeckAnalyzer", "Main deck cards: ${mainDeck.size}, Sideboard cards: ${sideboard.size}")

            // 调试：打印前10张牌的信息
            mainDeck.take(10).forEach { card ->
                AppLogger.d("DeckAnalyzer", "Card: ${card.cardName}, Qty: ${card.quantity}, Type: ${card.cardType}, Color: ${card.color}")
            }

            // 获取套牌名称
            val decklist = decklistDao.getDecklistById(decklistId)
            val decklistName = decklist?.deckName ?: "Unknown Deck"

            DeckAnalysis(
                decklistId = decklistId,
                decklistName = decklistName,
                manaCurve = calculateManaCurve(mainDeck, sideboard),
                colorDistribution = calculateColorDistribution(mainDeck, sideboard),
                typeDistribution = calculateTypeDistribution(mainDeck, sideboard),
                statistics = calculateStatistics(mainDeck, sideboard)
            )
        } catch (e: Exception) {
            AppLogger.e("DeckAnalyzer", "Failed to analyze decklist: ${e.message}", e)
            throw e
        }
    }

    /**
     * 计算法术力曲线
     */
    private fun calculateManaCurve(
        mainDeck: List<CardEntity>,
        sideboard: List<CardEntity>
    ): ManaCurve {
        val mainCurve = mutableMapOf<Int, Int>()
        val sideboardCurve = mutableMapOf<Int, Int>()
        val mainCurveByCard = mutableMapOf<Int, Int>()
        val sideboardCurveByCard = mutableMapOf<Int, Int>()
        var mainTotalCMC = 0.0
        var sideboardTotalCMC = 0.0
        var mainNonLandCount = 0
        var sideboardNonLandCount = 0

        // 统计主牌法术力曲线
        mainDeck.forEach { card ->
            val cmc = parseCMC(card.manaCost)
            val quantity = card.quantity

            if (cmc != null) {
                // 0-6+ 分别统计，7及以上合并为 7
                val key = minOf(cmc, 7)

                // 按卡牌数量统计
                mainCurve[key] = mainCurve.getOrDefault(key, 0) + quantity

                // 按牌名统计（每种牌只计1次）
                mainCurveByCard[key] = mainCurveByCard.getOrDefault(key, 0) + 1

                // 非地牌才计入平均法术力
                if (!isLand(card.cardType)) {
                    mainTotalCMC += cmc * quantity
                    mainNonLandCount += quantity
                }
            }
        }

        // 统计备牌法术力曲线
        sideboard.forEach { card ->
            val cmc = parseCMC(card.manaCost)
            val quantity = card.quantity

            if (cmc != null) {
                val key = minOf(cmc, 7)

                // 按卡牌数量统计
                sideboardCurve[key] = sideboardCurve.getOrDefault(key, 0) + quantity

                // 按牌名统计
                sideboardCurveByCard[key] = sideboardCurveByCard.getOrDefault(key, 0) + 1

                if (!isLand(card.cardType)) {
                    sideboardTotalCMC += cmc * quantity
                    sideboardNonLandCount += quantity
                }
            }
        }

        val mainAverage = if (mainNonLandCount > 0) mainTotalCMC / mainNonLandCount else 0.0
        val sideboardAverage = if (sideboardNonLandCount > 0) sideboardTotalCMC / sideboardNonLandCount else 0.0

        return ManaCurve(
            curve = mainCurve,
            sideboardCurve = sideboardCurve,
            curveByCard = mainCurveByCard,
            sideboardCurveByCard = sideboardCurveByCard,
            averageManaValue = mainAverage,
            sideboardAverageManaValue = sideboardAverage
        )
    }

    /**
     * 计算颜色分布
     */
    private fun calculateColorDistribution(
        mainDeck: List<CardEntity>,
        sideboard: List<CardEntity>
    ): ColorDistribution {
        val mainColors = mutableMapOf<ManaColor, Int>()
        val sideboardColors = mutableMapOf<ManaColor, Int>()
        val mainColorsByCard = mutableMapOf<ManaColor, Int>()
        val sideboardColorsByCard = mutableMapOf<ManaColor, Int>()

        // 统计主牌颜色
        mainDeck.forEach { card ->
            val quantity = card.quantity
            val colors = parseColors(card.color)

            if (colors.isEmpty()) {
                // 无色卡牌
                mainColors[ManaColor.COLORLESS] = mainColors.getOrDefault(ManaColor.COLORLESS, 0) + quantity
                mainColorsByCard[ManaColor.COLORLESS] = mainColorsByCard.getOrDefault(ManaColor.COLORLESS, 0) + 1
            } else if (colors.size == 1) {
                // 单色卡牌
                val color = colors[0]
                mainColors[color] = mainColors.getOrDefault(color, 0) + quantity
                mainColorsByCard[color] = mainColorsByCard.getOrDefault(color, 0) + 1
            } else {
                // 多色卡牌
                mainColors[ManaColor.MULTICOLOR] = mainColors.getOrDefault(ManaColor.MULTICOLOR, 0) + quantity
                mainColorsByCard[ManaColor.MULTICOLOR] = mainColorsByCard.getOrDefault(ManaColor.MULTICOLOR, 0) + 1
            }
        }

        // 统计备牌颜色
        sideboard.forEach { card ->
            val quantity = card.quantity
            val colors = parseColors(card.color)

            if (colors.isEmpty()) {
                sideboardColors[ManaColor.COLORLESS] = sideboardColors.getOrDefault(ManaColor.COLORLESS, 0) + quantity
                sideboardColorsByCard[ManaColor.COLORLESS] = sideboardColorsByCard.getOrDefault(ManaColor.COLORLESS, 0) + 1
            } else if (colors.size == 1) {
                val color = colors[0]
                sideboardColors[color] = sideboardColors.getOrDefault(color, 0) + quantity
                sideboardColorsByCard[color] = sideboardColorsByCard.getOrDefault(color, 0) + 1
            } else {
                sideboardColors[ManaColor.MULTICOLOR] = sideboardColors.getOrDefault(ManaColor.MULTICOLOR, 0) + quantity
                sideboardColorsByCard[ManaColor.MULTICOLOR] = sideboardColorsByCard.getOrDefault(ManaColor.MULTICOLOR, 0) + 1
            }
        }

        val mainTotal = mainDeck.sumOf { it.quantity }
        val sideboardTotal = sideboard.sumOf { it.quantity }

        return ColorDistribution(
            colors = mainColors,
            sideboardColors = sideboardColors,
            colorsByCard = mainColorsByCard,
            sideboardColorsByCard = sideboardColorsByCard,
            totalCards = mainTotal,
            sideboardTotal = sideboardTotal
        )
    }

    /**
     * 计算类型分布
     */
    private fun calculateTypeDistribution(
        mainDeck: List<CardEntity>,
        sideboard: List<CardEntity>
    ): TypeDistribution {
        val mainTypes = mutableMapOf<CardType, Int>()
        val sideboardTypes = mutableMapOf<CardType, Int>()
        val mainTypesByCard = mutableMapOf<CardType, Int>()
        val sideboardTypesByCard = mutableMapOf<CardType, Int>()

        // 统计主牌类型
        mainDeck.forEach { card ->
            val quantity = card.quantity
            val type = CardType.fromTypeLine(card.cardType)

            // 按数量统计
            mainTypes[type] = mainTypes.getOrDefault(type, 0) + quantity
            // 按牌名统计
            mainTypesByCard[type] = mainTypesByCard.getOrDefault(type, 0) + 1
        }

        // 统计备牌类型
        sideboard.forEach { card ->
            val quantity = card.quantity
            val type = CardType.fromTypeLine(card.cardType)

            // 按数量统计
            sideboardTypes[type] = sideboardTypes.getOrDefault(type, 0) + quantity
            // 按牌名统计
            sideboardTypesByCard[type] = sideboardTypesByCard.getOrDefault(type, 0) + 1
        }

        val mainTotal = mainDeck.sumOf { it.quantity }
        val sideboardTotal = sideboard.sumOf { it.quantity }

        return TypeDistribution(
            types = mainTypes,
            sideboardTypes = sideboardTypes,
            typesByCard = mainTypesByCard,
            sideboardTypesByCard = sideboardTypesByCard,
            totalCards = mainTotal,
            sideboardTotal = sideboardTotal
        )
    }

    /**
     * 计算统计摘要
     */
    private fun calculateStatistics(
        mainDeck: List<CardEntity>,
        sideboard: List<CardEntity>
    ): DeckStatistics {
        val mainCount = mainDeck.sumOf { it.quantity }
        val sideboardCount = sideboard.sumOf { it.quantity }

        val mainLands = mainDeck.count { isLand(it.cardType) }
        val sideboardLands = sideboard.count { isLand(it.cardType) }

        val mainNonLands = mainCount - mainLands
        val sideboardNonLands = sideboardCount - sideboardLands

        // 计算平均法术力值（仅非地）
        var mainTotalCMC = 0.0
        var sideboardTotalCMC = 0.0

        mainDeck.forEach { card ->
            if (!isLand(card.cardType)) {
                val cmc = parseCMC(card.manaCost)
                if (cmc != null) {
                    mainTotalCMC += cmc * card.quantity
                }
            }
        }

        sideboard.forEach { card ->
            if (!isLand(card.cardType)) {
                val cmc = parseCMC(card.manaCost)
                if (cmc != null) {
                    sideboardTotalCMC += cmc * card.quantity
                }
            }
        }

        val mainAverage = if (mainNonLands > 0) mainTotalCMC / mainNonLands else 0.0
        val sideboardAverage = if (sideboardNonLands > 0) sideboardTotalCMC / sideboardNonLands else 0.0

        // 统计稀有度分布
        val rarityDistribution = mutableMapOf<Rarity, Int>()
        mainDeck.forEach { card ->
            val rarity = Rarity.fromString(card.rarity)
            rarityDistribution[rarity] = rarityDistribution.getOrDefault(rarity, 0) + card.quantity
        }

        return DeckStatistics(
            mainDeckCount = mainCount,
            sideboardCount = sideboardCount,
            landCount = mainLands,
            sideboardLandCount = sideboardLands,
            nonLandCount = mainNonLands,
            sideboardNonLandCount = sideboardNonLands,
            averageManaValue = mainAverage,
            sideboardAverageManaValue = sideboardAverage,
            rarityDistribution = rarityDistribution
        )
    }

    /**
     * 解析法术力值字符串，返回数值
     * 正确的 CMC 计算逻辑：
     * - 通用法术力（数字）直接相加
     * - 每个有色符号（W/U/B/R/G）算 1
     * - X 算 0
     * - 没有法术力成本算 0
     */
    private fun parseCMC(manaCost: String?): Int? {
        if (manaCost.isNullOrBlank()) return 0

        var cmc = 0
        var i = 0

        while (i < manaCost.length) {
            val char = manaCost[i].uppercaseChar()
            when {
                // 处理数字（通用法术力）
                char.isDigit() -> {
                    // 提取完整数字
                    val numStart = i
                    while (i < manaCost.length && manaCost[i].isDigit()) {
                        i++
                    }
                    val number = manaCost.substring(numStart, i).toIntOrNull() ?: 0
                    cmc += number
                }
                // 处理 X（算0）
                char == 'X' -> {
                    i++
                }
                // 处理花色符号（W/U/B/R/G，每个算1）
                char in setOf('W', 'U', 'B', 'R', 'G') -> {
                    cmc += 1
                    i++
                }
                // 处理其他字符（如 {} / 等）
                else -> {
                    i++
                }
            }
        }

        return cmc
    }

    /**
     * 解析颜色字符串，返回颜色列表（按优先级排序）
     */
    private fun parseColors(color: String?): List<ManaColor> {
        if (color.isNullOrBlank()) return emptyList()

        val colors = mutableListOf<ManaColor>()
        val parts = color.split(",").map { it.trim() }

        parts.forEach { part ->
            when (part.uppercase()) {
                "W" -> colors.add(ManaColor.WHITE)
                "U" -> colors.add(ManaColor.BLUE)
                "B" -> colors.add(ManaColor.BLACK)
                "R" -> colors.add(ManaColor.RED)
                "G" -> colors.add(ManaColor.GREEN)
            }
        }

        // 按照固定优先级排序：W > U > B > R > G
        val priorityOrder = listOf(
            ManaColor.WHITE,
            ManaColor.BLUE,
            ManaColor.BLACK,
            ManaColor.RED,
            ManaColor.GREEN
        )

        return colors.sortedBy { priorityOrder.indexOf(it) }
    }

    /**
     * 判断是否为地牌
     */
    private fun isLand(cardType: String?): Boolean {
        if (cardType.isNullOrBlank()) return false
        return cardType.contains("Land", ignoreCase = true)
    }
}
