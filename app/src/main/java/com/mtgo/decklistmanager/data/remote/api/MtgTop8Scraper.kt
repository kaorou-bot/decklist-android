package com.mtgo.decklistmanager.data.remote.api

import android.util.Log
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
            Log.d(TAG, "========== MTGTop8 Event List Scraping Started ==========")
            Log.d(TAG, "Format: $format")
            Log.d(TAG, "Date filter: $date")
            Log.d(TAG, "Max events: $maxEvents")

            // 获取格式页面
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

            val events = mutableListOf<MtgTop8EventDto>()

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
                                        "$BASE_URL$href"
                                    } else {
                                        "$BASE_URL/$href"
                                    }

                                    // 提取 eventId
                                    val eventId = extractEventId(fullUrl)

                                    // 日期筛选
                                    if (date != null && col2 != date) {
                                        continue
                                    }

                                    Log.d(TAG, "Found event: $linkText -> $fullUrl")

                                    events.add(
                                        MtgTop8EventDto(
                                            eventId = eventId,
                                            eventName = linkText,
                                            eventDate = col2,
                                            format = format,
                                            eventUrl = fullUrl,
                                            deckCount = 0,
                                            eventType = extractEventType(linkText)
                                        )
                                    )
                                    processedCount++
                                }
                            }

                            if (events.size >= maxEvents) break

                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing row: ${e.message}")
                        }
                    }

                    if (events.isNotEmpty()) {
                        Log.d(TAG, "Successfully parsed ${events.size} events")
                        break
                    }
                }
            }

            if (!foundRows) {
                Log.w(TAG, "No table rows found with any selector!")
                return@withContext emptyList()
            }

            Log.d(TAG, "========== MTGTop8 Event List Scraping Completed ==========")
            Log.d(TAG, "Total events found: ${events.size}")
            events

        } catch (e: Exception) {
            Log.e(TAG, "========== MTGTop8 Event List Scraping Failed ==========")
            Log.e(TAG, "Error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取指定赛事的所有卡组（二级：Event → Decklist）
     * 通过枚举 d 参数来获取赛事下的所有卡组
     *
     * @param eventUrl 赛事 URL (如 https://mtgtop8.com/event?e=XXXX)
     * @return 赛事及其所有卡组
     */
    suspend fun fetchEventDecklists(eventUrl: String): MtgTop8EventDecklistsDto? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========== Fetching Event Decklists ==========")
            Log.d(TAG, "Event URL: $eventUrl")

            val eventId = extractEventId(eventUrl)
            val decklists = mutableListOf<MtgTop8DecklistDto>()
            var deckIndex = 1
            val maxDecks = 50  // 防止无限循环

            // 枚举 d 参数来获取所有卡组
            while (deckIndex <= maxDecks) {
                val deckUrl = buildDeckUrl(eventUrl, deckIndex)
                Log.d(TAG, "Trying deck $deckIndex: $deckUrl")

                try {
                    val doc = Jsoup.connect(deckUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(30000)
                        .get()

                    // 检查是否有卡牌数据
                    val hasCards = doc.select("div[id^=md]").isNotEmpty()
                    if (!hasCards) {
                        Log.d(TAG, "No cards found for d=$deckIndex, stopping enumeration")
                        break
                    }

                    // 提取玩家信息
                    val playerName = extractPlayerName(doc)
                    val deckName = extractDeckName(doc)

                    // 构建卡组 DTO
                    decklists.add(
                        MtgTop8DecklistDto(
                            deckId = "${eventId}_d$deckIndex",
                            deckName = deckName ?: "Deck $deckIndex",
                            playerName = playerName ?: "Player $deckIndex",
                            eventName = "",  // Will be filled from event
                            eventDate = "",  // Will be filled from event
                            format = "",     // Will be filled from event
                            url = deckUrl
                        )
                    )

                    Log.d(TAG, "Found deck $deckIndex: ${playerName ?: "Unknown"}")
                    deckIndex++

                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch deck $deckIndex: ${e.message}")
                    // 如果失败，继续尝试下一个
                    deckIndex++
                }
            }

            Log.d(TAG, "========== Event Decklists Fetched ==========")
            Log.d(TAG, "Total decklists found: ${decklists.size}")

            if (decklists.isEmpty()) {
                Log.w(TAG, "No decklists found in event")
                return@withContext null
            }

            // 构建赛事信息（需要进一步解析）
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
            Log.e(TAG, "Error fetching event decklists: ${e.message}", e)
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
     * 从赛事名称中提取赛事类型
     */
    private fun extractEventType(eventName: String): String? {
        return when {
            eventName.contains("Challenge", ignoreCase = true) -> "Challenge"
            eventName.contains("League", ignoreCase = true) -> "League"
            eventName.contains("Tournament", ignoreCase = true) -> "Tournament"
            eventName.contains("Championship", ignoreCase = true) -> "Championship"
            eventName.contains("Qualifier", ignoreCase = true) -> "Qualifier"
            else -> null
        }
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
        // 尝试多种选择器
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
        // 尝试多种选择器
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
