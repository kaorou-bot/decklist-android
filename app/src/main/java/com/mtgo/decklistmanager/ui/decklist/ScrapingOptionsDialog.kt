@file:Suppress("unused")
package com.mtgo.decklistmanager.ui.decklist

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.DialogScrapingOptionsBinding

/**
 * Scraping Options Dialog - 爬取选项对话框
 */
class ScrapingOptionsDialog : DialogFragment() {

    private var _binding: DialogScrapingOptionsBinding? = null
    private val binding get() = _binding!!

    private var formats: List<String> = emptyList()
    private var dates: List<String> = emptyList()
    private var onScrapingStart: ((formatFilter: String?, dateFilter: String?, maxDecks: Int) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogScrapingOptionsBinding.inflate(LayoutInflater.from(requireContext()))

        setupViews()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.scraping_options)
            .setView(binding.root)
            .setPositiveButton(R.string.start_scraping) { _, _ ->
                startScraping()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    private fun setupViews() {
        // Setup format spinner
        // TODO: Setup spinner adapter
        val formatItems = arrayOf("All Formats") + formats.toTypedArray()

        // Setup date spinner
        // TODO: Setup spinner adapter
        val dateItems = arrayOf("All Dates") + dates.toTypedArray()

        // Setup max decks input
        binding.etMaxDecks.setText("5")
    }

    private fun startScraping() {
        val formatFilter = null // TODO: Get from spinner
        val dateFilter = null // TODO: Get from spinner
        val maxDecks = binding.etMaxDecks.text.toString().toIntOrNull() ?: 5

        onScrapingStart?.invoke(formatFilter, dateFilter, maxDecks)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            formats: List<String>,
            dates: List<String>,
            onScrapingStart: (formatFilter: String?, dateFilter: String?, maxDecks: Int) -> Unit
        ): ScrapingOptionsDialog {
            return ScrapingOptionsDialog().apply {
                this.formats = formats
                this.dates = dates
                this.onScrapingStart = onScrapingStart
            }
        }
    }
}
