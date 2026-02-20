package com.mtgo.decklistmanager.data.remote.api.dto

import com.mtgo.decklistmanager.domain.model.CardInfo
import com.mtgo.decklistmanager.util.AppLogger

/**
 * 将 CardInfoDto 转换为 CardInfo 领域模型
 * 优先使用中文字段
 */
fun CardInfoDto.toCardInfo(): CardInfo {
    // 调试日志：记录服务器返回的原始字段
    AppLogger.d("ServerMapper", "toCardInfo - name: $name, nameZh: $nameZh, typeLine: $typeLine, typeLineZh: $typeLineZh")
    AppLogger.d("ServerMapper", "toCardInfo - oracleText length: ${oracleText?.length ?: 0}, oracleTextZh length: ${oracleTextZh?.length ?: 0}")

    // 检测缺少中文规则文本的卡牌
    if (oracleTextZh == null && oracleText != null) {
        AppLogger.w("ServerMapper", "⚠️ 缺少中文规则文本: $name ($nameZh) - 系列: $setNameZh")
    }

    // 处理双面牌背面信息
    val backFace = cardFaces?.getOrNull(1)  // 第二个面是背面

    // MTGCH 可能没有图片的 setCode 列表（使用 Scryfall 备用）
    val setsWithNoMtgchImages = setOf(
        "J25",  // Jurassic World 25
        "DBL",  // Double-Faced Cards
        "SLD",  // Secret Lair
        "PSLD",  // Secret Lair Drop
        "DMR",  // Dominaria Remastered (部分卡牌)
        "MM3",  // Modern Masters 2017
        "MM2",  // Modern Masters 2015
        "VMA",  // Vintage Masters
        "TPR",  // Tempest Remastered
        "MD1",  // Masters Draft
        "A25",  // Masters 25
        "UMA",  // Ultimate Masters
        "M19",  // Core Set 2019
        "M20",  // Core Set 2020
        "M21",  // Core Set 2021
        "2XM",  // Double Masters
        "2X2",  // Double Masters 2022
        "JMP",  // Jumpstart
        "J21",  // Jumpstart 2022
        "BRR",  // Battle Royale
        "GN2",  // Game Night 2019
        "GNT",  // Game Night: Free-for-All
        "HA1",  // Historic Anthology 1
        "HA2",  // Historic Anthology 2
        "HA3",  // Historic Anthology 3
        "HA4",  // Historic Anthology 4
        "H1R",  // Historic Horizons
        "H2R",  // H1R Remastered
        "PL21",  // Premier Play Innistrad
        "PL20",  // Premier Play Theros
        "PSAL",  // Streets of New Capenna Anthology
        "SNC",  // Streets of New Capenna
        "DMU",  // Dominaria United
        "BRO",  // The Brothers' War
        "ONE",  // Phyrexia: All Will Be One
        "MOM",  // March of the Machine
        "MAT",  // March of the Machine: The Aftermath
        "WOE",  // Wilds of Eldraine
        "LCI",  // Lost Caverns of Ixalan
        "BIG",  // Bloomburrow
        "BLB",  // Assassin's Creed
        "FDN",  // Foundations
        "SPG",  // Spree
        "CLU",  // Clue Edition
        "OUT"  // Outlaws of Thunder Junction
    )

    // 检查是否需要使用 Scryfall 备用图片
    val useScryfall = setCode?.let { it in setsWithNoMtgchImages } == true

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
        // ✅ 某些系列使用 Scryfall 备用图片
        imagePath = if (useScryfall) {
            imageUris?.scryfallImageUris?.png ?: imageUris?.png
        } else {
            imageUris?.png ?: imageUris?.scryfallImageUris?.png
        },
        imageUriSmall = if (useScryfall) {
            imageUris?.scryfallImageUris?.small ?: imageUris?.small
        } else {
            imageUris?.small ?: imageUris?.scryfallImageUris?.small
        },
        imageUriNormal = if (useScryfall) {
            imageUris?.scryfallImageUris?.normal ?: imageUris?.normal
        } else {
            imageUris?.normal ?: imageUris?.scryfallImageUris?.normal
        },
        imageUriLarge = if (useScryfall) {
            imageUris?.scryfallImageUris?.large ?: imageUris?.large
        } else {
            imageUris?.large ?: imageUris?.scryfallImageUris?.large
        },
        isDualFaced = isDoubleFaced == true,
        // ✅ 双面牌背面信息（从 cardFaces 提取）
        frontFaceName = nameZh ?: name,
        backFaceName = backFace?.nameZh ?: backFace?.name,
        frontImageUri = if (useScryfall) {
            imageUris?.scryfallImageUris?.normal ?: imageUris?.normal
        } else {
            imageUris?.normal ?: imageUris?.scryfallImageUris?.normal
        },
        backImageUri = if (useScryfall) {
            backFace?.imageUris?.scryfallImageUris?.normal ?: backFace?.imageUris?.normal
        } else {
            backFace?.imageUris?.normal ?: backFace?.imageUris?.scryfallImageUris?.normal
        },
        backFaceManaCost = backFace?.manaCost,
        backFaceTypeLine = backFace?.typeLineZh ?: backFace?.typeLine,
        backFaceOracleText = backFace?.oracleTextZh ?: backFace?.oracleText,
        backFacePower = backFace?.power,
        backFaceToughness = backFace?.toughness,
        backFaceLoyalty = backFace?.loyalty
    )
}
