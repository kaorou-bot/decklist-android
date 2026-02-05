@file:Suppress("unused")
package com.mtgo.decklistmanager.data.repository

import android.util.Log
import com.mtgo.decklistmanager.data.local.dao.CardDao
import com.mtgo.decklistmanager.data.local.dao.CardInfoDao
import com.mtgo.decklistmanager.data.local.dao.DecklistDao
import com.mtgo.decklistmanager.data.local.dao.EventDao
import com.mtgo.decklistmanager.data.local.dao.FavoriteDecklistDao
import com.mtgo.decklistmanager.data.local.entity.CardEntity
import com.mtgo.decklistmanager.data.local.entity.CardInfoEntity
import com.mtgo.decklistmanager.data.local.entity.DecklistEntity
import com.mtgo.decklistmanager.data.local.entity.EventEntity
import com.mtgo.decklistmanager.data.remote.api.MagicScraper
import com.mtgo.decklistmanager.data.remote.api.MtgTop8Scraper
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchApi
import com.mtgo.decklistmanager.data.remote.api.mtgch.toEntity
import com.mtgo.decklistmanager.data.remote.api.dto.ScryfallCardDto
import com.mtgo.decklistmanager.domain.model.*
import com.mtgo.decklistmanager.util.AppLogger
import com.mtgo.decklistmanager.util.LanguagePreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
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
    private val favoriteDecklistDao: FavoriteDecklistDao,
    private val magicScraper: MagicScraper,
    private val mtgTop8Scraper: MtgTop8Scraper,
    private val mtgchApi: MtgchApi,
    private val languagePreferenceManager: LanguagePreferenceManager
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
     * 确保卡牌详情已获取（供UI调用）
     * v4.0.0: 在加载套牌详情时调用，确保显示中文和法术力值
     */
    suspend fun ensureCardDetails(decklistId: Long) {
        fetchScryfallDetails(decklistId)
    }

    /**
     * 自动从 MTGCH 获取卡牌详情（并发优化版本 + 智能缓存）
     * v4.1.0: 先检查数据库缓存，只获取缺失的卡牌详情
     */
    private suspend fun fetchScryfallDetails(decklistId: Long) = coroutineScope {
        try {
            // 获取该牌组的所有卡牌
            val cards = cardDao.getCardsByDecklistId(decklistId)

            // 对每张唯一的卡牌，从 MTGCH 获取详情
            val uniqueCardNames = cards.map { it.cardName }.distinct()

            AppLogger.d("DecklistRepository", "fetchScryfallDetails: Processing ${uniqueCardNames.size} unique cards")

            // 检查哪些卡牌在数据库中已经有详情
            val cardsNeedingFetch = uniqueCardNames.filter { cardName ->
                val cached = cardInfoDao.getCardInfoByNameOrEnName(normalizeCardName(cardName))
                cached == null
            }

            AppLogger.d("DecklistRepository", "Cache check: ${uniqueCardNames.size - cardsNeedingFetch.size} cached, ${cardsNeedingFetch.size} need fetch")

            // 如果所有卡牌都已缓存，直接返回
            if (cardsNeedingFetch.isEmpty()) {
                AppLogger.d("DecklistRepository", "All cards already cached, skipping API fetch")
                return@coroutineScope
            }

            AppLogger.d("DecklistRepository", "Fetching ${cardsNeedingFetch.size} uncached cards from mtgch.com")

            // 使用 Semaphore 控制并发数量，避免过多请求
            // 减少并发数以避免 API 429 错误
            val semaphore = Semaphore(2)

            // 并发获取未缓存的卡牌详情
            cardsNeedingFetch.mapIndexed { index, cardName ->
                async {
                    semaphore.acquire()
                    try {
                        // 添加延迟以避免 API 频率限制
                        if (index > 0 && index % 2 == 0) {
                            delay(500)
                        }

                        // 标准化卡名（处理连体牌格式）
                        val formattedCardName = normalizeCardName(cardName)

                        val response = mtgchApi.searchCard(
                            query = formattedCardName,
                            pageSize = 10,
                            priorityChinese = true
                        )
                        if (response.isSuccessful && response.body() != null) {
                            val searchResponse = response.body()!!
                            val results = searchResponse.data
                            if (results != null && results.isNotEmpty()) {
                                // 精确匹配
                                val exactMatch = results.find { card ->
                                    val nameMatch = card.name?.equals(formattedCardName, ignoreCase = true) == true
                                    val zhNameMatch = card.zhsName?.equals(cardName, ignoreCase = true) == true
                                    val translatedNameMatch = card.atomicTranslatedName?.equals(cardName, ignoreCase = true) == true

                                    // 双面牌特殊处理
                                    val dualFaceMatch = card.name?.contains("//") == true &&
                                        (card.name.startsWith("$formattedCardName //", ignoreCase = true) ||
                                         card.name.endsWith("// $formattedCardName", ignoreCase = true) ||
                                         card.name.contains(" // $formattedCardName //", ignoreCase = true))

                                    // 连体牌特殊处理：检查原始卡名
                                    val splitMatch = !cardName.contains("/") && card.name?.contains("//") == true &&
                                        card.name.equals(formattedCardName, ignoreCase = true)

                                    nameMatch || zhNameMatch || translatedNameMatch || dualFaceMatch || splitMatch
                                }

                                if (exactMatch != null) {
                                    val mtgchCard = exactMatch

                                    // 更新所有同名卡牌的法术力值和中文名
                                    var displayName = mtgchCard.zhsName
                                        ?: mtgchCard.atomicTranslatedName
                                        ?: mtgchCard.name

                                    displayName = getBasicLandChineseName(displayName) ?: displayName

                                    AppLogger.d("DecklistRepository", "  Found: $cardName -> $displayName (mana: ${mtgchCard.manaCost})")

                                    // 先更新所有同名卡牌的 display_name（确保其他套牌也能看到中文名）
                                    // displayName 在这里保证不为 null（因为最后有 ?: mtgchCard.name）
                                    cardDao.updateDisplayNameByName(
                                        cardName = cardName,
                                        displayName = displayName!!
                                    )

                                    // 然后更新当前套牌中卡牌的其他详细信息
                                    cards.filter { it.cardName == cardName }.forEach { card ->
                                        cardDao.updateDetails(
                                            cardId = card.id,
                                            manaCost = mtgchCard.manaCost,
                                            color = mtgchCard.colors?.joinToString(","),
                                            rarity = mtgchCard.rarity,
                                            cardType = mtgchCard.zhsTypeLine ?: mtgchCard.atomicTranslatedType ?: mtgchCard.typeLine,
                                            cardSet = mtgchCard.setName,
                                            displayName = displayName
                                        )
                                    }

                                    // 缓存到 CardInfo 表
                                    val cardInfoEntity = mtgchCard.toEntity()
                                    val finalName = getBasicLandChineseName(cardInfoEntity.name)
                                    if (finalName != null) {
                                        cardInfoEntity.copy(name = finalName)
                                    } else {
                                        cardInfoEntity
                                    }.let { cardInfoDao.insertOrUpdate(it) }
                                } else {
                                    AppLogger.w("DecklistRepository", "  No exact match found for: $cardName (formatted: $formattedCardName)")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        AppLogger.e("DecklistRepository", "Error fetching card '$cardName': ${e.message}")
                    } finally {
                        semaphore.release()
                    }
                }
            }.awaitAll()

        } catch (e: Exception) {
            AppLogger.e("DecklistRepository", "Error in fetchScryfallDetails: ${e.message}")
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
                    deckName = null,  // MTGO 链接没有 deckName
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
                deckName = null,  // MTGO 链接没有 deckName
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
     * 获取卡牌信息 - 优先使用本地缓存，缓存未命中时调用 API
     * v4.1.0: 优化性能，减少 API 调用
     */
    suspend fun getCardInfo(cardName: String): CardInfo? = withContext(Dispatchers.IO) {
        AppLogger.d("DecklistRepository", "getCardInfo called for: $cardName")

        // 转换卡名格式（处理连体牌）
        val formattedCardName = normalizeCardName(cardName)

        // 1. 首先查询本地缓存（按名称或英文名称）
        var cachedInfo = cardInfoDao.getCardInfoByNameOrEnName(formattedCardName)

        // 2. 如果未找到，尝试前缀匹配（针对双面牌：用正面名查询完整名）
        if (cachedInfo == null) {
            AppLogger.d("DecklistRepository", "First query failed, trying prefix match for: $formattedCardName")
            cachedInfo = cardInfoDao.getCardInfoByEnNamePrefix(formattedCardName)
            AppLogger.d("DecklistRepository", "Prefix query result: ${cachedInfo != null}")
        }

        if (cachedInfo != null) {
            // v4.1.0: 检查双面牌数据完整性
            val isDualFaced = cachedInfo.isDualFaced

            if (!isDualFaced) {
                // 非双面牌，直接使用缓存
                AppLogger.d("DecklistRepository", "✓ Cache hit for: $cardName (not dual-faced)")
                return@withContext cachedInfo.toDomainModel()
            }

            // 双面牌需要检查数据完整性
            val backImageMissing = cachedInfo.backImageUri == null
            val backTypeLine = cachedInfo.backFaceTypeLine ?: ""
            val backIsCreature = backTypeLine.contains("Creature", ignoreCase = true)
            val backIsPlaneswalker = backTypeLine.contains("Planeswalker", ignoreCase = true)

            val backPowerMissing = backIsCreature && cachedInfo.backFacePower == null
            val backLoyaltyMissing = backIsPlaneswalker && cachedInfo.backFaceLoyalty == null

            // 检查：如果是双面牌，但没有任何背面数据，认为是旧格式
            val hasAnyBackData = cachedInfo.backFaceManaCost != null ||
                                 cachedInfo.backFaceTypeLine != null ||
                                 cachedInfo.backFaceOracleText != null ||
                                 cachedInfo.backFacePower != null ||
                                 cachedInfo.backFaceToughness != null ||
                                 cachedInfo.backFaceLoyalty != null

            AppLogger.d("DecklistRepository", "Cache check for $cardName:")
            AppLogger.d("DecklistRepository", "  hasAnyBackData: $hasAnyBackData")
            AppLogger.d("DecklistRepository", "  backTypeLine: $backTypeLine")
            AppLogger.d("DecklistRepository", "  backIsCreature: $backIsCreature, backIsPlaneswalker: $backIsPlaneswalker")
            AppLogger.d("DecklistRepository", "  backPower: ${cachedInfo.backFacePower}, backLoyalty: ${cachedInfo.backFaceLoyalty}")
            AppLogger.d("DecklistRepository", "  backPowerMissing: $backPowerMissing, backLoyaltyMissing: $backLoyaltyMissing")

            val needsRefresh = !hasAnyBackData || backImageMissing || backPowerMissing || backLoyaltyMissing

            if (needsRefresh) {
                AppLogger.d("DecklistRepository", "⚠ Dual-faced card needs refresh")
            } else {
                AppLogger.d("DecklistRepository", "✓ Cache hit for: $cardName")
                return@withContext cachedInfo.toDomainModel()
            }
        }

        AppLogger.d("DecklistRepository", "✗ Cache miss for: $cardName, fetching from API")

        // 2. 缓存未命中或数据不完整，调用 API 获取
        val apiResult = fetchCardInfoFromApi(formattedCardName)

        // 3. 如果 API 成功返回，存入本地缓存
        if (apiResult != null) {
            try {
                val entity = apiResult.toEntity()
                cardInfoDao.insertOrUpdate(entity)
                AppLogger.d("DecklistRepository", "✓ Cached card info for: $cardName")
            } catch (e: Exception) {
                AppLogger.e("DecklistRepository", "Failed to cache card info: ${e.message}")
            }
        }

        return@withContext apiResult
    }

    /**
     * 获取完整的 MTGCH 卡牌数据（包含 other_faces）
     * 用于直接构建 CardInfo，避免数据库缓存的旧数据问题
     */
    suspend fun getMtgchCard(cardName: String): com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto? = withContext(Dispatchers.IO) {
        AppLogger.d("DecklistRepository", "getMtgchCard called for: $cardName")

        // 转换卡名格式
        val formattedCardName = normalizeCardName(cardName)

        // 直接调用 API 获取最新数据
        val response = mtgchApi.searchCard(
            query = formattedCardName,
            pageSize = 20,
            priorityChinese = true
        )

        if (response.isSuccessful && response.body() != null) {
            val searchResponse = response.body()!!
            val results = searchResponse.data

            if (results != null && results.isNotEmpty()) {
                // 精确匹配
                val exactMatch = results.find { card ->
                    val nameMatch = card.name?.equals(formattedCardName, ignoreCase = true) == true
                    val zhNameMatch = card.zhsName?.equals(formattedCardName, ignoreCase = true) == true

                    // 双面牌特殊处理
                    val dualFaceMatch = card.name?.contains("//") == true &&
                        (card.name.startsWith("$formattedCardName //", ignoreCase = true) ||
                         card.name.endsWith("// $formattedCardName", ignoreCase = true))

                    nameMatch || zhNameMatch || dualFaceMatch
                }

                if (exactMatch != null) {
                    AppLogger.d("DecklistRepository", "✓ Found MTGCH card: $cardName")
                    return@withContext exactMatch
                }
            }
        }

        AppLogger.w("DecklistRepository", "✗ MTGCH card not found: $cardName")
        return@withContext null
    }

    /**
     * 更新卡牌信息缓存
     */
    suspend fun updateCardInfo(entity: CardInfoEntity) {
        cardInfoDao.insertOrUpdate(entity)
    }

    /**
     * 标准化卡牌名称
     * v4.1.0: 处理连体牌格式转换
     * - "Wear/Tear" -> "Wear // Tear"
     * - "Fire/Ice" -> "Fire // Ice"
     */
    private fun normalizeCardName(cardName: String): String {
        // 处理连体牌格式：将 "/" 转换为 " // "
        // 例如：Wear/Tear -> Wear // Tear
        if (cardName.contains("/") && !cardName.contains("//")) {
            return cardName.replace("/", " // ")
        }
        return cardName
    }

    private suspend fun fetchCardInfoFromApi(cardName: String): CardInfo? {
        // v4.0.0 在线模式：直接调用 MTGCH API
        // 注意：不使用 ! 前缀，因为 API 的精确搜索不可靠
        // 改为客户端精确匹配验证

        val response = mtgchApi.searchCard(
            query = cardName,
            pageSize = 20,  // 获取更多结果以便精确匹配
            priorityChinese = true
        )

        if (response.isSuccessful && response.body() != null) {
            val searchResponse = response.body()!!
            val results = searchResponse.data

            if (results != null && results.isNotEmpty()) {
                // v4.0.0: 严格精确匹配（忽略大小写），支持双面牌
                val exactMatch = results.find { card ->
                    val nameMatch = card.name?.equals(cardName, ignoreCase = true) == true
                    val zhNameMatch = card.zhsName?.equals(cardName, ignoreCase = true) == true
                    val translatedNameMatch = card.atomicTranslatedName?.equals(cardName, ignoreCase = true) == true

                    // 双面牌特殊处理：检查 name 是否包含卡名
                    val dualFaceMatch = card.name?.contains("//") == true &&
                        (card.name.startsWith("$cardName //", ignoreCase = true) ||
                         card.name.endsWith("// $cardName", ignoreCase = true) ||
                         card.name.contains(" // $cardName //", ignoreCase = true))

                    nameMatch || zhNameMatch || translatedNameMatch || dualFaceMatch
                }

                if (exactMatch != null) {
                    val cardInfoEntity = exactMatch.toEntity()

                    // v4.0.0: 基本地中文名映射
                    val finalDisplayName = getBasicLandChineseName(cardInfoEntity.name) ?: cardInfoEntity.name
                    if (finalDisplayName != cardInfoEntity.name) {
                        // 更新卡牌名称为中文基本地名称
                        return cardInfoEntity.copy(name = finalDisplayName).toDomainModel()
                    }

                    AppLogger.d("DecklistRepository", "✓ Found exact match: $cardName -> ${cardInfoEntity.name}")
                    return cardInfoEntity.toDomainModel()
                } else {
                    // 没有找到精确匹配，记录警告并返回 null
                    AppLogger.w("DecklistRepository", "✗ No exact match found for: $cardName")
                    AppLogger.w("DecklistRepository", "  Candidates: ${results.take(3).map { it.name }.joinToString(", ")}")
                    return null
                }
            }
        }

        // API 调用失败或没有结果
        AppLogger.w("DecklistRepository", "✗ API returned no results for: $cardName")
        return null
    }

    /**
     * v4.0.0: 获取常用卡牌的中文名称
     * 包括基本地和热门非基本地
     */
    private fun getBasicLandChineseName(englishName: String?): String? {
        if (englishName == null) return null

        return when (englishName) {
            // 基本地
            "Plains" -> "平原"
            "Island" -> "海岛"
            "Swamp" -> "沼泽"
            "Mountain" -> "山脉"
            "Forest" -> "树林"

            // 积雪基本地
            "Snow-Covered Plains" -> "积雪平原"
            "Snow-Covered Island" -> "积雪海岛"
            "Snow-Covered Swamp" -> "积雪沼泽"
            "Snow-Covered Mountain" -> "积雪山脉"
            "Snow-Covered Forest" -> "积雪树林"

            // 流行非基本地
            "Wasteland" -> "荒原"
            "Strip Mine" -> "矿脉"
            "Ancient Tomb" -> "古墓"
            "Dark Depths" -> "深渊"
            "Tabernacle at Pendrell Vale" -> "彭德尔幽谷小教堂"
            "Bazaar of Baghdad" -> "巴格达集市"
            "Mishra's Workshop" -> "米斯拉的工坊"
            "Karakas" -> "喀洛斯"
            "Glacial Fortress" -> "冰川堡垒"
            "Horizon Canopy" -> "天际冠冕"
            "Flagstones of Trokair" -> "特罗凯尔的旗帜石"
            "Gemstone Caverns" -> "宝石洞窟"
            "Sejiri Steppe" -> "圣教区阶地"
            "Vault of the Archangel" -> "天使金库"
            "Hall of Storm Giants" -> "风暴巨人殿堂"
            "Halls of Storm Giants" -> "风暴巨人殿堂"
            "Noble Hierarch" -> "尊贵大主教"
            "Dryad Arbor" -> "德鲁伊栖木"
            "Inkmoth Nexus" -> "墨蛾连接点"
            "Blinkmoth Nexus" -> "闪光蛾连接点"
            "Gavony Township" -> "加沃尼镇"
            "Kessig Wolf Run" -> "凯西格狼行地"
            "Hissing Quagmire" -> "嘶响沼地"
            "Westvale Abbey" -> "西vale修道院"
            "Urborg, Tomb of Yawgmoth" -> "尤巴该、Yawgmoth 之墓"
            "Bojuka Bog" -> "博久卡沼泽"
            "Phyrexian Tower" -> "非瑞克西亚高塔"
            "Command Tower" -> "指挥塔"

            else -> null
        }
    }

    /**
     * 清除卡牌缓存（用于语言切换后）
     */
    suspend fun clearCardInfoCache() {
        cardInfoDao.clearAll()
        AppLogger.d("DecklistRepository", "Card info cache cleared")
    }

    /**
     * 搜索卡牌（先查缓存，再查API）
     */
    /**
     * 搜索卡牌 - v4.0.0 在线模式
     * 直接调用 MTGCH API 进行搜索
     */
    suspend fun searchCardsByName(query: String, limit: Int = 20): List<CardInfo> =
        withContext(Dispatchers.IO) {
            AppLogger.d("DecklistRepository", "Searching online for: $query (limit: $limit)")

            try {
                // 不使用 ! 前缀，因为 API 的精确搜索不可靠
                val response = mtgchApi.searchCard(
                    query = query,
                    pageSize = limit * 2,  // 获取更多结果以便过滤
                    priorityChinese = true
                )

                if (response.isSuccessful && response.body() != null) {
                    val searchResponse = response.body()!!
                    val results = searchResponse.data

                    if (results != null && results.isNotEmpty()) {
                        // 优先精确匹配的结果
                        val exactMatches = results.filter { card ->
                            val nameMatch = card.name?.equals(query, ignoreCase = true) == true
                            val zhNameMatch = card.zhsName?.equals(query, ignoreCase = true) == true
                            val translatedNameMatch = card.atomicTranslatedName?.equals(query, ignoreCase = true) == true
                            nameMatch || zhNameMatch || translatedNameMatch
                        }

                        // 如果有精确匹配，优先返回；否则返回所有结果（前缀匹配）
                        val finalResults = if (exactMatches.isNotEmpty()) {
                            exactMatches
                        } else {
                            results
                        }

                        AppLogger.d("DecklistRepository", "✓ Found ${finalResults.size} results (exact: ${exactMatches.size})")
                        return@withContext finalResults.take(limit).map { it.toEntity().toDomainModel() }
                    }
                }

                AppLogger.d("DecklistRepository", "No results found online for: $query")
                emptyList()
            } catch (e: Exception) {
                AppLogger.e("DecklistRepository", "Error searching cards: ${e.message}", e)
                emptyList()
            }
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
     * 获取卡牌的所有版本（不同系列）
     * v4.1.0: 用于版本切换功能
     */
    suspend fun getCardAllVersions(cardName: String): List<CardInfo> = withContext(Dispatchers.IO) {
        AppLogger.d("DecklistRepository", "Getting all versions for: $cardName")

        // 标准化卡名（处理连体牌格式）
        val formattedCardName = normalizeCardName(cardName)

        try {
            // 先尝试从本地缓存获取卡牌的英文名
            val cachedCard = cardInfoDao.getCardInfoByNameOrEnName(formattedCardName)
            val searchName = cachedCard?.enName ?: formattedCardName

            AppLogger.d("DecklistRepository", "  Using search name: $searchName (cached: ${cachedCard != null})")

            // 不使用 unique=art 参数，获取所有版本（包括不同重印）
            val response = mtgchApi.searchCard(
                query = searchName,
                pageSize = 175,
                priorityChinese = true
            )

            if (response.isSuccessful && response.body() != null) {
                val searchResponse = response.body()!!
                val results = searchResponse.data

                AppLogger.d("DecklistRepository", "  API returned ${results?.size ?: 0} results")

                if (results != null && results.isNotEmpty()) {
                    // 更宽松的匹配逻辑，包含更多可能的版本
                    val matchingCards = results.filter { card ->
                        // 1. 精确名称匹配（英文名）
                        val exactMatch = card.name?.equals(searchName, ignoreCase = true) == true

                        // 2. 中文名匹配
                        val zhNameMatch = card.zhsName?.equals(cardName, ignoreCase = true) == true

                        // 3. 翻译名称匹配
                        val translatedNameMatch = card.atomicTranslatedName?.equals(cardName, ignoreCase = true) == true

                        // 4. 双面牌特殊处理：检查是否包含卡名
                        val dualFaceMatch = card.name?.contains("//") == true &&
                            (card.name.startsWith("$searchName //", ignoreCase = true) ||
                             card.name.endsWith("// $searchName", ignoreCase = true) ||
                             card.name.contains(" // $searchName //", ignoreCase = true))

                        // 5. 原始卡名匹配（未转换格式）
                        val originalNameMatch = !cardName.contains("//") && card.name?.equals(cardName, ignoreCase = true) == true

                        // 6. 连体牌特殊处理：检查是否匹配原始格式
                        val splitCardMatch = cardName.contains("/") && !formattedCardName.contains("//") &&
                            card.name?.contains("//") == true &&
                            card.name.equals(formattedCardName, ignoreCase = true)

                        exactMatch || zhNameMatch || translatedNameMatch || dualFaceMatch || originalNameMatch || splitCardMatch
                    }

                    AppLogger.d("DecklistRepository", "✓ Found ${matchingCards.size} versions for: $cardName (from ${results.size} results)")

                    // 如果匹配到的版本太少，尝试直接使用 searchName 前缀匹配
                    val finalResults = if (matchingCards.size <= 1 && results.size > 1) {
                        // 使用更宽松的前缀匹配
                        val baseName = searchName.split("//")[0].trim().lowercase()
                        results.filter { card ->
                            card.name?.lowercase()?.startsWith(baseName) == true ||
                            card.name?.lowercase()?.contains(" $baseName") == true
                        }.also {
                            AppLogger.d("DecklistRepository", "  Using prefix match: ${it.size} versions")
                        }
                    } else {
                        matchingCards
                    }

                    // 转换为 CardInfo 并按系列排序
                    return@withContext finalResults
                        .map { it.toEntity().toDomainModel() }
                        .sortedByDescending { it.setName ?: "" }
                }
            }

            AppLogger.d("DecklistRepository", "No versions found for: $cardName (search: $searchName)")
            emptyList()
        } catch (e: Exception) {
            AppLogger.e("DecklistRepository", "Error getting card versions: ${e.message}", e)
            emptyList()
        }
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
     * 从 MTGTop8 爬取赛事列表（只下载赛事，不下载卡组）
     *
     * @param format 格式代码
     * @param date 日期筛选
     * @param maxEvents 最大抓取赛事数量
     * @return Result<Int> 返回下载的赛事数量
     */
    suspend fun scrapeEventsFromMtgTop8(
        format: String,
        date: String? = null,
        maxEvents: Int = 10,
        maxDecksPerEvent: Int = 0  // 此参数不再使用，保留用于兼容性
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            AppLogger.separator("DecklistRepository", "开始下载赛事列表")
            AppLogger.d("DecklistRepository", "Format: $format")
            AppLogger.d("DecklistRepository", "Date: $date")
            AppLogger.d("DecklistRepository", "Max events: $maxEvents")

            // 只获取事件列表，不下载卡组
            val events = mtgTop8Scraper.fetchEventList(format, date, maxEvents)

            AppLogger.d("DecklistRepository", "爬虫返回 ${events.size} 个赛事")
            Log.d("DecklistRepository", "爬虫返回 ${events.size} 个赛事")

            if (events.isEmpty()) {
                AppLogger.e("DecklistRepository", "未能从MTGTop8获取赛事列表")
                Log.e("DecklistRepository", "未能从MTGTop8获取赛事列表")
                return@withContext Result.failure(Exception("Unable to fetch events from MTGTop8. The website structure may have changed or network connection failed."))
            }

            // 输出第一个赛事的信息用于调试
            val firstEvent = events[0]
            Log.d("DecklistRepository", "第一个赛事信息:")
            Log.d("DecklistRepository", "  - eventId: ${firstEvent.eventId}")
            Log.d("DecklistRepository", "  - eventName: ${firstEvent.eventName}")
            Log.d("DecklistRepository", "  - eventUrl: ${firstEvent.eventUrl}")
            Log.d("DecklistRepository", "  - eventDate: ${firstEvent.eventDate}")
            Log.d("DecklistRepository", "  - format: ${firstEvent.format}")

            // 只保存赛事信息
            var savedCount = 0
            var failedCount = 0
            for (eventDto in events) {
                try {
                    AppLogger.d("DecklistRepository", "正在保存赛事: ${eventDto.eventName}")
                    // 保存或更新赛事（不下载卡组）
                    saveEventData(eventDto)
                    savedCount++
                    AppLogger.d("DecklistRepository", "✓ 成功保存: ${eventDto.eventName}")
                } catch (e: Exception) {
                    failedCount++
                    AppLogger.e("DecklistRepository", "✗ 保存赛事失败: ${eventDto.eventName} - ${e.message}")
                    e.printStackTrace()
                }
            }

            AppLogger.d("DecklistRepository", "保存完成: 成功 $savedCount 个，失败 $failedCount 个")

            if (savedCount == 0) {
                Result.failure(Exception("Failed to save any events. Database error occurred."))
            } else {
                Result.success(savedCount)
            }

        } catch (e: Exception) {
            AppLogger.e("DecklistRepository", "下载赛事失败: ${e::class.java.simpleName} - ${e.message}", e)
            Result.failure(Exception("Download failed: ${e.message}"))
        }
    }

    /**
     * 下载单个赛事的所有卡组
     * @param eventUrl 赛事 URL
     * @param format 赛制代码 (ST, MO, PI, etc.)
     */
    suspend fun scrapeSingleEvent(
        eventUrl: String,
        format: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 获取该事件下的所有卡组
            val eventDecklists = mtgTop8Scraper.fetchEventDecklists(
                eventUrl = eventUrl,
                maxDecks = 0 // 下载所有卡组
            )

            if (eventDecklists == null || eventDecklists.decklists.isEmpty()) {
                return@withContext Result.failure(Exception("No decklists found in this event"))
            }

            // 保存或更新赛事
            val eventId = saveEventData(eventDecklists.event)

            // 第一阶段：快速下载并保存所有卡组（不获取Scryfall详情）
            val semaphore = Semaphore(8) // 增加并发数到8
            var totalDecklistsSaved = 0
            val decklistIds = mutableListOf<Long>()

            val saveResults = coroutineScope {
                eventDecklists.decklists.map { decklistDto ->
                    async {
                        semaphore.acquire()
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
                                decklistIds.add(decklistId)
                                1 // 成功保存1个卡组
                            } else {
                                0 // 失败
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            0 // 失败
                        } finally {
                            semaphore.release()
                        }
                    }
                }.awaitAll()
            }

            totalDecklistsSaved += saveResults.sum()

            // 第二阶段：后台异步获取Scryfall详情（不阻塞返回结果）
            if (decklistIds.isNotEmpty()) {
                launch {
                    try {
                        // 并发获取所有卡组的Scryfall详情
                        decklistIds.map { decklistId ->
                            async {
                                try {
                                    fetchScryfallDetails(decklistId)
                                } catch (e: Exception) {
                                    AppLogger.e("DecklistRepository", "Error fetching Scryfall details: ${e.message}")
                                }
                            }
                        }.awaitAll()
                        AppLogger.d("DecklistRepository", "Scryfall details fetched for ${decklistIds.size} decklists")
                    } catch (e: Exception) {
                        AppLogger.e("DecklistRepository", "Error in Scryfall batch fetch: ${e.message}")
                    }
                }
            }

            // 更新事件的卡组数量
            eventDao.updateDeckCount(eventId, eventDecklists.decklists.size)

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
                    source = eventDto.source,
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
                source = eventDto.source,
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
                    deckName = decklistDto.deckName,
                    format = format,
                    date = decklistDto.eventDate,
                    url = decklistDto.url,
                    playerName = decklistDto.playerName,
                    playerId = null,
                    record = decklistDto.record.ifEmpty { null },
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
                deckName = decklistDto.deckName,
                format = format,
                date = decklistDto.eventDate,
                url = decklistDto.url,
                playerName = decklistDto.playerName,
                playerId = null,
                record = decklistDto.record.ifEmpty { null },
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
        deckName = deckName,
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
        date = formatDateToChinese(date),
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
        cardSet = cardSet,
        cardNameZh = displayName  // v4.0.0: 使用中文名显示
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
        imagePath = null,
        // 双面牌图片处理：优先使用card_faces中的图片
        imageUriSmall = if (cardFaces != null && cardFaces.isNotEmpty()) {
            cardFaces[0].imageUris?.small ?: imageUris?.small
        } else {
            imageUris?.small
        },
        imageUriNormal = if (cardFaces != null && cardFaces.isNotEmpty()) {
            cardFaces[0].imageUris?.normal ?: imageUris?.normal
        } else {
            imageUris?.normal
        },
        imageUriLarge = if (cardFaces != null && cardFaces.isNotEmpty()) {
            cardFaces[0].imageUris?.large ?: imageUris?.large
        } else {
            imageUris?.large
        },
        // 双面牌处理
        isDualFaced = cardFaces != null && cardFaces.size >= 2,
        cardFacesJson = cardFaces?.let { com.google.gson.Gson().toJson(it) },
        frontFaceName = if (cardFaces != null && cardFaces.size >= 2) cardFaces[0].name else null,
        backFaceName = if (cardFaces != null && cardFaces.size >= 2) cardFaces[1].name else null,
        frontImageUri = if (cardFaces != null && cardFaces.size >= 2) cardFaces[0].imageUris?.normal else null,
        backImageUri = if (cardFaces != null && cardFaces.size >= 2) cardFaces[1].imageUris?.normal else null
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
        lastUpdated = lastUpdated,
        // 双面牌处理
        isDualFaced = isDualFaced,
        frontFaceName = frontFaceName,
        backFaceName = backFaceName,
        frontImageUri = frontImageUri,
        backImageUri = backImageUri,
        backFaceManaCost = backFaceManaCost,
        backFaceTypeLine = backFaceTypeLine,
        backFaceOracleText = backFaceOracleText,
        backFacePower = backFacePower,
        backFaceToughness = backFaceToughness,
        backFaceLoyalty = backFaceLoyalty
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
                    deckName = link.deckName,
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
                deckName = link.deckName,
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

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(decklistId: Long): Boolean = withContext(Dispatchers.IO) {
        val existing = favoriteDecklistDao.getByDecklistId(decklistId)
        if (existing != null) {
            // 已收藏，取消收藏
            favoriteDecklistDao.delete(existing)
            false
        } else {
            // 未收藏，添加收藏
            val favorite = com.mtgo.decklistmanager.data.local.entity.FavoriteDecklistEntity(
                decklistId = decklistId
            )
            favoriteDecklistDao.insert(favorite)
            true
        }
    }

    /**
     * 检查是否已收藏
     */
    suspend fun isFavorite(decklistId: Long): Boolean = withContext(Dispatchers.IO) {
        favoriteDecklistDao.getByDecklistId(decklistId) != null
    }

    /**
     * 获取收藏的牌组列表
     */
    suspend fun getFavoriteDecklists(): List<Decklist> = withContext(Dispatchers.IO) {
        val decklistIds = favoriteDecklistDao.getAllDecklistIds()
        if (decklistIds.isEmpty()) {
            return@withContext emptyList()
        }

        val decklists = decklistDao.getDecklistsByIds(decklistIds)
        decklists.map { it.toDomainModel() }
    }

    /**
     * 获取收藏数量
     */
    suspend fun getFavoriteCount(): Int = withContext(Dispatchers.IO) {
        favoriteDecklistDao.getCount()
    }

    /**
     * 删除赛事及其所有关联的卡组和卡牌
     */
    suspend fun deleteEvent(eventId: Long) = withContext(Dispatchers.IO) {
        try {
            // 1. 获取该赛事的所有卡组ID
            val decklistIds = decklistDao.getDecklistIdsByEventId(eventId)

            // 2. 删除所有卡牌
            if (decklistIds.isNotEmpty()) {
                cardDao.deleteByDecklistIds(decklistIds)
            }

            // 3. 删除所有卡组的收藏记录
            if (decklistIds.isNotEmpty()) {
                favoriteDecklistDao.deleteByDecklistIds(decklistIds)
            }

            // 4. 删除所有卡组
            eventDao.deleteDecklistsByEventId(eventId)

            // 5. 删除赛事
            eventDao.deleteEventById(eventId)

            AppLogger.d("DecklistRepository", "删除赛事成功: eventId=$eventId, 关联卡组数=${decklistIds.size}")
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("DecklistRepository", "删除赛事失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 修复数据库中双面牌的标记
     * 更新所有包含 " // " 的卡牌为双面牌
     */
    suspend fun fixDualFacedCards(): Int = withContext(Dispatchers.IO) {
        try {
            // 获取所有可能是双面牌的卡牌
            val cards = cardInfoDao.getPossibleDualFacedCards()

            // 逐个处理
            val updatedCards = cards.map { entity ->
                val separatorIndex = entity.name.indexOf(" // ")
                if (separatorIndex > 0) {
                    entity.copy(
                        isDualFaced = true,
                        frontFaceName = entity.name.substring(0, separatorIndex).trim(),
                        backFaceName = entity.name.substring(separatorIndex + 4).trim()
                    )
                } else {
                    entity.copy(isDualFaced = true)
                }
            }

            // 批量更新
            if (updatedCards.isNotEmpty()) {
                cardInfoDao.updateAll(updatedCards)
                AppLogger.d("DecklistRepository", "Fixed ${updatedCards.size} dual-faced cards in database")
            }

            updatedCards.size
        } catch (e: Exception) {
            AppLogger.e("DecklistRepository", "Failed to fix dual-faced cards: ${e.message}", e)
            0
        }
    }

    /**
     * 批量获取卡牌的中文名称
     * @param cardNames 卡牌英文名称列表
     * @return Map<英文名, 中文名>
     */
    suspend fun getChineseNamesForCards(cardNames: List<String>): Map<String, String> = withContext(Dispatchers.IO) {
        if (cardNames.isEmpty()) {
            return@withContext emptyMap()
        }

        try {
            val result = mutableMapOf<String, String>()
            for (name in cardNames) {
                // 优先通过英文名查询
                val cardInfo = cardInfoDao.getCardInfoByEnName(name) ?: cardInfoDao.getCardInfoByName(name)
                if (cardInfo != null) {
                    // name 字段存储的是中文名（或回退到英文名）
                    val chineseName = cardInfo.name
                    // 只有当中文名和英文名不同时才返回
                    if (chineseName != name && chineseName.isNotEmpty()) {
                        result[name] = chineseName
                    }
                }
            }
            result
        } catch (e: Exception) {
            AppLogger.e("DecklistRepository", "Failed to get Chinese names: ${e.message}", e)
            emptyMap()
        }
    }

    /**
     * 将日期从 YYYY-MM-DD 格式转换为中文格式
     * 例如：2025-01-15 -> 2025年1月15日
     */
    private fun formatDateToChinese(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1].toIntOrNull()?.let { if (it < 10) "0${it}".toInt() else it } ?: parts[1]
                val day = parts[2].toIntOrNull()?.let { if (it < 10) "0${it}".toInt() else it } ?: parts[2]
                "${year}年${month}月${day}日"
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    /**
     * 一次性修复：批量更新所有 NULL 的 display_name 和 mana_cost
     * v4.1.0: 从 card_info 表中查找中文名称和法术力值并更新到 cards 表
     * 只需运行一次，之后所有套牌都会显示中文名称和法术力值
     */
    suspend fun fixAllNullDisplayNames(): Int = withContext(Dispatchers.IO) {
        try {
            // 查找所有 display_name 或 mana_cost 为 NULL 的卡牌
            val nullCards = cardDao.getCardsWithMissingData()

            var updatedCount = 0
            for (card in nullCards) {
                // 从 card_info 表中查找中文名称和法术力值
                val cardInfo = cardInfoDao.getCardInfoByEnName(card.cardName)
                if (cardInfo != null) {
                    // 更新 display_name 和 mana_cost
                    cardDao.updateCardDetails(card.id, cardInfo.name, cardInfo.manaCost)
                    updatedCount++
                }
            }

            AppLogger.d("DecklistRepository", "✅ Fixed $updatedCount cards with missing display_names or mana_costs")
            updatedCount
        } catch (e: Exception) {
            AppLogger.e("DecklistRepository", "Failed to fix display names and mana costs: ${e.message}", e)
            0
        }
    }
}
