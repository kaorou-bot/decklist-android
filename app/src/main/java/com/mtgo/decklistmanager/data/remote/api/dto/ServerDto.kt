package com.mtgo.decklistmanager.data.remote.api.dto

/**
 * 赛事列表响应
 */
data class EventsResponse(
    val success: Boolean,
    val data: EventsData?
)

data class EventsData(
    val events: List<EventDto>,
    val total: Int?,
    val limit: Int?,
    val offset: Int?
)

/**
 * 赛事 DTO
 */
data class EventDto(
    val id: Long,
    val eventName: String,
    val eventType: String?,
    val format: String,
    val date: String,
    val sourceUrl: String?,
    val source: String,
    val deckCount: Int
)

/**
 * 赛事详情响应
 */
data class EventDetailResponse(
    val success: Boolean,
    val data: EventDto?
)

/**
 * 卡组列表响应
 */
data class DecklistsResponse(
    val success: Boolean,
    val data: DecklistsData?
)

data class DecklistsData(
    val decklists: List<DecklistDto>,
    val total: Int?,
    val limit: Int?,
    val offset: Int?
)

/**
 * 卡组 DTO
 */
data class DecklistDto(
    val id: Long,
    val eventId: Long?,
    val eventName: String,
    val deckName: String?,
    val format: String,
    val date: String,
    val playerName: String?,
    val record: String?,
    val url: String?,
    val source: String?
)

/**
 * 卡组详情响应
 */
data class DecklistDetailResponse(
    val success: Boolean,
    val data: DecklistDetailDto?
)

/**
 * 卡组详情 DTO（含卡牌）
 */
data class DecklistDetailDto(
    val id: Long,
    val eventId: Long?,
    val eventName: String,
    val deckName: String?,
    val format: String,
    val date: String,
    val playerName: String?,
    val record: String?,
    val mainDeck: List<CardDto>,
    val sideboard: List<CardDto>
)

/**
 * 卡牌 DTO（用于 decklist 中的简化版本）
 */
data class CardDto(
    val cardName: String,
    val quantity: Int,
    val manaCost: String?,
    val displayName: String?,
    val rarity: String?,
    val color: String?,
    val cardType: String?,
    val cardSet: String?
)

/**
 * 卡牌搜索响应
 */
data class CardSearchResponse(
    val success: Boolean,
    val cards: List<CardInfoDto>?,
    val total: Int?
)

/**
 * 完整卡牌信息 DTO（从 /api/cards/search 返回）
 */
data class CardInfoDto(
    val id: Long,
    val name: String,
    val nameZh: String?,
    val manaCost: String?,
    val cmc: Double?,
    val colors: List<String>?,
    val colorIdentity: List<String>?,
    val typeLine: String?,
    val typeLineZh: String?,           // 中文类型行 ✅
    val oracleText: String?,
    val oracleTextZh: String?,         // 中文规则文本 ✅
    val power: String?,
    val toughness: String?,
    val loyalty: String?,
    val rarity: String?,
    val setCode: String?,
    val setName: String?,
    val setNameZh: String?,            // 中文系列名称 ✅
    val collectorNumber: String?,
    val layout: String?,
    val imageUris: ImageUris?,
    val legalities: Map<String, String>?,
    val scryfallId: String?,
    val oracleId: String?,
    val releasedAt: String?,
    val isDoubleFaced: Boolean?,
    val isToken: Boolean?,
    val faceIndex: Int? = null,        // 面索引（用于双面牌）
    val cardFaces: List<ServerCardFaceDto>? = null  // 卡牌面列表（双面牌、多面牌）✅
)

/**
 * 图片 URIs
 */
data class ImageUris(
    val small: String?,
    val normal: String?,
    val large: String?,
    val png: String?,
    val artCrop: String?,
    val borderCrop: String?
)

/**
 * 卡牌面信息（用于双面牌、多面牌等）- 自有服务器版本
 */
data class ServerCardFaceDto(
    val name: String?,
    val nameZh: String?,
    val manaCost: String?,
    val typeLine: String?,
    val typeLineZh: String?,
    val oracleText: String?,
    val oracleTextZh: String?,
    val power: String?,
    val toughness: String?,
    val loyalty: String?,
    val imageUris: ImageUris?
)
