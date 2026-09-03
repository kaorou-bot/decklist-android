package com.mtgo.decklistmanager.data.remote.api.forge

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Forge 中文卡查 API（forge-card-api v1）
 *
 * Base URL: https://play.mtg-forge-kaorou.vip:8443/api/v1/
 * 规范文档: openapi.yaml（项目外部资料）
 *
 * 特点:
 * - 只读 JSON API，UTF-8，支持 gzip/CORS
 * - 卡牌标识为 24 位十六进制 id（非 Scryfall oracle_id）
 * - 同名卡牌只返回一次；详情 printings 包含全部印刷版本
 * - 卡图为自有 CDN 的中文卡图 HTTPS URL
 * - 测试期无需鉴权；如启用则通过 X-API-Key 请求头
 */
interface ForgeCardApi {

    /**
     * API、数据和卡图版本信息
     */
    @GET("meta")
    suspend fun meta(): Response<ForgeMetaDto>

    /**
     * 卡牌搜索、分页和高级筛选
     *
     * @param q 英文名、中文名、类别与规则文本关键词
     * @param page 页码，默认 1
     * @param pageSize 每页数量，默认 20，最大 100
     * @param colors 逗号分隔的 W,U,B,R,G,C
     * @param colorMode contains / exact / any
     * @param manaValue 法术力值精确匹配
     * @param minManaValue 最小法术力值
     * @param maxManaValue 最大法术力值
     * @param type 英文或中文类别关键词
     * @param rarity 逗号分隔的稀有度
     * @param set Forge 系列代码
     */
    @GET("cards")
    suspend fun searchCards(
        @Query("q") q: String? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
        @Query("colors") colors: String? = null,
        @Query("color_mode") colorMode: String? = null,
        @Query("mana_value") manaValue: Int? = null,
        @Query("min_mana_value") minManaValue: Int? = null,
        @Query("max_mana_value") maxManaValue: Int? = null,
        @Query("type") type: String? = null,
        @Query("rarity") rarity: String? = null,
        @Query("set") set: String? = null
    ): Response<ForgeCardPageDto>

    /**
     * 卡牌详情：中文文本、双面和全部印刷版本
     *
     * @param id 24 位十六进制卡牌 id
     */
    @GET("cards/{id}")
    suspend fun getCardDetail(
        @Path("id") id: String
    ): Response<ForgeCardDto>

    /**
     * 系列列表和中文系列名
     */
    @GET("sets")
    suspend fun getSets(
        @Query("q") q: String? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null
    ): Response<ForgeSetPageDto>
}

/**
 * 服务器配置
 */
object ForgeCardServerConfig {
    const val BASE_URL = "https://play.mtg-forge-kaorou.vip:8443/api/v1/"

    /** 如服务器启用鉴权，在此配置密钥（通过 X-API-Key 请求头传递） */
    val API_KEY: String? = null
}
