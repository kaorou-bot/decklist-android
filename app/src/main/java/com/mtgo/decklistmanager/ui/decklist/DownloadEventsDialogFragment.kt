package com.mtgo.decklistmanager.ui.decklist

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mtgo.decklistmanager.util.FormatMapper
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class DownloadEventsDialogFragment : DialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private val formats = arrayOf("Modern", "Standard", "Legacy", "Vintage", "Pauper", "Pioneer", "Historic", "Alchemy", "Premodern").map(com.mtgo.decklistmanager.util.FormatMapper::codeToName).toTypedArray()
    private val counts = arrayOf(5, 10, 20)
    private lateinit var formatInput: Spinner
    private lateinit var countInput: Spinner
    private var selectedDate: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val dp = resources.displayMetrics.density
        val form = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * dp).toInt(), (8 * dp).toInt(), (24 * dp).toInt(), 0)
        }
        fun label(text: String) { form.addView(TextView(context).apply {
            this.text = text
            setPadding(0, (12 * dp).toInt(), 0, (4 * dp).toInt())
        }) }
        fun selector(title: String, values: List<String>): Spinner {
            label(title)
            return Spinner(context).apply {
                contentDescription = title
                minimumHeight = (48 * dp).toInt()
                adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, values)
                form.addView(this, LinearLayout.LayoutParams(-1, -2))
            }
        }
        label("获取赛事列表后，进入赛事可继续获取套牌。")
        formatInput = selector("赛制", formats.toList())
        formatInput.setSelection(savedInstanceState?.getInt("format")
            ?: formats.indexOf(viewModel.selectedFormatName.value).coerceAtLeast(0))
        countInput = selector("最多获取", counts.map { "$it 场赛事" })
        countInput.setSelection(savedInstanceState?.getInt("count") ?: 1)
        selectedDate = savedInstanceState?.getString("date")
        label("日期（可选）")
        val dateButton = MaterialButton(context).apply { text = selectedDate ?: "全部日期" }
        form.addView(dateButton, LinearLayout.LayoutParams(-1, -2))
        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            selectedDate?.split("-")?.map { it.toInt() }?.let { calendar.set(it[0], it[1] - 1, it[2]) }
            DatePickerDialog(context, { _, year, month, day ->
                selectedDate = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day)
                dateButton.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        form.addView(MaterialButton(context, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = "清除日期"
            setOnClickListener { selectedDate = null; dateButton.text = "全部日期" }
        })
        return MaterialAlertDialogBuilder(context)
            .setTitle("获取赛事")
            .setView(ScrollView(context).apply { addView(form) })
            .setNegativeButton("取消", null)
            .setPositiveButton("开始获取") { _, _ ->
                val format = FormatMapper.nameToCode(formats[formatInput.selectedItemPosition]) ?: "MO"
                viewModel.startEventScraping(format, selectedDate, counts[countInput.selectedItemPosition])
            }.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::formatInput.isInitialized) {
            outState.putInt("format", formatInput.selectedItemPosition)
            outState.putInt("count", countInput.selectedItemPosition)
            outState.putString("date", selectedDate)
        }
        super.onSaveInstanceState(outState)
    }
}
