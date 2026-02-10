package com.mtgo.decklistmanager.di

import com.mtgo.decklistmanager.data.local.database.AppDatabase
import com.mtgo.decklistmanager.data.local.dao.*
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

    /**
     * 提供 FolderDao
     */
    @Provides
    @Singleton
    fun provideFolderDao(database: AppDatabase): FolderDao {
        return database.folderDao()
    }

    /**
     * 提供 TagDao
     */
    @Provides
    @Singleton
    fun provideTagDao(database: AppDatabase): TagDao {
        return database.tagDao()
    }

    /**
     * 提供 DecklistNoteDao
     */
    @Provides
    @Singleton
    fun provideDecklistNoteDao(database: AppDatabase): DecklistNoteDao {
        return database.decklistNoteDao()
    }

    /**
     * 提供 DecklistFolderRelationDao
     */
    @Provides
    @Singleton
    fun provideDecklistFolderRelationDao(database: AppDatabase): DecklistFolderRelationDao {
        return database.decklistFolderRelationDao()
    }

    /**
     * 提供 DecklistTagRelationDao
     */
    @Provides
    @Singleton
    fun provideDecklistTagRelationDao(database: AppDatabase): DecklistTagRelationDao {
        return database.decklistTagRelationDao()
    }
}
