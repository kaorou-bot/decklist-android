package com.mtgo.decklistmanager.ui.search

import com.google.gson.Gson
import com.mtgo.decklistmanager.data.local.dao.SearchHistoryDao
import com.mtgo.decklistmanager.data.local.entity.SearchHistoryEntity
import com.mtgo.decklistmanager.data.remote.api.mtgch.*
import com.mtgo.decklistmanager.ui.search.model.SearchFilters
import java.io.IOException
import java.lang.reflect.Proxy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = FakeApi()
    private lateinit var history: SearchHistoryDao
    private lateinit var model: SearchViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        history = Proxy.newProxyInstance(SearchHistoryDao::class.java.classLoader,
            arrayOf(SearchHistoryDao::class.java)) { _, method, _ ->
            when (method.name) {
                "getRecentSearchHistory" -> flowOf(emptyList<SearchHistoryEntity>())
                "insert" -> 1L
                "deleteOldSearchHistory" -> Unit
                else -> error("Unexpected DAO call: ${method.name}")
            }
        } as SearchHistoryDao
        model = SearchViewModel(api, history)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun restoredQueryAndFiltersRunTogether() = runTest(dispatcher) {
        val filters = SearchFilters(type = "Creature", colors = listOf("W"))
        val state = androidx.lifecycle.SavedStateHandle(mapOf(
            "query" to "Solitude", "filters" to Gson().toJson(filters)
        ))
        val restored = SearchViewModel(api, history, state)
        advanceUntilIdle()
        assertEquals("Solitude", restored.searchQuery.value)
        assertEquals(filters, restored.activeFilters)
        assertTrue(restored.uiState.value is SearchViewModel.UiState.Results)
        assertEquals(1, api.calls)
        restored.updateSearchQuery("")
        assertEquals("", state.get<String>("query"))
    }

    @Test fun initialAndBlankQueryDoNotShowNoResultsOrCallApi() = runTest(dispatcher) {
        assertSame(SearchViewModel.UiState.Initial, model.uiState.value)
        model.search("  ")
        advanceUntilIdle()
        assertSame(SearchViewModel.UiState.Initial, model.uiState.value)
        assertEquals(0, api.calls)
    }

    @Test fun noResultsAppearsOnlyAfterSuccessfulResponse() = runTest(dispatcher) {
        api.answer = { delay(100); response() }
        model.search("unknown")
        runCurrent()
        assertSame(SearchViewModel.UiState.Loading, model.uiState.value)
        advanceUntilIdle()
        assertEquals(emptyList<Any>(), (model.uiState.value as SearchViewModel.UiState.Results).items)
    }

    @Test fun failurePersistsAndRetryCanSucceed() = runTest(dispatcher) {
        api.answer = { throw IOException("offline") }
        model.search("Solitude")
        advanceUntilIdle()
        assertTrue(model.uiState.value is SearchViewModel.UiState.Error)
        api.answer = { response("Solitude") }
        model.search("Solitude")
        assertSame(SearchViewModel.UiState.Loading, model.uiState.value)
        advanceUntilIdle()
        assertEquals("Solitude", (model.uiState.value as SearchViewModel.UiState.Results).items.single().name)
    }

    @Test fun clearingInputCancelsPendingResult() = runTest(dispatcher) {
        api.answer = { withContext(NonCancellable) { delay(100); response("Old") } }
        model.search("Old")
        runCurrent()
        model.updateSearchQuery("")
        model.search("")
        advanceUntilIdle()
        assertSame(SearchViewModel.UiState.Initial, model.uiState.value)
    }

    @Test fun slowOldResponseCannotOverwriteNewQuery() = runTest(dispatcher) {
        api.answer = { query ->
            if (query == "Old") withContext(NonCancellable) { delay(200); response("Old") }
            else response("New")
        }
        model.search("Old")
        runCurrent()
        model.updateSearchQuery("New")
        model.search("New")
        advanceUntilIdle()
        assertEquals("New", (model.uiState.value as SearchViewModel.UiState.Results).items.single().name)
    }

    @Test fun blankQueryWithFiltersStillSearches() = runTest(dispatcher) {
        model.search("", filters = SearchFilters(type = "creature"))
        advanceUntilIdle()
        assertEquals(1, api.calls)
        assertTrue(model.uiState.value is SearchViewModel.UiState.Results)
    }

    private class FakeApi : MtgchApi by unusedApi() {
        var calls = 0
        var answer: suspend (String) -> Response<MtgchSearchResponse> = { response() }
        override suspend fun searchCard(query: String, limit: Int?, offset: Int?, unique: String?,
            color: String?, cmc: String?, type: String?, rarity: String?, set: String?
        ): Response<MtgchSearchResponse> {
            calls++
            return answer(query)
        }
    }

    companion object {
        private fun unusedApi() = Proxy.newProxyInstance(MtgchApi::class.java.classLoader,
            arrayOf(MtgchApi::class.java)) { _, method, _ -> error("Unexpected API call: ${method.name}") } as MtgchApi

        private fun response(name: String? = null): Response<MtgchSearchResponse> {
            val cards = if (name == null) emptyList() else listOf(Gson().fromJson(
                """{"name":"$name","oracle_id":"test-id"}""", MtgchCardDto::class.java))
            return Response.success(MtgchSearchResponse(true, cards, cards.size))
        }
    }
}
