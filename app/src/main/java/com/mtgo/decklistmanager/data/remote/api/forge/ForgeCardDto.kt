package com.mtgo.decklistmanager.data.remote.api.forge

import com.google.gson.annotations.SerializedName

/**
 * Forge 中文卡查 API 数据传输对象
 *
 * 字段命名遵循服务端 snake_case 规范（openapi.yaml）。
 * 不存在的数值使用 JSON null。
 */

/** GET /meta 响应 */
data class ForgeMetaDto(
    @SerializedName("api_version") val apiVersion: String?,
    @SerializedName("data_version") val dataVersion: String?,
    @SerializedName("base_url") val baseUrl: String?,
    @SerializedName("image_base_url") val imageBaseUrl: String?,
    @SerializedName("authentication") val authentication: String?,
    @SerializedName("cards") val cards: Int?,
    @SerializedName("printings") val printings: Int?,
    @SerializedName("sets") val sets: Int?
)

/** 分页包装（cards / sets 共用） */
data class ForgeCardPageDto(
    @SerializedName("page") val page: Int?,
    @SerializedName("page_size") val pageSize: Int?,
    @SerializedName("total") val total: Int?,
    @SerializedName("total_pages") val totalPages: Int?,
    @SerializedName("items") val items: List<ForgeCardDto>?
)

data class ForgeSetPageDto(
    @SerializedName("page") val page: Int?,
    @SerializedName("page_size") val pageSize: Int?,
    @SerializedName("total") val total: Int?,
    @SerializedName("total_pages") val totalPages: Int?,
    @SerializedName("items") val items: List<ForgeSetDto>?
)

/**
 * 卡牌摘要（搜索结果条目）
 *
 * 详情接口在此基础上追加 layout/oracle_text/printings 等字段，
 * 因此详情响应也可直接用本类反序列化（多余字段被忽略）。
 */
data class ForgeCardDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("name_zh") val nameZh: String?,
    @SerializedName("mana_cost") val manaCost: String?,
    @SerializedName("mana_value") val manaValue: Int?,
    @SerializedName("colors") val colors: List<String>?,
    @SerializedName("color_identity") val colorIdentity: List<String>?,
    @SerializedName("type_line") val typeLine: String?,
    @SerializedName("type_line_zh") val typeLineZh: String?,
    @SerializedName("set_code") val setCode: String?,
    @SerializedName("set_name") val setName: String?,
    @SerializedName("set_name_zh") val setNameZh: String?,
    @SerializedName("collector_number") val collectorNumber: String?,
    @SerializedName("rarity") val rarity: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("back_image_url") val backImageUrl: String?,
    @SerializedName("faces") val faces: List<ForgeFaceDto>?,

    // ===== 详情接口追加字段 =====
    @SerializedName("layout") val layout: String?,
    @SerializedName("oracle_text") val oracleText: String?,
    @SerializedName("oracle_text_zh") val oracleTextZh: String?,
    @SerializedName("power") val power: String?,
    @SerializedName("toughness") val toughness: String?,
    @SerializedName("loyalty") val loyalty: String?,
    @SerializedName("defense") val defense: String?,
    @SerializedName("printings") val printings: List<ForgePrintingDto>?
) {
    /**
     * 是否真双面牌：
     * - 详情接口带 layout：按 CardLayouts 分类（transform/modal/meld/flip 等）
     * - 搜索接口无 layout：以 back_image_url 非空作为真双面牌特征
     *   （历险/连体等多部分牌 back_image_url 为 null）
     */
    val isDoubleFaced: Boolean
        get() = com.mtgo.decklistmanager.util.CardLayouts.isTrueDualFace(layout) ||
                (layout == null && backImageUrl != null)
}

/** 卡牌的一个面 */
data class ForgeFaceDto(
    @SerializedName("name") val name: String?,
    @SerializedName("name_zh") val nameZh: String?,
    @SerializedName("mana_cost") val manaCost: String?,
    @SerializedName("mana_value") val manaValue: Int?,
    @SerializedName("colors") val colors: List<String>?,
    @SerializedName("type_line") val typeLine: String?,
    @SerializedName("type_line_zh") val typeLineZh: String?,
    @SerializedName("oracle_text") val oracleText: String?,
    @SerializedName("oracle_text_zh") val oracleTextZh: String?,
    @SerializedName("power") val power: String?,
    @SerializedName("toughness") val toughness: String?,
    @SerializedName("loyalty") val loyalty: String?,
    @SerializedName("defense") val defense: String?,
    @SerializedName("image_url") val imageUrl: String?
)

/** 印刷版本 */
data class ForgePrintingDto(
    @SerializedName("set_code") val setCode: String?,
    @SerializedName("set_name") val setName: String?,
    @SerializedName("set_name_zh") val setNameZh: String?,
    @SerializedName("collector_number") val collectorNumber: String?,
    @SerializedName("rarity") val rarity: String?,
    @SerializedName("artist") val artist: String?,
    @SerializedName("art_index") val artIndex: Int?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("back_image_url") val backImageUrl: String?
)

/** 系列 */
data class ForgeSetDto(
    @SerializedName("code") val code: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("name_zh") val nameZh: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("card_count") val cardCount: Int?
)

/** 错误响应 */
data class ForgeErrorDto(
    @SerializedName("error") val error: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("request_id") val requestId: String?
)
