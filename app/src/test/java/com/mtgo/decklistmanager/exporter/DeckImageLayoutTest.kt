package com.mtgo.decklistmanager.exporter

import com.mtgo.decklistmanager.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class DeckImageLayoutTest {
    private fun card(name: String, count: Int, location: CardLocation) = Card(
        decklistId = 1, cardName = name, quantity = count, location = location,
        manaCost = null, rarity = null, color = null, cardType = null, cardSet = null)
    @Test fun duplicateCopiesMergeWithinEachSectionOnly() {
        val cards = listOf(card("A", 2, CardLocation.MAIN), card("A", 2, CardLocation.MAIN),
            card("A", 1, CardLocation.SIDEBOARD), card("B", 0, CardLocation.MAIN))
        assertEquals(4, DeckImageLayout.entries(cards, CardLocation.MAIN).single().quantity)
        assertEquals(1, DeckImageLayout.entries(cards, CardLocation.SIDEBOARD).single().quantity)
    }
    @Test fun rowBoundariesNeverLoseLastCard() {
        assertEquals(0, DeckImageLayout.rows(0))
        assertEquals(1, DeckImageLayout.rows(8))
        assertEquals(2, DeckImageLayout.rows(9))
    }
    @Test fun sideboardAddsItsOwnHeaderAndRows() {
        assertEquals(DeckImageLayout.SECTION + DeckImageLayout.CELL_HEIGHT,
            DeckImageLayout.height(9, 1) - DeckImageLayout.height(9, 0))
    }
}
