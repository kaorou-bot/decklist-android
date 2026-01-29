package com.mtgo.decklistmanager.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * CardInfo - 单卡详细信息领域模型
 */
@Parcelize
data class CardInfo(
    val id: String, // Scryfall ID
    val name: String,
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
    val backFaceOracleText: String? = null // 反面规则文本
) : Parcelable
