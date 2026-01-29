@file:Suppress("unused")
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
 * MTGO Scraper - MTGO 网站爬虫
 * 使用 Jsoup 解析 HTML
 */
@Singleton
class MtgoScraper @Inject constructor() {

    companion object {
        private const val BASE_URL = "https://www.mtgo.com"
        private const val DECKLISTS_PATH = "/decklists"
    }

    /**
     * 获取牌组列表页面
     */
    suspend fun fetchDecklistPage(year: Int = 2026, month: Int = 1): List<MtgoDecklistLinkDto> {
        return try {
            val url = "$BASE_URL$DECKLISTS_PATH"
            val doc = Jsoup.connect(url)
                .timeout(30000)
                .get()

            parseDecklistLinks(doc)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 解析牌组链接
     */
    private fun parseDecklistLinks(doc: Document): List<MtgoDecklistLinkDto> {
        val links = mutableListOf<MtgoDecklistLinkDto>()

        // 查找所有牌组链接
        val elements = doc.select("a[href*=/decklist/]")

        for (element in elements) {
            val href = element.attr("href")
            val text = element.text()

            if (href.isNotEmpty() && text.isNotEmpty()) {
                val fullUrl = if (href.startsWith("http")) {
                    href
                } else {
                    "$BASE_URL$href"
                }

                // 提取日期
                val date = extractDateFromUrl(href)

                // 提取格式
                val format = extractFormat(text)

                // 提取事件类型
                val eventType = extractEventType(text)

                links.add(
                    MtgoDecklistLinkDto(
                        url = fullUrl,
                        eventName = text,
                        format = format,
                        date = date,
                        eventType = eventType
                    )
                )
            }
        }

        return links
    }

    /**
     * 获取并解析牌组详情页
     */
    suspend fun fetchDecklistDetail(url: String): MtgoDecklistDetailDto? {
        return try {
            val doc = Jsoup.connect(url)
                .timeout(30000)
                .get()

            // 查找 JSON 数据
            val scriptElements = doc.select("script")
            for (script in scriptElements) {
                val html = script.html()
                if (html.contains("window.MTGO.decklists.data")) {
                    val jsonStart = html.indexOf("{")
                    val jsonEnd = html.lastIndexOf("}") + 1
                    val jsonString = html.substring(jsonStart, jsonEnd)

                    // 这里需要使用 Gson 解析 JSON
                    // 由于复杂性，这里简化处理
                    return parseDecklistJson(jsonString)
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 解析牌组 JSON 数据
     */
    private fun parseDecklistJson(jsonString: String): MtgoDecklistDetailDto? {
        return try {
            // 使用 Gson 解析 JSON
            val gson = com.google.gson.Gson()
            val jsonObject = com.google.gson.JsonParser().parse(jsonString).asJsonObject

            val decklistsArray = jsonObject.getAsJsonArray("decklists")
            if (decklistsArray != null && decklistsArray.size() > 0) {
                val firstDecklist = decklistsArray.get(0).asJsonObject

                val player = firstDecklist.get("player")?.asString
                val loginid = firstDecklist.get("loginid")?.asString
                val record = firstDecklist.get("record")?.asString

                val mainDeck = parseCardsArray(firstDecklist.getAsJsonArray("main_deck"))
                val sideboardDeck = parseCardsArray(firstDecklist.getAsJsonArray("sideboard_deck"))

                MtgoDecklistDetailDto(
                    player = player,
                    loginid = loginid,
                    record = record,
                    mainDeck = mainDeck,
                    sideboardDeck = sideboardDeck
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 解析卡牌数组
     */
    private fun parseCardsArray(cardsArray: com.google.gson.JsonArray?): List<MtgoCardDto> {
        if (cardsArray == null) return emptyList()

        val cards = mutableListOf<MtgoCardDto>()

        // 使用 LinkedHashMap 来合并同名卡牌，保持顺序
        data class CardEntry(
            var quantity: Int,
            val attributes: MtgoCardAttributesDto
        )

        val cardMap = linkedMapOf<String, CardEntry>()

        for (i in 0 until cardsArray.size()) {
            try {
                val cardObj = cardsArray.get(i).asJsonObject
                val qty = cardObj.get("qty")?.asInt ?: 0
                val cardAttrs = cardObj.getAsJsonObject("card_attributes")

                val cardName = cardAttrs.get("card_name")?.asString ?: continue

                val attrs = MtgoCardAttributesDto(
                    cardName = cardName,
                    manaCost = cardAttrs.get("cost")?.asString,
                    rarity = cardAttrs.get("rarity")?.asString,
                    color = cardAttrs.get("color")?.asString,
                    cardType = cardAttrs.get("card_type")?.asString,
                    cardSet = cardAttrs.get("cardset")?.asString
                )

                // 合并同名卡牌
                if (cardMap.containsKey(cardName)) {
                    // 累加数量
                    cardMap[cardName]!!.quantity += qty
                } else {
                    cardMap[cardName] = CardEntry(qty, attrs)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 转换为 MtgoCardDto 列表，保持顺序
        cardMap.forEach { (_, entry) ->
            cards.add(MtgoCardDto(quantity = entry.quantity, cardAttributes = entry.attributes))
        }

        return cards
    }

    /**
     * 从 URL 提取日期
     */
    private fun extractDateFromUrl(url: String): String {
        val datePattern = Regex("""(\d{4})-(\d{2})-(\d{2})""")
        val match = datePattern.find(url)
        return match?.value ?: ""
    }

    /**
     * 提取格式
     */
    private fun extractFormat(text: String): String {
        val formats = listOf(
            "Standard", "Modern", "Pioneer", "Legacy", "Vintage",
            "Pauper", "Commander", "Limited", "Sealed", "Draft"
        )

        for (format in formats) {
            if (text.contains(format, ignoreCase = true)) {
                return format
            }
        }

        return "Other"
    }

    /**
     * 提取事件类型
     */
    private fun extractEventType(text: String): String {
        val types = listOf(
            "League", "Challenge", "Showcase", "Preliminary", "Qualifier"
        )

        for (type in types) {
            if (text.contains(type, ignoreCase = true)) {
                return type
            }
        }

        return "Other"
    }
}
