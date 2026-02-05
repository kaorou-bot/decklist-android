package com.mtgo.decklistmanager.ui.analysis

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.tabs.TabLayoutMediator
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ActivityDeckAnalysisBinding
import com.mtgo.decklistmanager.domain.model.ManaColor
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 套牌分析 Activity
 * v4.2.0: 显示套牌的法术力曲线、颜色分布、类型分布
 */
@AndroidEntryPoint
class DeckAnalysisActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DECKLIST_ID = "decklist_id"
        const val EXTRA_DECKLIST_NAME = "decklist_name"
    }

    private lateinit var binding: ActivityDeckAnalysisBinding
    private lateinit var viewModel: DeckAnalysisViewModel

    private var decklistId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeckAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取套牌 ID
        decklistId = intent.getLongExtra(EXTRA_DECKLIST_ID, 0L)
        if (decklistId == 0L) {
            finish()
            return
        }

        // 设置 Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 初始化 ViewModel
        viewModel = ViewModelProvider(this)[DeckAnalysisViewModel::class.java]

        // 设置 ViewPager 和 TabLayout
        setupViewPager()

        // 加载数据
        loadData()
    }

    private fun setupViewPager() {
        val adapter = DeckAnalysisPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_mana_curve)
                1 -> getString(R.string.tab_color_distribution)
                2 -> getString(R.string.tab_type_distribution)
                else -> ""
            }
        }.attach()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                viewModel.loadAnalysis(decklistId)
                viewModel.analysis.observe(this@DeckAnalysisActivity) { analysis ->
                    analysis?.let { updateUI(it) }
                }
            } catch (e: Exception) {
                AppLogger.e("DeckAnalysisActivity", "Failed to load analysis: ${e.message}", e)
                finish()
            }
        }
    }

    private fun updateUI(analysis: com.mtgo.decklistmanager.domain.model.DeckAnalysis) {
        // 更新套牌名称
        binding.tvDeckName.text = analysis.decklistName

        // 更新统计摘要
        binding.tvMainDeckCount.text = analysis.statistics.mainDeckCount.toString()
        binding.tvSideboardCount.text = analysis.statistics.sideboardCount.toString()
        binding.tvAverageMana.text = String.format("%.2f", analysis.statistics.averageManaValue)
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
