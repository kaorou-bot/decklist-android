package com.mtgo.decklistmanager.data.remote.api.dto

/**
 * MTGTop8 牌组链接 DTO
 */
data class MtgTop8DecklistDto(
    val deckId: String,           // 牌组唯一ID
    val deckName: String,         // 牌组名称
    val playerName: String,       // 玩家名称
    val eventName: String,        // 比赛名称
    val eventDate: String,        // 比赛日期
    val format: String,           // 格式代码 (MO, LE, ST, etc.)
    val url: String,             // 牌组页面URL
    val record: String = ""       // 比赛成绩/排名 (#1, #2, etc.)
)

/**
 * MTGTop8 牌组详情 DTO
 */
data class MtgTop8DecklistDetailDto(
    val mainDeck: List<MtgTop8CardDto>,       // 主牌列表
    val sideboardDeck: List<MtgTop8CardDto>    // 备牌列表
) {
    /**
     * MTGTop8 单卡 DTO
     */
    data class MtgTop8CardDto(
        val quantity: Int,        // 数量
        val name: String          // 卡牌名称
    )
}
