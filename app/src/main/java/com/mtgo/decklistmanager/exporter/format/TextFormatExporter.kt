package com.mtgo.decklistmanager.exporter.format

import com.mtgo.decklistmanager.exporter.DecklistExporter
import com.mtgo.decklistmanager.domain.model.Decklist
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文本格式导出器
 *
 * 纯文本格式，适合复制粘贴或简单查看
 *
 * 格式示例：
 * ```
 * 套牌名称：Red Deck Wins
 * 玩家：Player Name
 * 赛制：Standard
 *
 * 主牌 (60)
 * 4 Bolt
 * 2 Counterspell
 *
 * 备牌 (15)
 * 2 Red Elemental Blast
 * 1 Blue Elemental Blast
 * ```
 */
@Singleton
class TextFormatExporter @Inject constructor() : DecklistExporter {

    override suspend fun export(
        decklist: Decklist,
        includeSideboard: Boolean
    ): String {
        // TODO: 需要从 Repository 获取卡牌列表
        // 目前先返回一个基本的文本格式
        return buildString {
            line("套牌名称：${decklist.deckName ?: "Unknown"}")
            decklist.playerName?.let { line("玩家：$it") }
            decklist.format?.let { line("赛制：$it") }
            decklist.record?.let { line("战绩：$it") }
            line()
            line("// Main deck cards will be listed here")
            if (includeSideboard) {
                line()
                line("// Sideboard cards will be listed here")
            }
        }.trimEnd()
    }

    override fun getFileExtension() = "txt"

    override fun getFormatName() = "文本 (.txt)"

    /**
     * 添加一行内容
     */
    private fun StringBuilder.line(str: String = "") {
        appendLine(str)
    }
}
