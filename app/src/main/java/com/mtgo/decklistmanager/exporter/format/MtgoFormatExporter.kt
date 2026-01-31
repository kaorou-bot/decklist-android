package com.mtgo.decklistmanager.exporter.format

import com.mtgo.decklistmanager.exporter.DecklistExporter
import com.mtgo.decklistmanager.domain.model.Decklist
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
        includeSideboard: Boolean
    ): String {
        // TODO: 需要从 Repository 获取卡牌列表
        // 目前先返回一个基本的格式示例
        return buildString {
            line("// ${decklist.deckName ?: "Unknown Deck"}")
            decklist.playerName?.let { line("// Player: $it") }
            decklist.format?.let { line("// Format: $it") }
            line()
            line("// Main deck cards will be listed here")
            line("// Sideboard cards will be listed here")
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
