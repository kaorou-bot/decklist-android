package com.mtgo.decklistmanager.ui.search

import android.os.Bundle
import android.view.MenuItem
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ActivitySearchBinding
import com.mtgo.decklistmanager.databinding.DialogSearchFiltersBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 搜索页面 - 显示搜索结果和搜索历史
 */
@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var binding: ActivitySearchBinding

    private lateinit var resultAdapter: SearchResultAdapter
    private lateinit var historyAdapter: SearchHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.search)

        setupRecyclerViews()
        observeViewModel()
        setupListeners()
    }

    private fun setupRecyclerViews() {
        resultAdapter = SearchResultAdapter { result ->
            // 显示卡牌详情
            showCardDetail(result)
        }
        binding.recyclerViewResults.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = resultAdapter
        }

        historyAdapter = SearchHistoryAdapter(
            onItemClick = { query ->
                viewModel.searchFromHistory(query)
            },
            onDeleteClick = { id ->
                viewModel.deleteSearchHistory(id)
            }
        )
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = historyAdapter
        }
    }

    /**
     * 显示卡牌详情
     */
    private fun showCardDetail(result: SearchResultItem) {
        // TODO: 创建卡牌详情对话框，显示所有 MTGCH 字段
        val message = buildString {
            appendLine("卡牌名称：${result.displayName ?: result.name}")
            appendLine("英文名称：${result.name}")
            appendLine()
            appendLine("类型：${result.typeLine ?: result.type ?: ""}")
            appendLine("法术力：${result.manaCost ?: "N/A"}")
            result.oracleText?.let { appendLine("规则文本：$it") }
            result.power?.let { result.toughness?.let { appendLine("攻防：$it/$it") } }
            result.loyalty?.let { appendLine("忠诚：$it") }
            appendLine()
            appendLine("系列：${result.setName ?: "N/A"}")
            result.collectorNumber?.let { appendLine("编号：$it") }
            result.artist?.let { appendLine("画家：$it") }
            appendLine()
            appendLine("稀有度：${result.rarity ?: "N/A"}")
            result.colorIdentity?.let { appendLine("颜色标识：${it.joinToString()}") }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(result.displayName ?: result.name)
            .setMessage(message)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.searchResults.collect { results ->
                        resultAdapter.submitList(results)
                        binding.textViewEmpty.visibility = if (results.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                    }
                }
                launch {
                    viewModel.searchHistory.collect { history ->
                        historyAdapter.submitList(history)
                    }
                }
                launch {
                    viewModel.isSearching.collect { searching ->
                        binding.progressBar.visibility = if (searching) android.view.View.VISIBLE else android.view.View.GONE
                    }
                }
                launch {
                    viewModel.showHistory.collect { show ->
                        binding.layoutHistory.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
                    }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            android.widget.Toast.makeText(this@SearchActivity, it, android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.editTextQuery.setOnEditorActionListener { _, _, _ ->
            val query = binding.editTextQuery.text?.toString() ?: ""
            viewModel.search(query)
            true
        }
        binding.buttonClearHistory.setOnClickListener {
            viewModel.clearAllHistory()
        }
        binding.buttonFilters.setOnClickListener {
            showFiltersDialog()
        }
    }

    private fun showFiltersDialog() {
        val dialogBinding = DialogSearchFiltersBinding.inflate(LayoutInflater.from(this))
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setTitle("高级筛选")
            .setNegativeButton("取消", null)
            .setPositiveButton("应用") { _, _ ->
                // TODO: 收集筛选条件并构建 SearchFilters 对象
                val filters = SearchFilters()
                // 重新搜索，使用当前查询文本
                val query = binding.editTextQuery.text?.toString() ?: ""
                viewModel.search(query, filters = filters)
            }
            .create()
        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
