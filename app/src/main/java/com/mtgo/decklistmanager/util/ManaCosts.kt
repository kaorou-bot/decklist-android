package com.mtgo.decklistmanager.util

/**
 * 法术力费用规范化工具。
 *
 * Forge 服务端对基本地等无费用卡牌会返回字符串 "no cost"，
 * 客户端统一将其规范化为 null，避免界面直接显示 "no cost" 文本。
 */
object ManaCosts {

    /** "no cost" / 空白 → null，其余原样返回 */
    fun normalize(manaCost: String?): String? {
        if (manaCost.isNullOrBlank()) return null
        if ("no cost".equals(manaCost.trim(), ignoreCase = true)) return null
        return manaCost
    }
}
