package com.mtgo.decklistmanager.exporter

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.mtgo.decklistmanager.BuildConfig
import com.mtgo.decklistmanager.util.AppLogger
import java.io.File

/**
 * 文件保存工具
 *
 * 将导出的内容保存到系统 Download 目录
 */
class FileSaver(private val context: Context) {

    companion object {
        private const val TAG = "FileSaver"
        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.fileprovider"
    }

    /**
     * 保存文件到系统 Download 目录
     *
     * @param fileName 文件名
     * @param content 文件内容
     * @return 文件的 Uri
     */
    fun saveFile(fileName: String, content: String): Uri? {
        return try {
            AppLogger.d(TAG, "Saving file: $fileName")
            AppLogger.d(TAG, "Content length: ${content.length}")

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveFileWithMediaStore(fileName, content)
            } else {
                saveFileLegacy(fileName, content)
            }

            AppLogger.d(TAG, "File saved successfully, URI: $uri")
            uri
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save file: $fileName", e)
            null
        }
    }

    /**
     * Android 10+ 使用 MediaStore API 保存文件
     */
    private fun saveFileWithMediaStore(fileName: String, content: String): Uri? {
        val resolver = context.contentResolver

        val values = android.content.ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName))
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

        return uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                    outputStream.flush()
                }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(it, values, null, null)

                AppLogger.d(TAG, "File saved via MediaStore: $it")
                it
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to write file via MediaStore", e)
                null
            }
        }
    }

    /**
     * Android 9 及以下使用传统方式保存文件
     */
    private fun saveFileLegacy(fileName: String, content: String): Uri? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)
            file.writeText(content)

            AppLogger.d(TAG, "File saved via legacy method: ${file.absolutePath}")

            // 返回 FileProvider Uri
            FileProvider.getUriForFile(context, AUTHORITY, file)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save file via legacy method", e)
            null
        }
    }

    /**
     * 根据 MIME 类型获取
     */
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".dek") -> "text/plain"
            fileName.endsWith(".txt") -> "text/plain"
            fileName.endsWith(".json") -> "application/json"
            else -> "text/plain"
        }
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
