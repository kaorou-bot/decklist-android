package com.mtgo.decklistmanager.util

import android.util.Log
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MTGCH API 调试工具
 */
@Singleton
class MtgchApiDebugger @Inject constructor(
    private val mtgchApi: MtgchApi
) {

    suspend fun testApi(cardName: String) {
        try {
            Log.d("MtgchApiDebugger", "Testing API for card: $cardName")

            val response = mtgchApi.searchCard(
                query = "!$cardName",
                limit = 1
            )

            Log.d("MtgchApiDebugger", "Response successful: ${response.isSuccessful}")
            Log.d("MtgchApiDebugger", "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val searchResponse = response.body()!!
                val results = searchResponse.cards  // 使用 cards 属性
                Log.d("MtgchApiDebugger", "Results is null: ${results == null}")
                Log.d("MtgchApiDebugger", "Results count: ${results?.size ?: "null"}")

                if (results != null && results.isNotEmpty()) {
                    val card = results[0]
                    Log.d("MtgchApiDebugger", "Card found:")
                    Log.d("MtgchApiDebugger", "  - id: ${card.id}")
                    Log.d("MtgchApiDebugger", "  - name: ${card.name}")
                    Log.d("MtgchApiDebugger", "  - nameZh: ${card.nameZh}")
                    Log.d("MtgchApiDebugger", "  - manaCost: ${card.manaCost}")
                    Log.d("MtgchApiDebugger", "  - typeLine: ${card.typeLine}")
                    Log.d("MtgchApiDebugger", "  - typeLineZh: ${card.typeLineZh}")
                    Log.d("MtgchApiDebugger", "  - imageUri: ${card.imageUris?.normal}")
                    Log.d("MtgchApiDebugger", "  - oracleId: ${card.oracleId}")
                } else {
                    Log.w("MtgchApiDebugger", "No results found")
                }
            } else {
                Log.e("MtgchApiDebugger", "Response failed: ${response.message()}")
                response.errorBody()?.let {
                    Log.e("MtgchApiDebugger", "Error body: ${it.string()}")
                }
            }
        } catch (e: Exception) {
            Log.e("MtgchApiDebugger", "Exception: ${e.message}", e)
        }
    }
}
