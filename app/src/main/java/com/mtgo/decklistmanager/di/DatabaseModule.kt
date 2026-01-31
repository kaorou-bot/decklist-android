package com.mtgo.decklistmanager.di

import com.mtgo.decklistmanager.data.local.database.AppDatabase
import com.mtgo.decklistmanager.data.local.dao.FavoriteDecklistDao
import com.mtgo.decklistmanager.data.local.dao.SearchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 提供 FavoriteDecklistDao
     */
    @Provides
    @Singleton
    fun provideFavoriteDecklistDao(database: AppDatabase): FavoriteDecklistDao {
        return database.favoriteDecklistDao()
    }

    /**
     * 提供 SearchHistoryDao
     */
    @Provides
    @Singleton
    fun provideSearchHistoryDao(database: AppDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }
}
