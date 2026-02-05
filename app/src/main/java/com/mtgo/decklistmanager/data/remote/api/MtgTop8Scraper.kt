package com.mtgo.decklistmanager.data.remote.api

import android.util.Log
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
     * 测试网站连接 - 调试用
     * 返回网站是否可访问以及基本信息
     */
    suspend fun testConnection(format: String = FORMAT_MODERN): String = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        // 重试3次
        repeat(3) { attempt ->
            try {
                AppLogger.d(TAG, "测试连接 - 尝试 ${attempt + 1}/3")

                val url = "${MtgTop8Config.BASE_URL}/format?f=$format"

                val doc = Jsoup.connect(url)
                    .userAgent(Network.USER_AGENT)
                    .referrer(Network.REFERRER)
                    .timeout(Network.TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get()

                val title = doc.title()
                val htmlLength = doc.html().length

                // 尝试查找表格
                val tables = doc.select("table")
                val tableCount = tables.size

                // 尝试查找链接
                val eventLinks = doc.select("a[href*=\"event\"]")
                val linkCount = eventLinks.size

                // 获取前几个链接作为样本
                val sampleLinks = eventLinks.take(3).joinToString("\n") { link ->
                    "  - ${link.text()}: ${link.attr("href")}"
                }

                return@withContext """
                ✓ 连接成功！
                URL: $url
                页面标题: $title
                HTML长度: $htmlLength 字符
                表格数量: $tableCount
                赛事链接数量: $linkCount

                前几个赛事链接:
                $sampleLinks
                """.trimIndent()

            } catch (e: Exception) {
                AppLogger.w(TAG, "测试连接 - 尝试 ${attempt + 1}/3 失败: ${e.message}")
                lastException = e
                if (attempt < 2) {
                    // 等待2秒后重试
                    kotlinx.coroutines.delay(2000)
                }
            }
        }

        // 所有重试都失败
        """
        ✗ 连接失败（已重试3次）: ${lastException?.message}

        可能的原因:
        1. 网络连接不稳定
        2. 网站响应缓慢
        3. 防火墙或VPN阻止连接

        建议:
        - 检查网络连接
        - 尝试切换WiFi/移动数据
        - 关闭VPN或代理
        - 稍后再试

        错误详情: ${lastException?.stackTraceToString()?.take(500)}
        """.trimIndent()
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
                    .followRedirects(true)
                    .maxBodySize(0)
                    .ignoreHttpErrors(true)
                    .get()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to fetch URL: ${e.message}", e)
                Log.e(TAG, "Connection error: ${e::class.java.simpleName} - ${e.message}")
                Log.e(TAG, "URL was: $url")
                Log.e(TAG, "Timeout is set to ${Network.TIMEOUT_MS}ms")
                // 尝试重试一次
                try {
                    AppLogger.d(TAG, "第一次连接失败，尝试重试...")
                    kotlinx.coroutines.delay(2000)
                    Jsoup.connect(url)
                        .userAgent(Network.USER_AGENT)
                        .referrer(Network.REFERRER)
                        .timeout(Network.TIMEOUT_MS)
                        .followRedirects(true)
                        .maxBodySize(0)
                        .ignoreHttpErrors(true)
                        .get()
                } catch (retryException: Exception) {
                    Log.e(TAG, "重试也失败: ${retryException.message}")
                    return@withContext emptyList()
                }
            }

            AppLogger.d(TAG, "Page fetched successfully, title: ${doc.title()}")
            AppLogger.d(TAG, "Page HTML length: ${doc.html().length}")
            Log.d(TAG, "Page title: ${doc.title()}")
            Log.d(TAG, "HTML length: ${doc.html().length}")

            // 尝试多种选择器策略
            val events = mutableListOf<MtgTop8EventDto>()

            // 策略1：使用配置的选择器
            for (selector in Selectors.EVENT_ROWS) {
                val rows = doc.select(selector)
                AppLogger.d(TAG, "Selector '$selector' found ${rows.size} rows")

                if (rows.isNotEmpty()) {
                    val parsedEvents = parseEventRows(rows, format, date, maxEvents)
                    if (parsedEvents.isNotEmpty()) {
                        events.addAll(parsedEvents)
                        AppLogger.d(TAG, "Successfully parsed ${parsedEvents.size} events using selector: $selector")
                        break
                    }
                }
            }

            // 策略2：如果第一个策略失败，尝试查找所有包含链接的表格行
            if (events.isEmpty()) {
                AppLogger.w(TAG, "Primary selectors failed, trying fallback strategy")
                val allTables = doc.select("table")
                AppLogger.d(TAG, "Found ${allTables.size} tables on page")

                for (table in allTables) {
                    val rows = table.select("tr")
                    AppLogger.d(TAG, "Table has ${rows.size} rows")

                    if (rows.size > 1) {
                        val parsedEvents = parseEventRows(rows, format, date, maxEvents)
                        if (parsedEvents.isNotEmpty()) {
                            events.addAll(parsedEvents)
                            AppLogger.d(TAG, "Fallback strategy parsed ${parsedEvents.size} events")
                            break
                        }
                    }
                }
            }

            // 策略3：如果仍然失败，尝试查找所有包含"event"的链接
            if (events.isEmpty()) {
                AppLogger.w(TAG, "Fallback strategy failed, trying link-based strategy")
                val allLinks = doc.select("a[href*=\"event\"]")
                AppLogger.d(TAG, "Found ${allLinks.size} event links")

                for (link in allLinks) {
                    if (events.size >= maxEvents) break

                    try {
                        val href = link.attr("href")
                        val linkText = link.text().trim()

                        if (href.isNotEmpty()) {
                            val fullUrl = if (href.startsWith("http")) {
                                href
                            } else if (href.startsWith("/")) {
                                "${MtgTop8Config.BASE_URL}$href"
                            } else {
                                "${MtgTop8Config.BASE_URL}/$href"
                            }

                            val eventId = extractEventId(fullUrl)

                            AppLogger.d(TAG, "Found event link: $linkText -> $fullUrl")

                            events.add(
                                MtgTop8EventDto(
                                    eventId = eventId,
                                    eventName = linkText,
                                    eventDate = "",
                                    format = format,
                                    eventUrl = fullUrl,
                                    deckCount = 0,
                                    eventType = EventTypes.extractEventType(linkText)
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing link: ${e.message}")
                    }
                }
            }

            AppLogger.d(TAG, "Total events found: ${events.size}")

            if (events.isEmpty()) {
                AppLogger.e(TAG, "No events found! Page structure may have changed.")
                AppLogger.d(TAG, "Page HTML sample (first 2000 chars): ${doc.html().take(2000)}")
                Log.e(TAG, "No events found! HTML sample:")
                Log.e(TAG, doc.html().take(2000))
            } else {
                // 输出找到的第一个赛事信息
                val firstEvent = events[0]
                Log.d(TAG, "First event found:")
                Log.d(TAG, "  - Name: ${firstEvent.eventName}")
                Log.d(TAG, "  - URL: ${firstEvent.eventUrl}")
                Log.d(TAG, "  - Date: ${firstEvent.eventDate}")
                Log.d(TAG, "  - Format: ${firstEvent.format}")
            }

            events

        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception in fetchEventList: ${e.message}", e)
            Log.e(TAG, "Exception: ${e::class.java.simpleName} - ${e.message}")
            emptyList()
        }
    }

    /**
     * 解析赛事行
     */
    private fun parseEventRows(
        rows: Elements,
        format: String,
        date: String?,
        maxEvents: Int
    ): List<MtgTop8EventDto> {
        val events = mutableListOf<MtgTop8EventDto>()
        var processedCount = 0

        for (event in rows) {
            if (processedCount >= maxEvents) break

            try {
                val cells = event.select("td")
                if (cells.size < 3) {
                    continue
                }

                // 提取信息
                // HTML结构：
                // col0: 图标 (MTGO/Paper)
                // col1: 赛事名称和链接
                // col2: 星级
                // col3: 日期 (DD/MM/YY)
                // val col1 未使用，直接使用 cells[1]
                val col3 = if (cells.size > 3) cells[3].text().trim() else ""

                // 在第2列（col1）查找赛事链接
                val linkColumn = cells[1]
                val links = linkColumn.select("a")

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

                        // 转换日期格式：从 DD/MM/YY 转换为 YYYY-MM-DD
                        val formattedDate = if (col3.isNotEmpty()) {
                            convertDateToStandard(col3)
                        } else {
                            ""
                        }

                        // 如果指定了日期筛选，检查是否匹配
                        if (date != null && formattedDate.isNotEmpty() && formattedDate != date) {
                            continue
                        }

                        AppLogger.d(TAG, "Found event: $linkText -> $fullUrl, date: $col3 -> $formattedDate")

                        events.add(
                            MtgTop8EventDto(
                                eventId = eventId,
                                eventName = linkText,
                                eventDate = formattedDate,  // 使用转换后的日期
                                format = format,
                                eventUrl = fullUrl,
                                deckCount = 0,
                                eventType = EventTypes.extractEventType(linkText)
                            )
                        )
                        processedCount++
                        break // 每行只处理一个赛事
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing event row: ${e.message}")
            }
        }

        return events
    }

    /**
     * 转换日期格式：从 DD/MM/YY 转换为 YYYY-MM-DD
     */
    private fun convertDateToStandard(dateStr: String): String {
        return try {
            val parts = dateStr.split("/")
            if (parts.size == 3) {
                val day = parts[0].padStart(2, '0')
                val month = parts[1].padStart(2, '0')
                val year = "20${parts[2]}" // 假设是20xx年
                "$year-$month-$day"
            } else {
                dateStr // 如果格式不匹配，返回原字符串
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to convert date: $dateStr")
            dateStr
        }
    }

    /**
     * 从赛事URL提取赛事信息（名称、日期、赛制）
     */
    private suspend fun extractEventInfo(eventUrl: String): MtgTop8EventDto? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(eventUrl)
                .userAgent(Network.USER_AGENT)
                .referrer(Network.REFERRER)
                .timeout(Network.TIMEOUT_MS)
                .get()

            // 提取赛事名称
            // 注意：div.event_title 可能包含卡组名称而不是赛事名称
            // 我们需要从页面标题或其他地方提取真正的赛事名称
            var eventName = ""

            // 策略1：从页面标题提取（通常是 "Event Name" 或 "MTGTop8 - Event Name"）
            val pageTitle = doc.title().trim()
            AppLogger.d(TAG, "Page title: '$pageTitle'")

            // 移除常见的标题前缀
            eventName = pageTitle.replace("MTGTop8 - ", "")
                .replace("MTGTop8", "")
                .replace("- MTGTop8", "")
                .trim()

            // 策略2：如果页面标题包含卡组信息（有"by"或"-"），尝试从其他地方提取
            if (eventName.contains(" by ", ignoreCase = true) || eventName.split(" - ").size >= 3) {
                // 页面标题可能是卡组页面，需要从其他地方提取赛事名称
                // 尝试查找 h3 标题
                val h3Elements = doc.select("h3")
                for (h3 in h3Elements) {
                    val text = h3.text().trim()
                    // 赛事名称通常比较短，不包含"by"或过长的描述
                    if (text.isNotEmpty() &&
                        !text.contains(" by ", ignoreCase = true) &&
                        text.length < 100 &&
                        !text.matches(Regex("^#\\d+.*"))) {
                        eventName = text
                        AppLogger.d(TAG, "Found event name from h3: '$eventName'")
                        break
                    }
                }

                // 如果还是没找到，尝试从 div.event_title 中提取（但要排除卡组信息）
                if (eventName.isEmpty() || eventName.contains(" by ", ignoreCase = true)) {
                    val titleElements = doc.select("div.event_title")
                    if (titleElements.isNotEmpty()) {
                        val titleText = titleElements[0].text().trim()
                        // 如果包含排名符号，可能是卡组名称，跳过
                        if (!titleText.matches(Regex("^#\\d+.*"))) {
                            // 尝试提取赛事名称（在"by"之前的部分）
                            val byIndex = titleText.indexOf(" by ", ignoreCase = true)
                            eventName = if (byIndex > 0) {
                                titleText.substring(0, byIndex).trim()
                            } else {
                                titleText
                            }
                            AppLogger.d(TAG, "Found event name from event_title: '$eventName'")
                        }
                    }
                }
            }

            // 如果仍然为空，使用URL中的eventId作为临时名称
            if (eventName.isEmpty()) {
                val eventId = extractEventId(eventUrl)
                eventName = "Event $eventId"
                AppLogger.w(TAG, "Could not extract event name, using placeholder: '$eventName'")
            }

            AppLogger.d(TAG, "Final event name: '$eventName'")

            // 提取日期 - 从 meta 区域获取
            var eventDate = ""
            val metaDivs = doc.select("div.S14")
            for (div in metaDivs) {
                val text = div.text().trim()
                val datePattern = Regex("\\d{2}/\\d{2}/\\d{2}")
                if (datePattern.containsMatchIn(text)) {
                    val dateMatch = datePattern.find(text)
                    if (dateMatch != null) {
                        val rawDate = dateMatch.value
                        // 转换日期格式：从 MM/DD/YY 转换为 YYYY-MM-DD
                        eventDate = convertDateToStandard(rawDate)
                        break
                    }
                }
            }

            // 从URL提取format参数（如果有的话）
            val formatPattern = Regex("[?&]f=([^&]+)")
            val formatMatch = formatPattern.find(eventUrl)
            val format = formatMatch?.groupValues?.get(1) ?: ""

            val eventId = extractEventId(eventUrl)

            MtgTop8EventDto(
                eventId = eventId,
                eventName = eventName,
                eventDate = eventDate,
                format = format,
                eventUrl = eventUrl,
                deckCount = 0,
                eventType = null
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error extracting event info: ${e.message}")
            null
        }
    }

    /**
     * 获取指定赛事的所有卡组（枚举所有 d 参数）
     *
     * @param eventUrl 赛事 URL (如 https://mtgtop8.com/event?e=XXXX)
     * @param maxDecks 最大抓取卡组数量（0 表示全部）
     * @return 赛事及其所有卡组
     */
    suspend fun fetchEventDecklists(
        eventUrl: String,
        maxDecks: Int = 0
    ): MtgTop8EventDecklistsDto? = withContext(Dispatchers.IO) {
        try {
            AppLogger.separator(TAG, "Fetching Event Decklists")
            AppLogger.d(TAG, "Event URL: $eventUrl")
            AppLogger.d(TAG, "Max decks: $maxDecks")

            val eventId = extractEventId(eventUrl)
            val decklists = mutableListOf<MtgTop8DecklistDto>()

            // 首先提取正确的赛事信息
            val eventInfo = extractEventInfo(eventUrl)
            if (eventInfo == null) {
                AppLogger.w(TAG, "Failed to extract event info from URL")
                return@withContext null
            }

            AppLogger.d(TAG, "Event info: name=${eventInfo.eventName}, date=${eventInfo.eventDate}, format=${eventInfo.format}")

            // 第一步：从赛事页面提取第一个卡组的 d 参数
            val firstDeckId = extractFirstDeckId(eventUrl)
            if (firstDeckId == null) {
                AppLogger.w(TAG, "Failed to extract first deck ID from event page")
                return@withContext null
            }

            AppLogger.d(TAG, "First deck ID: $firstDeckId")

            // 第二步：统计实际卡组数量，并发枚举
            val actualDeckCount = countDecklistsInEvent(eventUrl)
            val decksToCheck = if (actualDeckCount > 0) {
                actualDeckCount
            } else {
                50 // 如果无法统计，使用默认值
            }

            AppLogger.d(TAG, "Will check $decksToCheck deck IDs")

            val checkRange = firstDeckId..(firstDeckId + decksToCheck - 1)

            AppLogger.d(TAG, "Concurrent checking deck IDs from $firstDeckId to ${firstDeckId + decksToCheck - 1}")

            // 并发检查所有可能的ID
            val results = checkRange.map { deckId ->
                async {
                    try {
                        val deckUrl = buildDeckUrl(eventUrl, deckId)
                        // 尝试获取卡组详情
                        val decklistDetail = fetchDecklistDetail(deckUrl)

                        if (decklistDetail != null &&
                            (decklistDetail.mainDeck.isNotEmpty() || decklistDetail.sideboardDeck.isNotEmpty())) {

                            // 从页面提取玩家名称、卡组名称、排名和日期
                            val playerInfo = extractPlayerAndDeckName(deckUrl)

                            // 验证套牌是否属于目标赛事
                            // 检查赛事 URL 是否匹配
                            val deckEventUrl = extractEventUrlFromDeckPage(deckUrl)
                            val belongsToEvent = deckEventUrl != null && deckEventUrl == eventUrl

                            if (!belongsToEvent) {
                                AppLogger.w(TAG, "Deck #$deckId belongs to different event!")
                                AppLogger.w(TAG, "  Target event URL: $eventUrl")
                                AppLogger.w(TAG, "  Deck event URL: $deckEventUrl")
                                AppLogger.w(TAG, "  Target event name: '${eventInfo.eventName}'")
                                AppLogger.w(TAG, "  Deck event name: '${playerInfo.eventName}'")
                                AppLogger.w(TAG, "  Deck: ${playerInfo.playerName} - ${playerInfo.deckName}")
                                null  // 跳过不属于此赛事的套牌
                            } else {
                                AppLogger.d(TAG, "Found deck #$deckId: ${playerInfo.playerName} - ${playerInfo.deckName}")

                                Pair(deckId, MtgTop8DecklistDto(
                                    deckId = "${eventId}_d$deckId",
                                    deckName = playerInfo.deckName,
                                    playerName = playerInfo.playerName,
                                    eventName = eventInfo.eventName,
                                    eventDate = playerInfo.date.ifEmpty { eventInfo.eventDate },
                                    record = playerInfo.record,
                                    format = eventInfo.format,
                                    url = deckUrl
                                ))
                            }
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        AppLogger.d(TAG, "Error checking deck ID $deckId: ${e.message}")
                        null
                    }
                }
            }.awaitAll()

            // 过滤掉null值并按ID排序
            val validDecklists = results.filterNotNull().sortedBy { it.first }.map { it.second }
            decklists.addAll(validDecklists)

            // 应用maxDecks限制
            val finalDecklists = if (maxDecks > 0 && decklists.size > maxDecks) {
                decklists.take(maxDecks)
            } else {
                decklists
            }

            AppLogger.separator(TAG, "Event Decklists Fetched")
            AppLogger.d(TAG, "Total decklists found: ${finalDecklists.size}")

            if (finalDecklists.isEmpty()) {
                AppLogger.w(TAG, "No decklists found in event")
                return@withContext null
            }

            // 使用提取的正确赛事信息
            val eventDto = MtgTop8EventDto(
                eventId = eventId,
                eventName = eventInfo.eventName,
                eventDate = eventInfo.eventDate,
                format = eventInfo.format,
                eventUrl = eventUrl,
                deckCount = finalDecklists.size,
                eventType = eventInfo.eventType
            )

            MtgTop8EventDecklistsDto(
                event = eventDto,
                decklists = finalDecklists
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
            val url = "${MtgTop8Config.BASE_URL}/format?f=$format"
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
                                        "${MtgTop8Config.BASE_URL}$href"
                                    } else {
                                        "${MtgTop8Config.BASE_URL}/$href"
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
     * 从赛事页面提取第一个卡组的 d 参数
     * 从页面中的 JavaScript 变量或链接中提取 d 参数
     */
    private suspend fun extractFirstDeckId(eventUrl: String): Int? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(eventUrl)
                .userAgent(Network.USER_AGENT)
                .referrer(Network.REFERRER)
                .timeout(Network.TIMEOUT_MS)
                .get()

            // 方法1：从 JavaScript 代码中查找 d 参数
            val scripts = doc.select("script")
            for (script in scripts) {
                val scriptHtml = script.html()
                val deckIdPattern = Regex("[?&]d=(\\d+)")
                val match = deckIdPattern.find(scriptHtml)
                if (match != null) {
                    val deckId = match.groupValues[1].toIntOrNull()
                    if (deckId != null && deckId > 0) {
                        AppLogger.d(TAG, "Found deck ID from script: $deckId")
                        return@withContext deckId
                    }
                }
            }

            // 方法2：从 HTML 中的链接提取 d 参数
            val links = doc.select("a[href~=d=\\d+]")
            for (link in links) {
                val href = link.attr("href")
                val deckIdPattern = Regex("[?&]d=(\\d+)")
                val match = deckIdPattern.find(href)
                if (match != null) {
                    val deckId = match.groupValues[1].toIntOrNull()
                    if (deckId != null && deckId > 0) {
                        AppLogger.d(TAG, "Found deck ID from link: $deckId")
                        return@withContext deckId
                    }
                }
            }

            AppLogger.w(TAG, "Could not extract deck ID from event page")
            null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error extracting deck ID: ${e.message}")
            null
        }
    }

    /**
     * 从赛事页面统计实际有多少个卡组
     * 通过统计页面中所有有效的d参数来获取
     */
    private suspend fun countDecklistsInEvent(eventUrl: String): Int = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(eventUrl)
                .userAgent(Network.USER_AGENT)
                .referrer(Network.REFERRER)
                .timeout(Network.TIMEOUT_MS)
                .get()

            // 查找所有包含d参数的链接
            val links = doc.select("a[href~=d=\\d+]")
            val deckIds = mutableSetOf<Int>()

            for (link in links) {
                val href = link.attr("href")
                val deckIdPattern = Regex("[?&]d=(\\d+)")
                val match = deckIdPattern.find(href)
                if (match != null) {
                    val deckId = match.groupValues[1].toIntOrNull()
                    if (deckId != null && deckId > 0) {
                        deckIds.add(deckId)
                    }
                }
            }

            AppLogger.d(TAG, "Found ${deckIds.size} decklists in event page")
            deckIds.size
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error counting decklists: ${e.message}")
            0
        }
    }

    /**
     * 从卡组 URL 提取玩家名称、卡组名称、日期和排名
     */
    private suspend fun extractPlayerAndDeckName(deckUrl: String): PlayerDeckInfo = withContext(Dispatchers.IO) {
        try {
            AppLogger.d(TAG, "Extracting player/deck info from: $deckUrl")
            val doc = Jsoup.connect(deckUrl)
                .userAgent(Network.USER_AGENT)
                .timeout(Network.TIMEOUT_MS)
                .get()

            AppLogger.d(TAG, "Page title: ${doc.title()}")
            AppLogger.d(TAG, "Page HTML length: ${doc.html().length}")

            // 提取玩家名称 - 从 a.player_big 链接获取
            var playerName = "Unknown"
            val playerElements = doc.select("a.player_big")
            AppLogger.d(TAG, "Found ${playerElements.size} a.player_big elements")
            if (playerElements.isNotEmpty()) {
                playerName = playerElements[0].text().trim()
                if (playerName.isEmpty()) {
                    playerName = "Unknown"
                }
                AppLogger.d(TAG, "Player name from a.player_big: '$playerName'")
            }

            // 提取卡组名称和排名 - 尝试多种选择器
            var deckName = "Unknown Deck"
            var record = ""

            // 策略1: 从 div.event_title 中查找
            val titleElements = doc.select("div.event_title")
            AppLogger.d(TAG, "Found ${titleElements.size} div.event_title elements")

            if (titleElements.isNotEmpty()) {
                // 打印所有找到的标题以便调试
                titleElements.forEachIndexed { index, element ->
                    AppLogger.d(TAG, "div.event_title [$index]: '${element.text()}'")
                }
            }

            // 策略2: 从页面标题提取（通常是 "Event Name - Deck Name - Player Name"）
            val pageTitle = doc.title()
            AppLogger.d(TAG, "Page title: '$pageTitle'")

            // 策略3: 尝试从所有h3标签中查找
            val h3Elements = doc.select("h3")
            AppLogger.d(TAG, "Found ${h3Elements.size} h3 elements")
            h3Elements.forEachIndexed { index, element ->
                AppLogger.d(TAG, "h3 [$index]: '${element.text()}'")
            }

            // 策略4: 查找包含套牌名称的元素
            // MTGTop8通常在页面中有套牌名称，格式可能是：
            // - "Deck Name by Player Name"
            // - "Deck Name - Player Name"
            // - "#X Deck Name - Player Name"

            // 尝试从div.event_title中提取
            for (titleElement in titleElements) {
                val titleText = titleElement.text().trim()
                AppLogger.d(TAG, "Checking title: '$titleText'")

                // 检查是否包含排名格式
                val rankPattern = Regex("^#([\\d\\-]+)\\s+")
                val rankMatch = rankPattern.find(titleText)

                if (rankMatch != null) {
                    record = "#${rankMatch.groupValues[1]}"
                    AppLogger.d(TAG, "Found record: $record")

                    // 移除排名并提取套牌名称
                    val withoutRank = titleText.replace(rankPattern, "")
                    val parts = withoutRank.split(" - ", limit = 2)

                    if (parts.isNotEmpty()) {
                        val candidateDeckName = parts[0].trim()
                        // 验证提取的名称不是玩家名称
                        if (candidateDeckName.isNotEmpty() &&
                            candidateDeckName != playerName &&
                            !candidateDeckName.matches(Regex("^#\\d+")) &&
                            !candidateDeckName.equals("Unknown", ignoreCase = true)) {
                            deckName = candidateDeckName
                            AppLogger.d(TAG, "Extracted deck name from event_title: '$deckName'")
                            break
                        }
                    }
                } else {
                    // 没有排名，尝试直接提取
                    val parts = titleText.split(" - ", limit = 2)
                    if (parts.size >= 2) {
                        val candidateDeckName = parts[0].trim()
                        // val candidatePlayerName = parts[1].trim() // 未使用

                        // 检查第一部分是否是玩家名称（如果和a.player_big不同，可能是套牌名称）
                        if (candidateDeckName.isNotEmpty() &&
                            candidateDeckName != playerName &&
                            !candidateDeckName.matches(Regex("^#\\d+.*")) &&
                            !candidateDeckName.equals("Unknown", ignoreCase = true)) {
                            deckName = candidateDeckName
                            AppLogger.d(TAG, "Extracted deck name (no rank): '$deckName'")
                            break
                        }
                    }
                }
            }

            // 如果还没找到，尝试从页面标题提取
            if (deckName == "Unknown Deck" && pageTitle.contains(" - ")) {
                val parts = pageTitle.split(" - ", limit = 3)
                if (parts.size >= 2) {
                    // 标题格式可能是: "Event - Deck - Player" 或 "Event - Player - Deck"
                    // 尝试找到套牌名称
                    for (part in parts) {
                        val trimmedPart = part.trim()
                        if (trimmedPart != playerName &&
                            trimmedPart.isNotEmpty() &&
                            !trimmedPart.equals("Unknown", ignoreCase = true)) {
                            deckName = trimmedPart
                            AppLogger.d(TAG, "Extracted deck name from page title: '$deckName'")
                            break
                        }
                    }
                }
            }

            // 提取日期 - 从 meta 区域获取
            var eventDate = ""
            val metaDivs = doc.select("div.S14")
            AppLogger.d(TAG, "Found ${metaDivs.size} div.S14 elements")
            for (div in metaDivs) {
                val text = div.text().trim()
                AppLogger.d(TAG, "Checking S14 div: '$text'")
                val datePattern = Regex("\\d{2}/\\d{2}/\\d{2}")
                if (datePattern.containsMatchIn(text)) {
                    val dateMatch = datePattern.find(text)
                    if (dateMatch != null) {
                        val rawDate = dateMatch.value
                        // 转换日期格式：从 MM/DD/YY 转换为 YYYY-MM-DD
                        eventDate = convertDateToStandard(rawDate)
                        AppLogger.d(TAG, "Found date: $rawDate -> $eventDate")
                        break
                    }
                }
            }

            // 提取赛事名称和URL - 从页面中查找
            var eventName = ""
            var eventUrl = ""

            // 策略1: 从 div.event_title 的第一个元素提取（通常是赛事名称）
            if (titleElements.isNotEmpty()) {
                val firstTitleText = titleElements[0].text().trim()
                // 移除排名前缀
                val rankPattern = Regex("^#([\\d\\-]+)\\s+")
                eventName = firstTitleText.replace(rankPattern, "").trim()
                AppLogger.d(TAG, "Extracted event name from event_title: '$eventName'")

                // 尝试从第一个 div.event_title 中提取赛事链接
                val linkElement = titleElements[0].selectFirst("a")
                if (linkElement != null) {
                    eventUrl = linkElement.attr("href")
                    if (eventUrl.isNotEmpty()) {
                        // 确保是完整URL
                        if (!eventUrl.startsWith("http")) {
                            eventUrl = "https://mtgtop8.com/$eventUrl"
                        }
                        AppLogger.d(TAG, "Extracted event URL: $eventUrl")
                    }
                }
            }

            // 策略2: 如果没找到，尝试从页面标题提取
            if (eventName.isEmpty() && pageTitle.contains(" - ")) {
                val parts = pageTitle.split(" - ", limit = 2)
                if (parts.isNotEmpty()) {
                    eventName = parts[0].trim()
                    AppLogger.d(TAG, "Extracted event name from page title: '$eventName'")
                }
            }

            AppLogger.d(TAG, "Final result - Player: '$playerName', Deck: '$deckName', Record: '$record', Date: '$eventDate', Event: '$eventName', EventURL: '$eventUrl'")

            PlayerDeckInfo(
                playerName = playerName,
                deckName = deckName,
                record = record,
                date = eventDate,
                eventName = eventName
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error extracting player/deck info: ${e.message}", e)
            PlayerDeckInfo("Unknown", "Unknown Deck", "", "", "")
        }
    }

    /**
     * 从套牌页面提取赛事 URL
     */
    private suspend fun extractEventUrlFromDeckPage(deckUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(deckUrl)
                .userAgent(Network.USER_AGENT)
                .timeout(Network.TIMEOUT_MS)
                .get()

            // 从 div.event_title 中查找赛事链接
            val titleElements = doc.select("div.event_title a")
            if (titleElements.isNotEmpty()) {
                var href = titleElements[0].attr("href")
                if (href.isNotEmpty()) {
                    // 确保是完整URL
                    if (!href.startsWith("http")) {
                        href = "https://mtgtop8.com/$href"
                    }
                    return@withContext href
                }
            }

            null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error extracting event URL: ${e.message}")
            null
        }
    }

    /**
     * 玩家和卡组信息数据类
     */
    private data class PlayerDeckInfo(
        val playerName: String,
        val deckName: String,
        val record: String,
        val date: String,
        val eventName: String  // 套牌所属的赛事名称
    )
}
