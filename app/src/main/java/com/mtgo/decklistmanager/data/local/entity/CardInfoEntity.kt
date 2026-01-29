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
        Index(value = ["name"]),
        Index(value = ["en_name"])
    ]
)
data class CardInfoEntity(
    @PrimaryKey
    val id: String, // Scryfall ID

    val name: String,  // 中文名称（优先）或英文名称

    @ColumnInfo(name = "en_name")
    @SerializedName("en_name")
    val enName: String? = null,  // 原始英文名称（用于查询）

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

    @ColumnInfo(name = "is_dual_faced")
    @SerializedName("is_dual_faced")
    val isDualFaced: Boolean = false, // 是否是双面牌

    @ColumnInfo(name = "card_faces_json")
    @SerializedName("card_faces_json")
    val cardFacesJson: String? = null, // 双面牌信息的JSON字符串

    @ColumnInfo(name = "front_face_name")
    @SerializedName("front_face_name")
    val frontFaceName: String? = null, // 正面名称

    @ColumnInfo(name = "back_face_name")
    @SerializedName("back_face_name")
    val backFaceName: String? = null, // 反面名称

    @ColumnInfo(name = "back_face_mana_cost")
    @SerializedName("back_face_mana_cost")
    val backFaceManaCost: String? = null, // 反面法术力值

    @ColumnInfo(name = "back_face_type_line")
    @SerializedName("back_face_type_line")
    val backFaceTypeLine: String? = null, // 反面类型

    @ColumnInfo(name = "back_face_oracle_text")
    @SerializedName("back_face_oracle_text")
    val backFaceOracleText: String? = null, // 反面规则文本

    @ColumnInfo(name = "front_image_uri")
    @SerializedName("front_image_uri")
    val frontImageUri: String? = null, // 正面图片URI

    @ColumnInfo(name = "back_image_uri")
    @SerializedName("back_image_uri")
    val backImageUri: String? = null, // 反面图片URI

    @ColumnInfo(name = "last_updated")
    @SerializedName("last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)
