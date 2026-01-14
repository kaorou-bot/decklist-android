package com.mtgo.decklistmanager.ui.decklist

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.domain.model.Decklist
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Main Activity - 主界面
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var rvDecklists: RecyclerView
    private lateinit var decklistAdapter: DecklistAdapter
    private lateinit var tvStatus: MaterialTextView
    private lateinit var progressOverlay: View
    private lateinit var etSearch: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        // Initial load
        viewModel.loadDecklists()
    }

    private fun initViews() {
        rvDecklists = findViewById(R.id.rvDecklists)
        tvStatus = findViewById(R.id.tvStatus)
        progressOverlay = findViewById(R.id.progressOverlay)
        etSearch = findViewById(R.id.etSearch)
    }

    private fun setupRecyclerView() {
        decklistAdapter = DecklistAdapter { decklist ->
            // Open deck detail activity
            val intent = Intent(this, DeckDetailActivity::class.java).apply {
                putExtra("decklistId", decklist.id)
            }
            startActivity(intent)
        }

        rvDecklists.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = decklistAdapter
        }
    }

    private fun setupObservers() {
        // Observe decklists
        viewModel.decklists.observe(this) { items ->
            val decklists = items.map { item ->
                com.mtgo.decklistmanager.domain.model.Decklist(
                    id = item.id,
                    eventName = item.eventName,
                    eventType = null, // Not available in DecklistItem
                    format = item.format,
                    date = item.date,
                    url = "", // Not available in DecklistItem
                    playerName = item.playerName,
                    playerId = null, // Not available in DecklistItem
                    record = item.record
                )
            }
            decklistAdapter.submitList(decklists)
        }

        // Observe UI state to control progress overlay
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is MainViewModel.UiState.Loading,
                    is MainViewModel.UiState.Scraping -> {
                        progressOverlay.visibility = View.VISIBLE
                    }
                    else -> {
                        progressOverlay.visibility = View.GONE
                    }
                }
            }
        }

        // Observe statistics using StateFlow collect
        lifecycleScope.launch {
            viewModel.statistics.collect { stats ->
                stats?.let {
                    updateStatusBar(it)
                }
            }
        }

        // Observe status messages using StateFlow collect
        lifecycleScope.launch {
            viewModel.statusMessage.collect { message ->
                message?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearStatusMessage()
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Search button
        findViewById<MaterialButton>(R.id.btnSearch).setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                viewModel.searchDecklists(query)
            } else {
                viewModel.loadDecklists()
            }
        }

        // Search on enter key
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.searchDecklists(query)
                } else {
                    viewModel.loadDecklists()
                }
                true
            } else {
                false
            }
        }

        // Refresh button
        findViewById<MaterialButton>(R.id.btnRefresh).setOnClickListener {
            viewModel.loadDecklists()
        }

        // Format filter button
        findViewById<MaterialButton>(R.id.btnFilterFormat).setOnClickListener {
            showFormatFilterDialog()
        }

        // Date filter button
        findViewById<MaterialButton>(R.id.btnFilterDate).setOnClickListener {
            showDateFilterDialog()
        }

        // Scraping button - 打开爬取对话框
        findViewById<MaterialButton>(R.id.btnScraping).setOnClickListener {
            showScrapingOptionsDialog()
        }

        // Statistics button
        findViewById<MaterialButton>(R.id.btnStats).setOnClickListener {
            viewModel.loadStatistics()
        }

        // Clear data button
        findViewById<MaterialButton>(R.id.btnClear).setOnClickListener {
            showClearDataConfirmation()
        }
    }

    private fun showFormatFilterDialog() {
        lifecycleScope.launch {
            viewModel.availableFormats.collect { formats ->
                if (formats.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No formats available. Add test data first.", Toast.LENGTH_SHORT).show()
                    return@collect
                }

                runOnUiThread {
                    val items = arrayOf("All Formats") + formats.toTypedArray()

                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Select Format")
                        .setSingleChoiceItems(items, -1) { dialog, which ->
                            val selected = if (which == 0) null else formats[which - 1]
                            viewModel.applyFormatFilter(selected)
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
    }

    private fun showDateFilterDialog() {
        lifecycleScope.launch {
            viewModel.availableDates.collect { dates ->
                if (dates.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No dates available. Add test data first.", Toast.LENGTH_SHORT).show()
                    return@collect
                }

                runOnUiThread {
                    val items = arrayOf("All Dates") + dates.toTypedArray()

                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Select Date")
                        .setSingleChoiceItems(items, -1) { dialog, which ->
                            val selected = if (which == 0) null else dates[which - 1]
                            viewModel.applyDateFilter(selected)
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
    }

    private fun showScrapingOptionsDialog() {
        // MTGTop8 格式选项
        val formatOptions = arrayOf("Modern", "Standard", "Pioneer", "Legacy", "Pauper", "Vintage")

        val builder = android.app.AlertDialog.Builder(this)
            .setTitle("Scrape from MTGTop8")
            .setMessage("Select format, date range, and number of decklists to scrape.\n\n• MTGTop8: Large tournament database\n• Multiple formats supported")

        // 创建自定义视图
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        // 格式选择
        val formatLabel = android.widget.TextView(this).apply {
            text = "Format:"
            textSize = 16f
        }
        val formatSpinner = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                formatOptions
            )
        }

        // 日期选择按钮
        val dateLabel = android.widget.TextView(this).apply {
            text = "Date (Optional):"
            textSize = 16f
            setPadding(0, 20, 0, 0)
        }
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
                this@MainActivity,
                { _, selectedYear, selectedMonth, dayOfMonth ->
                    selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, dayOfMonth)
                    dateButton.text = "Selected: $selectedDate"
                    dateButton.setBackgroundColor(getColor(android.R.color.holo_green_light))
                },
                year,   // Current year
                month,  // Current month
                day     // Current day
            )
            datePickerDialog.show()
        }

        // 清除日期按钮
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

        // 最大牌组数
        val maxDecksLabel = android.widget.TextView(this).apply {
            text = "Max Decks (1-50):"
            textSize = 16f
            setPadding(0, 20, 0, 0)
        }
        val maxDecksInput = android.widget.EditText(this).apply {
            setText("10")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(0, 10, 0, 0)
        }

        container.addView(formatLabel)
        container.addView(formatSpinner)
        container.addView(dateLabel)
        container.addView(dateButton)
        container.addView(clearDateButton)
        container.addView(maxDecksLabel)
        container.addView(maxDecksInput)

        builder.setView(container)

        builder.setPositiveButton("Start Scraping") { dialog, _ ->
            val format = when (formatSpinner.selectedItemPosition) {
                0 -> "MO"  // Modern
                1 -> "ST"  // Standard
                2 -> "PI"  // Pioneer
                3 -> "LE"  // Legacy
                4 -> "PA"  // Pauper
                5 -> "VI"  // Vintage
                else -> "MO"
            }
            val maxDecks = maxDecksInput.text.toString().toIntOrNull()?.coerceIn(1, 50) ?: 10

            // 使用 MTGTop8 爬取
            startMtgTop8Scraping(format, selectedDate, maxDecks)
            dialog.dismiss()
        }
        .setNegativeButton("Cancel", null)

        builder.show()
    }

    private fun startMtgTop8Scraping(format: String, date: String?, maxDecks: Int) {
        // 显示进度对话框
        val progressDialog = android.app.AlertDialog.Builder(this)
            .setTitle("Scraping from MTGTop8")
            .setMessage("Fetching decklists...\n\nFormat: ${format}\n${if (date != null) "Date: $date\n" else ""}Max decks: $maxDecks\n\nPlease wait.\n\nThis may take a few minutes.")
            .setCancelable(false)
            .create()

        progressDialog.show()

        lifecycleScope.launch {
            viewModel.startMtgTop8Scraping(format, date, maxDecks)

            // 观察UI状态变化
            viewModel.uiState.collect { state ->
                when (state) {
                    is MainViewModel.UiState.Loading,
                    is MainViewModel.UiState.Scraping -> {
                        // 继续显示进度
                    }
                    is MainViewModel.UiState.Success -> {
                        progressDialog.dismiss()
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            state.message,
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    is MainViewModel.UiState.Error -> {
                        progressDialog.dismiss()
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Error: ${state.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        progressDialog.dismiss()
                    }
                }
            }
        }
    }

    private fun showClearDataConfirmation() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Clear All Data")
            .setMessage("Are you sure you want to delete all decklists and cards?")
            .setPositiveButton("Clear") { _, _ ->
                viewModel.clearData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateStatusBar(stats: com.mtgo.decklistmanager.domain.model.Statistics) {
        tvStatus.text = "Decklists: ${stats.totalDecklists} | Cards: ${stats.totalCards} | Formats: ${stats.totalFormats}"
    }
}
