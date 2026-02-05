package com.mtgo.decklistmanager.ui.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.FragmentColorDistributionBinding
import com.mtgo.decklistmanager.domain.model.DeckAnalysis
import com.mtgo.decklistmanager.domain.model.ManaColor

/**
 * 颜色分布图表 Fragment
 */
class ColorDistributionFragment : Fragment() {

    private var _binding: FragmentColorDistributionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColorDistributionBinding.inflate(inflater, container, false)
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
        val chart = binding.pieChart

        // 准备数据
        val colors = analysis.colorDistribution.colors
        val entries = ArrayList<PieEntry>()
        val colorValues = ArrayList<Int>()

        ManaColor.entries.forEach { manaColor ->
            val count = colors[manaColor] ?: 0
            if (count > 0) {
                entries.add(PieEntry(count.toFloat(), manaColor.displayName))
                colorValues.add(getColor(manaColor))
            }
        }

        val dataSet = PieDataSet(entries, "颜色分布")
        dataSet.colors = colorValues
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        dataSet.sliceSpace = 3f

        val pieData = PieData(dataSet)
        chart.data = pieData

        // 设置格式化器
        chart.setUsePercentValues(true)
        chart.setDrawEntryLabels(false)
        chart.description.isEnabled = false
        chart.centerText = "颜色分布"
        chart.setCenterTextSize(18f)

        chart.animateY(1000)
        chart.invalidate()
    }

    private fun getColor(manaColor: ManaColor): Int {
        return when (manaColor) {
            ManaColor.WHITE -> android.graphics.Color.parseColor("#E8D8A0")
            ManaColor.BLUE -> android.graphics.Color.parseColor("#0E68AB")
            ManaColor.BLACK -> android.graphics.Color.parseColor("#150B00")
            ManaColor.RED -> android.graphics.Color.parseColor("#D3202A")
            ManaColor.GREEN -> android.graphics.Color.parseColor("#00733E")
            ManaColor.COLORLESS -> android.graphics.Color.parseColor("#9E9E9E")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
