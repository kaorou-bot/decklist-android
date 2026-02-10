package com.mtgo.decklistmanager.ui.comparison

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayoutMediator
import com.mtgo.decklistmanager.databinding.ActivityDeckComparisonBinding
import com.mtgo.decklistmanager.ui.comparison.adapter.CardComparisonAdapter
import com.mtgo.decklistmanager.ui.comparison.adapter.StatComparisonAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * DeckComparisonActivity - 套牌对比页面
 * 用于对比两个套牌之间的差异
 */
@AndroidEntryPoint
class DeckComparisonActivity : AppCompatActivity() {

    private val viewModel: DeckComparisonViewModel by viewModels()
    private lateinit var binding: ActivityDeckComparisonBinding

    private lateinit var statComparisonAdapter: StatComparisonAdapter
    private lateinit var cardComparisonAdapter: CardComparisonAdapter

    private var decklistId1: Long = 0
    private var decklistId2: Long = 0
    private var deckName1: String = ""
    private var deckName2: String = ""

    companion object {
        const val EXTRA_DECKLIST_ID_1 = "decklist_id_1"
        const val EXTRA_DECKLIST_ID_2 = "decklist_id_2"
        const val EXTRA_DECK_NAME_1 = "deck_name_1"
        const val EXTRA_DECK_NAME_2 = "deck_name_2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeckComparisonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取传入的参数
        decklistId1 = intent.getLongExtra(EXTRA_DECKLIST_ID_1, 0)
        decklistId2 = intent.getLongExtra(EXTRA_DECKLIST_ID_2, 0)
        deckName1 = intent.getStringExtra(EXTRA_DECK_NAME_1) ?: "Deck 1"
        deckName2 = intent.getStringExtra(EXTRA_DECK_NAME_2) ?: "Deck 2"

        if (decklistId1 == 0L || decklistId2 == 0L) {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerViews()
        setupObservers()
        loadComparison()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "套牌对比"
    }

    private fun setupRecyclerViews() {
        // 统计对比适配器
        statComparisonAdapter = StatComparisonAdapter()
        binding.recyclerStats.apply {
            layoutManager = LinearLayoutManager(this@DeckComparisonActivity)
            adapter = statComparisonAdapter
        }

        // 卡牌对比适配器
        cardComparisonAdapter = CardComparisonAdapter(deckName1, deckName2)
        binding.recyclerCards.apply {
            layoutManager = LinearLayoutManager(this@DeckComparisonActivity)
            adapter = cardComparisonAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.comparisonResult.collect { result ->
                statComparisonAdapter.submitList(result.statComparisons)
                cardComparisonAdapter.submitList(result.cardComparisons)
            }
        }
    }

    private fun loadComparison() {
        viewModel.compareDecks(decklistId1, decklistId2)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
