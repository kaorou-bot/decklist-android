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
            Log.d(TAG, "========== MTGTop8 Scraping Started ==========")
            Log.d(TAG, "Format: $format")
            Log.d(TAG, "Date filter: $date")
            Log.d(TAG, "Max events: $maxEvents")

            // 构建URL - MTGTop8 的格式页面
            val url = "$BASE_URL/format?f=$format"
            Log.d(TAG, "Fetching URL: $url")

            // MTGTop8 需要特殊的 User-Agent 和 Referer
            val doc = try {
                Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .referrer("https://www.google.com")
                    .timeout(30000)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .get()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch URL: ${e.message}", e)
                return@withContext emptyList()
            }

            Log.d(TAG, "Page fetched successfully, title: ${doc.title()}")
            Log.d(TAG, "Page HTML length: ${doc.html().length}")

            val decklistLinks = mutableListOf<MtgTop8DecklistDto>()

            // 尝试多种选择器来找到牌组列表
            val possibleSelectors = listOf(
                "tr.hover_tr",
                "tr[style*=\"hover\"]",
                "table.Stable tr",
                "tr:has(td)",
                "tr"
            )

            var foundRows = false
            for (selector in possibleSelectors) {
                val rows = doc.select(selector)
                Log.d(TAG, "Selector '$selector' found ${rows.size} rows")

                if (rows.isNotEmpty()) {
                    foundRows = true
                    var processedCount = 0

                    for (event in rows) {
                        if (processedCount >= maxEvents) break

                        try {
                            val cells = event.select("td")
                            if (cells.size < 3) {
                                Log.v(TAG, "Skipping row with only ${cells.size} cells")
                                continue
                            }

                            // 提取信息
                            val col0 = cells[0].text().trim()
                            val col1 = cells[1].text().trim()
                            val col2 = if (cells.size > 2) cells[2].text().trim() else ""

                            Log.v(TAG, "Row data: [$col0] [$col1] [$col2]")

                            // 查找牌组链接
                            val links = cells[1].select("a")
                            for (link in links) {
                                val href = link.attr("href")
                                val linkText = link.text().trim()

                                if (href.isNotEmpty() && (href.contains("deck") || href.contains("event"))) {
                                    Log.d(TAG, "Found link: $linkText -> $href")

                                    val fullUrl = if (href.startsWith("http")) {
                                        href
                                    } else if (href.startsWith("/")) {
                                        "$BASE_URL$href"
                                    } else {
                                        "$BASE_URL/$href"
                                    }

                                    decklistLinks.add(
                                        MtgTop8DecklistDto(
                                            deckId = extractDeckId(fullUrl),
                                            deckName = linkText,
                                            playerName = col1,
                                            eventName = col1,
                                            eventDate = col2,
                                            format = format,
                                            url = fullUrl
                                        )
                                    )
                                    processedCount++
                                }
                            }

                            if (decklistLinks.size >= maxEvents) break

                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing row: ${e.message}")
                        }
                    }

                    if (decklistLinks.isNotEmpty()) {
                        Log.d(TAG, "Successfully parsed ${decklistLinks.size} decklists")
                        break
                    }
                }
            }

            if (!foundRows) {
                Log.w(TAG, "No table rows found with any selector!")
            }

            Log.d(TAG, "========== MTGTop8 Scraping Completed ==========")
            Log.d(TAG, "Total decklists found: ${decklistLinks.size}")
            decklistLinks

        } catch (e: Exception) {
            Log.e(TAG, "========== MTGTop8 Scraping Failed ==========")
            Log.e(TAG, "Error: ${e.message}")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            e.printStackTrace()
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
