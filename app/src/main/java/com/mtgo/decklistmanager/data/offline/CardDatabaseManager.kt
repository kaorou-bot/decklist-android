package com.mtgo.decklistmanager.data.offline

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.data.local.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

/**
 * Manager for card database download and status
 */
class CardDatabaseManager(private val context: Context) {

    companion object {
        private const val TAG = "CardDatabaseManager"
        private const val PREFS_NAME = "card_database"
        private const val KEY_DATABASE_DOWNLOADED = "database_downloaded"
        private const val KEY_DOWNLOAD_PROGRESS = "download_progress"
        private const val KEY_TOTAL_CARDS = "total_cards"
        const val WORK_TAG = "card_database_import"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if database has been downloaded and actually contains data
     */
    fun isDatabaseDownloaded(): Boolean {
        // 首先检查 SharedPreferences 标记
        val flagValue = prefs.getBoolean(KEY_DATABASE_DOWNLOADED, false)
        Log.d(TAG, "SharedPreferences 中的标记: $flagValue")

        if (!flagValue) {
            return false
        }

        // 即使标记为 true，也要检查数据库中是否真的有数据
        return try {
            val count = runBlocking(Dispatchers.IO) {
                AppDatabase.getInstance(context).cardInfoDao().getCachedCardCount()
            }
            Log.d(TAG, "数据库中实际卡牌数量: $count")
            count > 0
        } catch (e: Exception) {
            Log.e(TAG, "检查数据库卡牌数量失败", e)
            false
        }
    }

    /**
     * Get download progress
     */
    fun getDownloadProgress(): Int {
        return prefs.getInt(KEY_DOWNLOAD_PROGRESS, 0)
    }

    /**
     * Get total cards count
     */
    fun getTotalCards(): Int {
        return prefs.getInt(KEY_TOTAL_CARDS, 0)
    }

    /**
     * Start database download
     */
    fun startDownload() {
        Log.d(TAG, "启动数据库导入任务")
        val workRequest = OneTimeWorkRequestBuilder<CardDatabaseDownloadWorker>()
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    /**
     * Clear database download status (for re-download)
     */
    fun clearDatabase() {
        Log.d(TAG, "清除数据库导入状态")
        prefs.edit().clear().apply()
    }
}
