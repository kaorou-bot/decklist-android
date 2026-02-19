package com.mtgo.decklistmanager.data.remote.api

import com.mtgo.decklistmanager.data.remote.api.dto.EventDto
import com.mtgo.decklistmanager.data.remote.api.dto.EventsResponse
import com.mtgo.decklistmanager.data.remote.api.dto.DecklistDto
import com.mtgo.decklistmanager.data.remote.api.dto.DecklistDetailDto
import com.mtgo.decklistmanager.data.remote.api.dto.DecklistDetailResponse
import com.mtgo.decklistmanager.data.remote.api.dto.DecklistsResponse
import com.mtgo.decklistmanager.data.remote.api.dto.EventDetailResponse
import com.mtgo.decklistmanager.data.remote.api.dto.CardSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 服务端 API - 赛事、卡组和卡牌数据
 *
 * 服务端已迁移爬虫逻辑，安卓端通过此接口获取数据
 */
interface ServerApi {

    /**
     * 获取赛事列表
     *
     * @param format 赛制代码（可选），如 "MO"(摩登), "ST"(标准)
     * @param date 日期筛选 YYYY-MM-DD（可选）
     * @param limit 每页数量（默认 50）
     * @param offset 偏移量（默认 0）
     */
    @GET("api/v1/events")
    suspend fun getEvents(
        @Query("format") format: String? = null,
        @Query("date") date: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<EventsResponse>

    /**
     * 获取赛事详情
     */
    @GET("api/v1/events/{eventId}")
    suspend fun getEventDetail(
        @Path("eventId") eventId: Long
    ): Response<EventDetailResponse>

    /**
     * 获取赛事下的卡组列表
     *
     * @param eventId 赛事 ID
     * @param limit 每页数量（默认 50）
     * @param offset 偏移量（默认 0）
     */
    @GET("api/v1/events/{eventId}/decklists")
    suspend fun getEventDecklists(
        @Path("eventId") eventId: Long,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<DecklistsResponse>

    /**
     * 获取卡组详情（含卡牌）
     */
    @GET("api/v1/decklists/{decklistId}")
    suspend fun getDecklistDetail(
        @Path("decklistId") decklistId: Long
    ): Response<DecklistDetailResponse>

    /**
     * 搜索卡牌
     *
     * @param q 搜索关键词（卡牌名称）
     * @param limit 返回数量（默认 20）
     */
    @GET("api/cards/search")
    suspend fun searchCard(
        @Query("q") q: String,
        @Query("limit") limit: Int = 20
    ): Response<CardSearchResponse>

    /**
     * 获取卡牌的所有印刷版本
     *
     * @param oracleId 卡牌 Oracle ID
     * @param limit 每页数量（默认 20）
     * @param offset 偏移量（默认 0）
     */
    @GET("api/cards/{oracleId}/printings")
    suspend fun getCardPrintings(
        @Path("oracleId") oracleId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<CardSearchResponse>

    /**
     * 按卡牌名称搜索印刷版本
     *
     * @param name 卡牌名称
     * @param limit 每页数量（默认 100）
     */
    @GET("api/cards/printings")
    suspend fun searchCardPrintingsByName(
        @Query("name") name: String,
        @Query("limit") limit: Int = 100
    ): Response<CardSearchResponse>
}
