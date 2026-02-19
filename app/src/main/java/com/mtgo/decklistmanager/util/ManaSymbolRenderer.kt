package com.mtgo.decklistmanager.util

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.graphics.Color
import android.graphics.Typeface
import android.text.style.StyleSpan

/**
 * 法术力值符号渲染工具
 * 将 "{2}{U}{B}" 等格式转换为带颜色的符号
 */
object ManaSymbolRenderer {

    /**
     * 将法术力值字符串转换为带颜色的 SpannableString
     */
    fun renderManaCost(manaCost: String?, context: Context): CharSequence {
        if (manaCost.isNullOrEmpty()) return ""

        // 添加日志来调试 split card
        if (manaCost.contains(" // ")) {
            android.util.Log.d("ManaSymbolRenderer", "Rendering split card manaCost: $manaCost")
        }

        val builder = SpannableStringBuilder()

        // 查找所有 {} 包围的符号
        val pattern = Regex("\\{([^}]+)\\}")
        var lastIndex = 0

        val matches = pattern.findAll(manaCost).toList()
        if (manaCost.contains(" // ")) {
            android.util.Log.d("ManaSymbolRenderer", "Found ${matches.size} matches")
        }

        matches.forEach { match ->
            // 添加符号之前的文本（如果有）
            if (match.range.first > lastIndex) {
                builder.append(manaCost.substring(lastIndex, match.range.first))
            }

            val symbol = match.groupValues[1]
            val startIndex = builder.length

            // 添加符号文本
            when (symbol.uppercase()) {
                "W" -> builder.append("W")  // 白色
                "U" -> builder.append("U")  // 蓝色
                "B" -> builder.append("B")  // 黑色
                "R" -> builder.append("R")  // 红色
                "G" -> builder.append("G")  // 绿色
                "C" -> builder.append("C")  // 无色
                "X" -> builder.append("X")  // X
                "Y" -> builder.append("Y")  // Y
                "Z" -> builder.append("Z")  // Z
                "W/U", "U/W" -> builder.append("W/U")  // 白蓝双色
                "U/B", "B/U" -> builder.append("U/B")  // 蓝黑双色
                "B/R", "R/B" -> builder.append("B/R")  // 黑红双色
                "R/G", "G/R" -> builder.append("R/G")  // 红绿双色
                "G/W", "W/G" -> builder.append("G/W")  // 绿白双色
                "W/B", "B/W" -> builder.append("W/B")  // 白黑双色
                "U/R", "R/U" -> builder.append("U/R")  // 蓝红双色
                "B/G", "G/B" -> builder.append("B/G")  // 黑绿双色
                "R/W", "W/R" -> builder.append("R/W")  // 红白双色
                "G/U", "U/G" -> builder.append("G/U")  // 绿蓝双色
                "2/W" -> builder.append("2/W")  // 半白
                "2/U" -> builder.append("2/U")  // 半蓝
                "2/B" -> builder.append("2/B")  // 半黑
                "2/R" -> builder.append("2/R")  // 半红
                "2/G" -> builder.append("2/G")  // 半绿
                "P" -> builder.append("P")  // 费用
                "S" -> builder.append("S")  // 蔓生
                "E" -> builder.append("E")  // 能量
                "T" -> builder.append("T")  // 点击
                "Q" -> builder.append("Q")  // 反点击
                else -> {
                    // 数字
                    if (symbol.toIntOrNull() != null) {
                        builder.append(symbol)
                    } else {
                        // 其他情况，添加原始符号
                        builder.append(symbol)
                    }
                }
            }

            val endIndex = builder.length

            // 设置颜色
            val color = getManaColor(symbol)
            builder.setSpan(
                ForegroundColorSpan(color),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // 设置粗体
            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            lastIndex = match.range.last + 1
        }

        // 添加剩余文本
        if (lastIndex < manaCost.length) {
            val remaining = manaCost.substring(lastIndex)
            if (manaCost.contains(" // ")) {
                android.util.Log.d("ManaSymbolRenderer", "Remaining text: '$remaining'")
            }
            builder.append(remaining)
        }

        val result = builder.toString()
        if (manaCost.contains(" // ")) {
            android.util.Log.d("ManaSymbolRenderer", "Final result: '$result'")
        }

        return builder
    }

    /**
     * 根据法术力符号获取对应颜色
     * 优化深色模式下的可见性
     */
    private fun getManaColor(symbol: String): Int {
        return when (symbol.uppercase()) {
            "W" -> Color.parseColor("#E8D8A0")  // 白色 - 加深，更易读
            "U" -> Color.parseColor("#0E68AB")  // 蓝色
            "B" -> Color.parseColor("#6B6B6B")  // 黑色 - 改为灰色，在深色模式下可见
            "R" -> Color.parseColor("#D3202A")  // 红色
            "G" -> Color.parseColor("#00733E")  // 绿色
            "C" -> Color.parseColor("#9E9E9E")  // 无色
            "X" -> Color.parseColor("#9E9E9E")  // X
            "W/U", "U/W" -> Color.parseColor("#B3CDE0")  // 白蓝混合
            "U/B", "B/U" -> Color.parseColor("#264678")  // 蓝黑混合
            "B/R", "R/B" -> Color.parseColor("#6E3237")  // 黑红混合
            "R/G", "G/R" -> Color.parseColor("#8B3A3A")  // 红绿混合
            "G/W", "W/G" -> Color.parseColor("#7CBA6C")  // 绿白混合
            "W/B", "B/W" -> Color.parseColor("#8C8C8C")  // 白黑混合
            "U/R", "R/U" -> Color.parseColor("#7A9A9A")  // 蓝红混合
            "B/G", "G/B" -> Color.parseColor("#3A5A3A")  // 黑绿混合
            "R/W", "W/R" -> Color.parseColor("#E5A5A5")  // 红白混合
            "G/U", "U/G" -> Color.parseColor("#4A9A9A")  // 绿蓝混合
            "2/W", "2/U", "2/B", "2/R", "2/G" -> Color.parseColor("#CCCCCC")  // 混合费用
            "P" -> Color.parseColor("#CCA43A")  // 费用
            "S" -> Color.parseColor("#339933")  // 蔓生
            "E" -> Color.parseColor("#FFD700")  // 能量
            else -> {
                // 如果是数字，使用灰色
                if (symbol.toIntOrNull() != null) {
                    Color.parseColor("#9E9E9E")
                } else {
                    Color.parseColor("#CCCCCC")
                }
            }
        }
    }

    /**
     * 简化版本：只渲染基本符号，不使用颜色
     * 用于不支持 Spannable 的场景
     */
    fun renderManaCostSimple(manaCost: String?): String {
        if (manaCost.isNullOrEmpty()) return ""

        return manaCost.replace(Regex("\\{([^}]+)\\")) { matchResult ->
            val symbol = matchResult.groupValues[1]
            when (symbol.uppercase()) {
                "W/U", "U/W" -> "◊"
                "U/B", "B/U" -> "◊"
                "B/R", "R/B" -> "◊"
                "R/G", "G/R" -> "◊"
                "G/W", "W/G" -> "◊"
                "W/B", "B/W" -> "◊"
                "U/R", "R/U" -> "◊"
                "B/G", "G/B" -> "◊"
                "R/W", "W/R" -> "◊"
                "G/U", "U/G" -> "◊"
                "2/W", "2/U", "2/B", "2/R", "2/G" -> "◊"
                "T" -> "T"
                "Q" -> "Q"
                "E" -> "E"
                "P" -> "P"
                "S" -> "S"
                else -> {
                    // 数字或单字母
                    if (symbol.length == 1 || symbol.toIntOrNull() != null) {
                        symbol
                    } else {
                        "◊"
                    }
                }
            }
        }
    }
}
