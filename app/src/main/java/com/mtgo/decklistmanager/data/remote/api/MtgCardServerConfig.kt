package com.mtgo.decklistmanager.data.remote.api

/**
 * MTG Card Server API 配置常量
 *
 * 自有服务端 API 配置
 * Base URL: http://182.92.109.160/
 * API 文档: 见项目根目录 API_DOCUMENTATION.md
 */
object MtgCardServerConfig {

    /**
     * API Base URL
     * 使用自有服务端地址
     */
    const val BASE_URL = "http://182.92.109.160/"

    /**
     * API 版本
     */
    const val API_VERSION = "v1.0.0"

    /**
     * 连接超时时间（毫秒）
     */
    const val CONNECT_TIMEOUT_MS = 30000L

    /**
     * 读取超时时间（毫秒）
     */
    const val READ_TIMEOUT_MS = 30000L

    /**
     * 写入超时时间（毫秒）
     */
    const val WRITE_TIMEOUT_MS = 30000L

    /**
     * 默认每页数量
     */
    const val DEFAULT_PAGE_SIZE = 20

    /**
     * 最大每页数量
     */
    const val MAX_PAGE_SIZE = 100

    /**
     * API 端点路径
     */
    object Endpoints {
        const val SEARCH = "api/result"
        const val RANDOM = "api/random"
        const val CARD_BY_ORACLE_ID = "api/cards/{oracleId}"
        const val CARD_BY_DB_ID = "api/cards/id/{id}"
        const val CARD_BY_NAME = "api/cards"
        const val HEALTH = "api/health"
        const val SETS = "api/sets"
        const val POPULAR = "api/stats/popular"
    }
}
