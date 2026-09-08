package com.mtgo.decklistmanager.util

import com.google.gson.Gson
import com.mtgo.decklistmanager.data.remote.api.mtgch.toEntity
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto
import org.junit.Assert.*
import org.junit.Test

class CardDetailHelperTest {
    @Test fun blankTranslationsFallBackToEnglish() {
        val dto = Gson().fromJson("""{"name":"Solitude","nameZh":"  ","typeLine":"Creature","typeLineZh":"","oracleText":"Lifelink","oracleTextZh":" "}""", MtgchCardDto::class.java)
        val card = CardDetailHelper.buildCardInfo(dto, displayName = " ", typeLine = "", oracleText = " ")
        assertEquals("Solitude", card.name)
        assertEquals("Creature", card.typeLine)
        assertEquals("Lifelink", card.oracleText)
        assertEquals("Lifelink", dto.toEntity().oracleText)
        assertEquals("Creature", dto.toEntity().typeLine)
    }

    @Test fun absentRulesAndManaStayAbsent() {
        val dto = Gson().fromJson("""{"name":"Plains","typeLine":"Basic Land — Plains"}""", MtgchCardDto::class.java)
        val card = CardDetailHelper.buildCardInfo(dto)
        assertNull(card.oracleText)
        assertTrue(card.manaCost.isNullOrBlank())
    }

    @Test fun nonblankTranslationIsPreferred() {
        val dto = Gson().fromJson("""{"name":"Solitude","nameZh":"孤寂"}""", MtgchCardDto::class.java)
        assertEquals("孤寂", CardDetailHelper.buildCardInfo(dto).name)
    }
}
