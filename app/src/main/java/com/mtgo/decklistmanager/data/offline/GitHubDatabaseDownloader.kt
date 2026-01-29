package com.mtgo.decklistmanager.data.offline

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 从GitHub下载完整卡牌数据库
 */
class GitHubDatabaseDownloader(private val context: Context) {

    companion object {
        private const val TAG = "GitHubDatabaseDownloader"
        // GitHub raw 文件 URL (需要替换为实际的URL)
        private const val GITHUB_DATABASE_URL = "https://raw.githubusercontent.com/kaorou-bot/decklist-android/main/mtgch_cards.jsonl"
        private const val DATABASE_FILE_NAME = "mtgch_cards.jsonl"
    }

    /**
     * 检查本地数据库是否存在
     */
    fun isDatabaseDownloaded(): Boolean {
        val file = File(context.filesDir, DATABASE_FILE_NAME)
        return file.exists() && file.length() > 100_000_000 // 至少100MB
    }

    /**
     * 从GitHub下载数据库
     * @param onProgress 进度回调 (current, total, percent)
     */
    suspend fun downloadFromGitHub(
        onProgress: ((current: Long, total: Long, percent: Int) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始从GitHub下载数据库...")

            val url = URL(GITHUB_DATABASE_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            val totalSize = connection.contentLengthLong
            Log.d(TAG, "数据库文件大小: ${totalSize / 1024 / 1024} MB")

            val inputStream = connection.inputStream
            val outputFile = File(context.filesDir, DATABASE_FILE_NAME)
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // 每读取1MB报告一次进度
                        if (totalBytesRead % (1024 * 1024) == 0L || totalBytesRead == totalSize) {
                            val percent = if (totalSize > 0) {
                                ((totalBytesRead * 100) / totalSize).toInt()
                            } else {
                                0
                            }
                            onProgress?.invoke(totalBytesRead, totalSize, percent)
                            Log.d(TAG, "下载进度: $percent% (${totalBytesRead / 1024 / 1024} MB / ${totalSize / 1024 / 1024} MB)")
                        }
                    }
                }
            }

            connection.disconnect()

            Log.d(TAG, "数据库下载完成: ${outputFile.absolutePath}")
            Result.success(outputFile)

        } catch (e: Exception) {
            Log.e(TAG, "从GitHub下载数据库失败", e)
            Result.failure(e)
        }
    }

    /**
     * 获取数据库文件路径
     */
    fun getDatabaseFile(): File {
        return File(context.filesDir, DATABASE_FILE_NAME)
    }
}
