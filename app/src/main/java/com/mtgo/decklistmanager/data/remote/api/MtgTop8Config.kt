package com.mtgo.decklistmanager.data.remote.api

/**
 * MTGTop8 配置常量
 * 集中管理所有 MTGTop8 相关的配置
 */
object MtgTop8Config {

    const val BASE_URL = "https://mtgtop8.com"

    // 格式代码
    object FormatCodes {
        const val STANDARD = "ST"
        const val MODERN = "MO"
        const val LEGACY = "LE"
        const val VINTAGE = "VI"
        const val PAUPER = "PA"
        const val PIONEER = "PI"
        const val COMMANDER = "EDH"
        const val CEDH = "cEDH"
    }

    // 格式显示名称
    object FormatNames {
        const val STANDARD = "Standard"
        const val MODERN = "Modern"
        const val LEGACY = "Legacy"
        const val VINTAGE = "Vintage"
        const val PAUPER = "Pauper"
        const val PIONEER = "Pioneer"
        const val COMMANDER = "Commander"
        const val CEDH = "cEDH"
    }

    // HTML 选择器配置
    object Selectors {
        val EVENT_ROWS = listOf(
            "tr.hover_tr",
            "tr[style*=\"hover\"]",
            "table.Stable tr",
            "tr:has(td)",
            "tr"
        )

        val DECK_CARDS = listOf(
            "div[id^=md]",  // main deck
            "div[id^=sb]"   // sideboard
        )

        const val TABLE_ROWS = "table.Stable tr[align=\"center\"]"
    }

    // 赛事类型关键词
    object EventTypes {
        private val TYPE_MAP = mapOf(
            "Challenge" to "Challenge",
            "League" to "League",
            "Tournament" to "Tournament",
            "Championship" to "Championship",
            "Qualifier" to "Qualifier"
        )

        fun extractEventType(eventName: String): String? {
            for ((keyword, type) in TYPE_MAP) {
                if (eventName.contains(keyword, ignoreCase = true)) {
                    return type
                }
            }
            return null
        }
    }

    // 网络请求配置
    object Network {
        const val TIMEOUT_MS = 45000  // 增加到45秒
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        const val REFERRER = "https://www.google.com"
        const val ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
        const val ACCEPT_LANGUAGE = "en-US,en;q=0.9"
    }

    /**
     * 根据代码获取格式名称
     */
    fun getFormatName(code: String): String {
        return when (code) {
            FormatCodes.STANDARD -> FormatNames.STANDARD
            FormatCodes.MODERN -> FormatNames.MODERN
            FormatCodes.LEGACY -> FormatNames.LEGACY
            FormatCodes.VINTAGE -> FormatNames.VINTAGE
            FormatCodes.PAUPER -> FormatNames.PAUPER
            FormatCodes.PIONEER -> FormatNames.PIONEER
            FormatCodes.COMMANDER -> FormatNames.COMMANDER
            FormatCodes.CEDH -> FormatNames.CEDH
            else -> code
        }
    }
}
