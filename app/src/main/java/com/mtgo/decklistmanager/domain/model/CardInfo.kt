package com.mtgo.decklistmanager.domain.model

import android.os.Parcelable
import com.mtgo.decklistmanager.data.local.entity.CardInfoEntity
import kotlinx.parcelize.Parcelize

/**
 * CardInfo - 单卡详细信息领域模型
 */
@Parcelize
data class CardInfo(
    val id: String, // Scryfall ID
    val oracleId: String? = null, // Oracle ID (用于获取印刷版本)
    val name: String,
    val enName: String? = null, // 英文名称（用于API搜索）
    val manaCost: String?,
    val cmc: Double?,
    val typeLine: String?,
    val oracleText: String?,
    val colors: List<String>?,
    val colorIdentity: List<String>?,
    val power: String?,
    val toughness: String?,
    val loyalty: String?,
    val rarity: String?,
    val setCode: String?,
    val setName: String?,
    val artist: String?,
    val cardNumber: String?,
    val legalStandard: String?,
    val legalModern: String?,
    val legalPioneer: String?,
    val legalLegacy: String?,
    val legalVintage: String?,
    val legalCommander: String?,
    val legalPauper: String?,
    val priceUsd: String?,
    val scryfallUri: String?,
    val imagePath: String?,
    val imageUriSmall: String?,
    val imageUriNormal: String?,
    val imageUriLarge: String?,
    val lastUpdated: Long = System.currentTimeMillis(),

    // 双面牌相关字段
    val isDualFaced: Boolean = false, // 是否是双面牌
    val frontFaceName: String? = null, // 正面名称
    val backFaceName: String? = null, // 反面名称
    val frontImageUri: String? = null, // 正面图片URI
    val backImageUri: String? = null, // 反面图片URI
    val backFaceManaCost: String? = null, // 反面法术力值
    val backFaceTypeLine: String? = null, // 反面类型
    val backFaceOracleText: String? = null, // 反面规则文本
    val backFacePower: String? = null, // 反面力量
    val backFaceToughness: String? = null, // 反面防御力
    val backFaceLoyalty: String? = null // 反面忠诚度
) : Parcelable {

    /**
     * 转换为 Entity 用于数据库存储
     */
    fun toEntity() = CardInfoEntity(
        id = id,
        oracleId = oracleId,
        name = name,
        enName = enName, // 保存英文名用于搜索
        manaCost = manaCost,
        cmc = cmc,
        typeLine = typeLine,
        oracleText = oracleText,
        colors = colors?.joinToString(","),
        colorIdentity = colorIdentity?.joinToString(","),
        power = power,
        toughness = toughness,
        loyalty = loyalty,
        rarity = rarity,
        setCode = setCode,
        setName = setName,
        artist = artist,
        cardNumber = cardNumber,
        legalStandard = legalStandard,
        legalModern = legalModern,
        legalPioneer = legalPioneer,
        legalLegacy = legalLegacy,
        legalVintage = legalVintage,
        legalCommander = legalCommander,
        legalPauper = legalPauper,
        priceUsd = priceUsd,
        scryfallUri = scryfallUri,
        imagePath = imagePath,
        imageUriSmall = imageUriSmall,
        imageUriNormal = imageUriNormal,
        imageUriLarge = imageUriLarge,
        isDualFaced = isDualFaced,
        cardFacesJson = null, // 不保存 JSON
        frontFaceName = frontFaceName,
        backFaceName = backFaceName,
        backFaceManaCost = backFaceManaCost,
        backFaceTypeLine = backFaceTypeLine,
        backFaceOracleText = backFaceOracleText,
        backFacePower = backFacePower,
        backFaceToughness = backFaceToughness,
        backFaceLoyalty = backFaceLoyalty,
        frontImageUri = frontImageUri,
        backImageUri = backImageUri,
        lastUpdated = lastUpdated
    )
}
