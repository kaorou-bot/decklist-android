package com.mtgo.decklistmanager.data.remote.api.mtgch

import com.google.gson.annotations.SerializedName

/**
 * MTGCH API 搜索响应
 */
data class MtgchSearchResponse(
    @SerializedName("count")
    val count: Int?,

    @SerializedName("next")
    val next: String?,

    @SerializedName("previous")
    val previous: String?,

    @SerializedName("page")
    val page: Int?,

    @SerializedName("page_size")
    val pageSize: Int?,

    @SerializedName("total_pages")
    val totalPages: Int?,

    @SerializedName("items")  // API 返回的是 items 不是 results
    val items: List<MtgchCardDto>?,

    // 保留 results 字段以兼容旧代码
    @SerializedName("results")
    val results: List<MtgchCardDto>?
) {
    // 提供一个便捷属性来获取结果
    val data: List<MtgchCardDto>?
        get() = items ?: results
}

/**
 * MTGCH API 卡牌数据传输对象
 */
data class MtgchCardDto(
    // 基础信息
    @SerializedName("id")
    val id: String?,

    @SerializedName("oracle_id")
    val oracleId: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("face_name")
    val faceName: String?,

    @SerializedName("lang")
    val lang: String?,

    // 中文信息
    @SerializedName("zhs_name")
    val zhsName: String?,

    @SerializedName("zhs_face_name")
    val zhsFaceName: String?,

    @SerializedName("zhs_text")
    val zhsText: String?,

    @SerializedName("zhs_type_line")
    val zhsTypeLine: String?,

    @SerializedName("zhs_flavor_text")
    val zhsFlavorText: String?,

    @SerializedName("zhs_image")
    val zhsImage: String?,

    @SerializedName("zhs_image_uris")
    val zhsImageUris: ImageUris?,

    @SerializedName("pinyin")
    val pinyin: String?,

    // 机器翻译的中文信息（MTGCH 提供的 AI 翻译）
    @SerializedName("atomic_translated_name")
    val atomicTranslatedName: String?,

    @SerializedName("atomic_translated_type")
    val atomicTranslatedType: String?,

    @SerializedName("atomic_translated_text")
    val atomicTranslatedText: String?,

    // 卡牌属性
    @SerializedName("mana_cost")
    val manaCost: String?,

    @SerializedName("cmc")
    val cmc: Int?,

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

    @SerializedName("defense")
    val defense: String?,

    // 颜色
    @SerializedName("colors")
    val colors: List<String>?,

    @SerializedName("color_identity")
    val colorIdentity: List<String>?,

    @SerializedName("color_indicator")
    val colorIndicator: List<String>?,

    // 合法性
    @SerializedName("legalities")
    val legalities: Map<String, String>?,

    // 系列信息
    @SerializedName("set")
    val setCode: String?,

    @SerializedName("set_name")
    val setName: String?,

    @SerializedName("set_translated_name")
    val setTranslatedName: String?,

    @SerializedName("collector_number")
    val collectorNumber: String?,

    @SerializedName("rarity")
    val rarity: String?,

    @SerializedName("released_at")
    val releasedAt: String?,

    @SerializedName("artist")
    val artist: String?,

    // 图片
    @SerializedName("image_uris")
    val imageUris: ImageUris?,

    @SerializedName("layout")
    val layout: String?,

    // 双面牌
    @SerializedName("other_faces")
    val otherFaces: List<MtgchCardDto>?,

    @SerializedName("face_index")
    val faceIndex: Int?,

    // 卡牌背面（针对双面牌）
    @SerializedName("card_faces")
    val cardFaces: List<CardFace>?,

    // 其他
    @SerializedName("keywords")
    val keywords: List<String>?,

    @SerializedName("scryfall_uri")
    val scryfallUri: String?,

    @SerializedName("edhrec_rank")
    val edhrecRank: Int?,

    @SerializedName("penny_rank")
    val pennyRank: Int?
)

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

    @SerializedName("mana_cost")
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

    @SerializedName("image_uris")
    val imageUris: ImageUris?,

    @SerializedName("zh_name")
    val zhName: String?,

    @SerializedName("zh_text")
    val zhText: String?,

    @SerializedName("zh_type_line")
    val zhTypeLine: String?
)
