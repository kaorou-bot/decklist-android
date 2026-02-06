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
    INSTANT("瞬间"),
    SORCERY("法术"),
    ENCHANTMENT("结界"),
    ARTIFACT("神器"),
    PLANESWALKER("鹏洛客"),
    LAND("地"),
    KINDRED("亲缘"),
    BATTLE("战役"),
    // 组合类型（当卡牌有多种类型时使用）
    ARTIFACT_CREATURE("神器生物"),
    ARTIFACT_LAND("神器地"),
    LAND_CREATURE("地生物"),
    LAND_ENCHANTMENT("地结界"),
    ENCHANTMENT_CREATURE("结界生物"),
    KINDRED_INSTANT("亲缘瞬间"),
    OTHER("其他");

    companion object {
        /**
         * 从类型行解析卡牌类型
         * 支持组合类型（如"Artifact Land"应计为ARTIFACT_LAND）
         */
        fun fromTypeLine(typeLine: String?): CardType {
            if (typeLine.isNullOrBlank()) return OTHER

            // 检查组合类型
            val hasArtifact = typeLine.contains("Artifact", ignoreCase = true) || typeLine.contains("神器")
            val hasCreature = typeLine.contains("Creature", ignoreCase = true) || typeLine.contains("生物")
            val hasLand = typeLine.contains("Land", ignoreCase = true) || typeLine.contains("地")
            val hasEnchantment = typeLine.contains("Enchantment", ignoreCase = true) || typeLine.contains("结界")
            // Instant 包括老版本的 Interrupt
            val hasInstant = typeLine.contains("Instant", ignoreCase = true) ||
                           typeLine.contains("Interrupt", ignoreCase = true) ||
                           typeLine.contains("瞬间")
            val hasKindred = typeLine.contains("Kindred", ignoreCase = true) ||
                            typeLine.contains("亲缘") ||
                            typeLine.contains("部族") ||
                            typeLine.contains("Tribal", ignoreCase = true)
            val hasBattle = typeLine.contains("Battle", ignoreCase = true) || typeLine.contains("战役")

            // 组合类型判断（按优先级）
            when {
                hasArtifact && hasLand -> return ARTIFACT_LAND
                hasLand && hasCreature -> return LAND_CREATURE
                hasLand && hasEnchantment -> return LAND_ENCHANTMENT
                hasEnchantment && hasCreature -> return ENCHANTMENT_CREATURE
                hasArtifact && hasCreature -> return ARTIFACT_CREATURE
                hasKindred && hasInstant -> return KINDRED_INSTANT
            }

            // 单一类型判断
            return when {
                hasCreature -> CREATURE
                hasInstant -> INSTANT  // 已包含 Interrupt
                // Sorcery 包括老版本的
                typeLine.contains("Sorcery", ignoreCase = true) ||
                typeLine.contains("法术") ||
                typeLine.contains("巫术") -> SORCERY
                hasEnchantment -> ENCHANTMENT
                hasArtifact -> ARTIFACT
                typeLine.contains("Planeswalker", ignoreCase = true) ||
                typeLine.contains("鹏洛客") -> PLANESWALKER
                hasLand -> LAND
                hasKindred -> KINDRED
                hasBattle -> BATTLE
                else -> OTHER
            }
        }

        /**
         * 当类型信息为 null 时，根据卡牌名称推断类型
         * 这主要用于处理基本地等常见卡牌
         */
        fun inferFromCardName(cardName: String): CardType {
            val name = cardName.lowercase()

            // 基本地 - 包括中英文名称
            val basicLands = setOf(
                // 英文
                "plains", "island", "swamp", "mountain", "forest",
                "snow-covered plains", "snow-covered island", "snow-covered swamp",
                "snow-covered mountain", "snow-covered forest",
                // 中文
                "平原", "海岛", "沼泽", "山脉", "森林",
                "积雪的平原", "积雪山岛", "积雪的沼泽", "积雪的山脉", "积雪的森林",
                "森", "岛"  // 简化名称
            )

            // 检索地
            val fetchLands = setOf(
                "flooded strand", "浸水群岛", "polluted delta", "污染三角洲",
                "bloodstained mire", "血斑泥沼", "wooded foothills", "树林 foothills",
                "windswept heath", "风蚀荒原", "arid mesa", "干旱台地",
                "misty rainforest", "雾雨林", "scalding tarn", "沸泉",
                "verdant catacombs", "翠绿墓园", "marsh flats", "沼泽平地",
                "catacombs", "twilight mire", "暮光泥沼", "graven coves", "墓穴湾"
            )

            // 对地
            val dualLands = setOf(
                "underground sea", "地下海", "tropical island", "热带岛",
                "taiga", "泰加", "savannah", "热带草原", "badlands", "荒原",
                "volcanic island", "火山岛", "bayou", "长沼",
                "scrubland", "灌丛", "plateau", "高原",
                "tundra", "冻原"
            )

            // 其他特殊地
            val otherLands = setOf(
                "lorien revealed", "洛汗揭示",
                "wasteland", "荒原",
                "strip mine", "矿坑"
            )

            // 瞬间 - 已知常用于 Vintage 的瞬间
            val knownInstants = setOf(
                "force of will", "意志之力", "force of negation",
                "mental misstep", "心智歧探"
            )

            // 法术 - 已知常用于 Vintage 的法术
            val knownSorceries = setOf(
                "gitaxian probe", "吉拉陆族探针"
            )

            return when {
                name in basicLands || name in fetchLands || name in dualLands || name in otherLands -> LAND
                name in knownInstants -> INSTANT
                name in knownSorceries -> SORCERY
                // 可以根据需要添加更多规则
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
