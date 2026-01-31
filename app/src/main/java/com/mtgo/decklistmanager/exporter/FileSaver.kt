package com.mtgo.decklistmanager.exporter

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.mtgo.decklistmanager.BuildConfig
import com.mtgo.decklistmanager.util.AppLogger
import java.io.File

/**
 * 文件保存工具
 *
 * 将导出的内容保存到设备存储
 */
class FileSaver(private val context: Context) {

    companion object {
        private const val TAG = "FileSaver"
        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.fileprovider"
    }

    /**
     * 保存文件到本地存储
     *
     * @param fileName 文件名
     * @param content 文件内容
     * @return 文件的 Uri
     */
    fun saveFile(fileName: String, content: String): Uri? {
        return try {
            // 使用应用外部文件目录（不需要权限）
            val downloadsDir = getDownloadsDirectory()
            val file = File(downloadsDir, fileName)

            file.writeText(content)

            AppLogger.d(TAG, "File saved to: ${file.absolutePath}")

            // 返回 FileProvider Uri
            FileProvider.getUriForFile(
                context,
                AUTHORITY,
                file
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save file: $fileName", e)
            null
        }
    }

    /**
     * 获取下载目录
     * 使用应用外部文件目录，不需要存储权限
     */
    private fun getDownloadsDirectory(): File {
        // 使用应用外部文件目录下的 exports 文件夹
        val exportsDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "DecklistManager"
        )

        if (!exportsDir.exists()) {
            val created = exportsDir.mkdirs()
            AppLogger.d(TAG, "Directory created: $created, path: ${exportsDir.absolutePath}")
        }

        return exportsDir
    }

    /**
     * 创建临时文件
     *
     * 用于分享到其他应用
     */
    fun createTempFile(fileName: String, content: String): Uri? {
        return try {
            val tempDir = File(context.cacheDir, "exports")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }

            val file = File(tempDir, fileName)
            file.writeText(content)

            FileProvider.getUriForFile(context, AUTHORITY, file)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to create temp file", e)
            null
        }
    }
}
