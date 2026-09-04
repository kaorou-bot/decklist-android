package com.mtgo.decklistmanager.util

import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto
import com.mtgo.decklistmanager.domain.model.CardInfo
import com.mtgo.decklistmanager.domain.model.CardPart

/**
 * 卡牌详情工具类
 * 统一的卡牌详情显示逻辑，供搜索页面和套牌页面共同使用
 */
object CardDetailHelper {

    /**
     * 从 MtgchCardDto 构建 CardInfo（用于显示卡牌详情）
     *
     * @param mtgchCard MTG API 返回的完整卡牌数据
     * @param cardInfoId 卡牌 ID
     * @param displayName 显示名称（可选，默认使用 mtgchCard 的名称）
     * @param manaCost 法术力费用（可选，默认使用 mtgchCard 的法术力）
     * @param cmc 法术力值（可选，默认使用 mtgchCard 的法术力值）
     * @param typeLine 类型行（可选，默认使用 mtgchCard 的类型行）
     * @param oracleText 规则文本（可选，默认使用 mtgchCard 的规则文本）
     * @param colors 颜色（可选，默认使用 mtgchCard 的颜色）
     * @param power 力量（可选，默认使用 mtgchCard 的力量）
     * @param toughness 防御力（可选，默认使用 mtgchCard 的防御力）
     * @param loyalty 忠诚度（可选，默认使用 mtgchCard 的忠诚度）
     * @param rarity 稀有度（可选，默认使用 mtgchCard 的稀有度）
     * @param setCode 系列代码（可选，默认使用 mtgchCard 的系列代码）
     * @param setName 系列名称（可选，默认使用 mtgchCard 的系列名称）
     * @param artist 画家（可选，默认使用 mtgchCard 的画家）
     * @param collectorNumber 收藏编号（可选，默认使用 mtgchCard 的收藏编号）
     * @param imageUrl 图片 URL（可选）
     *
     * @return 完整的 CardInfo 对象，包含双面牌的所有信息
     */
    fun buildCardInfo(
        mtgchCard: MtgchCardDto,
        cardInfoId: String = mtgchCard.idString ?: mtgchCard.oracleId ?: "",
        displayName: String? = null,
        manaCost: String? = null,
        cmc: Double? = null,
        typeLine: String? = null,
        oracleText: String? = null,
        colors: List<String>? = null,
        power: String? = null,
        toughness: String? = null,
        loyalty: String? = null,
        rarity: String? = null,
        setCode: String? = null,
        setName: String? = null,
        artist: String? = null,
        collectorNumber: String? = null,
        imageUrl: String? = null
    ): CardInfo {
        // 优先使用新的 cardFaces 字段，如果没有再尝试 otherFaces
        val cardFaces = mtgchCard.cardFaces
        val otherFaces = mtgchCard.otherFaces

        // 按 layout 区分两类多面牌：
        // - 真双面牌（transform/modal/meld/flip 等）：翻面按钮 + 切换卡图
        // - 多部分牌（adventure/split/aftermath 等）：同页展示所有部分，不翻面
        val layout = mtgchCard.layout
        val hasMultipleFaces = (cardFaces != null && cardFaces.size >= 2) ||
                (otherFaces != null && otherFaces.isNotEmpty())

        val isDualFaced = CardLayouts.isTrueDualFace(layout) ||
            (layout == null && mtgchCard.isDoubleFaced == true)

        val isMultiPart = if (CardLayouts.isMultiPart(layout)) {
            true
        } else {
            // 兼容：layout 未知但有多面数据，且未判定为双面牌
            layout == null && !isDualFaced && hasMultipleFaces
        }

        // 正面图片 - 优先使用服务器图片
        val frontImageUri = mtgchCard.imageUris?.normal

        // 反面图片 - 仅真双面牌需要（多部分牌不切换卡图）
        val backImageUri = if (isDualFaced) {
            when {
                // 使用 cardFaces 中的 imageUris
                cardFaces != null && cardFaces.size >= 2 -> {
                    AppLogger.d("CardDetailHelper", "Using cardFaces[1].imageUris for back image")
                    cardFaces[1].imageUris?.normal
                }
                // 兼容旧的 otherFaces 字段
                otherFaces != null && otherFaces.isNotEmpty() -> {
                    AppLogger.d("CardDetailHelper", "Using otherFaces[0] for back image")
                    otherFaces[0].zhsImageUris?.normal ?: otherFaces[0].imageUris?.normal
                }
                else -> {
                    AppLogger.d("CardDetailHelper", "No back image source available")
                    null
                }
            }
        } else null

        AppLogger.d("CardDetailHelper", "Card ${mtgchCard.name} - isDualFaced: $isDualFaced, isMultiPart: $isMultiPart")
        AppLogger.d("CardDetailHelper", "cardFaces size: ${cardFaces?.size}, otherFaces size: ${otherFaces?.size}")
        AppLogger.d("CardDetailHelper", "Final backImageUri: $backImageUri")

        // 正面名称（仅真双面牌）
        val frontFaceName = if (isDualFaced) {
            mtgchCard.faceName ?: mtgchCard.zhsFaceName ?: mtgchCard.name
        } else null

        // 反面名称 - 仅真双面牌
        val backFaceName = if (isDualFaced) {
            when {
                cardFaces != null && cardFaces.size >= 2 -> cardFaces[1].zhName ?: cardFaces[1].name
                otherFaces != null && otherFaces.isNotEmpty() ->
                    otherFaces[0].faceName ?: otherFaces[0].nameZh ?: otherFaces[0].name
                else -> null
            }
        } else null

        // 反面法术力 - 仅真双面牌
        val backFaceManaCost = if (isDualFaced) {
            when {
                cardFaces != null && cardFaces.size >= 2 -> cardFaces[1].manaCost
                otherFaces != null && otherFaces.isNotEmpty() -> otherFaces[0].manaCost
                else -> null
            }
        } else null

        // 反面类型 - 仅真双面牌
        val backFaceTypeLine = if (isDualFaced) {
            when {
                cardFaces != null && cardFaces.size >= 2 ->
                    cardFaces[1].zhTypeLine ?: cardFaces[1].typeLine
                otherFaces != null && otherFaces.isNotEmpty() ->
                    otherFaces[0].typeLineZh ?: otherFaces[0].typeLine
                else -> null
            }
        } else null

        // 反面规则文本 - 仅真双面牌
        val backFaceOracleText = if (isDualFaced) {
            when {
                cardFaces != null && cardFaces.size >= 2 ->
                    cardFaces[1].zhText ?: cardFaces[1].oracleText
                otherFaces != null && otherFaces.isNotEmpty() ->
                    otherFaces[0].oracleTextZh ?: otherFaces[0].oracleText
                else -> null
            }
        } else null

        // 反面力量 - 仅真双面牌
        val backFacePower = if (isDualFaced) {
            when {
                cardFaces != null && cardFaces.size >= 2 -> cardFaces[1].power
                otherFaces != null && otherFaces.isNotEmpty() -> otherFaces[0].power
                else -> null
            }
        } else null

        // 反面防御力 - 仅真双面牌
        val backFaceToughness = if (isDualFaced) {
            when {
                cardFaces != null && cardFaces.size >= 2 -> cardFaces[1].toughness
                otherFaces != null && otherFaces.isNotEmpty() -> otherFaces[0].toughness
                else -> null
            }
        } else null

        // 反面忠诚度 - 仅真双面牌
        val backFaceLoyalty = if (isDualFaced) {
            when {
                cardFaces != null && cardFaces.size >= 2 -> cardFaces[1].loyalty
                otherFaces != null && otherFaces.isNotEmpty() -> otherFaces[0].loyalty
                else -> null
            }
        } else null

        // 多部分牌（历险/连体/余波等）：提取所有部分，同页展示
        val multiParts: List<CardPart>? = if (isMultiPart) {
            cardFaces?.takeIf { it.isNotEmpty() }?.map { face ->
                CardPart(
                    name = face.name,
                    nameZh = face.zhName,
                    typeLine = face.typeLine,
                    typeLineZh = face.zhTypeLine,
                    manaCost = face.manaCost,
                    oracleText = face.oracleText,
                    oracleTextZh = face.zhText,
                    power = face.power,
                    toughness = face.toughness,
                    loyalty = face.loyalty
                )
            } ?: otherFaces?.takeIf { it.isNotEmpty() }?.map { face ->
                CardPart(
                    name = face.name,
                    nameZh = face.nameZh,
                    typeLine = face.typeLine,
                    typeLineZh = face.typeLineZh,
                    manaCost = face.manaCost,
                    oracleText = face.oracleText,
                    oracleTextZh = face.oracleTextZh,
                    power = face.power,
                    toughness = face.toughness,
                    loyalty = face.loyalty
                )
            }
        } else null

        // 获取中文名称（优先使用新字段 nameZh）
        val getZhsName = mtgchCard.nameZh ?: mtgchCard.atomicTranslatedName

        // 获取中文类型行（优先使用新字段 typeLineZh）
        val getTypeLineZh = mtgchCard.typeLineZh ?: mtgchCard.atomicTranslatedType

        // 获取中文规则文本（优先使用新字段 oracleTextZh）
        val getOracleTextZh = mtgchCard.oracleTextZh ?: mtgchCard.atomicTranslatedText

        return CardInfo(
            id = cardInfoId,
            oracleId = mtgchCard.oracleId, // 设置 Oracle ID
            name = displayName ?: (getZhsName ?: mtgchCard.name ?: ""),
            manaCost = ManaCosts.normalize(manaCost ?: mtgchCard.manaCost),
            cmc = cmc ?: mtgchCard.cmc?.toDouble(),
            typeLine = typeLine ?: (getTypeLineZh ?: mtgchCard.typeLine),
            oracleText = oracleText ?: (getOracleTextZh ?: mtgchCard.oracleText),
            colors = colors ?: mtgchCard.colors,
            colorIdentity = mtgchCard.colorIdentity,
            power = power ?: mtgchCard.power,
            toughness = toughness ?: mtgchCard.toughness,
            loyalty = loyalty ?: mtgchCard.loyalty,
            rarity = rarity ?: mtgchCard.rarity,
            setCode = setCode ?: mtgchCard.setCode,
            setName = setName ?: (mtgchCard.setNameZh ?: mtgchCard.setTranslatedName ?: mtgchCard.setName),
            artist = artist ?: mtgchCard.artist,
            cardNumber = collectorNumber ?: mtgchCard.collectorNumber,
            legalStandard = mtgchCard.legalities?.get("standard"),
            legalModern = mtgchCard.legalities?.get("modern"),
            legalPioneer = mtgchCard.legalities?.get("pioneer"),
            legalLegacy = mtgchCard.legalities?.get("legacy"),
            legalVintage = mtgchCard.legalities?.get("vintage"),
            legalCommander = mtgchCard.legalities?.get("commander"),
            legalPauper = mtgchCard.legalities?.get("pauper"),
            priceUsd = null,
            scryfallUri = mtgchCard.scryfallUri,
            imagePath = null,
            imageUriSmall = mtgchCard.zhsImageUris?.small ?: mtgchCard.imageUris?.small,
            imageUriNormal = imageUrl ?: frontImageUri,
            imageUriLarge = mtgchCard.zhsImageUris?.large ?: mtgchCard.imageUris?.large,
            isDualFaced = isDualFaced,
            frontFaceName = frontFaceName,
            backFaceName = backFaceName,
            frontImageUri = frontImageUri,
            backImageUri = backImageUri,
            backFaceManaCost = backFaceManaCost,
            backFaceTypeLine = backFaceTypeLine,
            backFaceOracleText = backFaceOracleText,
            backFacePower = backFacePower,
            backFaceToughness = backFaceToughness,
            backFaceLoyalty = backFaceLoyalty,
            isMultiPart = isMultiPart,
            multiParts = multiParts
        )
    }
}
