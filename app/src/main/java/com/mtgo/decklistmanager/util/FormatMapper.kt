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
    )

    /**
     * 将格式名称转换为格式代码
     * @param name 格式名称（如 "Modern"）
     * @return 格式代码（如 "MO"），如果未找到则返回null
     */
    fun nameToCode(name: String): String? {
        return formatMap[name]
    }

    /**
     * 将格式代码转换为格式名称
     * @param code 格式代码（如 "MO"）
     * @return 格式名称（如 "Modern"），如果未找到则返回原代码
     */
    fun codeToName(code: String): String {
        return formatMap.entries.find { it.value == code }?.key ?: code
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
        return formatMap.containsKey(name)
    }
}
