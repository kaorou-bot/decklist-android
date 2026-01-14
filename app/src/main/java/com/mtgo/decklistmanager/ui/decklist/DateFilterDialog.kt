package com.mtgo.decklistmanager.ui.decklist

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mtgo.decklistmanager.R

/**
 * Date Filter Dialog - 日期筛选对话框
 */
class DateFilterDialog : DialogFragment() {

    private var dates: List<String> = emptyList()
    private var selectedDate: String? = null
    private var onDateSelected: ((String?) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arrayOf("All Dates") + dates.toTypedArray()
        val checkedItem = if (selectedDate == null) 0 else dates.indexOf(selectedDate) + 1

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_date)
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                val selected = if (which == 0) null else dates[which - 1]
                onDateSelected?.invoke(selected)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    companion object {
        fun newInstance(
            dates: List<String>,
            selectedDate: String?,
            onDateSelected: (String?) -> Unit
        ): DateFilterDialog {
            return DateFilterDialog().apply {
                this.dates = dates
                this.selectedDate = selectedDate
                this.onDateSelected = onDateSelected
            }
        }
    }
}
