package com.mtgo.decklistmanager.domain.model

/**
 * 套牌分析数据模型
 * v4.2.0: 用于套牌统计分析功能
 */

/**
 * 套牌分析结果
 */
data class DeckAnalysis(
    val decklistId: Long,
    val decklistName: String,
    val manaCurve: ManaCurve,
    val colorDistribution: ColorDistribution,
    val typeDistribution: TypeDistribution,
    val statistics: DeckStatistics
)

/**
 * 法术力曲线
 */
data class ManaCurve(
    val curve: Map<Int, Int>,  // 法术力值 -> 数量（主牌，按卡牌数量统计）
    val sideboardCurve: Map<Int, Int>,  // 备牌曲线
    val curveByCard: Map<Int, Int>,  // 法术力值 -> 牌种数（主牌，按牌名统计）
    val sideboardCurveByCard: Map<Int, Int>,  // 备牌牌种数
    val averageManaValue: Double,  // 平均法术力值
    val sideboardAverageManaValue: Double  // 备牌平均法术力值
) {
    companion object {
        val EMPTY = ManaCurve(
            curve = emptyMap(),
            sideboardCurve = emptyMap(),
            curveByCard = emptyMap(),
            sideboardCurveByCard = emptyMap(),
            averageManaValue = 0.0,
            sideboardAverageManaValue = 0.0
        )
    }
}

/**
 * 颜色分布
 */
data class ColorDistribution(
    val colors: Map<ManaColor, Int>,  // 颜色 -> 数量（主牌，按卡牌数量统计）
    val sideboardColors: Map<ManaColor, Int>,  // 备牌颜色
    val colorsByCard: Map<ManaColor, Int>,  // 颜色 -> 牌种数（主牌，按牌名统计）
    val sideboardColorsByCard: Map<ManaColor, Int>,  // 备牌牌种数
    val totalCards: Int,
    val sideboardTotal: Int
) {
    companion object {
        val EMPTY = ColorDistribution(
            colors = emptyMap(),
            sideboardColors = emptyMap(),
            colorsByCard = emptyMap(),
            sideboardColorsByCard = emptyMap(),
            totalCards = 0,
            sideboardTotal = 0
        )
    }

    /**
     * 获取颜色占比
     */
    fun getColorPercentage(color: ManaColor): Float {
        if (totalCards == 0) return 0f
        return (colors[color] ?: 0) / totalCards.toFloat()
    }
}

/**
 * 类型分布
 */
data class TypeDistribution(
    val types: Map<CardType, Int>,  // 类型 -> 数量（主牌，按卡牌数量统计）
    val sideboardTypes: Map<CardType, Int>,  // 备牌类型
    val typesByCard: Map<CardType, Int>,  // 类型 -> 牌种数（主牌，按牌名统计）
    val sideboardTypesByCard: Map<CardType, Int>,  // 备牌牌种数
    val totalCards: Int,
    val sideboardTotal: Int
) {
    companion object {
        val EMPTY = TypeDistribution(
            types = emptyMap(),
            sideboardTypes = emptyMap(),
            typesByCard = emptyMap(),
            sideboardTypesByCard = emptyMap(),
            totalCards = 0,
            sideboardTotal = 0
        )
    }
}

/**
 * 套牌统计摘要
 */
data class DeckStatistics(
    val mainDeckCount: Int,  // 主牌总数
    val sideboardCount: Int,  // 备牌总数
    val landCount: Int,  // 地陆数量（主牌）
    val sideboardLandCount: Int,  // 地陆数量（备牌）
    val nonLandCount: Int,  // 非地数量（主牌）
    val sideboardNonLandCount: Int,  // 非地数量（备牌）
    val averageManaValue: Double,  // 平均法术力值（主牌，仅计非地）
    val sideboardAverageManaValue: Double,  // 平均法术力值（备牌，仅计非地）
    val rarityDistribution: Map<Rarity, Int>  // 稀有度分布
) {
    companion object {
        val EMPTY = DeckStatistics(
            mainDeckCount = 0,
            sideboardCount = 0,
            landCount = 0,
            sideboardLandCount = 0,
            nonLandCount = 0,
            sideboardNonLandCount = 0,
            averageManaValue = 0.0,
            sideboardAverageManaValue = 0.0,
            rarityDistribution = emptyMap()
        )
    }
}

