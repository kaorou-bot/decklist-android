package com.mtgo.decklistmanager.exporter.format

import com.mtgo.decklistmanager.domain.model.Card
import com.mtgo.decklistmanager.domain.model.CardLocation
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.exporter.DecklistExporter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MTGO .dek 格式导出器
 *
 * MTGO (Magic: The Gathering Online) 使用的套牌格式
 *
 * 格式示例：
 * ```
 * 4 Bolt
 * 2 Counterspell
 * 1 Black Lotus
 *
 * Sideboard
 * 2 Red Elemental Blast
 * 1 Blue Elemental Blast
 * ```
 */
@Singleton
class MtgoFormatExporter @Inject constructor() : DecklistExporter {

    override suspend fun export(
        decklist: Decklist,
        cards: List<Card>,
        includeSideboard: Boolean
    ): String {
        return buildString {
            // 主牌
            val mainDeck = cards.filter { it.location == CardLocation.MAIN }
            mainDeck.forEach { card ->
                val cardName = card.cardNameZh ?: card.cardName
                line("${card.quantity} $cardName")
            }

            // 备牌
            if (includeSideboard) {
                val sideboard = cards.filter { it.location == CardLocation.SIDEBOARD }
                if (sideboard.isNotEmpty()) {
                    line()
                    line("Sideboard")
                    sideboard.forEach { card ->
                        val cardName = card.cardNameZh ?: card.cardName
                        line("${card.quantity} $cardName")
                    }
                }
            }
        }.trimEnd()
    }

    override fun getFileExtension() = "dek"

    override fun getFormatName() = "MTGO (.dek)"

    /**
     * 添加一行内容
     */
    private fun StringBuilder.line(str: String = "") {
        appendLine(str)
    }
}
