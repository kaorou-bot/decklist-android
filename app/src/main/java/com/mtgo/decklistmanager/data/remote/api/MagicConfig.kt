package com.mtgo.decklistmanager.data.remote.api

/**
 * Magic.gg 配置常量
 * 集中管理所有 Magic.gg 相关的配置
 */
object MagicConfig {

    const val BASE_URL = "https://magic.gg"

    // MTGO Champions Showcase 赛事 URL
    val SHOWCASE_URLS = listOf(
        "https://magic.gg/decklists/2026-magic-online-champions-showcase-season-1-modern-decklists",
        "https://magic.gg/decklists/2025-magic-online-champions-showcase-season-3-modern-decklists",
        "https://magic.gg/decklists/2025-magic-online-champions-showcase-season-2-modern-decklists",
        "https://magic.gg/decklists/2025-magic-online-champions-showcase-season-1-modern-decklists"
    )

    // HTML 选择器
    object Selectors {
        const val DECK_LIST = "deck-list"
        const val MAIN_DECK = "main-deck"
        const val SIDEBOARD = "side-board"
    }

    // 网络请求配置
    object Network {
        const val TIMEOUT_MS = 30000
        const val USER_AGENT = "Mozilla/5.0 (Android 13; Mobile)"
    }

    // 日期格式化
    object DateFormat {
        private val MONTHS = mapOf(
            "January" to "01",
            "February" to "02",
            "March" to "03",
            "April" to "04",
            "May" to "05",
            "June" to "06",
            "July" to "07",
            "August" to "08",
            "September" to "09",
            "October" to "10",
            "November" to "11",
            "December" to "12"
        )

        /**
         * 将 "January 11, 2026" 格式转换为 "2026-01-11"
         */
        fun formatDate(eventDate: String): String {
            return try {
                val parts = eventDate.replace(",", "").split(" ")
                if (parts.size >= 3) {
                    val year = parts[2]
                    val month = MONTHS[parts[0]] ?: "01"
                    val day = parts[1].padStart(2, '0')
                    "$year-$month-$day"
                } else {
                    eventDate
                }
            } catch (e: Exception) {
                eventDate
            }
        }
    }
}
