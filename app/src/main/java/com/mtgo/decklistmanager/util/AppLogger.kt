package com.mtgo.decklistmanager.util

import android.util.Log

/**
 * 日志工具类
 * 根据构建配置自动控制日志输出
 */
object AppLogger {

    private const val DEFAULT_TAG = "DecklistManager"

    // 生产环境中设置为 false 禁用详细日志
    var isDebugMode: Boolean = true

    /**
     * Debug 级别日志
     */
    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugMode) {
            Log.d(tag, message)
        }
    }

    /**
     * Info 级别日志
     */
    fun i(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugMode) {
            Log.i(tag, message)
        }
    }

    /**
     * Warning 级别日志（始终显示）
     */
    fun w(tag: String = DEFAULT_TAG, message: String) {
        Log.w(tag, message)
    }

    /**
     * Warning 级别日志（始终显示）
     */
    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable) {
        Log.w(tag, message, throwable)
    }

    /**
     * Error 级别日志（始终显示）
     */
    fun e(tag: String = DEFAULT_TAG, message: String) {
        Log.e(tag, message)
    }

    /**
     * Error 级别日志（始终显示）
     */
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }

    /**
     * Verbose 级别日志
     */
    fun v(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugMode) {
            Log.v(tag, message)
        }
    }

    /**
     * 分隔线日志
     */
    fun separator(tag: String = DEFAULT_TAG, title: String = "") {
        if (isDebugMode) {
            val line = "=".repeat(50)
            d(tag, line)
            if (title.isNotEmpty()) {
                d(tag, "  $title")
                d(tag, line)
            }
        }
    }
}
