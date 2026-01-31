package com.mtgo.decklistmanager.exporter

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mtgo.decklistmanager.BuildConfig
import com.mtgo.decklistmanager.util.AppLogger
import java.io.File

/**
 * 分享工具
 *
 * 将导出的套牌分享到其他应用
 */
class ShareHelper(private val context: Context) {

    companion object {
        private const val TAG = "ShareHelper"
        private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.fileprovider"
    }

    /**
     * 分享文本
     *
     * @param text 要分享的文本
     * @param title 分享对话框标题
     */
    fun shareText(text: String, title: String = "分享套牌") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, title)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            AppLogger.d(TAG, "Shared text successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to share text", e)
        }
    }

    /**
     * 分享文件
     *
     * @param fileUri 文件的 Uri
     * @param title 分享对话框标题
     */
    fun shareFile(fileUri: Uri, title: String = "分享套牌") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = context.contentResolver.getType(fileUri)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, title)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            AppLogger.d(TAG, "Shared file successfully: $fileUri")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to share file", e)
        }
    }

    /**
     * 分享套牌到微信
     *
     * @param text 套牌内容
     * @return 是否成功分享
     */
    fun shareToWeChat(text: String): Boolean {
        return try {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                setPackage("com.tencent.mm") // 微信包名
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            AppLogger.d(TAG, "Shared to WeChat successfully")
            true
        } catch (e: Exception) {
            AppLogger.w(TAG, "WeChat not installed or failed to share", e)
            // 微信未安装，回退到普通分享
            shareText(text)
            false
        }
    }

    /**
     * 分享套牌到 QQ
     *
     * @param text 套牌内容
     * @return 是否成功分享
     */
    fun shareToQQ(text: String): Boolean {
        return try {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                setPackage("com.tencent.mobileqq") // QQ 包名
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            AppLogger.d(TAG, "Shared to QQ successfully")
            true
        } catch (e: Exception) {
            AppLogger.w(TAG, "QQ not installed or failed to share", e)
            // QQ 未安装，回退到普通分享
            shareText(text)
            false
        }
    }

    /**
     * 分享套牌链接
     *
     * @param url Moxfield 链接
     * @param deckName 套牌名称
     */
    fun shareDeckLink(url: String, deckName: String) {
        val shareText = "【$deckName】\n\n点击查看完整卡表：\n$url"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "分享套牌链接")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
