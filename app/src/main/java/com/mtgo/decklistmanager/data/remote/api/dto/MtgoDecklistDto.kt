package com.mtgo.decklistmanager.data.remote.api.dto

import com.google.gson.annotations.SerializedName

/**
 * MTGO Decklist Link DTO - 牌组链接数据
 */
data class MtgoDecklistLinkDto(
    val url: String,
    val eventName: String,
    val format: String,
    val date: String,
    val eventType: String
)

/**
 * MTGO Decklist Detail DTO - 牌组详情数据
 */
data class MtgoDecklistDetailDto(
    val player: String?,
    val loginid: String?,
    val record: String?,
    val mainDeck: List<MtgoCardDto>,
    val sideboardDeck: List<MtgoCardDto>
)

/**
 * MTGO Card DTO - 卡牌数据
 */
data class MtgoCardDto(
    @SerializedName("qty")
    val quantity: Int,

    @SerializedName("card_attributes")
    val cardAttributes: MtgoCardAttributesDto
)

/**
 * MTGO Card Attributes DTO - 卡牌属性
 */
data class MtgoCardAttributesDto(
    @SerializedName("card_name")
    val cardName: String,

    @SerializedName("cost")
    val manaCost: String?,

    val rarity: String?,
    val color: String?,

    @SerializedName("card_type")
    val cardType: String?,

    @SerializedName("cardset")
    val cardSet: String?
)
