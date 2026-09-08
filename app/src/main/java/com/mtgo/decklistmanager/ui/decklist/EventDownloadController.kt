package com.mtgo.decklistmanager.ui.decklist

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class EventDownloadRequest(val format: String, val date: String?, val count: Int, val decksPerEvent: Int = 0)

internal sealed class EventDownloadState {
    object Idle : EventDownloadState()
    data class Running(val request: EventDownloadRequest) : EventDownloadState()
    data class Success(val count: Int) : EventDownloadState()
    data class Failure(val message: String) : EventDownloadState()
}

internal class EventDownloadController(
    private val scope: CoroutineScope,
    private val download: suspend (EventDownloadRequest) -> Int
) {
    private val mutableState = MutableStateFlow<EventDownloadState>(EventDownloadState.Idle)
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var lastRequest: EventDownloadRequest? = null

    fun start(request: EventDownloadRequest) {
        if (job?.isActive == true) return
        lastRequest = request
        mutableState.value = EventDownloadState.Running(request)
        job = scope.launch {
            try {
                val count = withTimeout(60_000) { download(request) }
                ensureActive()
                mutableState.value = EventDownloadState.Success(count)
            } catch (e: TimeoutCancellationException) {
                mutableState.value = EventDownloadState.Failure("下载超时，请检查网络或减少赛事数量。")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.value = EventDownloadState.Failure("下载失败，请检查网络后重试。")
            }
        }
    }

    fun retry() { lastRequest?.let(::start) }
}
