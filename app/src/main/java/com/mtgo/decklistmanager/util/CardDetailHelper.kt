package com.mtgo.decklistmanager.util

import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto
import com.mtgo.decklistmanager.domain.model.CardInfo

/**
 * 卡牌详情工具类
 * 统一的卡牌详情显示逻辑，供搜索页面和套牌页面共同使用
 */
object CardDetailHelper {

    /**
     * 从 MtgchCardDto 构建 CardInfo（用于显示卡牌详情）
     *
     * @param mtgchCard MTGCH API 返回的完整卡牌数据
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
        cardInfoId: String = mtgchCard.id ?: mtgchCard.oracleId ?: "",
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
        val otherFaces = mtgchCard.otherFaces
        val isDualFaced = (mtgchCard.layout in getDualFaceLayouts())
            || (otherFaces != null && otherFaces.isNotEmpty())
            || (mtgchCard.name?.contains(" // ") == true)

        // 正面图片 - 优先使用中文图片
        val frontImageUri = mtgchCard.zhsImageUris?.normal ?: mtgchCard.imageUris?.normal

        // 反面图片 - 优先使用中文图片
        val backImageUri = if (otherFaces != null && otherFaces.isNotEmpty()) {
            otherFaces[0].zhsImageUris?.normal ?: otherFaces[0].imageUris?.normal
        } else null

        // 正面名称
        val frontFaceName = if (isDualFaced) {
            mtgchCard.faceName ?: mtgchCard.zhsFaceName ?: mtgchCard.name
        } else null

        // 反面名称
        val backFaceName = if (otherFaces != null && otherFaces.isNotEmpty()) {
            otherFaces[0].faceName ?: otherFaces[0].zhsFaceName ?: otherFaces[0].name
        } else null

        // 反面法术力
        val backFaceManaCost = if (otherFaces != null && otherFaces.isNotEmpty()) {
            otherFaces[0].manaCost
        } else null

        // 反面类型
        val backFaceTypeLine = if (otherFaces != null && otherFaces.isNotEmpty()) {
            otherFaces[0].zhsTypeLine ?: otherFaces[0].typeLine
        } else null

        // 反面规则文本
        val backFaceOracleText = if (otherFaces != null && otherFaces.isNotEmpty()) {
            otherFaces[0].zhsText ?: otherFaces[0].oracleText
        } else null

        // 反面力量
        val backFacePower = if (otherFaces != null && otherFaces.isNotEmpty()) {
            otherFaces[0].power
        } else null

        // 反面防御力
        val backFaceToughness = if (otherFaces != null && otherFaces.isNotEmpty()) {
            otherFaces[0].toughness
        } else null

        // 反面忠诚度
        val backFaceLoyalty = if (otherFaces != null && otherFaces.isNotEmpty()) {
            otherFaces[0].loyalty
        } else null

        return CardInfo(
            id = cardInfoId,
            name = displayName ?: (mtgchCard.zhsName ?: mtgchCard.atomicTranslatedName ?: mtgchCard.name ?: ""),
            manaCost = manaCost ?: mtgchCard.manaCost,
            cmc = cmc ?: mtgchCard.cmc?.toDouble(),
            typeLine = typeLine ?: (mtgchCard.zhsTypeLine ?: mtgchCard.atomicTranslatedType ?: mtgchCard.typeLine),
            oracleText = oracleText ?: (mtgchCard.zhsText ?: mtgchCard.atomicTranslatedText ?: mtgchCard.oracleText),
            colors = colors ?: mtgchCard.colors,
            colorIdentity = mtgchCard.colorIdentity,
            power = power ?: mtgchCard.power,
            toughness = toughness ?: mtgchCard.toughness,
            loyalty = loyalty ?: mtgchCard.loyalty,
            rarity = rarity ?: mtgchCard.rarity,
            setCode = setCode ?: mtgchCard.setCode,
            setName = setName ?: (mtgchCard.setTranslatedName ?: mtgchCard.setName),
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
            backFaceLoyalty = backFaceLoyalty
        )
    }

    /**
     * 获取双面牌的布局类型
     */
    private fun getDualFaceLayouts(): List<String> {
        return listOf(
            "transform",       // 变身牌
            "modal_dfc",       // 模态双面牌
            "reversible_card", // 可翻转卡牌
            "double_faced_token", // 双面衍生物
            "art_series",      // 艺术系列
            "double_sided",    // 双面卡（通用）
            "flip",            // 翻转牌
            "adventure",       // 冒险牌（双面）
            "split",           // 分割牌
            "aftermath",       // 战后牌
            "classify",        // 类别牌
            "prototype",       // 原型牌
            "saga"             // 传说牌（双面）
        )
    }
}
