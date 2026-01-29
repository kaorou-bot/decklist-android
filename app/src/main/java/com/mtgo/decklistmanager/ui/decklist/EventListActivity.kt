package com.mtgo.decklistmanager.ui.decklist

import android.content.Intent
import android.os.Bundle
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
import com.mtgo.decklistmanager.domain.model.Event
import com.mtgo.decklistmanager.data.remote.api.MtgTop8Scraper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Event List Activity - 赛事列表界面（一级）
 */
@AndroidEntryPoint
class EventListActivity : AppCompatActivity() {

    private val viewModel: EventListViewModel by viewModels()

    @Inject
    lateinit var mtgTop8Scraper: MtgTop8Scraper

    private lateinit var rvEvents: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private lateinit var tvStatus: MaterialTextView
    private lateinit var progressOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_list)

        initViews()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // Initial load
        viewModel.loadEvents()
    }

    private fun initViews() {
        rvEvents = findViewById(R.id.rvEvents)
        tvStatus = findViewById(R.id.tvStatus)
        progressOverlay = findViewById(R.id.progressOverlay)
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            // Open event detail activity
            val intent = Intent(this, EventDetailActivity::class.java).apply {
                putExtra("eventId", event.id)
            }
            startActivity(intent)
        }

        rvEvents.apply {
            layoutManager = LinearLayoutManager(this@EventListActivity)
            adapter = eventAdapter
        }
    }

    private fun setupObservers() {
        // Observe events
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
            eventAdapter.submitList(events)
        }

        // Observe UI state to control progress overlay
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is EventListViewModel.UiState.Loading,
                    is EventListViewModel.UiState.Scraping -> {
                        progressOverlay.visibility = View.VISIBLE
                    }
                    else -> {
                        progressOverlay.visibility = View.GONE
                    }
                }
            }
        }

        // Observe status messages
        lifecycleScope.launch {
            viewModel.statusMessage.collect { message ->
                message?.let {
                    tvStatus.text = "Status: $it"
                    Toast.makeText(this@EventListActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearStatusMessage()
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Refresh button
        findViewById<MaterialButton>(R.id.btnRefresh).setOnClickListener {
            viewModel.loadEvents()
        }

        // Filter format button
        findViewById<MaterialButton>(R.id.btnFilterFormat).setOnClickListener {
            showFormatFilterDialog()
        }

        // Filter date button
        findViewById<MaterialButton>(R.id.btnFilterDate).setOnClickListener {
            showDateFilterDialog()
        }

        // Scraping button
        findViewById<MaterialButton>(R.id.btnScraping).setOnClickListener {
            showScrapingDialog()
        }
    }

    private fun showFormatFilterDialog() {
        lifecycleScope.launch {
            val formats = viewModel.availableFormats.value
            if (formats.isEmpty()) {
                Toast.makeText(this@EventListActivity, "No formats available", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val items = arrayOf("All Formats") + formats.toTypedArray()
            android.app.AlertDialog.Builder(this@EventListActivity)
                .setTitle("Select Format")
                .setItems(items) { _, which ->
                    viewModel.applyFormatFilter(items[which])
                }
                .show()
        }
    }

    private fun showDateFilterDialog() {
        lifecycleScope.launch {
            val dates = viewModel.availableDates.value
            if (dates.isEmpty()) {
                Toast.makeText(this@EventListActivity, "No dates available", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val items = arrayOf("All Dates") + dates.toTypedArray()
            android.app.AlertDialog.Builder(this@EventListActivity)
                .setTitle("Select Date")
                .setItems(items) { _, which ->
                    viewModel.applyDateFilter(items[which])
                }
                .show()
        }
    }

    private fun showScrapingDialog() {
        // 显示下载选项对话框
        val formats = arrayOf("Modern", "Standard", "Legacy", "Vintage", "Pauper", "Pioneer", "Historic", "Alchemy", "Premodern")
        var selectedFormat = "Modern"

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("下载比赛列表")
            .setMessage("选择赛制下载比赛列表。下载后点击比赛可以查看卡组。\n\n如果下载失败，请点击测试连接查看详细信息。")
            .setSingleChoiceItems(formats, 0) { _, which ->
                selectedFormat = formats[which]
            }
            .setPositiveButton("下一步") { _: android.content.DialogInterface, _: Int ->
                // 使用FormatMapper转换format name到code
                val formatCode = com.mtgo.decklistmanager.util.FormatMapper.nameToCode(selectedFormat) ?: "MO"
                // 日期选择对话框
                showDateSelectionDialog(formatCode)
            }
            .setNeutralButton("测试连接") { _: android.content.DialogInterface, _: Int ->
                // 测试连接 - 使用当前选择的格式
                val formatCode = com.mtgo.decklistmanager.util.FormatMapper.nameToCode(selectedFormat) ?: "MO"
                testConnectionAndShowResult(formatCode, selectedFormat)
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()
    }

    private fun testConnectionAndShowResult(formatCode: String, formatName: String) {
        lifecycleScope.launch {
            tvStatus.text = "正在测试连接 $formatName..."
            progressOverlay.visibility = View.VISIBLE

            val result = mtgTop8Scraper.testConnection(formatCode)

            progressOverlay.visibility = View.GONE
            tvStatus.text = "状态: 就绪"

            android.app.AlertDialog.Builder(this@EventListActivity)
                .setTitle("连接测试结果 ($formatName)")
                .setMessage(result)
                .setPositiveButton("确定", null)
                .show()
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
            text = "Select Date (Optional):"
            textSize = 16f
        }
        container.addView(dateLabel)

        // Date button
        val dateButton = android.widget.Button(this).apply {
            text = "All Dates"
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
                this@EventListActivity,
                { _, selectedYear, selectedMonth, dayOfMonth ->
                    selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, dayOfMonth)
                    dateButton.text = "Selected: $selectedDate"
                    dateButton.setBackgroundColor(getColor(android.R.color.holo_green_light))
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        // Clear date button
        val clearDateButton = android.widget.Button(this).apply {
            text = "Clear Date"
            setPadding(0, 10, 0, 0)
            setBackgroundColor(getColor(android.R.color.darker_gray))
            setTextColor(getColor(android.R.color.white))
        }
        clearDateButton.setOnClickListener {
            selectedDate = null
            dateButton.text = "All Dates"
            dateButton.setBackgroundColor(getColor(android.R.color.holo_blue_light))
        }

        container.addView(dateButton)
        container.addView(clearDateButton)

        android.app.AlertDialog.Builder(this)
            .setTitle("Filter by Date")
            .setView(container)
            .setPositiveButton("Next") { _, _ ->
                showNumberOfEventsDialog(formatCode, selectedDate)
            }
            .setNegativeButton("Back", null)
            .show()
    }

    private fun showNumberOfEventsDialog(formatCode: String, selectedDate: String?) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Number of Events")
            .setItems(arrayOf("5", "10", "20")) { _, which ->
                val maxEvents = when (which) {
                    0 -> 5
                    1 -> 10
                    2 -> 20
                    else -> 10
                }
                viewModel.startEventScraping(formatCode, selectedDate, maxEvents, 0)
            }
            .setNegativeButton("Back", null)
            .show()
    }
}
