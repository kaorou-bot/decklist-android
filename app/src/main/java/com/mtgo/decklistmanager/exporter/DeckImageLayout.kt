package com.mtgo.decklistmanager.exporter

import com.mtgo.decklistmanager.domain.model.Card
import com.mtgo.decklistmanager.domain.model.CardLocation
import kotlin.math.ceil

internal object DeckImageLayout {
    const val WIDTH = 1600
    const val COLUMNS = 8
    const val MARGIN = 32
    const val CELL_WIDTH = 192
    const val CELL_HEIGHT = 336
    const val HEADER = 190
    const val SECTION = 60
    fun rows(count: Int) = (count + COLUMNS - 1) / COLUMNS
    fun entries(cards: List<Card>, location: CardLocation): List<Card> = cards
        .filter { it.location == location && it.quantity > 0 }
        .groupBy { it.cardName }.values.map { group -> group.first().copy(quantity = group.sumOf { it.quantity }) }
    fun height(mainCount: Int, sideCount: Int) = HEADER + SECTION + rows(mainCount) * CELL_HEIGHT +
        (if (sideCount > 0) SECTION + rows(sideCount) * CELL_HEIGHT else 0) + 64
}