/**
 * 法术力颜色枚举
 */
enum class ManaColor(val displayName: String, val colorCode: String) {
    WHITE("白色", "#F8F6D8"),
    BLUE("蓝色", "#0E68AB"),
    BLACK("黑色", "#150B00"),
    RED("红色", "#D3202A"),
    GREEN("绿色", "#00733E"),
    COLORLESS("无色", "#9E9E9E"),
    MULTICOLOR("多色", "#9C27B0");  // 多色卡牌

    companion object {
        fun fromString(color: String?): ManaColor? {
            if (color.isNullOrBlank()) return null
            return when (color.uppercase()) {
                "W" -> WHITE
                "U" -> BLUE
                "B" -> BLACK
                "R" -> RED
                "G" -> GREEN
                else -> null
            }
        }

        fun fromSet(colors: Set<String>?): Set<ManaColor> {
            if (colors.isNullOrEmpty()) return emptySet()
            return colors.mapNotNull { fromString(it) }.toSet()
        }
    }
}

/**
 * 卡牌类型枚举
 */
enum class CardType(val displayName: String) {
    CREATURE("生物"),
    INSTANT("法术"),
    SORCERY("巫术"),
    ENCHANTMENT("结界"),
    ARTIFACT("神器"),
    PLANESWALKER("鹏洛客"),
    LAND("地"),
    OTHER("其他");

    companion object {
        fun fromTypeLine(typeLine: String?): CardType {
            if (typeLine.isNullOrBlank()) return OTHER

            return when {
                // 英文类型
                typeLine.contains("Creature", ignoreCase = true) ||
                // 中文类型
                typeLine.contains("生物", ignoreCase = true) ||
                typeLine.contains("生物") -> CREATURE

                typeLine.contains("Instant", ignoreCase = true) ||
                typeLine.contains("瞬间", ignoreCase = true) ||
                typeLine.contains("瞬间") -> INSTANT

                typeLine.contains("Sorcery", ignoreCase = true) ||
                typeLine.contains("巫术", ignoreCase = true) ||
                typeLine.contains("巫术") -> SORCERY

                typeLine.contains("Enchantment", ignoreCase = true) ||
                typeLine.contains("结界", ignoreCase = true) ||
                typeLine.contains("结界") -> ENCHANTMENT

                typeLine.contains("Artifact", ignoreCase = true) ||
                typeLine.contains("神器", ignoreCase = true) ||
                typeLine.contains("神器") -> ARTIFACT

                typeLine.contains("Planeswalker", ignoreCase = true) ||
                typeLine.contains("鹏洛客", ignoreCase = true) ||
                typeLine.contains("鹏洛客") -> PLANESWALKER

                typeLine.contains("Land", ignoreCase = true) ||
                typeLine.contains("地", ignoreCase = true) ||
                typeLine.contains("地陆") -> LAND

                else -> OTHER
            }
        }
    }
}

/**
 * 稀有度枚举
 */
enum class Rarity(val displayName: String) {
    COMMON("普通"),
    UNCOMMON("非普通"),
    RARE("稀有"),
    MYTHIC("秘稀"),
    SPECIAL("特别");

    companion object {
        fun fromString(rarity: String?): Rarity {
            if (rarity.isNullOrBlank()) return SPECIAL
            return when (rarity.uppercase()) {
                "COMMON", "C" -> COMMON
                "UNCOMMON", "U" -> UNCOMMON
                "RARE", "R" -> RARE
                "MYTHIC", "M" -> MYTHIC
                else -> SPECIAL
            }
        }
    }
}
