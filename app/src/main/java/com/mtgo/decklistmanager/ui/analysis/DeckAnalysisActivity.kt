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
    private var isSideboardMode = false // true: 备牌模式, false: 主牌模式
    private lateinit var currentAnalysis: com.mtgo.decklistmanager.domain.model.DeckAnalysis

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

        // 设置主牌/备牌切换按钮
        setupToggleButtons()

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
                    analysis?.let {
                        currentAnalysis = it
                        updateUI(it)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("DeckAnalysisActivity", "Failed to load analysis: ${e.message}", e)
                finish()
            }
        }
    }

    private fun setupToggleButtons() {
        // 默认选中主牌
        binding.toggleGroup.check(R.id.btnMainDeck)

        binding.btnMainDeck.setOnClickListener {
            isSideboardMode = false
            updateStatsUI()
            notifyFragmentsModeChanged()
        }

        binding.btnSideboard.setOnClickListener {
            isSideboardMode = true
            updateStatsUI()
            notifyFragmentsModeChanged()
        }
    }

    private fun notifyFragmentsModeChanged() {
        // 通知所有 Fragment 更新显示
        val adapter = binding.viewPager.adapter as? DeckAnalysisPagerAdapter
        adapter?.fragmentMap?.values?.forEach { fragment ->
            when (fragment) {
                is ManaCurveFragment -> fragment.setSideboardMode(isSideboardMode)
                is ColorDistributionFragment -> fragment.setSideboardMode(isSideboardMode)
                is TypeDistributionFragment -> fragment.setSideboardMode(isSideboardMode)
            }
        }
    }

    private fun updateUI(analysis: com.mtgo.decklistmanager.domain.model.DeckAnalysis) {
        // 更新套牌名称
        binding.tvDeckName.text = analysis.decklistName

        // 更新统计摘要
        updateStatsUI()
    }

    private fun updateStatsUI() {
        if (!::currentAnalysis.isInitialized) return

        val stats = currentAnalysis.statistics
        if (isSideboardMode) {
            // 显示备牌数据
            binding.tvTotalCount.text = stats.sideboardCount.toString()
            binding.tvLandCount.text = stats.sideboardLandCount.toString()
            binding.tvAverageMana.text = String.format("%.2f", stats.sideboardAverageManaValue)
        } else {
            // 显示主牌数据
            binding.tvTotalCount.text = stats.mainDeckCount.toString()
            binding.tvLandCount.text = stats.landCount.toString()
            binding.tvAverageMana.text = String.format("%.2f", stats.averageManaValue)
        }
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
