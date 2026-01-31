package com.mtgo.decklistmanager.exporter.format

import com.google.gson.Gson
import com.mtgo.decklistmanager.exporter.DecklistExporter
import com.mtgo.decklistmanager.domain.model.Card
import com.mtgo.decklistmanager.domain.model.Decklist
import com.mtgo.decklistmanager.util.AppLogger
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * MTGA Arena .json 格式导出器
 *
 * MTG Arena 使用的套牌格式
 *
 * 格式示例：
 * ```json
 * {
 *   "name": "套牌名称",
 *   "format": "Standard",
 *   "mainDeck": [
 *     {"id": "12345", "quantity": 4},
 *     {"id": "67890", "quantity": 2}
 *   ],
 *   "sideboard": [
 *     {"id": "11111", "quantity": 2}
 *   ]
 * }
 * ```
 */
@ViewModelScoped
class ArenaFormatExporter @Inject constructor(
    // TODO: 需要 Repository 来获取 Scryfall ID
    // private val repository: DecklistRepository
) : DecklistExporter {

    private val gson = Gson()

    override suspend fun export(
        decklist: Decklist,
        cards: List<Card>,
        includeSideboard: Boolean
    ): String {
        return withContext(Dispatchers.Default) {
            val mainDeck = cards.filter { it.location == com.mtgo.decklistmanager.domain.model.CardLocation.MAIN }
            val sideboard = cards.filter { it.location == com.mtgo.decklistmanager.domain.model.CardLocation.SIDEBOARD }

            val arenaDeck = ArenaDeck(
                name = decklist.eventName ?: "Unknown Deck",
                format = decklist.format ?: "Standard",
                mainDeck = mainDeck.map { card ->
                    ArenaCardEntry(
                        id = getScryfallId(card),
                        quantity = card.quantity
                    )
                },
                sideboard = if (includeSideboard) {
                    sideboard.map { card ->
                        ArenaCardEntry(
                            id = getScryfallId(card),
                            quantity = card.quantity
                        )
                    }
                } else {
                    emptyList()
                }
            )

            gson.toJson(arenaDeck)
        }
    }

    override fun getFileExtension() = "json"

    override fun getFormatName() = "Arena (.json)"

    /**
     * 获取卡牌的 Scryfall ID
     *
     * TODO: 需要从 CardInfo 数据库获取
     * 目前暂时使用卡牌名称作为 ID（不完美但可用）
     */
    private fun getScryfallId(card: Card): String {
        // 临时方案：使用卡牌名称作为 ID
        // 正式实现需要从 card_info 表查询 scryfall_id
        return card.cardName.replace(" ", "-").lowercase()
    }
}

/**
 * Arena 套牌数据模型
 */
data class ArenaDeck(
    val name: String,
    val format: String,
    val mainDeck: List<ArenaCardEntry>,
    val sideboard: List<ArenaCardEntry>
)

/**
 * Arena 卡牌条目
 */
data class ArenaCardEntry(
    val id: String,
    val quantity: Int
)
