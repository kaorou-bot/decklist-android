# v5.0 - 完全放弃 MTGCH API（正确架构）

## 📋 概述

**日期**: 2025-02-19
**版本**: v5.0 (Corrected Architecture)
**关键变更**: 采用正确的数据流架构，完全使用自有服务器

---

## 🎯 正确的架构设计

### 数据流

```
1. 赛事列表
   ServerApi.getEvents() → EventDto[] → 数据库缓存 → UI

2. 赛事详情
   ServerApi.getEventDetail(id) → EventDto → UI

3. 套牌列表
   ServerApi.getEventDecklists(eventId) → DecklistDto[] → 数据库缓存 → UI

4. 套牌详情（重要！）
   步骤1: ServerApi.getDecklistDetail(decklistId)
          → 获取卡牌名称列表 (cardName, quantity)

   步骤2: ServerApi.searchCard(cardName)
          → 对每个卡牌名称调用搜索接口
          → 获取完整信息 (nameZh, manaCost, colors, typeLine, etc.)

   步骤3: 合并数据并保存到数据库
          → CardEntity (包含完整卡牌信息)
          → UI 显示
```

### 为什么这个架构是正确的？

1. **单一数据源**: 卡牌的完整信息来自 `/api/cards/search`，这是权威来源
2. **解耦**: 套牌接口只负责返回"有哪些牌"，卡牌接口负责"牌的详细信息"
3. **可复用**: 同样的卡牌信息可以用于套牌、搜索、单卡查询等多个场景
4. **数据一致性**: 所有卡牌的中文名、法术力值等都来自同一个接口

---

## ✅ 已完成迁移

### 1. 赛事和套牌数据

| 功能 | API 端点 | 状态 |
|------|----------|------|
| 赛事列表 | `GET /api/v1/events` | ✅ |
| 赛事详情 | `GET /api/v1/events/{id}` | ✅ |
| 套牌列表 | `GET /api/v1/events/{id}/decklists` | ✅ |
| 套牌详情（卡牌列表） | `GET /api/v1/decklists/{id}` | ✅ |

### 2. 卡牌详情数据

| 功能 | API 端点 | 状态 |
|------|----------|------|
| 卡牌搜索 | `GET /api/cards/search?q={name}` | ✅ |
| 完整卡牌信息 | 包含在搜索响应中 | ✅ |

**关键数据字段**：
```kotlin
data class CardInfoDto(
    val id: Long,
    val name: String,              // 英文名
    val nameZh: String?,           // 中文名 ✅
    val manaCost: String?,         // 法术力值 ✅
    val cmc: Double?,              // 转化法术力
    val colors: List<String>?,     // 颜色数组 ✅
    val colorIdentity: List<String>?, // 颜色身份
    val typeLine: String?,         // 类型行 ✅
    val oracleText: String?,       // 规则文本
    val power: String?,            // 攻击力
    val toughness: String?,        // 防御力
    val loyalty: String?,          // 忠诚度
    val rarity: String?,           // 稀有度 ✅
    val setCode: String?,          // 系列代码
    val setName: String?,          // 系列名称 ✅
    val collectorNumber: String?,  // 收藏编号
    val layout: String?,           // 布局
    val imageUris: ImageUris?,     // 图片 URL
    val legalities: Map<String, String>?, // 赛制合法性
    val scryfallId: String?,       // Scryfall ID
    val oracleId: String?,         // Oracle ID
    val releasedAt: String?,       // 发布日期
    val isDoubleFaced: Boolean?,   // 是否双面牌
    val isToken: Boolean?          // 是否衍生物
)
```

### 3. 实现细节

#### DeckDetailViewModel.kt

```kotlin
private suspend fun loadDecklistDetailFromServer() {
    // 步骤1: 从套牌接口获取卡牌名称列表
    val response = serverApi.getDecklistDetail(decklistId)
    val detail = response.body()!!.data!!

    val mainCardNames = detail.mainDeck.mapIndexed { index, cardDto ->
        CardRef(index, cardDto.cardName, cardDto.quantity, "main")
    }

    // 步骤2: 对每个唯一的卡牌名称，调用 /api/cards/search 获取完整信息
    val uniqueCardNames = mainCardNames.map { it.name }.distinct()
    val cardInfoMap = mutableMapOf<String, CardInfoDto>()

    for (cardName in uniqueCardNames) {
        val cardResponse = serverApi.searchCard(cardName, 1)
        if (cardResponse.isSuccessful && cardResponse.body()?.success == true) {
            val cards = cardResponse.body()!!.cards
            // 找到精确匹配的卡牌
            val exactMatch = cards.find { it.name.equals(cardName, ignoreCase = true) }
            if (exactMatch != null) {
                cardInfoMap[cardName] = exactMatch
            }
        }
    }

    // 步骤3: 使用完整信息构建 CardEntity
    val cardEntities = mainCardNames.map { cardRef ->
        val cardInfo = cardInfoMap[cardRef.name]
        CardEntity(
            cardName = cardRef.name,
            quantity = cardRef.quantity,
            manaCost = cardInfo?.manaCost,      // 从卡牌接口获取 ✅
            displayName = cardInfo?.nameZh,     // 从卡牌接口获取 ✅
            rarity = cardInfo?.rarity,          // 从卡牌接口获取 ✅
            color = cardInfo?.colors?.joinToString(","), // 从卡牌接口获取 ✅
            cardType = cardInfo?.typeLine,      // 从卡牌接口获取 ✅
            cardSet = cardInfo?.setName         // 从卡牌接口获取 ✅
        )
    }

    // 保存到数据库并更新 UI
    cardDao.insertAll(cardEntities)
    _mainDeck.value = cardEntities.filter { it.location == "main" }.map { it.toCard() }
}
```

