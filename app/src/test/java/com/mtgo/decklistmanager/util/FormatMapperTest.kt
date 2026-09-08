package com.mtgo.decklistmanager.util

import org.junit.Assert.*
import org.junit.Test

class FormatMapperTest {
    @Test fun chineseNamesRoundTripToExistingCodes() {
        FormatMapper.allFormatNames.forEach { name ->
            val code = FormatMapper.nameToCode(name)
            assertNotNull(code)
            assertEquals(name, FormatMapper.codeToName(code!!))
        }
    }
    @Test fun handlesHistoricalEnglishNamesAndUnknownFormats() {
        assertEquals("摩登", FormatMapper.codeToName("Modern"))
        assertEquals("薪传", FormatMapper.codeToName("LE"))
        assertEquals("MO", FormatMapper.nameToCode("摩登"))
        assertEquals("MO", FormatMapper.nameToCode("modern"))
        assertEquals("指挥官", FormatMapper.codeToName("Commander"))
        assertEquals("Custom", FormatMapper.codeToName("Custom"))
        assertNull(FormatMapper.nameToCode("Custom"))
    }
}
