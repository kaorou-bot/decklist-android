package com.mtgo.decklistmanager.data.repository

import com.mtgo.decklistmanager.data.local.dao.CardDao
import com.mtgo.decklistmanager.data.local.dao.CardInfoDao
import com.mtgo.decklistmanager.data.local.dao.DecklistDao
import com.mtgo.decklistmanager.data.local.dao.EventDao
import com.mtgo.decklistmanager.data.local.entity.CardEntity
import com.mtgo.decklistmanager.data.local.entity.CardInfoEntity
import com.mtgo.decklistmanager.data.local.entity.DecklistEntity
import com.mtgo.decklistmanager.data.local.entity.EventEntity
import com.mtgo.decklistmanager.data.remote.api.MagicScraper
import com.mtgo.decklistmanager.data.remote.api.MtgTop8Scraper
import com.mtgo.decklistmanager.data.remote.api.ScryfallApi
import com.mtgo.decklistmanager.data.remote.api.dto.ScryfallCardDto
import com.mtgo.decklistmanager.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decklist Repository - 牌组数据仓库
 * 负责协调数据源（本地数据库 + 网络API）
 */
@Singleton
class DecklistRepository @Inject constructor(
    private val decklistDao: DecklistDao,
    private val cardDao: CardDao,
    private val cardInfoDao: CardInfoDao,
    private val eventDao: EventDao,
    private val magicScraper: MagicScraper,
    private val mtgTop8Scraper: MtgTop8Scraper,
    private val scryfallApi: ScryfallApi
) {

    /**
     * 获取牌组列表（带筛选）
     */
    suspend fun getDecklists(
        format: String? = null,
        date: String? = null,
        limit: Int = 100
    ): List<Decklist> = withContext(Dispatchers.IO) {
        decklistDao.getDecklists(format, date, limit)
            .map { it.toDomainModel() }
    }

    /**
     * 根据 ID 获取牌组实体
     */
    suspend fun getDecklistById(decklistId: Long): DecklistEntity? = withContext(Dispatchers.IO) {
        decklistDao.getDecklistById(decklistId)
    }

    /**
     * 根据 ID 获取牌组的所有卡牌实体
     */
    suspend fun getCardsByDecklistId(decklistId: Long): List<CardEntity> = withContext(Dispatchers.IO) {
        cardDao.getCardsByDecklistId(decklistId)
    }

    /**
     * 获取牌组详情
     */
    suspend fun getDecklistDetail(decklistId: Long): Pair<Decklist, List<Card>>? =
        withContext(Dispatchers.IO) {
            val decklist = decklistDao.getDecklistById(decklistId)?.toDomainModel()
            val cards = cardDao.getCardsByDecklistId(decklistId).map { it.toDomainModel() }

            if (decklist != null) {
                Pair(decklist, cards)
            } else {
                null
            }
        }

    /**
     * 获取所有格式
     */
    suspend fun getAllFormats(): List<String> = withContext(Dispatchers.IO) {
        decklistDao.getAllFormats()
    }

    /**
     * 获取所有日期
     */
    suspend fun getAllDates(): List<String> = withContext(Dispatchers.IO) {
        decklistDao.getAllDates()
    }

    /**
     * 获取统计信息
     */
    suspend fun getStatistics(): Statistics = withContext(Dispatchers.IO) {
        Statistics(
            totalDecklists = decklistDao.getDecklistCount(),
            totalCards = cardDao.getCardCount(),
            totalFormats = decklistDao.getAllFormats().size,
            cachedCards = cardInfoDao.getCachedCardCount()
        )
    }

    /**
     * 根据卡牌名称搜索牌组
     */
    suspend fun searchDecklistsByCard(cardName: String): List<Decklist> =
        withContext(Dispatchers.IO) {
            val decklistIds = cardDao.searchDecklistsByCardName(cardName)
            decklistIds.mapNotNull { decklistId ->
                decklistDao.getDecklistById(decklistId)?.toDomainModel()
            }
        }

    /**
     * 清空所有数据
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        cardDao.clearAll()
        decklistDao.clearAll()
    }

    /**
     * 爬取 Magic.gg 牌组数据（并自动获取 Scryfall 详情）
     */
    suspend fun scrapeDecklists(
        formatFilter: String? = null,
        dateFilter: String? = null,
        eventFilter: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 从 Magic.gg 获取牌组
            val links = magicScraper.fetchDecklistPage()

            if (links.isEmpty()) {
                return@withContext Result.failure(Exception("无法从 Magic.gg 获取牌组数据"))
            }

            // 应用筛选
            var filteredLinks = links
            formatFilter?.let {
                filteredLinks = filteredLinks.filter { link ->
                    link.format.equals(it, ignoreCase = true)
                }
            }
            dateFilter?.let {
                filteredLinks = filteredLinks.filter { link ->
                    link.date == it
                }
            }
            eventFilter?.let {
                if (it != "All") {
                    filteredLinks = filteredLinks.filter { link ->
                        link.eventType.equals(it, ignoreCase = true)
                    }
                }
            }

            if (filteredLinks.isEmpty()) {
                return@withContext Result.failure(Exception("没有找到匹配的牌组"))
            }

            // 爬取所有匹配的牌组（不再限制数量）
            var successCount = 0
            for (link in filteredLinks) {
                try {
                    val detail = magicScraper.fetchDecklistDetail(link.url)
                    if (detail != null) {
                        val decklistId = saveDecklistData(link, detail)

                        // 自动从 Scryfall 获取卡牌详情（包括法术力值）
                        fetchScryfallDetails(decklistId)

                        successCount++
                    }
                    // 延迟避免请求过快
                    kotlinx.coroutines.delay(2000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (successCount == 0) {
                Result.failure(Exception("未能成功爬取任何牌组"))
            } else {
                Result.success(successCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 自动从 Scryfall 获取卡牌详情
     */
    private suspend fun fetchScryfallDetails(decklistId: Long) {
        try {
            // 获取该牌组的所有卡牌
            val cards = cardDao.getCardsByDecklistId(decklistId)

            // 对每张唯一的卡牌，从 Scryfall 获取详情
            val uniqueCardNames = cards.map { it.cardName }.distinct()

            for (cardName in uniqueCardNames) {
                try {
                    val response = scryfallApi.searchCardExact(cardName)
                    if (response.isSuccessful && response.body() != null) {
                        val scryfallCard = response.body()!!

                        // 更新所有同名卡牌的法术力值
                        cards.filter { it.cardName == cardName }.forEach { card ->
                            cardDao.updateDetails(
                                cardId = card.id,
                                manaCost = scryfallCard.manaCost,
                                color = scryfallCard.colors?.joinToString(","),
                                rarity = scryfallCard.rarity,
                                cardType = scryfallCard.typeLine,
                                cardSet = scryfallCard.setName
                            )
                        }

                        // 缓存到 CardInfo 表
                        val cardInfoEntity = scryfallCard.toEntity()
                        cardInfoDao.insertOrUpdate(cardInfoEntity)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 避免请求过快
                kotlinx.coroutines.delay(100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 保存牌组数据
     * @return decklistId (如果已存在则返回已存在的ID)
     */
    private suspend fun saveDecklistData(
        link: com.mtgo.decklistmanager.data.remote.api.dto.MtgoDecklistLinkDto,
        detail: com.mtgo.decklistmanager.data.remote.api.dto.MtgoDecklistDetailDto
    ): Long {
        // 检查是否已存在相同的牌组（根据 URL 或 玩家名+事件名）
        val existing = decklistDao.getDecklistByUrl(link.url)

        val decklistId = if (existing != null) {
            // 如果已存在，更新并删除旧卡牌
            decklistDao.update(
                DecklistEntity(
                    id = existing.id,
                    eventName = link.eventName,
                    eventType = link.eventType,
                    format = link.format,
                    date = link.date,
                    url = link.url,
                    playerName = detail.player,
                    playerId = detail.loginid,
                    record = detail.record,
                    eventId = null,
                    createdAt = existing.createdAt
                )
            )
            cardDao.deleteByDecklistId(existing.id)
            existing.id
        } else {
            // 新增
            val decklist = DecklistEntity(
                eventName = link.eventName,
                eventType = link.eventType,
                format = link.format,
                date = link.date,
                url = link.url,
                playerName = detail.player,
                playerId = detail.loginid,
                record = detail.record,
                eventId = null
            )
            decklistDao.insert(decklist)
        }

        // 保存主牌
        val mainDeckCards = detail.mainDeck.mapIndexed { index, card ->
            card.toEntity(decklistId, "main", index)
        }
        cardDao.insertAll(mainDeckCards)

        // 保存备牌
        val sideboardCards = detail.sideboardDeck.mapIndexed { index, card ->
            card.toEntity(decklistId, "sideboard", index)
        }
        cardDao.insertAll(sideboardCards)

        return decklistId
    }

    /**
     * 查询单卡信息（先查缓存，再查API）
     */
    suspend fun getCardInfo(cardName: String): CardInfo? = withContext(Dispatchers.IO) {
        // 先查缓存
        val cached = cardInfoDao.getCardInfoByName(cardName)
        if (cached != null) {
            return@withContext cached.toDomainModel()
        }

        // 查询 API
        try {
            val response = scryfallApi.searchCardExact(cardName)
            if (response.isSuccessful && response.body() != null) {
                val cardInfo = response.body()!!.toEntity()
                cardInfoDao.insertOrUpdate(cardInfo)
                return@withContext cardInfo.toDomainModel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }

    /**
     * 搜索卡牌（先查缓存，再查API）
     */
    suspend fun searchCardsByName(query: String, limit: Int = 20): List<CardInfo> =
        withContext(Dispatchers.IO) {
            // 先搜索缓存
            val cachedResults = cardInfoDao.searchCardsByName(query, limit)

            // 如果缓存结果不足，从API搜索
            if (cachedResults.size < limit) {
                try {
                    val response = scryfallApi.searchCards(query)
                    if (response.isSuccessful && response.body() != null) {
                        val apiCards = response.body()!!.data
                        apiCards?.forEach { cardDto ->
                            val entity = cardDto.toEntity()
                            cardInfoDao.insertOrUpdate(entity)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 重新查询缓存（包括新添加的）
                return@withContext cardInfoDao.searchCardsByName(query, limit)
                    .map { it.toDomainModel() }
            }

            cachedResults.map { it.toDomainModel() }
        }

    /**
     * 仅搜索缓存的卡牌
     */
    suspend fun searchCachedCards(query: String, limit: Int = 20): List<CardInfo> =
        withContext(Dispatchers.IO) {
            cardInfoDao.searchCardsByName(query, limit)
                .map { it.toDomainModel() }
        }

    /**
     * 获取赛事列表（带筛选）
     */
    suspend fun getEvents(
        format: String? = null,
        date: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<Event> = withContext(Dispatchers.IO) {
        eventDao.getEvents(format, date, limit, offset)
            .map { it.toDomainModel() }
    }

    /**
     * 根据 ID 获取赛事
     */
    suspend fun getEventById(eventId: Long): Event? = withContext(Dispatchers.IO) {
        eventDao.getEventById(eventId)?.toDomainModel()
    }

    /**
     * 获取赛事下的所有卡组
     */
    suspend fun getDecklistsByEventId(eventId: Long): List<Decklist> = withContext(Dispatchers.IO) {
        decklistDao.getDecklistsByEventId(eventId)
            .map { it.toDomainModel() }
    }

    /**
     * 从 MTGTop8 爬取赛事列表（三级结构：Event → Decklist → Card）
     * 注意：MTGTop8 的每个事件包含多个卡组，通过 d 参数枚举
     */
    suspend fun scrapeEventsFromMtgTop8(
        format: String,
        date: String? = null,
        maxEvents: Int = 10
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 1. 获取事件列表
            val events = mtgTop8Scraper.fetchEventList(format, date, maxEvents)

            if (events.isEmpty()) {
                return@withContext Result.failure(Exception("Unable to fetch events from MTGTop8"))
            }

            var totalDecklistsSaved = 0

            // 2. 对每个事件，枚举其包含的所有卡组
            for (eventDto in events) {
                try {
                    // 保存或更新赛事
                    val eventId = saveEventData(eventDto)

                    // 获取该事件下的所有卡组（通过枚举 d 参数）
                    val eventDecklists = mtgTop8Scraper.fetchEventDecklists(eventDto.eventUrl)

                    if (eventDecklists != null && eventDecklists.decklists.isNotEmpty()) {
                        // 遍历并保存每个卡组
                        for (decklistDto in eventDecklists.decklists) {
                            try {
                                // 获取卡组详情
                                val decklistDetail = mtgTop8Scraper.fetchDecklistDetail(decklistDto.url)

                                if (decklistDetail != null) {
                                    // 保存卡组数据并关联到事件
                                    val decklistId = saveMtgTop8DecklistDataWithEvent(
                                        decklistDto = decklistDto,
                                        detail = decklistDetail,
                                        format = format,
                                        eventId = eventId
                                    )

                                    // 自动从 Scryfall 获取卡牌详情
                                    fetchScryfallDetails(decklistId)

                                    totalDecklistsSaved++
                                }

                                // 延迟避免请求过快
                                kotlinx.coroutines.delay(1000)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // 更新事件的卡组数量
                        eventDao.updateDeckCount(eventId, eventDecklists.decklists.size)
                    }

                    // 事件之间的延迟
                    kotlinx.coroutines.delay(500)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (totalDecklistsSaved == 0) {
                Result.failure(Exception("Failed to scrape any decklists"))
            } else {
                Result.success(totalDecklistsSaved)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存赛事数据
     */
    private suspend fun saveEventData(eventDto: com.mtgo.decklistmanager.data.remote.api.dto.MtgTop8EventDto): Long {
        // 检查是否已存在相同赛事（根据 source_url）
        val existing = eventDao.getEventByUrl(eventDto.eventUrl)

        return if (existing != null) {
            // 更新
            eventDao.update(
                EventEntity(
                    id = existing.id,
                    eventName = eventDto.eventName,
                    eventType = eventDto.eventType,
                    format = eventDto.format,
                    date = eventDto.eventDate,
                    sourceUrl = eventDto.sourceUrl,
                    source = eventDto.source ?: "MTGTop8",
                    deckCount = eventDto.deckCount,
                    createdAt = existing.createdAt
                )
            )
            existing.id
        } else {
            // 新增
            val event = EventEntity(
                eventName = eventDto.eventName,
                eventType = eventDto.eventType,
                format = eventDto.format,
                date = eventDto.eventDate,
                sourceUrl = eventDto.sourceUrl,
                source = eventDto.source ?: "MTGTop8",
                deckCount = eventDto.deckCount
            )
            eventDao.insert(event)
        }
    }

    /**
     * 保存 MTGTop8 牌组数据（带赛事关联）
     */
    private suspend fun saveMtgTop8DecklistDataWithEvent(
        decklistDto: com.mtgo.decklistmanager.data.remote.api.dto.MtgTop8DecklistDto,
        detail: com.mtgo.decklistmanager.data.remote.api.dto.MtgTop8DecklistDetailDto,
        format: String,
        eventId: Long
    ): Long {
        // 检查是否已存在相同的牌组（根据 URL）
        val existing = decklistDao.getDecklistByUrl(decklistDto.url)

        val decklistId = if (existing != null) {
            // 如果已存在，更新并删除旧卡牌
            decklistDao.update(
                DecklistEntity(
                    id = existing.id,
                    eventName = decklistDto.eventName,
                    eventType = "MTGTop8",
                    format = format,
                    date = decklistDto.eventDate,
                    url = decklistDto.url,
                    playerName = decklistDto.playerName,
                    playerId = null,
                    record = null,
                    eventId = eventId,
                    createdAt = existing.createdAt
                )
            )
            cardDao.deleteByDecklistId(existing.id)
            existing.id
        } else {
            // 新增
            val decklist = DecklistEntity(
                eventName = decklistDto.eventName,
                eventType = "MTGTop8",
                format = format,
                date = decklistDto.eventDate,
                url = decklistDto.url,
                playerName = decklistDto.playerName,
                playerId = null,
                record = null,
                eventId = eventId
            )
            decklistDao.insert(decklist)
        }

        // 保存主牌
        val mainDeckCards = detail.mainDeck.mapIndexed { index, card ->
            CardEntity(
                decklistId = decklistId,
                cardName = card.name,
                quantity = card.quantity,
                location = "main",
                cardOrder = index,
                manaCost = null,
                rarity = null,
                color = null,
                cardType = null,
                cardSet = null
            )
        }
        cardDao.insertAll(mainDeckCards)

        // 保存备牌
        val sideboardCards = detail.sideboardDeck.mapIndexed { index, card ->
            CardEntity(
                decklistId = decklistId,
                cardName = card.name,
                quantity = card.quantity,
                location = "sideboard",
                cardOrder = index,
                manaCost = null,
                rarity = null,
                color = null,
                cardType = null,
                cardSet = null
            )
        }
        cardDao.insertAll(sideboardCards)

        return decklistId
    }

    // Extension functions for mapping
    fun DecklistEntity.toDomainModel() = Decklist(
        id = id,
        eventName = eventName,
        eventType = eventType,
        format = format,
        date = date,
        url = url,
        playerName = playerName,
        playerId = playerId,
        record = record,
        createdAt = createdAt
    )

    fun EventEntity.toDomainModel() = Event(
        id = id,
        eventName = eventName,
        eventType = eventType,
        format = format,
        date = date,
        sourceUrl = sourceUrl,
        source = source,
        deckCount = deckCount,
        createdAt = createdAt
    )

    fun CardEntity.toDomainModel() = Card(
        id = id,
        decklistId = decklistId,
        cardName = cardName,
        quantity = quantity,
        location = if (location == "main") CardLocation.MAIN else CardLocation.SIDEBOARD,
        cardOrder = cardOrder,
        manaCost = manaCost,
        rarity = rarity,
        color = color,
        cardType = cardType,
        cardSet = cardSet
    )

    private fun com.mtgo.decklistmanager.data.remote.api.dto.MtgoCardDto.toEntity(
        decklistId: Long,
        location: String,
        order: Int
    ) = CardEntity(
        decklistId = decklistId,
        cardName = cardAttributes.cardName,
        quantity = quantity,
        location = location,
        cardOrder = order,
        manaCost = cardAttributes.manaCost,
        rarity = cardAttributes.rarity,
        color = cardAttributes.color,
        cardType = cardAttributes.cardType,
        cardSet = cardAttributes.cardSet
    )

    private fun ScryfallCardDto.toEntity() = CardInfoEntity(
        id = id,
        name = name,
        manaCost = manaCost,
        cmc = cmc,
        typeLine = typeLine,
        oracleText = oracleText,
        colors = colors?.joinToString(","),
        colorIdentity = colorIdentity?.joinToString(","),
        power = power,
        toughness = toughness,
        loyalty = loyalty,
        rarity = rarity,
        setCode = setCode,
        setName = setName,
        artist = artist,
        cardNumber = cardNumber,
        legalStandard = legalities?.get("standard"),
        legalModern = legalities?.get("modern"),
        legalPioneer = legalities?.get("pioneer"),
        legalLegacy = legalities?.get("legacy"),
        legalVintage = legalities?.get("vintage"),
        legalCommander = legalities?.get("commander"),
        legalPauper = legalities?.get("pauper"),
        priceUsd = prices?.usd,
        scryfallUri = scryfallUri,
        imagePath = null, // Will be set when image is downloaded
        imageUriSmall = imageUris?.small,
        imageUriNormal = imageUris?.normal,
        imageUriLarge = imageUris?.large
    )

    private fun CardInfoEntity.toDomainModel() = CardInfo(
        id = id,
        name = name,
        manaCost = manaCost,
        cmc = cmc,
        typeLine = typeLine,
        oracleText = oracleText,
        colors = colors?.split(",")?.map { it.trim() },
        colorIdentity = colorIdentity?.split(",")?.map { it.trim() },
        power = power,
        toughness = toughness,
        loyalty = loyalty,
        rarity = rarity,
        setCode = setCode,
        setName = setName,
        artist = artist,
        cardNumber = cardNumber,
        legalStandard = legalStandard,
        legalModern = legalModern,
        legalPioneer = legalPioneer,
        legalLegacy = legalLegacy,
        legalVintage = legalVintage,
        legalCommander = legalCommander,
        legalPauper = legalPauper,
        priceUsd = priceUsd,
        scryfallUri = scryfallUri,
        imagePath = imagePath,
        imageUriSmall = imageUriSmall,
        imageUriNormal = imageUriNormal,
        imageUriLarge = imageUriLarge,
        lastUpdated = lastUpdated
    )

    /**
     * 从 MTGTop8 爬取牌组数据
     */
    suspend fun scrapeFromMtgTop8(format: String, date: String?, maxDecks: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 从 MTGTop8 获取牌组列表
            val decklistLinks = mtgTop8Scraper.fetchDecklistPage(format, date, maxDecks)

            if (decklistLinks.isEmpty()) {
                return@withContext Result.failure(Exception("Unable to fetch decklists from MTGTop8"))
            }

            var successCount = 0
            for (link in decklistLinks) {
                try {
                    // 获取牌组详情
                    val detail = mtgTop8Scraper.fetchDecklistDetail(link.url)
                    if (detail != null) {
                        val decklistId = saveMtgTop8DecklistData(link, detail, format)

                        // 自动从 Scryfall 获取卡牌详情
                        fetchScryfallDetails(decklistId)

                        successCount++
                    }
                    // 延迟避免请求过快
                    kotlinx.coroutines.delay(1500)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (successCount == 0) {
                Result.failure(Exception("Failed to scrape any decklists"))
            } else {
                Result.success(successCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存 MTGTop8 牌组数据
     */
    private suspend fun saveMtgTop8DecklistData(
        link: com.mtgo.decklistmanager.data.remote.api.dto.MtgTop8DecklistDto,
        detail: com.mtgo.decklistmanager.data.remote.api.dto.MtgTop8DecklistDetailDto,
        format: String
    ): Long {
        // 检查是否已存在相同的牌组（根据 URL）
        val existing = decklistDao.getDecklistByUrl(link.url)

        val decklistId = if (existing != null) {
            // 如果已存在，更新并删除旧卡牌
            decklistDao.update(
                DecklistEntity(
                    id = existing.id,
                    eventName = link.eventName,
                    eventType = "MTGTop8",
                    format = format,
                    date = link.eventDate,
                    url = link.url,
                    playerName = link.playerName,
                    playerId = null,
                    record = null,
                    eventId = null
                )
            )
            cardDao.deleteByDecklistId(existing.id)
            existing.id
        } else {
            // 新增
            val decklist = DecklistEntity(
                eventName = link.eventName,
                eventType = "MTGTop8",
                format = format,
                date = link.eventDate,
                url = link.url,
                playerName = link.playerName,
                playerId = null,
                record = null,
                eventId = null
            )
            decklistDao.insert(decklist)
        }

        // 保存主牌
        val mainDeckCards = detail.mainDeck.mapIndexed { index, card ->
            CardEntity(
                decklistId = decklistId,
                cardName = card.name,
                quantity = card.quantity,
                location = "main",
                cardOrder = index,
                manaCost = null,
                rarity = null,
                color = null,
                cardType = null,
                cardSet = null
            )
        }
        cardDao.insertAll(mainDeckCards)

        // 保存备牌
        val sideboardCards = detail.sideboardDeck.mapIndexed { index, card ->
            CardEntity(
                decklistId = decklistId,
                cardName = card.name,
                quantity = card.quantity,
                location = "sideboard",
                cardOrder = index,
                manaCost = null,
                rarity = null,
                color = null,
                cardType = null,
                cardSet = null
            )
        }
        cardDao.insertAll(sideboardCards)

        return decklistId
    }
}
