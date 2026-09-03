package com.mtgo.decklistmanager.data.remote.api.forge

import com.mtgo.decklistmanager.data.remote.api.forge.ForgeMapper.mergePrinting
import com.mtgo.decklistmanager.data.remote.api.forge.ForgeMapper.toMtgchCard
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchApi
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchCardDto
import com.mtgo.decklistmanager.data.remote.api.mtgch.MtgchSearchResponse
import com.mtgo.decklistmanager.data.remote.api.mtgch.SetInfo
import com.mtgo.decklistmanager.util.AppLogger
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/**
 * Forge 中文卡查 API 适配器
 *
 * 实现旧 MtgchApi 接口，内部调用新 ForgeCardApi，
 * 使上层 ViewModel / Repository 无需改动即可切换到新服务器。
 *
 * 关键映射:
 * - 旧 offset/limit 分页 → 新 page/page_size
 * - 旧查询语法（name:"x" t:"y" c>=wu mv=2 s:ISD r:rare）→ 新结构化参数
 * - 新 24 位 id → 旧 oracleId 字段（见 ForgeMapper）
 * - 新 printings → 旧 getCardPrintings 的 cards 列表
 */
class ForgeCardApiAdapter(
    private val forgeApi: ForgeCardApi
) : MtgchApi {

    companion object {
        private const val TAG = "ForgeAdapter"

        /** 旧查询语法分词器：字段操作符 + 引号值 / 普通词 */
        private val TOKEN = Regex("""[A-Za-z_]+(?:>=|<=|=|:)(?:"[^"]*"|\S+)|"[^"]*"|\S+""")
    }

    // ==================== 搜索 ====================

    override suspend fun searchCard(
        query: String,
        limit: Int?,
        offset: Int?,
        unique: String?,
        color: String?,
        cmc: String?,
        type: String?,
        rarity: String?,
        set: String?
    ): Response<MtgchSearchResponse> {
        val parsed = parseLegacyQuery(query)
        val pageSize = limit ?: 20
        val page = ((offset ?: 0) / pageSize) + 1

        return try {
            val response = forgeApi.searchCards(
                q = parsed.q,
                page = page,
                pageSize = pageSize,
                colors = parsed.colors ?: color?.let { expandColors(it) },
                colorMode = parsed.colorMode,
                manaValue = parsed.manaValue ?: cmc?.toIntOrNull(),
                minManaValue = parsed.minManaValue,
                maxManaValue = parsed.maxManaValue,
                type = parsed.type ?: type,
                rarity = parsed.rarity ?: rarity,
                set = parsed.set ?: set
            )

            if (response.isSuccessful) {
                val body = response.body()
                val cards = body?.items?.map { it.toMtgchCard() } ?: emptyList()
                Response.success(ForgeMapper.toSearchResponse(cards, body?.total))
            } else {
                Response.error(response.code(), errorBody(response))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "searchCard failed", e)
            Response.error(599, e.message.orEmpty().toResponseBody(null))
        }
    }

    // ==================== 详情 ====================

    override suspend fun getCardById(oracleId: String): Response<MtgchCardDto> {
        // 上层传入的 oracleId 实际为新 API 的 24 位 id
        return try {
            val response = forgeApi.getCardDetail(oracleId)
            if (response.isSuccessful && response.body() != null) {
                Response.success(response.body()!!.toMtgchCard())
            } else {
                Response.error(response.code(), errorBody(response))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "getCardById failed: $oracleId", e)
            Response.error(599, e.message.orEmpty().toResponseBody(null))
        }
    }

    override suspend fun getCardPrintings(
        oracleId: String,
        limit: Int?,
        offset: Int?
    ): Response<MtgchSearchResponse> {
        return try {
            val response = forgeApi.getCardDetail(oracleId)
            if (response.isSuccessful && response.body() != null) {
                val detail = response.body()!!
                val printings = detail.printings ?: emptyList()
                val cards = printings.map { detail.mergePrinting(it) }
                Response.success(ForgeMapper.toSearchResponse(cards, printings.size))
            } else {
                Response.error(response.code(), errorBody(response))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "getCardPrintings failed: $oracleId", e)
            Response.error(599, e.message.orEmpty().toResponseBody(null))
        }
    }

    // ==================== 系列 ====================

    override suspend fun getCardsBySet(
        setCode: String,
        limit: Int?,
        offset: Int?
    ): Response<MtgchSearchResponse> {
        return searchCard(query = "", limit = limit, offset = offset, set = setCode)
    }

    override suspend fun getAllSets(): Response<List<SetInfo>> {
        return try {
            val response = forgeApi.getSets(pageSize = 200)
            if (response.isSuccessful) {
                val sets = response.body()?.items?.map {
                    SetInfo(
                        code = it.code ?: "",
                        name = it.nameZh ?: it.name ?: "",
                        releasedAt = it.releaseDate
                    )
                } ?: emptyList()
                Response.success(sets)
            } else {
                Response.error(response.code(), errorBody(response))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "getAllSets failed", e)
            Response.error(599, e.message.orEmpty().toResponseBody(null))
        }
    }

    // ==================== 随机（新 API 不支持） ====================

    override suspend fun getRandomCard(limit: Int?): Response<List<MtgchCardDto>> {
        return Response.success(emptyList())
    }

    // ==================== 旧查询语法解析 ====================

    private data class ParsedQuery(
        val q: String?,
        val colors: String?,
        val colorMode: String?,
        val type: String?,
        val rarity: String?,
        val set: String?,
        val manaValue: Int?,
        val minManaValue: Int?,
        val maxManaValue: Int?
    )

    /**
     * 将旧 MTGCH 查询语法解析为新 API 结构化参数
     *
     * 支持: name:"x" o:"x" t:"x" c=wu c>=wu c<=wu mv=2 mv>2 mv<2 s:ISD r:rare
     * 不支持的字段（ci/po/to/f/l/ft/a/game/is）被忽略，
     * 纯文本与 name/o 的值合并进 q。
     */
    private fun parseLegacyQuery(query: String): ParsedQuery {
        val qParts = mutableListOf<String>()
        var colors: String? = null
        var colorMode: String? = null
        var type: String? = null
        val rarities = mutableListOf<String>()
        var set: String? = null
        var manaValue: Int? = null
        var minMv: Int? = null
        var maxMv: Int? = null

        for (match in TOKEN.findAll(query)) {
            val token = match.value
            val fieldMatch = Regex("""^([A-Za-z_]+)(>=|<=|=|:)(.*)$""").matchEntire(token)

            if (fieldMatch == null) {
                qParts.add(token.trim('"'))
                continue
            }

            val field = fieldMatch.groupValues[1].lowercase()
            val op = fieldMatch.groupValues[2]
            val rawValue = fieldMatch.groupValues[3].trim('"')

            when (field) {
                "name", "o", "oracle", "text", "ft", "flavor_text" -> {
                    if (rawValue.isNotBlank()) qParts.add(rawValue)
                }
                "t", "type" -> type = rawValue
                "c", "color" -> {
                    colors = expandColors(rawValue)
                    colorMode = when (op) {
                        "=" -> "exact"
                        "<=" -> "contains"   // 至多 → 降级为包含
                        else -> "contains"  // >= 至少包含
                    }
                }
                "mv", "cmc", "mana_value" -> {
                    val v = rawValue.toIntOrNull() ?: continue
                    when (op) {
                        "=" -> manaValue = v
                        ">" -> minMv = v + 1
                        "<" -> maxMv = v - 1
                        ">=" -> minMv = v
                        "<=" -> maxMv = v
                    }
                }
                "s", "set" -> set = rawValue
                "r", "rarity" -> rarities.add(rawValue)
                // 新 API 不支持的字段：静默忽略
                "ci", "po", "power", "to", "toughness", "f", "l", "a", "artist", "game", "is" -> {}
                else -> qParts.add(rawValue)
            }
        }

        return ParsedQuery(
            q = qParts.joinToString(" ").takeIf { it.isNotBlank() },
            colors = colors,
            colorMode = colorMode,
            type = type,
            rarity = rarities.joinToString(",").takeIf { it.isNotBlank() },
            set = set,
            manaValue = manaValue,
            minManaValue = minMv,
            maxManaValue = maxMv
        )
    }

    /** "wu" / "w,u" → "W,U"（新 API 要求大写逗号分隔） */
    private fun expandColors(value: String): String {
        return value
            .replace(",", "")
            .map { it.uppercaseChar() }
            .filter { it in "WUBRGC" }
            .joinToString(",")
            .takeIf { it.isNotEmpty() } ?: value
    }

    private fun errorBody(response: Response<*>): okhttp3.ResponseBody {
        return (response.errorBody()?.string() ?: "HTTP ${response.code()}")
            .toResponseBody(null)
    }
}
