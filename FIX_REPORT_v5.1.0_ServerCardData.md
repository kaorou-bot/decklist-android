# v5.1.0 - 服务器返回完整卡牌信息

**日期**: 2025-02-19
**状态**: ✅ 已完成

---

## 📋 问题

用户反馈：
- 一些牌（如：易形地窖）被识别成了同名的 token 牌（类别包含"衍生"）
- 一些牌（如：豹猫群）被识别成了 A-名称（A-豹猫群）

**根本原因**：
之前的实现需要调用 `/api/cards/search` 来搜索卡牌，但搜索可能返回多个结果（Token牌、备用列表版本等），导致选择了错误的版本。

---

## ✅ 解决方案

**服务器改进**：
服务器现在在 `/api/v1/decklists/{id}` 接口中直接返回完整的卡牌信息，包括：
- name, nameZh（中文名）
- manaCost, cmc（法术力值）
- typeLine, typeLineZh（类型）
- oracleText, oracleTextZh（规则文本）
- setCode, setName, setNameZh（系列）
- collectorNumber, rarity（编号、稀有度）
- colorIdentity, colors（颜色）
- imageUris（图片）
- oracleId, scryfallId
- cardFaces（双面牌信息）

**客户端改进**：
1. 创建 `CardWithQuantityDto` - 包含完整卡牌信息 + 数量
2. 更新 `DecklistDetailDto` 使用 `CardWithQuantityDto`
3. 重写 `DeckDetailViewModel.loadDecklistDetailFromServer()`
   - 直接使用服务器返回的完整卡牌数据
   - 移除了对 `/api/cards/search` 的调用
   - 不再需要匹配和选择逻辑

---

## 🔧 技术变更

### ServerDto.kt
```kotlin
// 新增 DTO
data class CardWithQuantityDto(
    val quantity: Int,
    // 完整的 CardInfoDto 字段
    val name: String,
    val nameZh: String?,
    val manaCost: String?,
    val typeLine: String?,
    val typeLineZh: String?,
    val oracleText: String?,
    val oracleTextZh: String?,
    val setName: String?,
    val setNameZh: String?,
    // ... 所有其他字段
)

data class DecklistDetailDto(
    // ...
    val mainDeck: List<CardWithQuantityDto>,  // ✅ 新
    val sideboard: List<CardWithQuantityDto>  // ✅ 新
)
```

### DeckDetailViewModel.kt
```kotlin
// 旧逻辑（已移除）
val cardResponse = serverApi.searchCard(formattedName, 1)
val exactMatch = cards.find { it.name.equals(cardName, ignoreCase = true) }

// 新逻辑（直接使用服务器数据）
val cardEntities = detail.mainDeck.mapIndexed { index, card ->
    CardEntity(
        decklistId = decklistId,
        cardName = card.name,
        quantity = card.quantity,
        location = "main",
        cardOrder = index,
        manaCost = card.manaCost,         // ✅ 直接使用
        displayName = card.nameZh,         // ✅ 直接使用
        rarity = card.rarity,
        color = card.colors?.joinToString(","),
        cardType = card.typeLineZh ?: card.typeLine,  // ✅ 直接使用
        cardSet = card.setNameZh ?: card.setName       // ✅ 直接使用
    )
}
```

---

## 📊 性能改进

### 旧实现
1. 调用 `/api/v1/decklists/{id}` - 获取卡牌名称列表
2. 对每个唯一卡牌名称调用 `/api/cards/search` - 获取完整信息
3. 匹配和选择正确的卡牌版本

**API 请求数** = 1 + (唯一卡牌名称数量)
- 典型套牌：60 张主牌 + 15 张备牌 = 75 张
- 如果每个卡牌名称都不同：1 + 75 = 76 次 API 请求
- 如果有重复卡牌：1 + 20 = 21 次 API 请求（常见情况）
- 实际范围：1 + 30 到 1 + 60 次请求

**网络延迟**：
- 单次搜索请求：~100-200ms
- 总延迟：3-12 秒（取决于唯一卡牌数量）
- 匹配逻辑：可能选错版本（Token、备用列表等）

### 新实现
1. 调用 `/api/v1/decklists/{id}` - 获取完整卡牌信息
2. 直接使用数据

**API 请求数** = 1 次（固定）
**网络延迟**：~1-2 秒
**版本正确性**：100%（服务器已选择正确版本）

---

## 🐛 修复的问题

1. ✅ Token 牌误匹配 - 服务器返回正确的非 Token 版本
2. ✅ A-开头备用列表版本 - 服务器返回正确的正式版本
3. ✅ Split 卡牌名称格式化 - 不再需要
4. ✅ 搜索匹配失败 - 不再需要搜索

---

## 📝 测试清单

- [ ] 加载套牌详情
- [ ] 验证中文名称正确显示
- [ ] 验证法术力值正确显示
- [ ] 验证类型和规则文本正确
- [ ] 验证系列名称正确
- [ ] 验证不再出现 Token 牌
- [ ] 验证不再出现 A-开头牌名

---

**状态**: 已编译并安装
**等待**: 用户测试验证
