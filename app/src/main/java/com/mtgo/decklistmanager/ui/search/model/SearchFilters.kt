package com.mtgo.decklistmanager.ui.search.model

/**
 * 颜色匹配模式
 * 参考: MTGCH 高级搜索的颜色逻辑选择
 */
enum class ColorMatchMode {
    /** 正好为所选颜色 (c=wu) */
    EXACT,

    /** 至多为所选颜色 (c<=wu) */
    AT_MOST,

    /** 至少包括所选颜色 (c>=wu) */
    AT_LEAST
}

/**
 * 比较运算符
 * 用于数值筛选（法术力值、力量、防御力）
 */
enum class CompareOperator(val symbol: String) {
    /** 等于 */
    EQUAL("="),

    /** 大于 */
    GREATER(">"),

    /** 小于 */
    LESS("<"),

    /** 大于等于 */
    GREATER_EQUAL(">="),

    /** 小于等于 */
    LESS_EQUAL("<="),

    /** 任意（不筛选） */
    ANY("");

    companion object {
        fun fromSymbol(symbol: String?): CompareOperator {
            return values().find { it.symbol == symbol } ?: ANY
        }
    }
}

/**
 * 合法性模式
 * 参考: MTGCH 赛制可用性筛选
 */
enum class LegalityMode(val value: String) {
    /** 合法 */
    LEGAL("legal"),

    /** 禁用 */
    BANNED("banned"),

    /** 限用 */
    RESTRICTED("restricted")
}

/**
 * 游戏平台
 * 参考: MTGCH 游戏平台筛选
 */
enum class GamePlatform(val displayName: String, val value: String) {
    /** 纸牌 */
    PAPER("实体卡牌", "paper"),

    /** 万智牌在线版 */
    MTGO("万智牌在线版(MTGO)", "mtgo"),

    /** 万智牌竞技场 */
    ARENA("万智牌竞技场", "arena")
}

/**
 * 数值筛选器
 * 用于法术力值、力量、防御力筛选
 */
data class NumericFilter(
    val operator: CompareOperator = CompareOperator.ANY,
    val value: Int = 0
) {
    val isActive: Boolean
        get() = operator != CompareOperator.ANY && value >= 0

    fun toQueryPart(field: String): String {
        return if (isActive) {
            "$field${operator.symbol}$value"
        } else {
            ""
        }
    }
}

/**
 * 完整的搜索过滤器
 * 完全复制 MTGCH 高级搜索的所有字段
 * 参考: https://mtgch.com/search
 */
data class SearchFilters(
    // ==================== 基础字段 ====================
    /** 卡牌名称 */
    val name: String? = null,

    // ==================== 规则与类型 ====================
    /** 规则概述/Oracle 文本 (o, text, oracle) */
    val oracleText: String? = null,

    /** 卡牌类型 (t, type) */
    val type: String? = null,

    // ==================== 颜色筛选 ====================
    /** 颜色列表 (c:wubrg) */
    val colors: List<String> = emptyList(),

    /** 颜色匹配模式 */
    val colorMode: ColorMatchMode = ColorMatchMode.AT_LEAST,

    /** 颜色标识 (ci:wubrg) */
    val colorIdentity: List<String>? = null,

    /** 是否搜索标识色（开关） */
    val searchColorIdentity: Boolean = false,

    // ==================== 数值筛选 ====================
    /** 法术力值 (mv, cmc, mana_value) */
    val manaValue: NumericFilter? = null,

    /** 力量 (po, power) */
    val power: NumericFilter? = null,

    /** 防御力 (to, toughness) */
    val toughness: NumericFilter? = null,

    // ==================== 赛制与系列 ====================
    /** 赛制 (f:modern) */
    val format: Format? = null,

    /** 可用性 (legal/banned/restricted) */
    val legality: LegalityMode? = null,

    /** 系列代码 (s:MOM) */
    val setCode: String? = null,

    // ==================== 其他属性 ====================
    /** 稀有度列表 (r:mythic) */
    val rarities: List<String> = emptyList(),

    /** 背景叙述 (ft, flavor_text) */
    val flavorText: String? = null,

    /** 画师 (a, artist) */
    val artist: String? = null,

    /** 游戏平台 */
    val game: GamePlatform? = null,

    /** 包含额外卡牌（命令者、Token等） */
    val includeExtras: Boolean = false
) {
    /**
     * 检查是否有任何有效的筛选条件
     */
    val hasActiveFilters: Boolean
        get() = name?.isNotBlank() == true ||
                oracleText?.isNotBlank() == true ||
                type?.isNotBlank() == true ||
                colors.isNotEmpty() ||
                (searchColorIdentity && colorIdentity?.isNotEmpty() == true) ||
                manaValue?.isActive == true ||
                power?.isActive == true ||
                toughness?.isActive == true ||
                format != null ||
                setCode?.isNotBlank() == true ||
                rarities.isNotEmpty() ||
                flavorText?.isNotBlank() == true ||
                artist?.isNotBlank() == true ||
                game != null ||
                includeExtras

    /**
     * 获取颜色字符串 (如 "wu", "wubrg")
     */
    fun getColorString(): String {
        return colors.sortedBy { mapColorToOrder(it) }.joinToString("")
    }

    /**
     * 获取颜色标识字符串 (如 "wub", "ur")
     */
    fun getColorIdentityString(): String {
        return colorIdentity?.sortedBy { mapColorToOrder(it) }?.joinToString("") ?: ""
    }

    /**
     * 颜色排序（WUBRG顺序）
     */
    private fun mapColorToOrder(color: String): Int {
        return when (color.lowercase()) {
            "w" -> 1
            "u" -> 2
            "b" -> 3
            "r" -> 4
            "g" -> 5
            "c" -> 6
            else -> 99
        }
    }

    /**
     * 创建空筛选器
     */
    companion object {
        fun empty() = SearchFilters()
    }
}

/**
 * 赛制枚举
 */
enum class Format(val displayName: String, val value: String) {
    STANDARD("标准赛制", "standard"),
    PIONEER("先驱赛制", "pioneer"),
    MODERN("摩登赛制", "modern"),
    LEGACY("薪传赛制", "legacy"),
    VINTAGE("复古赛制", "vintage"),
    COMMANDER("指挥官赛制", "commander"),
    PAUPER("穷神赛制", "pauper"),
    BRAWL("乱斗赛制", "brawl"),
    HISTORIC("历史赛制", "historic"),
    EXPLORER("探险赛制", "explorer"),
    TIMELESS("永恒赛制", "timeless");

    companion object {
        fun fromValue(value: String?): Format? {
            return values().find { it.value == value }
        }
    }
}

/**
 * 稀有度枚举
 */
enum class Rarity(val displayName: String, val value: String) {
    COMMON("普通", "common"),
    UNCOMMON("非普通", "uncommon"),
    RARE("稀有", "rare"),
    MYTHIC("秘稀", "mythic"),
    SPECIAL("特殊", "special")
}

/**
 * 颜色枚举
 */
enum class Color(val symbol: String, val displayName: String, val code: String) {
    WHITE("W", "白色", "w"),
    BLUE("U", "蓝色", "u"),
    BLACK("B", "黑色", "b"),
    RED("R", "红色", "r"),
    GREEN("G", "绿色", "g"),
    COLORLESS("C", "无色", "c");

    companion object {
        fun fromCode(code: String): Color? {
            return values().find { it.code == code.lowercase() }
        }
    }
}
