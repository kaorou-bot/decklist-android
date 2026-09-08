package com.mtgo.decklistmanager.ui.decklist

/** Numeric placement ordering, without interpreting win/loss records as ranks. */
internal object EventPlacementOrder {
    private val placement = Regex("""^#?\s*([0-9]+)(?:st|nd|rd|th)?$""", RegexOption.IGNORE_CASE)

    fun rank(record: String?): Int = placement.matchEntire(record.orEmpty().trim())
        ?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 } ?: Int.MAX_VALUE
}
