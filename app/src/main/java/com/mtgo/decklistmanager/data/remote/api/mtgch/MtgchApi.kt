package com.mtgo.decklistmanager.data.remote.api.mtgch

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * MTGCH (大学院废墟) API 接口
 * API 文档: https://mtgch.com/api/v1/docs
 */
interface MtgchApi {

    /**
     * 通过查询字符串搜索卡牌
     * @param q 查询字符串（支持精确查询，例如："!黑土退藏"）
     * @param page 页码（默认1）
     * @param pageSize 每页数量（默认20，最大100）
     * @param unique 去重方式: id, oracle_id, illustration_id
     * @param priorityChinese 是否优先显示中文卡牌（默认true）
     * @param view 视图类型：0为详细信息，1为前端展示信息
     * @param lang 语言代码（zh=中文，en=英文）
     */
    @GET("api/v1/result")
    suspend fun searchCard(
        @Query("q") query: String,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
        @Query("unique") unique: String? = null,
        @Query("priority_chinese") priorityChinese: Boolean? = null,
        @Query("view") view: Int? = null,
        @Query("lang") lang: String? = null
    ): retrofit2.Response<MtgchSearchResponse>

    /**
     * 通过UUID获取单张卡牌
     */
    @GET("api/v1/card/{card_id}/")
    suspend fun getCardById(
        @Path("card_id") cardId: String,
        @Query("view") view: Int? = null
    ): retrofit2.Response<MtgchCardDto>

    /**
     * 通过系列代码和收藏编号获取单张卡牌
     */
    @GET("api/v1/card/{set}/{collector_number}/")
    suspend fun getCardBySetAndNumber(
        @Path("set") setCode: String,
        @Path("collector_number") collectorNumber: String,
        @Query("view") view: Int? = null
    ): retrofit2.Response<MtgchCardDto>

    /**
     * 获取随机卡牌
     */
    @GET("api/v1/random")
    suspend fun getRandomCard(): retrofit2.Response<MtgchCardDto>
}
