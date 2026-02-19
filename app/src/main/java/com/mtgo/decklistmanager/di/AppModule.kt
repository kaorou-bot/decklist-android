package com.mtgo.decklistmanager.di

import com.mtgo.decklistmanager.data.local.dao.CardDao
import com.mtgo.decklistmanager.data.local.dao.CardInfoDao
import com.mtgo.decklistmanager.data.local.dao.DecklistDao
import com.mtgo.decklistmanager.data.local.dao.EventDao
import com.mtgo.decklistmanager.data.local.database.AppDatabase
import com.mtgo.decklistmanager.data.remote.api.MagicScraper
import com.mtgo.decklistmanager.data.remote.api.MtgTop8Scraper
import com.mtgo.decklistmanager.data.remote.api.ServerApi
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
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
     * 提供 Retrofit for MTGCH API (外部 MTGCH 服务)
     * Base URL: https://mtgch.com/
     * 用于卡牌搜索和详情查询
     */
    @Provides
    @Singleton
    @Named("mtgch")
    fun provideMtgchRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://mtgch.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 提供 Retrofit for Server API (自有服务端)
     * Base URL: http://182.92.109.160/
     * 用于赛事和套牌数据
     */
    @Provides
    @Singleton
    @Named("server")
    fun provideServerRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://182.92.109.160/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMtgchApi(@Named("mtgch") retrofit: Retrofit): MtgchApi {
        return retrofit.create(MtgchApi::class.java)
    }

    /**
     * 提供 ServerApi - 服务端赛事和套牌数据 API
     * Base URL: http://182.92.109.160/
     */
    @Provides
    @Singleton
    fun provideServerApi(@Named("server") retrofit: Retrofit): ServerApi {
        return retrofit.create(ServerApi::class.java)
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