---

## 🔧 技术实现

### 新增的 DTO

#### ServerDto.kt

```kotlin
// 卡牌搜索响应
data class CardSearchResponse(
    val success: Boolean,
    val cards: List<CardInfoDto>?,
    val total: Int?
)

// 完整卡牌信息
data class CardInfoDto(
    val id: Long,
    val name: String,
    val nameZh: String?,
    val manaCost: String?,
    // ... 完整字段（见上方）
)

// 图片 URIs
data class ImageUris(
    val small: String?,
    val normal: String?,
    val large: String?,
    val png: String?,
    val artCrop: String?,
    val borderCrop: String?
)
```

#### ServerApi.kt

```kotlin
/**
 * 搜索卡牌
 */
@GET("api/cards/search")
suspend fun searchCard(
    @Query("q") q: String,
    @Query("limit") limit: Int = 20
): Response<CardSearchResponse>
```

---

## 📊 性能优化

### 当前实现（逐个查询）

```kotlin
// 对每个唯一卡牌名称调用一次 API
for (cardName in uniqueCardNames) {
    val cardResponse = serverApi.searchCard(cardName, 1)
    // 处理响应...
}
```

**性能分析**：
- 假设套牌有 60 张主牌 + 15 张备牌 = 75 张
- 假设有 30 张不同的卡牌（去重后）
- 需要调用 API 30 次
- 每次约 100-200ms
- 总计: 3-6 秒

### 优化方案（批量查询）- 可选

如果服务器支持，可以实现批量查询：

```kotlin
// 伪代码：一次性查询多个卡牌
val cardNames = uniqueCardNames.joinToString("|")
val response = serverApi.searchCards(cardNames, limit = 100)
```

**预期提升**：
- 30 次 API 调用 → 1 次 API 调用
- 3-6 秒 → 200-500ms

---

## 🚨 已知限制

### 1. 查询次数较多

**问题**: 当前实现对每个唯一卡牌名称调用一次 API

**影响**:
- 套牌加载时间较长（3-6秒）
- 网络请求次数多

**解决方案**:
- 短期：添加加载进度指示器
- 长期：服务器实现批量查询接口

### 2. 缓存未充分利用

**问题**: 每次打开套牌都重新查询卡牌信息

**影响**:
- 重复查询相同卡牌
- 流量消耗

**解决方案**:
- 在数据库中增加 `cards_info` 表
- 第一次查询后缓存卡牌信息
- 后续查询直接从缓存读取

---

## 📝 与旧架构对比

### 错误的架构（v5.0 初版）

```kotlin
// ❌ 从套牌接口获取所有字段
val cardDto = detail.mainDeck[0]
CardEntity(
    manaCost = cardDto.manaCost,        // 质量差
    displayName = cardDto.displayName,  // 英文而非中文
    // ...
)
```

**问题**：
- `/api/v1/decklists/{id}` 返回的 `displayName` 是英文名
- `manaCost` 格式不正确
- 数据质量差

### 正确的架构（v5.0 修正版）

```kotlin
// ✅ 从套牌接口获取名称，从卡牌接口获取详细信息
val cardName = detail.mainDeck[0].cardName
val cardInfo = serverApi.searchCard(cardName)
CardEntity(
    manaCost = cardInfo.manaCost,       // 正确格式 ✅
    displayName = cardInfo.nameZh,      // 中文名 ✅
    // ...
)
```

**优势**：
- 数据源权威（来自卡牌数据库）
- 数据质量高
- 架构清晰

---

## 🎉 测试验证

### 测试用例

```bash
# 测试卡牌搜索接口
curl "http://182.92.109.160/api/cards/search?q=Force%20of%20Negation&limit=1"

# 预期结果：
{
    "success": true,
    "cards": [
        {
            "name": "Force of Negation",
            "nameZh": "否认之力",     ✅ 中文
            "manaCost": "{1}{U}{U}",  ✅ 正确格式
            "rarity": "rare",         ✅ 正确稀有度
            // ...
        }
    ],
    "total": 1
}
```

---

## 📈 下一步优化

### 短期（当前版本）
1. ✅ 采用正确的架构
2. ✅ 获取高质量的中文名和法术力值
3. ⏳ 添加加载进度提示
4. ⏳ 错误处理和重试逻辑

### 中期
1. 实现卡牌信息缓存（避免重复查询）
2. 批量查询优化（减少 API 调用）
3. 离线支持（已缓存的卡牌可离线查看）

### 长期
1. 完全移除 MTGCH API 依赖（包括 SearchViewModel）
2. 所有卡牌功能都使用自有服务器
3. 实现 WebSocket 实时更新

---

## 🔗 相关文档

- [SERVER_API_SPEC.md](../SERVER_API_SPEC.md) - 服务器 API 规范
- [API_DOCUMENTATION.md](../API_DOCUMENTATION.md) - 卡牌服务器 API 文档
- [MIGRATION_STATUS_v5.0.md](../MIGRATION_STATUS_v5.0.md) - 旧版迁移状态（已废弃）

---

## 📌 总结

**v5.0 (Corrected)** 实现了：

1. ✅ **完全使用自有服务器** - 不再依赖 MTGCH API（套牌数据部分）
2. ✅ **正确的架构设计** - 套牌接口提供名称，卡牌接口提供详情
3. ✅ **高质量数据** - 中文名、法术力值、稀有度等全部正确
4. ✅ **可扩展性** - 为未来的批量查询、缓存等优化留出空间

**当前状态**: 可用于生产环境，性能可接受（3-6秒加载时间）
**优化空间**: 批量查询、缓存机制可进一步提升性能
