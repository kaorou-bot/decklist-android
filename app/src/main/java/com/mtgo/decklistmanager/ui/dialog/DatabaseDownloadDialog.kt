package com.mtgo.decklistmanager.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.mtgo.decklistmanager.R

/**
 * 首次使用提示下载卡牌数据库的对话框
 */
class DatabaseDownloadDialog(
    context: Context,
    private val onDownloadStart: () -> Unit,
    private val onSkip: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_database_download)

        // 设置标题
        findViewById<TextView>(R.id.tvTitle).text = "导入卡牌数据库"

        // 设置说明
        findViewById<TextView>(R.id.tvDescription).text =
            "为了更快的查询速度和离线使用，建议导入完整的卡牌数据库。\n\n" +
            "• 数据库大小: 约 62 MB\n" +
            "• 包含 25,000+ 张卡牌信息\n" +
            "• 卡牌图片仍需联网下载\n" +
            "• 导入时间约 1-3 分钟"

        // 开始下载按钮
        findViewById<View>(R.id.btnDownload).setOnClickListener {
            dismiss()
            onDownloadStart()
        }

        // 跳过按钮
        findViewById<View>(R.id.btnSkip).setOnClickListener {
            dismiss()
            onSkip()
        }

        // 取消按钮
        findViewById<View>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }
    }
}
