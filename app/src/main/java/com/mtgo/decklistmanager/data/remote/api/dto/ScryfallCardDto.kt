package com.mtgo.decklistmanager.data.remote.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Scryfall Card DTO - Scryfall API 卡牌数据模型
 */
data class ScryfallCardDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("mana_cost")
    val manaCost: String?,

    @SerializedName("cmc")
    val cmc: Double?,

    @SerializedName("type_line")
    val typeLine: String?,

    @SerializedName("oracle_text")
    val oracleText: String?,

    @SerializedName("colors")
    val colors: List<String>?,

    @SerializedName("color_identity")
    val colorIdentity: List<String>?,

    @SerializedName("power")
    val power: String?,

    @SerializedName("toughness")
    val toughness: String?,

    @SerializedName("loyalty")
    val loyalty: String?,

    @SerializedName("rarity")
    val rarity: String?,

    @SerializedName("set")
    val setCode: String?,

    @SerializedName("set_name")
    val setName: String?,

    @SerializedName("artist")
    val artist: String?,

    @SerializedName("collector_number")
    val cardNumber: String?,

    @SerializedName("legalities")
    val legalities: Map<String, String>?,

    @SerializedName("prices")
    val prices: ScryfallPriceDto?,

    @SerializedName("scryfall_uri")
    val scryfallUri: String?,

    @SerializedName("image_uris")
    val imageUris: ScryfallImageUrisDto?,

    @SerializedName("card_faces")
    val cardFaces: List<CardFaceDto>?
)

/**
 * Scryfall Card Face DTO - 双面牌的每一面
 */
data class CardFaceDto(
    @SerializedName("name")
    val name: String?,

    @SerializedName("mana_cost")
    val manaCost: String?,

    @SerializedName("type_line")
    val typeLine: String?,

    @SerializedName("oracle_text")
    val oracleText: String?,

    @SerializedName("colors")
    val colors: List<String>?,

    @SerializedName("power")
    val power: String?,

    @SerializedName("toughness")
    val toughness: String?,

    @SerializedName("loyalty")
    val loyalty: String?,

    @SerializedName("image_uris")
    val imageUris: ScryfallImageUrisDto?
)

/**
 * Scryfall Price DTO
 */
data class ScryfallPriceDto(
    @SerializedName("usd")
    val usd: String?
)

/**
 * Scryfall Image URIs DTO
 */
data class ScryfallImageUrisDto(
    @SerializedName("small")
    val small: String?,

    @SerializedName("normal")
    val normal: String?,

    @SerializedName("large")
    val large: String?,

    @SerializedName("png")
    val png: String?
)
