package com.mtgo.decklistmanager.ui.decklist

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

/** Keeps the image inside the viewport while allowing inspection at card size. */
class ZoomableDeckImageView(context: Context) : AppCompatImageView(context) {
    private val transform = Matrix()
    private var zoom = 1f
    private val scales = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoomBy(detector.scaleFactor, detector.focusX, detector.focusY)
            return true
        }
    })
    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean = performClick()
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (zoom > 1.1f) resetZoom() else zoomBy(3f, e.x, e.y)
            return true
        }
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (!scales.isInProgress && e2.pointerCount == 1) {
                transform.postTranslate(-distanceX, -distanceY)
                constrain()
            }
            return true
        }
    })
    init { scaleType = ScaleType.MATRIX; isClickable = true }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (width > 0 && height > 0) resetZoom()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetZoom()
    }
    fun resetZoom() {
        val image = drawable ?: return
        if (width == 0 || height == 0 || image.intrinsicWidth <= 0 || image.intrinsicHeight <= 0) return
        zoom = 1f
        transform.setRectToRect(RectF(0f, 0f, image.intrinsicWidth.toFloat(), image.intrinsicHeight.toFloat()),
            RectF(0f, 0f, width.toFloat(), height.toFloat()), Matrix.ScaleToFit.CENTER)
        imageMatrix = transform
    }
    fun zoomBy(factor: Float, x: Float = width / 2f, y: Float = height / 2f) {
        if (drawable == null) return
        val next = (zoom * factor).coerceIn(1f, 8f)
        transform.postScale(next / zoom, next / zoom, x, y)
        zoom = next
        constrain()
    }
    private fun constrain() {
        val image = drawable ?: return
        val bounds = RectF(0f, 0f, image.intrinsicWidth.toFloat(), image.intrinsicHeight.toFloat())
        transform.mapRect(bounds)
        fun offset(start: Float, end: Float, limit: Float): Float = when {
            end - start < limit -> (limit - start - end) / 2f
            start > 0 -> -start
            end < limit -> limit - end
            else -> 0f
        }
        transform.postTranslate(offset(bounds.left, bounds.right, width.toFloat()), offset(bounds.top, bounds.bottom, height.toFloat()))
        imageMatrix = transform
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scales.onTouchEvent(event)
        gestures.onTouchEvent(event)
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
