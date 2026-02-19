package com.mtgo.decklistmanager.data.remote.api.dto

import com.mtgo.decklistmanager.domain.model.CardInfo

/**
 * 将 CardInfoDto 转换为 CardInfo 领域模型
 * 优先使用中文字段
 */
fun CardInfoDto.toCardInfo(): CardInfo {
    // 处理双面牌背面信息
    val backFace = cardFaces?.getOrNull(1)  // 第二个面是背面

    return CardInfo(
        id = scryfallId ?: oracleId ?: id.toString(),
        oracleId = oracleId,
        name = nameZh ?: name,                    // ✅ 中文名
        enName = name,                            // ✅ 英文名（用于搜索）
        manaCost = manaCost,
        cmc = cmc,
        typeLine = typeLineZh ?: typeLine,        // ✅ 中文类型行
        oracleText = oracleTextZh ?: oracleText,  // ✅ 中文规则文本
        colors = colors,
        colorIdentity = colorIdentity,
        power = power,
        toughness = toughness,
        loyalty = loyalty,
        rarity = rarity,
        setCode = setCode,
        setName = setNameZh ?: setName,           // ✅ 中文系列名称
        artist = null,                            // ServerDto 暂未提供
        cardNumber = collectorNumber,
        legalStandard = legalities?.get("standard"),
        legalModern = legalities?.get("modern"),
        legalPioneer = legalities?.get("pioneer"),
        legalLegacy = legalities?.get("legacy"),
        legalVintage = legalities?.get("vintage"),
        legalCommander = legalities?.get("commander"),
        legalPauper = legalities?.get("pauper"),
        priceUsd = null,                          // ServerDto 暂未提供价格
        scryfallUri = null,                       // ServerDto 暂未提供
        imagePath = imageUris?.png ?: imageUris?.large,
        imageUriSmall = imageUris?.small,
        imageUriNormal = imageUris?.normal,
        imageUriLarge = imageUris?.large,
        isDualFaced = isDoubleFaced == true,
        // ✅ 双面牌背面信息（从 cardFaces 提取）
        frontFaceName = nameZh ?: name,
        backFaceName = backFace?.nameZh ?: backFace?.name,
        frontImageUri = imageUris?.normal,
        backImageUri = backFace?.imageUris?.normal,
        backFaceManaCost = backFace?.manaCost,
        backFaceTypeLine = backFace?.typeLineZh ?: backFace?.typeLine,
        backFaceOracleText = backFace?.oracleTextZh ?: backFace?.oracleText,
        backFacePower = backFace?.power,
        backFaceToughness = backFace?.toughness,
        backFaceLoyalty = backFace?.loyalty
    )
}
