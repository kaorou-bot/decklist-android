package com.mtgo.decklistmanager.data.remote.api.mtgch

import com.google.gson.annotations.SerializedName

/**
 * MTG Card Server API 搜索响应
 *
 * 自有服务端 API - 新版数据结构
 * API 文档: 见项目根目录 API_DOCUMENTATION.md
 *
 * 响应格式:
 * {
 *   "success": true,
 *   "cards": [...],
 *   "total": 67
 * }
 */
data class MtgchSearchResponse(
    @SerializedName("success")
    val success: Boolean?,

    @SerializedName("cards")
    val cards: List<MtgchCardDto>?,

    @SerializedName("total")
    val total: Int?
) {
    // 提供一个便捷属性来获取结果
    val data: List<MtgchCardDto>?
        get() = cards ?: emptyList()
}

/**
 * MTG Card Server API 卡牌数据传输对象
 *
 * 新版自有服务端 API 数据结构
 * 支持中英文卡牌信息、双面牌、图片等
 *
 * API 文档: API_DOCUMENTATION.md
 *
 * 主要字段映射:
 * - 中文名称: nameZh (95.67% 覆盖率)
 * - 中文类型: typeLineZh (96.94% 覆盖率)
 * - 中文规则: oracleTextZh (70.86% 覆盖率)
 * - 系列信息: setCode, setName, collectorNumber
 * - 标识符: oracleId (同名牌共享), scryfallId (每张唯一)
 * - 双面牌: isDoubleFaced, cardFaces
 */
