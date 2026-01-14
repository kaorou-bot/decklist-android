package com.mtgo.decklistmanager.ui.decklist

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Custom LinearLayoutManager for RecyclerView inside NestedScrollView
 *
 * When RecyclerView is placed inside NestedScrollView with wrap_content height,
 * it needs special handling to measure child items correctly.
 */
class WrappedLinearLayoutManager(context: Context) : LinearLayoutManager(context) {

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            // Handle IndexOutOfBoundsException that can occur with wrap_content
            // in NestedScrollView
        }
    }
}
