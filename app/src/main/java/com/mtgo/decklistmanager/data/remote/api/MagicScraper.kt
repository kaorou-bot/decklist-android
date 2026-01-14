package com.mtgo.decklistmanager.data.remote.api

import com.mtgo.decklistmanager.data.remote.api.dto.MtgoDecklistDetailDto
import com.mtgo.decklistmanager.data.remote.api.dto.MtgoDecklistLinkDto
import com.mtgo.decklistmanager.data.remote.api.dto.MtgoCardDto
import com.mtgo.decklistmanager.data.remote.api.dto.MtgoCardAttributesDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Magic.gg Scraper - Magic.gg 网站爬虫
 * 从 Magic.gg 官方赛事页面爬取 MTGO 牌组数据
 */
@Singleton
class MagicScraper @Inject constructor() {

    companion object {
        private const val BASE_URL = "https://magic.gg"

        // MTGO Champions Showcase 2025 赛事URL
        private val SHOWCASE_URLS = listOf(
            "https://magic.gg/decklists/2026-magic-online-champions-showcase-season-1-modern-decklists",
            "https://magic.gg/decklists/2025-magic-online-champions-showcase-season-3-modern-decklists",
            "https://magic.gg/decklists/2025-magic-online-champions-showcase-season-2-modern-decklists",
            "https://magic.gg/decklists/2025-magic-online-champions-showcase-season-1-modern-decklists"
        )
    }

    /**
     * 获取所有可用的牌组赛事
     */
    suspend fun fetchDecklistPage(year: Int = 2025, month: Int = 1): List<MtgoDecklistLinkDto> {
        return try {
            val links = mutableListOf<MtgoDecklistLinkDto>()

            // 爬取所有 Champions Showcase 赛事
            for (url in SHOWCASE_URLS) {
                try {
                    val decklists = fetchShowcaseDecklists(url)
                    links.addAll(decklists)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // 继续尝试下一个URL
                }
            }

            links
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 从特定的 Showcase 页面获取牌组列表
     */
    private suspend fun fetchShowcaseDecklists(url: String): List<MtgoDecklistLinkDto> {
        return try {
            val doc = Jsoup.connect(url)
                .timeout(30000)
                .userAgent("Mozilla/5.0 (Android 13; Mobile)")
                .get()

            // 直接从HTML中查找 <deck-list> 标签
            val deckLists = doc.select("deck-list")

            if (deckLists.isEmpty()) {
                return emptyList()
            }

            val links = mutableListOf<MtgoDecklistLinkDto>()

            for (deckList in deckLists) {
                try {
                    val player = deckList.attr("deck-title")
                    val subtitle = deckList.attr("subtitle")
                    val eventDate = deckList.attr("event-date")
                    val eventName = deckList.attr("event-name")
                    val format = deckList.attr("format")

                    if (player.isNotEmpty()) {
                        links.add(
                            MtgoDecklistLinkDto(
                                url = "$url#$player".replace(" ", "-"),
                                eventName = eventName,
                                format = format,
                                date = formatDate(eventDate),
                                eventType = "Champions Showcase"
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            links
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取并解析牌组详情页
     */
    suspend fun fetchDecklistDetail(url: String): MtgoDecklistDetailDto? {
        return try {
            // 移除 URL 中的片段标识符
            val cleanUrl = url.substringBefore("#")

            val doc = Jsoup.connect(cleanUrl)
                .timeout(30000)
                .userAgent("Mozilla/5.0 (Android 13; Mobile)")
                .get()

            // 从URL中提取玩家名称
            val playerName = url.substringAfter("#", "").replace("-", " ")

            // 查找所有 <deck-list> 标签
            val deckLists = doc.select("deck-list")

            // 找到匹配的牌组
            for (deckList in deckLists) {
                val player = deckList.attr("deck-title")

                // 检查是否匹配URL中的玩家名称
                if (player.equals(playerName, ignoreCase = true) ||
                    playerName.isEmpty() ||
                    player.replace(" ", "-").equals(playerName.replace(" ", "-"), ignoreCase = true)) {
                    return parseSingleDecklist(deckList, player)
                }
            }

            // 如果没有找到匹配的，返回第一个
            if (deckLists.isNotEmpty()) {
                val firstDeck = deckLists.first() ?: return null
                val player = firstDeck.attr("deck-title")
                return parseSingleDecklist(firstDeck, player)
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 解析单个牌组
     */
    private fun parseSingleDecklist(deckListElement: org.jsoup.nodes.Element, player: String): MtgoDecklistDetailDto {
        // 提取主牌
        val mainDeckElement = deckListElement.selectFirst("main-deck")
        val mainDeck = if (mainDeckElement != null) {
            parseCards(mainDeckElement.html())
        } else {
            emptyList()
        }

        // 提取备牌
        val sideboardElement = deckListElement.selectFirst("side-board")
        val sideboard = if (sideboardElement != null) {
            parseCards(sideboardElement.html())
        } else {
            emptyList()
        }

        return MtgoDecklistDetailDto(
            player = player,
            loginid = null,
            record = null,
            mainDeck = mainDeck,
            sideboardDeck = sideboard
        )
    }

    /**
     * 解析卡牌列表
     */
    private fun parseCards(html: String): List<MtgoCardDto> {
        val cards = mutableListOf<MtgoCardDto>()

        // 移除HTML标签并分割
        val text = html.replace("<[^>]*>".toRegex(), "")
        val lines = text.split("\n")

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty()) {
                val card = parseCardLine(trimmedLine)
                if (card != null) {
                    cards.add(card)
                }
            }
        }

        return cards
    }

    /**
     * 解析单行卡牌
     * 格式: "4 Lightning Bolt" 或 "1 Lightning Bolt"
     */
    private fun parseCardLine(line: String): MtgoCardDto? {
        try {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return null

            val parts = trimmed.split(Regex("\\s+"), limit = 2)
            if (parts.size >= 2) {
                val quantity = parts[0].toIntOrNull() ?: 1
                val cardName = parts[1].trim()

                return MtgoCardDto(
                    quantity = quantity,
                    cardAttributes = MtgoCardAttributesDto(
                        cardName = cardName,
                        manaCost = null, // Magic.gg 不提供法术力值
                        rarity = null,
                        color = null,
                        cardType = null,
                        cardSet = null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * 格式化日期
     */
    private fun formatDate(eventDate: String): String {
        // 输入格式: "January 11, 2026"
        // 输出格式: "2026-01-11"
        return try {
            val months = mapOf(
                "January" to "01",
                "February" to "02",
                "March" to "03",
                "April" to "04",
                "May" to "05",
                "June" to "06",
                "July" to "07",
                "August" to "08",
                "September" to "09",
                "October" to "10",
                "November" to "11",
                "December" to "12"
            )

            val parts = eventDate.replace(",", "").split(" ")
            if (parts.size >= 3) {
                val year = parts[2]
                val month = months[parts[0]] ?: "01"
                val day = parts[1].padStart(2, '0')
                "$year-$month-$day"
            } else {
                eventDate
            }
        } catch (e: Exception) {
            eventDate
        }
    }
}
