package com.mtgo.decklistmanager.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce

/**
 * 防抖工具类 - 用于延迟执行操作
 *
 * 使用场景：
 * - 搜索输入防抖（用户停止输入 300ms 后才执行搜索）
 * - 其他需要延迟响应的场景
 *
 * @param delayMillis 延迟时间（毫秒）
 */
class Debouncer(
    private val delayMillis: Long = 300,
    private val scope: CoroutineScope
) {
    private var job: Job? = null
    private val flow = MutableSharedFlow<Unit>()

    init {
        // 监听 flow 并在延迟后执行操作
        scope.launch {
            flow.debounce(delayMillis).collect {
                onDebounce?.invoke()
            }
        }
    }

    private var onDebounce: (() -> Unit)? = null

    /**
     * 设置防抖触发后的回调
     */
    fun setOnDebounceListener(listener: () -> Unit) {
        onDebounce = listener
    }

    /**
     * 触发防抖（会取消之前的倒计时，重新开始）
     */
    fun debounce() {
        job?.cancel()
        job = scope.launch {
            delay(delayMillis)
            onDebounce?.invoke()
        }
    }

    /**
     * 使用 Flow 实现的防抖（更优雅的方式）
     */
    suspend fun emit() {
        flow.emit(Unit)
    }

    /**
     * 取消所有待执行的操作
     */
    fun cancel() {
        job?.cancel()
    }
}
