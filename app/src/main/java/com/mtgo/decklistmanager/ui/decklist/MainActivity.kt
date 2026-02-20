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
import com.mtgo.decklistmanager.domain.model.Tag
import com.mtgo.decklistmanager.ui.base.BaseActivity
import com.mtgo.decklistmanager.util.AppLogger
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
    private val tagViewModel: com.mtgo.decklistmanager.ui.tag.TagViewModel by viewModels()

    private lateinit var rvDecklists: RecyclerView
    private lateinit var decklistAdapter: DecklistAdapter
    private lateinit var eventAdapter: EventAdapter
    private lateinit var eventSectionAdapter: EventSectionAdapter
    private lateinit var filterBar: View
    private lateinit var formatSelector: View
    private lateinit var tagSelector: View
    private lateinit var dateSelector: View
    private lateinit var bottomNavigation: com.google.android.material.bottomnavigation.BottomNavigationView
    private lateinit var tvCurrentFormat: android.widget.TextView
    private lateinit var tvCurrentTag: android.widget.TextView
    private lateinit var tvCurrentDate: android.widget.TextView

    private var currentTab = TAB_EVENT_LIST
    private var itemTouchHelper: ItemTouchHelper? = null
    private var isProgrammaticNav = false // Flag to prevent listener loops
    private var selectedTag: Tag? = null

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

        // v4.1.0: 一次性修复所有 NULL 的 display_name
        viewModel.fixAllNullDisplayNames()

        // Initial tab selection - don't trigger listener
        isProgrammaticNav = true
        val requestedTab = intent.getStringExtra("tab")
        when (requestedTab) {
            "favorites" -> switchToTab(TAB_FAVORITES)
            else -> switchToTab(TAB_EVENT_LIST)
        }
        isProgrammaticNav = false
    }

    override fun onResume() {
        super.onResume()
        // 刷新赛事列表（当从赛事详情页返回时，可能需要更新赛事的卡组数量）
        if (currentTab == TAB_EVENT_LIST) {
            viewModel.refreshEvents()
        }
    }

    private fun initViews() {
        rvDecklists = findViewById(R.id.rvDecklists)
        progressOverlay = findViewById(R.id.progressOverlay)
        filterBar = findViewById(R.id.filterBar)
        formatSelector = findViewById(R.id.formatSelector)
        tagSelector = findViewById(R.id.tagSelector)
        dateSelector = findViewById(R.id.dateSelector)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        tvCurrentFormat = findViewById(R.id.tvCurrentFormat)
        tvCurrentTag = findViewById(R.id.tvCurrentTag)
        tvCurrentDate = findViewById(R.id.tvCurrentDate)

        // 设置初始筛选器可见性（默认为赛事列表页）
        formatSelector.visibility = View.VISIBLE
        tagSelector.visibility = View.GONE
        dateSelector.visibility = View.VISIBLE
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
                // Show filter bar (download button)
                filterBar.visibility = View.VISIBLE
                // Show format and date selectors, hide tag selector
                formatSelector.visibility = View.VISIBLE
                tagSelector.visibility = View.GONE
                dateSelector.visibility = View.VISIBLE
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
                // Hide filter bar (download button)
                filterBar.visibility = View.GONE
                // Show format and tag selectors, hide date selector
                formatSelector.visibility = View.VISIBLE
                tagSelector.visibility = View.VISIBLE
                dateSelector.visibility = View.GONE
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

        // Tag selector in status bar
        tagSelector.setOnClickListener {
            showTagFilterDialog()
        }

        // Date selector in status bar
        dateSelector.setOnClickListener {
            showDateFilterDialog()
        }
    }

    private fun setupRecyclerView() {
        // Decklist Adapter for favorites tab
        decklistAdapter = DecklistAdapter(
            onItemClick = { decklist ->
                // Open deck detail activity
                val intent = Intent(this, DeckDetailActivity::class.java).apply {
                    putExtra("decklistId", decklist.id)
                }
                startActivity(intent)
            },
            onFavoriteClick = { decklist ->
                // Toggle favorite status
                lifecycleScope.launch {
                    val newState = viewModel.toggleFavorite(decklist.id)
                    Toast.makeText(
                        this@MainActivity,
                        if (newState) "Added to favorites" else "Removed from favorites",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Refresh list to update icon
                    viewModel.loadFavoriteDecklists()
                }
            },
            viewModel = viewModel,
            coroutineScope = this.lifecycleScope
        )

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

            // 添加滚动监听器，用于分页加载
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    // 只在向下滑动时加载更多
                    if (dy > 0 && currentTab == TAB_EVENT_LIST) {
                        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                        if (layoutManager != null) {
                            val visibleItemCount = layoutManager.childCount
                            val totalItemCount = layoutManager.itemCount
                            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                            // 当滚动到距离底部还有 5 个 item 时开始加载
                            if (visibleItemCount + firstVisibleItemPosition + 5 >= totalItemCount) {
                                viewModel.loadMoreEvents()
                            }
                        }
                    }
                }
            })
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
            0 // 初始设置为0，稍后根据当前标签页动态设置
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // 不支持拖拽
            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                // 只在赛事列表标签页启用滑动，收藏页面禁用滑动以允许正常滚动
                return if (currentTab == TAB_EVENT_LIST && rvDecklists.adapter == eventSectionAdapter) {
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                } else {
                    0 // 其他页面不支持滑动，确保可以正常滚动
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 只在赛事列表标签页允许删除
                if (currentTab == TAB_EVENT_LIST && rvDecklists.adapter == eventSectionAdapter) {
                    val position = viewHolder.adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        // v4.0.0: 使用 eventSectionAdapter 获取 item
                        val item = eventSectionAdapter.getItemAtPosition(position)

                        // 只能删除 EventItem，不能删除 DateHeader
                        if (item is com.mtgo.decklistmanager.ui.decklist.EventListItem.EventItem) {
                            val event = item.event

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
                        } else {
                            // 如果是 DateHeader，恢复视图
                            viewHolder.itemView.alpha = 1f
                            viewHolder.itemView.translationX = 0f
                        }
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
                if (currentTab == TAB_EVENT_LIST && rvDecklists.adapter == eventSectionAdapter) {
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

        // 监听加载更多状态，可以在这里添加底部加载提示
        collectFlow(viewModel.isLoadingMore) { isLoading ->
            if (isLoading) {
                // 可以在这里显示底部加载进度条或提示
                AppLogger.d("MainActivity", "Loading more events...")
            }
        }

        // 监听是否还有更多数据
        collectFlow(viewModel.hasMore) { hasMore ->
            AppLogger.d("MainActivity", "Has more events: $hasMore")
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
                // 根据当前tab加载相应数据
                if (currentTab == TAB_FAVORITES) {
                    viewModel.loadFavoriteDecklists()
                } else {
                    viewModel.refreshEvents()  // 筛选时刷新而不是追加
                }
            }
        }
    }

    private fun showTagFilterDialog() {
        lifecycleScope.launch {
            val allTags = tagViewModel.getAllTags()
            if (allTags.isEmpty()) {
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("标签")
                    .setMessage("暂无标签，请先在套牌详情中添加标签")
                    .setPositiveButton("确定", null)
                    .show()
                return@launch
            }

            val tagNames = arrayOf("全部标签") + allTags.map { it.name }.toTypedArray()
            val checkedIndex = if (selectedTag == null) 0 else allTags.indexOfFirst { it.id == selectedTag?.id } + 1

            android.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("选择标签")
                .setSingleChoiceItems(tagNames, checkedIndex) { dialog, which ->
                    if (which == 0) {
                        selectedTag = null
                        tvCurrentTag.text = "标签: 全部"
                        viewModel.applyTagFilter(null)
                    } else {
                        val tag = allTags[which - 1]
                        selectedTag = tag
                        tvCurrentTag.text = "标签: ${tag.name}"
                        viewModel.applyTagFilter(tag.id)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
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
            R.id.menu_settings -> {
                // 打开设置
                val intent = Intent(this, com.mtgo.decklistmanager.ui.settings.SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_search -> {
                // 打开搜索
                val intent = Intent(this, com.mtgo.decklistmanager.ui.search.SearchActivity::class.java)
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
