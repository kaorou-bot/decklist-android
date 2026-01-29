package com.mtgo.decklistmanager.data.local.database

import androidx.room.TypeConverter

/**
 * Room Type Converters - 类型转换器
 */
class Converters {

    /**
     * 字符串列表转字符串（逗号分隔）
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    /**
     * 字符串转字符串列表
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }
    }
}
