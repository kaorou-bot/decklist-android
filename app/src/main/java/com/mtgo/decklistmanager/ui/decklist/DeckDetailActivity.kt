package com.mtgo.decklistmanager.ui.decklist

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ActivityDeckDetailBinding
import com.mtgo.decklistmanager.domain.model.Card
import com.mtgo.decklistmanager.domain.model.CardLocation
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.utils.DecklistExporter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Deck Detail Activity - 牌组详情页面
 */
@AndroidEntryPoint
class DeckDetailActivity : AppCompatActivity() {

    private val viewModel: DeckDetailViewModel by viewModels()
    private lateinit var binding: ActivityDeckDetailBinding
    private lateinit var exporter: DecklistExporter

    private lateinit var mainDeckAdapter: CardAdapter
    private lateinit var sideboardAdapter: CardAdapter

    private var currentDecklist: Decklist? = null
    private var allCards: List<Card> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeckDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exporter = DecklistExporter(this)

        setupToolbar()
        setupRecyclerViews()
        setupObservers()
        loadData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_deck_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_export_text -> {
                showExportDialog("text")
                true
            }
            R.id.action_export_json -> {
                showExportDialog("json")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerViews() {
        mainDeckAdapter = CardAdapter { cardName ->
            showCardInfo(cardName)
        }

        sideboardAdapter = CardAdapter { cardName ->
            showCardInfo(cardName)
        }

        // Use WrappedLinearLayoutManager for correct measurement in NestedScrollView
        binding.rvMainDeck.apply {
            layoutManager = WrappedLinearLayoutManager(this@DeckDetailActivity)
            adapter = mainDeckAdapter
            // Important: Set to false when in NestedScrollView
            isNestedScrollingEnabled = false
        }

        binding.rvSideboard.apply {
            layoutManager = WrappedLinearLayoutManager(this@DeckDetailActivity)
            adapter = sideboardAdapter
            // Important: Set to false when in NestedScrollView
            isNestedScrollingEnabled = false
        }
    }

    private fun setupObservers() {
        // Observe decklist
        viewModel.decklist.observe(this) { decklist ->
            decklist?.let {
                currentDecklist = it
                updateDecklistInfo(it)
            }
        }

        // Observe main deck
        viewModel.mainDeck.observe(this) { cards ->
            mainDeckAdapter.submitList(cards)
        }

        // Observe sideboard
        viewModel.sideboard.observe(this) { cards ->
            sideboardAdapter.submitList(cards)
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe card info loading state
        viewModel.isCardInfoLoading.observe(this) { isLoading ->
            if (isLoading) {
                // Show loading toast
                android.widget.Toast.makeText(
                    this,
                    "Loading card info...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Observe card info error
        viewModel.cardInfoError.observe(this) { errorMessage ->
            errorMessage?.let {
                android.widget.Toast.makeText(
                    this,
                    it,
                    android.widget.Toast.LENGTH_LONG
                ).show()
                viewModel.clearCardInfoError()
            }
        }

        // Observe card info for popup
        viewModel.cardInfo.observe(this) { cardInfo ->
            cardInfo?.let {
                showCardInfoDialog(it)
                viewModel.clearCardInfo()
            }
        }
    }

    private fun loadData() {
        viewModel.loadDecklistDetail()
    }

    private fun updateDecklistInfo(decklist: Decklist) {
        binding.apply {
            tvEventName.text = decklist.eventName
            tvFormat.text = "Format: ${decklist.format}"
            tvDate.text = "Date: ${decklist.date}"
            tvPlayer.text = decklist.playerName?.let { "Player: $it" } ?: "Player: N/A"
            tvRecord.text = decklist.record ?: "N/A"
        }
    }

    private fun showCardInfo(cardName: String) {
        viewModel.loadCardInfo(cardName)
    }

    private fun showCardInfoDialog(cardInfo: com.mtgo.decklistmanager.domain.model.CardInfo) {
        // Show card info popup
        CardInfoFragment.newInstance(cardInfo).show(
            supportFragmentManager,
            "card_info"
        )
    }

    private fun showExportDialog(format: String) {
        val decklist = currentDecklist
        if (decklist == null) {
            android.widget.Toast.makeText(this, "No decklist data", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // Collect all cards
        val mainCards = viewModel.mainDeck.value ?: emptyList()
        val sideCards = viewModel.sideboard.value ?: emptyList()
        allCards = mainCards + sideCards

        val formatName = if (format == "text") "Text" else "JSON"

        AlertDialog.Builder(this)
            .setTitle("Export as $formatName")
            .setMessage("Export '${decklist.eventName}' to $formatName format?")
            .setPositiveButton("Export") { _, _ ->
                performExport(format, decklist, allCards)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performExport(format: String, decklist: Decklist, cards: List<Card>) {
        val uri = if (format == "text") {
            exporter.exportToText(decklist, cards)
        } else {
            exporter.exportToJSON(decklist, cards)
        }

        if (uri != null) {
            AlertDialog.Builder(this)
                .setTitle("Export Successful")
                .setMessage("Decklist exported successfully!")
                .setPositiveButton("Share") { _, _ ->
                    exporter.shareFile(uri, if (format == "text") "text/plain" else "application/json")
                }
                .setNegativeButton("Open") { _, _ ->
                    exporter.openFile(uri, if (format == "text") "text/plain" else "application/json")
                }
                .setNeutralButton("Close", null)
                .show()
        } else {
            android.widget.Toast.makeText(this, "Export failed", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
