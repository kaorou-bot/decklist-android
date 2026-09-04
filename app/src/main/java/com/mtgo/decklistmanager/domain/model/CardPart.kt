package com.mtgo.decklistmanager.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * CardPart - 多部分卡牌（历险牌/连体牌/余波牌等）的一个部分
 *
 * 多部分卡牌的所有部分在同一页面展示，不提供翻面切换。
 */
@Parcelize
data class CardPart(
    val name: String? = null,          // 英文名称
    val nameZh: String? = null,        // 中文名称
    val typeLine: String? = null,      // 英文类型行
    val typeLineZh: String? = null,    // 中文类型行
    val manaCost: String? = null,      // 法术力费用
    val oracleText: String? = null,    // 英文规则文本
    val oracleTextZh: String? = null,  // 中文规则文本
    val power: String? = null,
    val toughness: String? = null,
    val loyalty: String? = null
) : Parcelable
