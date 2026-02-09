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
import com.mtgo.decklistmanager.databinding.FragmentManaCurveBinding
import com.mtgo.decklistmanager.domain.model.DeckAnalysis

/**
 * 法术力曲线图表 Fragment
 */
class ManaCurveFragment : Fragment() {

    private var _binding: FragmentManaCurveBinding? = null
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
        _binding = FragmentManaCurveBinding.inflate(inflater, container, false)
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
        val (curve, curveByCard) = if (isSideboardMode) {
            analysis.manaCurve.sideboardCurve to analysis.manaCurve.sideboardCurveByCard
        } else {
            analysis.manaCurve.curve to analysis.manaCurve.curveByCard
        }

        val selectedCurve = if (isByQuantity) curve else curveByCard
        val entries = ArrayList<BarEntry>()

        // 0-6+ 法术力值
        for (i in 0..7) {
            val count = selectedCurve[i] ?: 0
            entries.add(BarEntry(i.toFloat(), count.toFloat()))
        }

        val dataSet = BarDataSet(entries, "卡牌数量")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.primary)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

        // 使用整数格式化器
        dataSet.valueFormatter = IntegerFormatter()

        val barData = BarData(dataSet)
        chart.data = barData

        // 设置 X 轴标签
        val labels = arrayOf("0", "1", "2", "3", "4", "5", "6", "6+")
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        chart.xAxis.setDrawGridLines(false)

        // 设置 Y 轴标签颜色
        chart.axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false

        // 设置图例文字颜色
        chart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        chart.legend.textSize = 12f

        chart.description.isEnabled = false
        chart.setFitBars(true)

        chart.animateY(1000)
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
