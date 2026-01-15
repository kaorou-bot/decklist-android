package com.mtgo.decklistmanager.data.constants

/**
 * 应用常量配置
 * 集中管理应用中使用的各种常量
 */
object AppConstants {

    // 爬取相关常量
    object Scraping {
        const val REQUEST_DELAY_MS = 2000L
        const val API_RATE_LIMIT_DELAY_MS = 100L

        const val MAX_DECKS_MIN = 1
        const val MAX_DECKS_MAX = 50
        const val MAX_DECKS_DEFAULT = 10

        const val MAX_EVENTS_DEFAULT = 10
    }

    // Scryfall API 配置
    object Scryfall {
        const val BASE_URL = "https://api.scryfall.com/"
        const val RATE_LIMIT_DELAY_MS = 100L
    }

    // 数据库配置
    object Database {
        const val DATABASE_NAME = "decklist_database"
        const val DATABASE_VERSION = 2
    }

    // UI 相关常量
    object UI {
        const val TOAST_DURATION_SHORT = 2000
        const val TOAST_DURATION_LONG = 3500
    }

    // 日期格式
    object DateFormat {
        const val DATE_FORMAT_DISPLAY = "yyyy-MM-dd"
        const val DATE_FORMAT_STORAGE = "yyyy-MM-dd"
    }
}
