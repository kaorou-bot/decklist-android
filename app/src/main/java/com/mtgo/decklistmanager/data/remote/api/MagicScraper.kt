@file:Suppress("unused")
package com.mtgo.decklistmanager.data.remote.api

import com.mtgo.decklistmanager.data.remote.api.MagicConfig.BASE_URL
import com.mtgo.decklistmanager.data.remote.api.MagicConfig.SHOWCASE_URLS
import com.mtgo.decklistmanager.data.remote.api.MagicConfig.Selectors
import com.mtgo.decklistmanager.data.remote.api.MagicConfig.Network
import com.mtgo.decklistmanager.data.remote.api.MagicConfig.DateFormat
import com.mtgo.decklistmanager.util.AppLogger
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
        private const val TAG = "MagicScraper"
    }

    /**
     * 获取所有可用的牌组赛事
     */
    suspend fun fetchDecklistPage(year: Int = 2025, month: Int = 1): List<MtgoDecklistLinkDto> {
        return try {
            val links = mutableListOf<MtgoDecklistLinkDto>()

            for (url in SHOWCASE_URLS) {
                try {
                    val decklists = fetchShowcaseDecklists(url)
                    links.addAll(decklists)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error fetching showcase: ${e.message}")
                }
            }

            links
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error fetching decklist page: ${e.message}")
            emptyList()
        }
    }

    /**
     * 从特定的 Showcase 页面获取牌组列表
     */
    private suspend fun fetchShowcaseDecklists(url: String): List<MtgoDecklistLinkDto> {
        return try {
            val doc = Jsoup.connect(url)
                .timeout(Network.TIMEOUT_MS)
                .userAgent(Network.USER_AGENT)
                .get()

            val deckLists = doc.select(Selectors.DECK_LIST)

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
                                date = DateFormat.formatDate(eventDate),
                                eventType = "Champions Showcase"
                            )
                        )
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error parsing deck list: ${e.message}")
                }
            }

            links
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error fetching showcase decklists: ${e.message}")
            emptyList()
        }
    }

    /**
     * 获取并解析牌组详情页
     */
    suspend fun fetchDecklistDetail(url: String): MtgoDecklistDetailDto? {
        return try {
            val cleanUrl = url.substringBefore("#")

            val doc = Jsoup.connect(cleanUrl)
                .timeout(Network.TIMEOUT_MS)
                .userAgent(Network.USER_AGENT)
                .get()

            val playerName = url.substringAfter("#", "").replace("-", " ")

            val deckLists = doc.select(Selectors.DECK_LIST)

            for (deckList in deckLists) {
                val player = deckList.attr("deck-title")

                if (player.equals(playerName, ignoreCase = true) ||
                    playerName.isEmpty() ||
                    player.replace(" ", "-").equals(playerName.replace(" ", "-"), ignoreCase = true)) {
                    return parseSingleDecklist(deckList, player)
                }
            }

            if (deckLists.isNotEmpty()) {
                val firstDeck = deckLists.first() ?: return null
                val player = firstDeck.attr("deck-title")
                return parseSingleDecklist(firstDeck, player)
            }

            null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error fetching decklist detail: ${e.message}")
            null
        }
    }

    /**
     * 解析单个牌组
     */
    private fun parseSingleDecklist(deckListElement: org.jsoup.nodes.Element, player: String): MtgoDecklistDetailDto {
        val mainDeckElement = deckListElement.selectFirst(Selectors.MAIN_DECK)
        val mainDeck = if (mainDeckElement != null) {
            parseCards(mainDeckElement.html())
        } else {
            emptyList()
        }

        val sideboardElement = deckListElement.selectFirst(Selectors.SIDEBOARD)
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
        return try {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return null

            val parts = trimmed.split(Regex("\\s+"), limit = 2)
            if (parts.size >= 2) {
                val quantity = parts[0].toIntOrNull() ?: 1
                val cardName = parts[1].trim()

                MtgoCardDto(
                    quantity = quantity,
                    cardAttributes = MtgoCardAttributesDto(
                        cardName = cardName,
                        manaCost = null,
                        rarity = null,
                        color = null,
                        cardType = null,
                        cardSet = null
                    )
                )
            } else {
                null
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error parsing card line: ${e.message}")
            null
        }
    }
}
