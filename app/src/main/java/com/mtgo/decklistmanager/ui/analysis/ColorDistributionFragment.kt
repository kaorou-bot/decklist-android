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
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.FragmentColorDistributionBinding
import com.mtgo.decklistmanager.domain.model.DeckAnalysis
import com.mtgo.decklistmanager.domain.model.ManaColor

/**
 * 自定义值格式化器 - 显示"颜色名称 数量"
 */
class ColorLabelFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        // value 是 PieEntry 的值，即数量
        // 我们需要从 PieEntry 的 data 中获取标签
        return value.toInt().toString()
    }
}

/**
 * 颜色分布图表 Fragment
 */
class ColorDistributionFragment : Fragment() {

    private var _binding: FragmentColorDistributionBinding? = null
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
        _binding = FragmentColorDistributionBinding.inflate(inflater, container, false)
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
        viewModel.sideboard.observe(viewLifecycleOwner) { sideboard ->
            setSideboardMode(sideboard)
        }
        viewModel.analysis.observe(viewLifecycleOwner) { analysis ->
            analysis?.let {
                currentAnalysis = it
                setupChart(it)
            }
        }
    }

    private fun setupChart(analysis: DeckAnalysis) {
        if (_binding == null) return
        val chart = binding.pieChart

        // 准备数据 - 根据当前模式选择数据源
        val (colors, colorsByCard) = if (isSideboardMode) {
            analysis.colorDistribution.sideboardColors to analysis.colorDistribution.sideboardColorsByCard
        } else {
            analysis.colorDistribution.colors to analysis.colorDistribution.colorsByCard
        }

        val selectedColors = if (isByQuantity) colors else colorsByCard
        val entries = ArrayList<PieEntry>()
        val colorValues = ArrayList<Int>()

        ManaColor.entries.forEach { manaColor ->
            val count = selectedColors[manaColor] ?: 0
            if (count > 0) {
                // 使用标签作为 data，以便在格式化器中获取
                val label = "${manaColor.displayName} ${count}"
                entries.add(PieEntry(count.toFloat(), label))
                colorValues.add(getColor(manaColor))
            }
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colorValues
        dataSet.sliceSpace = 3f
        // 文字说明由独立图例提供。
        dataSet.setDrawValues(false)

        val pieData = PieData(dataSet)
        chart.data = pieData

        // 设置格式化器
        chart.setUsePercentValues(false)  // 不使用百分比
        chart.setDrawEntryLabels(false)
        chart.setEntryLabelTextSize(14f)
        chart.setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        chart.description.isEnabled = false
        chart.centerText = "颜色分布"
        chart.setCenterTextSize(18f)
        chart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        chart.setHoleColor(ContextCompat.getColor(requireContext(), R.color.card_background))
        chart.holeRadius = 66f
        chart.transparentCircleRadius = 70f
        chart.centerText = "${selectedColors.values.sum()}\n${if (isByQuantity) "张非地牌" else "种非地牌"}"
        chart.setCenterTextSize(16f)
        chart.isRotationEnabled = false
        chart.legend.isEnabled = false
        chart.setExtraOffsets(4f, 4f, 4f, 4f)
        renderColorLegend(selectedColors)

        chart.animateY(300)
        chart.invalidate()
    }

    private fun renderColorLegend(colors: Map<ManaColor, Int>) {
        val context = requireContext()
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        binding.colorLegend.removeAllViews()
        ManaColor.entries.forEach { color ->
            val count = colors[color] ?: 0
            val row = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(4), dp(8), dp(4), dp(8))
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                }
            }
            row.addView(View(context).apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(getColor(color))
                    setStroke(dp(1), ContextCompat.getColor(context, R.color.outline))
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(dp(12), dp(12)).apply {
                    marginEnd = dp(8)
                }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            row.addView(android.widget.TextView(context).apply {
                text = "${color.displayName}  ${count}${if (isByQuantity) "张" else "种"}"
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            })
            binding.colorLegend.addView(row)
        }
    }

    private fun getColor(manaColor: ManaColor): Int {
        return when (manaColor) {
            ManaColor.WHITE -> android.graphics.Color.parseColor("#E8D8A0")
            ManaColor.BLUE -> android.graphics.Color.parseColor("#0E68AB")
            ManaColor.BLACK -> android.graphics.Color.parseColor("#150B00")
            ManaColor.RED -> android.graphics.Color.parseColor("#D3202A")
            ManaColor.GREEN -> android.graphics.Color.parseColor("#00733E")
            ManaColor.COLORLESS -> android.graphics.Color.parseColor("#9E9E9E")
            ManaColor.MULTICOLOR -> android.graphics.Color.parseColor("#FFD700")  // 金色
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
