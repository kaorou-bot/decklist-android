package com.mtgo.decklistmanager.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Bundled notices remain readable without a network connection. */
class OpenSourceLicensesDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val padding = (24 * resources.displayMetrics.density).toInt()
        val text = TextView(context).apply {
            text = context.assets.open("open_source_licenses.txt").bufferedReader().use { it.readText() }
            textSize = 14f
            setTextIsSelectable(true)
            setPadding(padding, padding / 2, padding, padding)
            Linkify.addLinks(this, Linkify.WEB_URLS)
            movementMethod = LinkMovementMethod.getInstance()
        }
        return MaterialAlertDialogBuilder(context)
            .setTitle("开源许可")
            .setView(ScrollView(context).apply { addView(text) })
            .setPositiveButton("关闭", null)
            .create()
    }
}
