package com.mtgo.decklistmanager.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * CardInfo Entity - 单卡信息缓存表（Scryfall 数据）
 */
@Entity(
    tableName = "card_info",
    indices = [
        Index(value = ["name"])
    ]
)
data class CardInfoEntity(
    @PrimaryKey
    val id: String, // Scryfall ID

    val name: String,

    @ColumnInfo(name = "mana_cost")
    @SerializedName("mana_cost")
    val manaCost: String?,

    val cmc: Double?,

    @ColumnInfo(name = "type_line")
    @SerializedName("type_line")
    val typeLine: String?,

    @ColumnInfo(name = "oracle_text")
    @SerializedName("oracle_text")
    val oracleText: String?,

    val colors: String?, // Comma-separated: "R,G"

    @ColumnInfo(name = "color_identity")
    @SerializedName("color_identity")
    val colorIdentity: String?,

    val power: String?,
    val toughness: String?,
    val loyalty: String?,

    val rarity: String?,

    @ColumnInfo(name = "set_code")
    @SerializedName("set_code")
    val setCode: String?,

    @ColumnInfo(name = "set_name")
    @SerializedName("set_name")
    val setName: String?,

    val artist: String?,

    @ColumnInfo(name = "card_number")
    @SerializedName("card_number")
    val cardNumber: String?,

    @ColumnInfo(name = "legal_standard")
    @SerializedName("legal_standard")
    val legalStandard: String?,

    @ColumnInfo(name = "legal_modern")
    @SerializedName("legal_modern")
    val legalModern: String?,

    @ColumnInfo(name = "legal_pioneer")
    @SerializedName("legal_pioneer")
    val legalPioneer: String?,

    @ColumnInfo(name = "legal_legacy")
    @SerializedName("legal_legacy")
    val legalLegacy: String?,

    @ColumnInfo(name = "legal_vintage")
    @SerializedName("legal_vintage")
    val legalVintage: String?,

    @ColumnInfo(name = "legal_commander")
    @SerializedName("legal_commander")
    val legalCommander: String?,

    @ColumnInfo(name = "legal_pauper")
    @SerializedName("legal_pauper")
    val legalPauper: String?,

    @ColumnInfo(name = "price_usd")
    @SerializedName("price_usd")
    val priceUsd: String?,

    @ColumnInfo(name = "scryfall_uri")
    @SerializedName("scryfall_uri")
    val scryfallUri: String?,

    @ColumnInfo(name = "image_path")
    @SerializedName("image_path")
    val imagePath: String?,

    @ColumnInfo(name = "image_uri_small")
    @SerializedName("image_uri_small")
    val imageUriSmall: String?,

    @ColumnInfo(name = "image_uri_normal")
    @SerializedName("image_uri_normal")
    val imageUriNormal: String?,

    @ColumnInfo(name = "image_uri_large")
    @SerializedName("image_uri_large")
    val imageUriLarge: String?,

    @ColumnInfo(name = "last_updated")
    @SerializedName("last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)
