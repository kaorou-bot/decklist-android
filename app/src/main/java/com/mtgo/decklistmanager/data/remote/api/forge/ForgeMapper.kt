package com.mtgo.decklistmanager.data.remote.api.forge

import com.mtgo.decklistmanager.data.remote.api.mtgch.CardFace
import com.mtgo.decklistmanager.data.remote.api.mtgch.ImageUris
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchSearchResponse

/**
 * Forge 中文卡查 API → 旧 MtgchCardDto 适配映射
 *
 * 设计要点:
 * - 新 API 的 24 位十六进制 id 填入旧模型的 oracleId 字段，
 *   使上层（SearchViewModel / CardDetailViewModel / DecklistRepository）
 *   的标识符传递链路无需改动。
 * - 新 API 的卡图即中文卡图，同时填入 imageUris 与 zhsImageUris。
 * - 印刷版本（printings）与卡牌基础信息合并为完整 MtgchCardDto，
 *   供版本切换 UI 使用。
 */
object ForgeMapper {

    /** 单个 URL 扩展为 ImageUris（各尺寸同源） */
    private fun String?.toImageUris(): ImageUris? {
        if (this.isNullOrBlank()) return null
        return ImageUris(
            small = this,
            normal = this,
            large = this,
            png = null,
            artCrop = null,
            borderCrop = null
        )
    }

    /** 搜索结果 / 详情 → 旧卡牌 DTO */
    fun ForgeCardDto.toMtgchCard(): MtgchCardDto {
        val frontImage = imageUrl.toImageUris()
        val faces = this.faces

        return MtgchCardDto(
            id = null,
            oracleId = id,                    // 新 id 作为标识符
            scryfallId = null,
            name = name,
            nameZh = nameZh,
            manaCost = manaCost,
            cmc = manaValue,
            colors = colors,
            colorIdentity = colorIdentity,
            typeLine = typeLine,
            typeLineZh = typeLineZh,
            oracleText = oracleText,
            oracleTextZh = oracleTextZh,
            power = power,
            toughness = toughness,
            loyalty = loyalty,
            rarity = rarity,
            setCode = setCode,
            setName = setName,
            collectorNumber = collectorNumber,
            releasedAt = null,
            imageUris = frontImage,
            layout = layout,
            isDoubleFaced = isDoubleFaced,
            isToken = rarity == "token",
            cardFaces = faces?.map { face ->
                CardFace(
                    name = face.name,
                    faceName = face.name,
                    manaCost = null,
                    typeLine = face.typeLine,
                    oracleText = face.oracleText,
                    power = null,
                    toughness = null,
                    loyalty = null,
                    colors = null,
                    imageUris = face.imageUrl.toImageUris(),
                    zhName = face.nameZh,
                    zhText = face.oracleTextZh,
                    zhTypeLine = face.typeLineZh
                )
            },
            faceName = faces?.firstOrNull()?.name,
            lang = null,
            zhsFaceName = faces?.firstOrNull()?.nameZh,
            zhsImage = imageUrl,
            zhsImageUris = frontImage,        // 新 API 卡图即中文卡图
            atomicTranslatedName = null,
            atomicTranslatedType = null,
            atomicTranslatedText = null,
            defense = defense,
            colorIndicator = null,
            legalities = null,
            setNameZh = setNameZh,
            setTranslatedName = setNameZh,
            artist = null,
            otherFaces = null,
            faceIndex = null,
            keywords = null,
            scryfallUri = null,
            edhrecRank = null,
            pennyRank = null,
            oldId = id
        )
    }

    /**
     * 将印刷版本与卡牌基础信息合并为完整 DTO（用于版本切换）
     *
     * 新 API 的 printing 只含系列/图片信息，名称/法术力等取自详情主体。
     */
    fun ForgeCardDto.mergePrinting(printing: ForgePrintingDto): MtgchCardDto {
        val base = toMtgchCard()
        val printImage = printing.imageUrl.toImageUris()
        val backImage = printing.backImageUrl ?: backImageUrl

        // 双面牌：正面用 printing 图，背面用 printing 背面图
        val mergedFaces = if (isDoubleFaced) {
            listOf(
                CardFace(
                    name = faces?.getOrNull(0)?.name ?: name,
                    faceName = faces?.getOrNull(0)?.name,
                    manaCost = null,
                    typeLine = faces?.getOrNull(0)?.typeLine ?: typeLine,
                    oracleText = faces?.getOrNull(0)?.oracleText ?: oracleText,
                    power = power,
                    toughness = toughness,
                    loyalty = loyalty,
                    colors = colors,
                    imageUris = printImage,
                    zhName = faces?.getOrNull(0)?.nameZh ?: nameZh,
                    zhText = faces?.getOrNull(0)?.oracleTextZh ?: oracleTextZh,
                    zhTypeLine = faces?.getOrNull(0)?.typeLineZh ?: typeLineZh
                ),
                CardFace(
                    name = faces?.getOrNull(1)?.name,
                    faceName = faces?.getOrNull(1)?.name,
                    manaCost = null,
                    typeLine = faces?.getOrNull(1)?.typeLine,
                    oracleText = faces?.getOrNull(1)?.oracleText,
                    power = null,
                    toughness = null,
                    loyalty = null,
                    colors = null,
                    imageUris = backImage.toImageUris(),
                    zhName = faces?.getOrNull(1)?.nameZh,
                    zhText = faces?.getOrNull(1)?.oracleTextZh,
                    zhTypeLine = faces?.getOrNull(1)?.typeLineZh
                )
            )
        } else {
            base.cardFaces
        }

        return base.copy(
            setCode = printing.setCode ?: base.setCode,
            setName = printing.setName ?: base.setName,
            setNameZh = printing.setNameZh ?: base.setNameZh,
            setTranslatedName = printing.setNameZh ?: base.setTranslatedName,
            collectorNumber = printing.collectorNumber ?: base.collectorNumber,
            rarity = printing.rarity ?: base.rarity,
            artist = printing.artist ?: base.artist,
            releasedAt = printing.releaseDate,
            imageUris = printImage,
            zhsImage = printing.imageUrl,
            zhsImageUris = printImage,
            cardFaces = mergedFaces
        )
    }

    /** 搜索响应 → 旧搜索响应包装 */
    fun toSearchResponse(cards: List<MtgchCardDto>, total: Int?): MtgchSearchResponse {
        return MtgchSearchResponse(
            success = true,
            cards = cards,
            total = total
        )
    }
}
