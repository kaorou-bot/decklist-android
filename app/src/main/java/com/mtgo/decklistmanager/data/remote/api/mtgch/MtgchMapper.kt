package com.mtgo.decklistmanager.data.remote.api.mtgch

import com.mtgo.decklistmanager.data.local.entity.CardInfoEntity
import com.google.gson.Gson

/**
 * 将 MTGCH API 卡牌 DTO 转换为 CardInfoEntity
 */
fun MtgchCardDto.toEntity(): CardInfoEntity {
    // 检测双面牌 - 包含更多布局类型
    val dualFaceLayouts = listOf(
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

    val isDualFaced = (layout != null && layout in dualFaceLayouts)
            || (otherFaces != null && otherFaces.isNotEmpty())
            || (cardFaces != null && cardFaces.isNotEmpty())
            || (imageUris == null && zhsImageUris == null) // 双面牌通常没有单一的 imageUris
            || (name?.contains(" // ") == true)  // 冒险牌特征：Name // Adventure Name
            || (zhsName?.contains("//") == true)  // 中文名称可能也包含

    // 对于双面牌，需要从 cardFaces 或 otherFaces 中提取中文名称
    val displayName = if (isDualFaced) {
        // 优先从 cardFaces 获取
        if (cardFaces != null && cardFaces.isNotEmpty()) {
            val frontZhName = cardFaces.getOrNull(0)?.zhName
            val backZhName = cardFaces.getOrNull(1)?.zhName

            when {
                frontZhName != null && backZhName != null -> "$frontZhName // $backZhName"
                frontZhName != null -> frontZhName
                else -> zhsName ?: atomicTranslatedName ?: name ?: ""
            }
        }
        // 其次从 otherFaces 获取（需要解析名称）
        else if (otherFaces != null && otherFaces.isNotEmpty()) {
            val otherFace = otherFaces[0]
            val otherFaceName = otherFace.zhsFaceName ?: otherFace.faceName ?: otherFace.name

            // 如果名称包含 " // "，则分割它
            if (otherFaceName != null && otherFaceName.contains(" // ")) {
                otherFaceName  // 已经是 "Front // Back" 格式
            } else {
                zhsName ?: atomicTranslatedName ?: name ?: ""
            }
        }
        // 最后使用 zhsName 或 name
        else {
            zhsName ?: atomicTranslatedName ?: name ?: ""
        }
    } else {
        // v4.0.0: 优先官方中文，其次机器翻译，最后英文
        zhsName ?: atomicTranslatedName ?: name ?: ""
    }

    // 提取双面牌信息
    val frontFaceName = if (isDualFaced) {
        // 正面名称：优先从顶层卡牌对象获取（faceName/zhsFaceName）
        when {
            cardFaces != null && cardFaces.isNotEmpty() -> {
                cardFaces.getOrNull(0)?.zhName ?: cardFaces.getOrNull(0)?.name
            }
            otherFaces != null && otherFaces.isNotEmpty() -> {
                // 顶层卡牌对象包含正面信息
                faceName ?: zhsFaceName ?: name
            }
            else -> faceName ?: zhsFaceName ?: name
        }
    } else null

    // 尝试从 card_faces 或 other_faces 获取背面名称
    val backFaceName = if (isDualFaced) {
        // 背面名称：从 otherFaces[0] 获取（反面数据）
        when {
            cardFaces != null && cardFaces.isNotEmpty() -> {
                // 优先使用中文背面名称
                cardFaces.getOrNull(1)?.zhName ?: cardFaces.getOrNull(1)?.name
            }
            otherFaces != null && otherFaces.isNotEmpty() -> {
                // otherFaces[0] 包含反面数据
                // 优先级：官方中文面名 > 机器翻译面名 > 英文面名
                otherFaces[0].zhsFaceName ?: otherFaces[0].atomicTranslatedName ?: otherFaces[0].faceName ?: otherFaces[0].name
            }
            else -> null
        }
    } else null

    // 图片 URLs
    val frontImageUri = if (isDualFaced) {
        zhsImageUris?.normal ?: imageUris?.normal ?: zhsImage
    } else {
        zhsImageUris?.normal ?: imageUris?.normal ?: zhsImage
    }

    // 尝试从多个来源获取背面图片
    val backImageUri = if (isDualFaced) {
        when {
            // 优先从 card_faces 获取
            cardFaces != null && cardFaces.size > 1 -> {
                cardFaces.getOrNull(1)?.imageUris?.normal
            }
            // 其次从 other_faces 获取
            otherFaces != null && otherFaces.isNotEmpty() -> {
                otherFaces[0].zhsImageUris?.normal ?: otherFaces[0].imageUris?.normal
            }
            else -> null
        }
    } else null

    // 提取反面详细信息
    val backFaceManaCost = if (isDualFaced) {
        when {
            cardFaces != null && cardFaces.size > 1 -> {
                cardFaces.getOrNull(1)?.manaCost
            }
            otherFaces != null && otherFaces.isNotEmpty() -> {
                otherFaces[0].manaCost
            }
            else -> null
        }
    } else null

    val backFaceTypeLine = if (isDualFaced) {
        when {
            cardFaces != null && cardFaces.size > 1 -> {
                cardFaces.getOrNull(1)?.zhTypeLine ?: cardFaces.getOrNull(1)?.typeLine
            }
            otherFaces != null && otherFaces.isNotEmpty() -> {
                // 优先使用官方中文，其次机器翻译，最后英文原文
                otherFaces[0].zhsTypeLine ?: otherFaces[0].atomicTranslatedType ?: otherFaces[0].typeLine
            }
            else -> null
        }
    } else null

    val backFaceOracleText = if (isDualFaced) {
        when {
            cardFaces != null && cardFaces.size > 1 -> {
                cardFaces.getOrNull(1)?.zhText ?: cardFaces.getOrNull(1)?.oracleText
            }
            otherFaces != null && otherFaces.isNotEmpty() -> {
                // 优先使用官方中文，其次机器翻译，最后英文原文
                otherFaces[0].zhsText ?: otherFaces[0].atomicTranslatedText ?: otherFaces[0].oracleText
            }
            else -> null
        }
    } else null

    // 提取反面攻防数据
    val backFacePower = if (isDualFaced) {
        when {
            cardFaces != null && cardFaces.size > 1 -> {
                cardFaces.getOrNull(1)?.power
            }
            otherFaces != null && otherFaces.isNotEmpty() -> {
                otherFaces[0].power
            }
            else -> null
        }
    } else null

    val backFaceToughness = if (isDualFaced) {
        when {
            cardFaces != null && cardFaces.size > 1 -> {
                cardFaces.getOrNull(1)?.toughness
            }
            otherFaces != null && otherFaces.isNotEmpty() -> {
                otherFaces[0].toughness
            }
            else -> null
        }
    } else null

    val backFaceLoyalty = if (isDualFaced) {
        when {
            cardFaces != null && cardFaces.size > 1 -> {
                cardFaces.getOrNull(1)?.loyalty
            }
            otherFaces != null && otherFaces.isNotEmpty() -> {
                otherFaces[0].loyalty
            }
            else -> null
        }
    } else null

    // 序列化 card_faces
    val gson = Gson()
    val cardFacesJson = if (cardFaces != null) {
        gson.toJson(cardFaces)
    } else null

    // 使用 id 或生成一个唯一 ID
    val entityId = id ?: oracleId ?: "${name}_${setCode}_${collectorNumber}"

    return CardInfoEntity(
        id = entityId,
        name = displayName,  // 使用处理后的显示名称
        enName = name,  // 保存原始英文名
        manaCost = manaCost,
        cmc = cmc?.toDouble(),
        // v4.0.0: 优先使用官方中文，其次机器翻译，最后英文原文
        typeLine = zhsTypeLine ?: atomicTranslatedType ?: typeLine,
        oracleText = zhsText ?: atomicTranslatedText ?: oracleText,
        colors = colors?.joinToString(","),
        colorIdentity = colorIdentity?.joinToString(","),
        power = power,
        toughness = toughness,
        loyalty = loyalty,
        rarity = rarity,
        setCode = setCode,
        setName = setTranslatedName ?: setName,
        artist = artist,
        cardNumber = collectorNumber,
        legalStandard = legalities?.get("standard"),
        legalModern = legalities?.get("modern"),
        legalPioneer = legalities?.get("pioneer"),
        legalLegacy = legalities?.get("legacy"),
        legalVintage = legalities?.get("vintage"),
        legalCommander = legalities?.get("commander"),
        legalPauper = legalities?.get("pauper"),
        priceUsd = null,
        scryfallUri = scryfallUri,
        imagePath = frontImageUri,
        imageUriSmall = zhsImageUris?.small ?: imageUris?.small,
        imageUriNormal = frontImageUri,
        imageUriLarge = zhsImageUris?.large ?: imageUris?.large,
        isDualFaced = isDualFaced,
        cardFacesJson = cardFacesJson,
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
        lastUpdated = System.currentTimeMillis()
    )
}
