package com.mtgo.decklistmanager.data.remote.api.mtgch

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * MTG Card Server API 接口
 * Base URL: http://182.92.109.160/
 *
 * 自有服务端 API - 提供卡牌搜索、详情查询、印刷版本等功能
 * API 文档: API_DOCUMENTATION.md
 *
 * 数据结构:
 * - Card 对象主要字段: id, name, nameZh, manaCost, cmc, colors, typeLine, typeLineZh, oracleText, oracleTextZh
 * - 图片 URLs: imageUris (small, normal, large, png, art_crop, border_crop)
 * - 标识符: oracleId (同名牌共享), scryfallId (每张唯一)
 */
interface MtgchApi {

    /**
     * 搜索卡牌
     *
     * 对应服务端接口: GET /api/cards/search
     *
     * @param q 搜索关键词（支持中英文）
     * @param limit 每页数量，默认 20
     * @param offset 偏移量，默认 0
     * @param unique 去重模式：oracle_id（默认）、id、none
     * @param color 颜色筛选：W,U,B,R,G 或组合 "R,U"
     * @param cmc 法术力值筛选：">3", "=2", "<5"
     * @param type 类型筛选：creature, instant, sorcery 等
     * @param rarity 稀有度：common, uncommon, rare, mythic
     * @param set 系列代码：NEO, MOM 等
     * @return 搜索结果 { success: true, cards: [...], total: 67 }
     */
    @GET("api/cards/search")
    suspend fun searchCard(
        @Query("q") query: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("unique") unique: String? = null,
        @Query("color") color: String? = null,
        @Query("cmc") cmc: String? = null,
        @Query("type") type: String? = null,
        @Query("rarity") rarity: String? = null,
        @Query("set") set: String? = null
    ): Response<MtgchSearchResponse>

    /**
     * 获取随机卡牌
     *
     * 对应服务端接口: GET /api/cards/random
     *
     * @param limit 返回数量，默认 1
     * @return 随机卡牌数组
     */
    @GET("api/cards/random")
    suspend fun getRandomCard(
        @Query("limit") limit: Int? = null
    ): Response<List<MtgchCardDto>>

    /**
     * 通过 Oracle ID 获取单张卡牌
     *
     * 对应服务端接口: GET /api/cards/:oracleId
     * 响应格式: MtgchCardDto 直接返回卡牌对象
     *
     * @param oracleId Oracle ID（同名牌共享同一 ID）
     * @return 卡牌详情
     */
    @GET("api/cards/{oracleId}")
    suspend fun getCardById(
        @Path("oracleId") oracleId: String
    ): Response<MtgchCardDto>

    /**
     * 获取卡牌的所有印刷版本（不同系列、不同图片）
     *
     * 对应服务端接口: GET /api/cards/:oracleId/printings
     * 响应格式: { success: true, cards: [...], total: 67 }
     *
     * @param oracleId Oracle ID
     * @param limit 每页数量，默认 20
     * @param offset 偏移量，默认 0
     * @return 印刷版本列表
     */
    @GET("api/cards/{oracleId}/printings")
    suspend fun getCardPrintings(
        @Path("oracleId") oracleId: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<MtgchSearchResponse>

    /**
     * 按系列查询卡牌
     *
     * 对应服务端接口: GET /api/sets/:setCode/cards
     *
     * @param setCode 系列代码（如 NEO, MOM）
     * @param limit 每页数量
     * @param offset 偏移量
     * @return 该系列的所有卡牌
     */
    @GET("api/sets/{setCode}/cards")
    suspend fun getCardsBySet(
        @Path("setCode") setCode: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<MtgchSearchResponse>

    /**
     * 获取所有系列
     *
     * 对应服务端接口: GET /api/sets
     *
     * @return 所有系列列表
     */
    @GET("api/sets")
    suspend fun getAllSets(): Response<List<SetInfo>>
}

/**
 * 系列信息
 */
data class SetInfo(
    val code: String,
    val name: String,
    val releasedAt: String?
)
