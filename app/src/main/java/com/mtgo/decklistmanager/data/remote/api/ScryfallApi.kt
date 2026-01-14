package com.mtgo.decklistmanager.data.remote.api

import com.mtgo.decklistmanager.data.remote.api.dto.ScryfallCardDto
import com.mtgo.decklistmanager.data.remote.api.dto.MtgoDecklistDetailDto
import com.mtgo.decklistmanager.data.remote.api.dto.MtgoDecklistLinkDto
import retrofit2.Response
import retrofit2.http.*

/**
 * Scryfall API - 单卡查询接口
 */
interface ScryfallApi {

    /**
     * 精确搜索卡牌
     */
    @GET("cards/named")
    suspend fun searchCardExact(
        @Query("exact") cardName: String
    ): Response<ScryfallCardDto>

    /**
     * 模糊搜索卡牌
     */
    @GET("cards/named")
    suspend fun searchCardFuzzy(
        @Query("fuzzy") cardName: String
    ): Response<ScryfallCardDto>

    /**
     * 高级搜索
     */
    @GET("cards/search")
    suspend fun searchCards(
        @Query("q") query: String,
        @Query("unique") unique: String = "cards",
        @Query("order") order: String = "name",
        @Query("page") page: Int = 1
    ): Response<ScryfallSearchResponseDto>
}

/**
 * Scryfall Search Response DTO
 */
data class ScryfallSearchResponseDto(
    val data: List<ScryfallCardDto>,
    val has_more: Boolean,
    val total_cards: Int
)
