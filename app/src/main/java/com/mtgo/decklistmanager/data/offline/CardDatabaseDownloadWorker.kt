package com.mtgo.decklistmanager.data.offline

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.mtgo.decklistmanager.data.local.database.AppDatabase
import com.mtgo.decklistmanager.data.local.entity.CardInfoEntity
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Worker for importing card database from assets
 */
class CardDatabaseDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "CardDatabaseDownload"
        private const val PROGRESS_UPDATE_INTERVAL = 100
        private const val ASSETS_FILE_NAME = "mtgch_cards.jsonl"

        // Progress keys
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
        const val KEY_CURRENT = "current"
    }

    private val database = AppDatabase.getInstance(applicationContext)
    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始导入卡牌数据库")

            // 导入卡牌数据（带进度），返回成功数量
            val (totalLines, successCount) = importCardsFromReader()

            // 标记数据库已导入
            val prefs = applicationContext.getSharedPreferences("card_database", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("database_downloaded", true).apply()

            Log.d(TAG, "卡牌数据库导入完成")
            Result.success(workDataOf(
                KEY_TOTAL to totalLines,
                KEY_CURRENT to successCount
            ))
        } catch (e: Exception) {
            Log.e(TAG, "导入数据库失败", e)
            Result.failure()
        }
    }

    private suspend fun importCardsFromReader(): Pair<Int, Int> {
        var totalCards = 0
        var successCount = 0
        var errorCount = 0
        val startTime = System.currentTimeMillis()

        // 先统计总行数（用于计算进度）
        Log.d(TAG, "正在统计文件总行数...")
        val inputStream = applicationContext.assets.open(ASSETS_FILE_NAME)
        val totalLines = BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { r ->
            var count = 0
            var line: String?
            while (r.readLine().also { line = it } != null) {
                count++
            }
            count
        }
        Log.d(TAG, "文件总行数: $totalLines")

        // 重新打开文件进行导入
        val importInputStream = applicationContext.assets.open(ASSETS_FILE_NAME)
        val importReader = BufferedReader(InputStreamReader(importInputStream, "UTF-8"))

        var line: String?
        var lineCount = 0

        try {
            // 使用传统的 readLine() 循环，避免 useLines 的潜在问题
            while (importReader.readLine().also { line = it } != null) {
                lineCount++

                if (line.isNullOrBlank()) {
                    continue  // 跳过空行
                }

                totalCards++

                try {
                    val cardDto = gson.fromJson(line, MtgchCardDto::class.java)
                    val entity = mapToEntity(cardDto)

                    database.cardInfoDao().insertOrUpdate(entity)
                    successCount++

                    // 进度报告（每100张报告一次）
                    if (totalCards % PROGRESS_UPDATE_INTERVAL == 0) {
                        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                        val cardsPerSecond = if (elapsedSeconds > 0) totalCards / elapsedSeconds else 0.0
                        val progress = ((totalCards * 100) / totalLines).toInt()

                        Log.d(TAG, "进度: $progress% (已处理 $totalCards 张, 速度: ${cardsPerSecond.toInt()} 张/秒)")

                        try {
                            setProgressAsync(
                                workDataOf(
                                    KEY_PROGRESS to progress,
                                    KEY_CURRENT to totalCards,
                                    KEY_TOTAL to totalLines
                                )
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "更新进度失败: ${e.message}")
                        }
                    }

                } catch (e: Exception) {
                    errorCount++
                    Log.w(TAG, "导入第 $totalCards 张卡牌失败: ${e.javaClass.simpleName} - ${e.message}")
                    if (errorCount <= 10) {  // 只记录前10个错误
                        val currentLine = line
                        if (!currentLine.isNullOrBlank()) {
                            Log.d(TAG, "失败的数据预览: ${currentLine.take(100)}...")
                        }
                    }
                }
            }

            importReader.close()

            val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
            Log.d(TAG, "✅ 导入完成: 总计 $totalCards 张, 成功 $successCount 张, 失败 $errorCount 张, 用时 ${elapsedSeconds.toInt()} 秒")

            return Pair(totalLines, successCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ 导入过程发生严重错误: ${e.javaClass.simpleName} - ${e.message}", e)
            throw e
        }
    }

    private fun mapToEntity(dto: MtgchCardDto): CardInfoEntity {
        // 判断是否是双面牌
        val isDualFaced = when (dto.layout) {
            "transform", "modal_dfc", "reversible_card", "double_faced_token" -> true
            else -> false
        }

        // 提取双面牌的正面和背面名称
        val frontFaceName = if (isDualFaced) {
            when {
                dto.cardFaces != null && dto.cardFaces.isNotEmpty() -> {
                    dto.cardFaces[0].zhName ?: dto.cardFaces[0].name
                }
                dto.otherFaces != null && dto.otherFaces.isNotEmpty() -> {
                    val fullName = dto.otherFaces[0].zhsFaceName ?: dto.otherFaces[0].faceName ?: dto.otherFaces[0].name
                    if (fullName != null && fullName.contains(" // ")) {
                        fullName.substringBefore(" // ").trim()
                    } else {
                        fullName
                    }
                }
                dto.faceName != null && dto.faceName.contains(" // ") -> {
                    dto.faceName.substringBefore(" // ").trim()
                }
                dto.name != null && dto.name.contains(" // ") -> {
                    dto.name.substringBefore(" // ").trim()
                }
                else -> dto.faceName ?: dto.name
            }
        } else null

        val backFaceName = if (isDualFaced) {
            when {
                dto.cardFaces != null && dto.cardFaces.size >= 2 -> {
                    dto.cardFaces[1].zhName ?: dto.cardFaces[1].name
                }
                dto.otherFaces != null && dto.otherFaces.isNotEmpty() -> {
                    val fullName = dto.otherFaces[0].zhsFaceName ?: dto.otherFaces[0].faceName ?: dto.otherFaces[0].name
                    if (fullName != null && fullName.contains(" // ")) {
                        fullName.substringAfter(" // ").trim()
                    } else {
                        null
                    }
                }
                dto.faceName != null && dto.faceName.contains(" // ") -> {
                    dto.faceName.substringAfter(" // ").trim()
                }
                dto.name != null && dto.name.contains(" // ") -> {
                    dto.name.substringAfter(" // ").trim()
                }
                else -> null
            }
        } else null

        // 处理颜色列表
        val colors = if (dto.colors?.isNotEmpty() == true) {
            dto.colors.joinToString(",")
        } else null

        val colorIdentity = if (dto.colorIdentity?.isNotEmpty() == true) {
            dto.colorIdentity.joinToString(",")
        } else null

        // 提取合法性信息
        val legalities = dto.legalities ?: emptyMap()

        // 提取图片 URLs
        val imageUris = dto.imageUris

        // 双面牌的图片
        val frontImageUri: String? = if (isDualFaced && dto.cardFaces?.isNotEmpty() == true) {
            dto.cardFaces[0].imageUris?.large ?: dto.cardFaces[0].imageUris?.normal
        } else if (isDualFaced && dto.otherFaces?.isNotEmpty() == true) {
            dto.otherFaces[0].imageUris?.large ?: dto.otherFaces[0].imageUris?.normal
        } else null

        // 双面牌的背面信息 - 从 cardFaces 或 otherFaces 提取
        val backFaceManaCost: String? = if (isDualFaced) {
            when {
                dto.cardFaces != null && dto.cardFaces.size >= 2 -> {
                    dto.cardFaces[1].manaCost
                }
                dto.otherFaces != null && dto.otherFaces.size >= 2 -> {
                    dto.otherFaces[1].manaCost
                }
                else -> null
            }
        } else null

        val backFaceTypeLine: String? = if (isDualFaced) {
            when {
                dto.cardFaces != null && dto.cardFaces.size >= 2 -> {
                    dto.cardFaces[1].zhTypeLine ?: dto.cardFaces[1].typeLine
                }
                dto.otherFaces != null && dto.otherFaces.size >= 2 -> {
                    dto.otherFaces[1].zhsTypeLine ?: dto.otherFaces[1].typeLine
                }
                else -> null
            }
        } else null

        val backFaceOracleText: String? = if (isDualFaced) {
            when {
                dto.cardFaces != null && dto.cardFaces.size >= 2 -> {
                    dto.cardFaces[1].zhText ?: dto.cardFaces[1].oracleText
                }
                dto.otherFaces != null && dto.otherFaces.size >= 2 -> {
                    dto.otherFaces[1].zhsText ?: dto.otherFaces[1].oracleText
                }
                else -> null
            }
        } else null

        val backImageUri: String? = if (isDualFaced) {
            when {
                dto.cardFaces != null && dto.cardFaces.size >= 2 -> {
                    dto.cardFaces[1].imageUris?.large ?: dto.cardFaces[1].imageUris?.normal
                }
                dto.otherFaces != null && dto.otherFaces.size >= 2 -> {
                    dto.otherFaces[1].imageUris?.large ?: dto.otherFaces[1].imageUris?.normal
                }
                else -> null
            }
        } else null

        // 序列化 card_faces 为 JSON
        val cardFacesJson = if (dto.cardFaces != null) {
            gson.toJson(dto.cardFaces)
        } else null

        return CardInfoEntity(
            id = dto.id ?: "",
            name = dto.zhsName ?: dto.atomicTranslatedName ?: dto.name ?: "",
            enName = dto.name,
            manaCost = dto.manaCost,
            cmc = dto.cmc?.toDouble(),
            typeLine = dto.zhsTypeLine ?: dto.atomicTranslatedType ?: dto.typeLine,
            oracleText = dto.zhsText ?: dto.atomicTranslatedText ?: dto.oracleText,
            colors = colors,
            colorIdentity = colorIdentity,
            power = dto.power,
            toughness = dto.toughness,
            loyalty = dto.loyalty,
            rarity = dto.rarity,
            setCode = dto.setCode,
            setName = dto.setName,
            artist = dto.artist,
            cardNumber = dto.collectorNumber,
            legalStandard = legalities["standard"],
            legalModern = legalities["modern"],
            legalPioneer = legalities["pioneer"],
            legalLegacy = legalities["legacy"],
            legalVintage = legalities["vintage"],
            legalCommander = legalities["commander"],
            legalPauper = legalities["pauper"],
            priceUsd = null, // MTGCH API 不提供价格信息
            scryfallUri = dto.scryfallUri,
            imagePath = null, // 本地存储路径，暂时不用
            imageUriSmall = dto.zhsImageUris?.small ?: imageUris?.small,
            imageUriNormal = dto.zhsImageUris?.normal ?: imageUris?.normal,
            imageUriLarge = dto.zhsImageUris?.large ?: imageUris?.large,
            isDualFaced = isDualFaced,
            cardFacesJson = cardFacesJson,
            frontFaceName = frontFaceName,
            backFaceName = backFaceName,
            backFaceManaCost = backFaceManaCost,
            backFaceTypeLine = backFaceTypeLine,
            backFaceOracleText = backFaceOracleText,
            frontImageUri = frontImageUri,
            backImageUri = backImageUri,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
