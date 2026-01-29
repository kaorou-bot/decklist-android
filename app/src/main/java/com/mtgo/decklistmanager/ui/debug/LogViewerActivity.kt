package com.mtgo.decklistmanager.ui.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 日志查看器 Activity
 */
class LogViewerActivity : AppCompatActivity() {

    private lateinit var tvLogs: MaterialTextView
    private lateinit var scrollView: ScrollView
    private lateinit var btnRefresh: MaterialButton
    private lateinit var btnClear: MaterialButton
    private lateinit var btnCopy: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)

        tvLogs = findViewById(R.id.tvLogs)
        scrollView = findViewById(R.id.scrollView)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnClear = findViewById(R.id.btnClear)
        btnCopy = findViewById(R.id.btnCopy)

        title = "应用日志"

        setupClickListeners()
        loadLogs()
    }

    private fun setupClickListeners() {
        btnRefresh.setOnClickListener {
            loadLogs()
            Toast.makeText(this, "日志已刷新", Toast.LENGTH_SHORT).show()
        }

        btnClear.setOnClickListener {
            clearLogs()
        }

        btnCopy.setOnClickListener {
            copyLogsToClipboard()
        }
    }

    private fun loadLogs() {
        lifecycleScope.launch {
            try {
                tvLogs.text = "正在加载日志..."
                val logs = withContext(Dispatchers.IO) {
                    // 读取应用日志
                    val process = Runtime.getRuntime().exec("logcat -d -v time")
                    val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                    val logList = mutableListOf<String>()
                    bufferedReader.forEachLine { line ->
                        // 只过滤我们应用的日志
                        if (line.contains("com.mtgo.decklistmanager") ||
                            line.contains("EventDetailViewModel") ||
                            line.contains("DecklistRepository") ||
                            line.contains("MainActivity") ||
                            line.contains("CardDetailViewModel") ||
                            line.contains("MtgchApi") ||
                            line.contains("AppLogger")) {
                            logList.add(line)
                        }
                    }

                    // 只取最后500行
                    val logLines = if (logList.size > 500) {
                        logList.takeLast(500)
                    } else {
                        logList
                    }

                    if (logLines.isEmpty()) {
                        "没有找到应用日志\n\n" +
                        "提示：\n" +
                        "1. 确保应用正在运行\n" +
                        "2. 尝试执行一些操作后再刷新\n" +
                        "3. 或使用 Android Studio 的 Logcat 查看完整日志"
                    } else {
                        logLines.joinToString("\n")
                    }
                }
                tvLogs.text = logs

                // 滚动到底部
                scrollView.post {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            } catch (e: Exception) {
                tvLogs.text = "读取日志失败: ${e.message}\n\n" +
                        "提示: 需要授予 READ_LOGS 权限\n" +
                        "或使用 Android Studio 的 Logcat 查看\n\n" +
                        "错误详情: ${e.stackTraceToString()}"
            }
        }
    }

    private fun clearLogs() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Runtime.getRuntime().exec("logcat -c")
                }
                tvLogs.text = "日志已清除"
                Toast.makeText(this@LogViewerActivity, "日志已清除", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@LogViewerActivity, "清除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyLogsToClipboard() {
        val logs = tvLogs.text.toString()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("应用日志", logs)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }
}
