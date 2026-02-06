package com.mtgo.decklistmanager.ui.analysis

import com.github.mikephil.charting.formatter.ValueFormatter

/**
 * 整数格式化器 - 显示整数而非小数
 */
class IntegerFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return value.toInt().toString()
    }
}
