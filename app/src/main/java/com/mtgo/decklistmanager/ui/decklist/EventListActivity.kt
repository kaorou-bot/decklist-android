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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Event List Activity - 赛事列表界面（一级）
 */
@AndroidEntryPoint
class EventListActivity : AppCompatActivity() {

    private val viewModel: EventListViewModel by viewModels()

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
                    sourceUrl = null,
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
            val formats = viewModel.availableFormats.value ?: emptyList()
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
            val dates = viewModel.availableDates.value ?: emptyList()
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
        // Show scraping options dialog
        val formats = arrayOf("Modern", "Standard", "Legacy", "Vintage", "Pauper", "Pioneer")
        var selectedFormat = "Modern"

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Scrape Events from MTGTop8")
            .setSingleChoiceItems(formats, 0) { _, which ->
                selectedFormat = formats[which]
            }
            .setPositiveButton("Start") { _, _ ->
                val formatCode = when (selectedFormat) {
                    "Modern" -> "MO"
                    "Standard" -> "ST"
                    "Legacy" -> "LE"
                    "Vintage" -> "VI"
                    "Pauper" -> "PA"
                    "Pioneer" -> "PI"
                    else -> "MO"
                }

                android.app.AlertDialog.Builder(this)
                    .setTitle("Number of Events")
                    .setItems(arrayOf("5", "10", "20")) { _, which ->
                        val maxEvents = when (which) {
                            0 -> 5
                            1 -> 10
                            2 -> 20
                            else -> 10
                        }
                        viewModel.startEventScraping(formatCode, null, maxEvents)
                    }
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }
}
