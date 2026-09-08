package com.mtgo.decklistmanager.util

/**
 * MTGTop8 Format 映射
 * 将格式代码（code）和格式名称（name）进行互相转换
 *
 * 注意：所有参数都来自MTGTop8网站的实际URL参数
 * 参考网址：https://mtgtop8.com/
 */
object FormatMapper {

    /**
     * 所有支持的格式映射
     * Key: 格式名称（用于UI显示）
     * Value: 格式代码（用于URL和数据库存储）
     *
     * 注意：这里使用 name → code 的映射，方便按name查找
     */
    private val formatMap = mapOf(
        "Modern" to "MO",
        "Standard" to "ST",
        "Legacy" to "LE",
        "Vintage" to "VI",
        "Pauper" to "PAU",
        "Pioneer" to "PI",
        "Historic" to "HI",
        "Alchemy" to "ALCH",
        "Block" to "BL",
        "Explorer" to "EXP",
        "Highlander" to "HIGH",
        "Peasant" to "PEA",
        "Premodern" to "PREM",
        "cEDH" to "cEDH",
        "EDH" to "EDH",
        "Limited" to "format_limited"
    )

    private val chineseNames = mapOf(
        "MO" to "摩登", "ST" to "标准", "LE" to "薪传", "VI" to "特选",
        "PAU" to "纯普", "PI" to "先驱", "HI" to "史迹", "ALCH" to "炼金",
        "BL" to "环境构筑", "EXP" to "探索", "HIGH" to "高地人",
        "PEA" to "农民", "PREM" to "前摩登", "cEDH" to "竞技指挥官",
        "EDH" to "指挥官", "format_limited" to "限制"
    )

    /**
     * 所有支持的格式名称列表（按常用程度排序）
     */
    val allFormatNames = listOf(
        "Modern",
        "Standard",
        "Legacy",
        "Pauper",
        "Pioneer",
        "Vintage",
        "Historic",
        "Alchemy",
        "Premodern",
        "Explorer",
        "Block",
        "Highlander",
        "Peasant",
        "cEDH",
        "EDH",
        "Limited"
    ).map(::codeToName)

    /**
     * 将格式名称转换为格式代码
     * @param name 格式名称（如 "Modern"）
     * @return 格式代码（如 "MO"），如果未找到则返回null
     */
    fun nameToCode(name: String): String? {
        val value = name.trim()
        return chineseNames.entries.find { it.value == value }?.key
            ?: formatMap.entries.find { it.key.equals(value, true) || it.value.equals(value, true) }?.value
            ?: if (value.equals("Commander", true)) "EDH" else null
    }

    /**
     * 将格式代码转换为格式名称
     * @param code 格式代码（如 "MO"）
     * @return 格式名称（如 "Modern"），如果未找到则返回原代码
     */
    fun codeToName(code: String): String {
        return nameToCode(code)?.let { chineseNames[it] } ?: code
    }

    /**
     * 获取所有格式名称（包含已有数据的格式优先显示）
     * @param existingCodes 数据库中已存在的格式代码列表
     * @return 格式名称列表（有数据的在前）
     */
    fun getAllFormatNamesSorted(existingCodes: List<String>): List<String> {
        return allFormatNames.sortedBy { name ->
            val code = nameToCode(name)
            if (existingCodes.contains(code)) 0 else 1
        }
    }

    /**
     * 检查格式代码是否有效
     * @param code 格式代码
     * @return 是否有效
     */
    fun isValidCode(code: String): Boolean {
        return formatMap.values.contains(code)
    }

    /**
     * 检查格式名称是否有效
     * @param name 格式名称
     * @return 是否有效
     */
    fun isValidName(name: String): Boolean {
        return nameToCode(name) != null
    }
}
