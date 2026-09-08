package com.mtgo.decklistmanager.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ReplacementSpan
import androidx.appcompat.content.res.AppCompatResources
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Replaces braced symbols with bundled artwork without changing the underlying text. */
object ManaSymbolRenderer {
    fun renderManaCost(manaCost: String?, context: Context): CharSequence {
        val text = ManaCosts.normalize(manaCost) ?: return ""
        val result = SpannableStringBuilder(text)
        Regex("\\{([^}]+)\\}").findAll(text).forEach { match ->
            val resource = ManaSymbols.resourceFor(match.groupValues[1]) ?: return@forEach
            val drawable = AppCompatResources.getDrawable(context, resource)?.mutate() ?: return@forEach
            result.setSpan(SymbolSpan(drawable), match.range.first, match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return result
    }

    private class SymbolSpan(private val drawable: Drawable) : ReplacementSpan() {
        private fun height(paint: Paint) = ceil(paint.textSize * 1.05f).toInt()
        private fun width(paint: Paint) = (height(paint) * drawable.intrinsicWidth.toFloat()
            / drawable.intrinsicHeight).roundToInt()

        override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int,
            fm: Paint.FontMetricsInt?): Int {
            val metrics = paint.fontMetricsInt
            val top = (metrics.ascent + metrics.descent - height(paint)) / 2
            fm?.let {
                it.ascent = minOf(metrics.ascent, top)
                it.descent = maxOf(metrics.descent, top + height(paint))
                it.top = minOf(metrics.top, it.ascent)
                it.bottom = maxOf(metrics.bottom, it.descent)
            }
            return width(paint) + ceil(paint.textSize * 0.08f).toInt()
        }

        override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int,
            x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
            val metrics = paint.fontMetricsInt
            val offset = y + (metrics.ascent + metrics.descent - height(paint)) / 2f
            drawable.setBounds(0, 0, width(paint), height(paint))
            val checkpoint = canvas.save()
            canvas.translate(x, offset)
            drawable.draw(canvas)
            canvas.restoreToCount(checkpoint)
        }
    }
}
