package com.mtgo.decklistmanager.data.remote.api.dto

/**
 * MTGTop8 赛事 DTO
 * 代表从 MTGTop8 格式页面解析出的赛事信息
 */
data class MtgTop8EventDto(
    val eventId: String,        // 赛事唯一ID (来自 URL 参数 e=XXX)
    val eventName: String,      // 赛事名称 (如 "MTGO Challenge 32")
    val eventDate: String,      // 赛事日期 (格式: YYYY-MM-DD)
    val format: String,         // 格式 (Modern, Standard, Legacy, etc.)
    val eventUrl: String,       // 赛事页面 URL
    val deckCount: Int = 0,     // 该赛事包含的卡组数量 (初始为0，后续更新)
    val eventType: String? = null,  // 赛事类型 (Challenge, League, Tournament, etc.)
    val sourceUrl: String = eventUrl,  // 来源 URL (与 eventUrl 相同)
    val source: String = "MTGTop8"  // 来源 (默认 MTGTop8)
)

/**
 * MTGTop8 赛事卡组列表 DTO
 * 代表一个赛事下的所有卡组信息
 */
data class MtgTop8EventDecklistsDto(
    val event: MtgTop8EventDto,           // 赛事信息
    val decklists: List<MtgTop8DecklistDto>  // 该赛事下的所有卡组
)
