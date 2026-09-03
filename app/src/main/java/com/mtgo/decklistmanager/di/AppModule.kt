package com.mtgo.decklistmanager.di

import com.mtgo.decklistmanager.data.local.dao.CardDao
import com.mtgo.decklistmanager.data.local.dao.CardInfoDao
import com.mtgo.decklistmanager.data.local.dao.DecklistDao
import com.mtgo.decklistmanager.data.local.dao.EventDao
import com.mtgo.decklistmanager.data.local.database.AppDatabase
import com.mtgo.decklistmanager.data.remote.api.MagicScraper
import com.mtgo.decklistmanager.data.remote.api.MtgTop8Scraper
import com.mtgo.decklistmanager.data.remote.api.forge.ForgeCardApi
import com.mtgo.decklistmanager.data.remote.api.forge.ForgeCardApiAdapter
import com.mtgo.decklistmanager.data.remote.api.forge.ForgeCardServerConfig
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(application: android.app.Application): AppDatabase {
        return AppDatabase.getInstance(application)
    }

    @Provides
    @Singleton
    fun provideDecklistDao(database: AppDatabase): DecklistDao {
        return database.decklistDao()
    }

    @Provides
    @Singleton
    fun provideCardDao(database: AppDatabase): CardDao {
        return database.cardDao()
    }

    @Provides
    @Singleton
    fun provideCardInfoDao(database: AppDatabase): CardInfoDao {
        return database.cardInfoDao()
    }

    @Provides
    @Singleton
    fun provideEventDao(database: AppDatabase): EventDao {
        return database.eventDao()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 自定义拦截器：打印 cardFaces 的原始 JSON
        val cardFacesInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            // 只处理搜索和印刷版本接口
            val url = request.url.toString()
            if (url.contains("/api/cards/") && response.isSuccessful) {
                val responseBody = response.peekBody(Long.MAX_VALUE)
                val jsonString = responseBody.string()

                // 打印原始 JSON（仅包含 cardFaces 的部分）
                if (jsonString.contains("\"card_faces\"")) {
                    val cardFacesStart = jsonString.indexOf("\"card_faces\"")
                    val cardFacesEnd = jsonString.indexOf("]", cardFacesStart) + 1
                    if (cardFacesEnd > 0) {
                        val cardFacesJson = jsonString.substring(
                            maxOf(0, cardFacesStart - 50),
                            minOf(jsonString.length, cardFacesEnd + 50)
                        )
                        android.util.Log.d("RAW_JSON", "cardFaces JSON: $cardFacesJson")
                    }
                }

                // 打印完整的响应（如果有双面牌）
                if (jsonString.contains("\"isDoubleFaced\":true") || jsonString.contains("\"layout\":\"transform\"")) {
                    android.util.Log.d("RAW_JSON", "=== Dual-Faced Card Full Response ===")
                    android.util.Log.d("RAW_JSON", jsonString.substring(0, minOf(5000, jsonString.length)))
                }
            }

            response
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(cardFacesInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 提供 Retrofit for Forge 中文卡查 API
     * Base URL: https://play.mtg-forge-kaorou.vip:8443/api/v1/
     *
     * 如服务器启用鉴权，自动附加 X-API-Key 请求头
     */
    @Provides
    @Singleton
    fun provideForgeRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val client = if (ForgeCardServerConfig.API_KEY != null) {
            okHttpClient.newBuilder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("X-API-Key", ForgeCardServerConfig.API_KEY!!)
                            .build()
                    )
                }
                .build()
        } else {
            okHttpClient
        }

        return Retrofit.Builder()
            .baseUrl(ForgeCardServerConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideForgeCardApi(retrofit: Retrofit): ForgeCardApi {
        return retrofit.create(ForgeCardApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMtgchApi(forgeCardApi: ForgeCardApi): MtgchApi {
        // 旧接口由 Forge 中文卡查 API 适配器实现（2026-09-03 切换）
        return ForgeCardApiAdapter(forgeCardApi)
    }

    @Provides
    @Singleton
    fun provideMagicScraper(): MagicScraper {
        return MagicScraper()
    }

    @Provides
    @Singleton
    fun provideMtgTop8Scraper(): MtgTop8Scraper {
        return MtgTop8Scraper()
    }
}
