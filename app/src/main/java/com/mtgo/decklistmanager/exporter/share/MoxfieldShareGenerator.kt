package com.mtgo.decklistmanager.exporter.share

import com.mtgo.decklistmanager.domain.model.Card
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.util.AppLogger
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Moxfield 分享链接生成器
 *
 * 生成可在 Moxfield (moxfield.com) 上导入的分享链接
 *
 * Moxfield 是最受欢迎的 MTG 套牌分享网站
 */
@Singleton
class MoxfieldShareGenerator @Inject constructor() {

    companion object {
        private const val MOXFIELD_IMPORT_URL = "https://www.moxfield.com/import"
        private const val TAG = "MoxfieldShareGenerator"
    }

    /**
     * 生成 Moxfield 分享链接
     *
     * @param decklist 套牌数据
     * @return Moxfield 导入链接
     */
    suspend fun generateShareLink(decklist: Decklist): String {
        try {
            val decklistText = convertToMoxfieldFormat(decklist)
            val encoded = URLEncoder.encode(decklistText, "UTF-8")
            return "$MOXFIELD_IMPORT_URL?deck=$encoded"
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to generate Moxfield link", e)
            // 返回基本的导入页面
            return MOXFIELD_IMPORT_URL
        }
    }

    /**
     * 将套牌转换为 Moxfield 格式
     *
     * Moxfield 格式：
     * ```
     * 4 Bolt
     * 2 Counterspell
     *
     * 2 Red Elemental Blast
     * 1 Blue Elemental Blast
     * ```
     *
     * 主牌和备牌之间用空行分隔
     */
    private fun convertToMoxfieldFormat(decklist: Decklist): String {
        // TODO: 需要从 Repository 获取卡牌列表
        // 目前先返回一个基本的格式
        return buildString {
            line("// ${decklist.deckName ?: "Unknown Deck"}")
            line("// Main deck cards will be listed here")
            line("// Sideboard cards will be listed here")
        }.trimEnd()
    }

    /**
     * 生成可分享的文本格式
     *
     * 适用于分享到社交媒体或聊天应用
     */
    suspend fun generateShareText(decklist: Decklist): String {
        // TODO: 需要从 Repository 获取卡牌列表来计算数量
        return buildString {
            line("📜 ${decklist.deckName ?: "Unknown Deck"}")
            decklist.playerName?.let { line("👤 玩家：$it") }
            decklist.format?.let { line("🏆 赛制：$it") }
            decklist.record?.let { line("📊 战绩：$it") }
            line()
            line("🔗 点击链接查看完整卡表：")
            line(generateShareLink(decklist))
        }
    }

    /**
     * 添加一行内容
     */
    private fun StringBuilder.line(str: String = "") {
        appendLine(str)
    }
}