data class MtgchCardDto(
    // 基础信息
    @SerializedName("id")
    val id: Int?,              // 数据库内部 ID (number 类型)

    @SerializedName("oracleId")
    val oracleId: String?,     // Oracle ID (同名牌共享)

    @SerializedName("scryfallId")
    val scryfallId: String?,   // Scryfall ID (每张唯一)

    @SerializedName("name")
    val name: String?,         // 卡牌名称（英文，100% 覆盖）

    @SerializedName("nameZh")
    val nameZh: String?,       // 卡牌名称（中文，95.67% 覆盖）

    @SerializedName("manaCost")
    val manaCost: String?,     // 法术力费用（如 "{R}"）

    @SerializedName("cmc")
    val cmc: Int?,             // 转换法术力值

    // 颜色
    @SerializedName("colors")
    val colors: List<String>?, // 颜色数组

    @SerializedName("colorIdentity")
    val colorIdentity: List<String>?, // 颜色标识

    // 类型
    @SerializedName("typeLine")
    val typeLine: String?,     // 类型行（英文，100% 覆盖）

    @SerializedName("typeLineZh")
    val typeLineZh: String?,   // 类型行（中文，96.94% 覆盖）

    // 规则文本
    @SerializedName("oracleText")
    val oracleText: String?,   // 规则文本（英文，93.6% 覆盖）

    @SerializedName("oracleTextZh")
    val oracleTextZh: String?, // 规则文本（中文，70.86% 覆盖）

    // 攻防忠诚度
    @SerializedName("power")
    val power: String?,        // 力量

    @SerializedName("toughness")
    val toughness: String?,    // 防御

    @SerializedName("loyalty")
    val loyalty: String?,      // 忠诚度（鹏洛客）

    // 稀有度
    @SerializedName("rarity")
    val rarity: String?,       // common, uncommon, rare, mythic

    // 系列信息
    @SerializedName("setCode")
    val setCode: String?,      // 系列代码（如 "lea", "NEO"）

    @SerializedName("setName")
    val setName: String?,      // 系列名称（英文，如 "Limited Edition Alpha"）

    @SerializedName("collectorNumber")
    val collectorNumber: String?, // 收藏编号

    @SerializedName("releasedAt")
    val releasedAt: String?,   // 发布日期（ISO 8601）

    // 图片
    @SerializedName("imageUris")
    val imageUris: ImageUris?, // 图片 URL 对象

    // 布局和双面牌
    @SerializedName("layout")
    val layout: String?,       // 卡牌布局

    @SerializedName("isDoubleFaced")
    val isDoubleFaced: Boolean?, // 是否双面牌

    @SerializedName("isToken")
    val isToken: Boolean?,     // 是否衍生物

    // 双面牌的两个面
    @SerializedName("cardFaces")
    val cardFaces: List<CardFace>?,

    // ========== 以下字段保留用于兼容旧数据结构 ==========

    @SerializedName("faceName")
    val faceName: String?,     // 面名称（旧字段）

    @SerializedName("lang")
    val lang: String?,         // 语言（旧字段）

    @SerializedName("zhsFaceName")
    val zhsFaceName: String?,  // 中文面名称（旧字段）

    @SerializedName("zhsImage")
    val zhsImage: String?,     // 中文图片 URL（旧字段）

    @SerializedName("zhsImageUris")
    val zhsImageUris: ImageUris?, // 中文图片 URLs（旧字段）

    @SerializedName("atomicTranslatedName")
    val atomicTranslatedName: String?, // AI 翻译中文名（旧字段）

    @SerializedName("atomicTranslatedType")
    val atomicTranslatedType: String?, // AI 翻译中文类型（旧字段）

    @SerializedName("atomicTranslatedText")
    val atomicTranslatedText: String?, // AI 翻译中文规则（旧字段）

    @SerializedName("defense")
    val defense: String?,      // 防御值（战斗）

    @SerializedName("colorIndicator")
    val colorIndicator: List<String>?, // 颜色指示器

    @SerializedName("legalities")
    val legalities: Map<String, String>?, // 合法性

    @SerializedName("setNameZh")
    val setNameZh: String?,      // 系列中文名（新字段）

    @SerializedName("setTranslatedName")
    val setTranslatedName: String?, // 系列中文名（旧字段，兼容）

    @SerializedName("artist")
    val artist: String?,       // 画家

    @SerializedName("otherFaces")
    val otherFaces: List<MtgchCardDto>?, // 其他面（旧字段）

    @SerializedName("faceIndex")
    val faceIndex: Int?,       // 面索引（旧字段）

    @SerializedName("keywords")
    val keywords: List<String>?, // 关键词

    @SerializedName("scryfallUri")
    val scryfallUri: String?,  // Scryfall URI

    @SerializedName("edhrecRank")
    val edhrecRank: Int?,

    @SerializedName("pennyRank")
    val pennyRank: Int?,

    // 兼容：旧的 ID 字段（可能是 String）
    @SerializedName("old_id")
    val oldId: String?
) {
    // 兼容旧代码：提供 id 字段的 String 版本
    val idString: String?
        get() = oldId ?: id?.toString()
}

/**
 * 图片 URLs
 */
data class ImageUris(
    @SerializedName("small")
    val small: String?,

    @SerializedName("normal")
    val normal: String?,

    @SerializedName("large")
    val large: String?,

    @SerializedName("png")
    val png: String?,

    @SerializedName("art_crop")
    val artCrop: String?,

    @SerializedName("border_crop")
    val borderCrop: String?
)

/**
 * 卡牌面（用于双面牌）
 */
data class CardFace(
    @SerializedName("name")
    val name: String?,

    @SerializedName("face_name")
    val faceName: String?,

    @SerializedName("manaCost")
    val manaCost: String?,

    @SerializedName("type_line")
    val typeLine: String?,

    @SerializedName("oracle_text")
    val oracleText: String?,

    @SerializedName("power")
    val power: String?,

    @SerializedName("toughness")
    val toughness: String?,

    @SerializedName("loyalty")
    val loyalty: String?,

    @SerializedName("colors")
    val colors: List<String>?,

    @SerializedName("imageUris")
    val imageUris: ImageUris?,

    @SerializedName("nameZh")
    val zhName: String?,

    @SerializedName("oracleTextZh")
    val zhText: String?,

    @SerializedName("typeLineZh")
    val zhTypeLine: String?
)
