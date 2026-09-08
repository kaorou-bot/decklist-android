package com.mtgo.decklistmanager.ui.decklist

import org.junit.Assert.assertEquals
import org.junit.Test

class EventPlacementOrderTest {
    @Test fun ordersNumericRanksAndKeepsUnrankedLast() {
        val input = listOf("#10", null, "#2", "#1", "", "#8", "5-0")
        assertEquals(listOf("#1", "#2", "#8", "#10", null, "", "5-0"), input.sortedBy(EventPlacementOrder::rank))
    }

    @Test fun supportsPlainAndOrdinalPlacementsButRejectsInvalidRanks() {
        assertEquals(2, EventPlacementOrder.rank(" 2nd "))
        assertEquals(12, EventPlacementOrder.rank("12"))
        assertEquals(Int.MAX_VALUE, EventPlacementOrder.rank("#0"))
        assertEquals(Int.MAX_VALUE, EventPlacementOrder.rank("#999999999999"))
    }
}
