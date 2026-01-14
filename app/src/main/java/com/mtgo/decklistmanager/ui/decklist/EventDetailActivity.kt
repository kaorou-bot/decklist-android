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
import com.google.android.material.textview.MaterialTextView
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.domain.model.Event
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Event Detail Activity - 赛事详情界面（二级：显示该赛事下的所有卡组）
 */
@AndroidEntryPoint
class EventDetailActivity : AppCompatActivity() {

    private val viewModel: EventDetailViewModel by viewModels()

    private lateinit var rvDecklists: RecyclerView
    private lateinit var decklistAdapter: DecklistAdapter
    private lateinit var tvEventName: MaterialTextView
    private lateinit var tvEventType: MaterialTextView
    private lateinit var tvFormat: MaterialTextView
    private lateinit var tvDate: MaterialTextView
    private lateinit var tvDeckCount: MaterialTextView
    private lateinit var tvSource: MaterialTextView
    private lateinit var tvStatus: MaterialTextView
    private lateinit var progressOverlay: View

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
        setupObservers()

        // Load event detail
        viewModel.loadEventDetail(eventId)
    }

    private fun initViews() {
        rvDecklists = findViewById(R.id.rvDecklists)
        tvEventName = findViewById(R.id.tvEventName)
        tvEventType = findViewById(R.id.tvEventType)
        tvFormat = findViewById(R.id.tvFormat)
        tvDate = findViewById(R.id.tvDate)
        tvDeckCount = findViewById(R.id.tvDeckCount)
        tvSource = findViewById(R.id.tvSource)
        tvStatus = findViewById(R.id.tvStatus)
        progressOverlay = findViewById(R.id.progressOverlay)
    }

    private fun setupRecyclerView() {
        decklistAdapter = DecklistAdapter { decklist ->
            // Open deck detail activity
            val intent = Intent(this, com.mtgo.decklistmanager.ui.decklist.DeckDetailActivity::class.java).apply {
                putExtra("decklistId", decklist.id)
                putExtra("eventId", eventId)  // Pass eventId for navigation
            }
            startActivity(intent)
        }

        rvDecklists.apply {
            layoutManager = LinearLayoutManager(this@EventDetailActivity)
            adapter = decklistAdapter
        }
    }

    private fun setupObservers() {
        // Observe event
        viewModel.event.observe(this) { event ->
            event?.let {
                tvEventName.text = it.eventName
                tvEventType.text = it.eventType ?: "N/A"
                tvFormat.text = it.format
                tvDate.text = it.date
                tvDeckCount.text = "${it.deckCount} Decks"
                tvSource.text = "Source: ${it.source}"
            }
        }

        // Observe decklists
        viewModel.decklists.observe(this) { items ->
            val decklists = items.map { item ->
                Decklist(
                    id = item.id,
                    eventName = item.eventName,
                    eventType = null,
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

        // Observe UI state to control progress overlay
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is EventDetailViewModel.UiState.Loading -> {
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
                    Toast.makeText(this@EventDetailActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearStatusMessage()
                }
            }
        }
    }
}
