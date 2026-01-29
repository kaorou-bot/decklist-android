package com.mtgo.decklistmanager.di

import com.mtgo.decklistmanager.data.local.dao.CardDao
import com.mtgo.decklistmanager.data.local.dao.CardInfoDao
import com.mtgo.decklistmanager.data.local.dao.DecklistDao
import com.mtgo.decklistmanager.data.local.dao.EventDao
import com.mtgo.decklistmanager.data.local.database.AppDatabase
import com.mtgo.decklistmanager.data.remote.api.MagicScraper
import com.mtgo.decklistmanager.data.remote.api.MtgTop8Scraper
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

/**
 * App Module - 应用级别的依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 提供 AppDatabase
     */
    @Provides
    @Singleton
    fun provideAppDatabase(application: android.app.Application): AppDatabase {
        return AppDatabase.getInstance(application)
    }

    /**
     * 提供 DecklistDao
     */
    @Provides
    @Singleton
    fun provideDecklistDao(database: AppDatabase): DecklistDao {
        return database.decklistDao()
    }

    /**
     * 提供 CardDao
     */
    @Provides
    @Singleton
    fun provideCardDao(database: AppDatabase): CardDao {
        return database.cardDao()
    }

    /**
     * 提供 CardInfoDao
     */
    @Provides
    @Singleton
    fun provideCardInfoDao(database: AppDatabase): CardInfoDao {
        return database.cardInfoDao()
    }

    /**
     * 提供 EventDao
     */
    @Provides
    @Singleton
    fun provideEventDao(database: AppDatabase): EventDao {
        return database.eventDao()
    }

    /**
     * 提供 OkHttpClient
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 提供 Retrofit for mtgch.com
     */
    @Provides
    @Singleton
    fun provideMtgchRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://mtgch.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 提供 MtgchApi
     */
    @Provides
    @Singleton
    fun provideMtgchApi(retrofit: Retrofit): MtgchApi {
        return retrofit.create(MtgchApi::class.java)
    }

    /**
     * 提供 MagicScraper
     */
    @Provides
    @Singleton
    fun provideMagicScraper(): MagicScraper {
        return MagicScraper()
    }

    /**
     * 提供 MtgTop8Scraper
     */
    @Provides
    @Singleton
    fun provideMtgTop8Scraper(): MtgTop8Scraper {
        return MtgTop8Scraper()
    }
}
