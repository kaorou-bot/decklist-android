package com.mtgo.decklistmanager.ui.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.HorizontalBarChart
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

        // 获取父 Activity 的 ViewModel
        val viewModel = ViewModelProvider(requireActivity()).get(DeckAnalysisViewModel::class.java)
        viewModel.analysis.observe(viewLifecycleOwner) { analysis ->
            analysis?.let { setupChart(it) }
        }
    }

    private fun setupChart(analysis: DeckAnalysis) {
        val chart = binding.horizontalBarChart

        // 准备数据
        val types = analysis.typeDistribution.types
        val entries = ArrayList<BarEntry>()
        val typeLabels = ArrayList<String>()

        CardType.entries.forEachIndexed { index, cardType ->
            val count = types[cardType] ?: 0
            if (count > 0) {
                entries.add(BarEntry(count.toFloat(), index.toFloat()))
                typeLabels.add(cardType.displayName)
            }
        }

        val dataSet = BarDataSet(entries, "类型分布")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.primary)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

        val barData = BarData(dataSet)
        chart.data = barData

        // 设置 Y 轴标签
        chart.axisLeft.valueFormatter = IndexAxisValueFormatter(typeLabels.toTypedArray())

        chart.axisRight.isEnabled = false
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.isEnabled = false
        chart.description.isEnabled = false

        chart.animateY(1000)
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
