package com.mtgo.decklistmanager.ui.decklist

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.domain.model.Event
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Event Detail Activity - 赛事详情界面（二级：显示该赛事下的所有卡组）
 */
@AndroidEntryPoint
class EventDetailActivity : AppCompatActivity() {

    private val viewModel: EventDetailViewModel by viewModels()

    private lateinit var rvDecklists: RecyclerView
    private lateinit var decklistTableAdapter: DecklistTableAdapter
    private lateinit var tvEventName: MaterialTextView
    private lateinit var tvFormat: MaterialTextView
    private lateinit var tvDate: MaterialTextView
    private lateinit var tvDeckCount: MaterialTextView
    private lateinit var tvStatus: MaterialTextView
    private lateinit var tvProgressText: MaterialTextView
    private lateinit var btnDownloadDecklists: MaterialButton
    private lateinit var bottomNavigation: com.google.android.material.bottomnavigation.BottomNavigationView
    private lateinit var progressContainer: View
    private lateinit var progressBar: View

    private var eventId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        eventId = intent.getLongExtra("eventId", -1)
        if (eventId == -1L) {
            Toast.makeText(this, "Invalid event ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupBottomNavigation()
        setupObservers()
        setupClickListeners()

        // Load event detail
        viewModel.loadEventDetail(eventId)
    }

    private fun initViews() {
        rvDecklists = findViewById(R.id.rvDecklists)
        tvEventName = findViewById(R.id.tvEventName)
        tvFormat = findViewById(R.id.tvFormat)
        tvDate = findViewById(R.id.tvDate)
        tvDeckCount = findViewById(R.id.tvDeckCount)
        tvStatus = findViewById(R.id.tvStatus)
        tvProgressText = findViewById(R.id.tvProgressText)
        btnDownloadDecklists = findViewById(R.id.btnDownloadDecklists)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        progressContainer = findViewById(R.id.progressContainer)
        progressBar = findViewById(R.id.progressBar)

        // Verify critical views exist
        AppLogger.d("EventDetailActivity", "Views initialized successfully")
    }

    private fun setupBottomNavigation() {
        try {
            bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_event_list -> {
                        // Navigate back to MainActivity (Event List tab)
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        finish()
                        true
                    }
                    R.id.nav_favorites -> {
                        // Navigate to MainActivity (Favorites tab)
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("tab", "favorites")
                        }
                        startActivity(intent)
                        finish()
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            AppLogger.e("EventDetailActivity", "Error setting up bottom navigation: ${e.message}")
        }
    }

    private fun setupRecyclerView() {
        decklistTableAdapter = DecklistTableAdapter { decklist ->
            // Open deck detail activity
            val intent = Intent(this, DeckDetailActivity::class.java).apply {
                putExtra("decklistId", decklist.id)
                putExtra("eventId", eventId)
            }
            startActivity(intent)
        }

        rvDecklists.apply {
            layoutManager = LinearLayoutManager(this@EventDetailActivity)
            adapter = decklistTableAdapter
        }
    }

    private fun setupObservers() {
        // Observe event
        viewModel.event.observe(this) { event ->
            event?.let {
                tvEventName.text = it.eventName
                tvFormat.text = it.format
                tvDate.text = it.date
                tvDeckCount.text = "${it.deckCount} Decks"

                // 不再自动下载，而是在 decklists observer 中检查是否需要提示
            }
        }

        // Observe decklists
        viewModel.decklists.observe(this) { items ->
            AppLogger.d("EventDetailActivity", "Decklists observer triggered - items: ${items?.size ?: "null"}")
            // Check if downloading is in progress
            val isDownloading = viewModel.uiState.value is EventDetailViewModel.UiState.Downloading
            AppLogger.d("EventDetailActivity", "isDownloading: $isDownloading")

            val decklists = items.map { item ->
                Decklist(
                    id = item.id,
                    eventName = item.eventName,
                    eventType = null,
                    deckName = item.deckName,
                    format = item.format,
                    date = item.date,
                    url = "",
                    playerName = item.playerName,
                    playerId = null,
                    record = item.record,
                    isLoading = isDownloading  // Show loading indicator for all items during download
                )
            }
            // 按URL中的d=参数排序
            val sortedDecklists = decklists.sortedBy { decklist ->
                // 从url中提取d=参数的数字部分，格式: "...?d=123" 或 "...&d=123"
                val dPattern = Regex("[?&]d=(\\d+)")
                val match = dPattern.find(decklist.url)
                if (match != null) {
                    match.groupValues[1].toIntOrNull() ?: 0
                } else {
                    0
                }
            }
            decklistTableAdapter.submitList(sortedDecklists)

            AppLogger.d("EventDetailActivity", "sortedDecklists size: ${sortedDecklists.size}")
            // 如果套牌列表为空且不在下载中，显示下载提示对话框
            if (sortedDecklists.isEmpty() && !isDownloading) {
                // 延迟检查，确保数据已经加载完成
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    val shouldShow = viewModel.shouldShowDownloadDialog()
                    AppLogger.d("EventDetailActivity", "shouldShowDownloadDialog: $shouldShow (delayed check)")
                    if (shouldShow) {
                        AppLogger.d("EventDetailActivity", "Showing download dialog for empty event")
                        showDownloadDialogIfNeeded()
                    }
                }, 100)  // 延迟100ms
            }
        }

