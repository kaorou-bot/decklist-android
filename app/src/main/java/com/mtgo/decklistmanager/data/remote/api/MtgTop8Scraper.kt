package com.mtgo.decklistmanager.data.remote.api

import com.mtgo.decklistmanager.data.remote.api.MtgTop8Config.FormatCodes
import com.mtgo.decklistmanager.data.remote.api.MtgTop8Config.getFormatName
import com.mtgo.decklistmanager.data.remote.api.MtgTop8Config.Selectors
import com.mtgo.decklistmanager.data.remote.api.MtgTop8Config.EventTypes
import com.mtgo.decklistmanager.data.remote.api.MtgTop8Config.Network
import com.mtgo.decklistmanager.util.AppLogger
import com.mtgo.decklistmanager.data.remote.api.dto.MtgTop8DecklistDto
import com.mtgo.decklistmanager.data.remote.api.dto.MtgTop8DecklistDetailDto
import com.mtgo.decklistmanager.data.remote.api.dto.MtgTop8EventDto
import com.mtgo.decklistmanager.data.remote.api.dto.MtgTop8EventDecklistsDto
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

        // 保留向后兼容的常量
        const val FORMAT_STANDARD = FormatCodes.STANDARD
        const val FORMAT_MODERN = FormatCodes.MODERN
        const val FORMAT_LEGACY = FormatCodes.LEGACY
        const val FORMAT_VINTAGE = FormatCodes.VINTAGE
        const val FORMAT_PAUPER = FormatCodes.PAUPER
        const val FORMAT_PIONEER = FormatCodes.PIONEER
        const val FORMAT_COMMANDER = FormatCodes.COMMANDER
        const val FORMAT_CEDH = FormatCodes.CEDH
    }

    /**
     * 获取指定格式的赛事列表（一级：Event 列表）
     *
     * @param format 格式代码 (ST, MO, LE, etc.)
     * @param date 可选日期筛选 (YYYY-MM-DD)，null 表示所有日期
     * @param maxEvents 最大抓取比赛数量
     * @return 赛事列表
     */
    suspend fun fetchEventList(
        format: String = FORMAT_MODERN,
        date: String? = null,
        maxEvents: Int = 10
    ): List<MtgTop8EventDto> = withContext(Dispatchers.IO) {
        try {
            AppLogger.separator(TAG, "MTGTop8 Event List Scraping Started")
            AppLogger.d(TAG, "Format: $format")
            AppLogger.d(TAG, "Date filter: $date")
            AppLogger.d(TAG, "Max events: $maxEvents")

            val url = "${MtgTop8Config.BASE_URL}/format?f=$format"
            AppLogger.d(TAG, "Fetching URL: $url")

            val doc = try {
                Jsoup.connect(url)
                    .userAgent(Network.USER_AGENT)
                    .referrer(Network.REFERRER)
                    .timeout(Network.TIMEOUT_MS)
                    .header("Accept", Network.ACCEPT_HEADER)
                    .header("Accept-Language", Network.ACCEPT_LANGUAGE)
                    .get()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to fetch URL: ${e.message}", e)
                return@withContext emptyList()
            }

            AppLogger.d(TAG, "Page fetched successfully, title: ${doc.title()}")

            val events = mutableListOf<MtgTop8EventDto>()

            var foundRows = false
            for (selector in Selectors.EVENT_ROWS) {
                val rows = doc.select(selector)
                AppLogger.d(TAG, "Selector '$selector' found ${rows.size} rows")

                if (rows.isNotEmpty()) {
                    foundRows = true
                    var processedCount = 0

                    for (event in rows) {
                        if (processedCount >= maxEvents) break

                        try {
                            val cells = event.select("td")
                            if (cells.size < 3) {
                                continue
                            }

                            // 提取信息
                            val col0 = cells[0].text().trim()
                            val col1 = cells[1].text().trim()
                            val col2 = if (cells.size > 2) cells[2].text().trim() else ""

                            // 查找赛事链接
                            val links = cells[1].select("a")
                            for (link in links) {
                                val href = link.attr("href")
                                val linkText = link.text().trim()

                                if (href.isNotEmpty() && href.contains("event")) {
                                    val fullUrl = if (href.startsWith("http")) {
                                        href
                                    } else if (href.startsWith("/")) {
                                        "${MtgTop8Config.BASE_URL}$href"
                                    } else {
                                        "${MtgTop8Config.BASE_URL}/$href"
                                    }

                                    val eventId = extractEventId(fullUrl)

                                    if (date != null && col2 != date) {
                                        continue
                                    }

                                    AppLogger.d(TAG, "Found event: $linkText -> $fullUrl")

                                    events.add(
                                        MtgTop8EventDto(
                                            eventId = eventId,
                                            eventName = linkText,
                                            eventDate = col2,
                                            format = format,
                                            eventUrl = fullUrl,
                                            deckCount = 0,
                                            eventType = EventTypes.extractEventType(linkText)
                                        )
                                    )
                                    processedCount++
                                }
                            }

                            if (events.size >= maxEvents) break

                        } catch (e: Exception) {
                            AppLogger.e(TAG, "Error parsing row: ${e.message}")
                        }
                    }

                    if (events.isNotEmpty()) {
                        AppLogger.d(TAG, "Successfully parsed ${events.size} events")
                        break
                    }
                }
            }

            if (!foundRows) {
                AppLogger.w(TAG, "No table rows found with any selector!")
                return@withContext emptyList()
            }

            AppLogger.separator(TAG, "MTGTop8 Event List Scraping Completed")
            AppLogger.d(TAG, "Total events found: ${events.size}")
            events

        } catch (e: Exception) {
            AppLogger.separator(TAG, "MTGTop8 Event List Scraping Failed")
            AppLogger.e(TAG, "Error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取指定赛事的所有卡组（从事件页面直接解析）
     * 注意：暂时只返回事件页面显示的第一个卡组
     *
     * @param eventUrl 赛事 URL (如 https://mtgtop8.com/event?e=XXXX)
     * @return 赛事及其所有卡组
     */
    suspend fun fetchEventDecklists(eventUrl: String): MtgTop8EventDecklistsDto? = withContext(Dispatchers.IO) {
        try {
            AppLogger.separator(TAG, "Fetching Event Decklists")
            AppLogger.d(TAG, "Event URL: $eventUrl")

            val eventId = extractEventId(eventUrl)
            val decklists = mutableListOf<MtgTop8DecklistDto>()

            val decklistDetail = fetchDecklistDetail(eventUrl)

            if (decklistDetail != null) {
                decklists.add(
                    MtgTop8DecklistDto(
                        deckId = "${eventId}_d1",
                        deckName = "Event Deck",
                        playerName = "Unknown",
                        eventName = "",
                        eventDate = "",
                        format = "",
                        url = eventUrl
                    )
                )
                AppLogger.d(TAG, "Found deck: Unknown - Event Deck")
                AppLogger.d(TAG, "Main deck: ${decklistDetail.mainDeck.size} cards")
                AppLogger.d(TAG, "Sideboard: ${decklistDetail.sideboardDeck.size} cards")
            }

            AppLogger.separator(TAG, "Event Decklists Fetched")
            AppLogger.d(TAG, "Total decklists found: ${decklists.size}")

            if (decklists.isEmpty()) {
                AppLogger.w(TAG, "No decklists found in event")
                return@withContext null
            }

            val eventDto = MtgTop8EventDto(
                eventId = eventId,
                eventName = "Event $eventId",
                eventDate = "",
                format = "",
                eventUrl = eventUrl,
                deckCount = decklists.size
            )

            MtgTop8EventDecklistsDto(
                event = eventDto,
                decklists = decklists
            )

        } catch (e: Exception) {
            AppLogger.e(TAG, "Error fetching event decklists: ${e.message}", e)
            null
        }
    }

    /**
     * 获取指定格式的牌组列表（两层爬取：赛事 → 卡组）
     *
     * @param format 格式代码 (ST, MO, LE, etc.)
     * @param date 可选日期筛选 (YYYY-MM-DD)，null 表示所有日期
     * @param maxEvents 最大抓取比赛数量
     * @return 卡组链接列表
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

            // 第一步：获取赛事列表
            val url = "$BASE_URL/format?f=$format"
            Log.d(TAG, "Fetching URL: $url")

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

            val eventLinks = mutableListOf<Triple<String, String, String>>() // (eventName, eventUrl, eventDate)

            // 尝试多种选择器来找到赛事列表
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

                            // 查找赛事链接
                            val links = cells[1].select("a")
                            for (link in links) {
                                val href = link.attr("href")
                                val linkText = link.text().trim()

                                if (href.isNotEmpty() && href.contains("event")) {
                                    Log.d(TAG, "Found event link: $linkText -> $href")

                                    val fullUrl = if (href.startsWith("http")) {
                                        href
                                    } else if (href.startsWith("/")) {
                                        "$BASE_URL$href"
                                    } else {
                                        "$BASE_URL/$href"
                                    }

                                    eventLinks.add(Triple(linkText, fullUrl, col2))
                                    processedCount++
                                }
                            }

                            if (eventLinks.size >= maxEvents) break

                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing row: ${e.message}")
                        }
                    }

                    if (eventLinks.isNotEmpty()) {
                        Log.d(TAG, "Successfully parsed ${eventLinks.size} events")
                        break
                    }
                }
            }

            if (!foundRows) {
                Log.w(TAG, "No table rows found with any selector!")
                return@withContext emptyList()
            }

            // 第二步：对每个赛事，返回虚拟的卡组链接
            // 实际的卡牌数据会在 fetchDecklistDetail 中从赛事页面解析
            Log.d(TAG, "========== Creating Decklists from Events ==========")
            val allDecklists = mutableListOf<MtgTop8DecklistDto>()
            var deckCounter = 0

            for ((eventName, eventUrl, eventDate) in eventLinks) {
                deckCounter++
                val deckId = "deck_${System.currentTimeMillis()}_$deckCounter"

                allDecklists.add(
                    MtgTop8DecklistDto(
                        deckId = deckId,
                        deckName = eventName,
                        playerName = "MTGTop8",
                        eventName = eventName,
                        eventDate = eventDate,
                        format = format,
                        url = eventUrl
                    )
                )
            }

            Log.d(TAG, "========== MTGTop8 Scraping Completed ==========")
            Log.d(TAG, "Total decklists found: ${allDecklists.size}")
            allDecklists

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
     * @param url 牌组页面 URL 或赛事页面 URL
     * @return 牌组详情
     */
    suspend fun fetchDecklistDetail(url: String): MtgTop8DecklistDetailDto? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching decklist detail: $url")

            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(30000)
                .get()

            // 检查是否是赛事页面（包含卡牌 div）还是卡组页面
            val hasDeckDivs = doc.select("div[id^=md]").isNotEmpty() || doc.select("div[id^=sb]").isNotEmpty()

            if (hasDeckDivs) {
                // 新格式：直接从赛事页面解析卡牌
                Log.d(TAG, "Parsing cards from event page format")

                val mainDeckCards = mutableListOf<MtgTop8DecklistDetailDto.MtgTop8CardDto>()
                val sideboardCards = mutableListOf<MtgTop8DecklistDetailDto.MtgTop8CardDto>()

                // 解析主牌 (md 前缀)
                val mainDeckElements = doc.select("div[id^=md]")
                Log.d(TAG, "Found ${mainDeckElements.size} main deck cards")

                for (cardElement in mainDeckElements) {
                    try {
                        val text = cardElement.text().trim()
                        val parts = text.split(" ", limit = 2)
                        if (parts.size >= 2) {
                            val quantity = parts[0].toIntOrNull()
                            val cardName = parts[1]

                            if (quantity != null && cardName.isNotEmpty()) {
                                mainDeckCards.add(
                                    MtgTop8DecklistDetailDto.MtgTop8CardDto(
                                        quantity = quantity,
                                        name = cardName
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing main deck card: ${e.message}")
                    }
                }

                // 解析备牌 (sb 前缀)
                val sideboardElements = doc.select("div[id^=sb]")
                Log.d(TAG, "Found ${sideboardElements.size} sideboard cards")

                for (cardElement in sideboardElements) {
                    try {
                        val text = cardElement.text().trim()
                        val parts = text.split(" ", limit = 2)
                        if (parts.size >= 2) {
                            val quantity = parts[0].toIntOrNull()
                            val cardName = parts[1]

                            if (quantity != null && cardName.isNotEmpty()) {
                                sideboardCards.add(
                                    MtgTop8DecklistDetailDto.MtgTop8CardDto(
                                        quantity = quantity,
                                        name = cardName
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing sideboard card: ${e.message}")
                    }
                }

                Log.d(TAG, "Parsed ${mainDeckCards.size} main deck cards and ${sideboardCards.size} sideboard cards")

                MtgTop8DecklistDetailDto(
                    mainDeck = mainDeckCards,
                    sideboardDeck = sideboardCards
                )
            } else {
                // 旧格式：使用表格解析
                Log.d(TAG, "Parsing cards from table format")

                // 解析主牌
                val mainDeck = parseDeckSection(doc, "主牌", "Maindeck")
                // 解备牌
                val sideboard = parseDeckSection(doc, "备牌", "Sideboard")

                MtgTop8DecklistDetailDto(
                    mainDeck = mainDeck,
                    sideboardDeck = sideboard
                )
            }

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
     * 从 URL 中提取赛事 ID
     */
    private fun extractEventId(url: String): String {
        val regex = Regex("[?&]e=([^&]+)")
        return regex.find(url)?.groupValues?.get(1) ?: ""
    }

    /**
     * 从赛事名称中提取赛事类型（已移至 EventTypes）
     */
    private fun extractEventType(eventName: String): String? {
        return EventTypes.extractEventType(eventName)
    }

    /**
     * 构建卡组 URL（添加 d 参数）
     */
    private fun buildDeckUrl(eventUrl: String, deckIndex: Int): String {
        return if (eventUrl.contains("?")) {
            "$eventUrl&d=$deckIndex"
        } else {
            "$eventUrl?d=$deckIndex"
        }
    }

    /**
     * 从卡组页面提取玩家名称
     */
    private fun extractPlayerName(doc: Document): String? {
        val possibleSelectors = listOf(
            "div.O0",
            "div[class*=\"player\"]",
            "td:contains(Player)",
            "h3",
            "title"
        )

        for (selector in possibleSelectors) {
            val elements = doc.select(selector)
            for (element in elements) {
                val text = element.text().trim()
                if (text.isNotEmpty() && text.length < 50) {
                    return text
                }
            }
        }

        return null
    }

    /**
     * 从卡组页面提取卡组名称
     */
    private fun extractDeckName(doc: Document): String? {
        val possibleSelectors = listOf(
            "h2",
            "h1",
            "div[class*=\"deck\"]",
            "title"
        )

        for (selector in possibleSelectors) {
            val elements = doc.select(selector)
            for (element in elements) {
                val text = element.text().trim()
                if (text.isNotEmpty() && text.length < 100) {
                    return text
                }
            }
        }

        return null
    }
}
