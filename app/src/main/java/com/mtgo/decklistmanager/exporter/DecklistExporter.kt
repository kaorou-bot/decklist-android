package com.mtgo.decklistmanager.exporter

import com.mtgo.decklistmanager.domain.model.Decklist

/**
 * 套牌导出器接口
 *
 * 定义了将套牌导出为不同格式的标准接口
 */
interface DecklistExporter {
    /**
     * 导出套牌
     * @param decklist 套牌数据
     * @param includeSideboard 是否包含备牌
     * @return 导出的字符串内容
     */
    suspend fun export(
        decklist: Decklist,
        includeSideboard: Boolean = true
    ): String

    /**
     * 获取文件扩展名
     */
    fun getFileExtension(): String

    /**
     * 获取格式名称
     */
    fun getFormatName(): String
}

/**
 * 导出结果
 *
 * @property content 导出的内容
 * @property fileName 文件名
 * @property formatName 格式名称
 * @property fileSize 文件大小（字节）
 */
data class ExportResult(
    val content: String,
    val fileName: String,
    val formatName: String,
    val fileSize: Int
)
