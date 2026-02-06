package com.mtgo.decklistmanager.ui.analysis

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * 套牌分析 ViewPager Adapter
 */
class DeckAnalysisPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    val fragmentMap = mutableMapOf<Int, Fragment>()

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> ManaCurveFragment()
            1 -> ColorDistributionFragment()
            2 -> TypeDistributionFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
        fragmentMap[position] = fragment
        return fragment
    }
}
