package com.mtgo.decklistmanager.data.remote.api

import android.util.Log
import com.mtgo.decklistmanager.data.remote.api.dto.MtgTop8DecklistDto
import com.mtgo.decklistmanager.data.remote.api.dto.MtgTop8DecklistDetailDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.URLEncoder

/**
 * MTGTop8 Scraper - MTGTop8.com 爬虫
 *
 * MTGTop8 是一个包含大量 MTG 比赛牌组数据的网站
 * 网站结构：
 * - 首页: https://mtgtop8.com/
 * - 格式页面: https://mtgtop8.com/format?f=ST (Standard), MO (Modern), LE (Legacy), etc.
 * - 牌组页面: https://mtgtop8.com/event?e=XXXX&d=YYYY (比赛详情)
 * - 牌组详情: https://mtgtop8.com/deck?id=XXXX (具体牌组)
 */
class MtgTop8Scraper {

    companion object {
        private const val TAG = "MtgTop8Scraper"
        private const val BASE_URL = "https://mtgtop8.com"

        // 格式代码
        const val FORMAT_STANDARD = "ST"
        const val FORMAT_MODERN = "MO"
        const val FORMAT_LEGACY = "LE"
        const val FORMAT_VINTAGE = "VI"
        const val FORMAT_PAUPER = "PA"
        const val FORMAT_PIONEER = "PI"
        const val FORMAT_COMMANDER = "EDH"
        const val FORMAT_CEDH = "cEDH"
    }

    /**
     * 获取指定格式的牌组列表
     *
     * @param format 格式代码 (ST, MO, LE, etc.)
     * @param date 可选日期筛选 (YYYY-MM-DD)，null 表示所有日期
     * @param maxEvents 最大抓取比赛数量
     * @return 牌组链接列表
     */
    suspend fun fetchDecklistPage(
        format: String = FORMAT_MODERN,
        date: String? = null,
        maxEvents: Int = 10
    ): List<MtgTop8DecklistDto> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching format page: $format, date: $date")

            // 构建URL - MTGTop8 的格式页面
            val url = StringBuilder("$BASE_URL/format?f=$format").apply {
                // MTGTop8 不直接支持日期参数，我们获取所有数据然后在内存中过滤
            }.toString()

            Log.d(TAG, "Fetching URL: $url")

            // MTGTop8 需要特殊的 User-Agent
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(30000)
                .get()

            val decklistLinks = mutableListOf<MtgTop8DecklistDto>()

            // MTGTop8 的结构：每个牌组都在 tr 元素中，class可能是 "hover_tr" 或其他
            // 尝试多种选择器
            val eventElements = doc.select("tr.hover_tr")
            Log.d(TAG, "Found ${eventElements.size} event elements")

            for (event in eventElements) {
                try {
                    // 提取比赛信息 - MTGTop8 的结构
                    val cells = event.select("td")
                    if (cells.size < 3) continue

                    val eventName = cells[1].text().trim()
                    val eventDate = cells[2].text().trim()

                    // 如果指定了日期，只爬取匹配的牌组
                    if (date != null && !eventDate.contains(date)) {
                        continue
                    }

                    // 提取牌组链接 - 在第二列中寻找所有链接
                    val deckLinks = cells[1].select("a")
                    for (deckLink in deckLinks) {
                        val deckUrl = deckLink.attr("href")
                        if (deckUrl.contains("deck?id=") || deckUrl.contains("deck?e=")) {
                            val deckId = extractDeckId(deckUrl)
                            val deckName = deckLink.text().trim()
                            val playerName = eventName // MTGTop8 可能不单独显示玩家名

                            decklistLinks.add(
                                MtgTop8DecklistDto(
                                    deckId = deckId,
                                    deckName = deckName,
                                    playerName = playerName,
                                    eventName = eventName,
                                    eventDate = eventDate,
                                    format = format,
                                    url = if (deckUrl.startsWith("http")) deckUrl else "$BASE_URL/$deckUrl"
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing event: ${e.message}", e)
                }
            }

            Log.d(TAG, "Found ${decklistLinks.size} decklists")
            decklistLinks

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching format page: ${e.message}", e)
            Log.e(TAG, "Stack trace:", e)
            emptyList()
        }
    }

    /**
     * 获取牌组详情
     *
     * @param url 牌组页面 URL
     * @return 牌组详情
     */
    suspend fun fetchDecklistDetail(url: String): MtgTop8DecklistDetailDto? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching decklist detail: $url")

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(30000)
                .get()

            // 解析主牌
            val mainDeck = parseDeckSection(doc, "主牌", "Maindeck")
            // 解备牌
            val sideboard = parseDeckSection(doc, "备牌", "Sideboard")

            MtgTop8DecklistDetailDto(
                mainDeck = mainDeck,
                sideboardDeck = sideboard
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching decklist detail: ${e.message}", e)
            null
        }
    }

    /**
     * 解析牌组的某个部分（主牌或备牌）
     */
    private fun parseDeckSection(doc: Document, vararg sectionNames: String): List<MtgTop8DecklistDetailDto.MtgTop8CardDto> {
        val cards = mutableListOf<MtgTop8DecklistDetailDto.MtgTop8CardDto>()

        for (sectionName in sectionNames) {
            // MTGTop8 使用表格显示牌组
            // 尝试不同的选择器
            val deckRows = doc.select("table.Stable tr[align=\"center\"]")

            for (row in deckRows) {
                try {
                    val quantityCell = row.select("td:nth-child(1)")
                    val nameCell = row.select("td:nth-child(2)")

                    if (quantityCell.isNotEmpty() && nameCell.isNotEmpty()) {
                        val quantity = quantityCell.text().trim().toIntOrNull()
                        val cardName = nameCell.text().trim()

                        if (quantity != null && cardName.isNotEmpty()) {
                            cards.add(
                                MtgTop8DecklistDetailDto.MtgTop8CardDto(
                                    quantity = quantity,
                                    name = cardName
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing card row: ${e.message}")
                }
            }

            if (cards.isNotEmpty()) {
                break
            }
        }

        return cards
    }

    /**
     * 从 URL 中提取牌组 ID
     */
    private fun extractDeckId(url: String): String {
        val regex = Regex("deck\\?id=([^&]+)")
        return regex.find(url)?.groupValues?.get(1) ?: ""
    }

    /**
     * 获取格式显示名称
     */
    fun getFormatName(code: String): String {
        return when (code) {
            FORMAT_STANDARD -> "Standard"
            FORMAT_MODERN -> "Modern"
            FORMAT_LEGACY -> "Legacy"
            FORMAT_VINTAGE -> "Vintage"
            FORMAT_PAUPER -> "Pauper"
            FORMAT_PIONEER -> "Pioneer"
            FORMAT_COMMANDER -> "Commander"
            FORMAT_CEDH -> "cEDH"
            else -> code
        }
    }
}
