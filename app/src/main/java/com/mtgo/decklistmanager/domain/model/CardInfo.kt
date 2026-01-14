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
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable
