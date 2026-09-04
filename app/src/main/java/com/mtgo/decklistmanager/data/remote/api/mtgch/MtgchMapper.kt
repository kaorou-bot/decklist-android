package com.mtgo.decklistmanager.data.remote.api.mtgch

import com.mtgo.decklistmanager.data.local.entity.CardInfoEntity
import com.mtgo.decklistmanager.util.ManaCosts
import com.google.gson.Gson

/**
 * 将 MTG API 卡牌 DTO 转换为 CardInfoEntity
 *
 * 新版 API 字段映射:
 * - 中文名称: nameZh (旧: zhs_name)
 * - 中文类型: typeLineZh (旧: zhs_type_line)
 * - 中文规则: oracleTextZh (旧: zhs_text)
 * - 系列代码: setCode (旧: set)
 * - 系列名称: setName (旧: set_name)
 */
fun MtgchCardDto.toEntity(): CardInfoEntity {
    // 真双面牌（transform/modal/meld/flip 等）：翻面按钮 + 切换卡图
    // 多部分牌（adventure/split/aftermath 等）：所有部分同页展示，不翻面
    val isDoubleFacedFlag = isDoubleFaced == true ||
        com.mtgo.decklistmanager.util.CardLayouts.isTrueDualFace(layout)

    val isMultiPartCard = com.mtgo.decklistmanager.util.CardLayouts.isMultiPart(layout) ||
        (!isDoubleFacedFlag && cardFaces != null && cardFaces.size >= 2)

    // isDualFaced 用于标识需要显示背面和翻转功能的真双面牌
    // 对于 split/adventure/flip 等伪双面牌，isDualFaced 应该为 false
    val isDualFaced = isDoubleFacedFlag

    // 获取中文名称（优先使用新字段 nameZh，其次使用旧字段）
    val getZhsName = nameZh ?: atomicTranslatedName

    // 对于双面牌，需要从 cardFaces 或 otherFaces 中提取中文名称
    val displayName = if (isDualFaced) {
        // 优先从 cardFaces 获取
        if (cardFaces != null && cardFaces.isNotEmpty()) {
            val frontZhName = cardFaces.getOrNull(0)?.zhName
            val backZhName = cardFaces.getOrNull(1)?.zhName

            when {
                frontZhName != null && backZhName != null -> "$frontZhName // $backZhName"
                frontZhName != null -> frontZhName
                else -> getZhsName ?: name ?: ""
            }
        }
        // 其次从 otherFaces 获取（需要解析名称）
        else if (otherFaces != null && otherFaces.isNotEmpty()) {
            val otherFace = otherFaces[0]
            val otherFaceName = otherFace.nameZh ?: otherFace.faceName ?: otherFace.name

            // 如果名称包含 " // "，则分割它
            if (otherFaceName != null && otherFaceName.contains(" // ")) {
                otherFaceName  // 已经是 "Front // Back" 格式
            } else {
                getZhsName ?: name ?: ""
            }
        }
        // 最后使用中文名或英文名
        else {
            getZhsName ?: name ?: ""
        }
    } else {
        // 优先官方中文，其次机器翻译，最后英文
        getZhsName ?: name ?: ""
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
                otherFaces[0].nameZh ?: otherFaces[0].atomicTranslatedName ?: otherFaces[0].faceName ?: otherFaces[0].name
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

    // 提取反面详细信息（仅针对真正的双面牌）
    val backFaceManaCost = if (isDoubleFacedFlag) {
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

    val backFaceTypeLine = if (isDoubleFacedFlag) {
        when {
            cardFaces != null && cardFaces.size > 1 -> {
                cardFaces.getOrNull(1)?.zhTypeLine ?: cardFaces.getOrNull(1)?.typeLine
            }
            otherFaces != null && otherFaces.isNotEmpty() -> {
                otherFaces[0].typeLineZh ?: otherFaces[0].atomicTranslatedType ?: otherFaces[0].typeLine
            }
            else -> null
        }
    } else null

    val backFaceOracleText = if (isDoubleFacedFlag) {
        when {
            cardFaces != null && cardFaces.size > 1 -> {
                // 优先使用中文规则文本
                cardFaces.getOrNull(1)?.zhText ?: cardFaces.getOrNull(1)?.oracleText
            }
            otherFaces != null && otherFaces.isNotEmpty() -> {
                // 优先使用官方中文，其次机器翻译，最后英文原文
                otherFaces[0].oracleTextZh ?: otherFaces[0].atomicTranslatedText ?: otherFaces[0].oracleText
            }
            else -> null
        }
    } else null

    // 提取反面攻防数据（仅针对真正的双面牌）
    val backFacePower = if (isDoubleFacedFlag) {
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

    val backFaceToughness = if (isDoubleFacedFlag) {
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

    val backFaceLoyalty = if (isDoubleFacedFlag) {
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

    // 多部分牌（历险/连体/余波等）：序列化为 CardPart 列表 JSON，供同页展示
    val multiPartsJson = if (isMultiPartCard && cardFaces != null && cardFaces.isNotEmpty()) {
        val parts = cardFaces.map { face ->
            com.mtgo.decklistmanager.domain.model.CardPart(
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
        }
        gson.toJson(parts)
    } else null

    // 使用 idString 或生成一个唯一 ID
    val entityId = idString ?: oracleId ?: "${name}_${setCode}_${collectorNumber}"

    // 对于双面牌，优先使用正面（card_faces[0]）的法术力值、攻防、忠诚度和规则文本；
    // 面数据缺失时回退到顶层字段（兼容旧服务端不返回逐面字段的情况）
    val finalManaCost = ManaCosts.normalize(
        if (isDoubleFacedFlag && cardFaces != null && cardFaces.isNotEmpty()) {
            cardFaces[0].manaCost ?: manaCost
        } else {
            manaCost  // 单面牌或多部分牌使用顶层法术力值
        }
    )

    val finalPower = if (isDoubleFacedFlag && cardFaces != null && cardFaces.isNotEmpty() && cardFaces[0].power != null) {
        cardFaces[0].power
    } else {
        power
    }

    val finalToughness = if (isDoubleFacedFlag && cardFaces != null && cardFaces.isNotEmpty() && cardFaces[0].toughness != null) {
        cardFaces[0].toughness
    } else {
        toughness
    }

    val finalLoyalty = if (isDoubleFacedFlag && cardFaces != null && cardFaces.isNotEmpty() && cardFaces[0].loyalty != null) {
        cardFaces[0].loyalty
    } else {
        loyalty
    }

    // 获取中文类型行
    val getTypeLineZh = typeLineZh ?: atomicTranslatedType

    // 获取中文规则文本
    val getOracleTextZh = oracleTextZh ?: atomicTranslatedText

    val finalOracleText = if (isDoubleFacedFlag && cardFaces != null && cardFaces.isNotEmpty()) {
        // 优先使用中文规则文本，其次英文
        cardFaces[0].zhText ?: cardFaces[0].oracleText
    } else {
        getOracleTextZh ?: oracleText
    }

    return CardInfoEntity(
        id = entityId,
        oracleId = oracleId, // 保存 Oracle ID
        name = displayName,  // 使用处理后的显示名称
        enName = name,  // 保存原始英文名
        manaCost = finalManaCost,
        cmc = cmc?.toDouble(),
        // 优先使用官方中文，其次机器翻译，最后英文原文
        typeLine = getTypeLineZh ?: typeLine,
        oracleText = finalOracleText,
        colors = colors?.joinToString(","),
        colorIdentity = colorIdentity?.joinToString(","),
        power = finalPower,
        toughness = finalToughness,
        loyalty = finalLoyalty,
        rarity = rarity,
        setCode = setCode,
        setName = setNameZh ?: setTranslatedName ?: setName,
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
        isMultiPart = isMultiPartCard,
        multiPartsJson = multiPartsJson,
        layout = layout,
        lastUpdated = System.currentTimeMillis()
    )
}
