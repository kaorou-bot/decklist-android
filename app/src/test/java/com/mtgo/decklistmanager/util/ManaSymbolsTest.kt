package com.mtgo.decklistmanager.util

import org.junit.Assert.*
import org.junit.Test

class ManaSymbolsTest {
    @Test fun cardCostsResolveIncludingSpecialSymbols() {
        listOf("W", "U", "B", "R", "G", "C", "S", "0", "10", "X", "W/U", "2/B", "G/P", "W/U/P").forEach {
            assertNotNull("Missing symbol: $it", ManaSymbols.resourceFor(it))
        }
    }
    @Test fun reversedHybridAndLowercaseResolve() {
        assertEquals(ManaSymbols.resourceFor("W/U"), ManaSymbols.resourceFor("u/w"))
        assertEquals(ManaSymbols.resourceFor("B"), ManaSymbols.resourceFor("b"))
    }
    @Test fun unknownSymbolsAreNotSubstituted() {
        assertNull(ManaSymbols.resourceFor("UNKNOWN"))
        assertNull(ManaSymbols.resourceFor("999"))
    }
}
