package com.mtgo.decklistmanager.ui.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.FragmentTypeDistributionBinding
import com.mtgo.decklistmanager.domain.model.CardType
import com.mtgo.decklistmanager.domain.model.DeckAnalysis

/**
 * 类型分布图表 Fragment
 */
class TypeDistributionFragment : Fragment() {

    private var _binding: FragmentTypeDistributionBinding? = null
    private val binding get() = _binding!!

    private var currentAnalysis: DeckAnalysis? = null
    private var isByQuantity = true // true: 按数量, false: 按牌名
    private var isSideboardMode = false // true: 备牌模式, false: 主牌模式

    fun setSideboardMode(isSideboard: Boolean) {
        isSideboardMode = isSideboard
        currentAnalysis?.let { setupChart(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTypeDistributionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置切换按钮
        binding.toggleGroup.check(R.id.btnByQuantity)
        binding.btnByQuantity.setOnClickListener {
            isByQuantity = true
            currentAnalysis?.let { setupChart(it) }
        }
        binding.btnByCardName.setOnClickListener {
            isByQuantity = false
            currentAnalysis?.let { setupChart(it) }
        }

        // 获取父 Activity 的 ViewModel
        val viewModel = ViewModelProvider(requireActivity()).get(DeckAnalysisViewModel::class.java)
        viewModel.analysis.observe(viewLifecycleOwner) { analysis ->
            analysis?.let {
                currentAnalysis = it
                setupChart(it)
            }
        }
    }

    private fun setupChart(analysis: DeckAnalysis) {
        val chart = binding.barChart

        // 准备数据 - 根据当前模式选择数据源
        val (types, typesByCard) = if (isSideboardMode) {
            analysis.typeDistribution.sideboardTypes to analysis.typeDistribution.sideboardTypesByCard
        } else {
            analysis.typeDistribution.types to analysis.typeDistribution.typesByCard
        }

        val selectedTypes = if (isByQuantity) types else typesByCard
        val entries = ArrayList<BarEntry>()
        val typeLabels = ArrayList<String>()

        // 使用连续索引，确保数据对齐
        var currentIndex = 0f
        CardType.entries.forEach { cardType ->
            val count = selectedTypes[cardType] ?: 0
            if (count > 0) {
                entries.add(BarEntry(currentIndex, count.toFloat()))
                typeLabels.add(cardType.displayName)
                currentIndex++
            }
        }

        val dataSet = BarDataSet(entries, "类型分布")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.primary)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

        // 使用整数格式化器
        dataSet.valueFormatter = IntegerFormatter()

        val barData = BarData(dataSet)
        chart.data = barData

        // 设置 X 轴标签（类别）
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(typeLabels.toTypedArray())
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f

        // 设置 Y 轴
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false

        chart.animateY(1000)
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