        // Observe UI state to control progress indicator
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is EventDetailViewModel.UiState.Loading -> {
                        // Initial loading - don't show overlay
                        progressContainer.visibility = View.GONE
                    }
                    is EventDetailViewModel.UiState.Downloading -> {
                        // Show inline progress
                        progressContainer.visibility = View.VISIBLE
                        tvStatus.text = "正在下载套牌..."
                    }
                    is EventDetailViewModel.UiState.Success -> {
                        // Hide progress
                        progressContainer.visibility = View.GONE
                        val message = (state as EventDetailViewModel.UiState.Success).message
                        tvStatus.text = message
                    }
                    is EventDetailViewModel.UiState.Error -> {
                        // Hide progress
                        progressContainer.visibility = View.GONE
                        val message = (state as EventDetailViewModel.UiState.Error).message
                        tvStatus.text = "错误: $message"
                    }
                    else -> {
                        progressContainer.visibility = View.GONE
                    }
                }
            }
        }

        // Observe status messages
        lifecycleScope.launch {
            viewModel.statusMessage.collect { message ->
                message?.let {
                    tvStatus.text = "Status: $it"
                    Toast.makeText(this@EventDetailActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearStatusMessage()
                }
            }
        }
    }

    private fun setupClickListeners() {
        // 返回按钮
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnDownloadDecklists.setOnClickListener {
            // Get event info
            val event = viewModel.event.value
            if (event != null && event.sourceUrl != null) {
                // event.format 已经是format code（如 "MO", "ST"）
                // 直接使用即可，不需要转换
                val formatCode = event.format

                // Show confirmation dialog
                android.app.AlertDialog.Builder(this)
                    .setTitle("下载套牌")
                    .setMessage("下载此赛事的所有套牌?\n\n赛事: ${event.eventName}\n赛制: ${event.format}")
                    .setPositiveButton("下载") { _, _ ->
                        viewModel.downloadEventDecklists(event.sourceUrl, formatCode)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else {
                Toast.makeText(this, "赛事信息不可用", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 如果赛事没有套牌，显示下载提示对话框
     */
    private fun showDownloadDialogIfNeeded() {
        val event = viewModel.event.value
        if (event != null && event.sourceUrl != null) {
            android.app.AlertDialog.Builder(this)
                .setTitle("赛事暂无套牌")
                .setMessage("当前赛事还没有套牌数据，是否下载该赛事的套牌?\n\n赛事: ${event.eventName}\n赛制: ${event.format}")
                .setPositiveButton("下载") { _, _ ->
                    val formatCode = event.format
                    viewModel.downloadEventDecklists(event.sourceUrl, formatCode)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
}
