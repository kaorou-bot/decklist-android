package com.mtgo.decklistmanager.util

/**
 * 卡牌布局分类工具
 *
 * 万智牌多面/多部分牌分两类，展示逻辑完全不同：
 *
 * 1. 真双面牌（TRUE_DUAL_FACE）：物理上一张牌有正反面，
 *    需要"查看其他部分"翻面按钮 + 切换正反面卡图。
 *    注意 Forge API 的 layout 取值与 Scryfall 不同：
 *    模态双面牌为 "modal"（Scryfall 为 modal_dfc），融合牌为 "meld"。
 *
 * 2. 多部分牌（MULTI_PART）：历险牌、连体牌、余波牌等，
 *    所有部分在同一页面展示，不提供翻面切换、不切换卡图。
 */
object CardLayouts {

    /** 真双面牌布局（需要翻面按钮 + 切换卡图） */
    val TRUE_DUAL_FACE_LAYOUTS = setOf(
        "transform",           // 转化双面牌（如掘密师）
        "modal",               // 模态双面牌（Forge 取值，如艾兰卓之神）
        "modal_dfc",           // 模态双面牌（Scryfall 取值，兼容）
        "meld",                // 融合牌（如吉瑟拉）
        "flip",                // 神河翻转牌（如岩浆奔越鬼）
        "reversible_card",     // 可翻转卡牌
        "double_faced_token",  // 双面衍生物
        "double_sided"         // 双面卡（通用）
    )

    /** 多部分牌布局（同页展示所有部分，不翻面） */
    val MULTI_PART_LAYOUTS = setOf(
        "adventure",           // 历险牌（如厚颜借物灵）
        "split",               // 连体牌（如热火 // 寒冰）
        "aftermath",           // 余波牌（如尽礼 // 尽吞）
        "fuse",                // 融合连体
        "classify",            // 类别牌
        "prototype",           // 原型牌
        "saga"                 // 传纪牌
    )

    fun isTrueDualFace(layout: String?): Boolean =
        layout?.lowercase() in TRUE_DUAL_FACE_LAYOUTS

    fun isMultiPart(layout: String?): Boolean =
        layout?.lowercase() in MULTI_PART_LAYOUTS
}
