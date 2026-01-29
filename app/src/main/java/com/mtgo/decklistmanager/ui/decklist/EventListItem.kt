package com.mtgo.decklistmanager.ui.decklist

import com.mtgo.decklistmanager.domain.model.Event

/**
 * 日期分组赛事列表项
 */
sealed class EventListItem {
    data class DateHeader(val date: String) : EventListItem()
    data class EventItem(val event: Event) : EventListItem()
}
