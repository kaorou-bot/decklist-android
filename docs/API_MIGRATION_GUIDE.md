# MTG Card Server API 集成指南

> **项目版本**: v4.3.0 → v4.4.0
> **更新日期**: 2026-02-13
> **目标**: 将 Android 应用从 MTGCH API 迁移到自有的 MTG Card Server API

---

## 目录

1. [概述](#概述)
2. [当前状态分析](#当前状态分析)
3. [API 差异对比](#api-差异对比)
4. [Android 端开发任务](#android-端开发任务)
5. [服务端开发任务](#服务端开发任务)
6. [并行开发计划](#并行开发计划)
7. [测试方案](#测试方案)
8. [部署清单](#部署清单)

---

## 概述

### 目标

将 Android 应用的卡牌数据源从第三方 MTGCH API (mtgch.com) 迁移到自有的 MTG Card Server，实现：

- ✅ 完全掌控卡牌数据
- ✅ 无第三方依赖
- ✅ 可扩展的数据服务
- ✅ 更快的响应速度

### 架构变化

```
┌─────────────────────────────────────────────────────────────────┐
│                        变更前                                   │
├─────────────────────────────────────────────────────────────────┤
│  Android App                                                    │
│     ↓                                                           │
│  MTGCH API (mtgch.com) ← 第三方服务                             │
│     ↓                                                           │
│  Scryfall API (备用)                                            │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        变更后                                   │
├─────────────────────────────────────────────────────────────────┤
│  Android App                                                    │
│     ↓                                                           │
│  MTG Card Server (自建) ← 可完全控制                            │
│     ↓                                                           │
│  Scryfall API (仅获取图片)                                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 当前状态分析

### Android 端当前使用的 API

**MTGCH API 端点**:
```
Base URL: https://mtgch.com/
```

| 端点 | 方法 | 用途 | 使用频率 |
|------|------|------|---------|
| `/api/v1/result` | GET | 卡牌搜索 | 高 |
| `/api/v1/card/{card_id}/` | GET | 单卡详情 | 中 |
| `/api/v1/card/{set}/{collector_number}/` | GET | 按系列编号查询 | 低 |
| `/api/v1/random` | GET | 随机卡牌 | 低 |

### 当前代码中的使用位置

```
app/src/main/java/com/mtgo/decklistmanager/
├── data/remote/api/mtgch/
│   ├── MtgchApi.kt              # Retrofit 接口定义
│   ├── MtgchCardDto.kt          # 数据模型
│   ├── MtgchSearchResponse.kt   # 响应模型
│   └── MtgchMapper.kt           # 数据转换
├── ui/search/SearchViewModel.kt        # 搜索功能
├── data/repository/DecklistRepository.kt # 套牌卡牌获取
└── di/AppModule.kt                     # 依赖注入配置
```

---

## API 差异对比

### 1. 搜索端点对比

| 对比项 | MTGCH API | MTG Card Server | 兼容性 |
|--------|-----------|-----------------|--------|
| **URL** | `/api/v1/result` | `/api/result` | ⚠️ 不同 |
| **方法** | GET | GET | ✅ 相同 |
| **查询参数** | `q`, `page`, `page_size`, `unique`, `priority_chinese`, `view`, `lang` | `q`, `page`, `page_size`, `unique` | ⚠️ 部分兼容 |
| **响应结构** | `{ items, page, page_size, total_pages }` | `{ success, count, items, page, page_size, total_pages }` | ⚠️ 新增 success |
| **中文字段名** | `zhs_name`, `zhs_text`, `zhs_type_line` | `zh_name`, `face_name` | ❌ 不同 |
| **双面牌标识** | 通过 `layout`, `card_faces` 判断 | `is_double_faced` (0/1) | ⚠️ 不同方式 |
| **图片 URL** | ✅ 包含 `image_uris`, `zhs_image_uris` | ❌ 不包含 | ❌ 缺失 |

### 2. 卡牌对象字段对比

| 功能 | MTGCH 字段 | MTG Card Server 字段 | 说明 |
|------|-----------|---------------------|------|
| **ID** | `id` (String) | `id` (Long), `oracle_id` (String) | 类型不同 |
| **英文名** | `name` | `name` | ✅ 相同 |
| **中文名** | `zhs_name`, `atomic_translated_name` | `zh_name`, `face_name` | ⚠️ 命名不同 |
| **法术力** | `mana_cost` | `mana_cost` | ✅ 相同 |
| **CMC** | `cmc` (Int) | `cmc` (Double) | ⚠️ 类型不同 |
| **类型** | `type_line` | `type_line` | ✅ 相同 |
| **描述** | `oracle_text`, `zhs_text` | `oracle_text` | ⚠️ 无专用中文描述 |
| **颜色** | `colors` (String[]) | `colors` (String[]) | ✅ 相同 |
| **稀有度** | `rarity` | `rarity` | ✅ 相同 |
| **系列代码** | `set` | `set_code` | ⚠️ 命名不同 |
| **系列名称** | `set_name`, `set_translated_name` | `set_name` | ⚠️ 少中文系列名 |
| **图片** | `image_uris`, `zhs_image_uris` | ❌ 无 | ❌ 缺失 |
| **双面牌** | `card_faces`, `other_faces` | `is_double_faced` | ⚠️ 详细程度不同 |
| **合法性** | `legalities` (Map) | `legalities` (Map) | ✅ 相同 |

### 3. 功能缺失分析

| 功能 | MTGCH | MTG Card Server | 影响 | 优先级 |
|------|-------|-----------------|------|--------|
| 基础搜索 | ✅ | ✅ | - | - |
| 中文显示 | ✅ | ✅ | - | - |
| 卡牌图片 | ✅ | ❌ | 需额外调用 Scryfall | 🔴 高 |
| 双面牌详情 | ✅ | ⚠️ | 仅标识，无详情 | 🟡 中 |
| 高级筛选 | ✅ | ⚠️ | 需服务端支持 | 🟡 中 |
| 中文系列名 | ✅ | ❌ | 显示英文系列名 | 🟢 低 |
| 中文规则描述 | ✅ | ⚠️ | 依赖翻译 | 🟢 低 |

---

## Android 端开发任务

### 文件清单

#### 新建文件

```
app/src/main/java/com/mtgo/decklistmanager/data/remote/api/mtgserver/
├── MtgCardServerApi.kt              # Retrofit API 接口
├── MtgCardServerResponse.kt         # API 响应数据模型
├── MtgCardServerDto.kt              # 卡牌 DTO
├── MtgCardServerMapper.kt           # 数据映射器
└── ScryfallImageApi.kt              # Scryfall 图片 API (新增)
```

#### 修改文件

| 文件路径 | 修改内容 | 代码行数估计 |
|---------|---------|-------------|
| `di/AppModule.kt` | 替换 Retrofit 配置 | ~20 行 |
| `ui/search/SearchViewModel.kt` | 替换 API 调用 | ~30 行 |
| `data/repository/DecklistRepository.kt` | 替换 API 调用 | ~50 行 |
| `ui/decklist/DeckDetailViewModel.kt` | 图片加载逻辑 | ~20 行 |
| `ui/carddetail/CardDetailActivity.kt` | 图片显示 | ~10 行 |

### 详细代码模板

#### 1. MtgCardServerApi.kt

```kotlin
package com.mtgo.decklistmanager.data.remote.api.mtgserver

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * MTG Card Server API 接口
 * Base URL: http://182.92.109.160:3000
 *
 * API 文档: /home/dministrator/MTG_CARD_SERVER_API.md
 */
interface MtgCardServerApi {

    /**
     * 搜索卡牌
     *
     * @param query 搜索关键词（支持中文或英文）
     * @param page 页码（从 1 开始，默认 1）
     * @param pageSize 每页数量（默认 20，最大 100）
     * @param unique 是否去重（默认 true）
     * @return 响应包含 success, count, items 等字段
     */
    @GET("api/result")
    suspend fun searchCard(
        @Query("q") query: String,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
        @Query("unique") unique: Boolean? = null
    ): Response<MtgCardServerSearchResponse>

    /**
     * 获取单个卡牌详情
     *
     * @param id 卡牌数据库 ID
     */
    @GET("api/cards/{id}")
    suspend fun getCard(
        @Path("id") id: Long
    ): Response<MtgCardServerDto>

    /**
     * 获取随机卡牌
     */
    @GET("api/random")
    suspend fun getRandomCard(): Response<MtgCardServerDto>

    /**
     * 获取所有 Magic 系列
     */
    @GET("api/sets")
    suspend fun getAllSets(): Response<List<MtgSetDto>>

    /**
     * 获取热门卡牌统计
     *
     * @param limit 返回数量（默认 50）
     */
    @GET("api/stats/popular")
    suspend fun getPopularCards(
        @Query("limit") limit: Int = 50
    ): Response<List<PopularCardDto>>
}

/**
 * 系列 DTO
 */
data class MtgSetDto(
    val id: String?,
    val name: String?,
    val code: String?,
    val releaseDate: String?
)

/**
 * 热门卡牌 DTO
 */
data class PopularCardDto(
    val cardName: String?,
    val count: Int?
)
```

#### 2. MtgCardServerResponse.kt

```kotlin
package com.mtgo.decklistmanager.data.remote.api.mtgserver

import com.google.gson.annotations.SerializedName

/**
 * MTG Card Server 搜索响应
 */
data class MtgCardServerSearchResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("count")
    val count: Int,

    @SerializedName("page")
    val page: Int?,

    @SerializedName("page_size")
    val pageSize: Int?,

    @SerializedName("total_pages")
    val totalPages: Int?,

    @SerializedName("items")
    val items: List<MtgCardServerDto>?
)

/**
 * MTG Card Server 卡牌 DTO
 *
 * 注意：此 API 不包含图片 URLs，需要通过 scryfall_id 从 Scryfall 获取
 */
data class MtgCardServerDto(
    // ===== 基础标识 =====
    @SerializedName("id")
    val id: Long?,

    @SerializedName("oracle_id")
    val oracleId: String?,

    @SerializedName("scryfall_id")
    val scryfallId: String?,

    // ===== 名称 =====
    @SerializedName("name")
    val name: String?,            // 英文名

    @SerializedName("zh_name")
    val zhName: String?,          // 中文名

    @SerializedName("face_name")
    val faceName: String?,        // 正面中文名

    @SerializedName("lang")
    val lang: String?,            // 卡牌语言

    // ===== 法术力 =====
    @SerializedName("mana_cost")
    val manaCost: String?,        // 例如 "{2}{R}"

    @SerializedName("cmc")
    val cmc: Double?,             // 集换法术力值 (注意是 Double 类型)

    // ===== 类型 =====
    @SerializedName("type_line")
    val typeLine: String?,        // 类型号，例如 "Instant — Creature"

    @SerializedName("oracle_text")
    val oracleText: String?,      // Oracle 规则描述

    // ===== 系列信息 =====
    @SerializedName("set_code")
    val setCode: String?,         // 系列代码，例如 "LEA"

    @SerializedName("set_name")
    val setName: String?,         // 系列名称，例如 "Limited Edition Alpha"

    @SerializedName("set_released_at")
    val setReleasedAt: String?,   // 系列发布日期

    // ===== 颜色 =====
    @SerializedName("colors")
    val colors: List<String>?,    // 颜色数组，例如 ["R"]

    @SerializedName("color_identity")
    val colorIdentity: List<String>?,  // 颜色标识

    // ===== 稀有度 =====
    @SerializedName("rarity")
    val rarity: String?,          // common, uncommon, rare, mythic

    // ===== 攻防 =====
    @SerializedName("power")
    val power: String?,

    @SerializedName("toughness")
    val toughness: String?,

    @SerializedName("loyalty")
    val loyalty: String?,

    @SerializedName("hand")
    val hand: String?,

    @SerializedName("life")
    val life: String?,

    // ===== 特殊标识 =====
    @SerializedName("is_double_faced")
    val isDoubleFaced: Int?,      // 0 = 单面, 1 = 双面

    @SerializedName("is_token")
    val isToken: Int?,            // 0 = 非衍生物, 1 = 衍生物

    // ===== 类型数组 =====
    @SerializedName("type")
    val type: String?,

    @SerializedName("types")
    val types: List<String>?,     // 类型数组，例如 ["Instant"]

    // ===== 图片 (当前不包含，预留字段) =====
    @SerializedName("image_uris")
    val imageUris: Map<String, String>?,

    // ===== 合法性 =====
    @SerializedName("legalities")
    val legalities: Map<String, String>?
) {
    /**
     * 获取显示名称（优先中文）
     */
    fun getDisplayName(): String {
        return zhName ?: faceName ?: name ?: ""
    }

    /**
     * 是否为双面牌
     */
    fun isDualFaced(): Boolean {
        return isDoubleFaced == 1 ||
               faceName?.contains("//") == true ||
               zhName?.contains("//") == true
    }

    /**
     * 构建 Scryfall 图片 URL
     */
    fun getScryfallImageUrl(): String? {
        return if (scryfallId != null) {
            "https://api.scryfall.com/cards/$scryfallId?format=image"
        } else null
    }
}
```

#### 3. MtgCardServerMapper.kt

```kotlin
package com.mtgo.decklistmanager.data.remote.api.mtgserver

import com.mtgo.decklistmanager.data.local.entity.CardInfoEntity
import com.mtgo.decklistmanager.ui.search.model.SearchResultItem
import com.mtgo.decklistmanager.util.AppLogger

/**
 * 将 MTG Card Server DTO 转换为应用内部模型
 */

/**
 * 转换为 CardInfoEntity (用于数据库缓存)
 */
fun MtgCardServerDto.toEntity(): CardInfoEntity {
    val isDualFaced = this.isDualFaced()
    val displayName = this.getDisplayName()

    // 构建 Scryfall 图片 URL
    val scryfallImageUrl = this.getScryfallImageUrl()

    AppLogger.d("MtgCardServerMapper", "Converting: $name -> $displayName (dual: $isDualFaced)")

    return CardInfoEntity(
        // ID
        id = oracleId ?: id?.toString() ?: name ?: "",
        name = displayName,
        enName = name,

        // 法术力
        manaCost = manaCost,
        cmc = cmc,

        // 类型
        typeLine = typeLine,
        oracleText = oracleText,

        // 颜色
        colors = colors?.joinToString(","),
        colorIdentity = colorIdentity?.joinToString(","),

        // 攻防
        power = power,
        toughness = toughness,
        loyalty = loyalty,

        // 系列和稀有度
        rarity = rarity,
        setCode = setCode,
        setName = setName,
        cardNumber = null,  // API 暂无 collector_number

        // 合法性
        legalStandard = legalities?.get("standard"),
        legalModern = legalities?.get("modern"),
        legalPioneer = legalities?.get("pioneer"),
        legalLegacy = legalities?.get("legacy"),
        legalVintage = legalities?.get("vintage"),
        legalCommander = legalities?.get("commander"),
        legalPauper = legalities?.get("pauper"),

        // 价格和链接
        priceUsd = null,
        scryfallUri = if (scryfallId != null) "https://scryfall.com/cards/$scryfallId" else null,

        // 图片 (使用 Scryfall)
        imagePath = scryfallImageUrl,
        imageUriSmall = scryfallImageUrl,
        imageUriNormal = scryfallImageUrl,
        imageUriLarge = scryfallImageUrl,

        // 双面牌
        isDualFaced = isDualFaced,
        cardFacesJson = null,  // API 暂不支持
        frontFaceName = if (isDualFaced) faceName else null,
        backFaceName = null,  // API 暂无
        backFaceManaCost = null,
        backFaceTypeLine = null,
        backFaceOracleText = null,
        backFacePower = null,
        backFaceToughness = null,
        backFaceLoyalty = null,
        frontImageUri = scryfallImageUrl,
        backImageUri = null,

        // 元数据
        lastUpdated = System.currentTimeMillis()
    )
}

/**
 * 转换为 SearchResultItem (用于搜索结果显示)
 */
fun MtgCardServerDto.toSearchResultItem(): SearchResultItem {
    return SearchResultItem(
        id = oracleId ?: id?.toString() ?: "",
        name = getDisplayName(),
        enName = name,
        manaCost = manaCost,
        cmc = cmc?.toInt() ?: 0,
        typeLine = typeLine,
        setCode = setCode,
        setName = setName,
        rarity = rarity,
        colors = colors ?: emptyList(),
        isDoubleFaced = isDualFaced(),
        oracleId = oracleId,
        scryfallId = scryfallId
    )
}
```

#### 4. ScryfallImageApi.kt (新增)

```kotlin
package com.mtgo.decklistmanager.data.remote.api.mtgserver

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Scryfall API - 仅用于获取卡牌图片
 * Base URL: https://api.scryfall.com
 *
 * 注意：这是 Scryfall 的官方 API，请遵守其使用条款
 * - 速率限制: 10-100 requests/second
 * - 文档: https://scryfall.com/docs/api
 */
interface ScryfallImageApi {

    /**
     * 根据 Scryfall ID 获取卡牌详细信息（含图片）
     *
     * @param scryfallId Scryfall 卡牌 ID
     */
    @GET("cards/{scryfallId}")
    suspend fun getCardById(
        @Path("scryfallId") scryfallId: String
    ): Response<ScryfallCardDto>

    /**
     * 根据 Oracle ID 获取卡牌
     */
    @GET("cards/oracle/{oracleId}")
    suspend fun getCardByOracleId(
        @Path("oracleId") oracleId: String
    ): Response<ScryfallCardDto>
}

/**
 * Scryfall 卡牌 DTO (简化版，仅包含需要的字段)
 */
data class ScryfallCardDto(
    val id: String?,
    val oracle_id: String?,
    val name: String?,
    val image_uris: ScryfallImageUris?,
    val card_faces: List<ScryfallCardFace>?
)

data class ScryfallImageUris(
    val small: String?,
    val normal: String?,
    val large: String?,
    val png: String?,
    val art_crop: String?,
    val border_crop: String?
)

data class ScryfallCardFace(
    val name: String?,
    val image_uris: ScryfallImageUris?,
    val oracle_text: String?,
    val type_line: String?
)
```

#### 5. AppModule.kt 修改

```kotlin
package com.mtgo.decklistmanager.di

import com.mtgo.decklistmanager.data.remote.api.mtgserver.MtgCardServerApi
import com.mtgo.decklistmanager.data.remote.api.mtgserver.ScryfallImageApi
// ... 其他 import

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ... 其他 Provider

    /**
     * 提供 OkHttpClient (共享)
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 提供 Retrofit for MTG Card Server
     * Base URL: http://182.92.109.160:3000
     */
    @Provides
    @Singleton
    fun provideMtgCardServerRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://182.92.109.160:3000/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 提供 Retrofit for Scryfall Image API
     * Base URL: https://api.scryfall.com
     */
    @Provides
    @Singleton
    fun provideScryfallRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.scryfall.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 提供 MtgCardServerApi
     */
    @Provides
    @Singleton
    fun provideMtgCardServerApi(
        mtgCardServerRetrofit: Retrofit
    ): MtgCardServerApi {
        return mtgCardServerRetrofit.create(MtgCardServerApi::class.java)
    }

    /**
     * 提供 ScryfallImageApi
     */
    @Provides
    @Singleton
    fun provideScryfallImageApi(
        scryfallRetrofit: Retrofit
    ): ScryfallImageApi {
        return scryfallRetrofit.create(ScryfallImageApi::class.java)
    }

    // 删除旧的 provideMtgchRetrofit 和 provideMtgchApi
}
```

#### 6. SearchViewModel.kt 修改

```kotlin
package com.mtgo.decklistmanager.ui.search

import com.mtgo.decklistmanager.data.remote.api.mtgserver.MtgCardServerApi
// ... 其他 import

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mtgCardServerApi: MtgCardServerApi,  // 替换 mtgchApi
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {

    // ... 其他字段

    /**
     * 执行在线搜索
     */
    fun search(
        query: String,
        page: Int = 1,
        pageSize: Int = 50,
        filters: SearchFilters? = null
    ) {
        val hasFilters = hasActiveFilters(filters)
        if (query.isBlank() && !hasFilters) {
            _searchResults.value = emptyList()
            _showHistory.value = true
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _showHistory.value = false
            _errorMessage.value = null

            try {
                // 构建搜索查询字符串
                val searchQuery = buildSearchQuery(query, filters)

                AppLogger.d("SearchViewModel", "Searching: $searchQuery")

                // 调用 MTG Card Server API
                val response = mtgCardServerApi.searchCard(
                    query = searchQuery,
                    page = page,
                    pageSize = pageSize,
                    unique = true
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    val cards = body.items ?: emptyList()

                    AppLogger.d("SearchViewModel", "Found ${cards.size} results")

                    // 转换为 SearchResultItem
                    val results = cards.map { it.toSearchResultItem() }
                    _searchResults.value = results

                    // 保存搜索历史
                    if (query.isNotBlank() && results.isNotEmpty()) {
                        saveSearchHistory(query, results.size)
                    }
                } else {
                    val errorMsg = "搜索失败: ${response.code()} ${response.message()}"
                    AppLogger.e("SearchViewModel", errorMsg)
                    _errorMessage.value = errorMsg
                    _searchResults.value = emptyList()
                }
            } catch (e: Exception) {
                AppLogger.e("SearchViewModel", "Search error", e)
                _errorMessage.value = "搜索出错: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    // buildSearchQuery 方法可能需要调整，取决于服务端支持的查询语法
    private fun buildSearchQuery(
        query: String,
        filters: SearchFilters?
    ): String {
        // 如果服务端支持 MTGCH 的查询语法，可以保持不变
        // 否则需要调整为服务端支持的格式

        // TODO: 根据服务端实际支持的筛选参数调整此方法
        val parts = mutableListOf<String>()

        // 原始查询
        if (query.isNotBlank()) {
            parts.add(query)
        }

        // 筛选条件 (示例，需根据服务端 API 调整)
        filters?.let { f ->
            f.colors?.let { colors ->
                if (colors.isNotEmpty()) {
                    // MTGCH 语法: color:U,R
                    parts.add("color:${colors.joinToString(",")}")
                }
            }

            f.type?.let { type ->
                parts.add("type:$type")
            }

            f.rarity?.let { rarity ->
                parts.add("rarity:$rarity")
            }

            f.set?.let { set ->
                parts.add("set:$set")
            }

            when (f.cmcOperator) {
                "=" -> f.cmc?.let { parts.add("cmc=$it") }
                ">" -> f.cmc?.let { parts.add("cmc>$it") }
                "<" -> f.cmc?.let { parts.add("cmc<$it") }
                else -> {} // "任意" 不添加条件
            }
        }

        return parts.joinToString(" ")
    }
}
```

#### 7. DecklistRepository.kt 修改

```kotlin
package com.mtgo.decklistmanager.data.repository

import com.mtgo.decklistmanager.data.remote.api.mtgserver.MtgCardServerApi
import com.mtgo.decklistmanager.data.remote.api.mtgserver.ScryfallImageApi
// ... 其他 import

@Singleton
class DecklistRepository @Inject constructor(
    private val decklistDao: DecklistDao,
    private val cardDao: CardDao,
    private val cardInfoDao: CardInfoDao,
    private val eventDao: EventDao,
    private val favoriteDecklistDao: FavoriteDecklistDao,
    private val magicScraper: MagicScraper,
    private val mtgTop8Scraper: MtgTop8Scraper,
    private val mtgCardServerApi: MtgCardServerApi,  // 替换 mtgchApi
    private val scryfallImageApi: ScryfallImageApi,  // 新增
    private val languagePreferenceManager: LanguagePreferenceManager
) {

    /**
     * 自动从 MTG Card Server 获取卡牌详情
     */
    private suspend fun fetchScryfallDetails(decklistId: Long) = coroutineScope {
        try {
            val cards = cardDao.getCardsByDecklistId(decklistId)
            val uniqueCardNames = cards.map { it.cardName }.distinct()

            AppLogger.d("DecklistRepository", "fetchScryfallDetails: Processing ${uniqueCardNames.size} unique cards")

            // 检查哪些卡牌需要更新
            val cardsNeedingUpdate = uniqueCardNames.filter { cardName ->
                val cardRecords = cards.filter { it.cardName == cardName }
                cardRecords.any { it.displayName.isNullOrBlank() || it.manaCost.isNullOrBlank() }
            }

            if (cardsNeedingUpdate.isEmpty()) {
                AppLogger.d("DecklistRepository", "All cards already complete, skipping update")
                return@coroutineScope
            }

            AppLogger.d("DecklistRepository", "Updating ${cardsNeedingUpdate.size} cards from cache or API")

            val semaphore = Semaphore(2)

            cardsNeedingUpdate.mapIndexed { index, cardName ->
                async {
                    semaphore.acquire()
                    try {
                        if (index > 0 && index % 2 == 0) {
                            delay(500)
                        }

                        val formattedCardName = normalizeCardName(cardName)

                        // 1. 先查缓存
                        val cachedCardInfo = cardInfoDao.getCardInfoByNameOrEnName(formattedCardName)

                        if (cachedCardInfo != null) {
                            updateCardsFromCache(cards, cardName, cachedCardInfo)
                        } else {
                            // 2. 缓存未命中，调用 API
                            AppLogger.d("DecklistRepository", "  Cache miss for: $cardName, fetching from API")

                            val response = mtgCardServerApi.searchCard(
                                query = formattedCardName,
                                pageSize = 20,
                                unique = true
                            )

                            if (response.isSuccessful && response.body()?.success == true) {
                                val searchResponse = response.body()!!
                                val results = searchResponse.items

                                if (results != null && results.isNotEmpty()) {
                                    val bestMatch = findBestMatch(formattedCardName, results)
                                    if (bestMatch != null) {
                                        // 保存到缓存
                                        val entity = bestMatch.toEntity()
                                        cardInfoDao.insert(entity)

                                        // 更新 cards 表
                                        updateCardsFromDto(cards, cardName, bestMatch)
                                    }
                                }
                            }
                        }
                    } finally {
                        semaphore.release()
                    }
                }
            }.awaitAll()

        } catch (e: Exception) {
            AppLogger.e("DecklistRepository", "Error fetching card details", e)
        }
    }

    /**
     * 从缓存更新卡牌
     */
    private suspend fun updateCardsFromCache(
        cards: List<CardEntity>,
        cardName: String,
        cachedCardInfo: CardInfoEntity
    ) {
        cards.filter { it.cardName == cardName }.forEach { card ->
            cardDao.updateDetails(
                cardId = card.id,
                manaCost = cachedCardInfo.manaCost,
                color = cachedCardInfo.colors,
                rarity = cachedCardInfo.rarity,
                cardType = cachedCardInfo.typeLine,
                cardSet = cachedCardInfo.setName,
                displayName = cachedCardInfo.name
            )
        }
    }

    /**
     * 从 DTO 更新卡牌
     */
    private suspend fun updateCardsFromDto(
        cards: List<CardEntity>,
        cardName: String,
        dto: MtgCardServerDto
    ) {
        val displayName = dto.getDisplayName()
        cards.filter { it.cardName == cardName }.forEach { card ->
            cardDao.updateDetails(
                cardId = card.id,
                manaCost = dto.manaCost,
                color = dto.colors?.joinToString(","),
                rarity = dto.rarity,
                cardType = dto.typeLine,
                cardSet = dto.setName,
                displayName = displayName
            )
        }
    }

    /**
     * 查找最佳匹配的卡牌
     */
    private fun findBestMatch(
        cardName: String,
        results: List<MtgCardServerDto>
    ): MtgCardServerDto? {
        // 优先匹配英文名
        val exactEnglishMatch = results.find { it.name.equals(cardName, ignoreCase = true) }
        if (exactEnglishMatch != null) return exactEnglishMatch

        // 其次匹配中文名
        val exactChineseMatch = results.find { it.zhName.equals(cardName, ignoreCase = true) }
        if (exactChineseMatch != null) return exactChineseMatch

        // 模糊匹配
        val fuzzyMatch = results.find {
            it.name?.contains(cardName, ignoreCase = true) == true ||
            it.zhName?.contains(cardName, ignoreCase = true) == true
        }
        if (fuzzyMatch != null) return fuzzyMatch

        // 返回第一个结果
        return results.firstOrNull()
    }
}
```

---

## 服务端开发任务

### API 规范补充

为了完全兼容 Android 端的功能，服务端需要补充以下功能：

#### 1. 高级搜索筛选

**当前状态**: 基础搜索仅支持关键词

**需要支持的筛选参数**:

| 参数 | 格式 | 示例 | 说明 |
|------|------|------|------|
| 颜色筛选 | `color:{colors}` | `color:U,R` | 多个颜色用逗号分隔 |
| 颜色标识 | `color_identity:{colors}` | `color_identity:WUB` | 精确匹配 |
| 法术力值 | `cmc:{operator}{value}` | `cmc>3`, `cmc=2` | 支持 =, >, < |
| 类型筛选 | `type:{type}` | `type:creature`, `type:instant` | 卡牌类型 |
| 稀有度 | `rarity:{rarity}` | `rarity:rare` | common/uncommon/rare/mythic |
| 系列代码 | `set:{code}` | `set:NEO` | 系列代码 |
| 伙伴颜色 | `partner:{colors}` | `partner:G` | 指定伙伴 |

**实现建议**:

```sql
-- 示例：颜色筛选
SELECT * FROM cards
WHERE colors LIKE '%R%' OR colors LIKE '%W%';

-- 示例：法术力值筛选
SELECT * FROM cards
WHERE cmc > 3;

-- 示例：类型筛选
SELECT * FROM cards
WHERE type_line LIKE '%Creature%' OR types LIKE '%Creature%';

-- 组合查询
SELECT * FROM cards
WHERE (name LIKE '%闪电%' OR zh_name LIKE '%闪电%')
  AND (colors LIKE '%R%' OR colors LIKE '%R,W%' OR colors LIKE '%W,R%')
  AND cmc >= 2
  AND type_line LIKE '%Instant%';
```

#### 2. 双面牌详细信息

**当前状态**: 仅有 `is_double_faced` 标识

**需要新增字段**:

```sql
ALTER TABLE cards ADD COLUMN back_face_name TEXT;
ALTER TABLE cards ADD COLUMN back_face_mana_cost TEXT;
ALTER TABLE cards ADD COLUMN back_face_type_line TEXT;
ALTER TABLE cards ADD COLUMN back_face_oracle_text TEXT;
ALTER TABLE cards ADD COLUMN back_face_power TEXT;
ALTER TABLE cards ADD COLUMN back_face_toughness TEXT;
ALTER TABLE cards ADD COLUMN back_face_loyalty TEXT;

-- 双面牌 JSON (可选，用于完整双面牌数据)
ALTER TABLE cards ADD COLUMN card_faces_json TEXT;
```

**API 响应示例**:

```json
{
  "id": 12345,
  "name": "Delver of Secrets",
  "zh_name": "秘密家德尔弗",
  "is_double_faced": 1,
  "back_face_name": "Insectile Aberration",
  "back_face_zh_name": "昆虫异变",
  "back_face_mana_cost": null,
  "back_face_type_line": "Creature — Horror",
  "back_face_oracle_text": "This creature can't be blocked.",
  "back_face_power": "3",
  "back_face_toughness": "2"
}
```

#### 3. 图片 URLs

**当前状态**: 不返回图片

**推荐方案**:

有两种方式可以支持图片：

**方案 A**: 返回 Scryfall 图片 URL（推荐）

```json
{
  "scryfall_id": "abc123",
  "image_uris": {
    "small": "https://api.scryfall.com/cards/abc123?format=image&version=small",
    "normal": "https://api.scryfall.com/cards/abc123?format=image&version=normal",
    "large": "https://api.scryfall.com/cards/abc123?format=image&version=large"
  },
  "back_image_uris": {  // 双面牌背面
    "normal": "https://api.scryfall.com/cards/abc123/back?format=image"
  }
}
```

**实现代码**:

```javascript
// 在服务端添加图片 URL 生成
function addImageUrls(card) {
  if (!card.scryfall_id) return card;

  const baseUrl = 'https://api.scryfall.com/cards';
  const imageUrls = {
    small: `${baseUrl}/${card.scryfall_id}?format=image&version=small`,
    normal: `${baseUrl}/${card.scryfall_id}?format=image&version=normal`,
    large: `${baseUrl}/${card.scryfall_id}?format=image&version=large`,
    png: `${baseUrl}/${card.scryfall_id}?format=image&version=png`
  };

  return {
    ...card,
    image_uris: imageUrls
  };
}

// 在 API 响应中应用
app.get('/api/result', (req, res) => {
  const cards = searchCards(req.query);
  const cardsWithImages = cards.map(addImageUrls);
  res.json({ success: true, items: cardsWithImages });
});
```

**方案 B**: 代理 Scryfall API（复杂但更好控制）

```javascript
// 创建 Scryfall 代理端点
app.get('/api/cards/:scryfallId/images', async (req, res) => {
  try {
    const scryfallResponse = await fetch(
      `https://api.scryfall.com/cards/${req.params.scryfallId}`
    );
    const data = await scryfallResponse.json();

    // 只返回图片相关信息
    res.json({
      image_uris: data.image_uris,
      card_faces: data.card_faces?.map(face => ({
        image_uris: face.image_uris
      }))
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch images' });
  }
});
```

#### 4. 中文系列名称

**当前状态**: 仅返回英文系列名

**需要添加**:

```sql
-- 方案 A: 添加列
ALTER TABLE cards ADD COLUMN set_zh_name TEXT;

-- 方案 B: 创建翻译表
CREATE TABLE set_translations (
  set_code TEXT PRIMARY KEY,
  en_name TEXT NOT NULL,
  zh_name TEXT
);

-- 插入数据
INSERT INTO set_translations (set_code, en_name, zh_name) VALUES
('NEO', 'Kamigawa: Neon Dynasty', '神河：霓朝志'),
('MOM', 'March of the Machine', '机器临世的征途'),
-- ... 更多系列
```

#### 5. 搜索历史记录 (可选)

如果需要在服务端同步搜索历史：

```sql
CREATE TABLE search_history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id TEXT,  -- 可选，用于多用户
  query TEXT NOT NULL,
  result_count INTEGER,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- API 端点
-- POST /api/search/history - 保存搜索历史
-- GET /api/search/history - 获取搜索历史
```

### 服务端实现清单

#### 任务 1: 增强搜索功能

**文件**: `routes/search.js` (或对应的路由文件)

```javascript
/**
 * 增强的搜索端点
 * GET /api/result?q={query}&color={colors}&cmc={cmc}&type={type}&rarity={rarity}&set={set}
 */
app.get('/api/result', async (req, res) => {
  try {
    const {
      q = '',           // 搜索关键词
      color,            // 颜色筛选: U,R,W,B,G
      cmc,              // 法术力值
      type,             // 类型: creature, instant, sorcery...
      rarity,           // 稀有度: common, uncommon, rare, mythic
      set: setCode,     // 系列代码
      page = 1,
      page_size = 20,
      unique = true
    } = req.query;

    // 构建 SQL 查询
    let sql = 'SELECT * FROM cards WHERE 1=1';
    const params = [];

    // 关键词搜索
    if (q) {
      sql += ' AND (name LIKE ? OR zh_name LIKE ? OR face_name LIKE ?)';
      const searchTerm = `%${q}%`;
      params.push(searchTerm, searchTerm, searchTerm);
    }

    // 颜色筛选
    if (color) {
      const colors = color.split(',');
      const colorConditions = colors.map(() => 'colors LIKE ?').join(' OR ');
      sql += ` AND (${colorConditions})`;
      colors.forEach(c => params.push(`%"${c.trim()}"%`));
    }

    // 法术力值筛选
    if (cmc) {
      const cmcMatch = cmc.match(/^([=<>])(\d+)$/);
      if (cmcMatch) {
        const [, operator, value] = cmcMatch;
        sql += ` AND cmc ${operator} ?`;
        params.push(parseFloat(value));
      } else if (!isNaN(parseFloat(cmc))) {
        sql += ' AND cmc = ?';
        params.push(parseFloat(cmc));
      }
    }

    // 类型筛选
    if (type) {
      sql += ' AND (type_line LIKE ? OR types LIKE ?)';
      params.push(`%${type}%`, `%"${type}"%`);
    }

    // 稀有度筛选
    if (rarity) {
      sql += ' AND rarity = ?';
      params.push(rarity);
    }

    // 系列筛选
    if (setCode) {
      sql += ' AND set_code = ?';
      params.push(setCode.toUpperCase());
    }

    // 获取总数
    const countSql = sql.replace('SELECT *', 'SELECT COUNT(*) as count');
    const countResult = await db.get(countSql, params);
    const totalCards = countResult.count;

    // 分页
    const offset = (page - 1) * page_size;
    sql += ' ORDER BY name LIMIT ? OFFSET ?';
    params.push(parseInt(page_size), offset);

    // 执行查询
    const cards = await db.all(sql, params);

    // 添加图片 URLs
    const cardsWithImages = cards.map(addScryfallImageUrls);

    // 去重（如果 unique=true）
    const items = unique
      ? deduplicateByOracleId(cardsWithImages)
      : cardsWithImages;

    res.json({
      success: true,
      count: totalCards,
      page: parseInt(page),
      page_size: parseInt(page_size),
      total_pages: Math.ceil(totalCards / page_size),
      items
    });

  } catch (error) {
    console.error('Search error:', error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

/**
 * 添加 Scryfall 图片 URL
 */
function addScryfallImageUrls(card) {
  if (!card.scryfall_id) return card;

  const baseUrl = 'https://api.scryfall.com/cards';

  return {
    ...card,
    image_uris: {
      small: `${baseUrl}/${card.scryfall_id}?format=image&version=small`,
      normal: `${baseUrl}/${card.scryfall_id}?format=image&version=normal`,
      large: `${baseUrl}/${card.scryfall_id}?format=image&version=large`,
      png: `${baseUrl}/${card.scryfall_id}?format=image&version=png`
    }
  };
}

/**
 * 按 Oracle ID 去重
 */
function deduplicateByOracleId(cards) {
  const seen = new Set();
  return cards.filter(card => {
    if (seen.has(card.oracle_id)) {
      return false;
    }
    seen.add(card.oracle_id);
    return true;
  });
}
```

#### 任务 2: 数据库迁移脚本

**文件**: `migrations/002_add_enhanced_card_fields.sql`

```sql
-- ===== 迁移 002: 增强卡牌字段 =====

-- 双面牌背面信息
ALTER TABLE cards ADD COLUMN back_face_name TEXT;
ALTER TABLE cards ADD COLUMN back_face_mana_cost TEXT;
ALTER TABLE cards ADD COLUMN back_face_type_line TEXT;
ALTER TABLE cards ADD COLUMN back_face_oracle_text TEXT;
ALTER TABLE cards ADD COLUMN back_face_power TEXT;
ALTER TABLE cards ADD COLUMN back_face_toughness TEXT;
ALTER TABLE cards ADD COLUMN back_face_loyalty TEXT;

-- 卡牌面 JSON (完整双面牌数据)
ALTER TABLE cards ADD COLUMN card_faces_json TEXT;

-- 中文系列名称
ALTER TABLE cards ADD COLUMN set_zh_name TEXT;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_cards_back_face ON cards(back_face_name);
CREATE INDEX IF NOT EXISTS idx_cards_set_zh_name ON cards(set_zh_name);
```

#### 任务 3: 系列翻译数据

**文件**: `data/set_translations.json`

```json
{
  "NEO": {
    "en": "Kamigawa: Neon Dynasty",
    "zh": "神河：霓朝志"
  },
  "MOM": {
    "en": "March of the Machine",
    "zh": "机器临世的征途"
  },
  "ONE": {
    "en": "Phyrexia: All Will Be One",
    "zh": "新非瑞克西亚：万众归一"
  },
  "BRO": {
    "en": "The Brothers' War",
    "zh": "兄弟战争"
  },
  "DMU": {
    "en": "Dominaria United",
    "zh": "多明纳里亚 united"
  },
  "SNC": {
    "en": "Streets of New Capenna",
    "zh": "新卡佩纳城"
  },
  "Kamigawa: Neon Dynasty": {
    "en": "Kamigawa: Neon Dynasty",
    "zh": "神河：霓朝志"
  }
}
```

**导入脚本**:

```javascript
// scripts/import_set_translations.js
const fs = require('fs');
const db = require('../database');

const translations = JSON.parse(
  fs.readFileSync('./data/set_translations.json', 'utf8')
);

async function importTranslations() {
  for (const [code, names] of Object.entries(translations)) {
    await db.run(
      `UPDATE cards SET set_zh_name = ? WHERE set_code = ?`,
      [names.zh, code]
    );
  }
  console.log('Set translations imported successfully');
}

importTranslations().catch(console.error);
```

#### 任务 4: 环境变量配置

**文件**: `.env`

```env
# 服务器配置
PORT=3000
NODE_ENV=development

# 数据库
DB_PATH=./data/cards.db

# Scryfall API (用于图片)
SCRYFALL_API_BASE=https://api.scryfall.com

# CORS 配置
CORS_ORIGIN=*

# 日志
LOG_LEVEL=info
```

#### 任务 5: API 响应格式规范化

确保所有端点返回统一格式：

```javascript
// middleware/responseFormatter.js
function formatResponse(req, res, next) {
  const originalJson = res.json;

  res.json = function(data) {
    // 确保响应包含 success 字段
    if (typeof data === 'object' && data !== null && !('success' in data)) {
      data = { success: true, ...data };
    }
    originalJson.call(this, data);
  };

  next();
}

app.use(formatResponse);
```

---

## 并行开发计划

### 阶段划分

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          阶段 1: 基础搭建 (1-2 天)                      │
├─────────────────────────────────────────────────────────────────────────┤
│  Android 端                    │  服务端                                │
│  ─────────────                 │  ──────────                            │
│  ✅ 创建新的 API 接口文件       │  ✅ 确认当前 API 可用                   │
│  ✅ 创建数据模型类              │  ✅ 测试基础搜索端点                    │
│  ✅ 配置依赖注入                │  ✅ 准备开发环境                        │
│  ✅ 测试 API 连接               │                                        │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                      阶段 2: 核心功能对接 (2-3 天)                       │
├─────────────────────────────────────────────────────────────────────────┤
│  Android 端                    │  服务端                                │
│  ─────────────                 │  ──────────                            │
│  ⏳ 更新 SearchViewModel        │  ⏳ 实现高级搜索筛选                   │
│  ⏳ 更新 DecklistRepository     │  ⏳ 添加 Scryfall 图片 URL            │
│  ⏳ 实现 Scryfall 图片 API       │  ⏳ 优化查询性能                       │
│  ⏳ 测试搜索功能                │  ⏳ 添加单元测试                       │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                      阶段 3: 增强功能 (2-3 天)                          │
├─────────────────────────────────────────────────────────────────────────┤
│  Android 端                    │  服务端                                │
│  ─────────────                 │  ──────────                            │
│  ⏳ 双面牌支持                  │  ⏳ 添加双面牌详细字段                 │
│  ⏳ 卡牌详情页适配              │  ⏳ 数据库迁移                         │
│  ⏳ 搜索历史功能                │  ⏳ 系列翻译数据                       │
│  ⏳ 错误处理优化                │  ⏳ API 响应规范化                     │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                       阶段 4: 测试与优化 (1-2 天)                        │
├─────────────────────────────────────────────────────────────────────────┤
│  Android 端                    │  服务端                                │
│  ─────────────                 │  ──────────                            │
│  ⏳ 集成测试                    │  ⏳ 性能测试                           │
│  ⏳ UI 适配                    │  ⏳ 错误处理                           │
│  ⏳ 用户体验优化                │  ⏳ 日志完善                           │
│  ⏳ 准备发布                    │  ⏳ 准备部署                           │
└─────────────────────────────────────────────────────────────────────────┘
```

### 接口约定

为了方便并行开发，双方需要遵循以下约定：

#### API 端点约定

| 端点 | 方法 | 参数 | 响应格式 | 状态 |
|------|------|------|---------|------|
| `/api/result` | GET | `q`, `page`, `page_size`, `unique`, `color`, `cmc`, `type`, `rarity`, `set` | `{success, count, items[], page, total_pages}` | ✅ 已实现 |
| `/api/cards/{id}` | GET | - | 卡牌对象 | ✅ 已实现 |
| `/api/random` | GET | - | 卡牌对象 | ✅ 已实现 |
| `/api/sets` | GET | - | `[{code, name, zh_name}]` | ⚠️ 待添加 zh_name |
| `/api/stats/popular` | GET | `limit` | `[{card_name, count}]` | ✅ 已实现 |

#### 数据字段约定

**必选字段** (当前已支持):
```json
{
  "id": 12345,
  "oracle_id": "abc123",
  "scryfall_id": "xyz789",
  "name": "Lightning Bolt",
  "zh_name": "闪电箭",
  "mana_cost": "{R}",
  "cmc": 1.0,
  "type_line": "Instant",
  "oracle_text": "Deal 3 damage...",
  "colors": ["R"],
  "rarity": "common",
  "set_code": "LEA",
  "set_name": "Limited Edition Alpha"
}
```

**推荐字段** (增强体验):
```json
{
  "image_uris": {
    "normal": "https://..."
  },
  "set_zh_name": "限定版 Alpha",
  "is_double_faced": 0,
  "back_face_name": null
}
```

### 开发分支策略

```
main (生产)
 └── dev/v4.3.0 (当前)
      └── feature/mtg-card-server-api (新建)
           ├── android-api-integration (Android 开发)
           └── server-api-enhancement (服务端开发)
```

---

## 测试方案

### API 测试

#### 手动测试用例

**测试工具**: Postman / cURL / 浏览器

**测试集合**: `/tests/api_test_collection.json`

```json
{
  "info": {
    "name": "MTG Card Server API Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "基础搜索",
      "request": {
        "method": "GET",
        "url": "http://182.92.109.160:3000/api/result?q=闪电箭",
        "description": "测试中文名搜索"
      }
    },
    {
      "name": "英文名搜索",
      "request": {
        "method": "GET",
        "url": "http://182.92.109.160:3000/api/result?q=Lightning+Bolt"
      }
    },
    {
      "name": "颜色筛选",
      "request": {
        "method": "GET",
        "url": "http://182.92.109.160:3000/api/result?q=&type=creature&color=U"
      }
    },
    {
      "name": "法术力值筛选",
      "request": {
        "method": "GET",
        "url": "http://182.92.109.160:3000/api/result?q=&cmc=>3"
      }
    },
    {
      "name": "分页测试",
      "request": {
        "method": "GET",
        "url": "http://182.92.109.160:3000/api/result?q=龙&page=1&page_size=10"
      }
    },
    {
      "name": "单卡详情",
      "request": {
        "method": "GET",
        "url": "http://182.92.109.160:3000/api/cards/12345"
      }
    },
    {
      "name": "随机卡牌",
      "request": {
        "method": "GET",
        "url": "http://182.92.109.160:3000/api/random"
      }
    }
  ]
}
```

#### 自动化测试

**服务端测试文件**: `tests/api.test.js`

```javascript
const request = require('supertest');
const app = require('../app');

describe('MTG Card Server API Tests', () => {

  describe('GET /api/result', () => {

    test('应该返回成功响应', async () => {
      const response = await request(app)
        .get('/api/result')
        .query({ q: '闪电箭' });

      expect(response.status).toBe(200);
      expect(response.body.success).toBe(true);
      expect(Array.isArray(response.body.items)).toBe(true);
    });

    test('应该支持中文搜索', async () => {
      const response = await request(app)
        .get('/api/result')
        .query({ q: '闪电箭' });

      expect(response.body.items.length).toBeGreaterThan(0);
      expect(response.body.items[0].zh_name).toContain('闪电');
    });

    test('应该支持颜色筛选', async () => {
      const response = await request(app)
        .get('/api/result')
        .query({ color: 'R' });

      response.body.items.forEach(card => {
        expect(card.colors).toContain('R');
      });
    });

    test('应该支持法术力值筛选', async () => {
      const response = await request(app)
        .get('/api/result')
        .query({ cmc: '>3' });

      response.body.items.forEach(card => {
        expect(card.cmc).toBeGreaterThan(3);
      });
    });

    test('应该支持分页', async () => {
      const response1 = await request(app)
        .get('/api/result')
        .query({ q: '龙', page: 1, page_size: 5 });

      const response2 = await request(app)
        .get('/api/result')
        .query({ q: '龙', page: 2, page_size: 5 });

      expect(response1.body.page).toBe(1);
      expect(response2.body.page).toBe(2);
      expect(response1.body.items).not.toEqual(response2.body.items);
    });
  });

  describe('GET /api/cards/:id', () => {

    test('应该返回单张卡牌详情', async () => {
      const response = await request(app)
        .get('/api/cards/1');

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('name');
      expect(response.body).toHaveProperty('oracle_id');
    });

    test('无效 ID 应返回 404', async () => {
      const response = await request(app)
        .get('/api/cards/999999999');

      expect(response.status).toBe(404);
    });
  });

  describe('GET /api/random', () => {

    test('应该返回随机卡牌', async () => {
      const response1 = await request(app)
        .get('/api/random');

      const response2 = await request(app)
        .get('/api/random');

      expect(response1.status).toBe(200);
      expect(response2.status).toBe(200);
      // 随机卡牌可能相同，但大概率不同
    });
  });
});
```

### Android 端测试

**测试文件**: `app/src/androidTest/java/com/mtgo/decklistmanager/api/MtgCardServerApiTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class MtgCardServerApiTest {

    @Inject
    lateinit var api: MtgCardServerApi

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testBasicSearch() = runTest {
        val response = api.searchCard(
            query = "闪电箭",
            page = 1,
            pageSize = 20
        )

        assertTrue(response.isSuccessful)
        assertTrue(response.body()?.success == true)
        assertTrue((response.body()?.items?.size ?: 0) > 0)
    }

    @Test
    fun testColorFilter() = runTest {
        val response = api.searchCard(
            query = "",
            page = 1,
            pageSize = 20
            // TODO: 添加颜色筛选参数
        )

        assertTrue(response.isSuccessful)
        // 验证返回的卡牌都包含指定颜色
    }

    @Test
    fun testPagination() = runTest {
        val page1 = api.searchCard(
            query = "龙",
            page = 1,
            pageSize = 10
        )

        val page2 = api.searchCard(
            query = "龙",
            page = 2,
            pageSize = 10
        )

        assertTrue(page1.isSuccessful)
        assertTrue(page2.isSuccessful)
        assertNotEquals(page1.body()?.items, page2.body()?.items)
    }
}
```

---

## 部署清单

### 服务端部署

#### 服务器环境准备

**操作系统**: Linux (Ubuntu/CentOS)

**依赖检查**:
```bash
# Node.js 版本
node --version  # >= 16.x

# npm 版本
npm --version   # >= 8.x

# SQLite3
sqlite3 --version
```

**安装步骤**:
```bash
# 1. 克隆代码
git clone <repository-url>
cd mtg-card-server

# 2. 安装依赖
npm install

# 3. 配置环境变量
cp .env.example .env
vim .env

# 4. 初始化数据库
npm run db:migrate

# 5. 启动服务
npm start
# 或使用 PM2
pm2 start npm --name "mtg-card-server" -- start

# 6. 配置反向代理 (Nginx)
# /etc/nginx/sites-available/mtg-card-server
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}

# 7. 启用配置
sudo ln -s /etc/nginx/sites-available/mtg-card-server /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

#### 数据库备份

```bash
# 备份脚本
#!/bin/bash
# backup.sh

BACKUP_DIR="/backups/mtg-card-server"
DATE=$(date +%Y%m%d_%H%M%S)
DB_PATH="./data/cards.db"

mkdir -p $BACKUP_DIR
cp $DB_PATH $BACKUP_DIR/cards_$DATE.db

# 保留最近 7 天的备份
find $BACKUP_DIR -name "cards_*.db" -mtime +7 -delete

echo "Backup completed: cards_$DATE.db"
```

#### 监控和日志

```bash
# PM2 监控
pm2 monit

# 日志查看
pm2 logs mtg-card-server

# 日志轮转配置
# /etc/logrotate.d/mtg-card-server
/home/user/mtg-card-server/logs/*.log {
    daily
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 user user
    sharedscripts
}
```

### Android 端部署

#### 构建配置

**文件**: `app/build.gradle`

```gradle
android {
    // ...

    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'

            // API Base URL (通过 BuildConfig)
            buildConfigField "String", "MTG_CARD_SERVER_BASE_URL",
                "\"http://182.92.109.160:3000/\""
        }

        debug {
            buildConfigField "String", "MTG_CARD_SERVER_BASE_URL",
                "\"http://182.92.109.160:3000/\""
        }
    }
}
```

#### 版本发布

**版本号**: v4.4.0
**versionCode**: 86

**发布检查清单**:
- [ ] 所有单元测试通过
- [ ] API 集成测试通过
- [ ] 真机测试完成
- [ ] ProGuard 混淆配置正确
- [ ] 更新日志准备完毕
- [ ] APK/AAB 文件生成

**发布命令**:
```bash
# 构建 Release APK
./gradlew assembleRelease

# 构建 App Bundle (推荐用于 Google Play)
./gradlew bundleRelease

# 输出位置
# APK: app/build/outputs/apk/release/
# AAB: app/build/outputs/bundle/release/
```

---

## 附录

### A. 常见问题 FAQ

**Q1: 如何处理 API 不可用的情况？**

A: Android 端应该：
1. 显示友好的错误提示
2. 提供重试机制
3. 缓存最近的搜索结果
4. 支持离线模式（仅限已缓存数据）

**Q2: 如何同步 Android 和服务端的开发？**

A: 建议使用以下工具：
- **API 文档**: Swagger/OpenAPI
- **接口测试**: Postman
- **版本控制**: Git 分支
- **沟通工具**: 每日站会

**Q3: 图片加载慢怎么办？**

A: 优化方案：
1. 使用 Glide 的缓存机制
2. 预加载常用卡牌图片
3. 支持低分辨率占位图
4. 考虑使用 CDN

**Q4: 如何处理双面牌？**

A: 当前方案：
- 服务端返回 `is_double_faced` 标识
- Android 端根据标识决定显示逻辑
- 后续可扩展：服务端返回完整的 `card_faces` 数据

### B. 通信协议

**请求日志格式**:
```
[Android] → [Server]
{
  "timestamp": "2026-02-13T10:30:45Z",
  "endpoint": "/api/result",
  "params": { "q": "闪电箭", "page": 1 },
  "user_agent": "MTGDecklistManager/4.4.0 (Android)"
}

[Server] → [Android]
{
  "timestamp": "2026-02-13T10:30:46Z",
  "status": 200,
  "response_time_ms": 120,
  "success": true,
  "count": 1
}
```

### C. 相关文件索引

#### Android 端

| 文件 | 路径 | 说明 |
|------|------|------|
| API 接口 | `data/remote/api/mtgserver/MtgCardServerApi.kt` | Retrofit 接口 |
| 数据模型 | `data/remote/api/mtgserver/MtgCardServerDto.kt` | DTO 定义 |
| 数据映射 | `data/remote/api/mtgserver/MtgCardServerMapper.kt` | 转换逻辑 |
| 依赖注入 | `di/AppModule.kt` | Hilt 模块 |
| 搜索功能 | `ui/search/SearchViewModel.kt` | 搜索逻辑 |
| 套牌仓库 | `data/repository/DecklistRepository.kt` | 数据获取 |

#### 服务端

| 文件 | 路径 | 说明 |
|------|------|------|
| API 路由 | `routes/api.js` | 端点定义 |
| 数据库 | `database/db.js` | SQLite 连接 |
| 数据库迁移 | `migrations/` | SQL 迁移脚本 |
| 环境配置 | `.env` | 配置变量 |
| 单元测试 | `tests/api.test.js` | API 测试 |
| 部署脚本 | `deploy.sh` | 部署流程 |

---

**文档版本**: 1.0
**最后更新**: 2026-02-13
**维护者**: MTG Decklist Manager Team

---

## 快速参考卡片

### Android 端命令速查

```bash
# 构建项目
./gradlew assembleDebug

# 运行测试
./gradlew test

# 安装到设备
./gradlew installDebug

# 查看日志
adb logcat | grep "DecklistManager"

# 生成签名 APK
./gradlew assembleRelease
```

### 服务端命令速查

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 运行测试
npm test

# 数据库迁移
npm run db:migrate

# PM2 启动
pm2 start npm -- start

# 查看日志
pm2 logs mtg-card-server
```

### API 端点速查

```
GET  /api/result?q={query}          # 搜索卡牌
GET  /api/cards/{id}               # 单卡详情
GET  /api/random                   # 随机卡牌
GET  /api/sets                     # 所有系列
GET  /api/stats/popular?limit=50   # 热门卡牌
```
