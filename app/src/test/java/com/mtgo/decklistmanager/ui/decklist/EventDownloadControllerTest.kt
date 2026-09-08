package com.mtgo.decklistmanager.ui.decklist

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventDownloadControllerTest {
    private val request = EventDownloadRequest("MO", "2026-09-03", 5)

    @Test fun duplicateStartDoesNotDownloadTwice() = runTest {
        var calls = 0
        val controller = EventDownloadController(backgroundScope) { calls++; delay(100); 5 }
        controller.start(request)
        controller.start(request)
        runCurrent()
        assertEquals(1, calls)
        advanceTimeBy(101)
        assertEquals(EventDownloadState.Success(5), controller.state.value)
    }

    @Test fun retryPreservesRequestAndCanSucceed() = runTest {
        val requests = mutableListOf<EventDownloadRequest>()
        val controller = EventDownloadController(backgroundScope) {
            requests.add(it)
            if (requests.size == 1) throw java.io.IOException("offline")
            3
        }
        controller.start(request)
        runCurrent()
        assertTrue(controller.state.value is EventDownloadState.Failure)
        controller.retry()
        runCurrent()
        assertEquals(listOf(request, request), requests)
        assertEquals(EventDownloadState.Success(3), controller.state.value)
    }

    @Test fun timeoutOffersRetryAndZeroRemainsSuccess() = runTest {
        var slow = true
        val controller = EventDownloadController(backgroundScope) { if (slow) delay(70_000); 0 }
        controller.start(request)
        advanceTimeBy(60_001)
        assertTrue(controller.state.value is EventDownloadState.Failure)
        slow = false
        controller.retry()
        runCurrent()
        assertEquals(EventDownloadState.Success(0), controller.state.value)
    }
}
