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
     * @param maxEvents 最大抓取比赛数量
     * @return 牌组链接列表
     */
    suspend fun fetchDecklistPage(
        format: String = FORMAT_MODERN,
        maxEvents: Int = 10
    ): List<MtgTop8DecklistDto> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching format page: $format")

            // MTGTop8 需要特殊的 User-Agent
            val doc = Jsoup.connect("$BASE_URL/format?f=$format")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(30000)
                .get()

            val decklistLinks = mutableListOf<MtgTop8DecklistDto>()

            // 解析牌组列表
            // MTGTop8 的结构：每个牌组都在 .hover_tr 元素中
            val eventElements = doc.select("tr.hover_tr")

            for (event in eventElements.take(maxEvents)) {
                try {
                    // 提取比赛信息
                    val eventName = event.select("td:nth-child(2)").text()
                    val eventDate = event.select("td:nth-child(3)").text()

                    // 提取牌组链接
                    val deckLinks = event.select("td:nth-child(2) a")
                    for (deckLink in deckLinks) {
                        val deckUrl = deckLink.attr("href")
                        if (deckUrl.contains("deck?id=")) {
                            val deckId = extractDeckId(deckUrl)
                            val deckName = deckLink.text()
                            val playerName = event.select("td:nth-child(2) a").first()?.text() ?: ""

                            decklistLinks.add(
                                MtgTop8DecklistDto(
                                    deckId = deckId,
                                    deckName = deckName,
                                    playerName = playerName,
                                    eventName = eventName,
                                    eventDate = eventDate,
                                    format = format,
                                    url = "$BASE_URL/$deckUrl"
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
