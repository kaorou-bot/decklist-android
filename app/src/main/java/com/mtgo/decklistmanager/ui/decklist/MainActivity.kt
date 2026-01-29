@file:Suppress("unused")
package com.mtgo.decklistmanager.ui.decklist

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.domain.model.Event
import com.mtgo.decklistmanager.ui.base.BaseActivity
import com.mtgo.decklistmanager.ui.debug.LogViewerActivity
import com.mtgo.decklistmanager.util.LanguagePreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * Main Activity - 主界面（带底部导航栏）
 */
@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val languagePreferenceManager: LanguagePreferenceManager by lazy { LanguagePreferenceManager(this) }

    private lateinit var rvDecklists: RecyclerView
    private lateinit var decklistAdapter: DecklistAdapter
    private lateinit var eventAdapter: EventAdapter
    private lateinit var eventSectionAdapter: EventSectionAdapter
    private lateinit var filterBar: View
    private lateinit var formatSelector: View
    private lateinit var dateSelector: View
    private lateinit var btnDownloadEvent: MaterialButton
    private lateinit var bottomNavigation: com.google.android.material.bottomnavigation.BottomNavigationView
    private lateinit var tvCurrentFormat: android.widget.TextView
    private lateinit var tvCurrentDate: android.widget.TextView

    private var currentTab = TAB_EVENT_LIST
    private var itemTouchHelper: ItemTouchHelper? = null
    private var isProgrammaticNav = false // Flag to prevent listener loops

    companion object {
        private const val TAB_EVENT_LIST = 0
        private const val TAB_FAVORITES = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))

        initViews()
        setupRecyclerView()
        setupBottomNavigation()
        setupObservers()
        setupClickListeners()

        // v4.0.0 在线模式：无需检查本地数据库，直接使用 MTGCH API
        // checkOfflineDatabase()

        // Initial tab selection - don't trigger listener
        isProgrammaticNav = true
        val requestedTab = intent.getStringExtra("tab")
        when (requestedTab) {
            "favorites" -> switchToTab(TAB_FAVORITES)
            else -> switchToTab(TAB_EVENT_LIST)
        }
        isProgrammaticNav = false
    }

    private fun initViews() {
        rvDecklists = findViewById(R.id.rvDecklists)
        progressOverlay = findViewById(R.id.progressOverlay)
        filterBar = findViewById(R.id.filterBar)
        formatSelector = findViewById(R.id.formatSelector)
        dateSelector = findViewById(R.id.dateSelector)
        btnDownloadEvent = findViewById(R.id.btnDownloadEvent)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        tvCurrentFormat = findViewById(R.id.tvCurrentFormat)
        tvCurrentDate = findViewById(R.id.tvCurrentDate)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            // Ignore programmatic navigation to prevent loops
            if (isProgrammaticNav) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_event_list -> {
                    switchToTab(TAB_EVENT_LIST)
                    true
                }
                R.id.nav_favorites -> {
                    switchToTab(TAB_FAVORITES)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchToTab(tab: Int) {
        // Avoid redundant switching
        if (currentTab == tab) {
            return
        }

        currentTab = tab

        when (tab) {
            TAB_EVENT_LIST -> {
                // Show filter bar
                filterBar.visibility = View.VISIBLE
                // Switch to event adapter
                switchAdapter(true)
                // Load events
                viewModel.loadEvents()
                // Set selected item in bottom navigation - don't trigger listener
                isProgrammaticNav = true
                bottomNavigation.post {
                    bottomNavigation.selectedItemId = R.id.nav_event_list
                    isProgrammaticNav = false
                }
            }
            TAB_FAVORITES -> {
                // Hide filter bar
                filterBar.visibility = View.GONE
                // Switch to decklist adapter
                switchAdapter(false)
                // Load favorites
                viewModel.loadFavoriteDecklists()
                // Set selected item in bottom navigation - don't trigger listener
                isProgrammaticNav = true
                bottomNavigation.post {
                    bottomNavigation.selectedItemId = R.id.nav_favorites
                    isProgrammaticNav = false
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Format selector in status bar
        formatSelector.setOnClickListener {
            showFormatFilterDialog()
        }

        // Date selector in status bar
        dateSelector.setOnClickListener {
            showDateFilterDialog()
        }

        // Download event button
        btnDownloadEvent.setOnClickListener {
            showDownloadEventDialog()
        }
    }

    private fun setupRecyclerView() {
        // Decklist Adapter for favorites tab
        decklistAdapter = DecklistAdapter { decklist ->
            // Open deck detail activity
            val intent = Intent(this, DeckDetailActivity::class.java).apply {
                putExtra("decklistId", decklist.id)
            }
            startActivity(intent)
        }

        // Event Adapter for event list tab
        eventAdapter = EventAdapter { event ->
            // Open event detail activity
            val intent = Intent(this, EventDetailActivity::class.java).apply {
                putExtra("eventId", event.id)
            }
            startActivity(intent)
        }

        // Event Section Adapter with date grouping
        eventSectionAdapter = EventSectionAdapter { event ->
            // Open event detail activity
            val intent = Intent(this, EventDetailActivity::class.java).apply {
                putExtra("eventId", event.id)
            }
            startActivity(intent)
        }

        rvDecklists.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = eventSectionAdapter // 默认使用 EventSectionAdapter (带日期分组)
        }

        // Setup swipe to delete for events
        setupSwipeToDelete()
    }

    /**
     * 设置滑动删除功能
     */
    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // 不支持拖拽
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 只在赛事列表标签页允许删除
                if (currentTab == TAB_EVENT_LIST && rvDecklists.adapter == eventAdapter) {
                    val position = viewHolder.adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val event = eventAdapter.getItemAtPosition(position)

                        // 立即恢复视图
                        viewHolder.itemView.alpha = 1f
                        viewHolder.itemView.translationX = 0f

                        // 转换为EventItem
                        val eventItem = MainViewModel.EventItem(
                            id = event.id,
                            eventName = event.eventName,
                            eventType = event.eventType,
                            format = event.format,
                            date = event.date,
                            sourceUrl = event.sourceUrl,
                            source = event.source,
                            deckCount = event.deckCount
                        )

                        // 显示确认对话框
                        showDeleteEventDialog(eventItem, position, viewHolder)
                    }
                } else {
                    // 如果不是赛事列表，恢复视图
                    viewHolder.itemView.alpha = 1f
                    viewHolder.itemView.translationX = 0f
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                // 只在赛事列表标签页绘制删除背景
                if (currentTab == TAB_EVENT_LIST && rvDecklists.adapter == eventAdapter) {
                    val itemView = viewHolder.itemView
                    val paint = Paint()
                    val textPaint = Paint()
                    textPaint.color = Color.WHITE
                    textPaint.textSize = 48f

                    when {
                        dX > 0 -> {
                            // 向右滑动
                            paint.color = Color.parseColor("#FF5252")
                            c.drawRect(
                                RectF(
                                    itemView.left.toFloat(),
                                    itemView.top.toFloat(),
                                    dX,
                                    itemView.bottom.toFloat()
                                ), paint
                            )
                            // 绘制"删除"文字
                            val deleteText = "删除"
                            val textWidth = textPaint.measureText(deleteText)
                            val textX = itemView.left + dX / 2 - textWidth / 2
                            val textY = itemView.top + (itemView.bottom - itemView.top) / 2 + 48f / 3
                            c.drawText(deleteText, textX, textY, textPaint)
                        }
                        dX < 0 -> {
                            // 向左滑动
                            paint.color = Color.parseColor("#FF5252")
                            c.drawRect(
                                RectF(
                                    itemView.right.toFloat() + dX,
                                    itemView.top.toFloat(),
                                    itemView.right.toFloat(),
                                    itemView.bottom.toFloat()
                                ), paint
                            )
                            // 绘制"删除"文字
                            val deleteText = "删除"
                            val textWidth = textPaint.measureText(deleteText)
                            val textX = itemView.right + dX / 2 - textWidth / 2
                            val textY = itemView.top + (itemView.bottom - itemView.top) / 2 + 48f / 3
                            c.drawText(deleteText, textX, textY, textPaint)
                        }
                    }
                }

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }

        itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper?.attachToRecyclerView(rvDecklists)
    }

    /**
     * 切换适配器（赛事/卡组）
     */
    private fun switchAdapter(isEventList: Boolean) {
        if (isEventList) {
            rvDecklists.adapter = eventSectionAdapter
            itemTouchHelper?.attachToRecyclerView(rvDecklists)
        } else {
            rvDecklists.adapter = decklistAdapter
            itemTouchHelper?.attachToRecyclerView(null)
        }
    }

    private fun setupObservers() {
        // Observe events and group by date
        viewModel.events.observe(this) { items ->
            val events = items.map { item ->
                Event(
                    id = item.id,
                    eventName = item.eventName,
                    eventType = item.eventType,
                    format = item.format,
                    date = item.date,
                    sourceUrl = item.sourceUrl,
                    source = item.source,
                    deckCount = item.deckCount
                )
            }

            // 按日期分组
            val grouped: List<EventListItem> = groupEventsByDate(events)
            eventSectionAdapter.submitList(grouped)
        }

        // Observe decklists (收藏列表)
        viewModel.decklists.observe(this) { items ->
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
                    record = item.record
                )
            }
            decklistAdapter.submitList(decklists)
        }

        collectFlow(viewModel.uiState) { state ->
            when (state) {
                is MainViewModel.UiState.Loading,
                is MainViewModel.UiState.Scraping -> showLoading()
                else -> hideLoading()
            }
        }

        collectFlow(viewModel.favoriteCount) { _ ->
            // Favorites button no longer shows count number
            // btnFavorites.text = "我的收藏"  // Removed: BottomNavigationView doesn't support dynamic text
        }

        // Update format filter status bar text with current selection
        collectFlow(viewModel.selectedFormatName) { formatName ->
            val displayText = if (formatName == null || formatName == "All Formats") {
                "全部"
            } else {
                formatName
            }

            tvCurrentFormat.text = "赛制: $displayText"
        }

        // Update date filter status bar text with current selection
        collectFlow(viewModel.selectedDate) { date ->
            val displayText = date ?: "全部"

            tvCurrentDate.text = "日期: $displayText"
        }

        collectFlow(viewModel.statusMessage) { message ->
            message?.let {
                Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                viewModel.clearStatusMessage()
            }
        }
    }

    private fun showFormatFilterDialog() {
        lifecycleScope.launch {
            // Use .value to get current StateFlow value
            val formats = viewModel.availableFormats.value
            if (formats.isEmpty()) {
                // 不显示提示，直接返回
                return@launch
            }

            val items = arrayOf("All Formats") + formats.toTypedArray()
            showSingleChoiceDialog("选择赛制", items) { _, selected ->
                val format = if (selected == "All Formats") null else selected
                viewModel.applyFormatFilter(format)
            }
        }
    }

    private fun showDateFilterDialog() {
        lifecycleScope.launch {
            // Use .value to get current StateFlow value
            val dates = viewModel.availableDates.value
            if (dates.isEmpty()) {
                // 不显示提示，直接显示空列表
                return@launch
            }

            val items = arrayOf("All Dates") + dates.toTypedArray()
            showSingleChoiceDialog("选择日期", items) { _, selected ->
                val date = if (selected == "All Dates") null else selected
                viewModel.applyDateFilter(date)
            }
        }
    }

    private fun showDownloadEventDialog() {
        lifecycleScope.launch {
            // 获取当前选中的赛制
            val currentFormat = viewModel.selectedFormatName.value
            val formats = arrayOf("Modern", "Standard", "Legacy", "Vintage", "Pauper", "Pioneer", "Historic", "Alchemy", "Premodern")
            // 默认选中当前筛选的赛制，如果没有则默认Modern
            val defaultIndex = if (currentFormat != null && currentFormat != "All Formats") {
                formats.indexOf(currentFormat).let { if (it >= 0) it else 0 }
            } else {
                0
            }
            var selectedFormat = formats[defaultIndex]

            val dialog = android.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("下载赛事列表")
                .setMessage("选择赛制下载赛事列表。下载后点击赛事可以查看详情并下载套牌。")
                .setSingleChoiceItems(formats, defaultIndex) { _, which ->
                    selectedFormat = formats[which]
                }
                .setPositiveButton("下一步") { _, _ ->
                    // 使用FormatMapper转换format name到code
                    val formatCode = com.mtgo.decklistmanager.util.FormatMapper.nameToCode(selectedFormat) ?: "MO"
                    // 日期选择对话框
                    showDateSelectionDialog(formatCode)
                }
                .setNegativeButton("取消", null)
                .create()

            dialog.show()
        }
    }

    private fun showDateSelectionDialog(formatCode: String) {
        // Create date selection dialog
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        // Date label
        val dateLabel = android.widget.TextView(this).apply {
            text = "选择日期 (可选):"
            textSize = 16f
        }
        container.addView(dateLabel)

        // Date button
        val dateButton = android.widget.Button(this).apply {
            text = "全部日期"
            setPadding(0, 10, 0, 0)
            setBackgroundColor(getColor(android.R.color.holo_blue_light))
            setTextColor(getColor(android.R.color.white))
        }

        var selectedDate: String? = null

        dateButton.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            val datePickerDialog = android.app.DatePickerDialog(
                this@MainActivity,
                { _, selectedYear, selectedMonth, dayOfMonth ->
                    selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, dayOfMonth)
                    dateButton.text = "已选择: $selectedDate"
                    dateButton.setBackgroundColor(getColor(android.R.color.holo_green_light))
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        // Clear date button
        val clearDateButton = android.widget.Button(this).apply {
            text = "清除日期"
            setPadding(0, 10, 0, 0)
            setBackgroundColor(getColor(android.R.color.darker_gray))
            setTextColor(getColor(android.R.color.white))
        }
        clearDateButton.setOnClickListener {
            selectedDate = null
            dateButton.text = "全部日期"
            dateButton.setBackgroundColor(getColor(android.R.color.holo_blue_light))
        }

        container.addView(dateButton)
        container.addView(clearDateButton)

        android.app.AlertDialog.Builder(this)
            .setTitle("筛选日期")
            .setView(container)
            .setPositiveButton("下一步") { _, _ ->
                showNumberOfEventsDialog(formatCode, selectedDate)
            }
            .setNegativeButton("返回", null)
            .show()
    }

    private fun showNumberOfEventsDialog(formatCode: String, selectedDate: String?) {
        android.app.AlertDialog.Builder(this)
            .setTitle("赛事数量")
            .setItems(arrayOf("5", "10", "20")) { _, which ->
                val maxEvents = when (which) {
                    0 -> 5
                    1 -> 10
                    2 -> 20
                    else -> 10
                }
                viewModel.startEventScraping(formatCode, selectedDate, maxEvents, 0)
            }
            .setNegativeButton("返回", null)
            .show()
    }

    private fun showDeleteEventDialog(event: MainViewModel.EventItem, _position: Int, viewHolder: RecyclerView.ViewHolder) {
        android.app.AlertDialog.Builder(this)
            .setTitle("删除赛事")
            .setMessage("确定要删除赛事 \"${event.eventName}\" 吗？\n\n这将同时删除该赛事下的所有套牌和卡牌数据。")
            .setPositiveButton("删除") { _, _ ->
                // 用户确认删除
                viewModel.deleteEvent(event.id, event.eventName)
            }
            .setNegativeButton("取消", null)
            .setOnCancelListener {
                // 用户取消，恢复视图
                viewHolder.itemView.alpha = 1f
                viewHolder.itemView.translationX = 0f
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh -> {
                // 刷新当前列表
                when (currentTab) {
                    TAB_EVENT_LIST -> viewModel.loadEvents()
                    TAB_FAVORITES -> viewModel.loadFavoriteDecklists()
                }
                Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_language -> {
                // 切换语言
                lifecycleScope.launch {
                    val newLanguage = languagePreferenceManager.toggleLanguage()
                    val languageName = if (newLanguage == LanguagePreferenceManager.LANGUAGE_CHINESE) {
                        "中文"
                    } else {
                        "English"
                    }

                    // 清除卡牌缓存
                    viewModel.clearCardCache()

                    Toast.makeText(
                        this@MainActivity,
                        "已切换至$languageName，卡牌信息将在下次查看时更新",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }
            R.id.menu_view_logs -> {
                // 查看日志
                val intent = Intent(this, LogViewerActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Group events by date for sectioned adapter
    private fun groupEventsByDate(events: List<Event>): List<EventListItem> {
        val result = mutableListOf<EventListItem>()
        // Group by date
        val grouped = events.groupBy { it.date }
        // Sort dates descending
        val sortedDates = grouped.keys.sortedDescending()
        for (date in sortedDates) {
            result.add(EventListItem.DateHeader(date))
            grouped[date]?.forEach { event ->
                result.add(EventListItem.EventItem(event))
            }
        }
        return result
    }
}
