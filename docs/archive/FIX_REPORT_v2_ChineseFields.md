# 修复报告 v2 - 完整中文字段支持

**日期**: 2025-02-19
**版本**: v5.1.0
**状态**: 卡牌详情已修复 ✅ | 搜索功能待修复 ⚠️

---

## ✅ 已完成修复

### 1. 卡牌详情使用 ServerApi

**文件**: `DecklistRepository.kt`
- ✅ 添加 `ServerApi` 依赖注入
- ✅ `fetchCardInfoFromApi()` 改用 `serverApi.searchCard()`
- ✅ 不再依赖返回 404 的 MTGCH API

### 2. 完整中文字段支持

**文件**: `ServerDto.kt` 和 `ServerMapper.kt`

#### 新增中文字段到 CardInfoDto:
```kotlin
data class CardInfoDto(
    // ... 英文字段
    val typeLine: String?,
    val typeLineZh: String?,        // ✅ 中文类型行
    val oracleText: String?,
    val oracleTextZh: String?,      // ✅ 中文规则文本
    val setName: String?,
    val setNameZh: String?,         // ✅ 中文系列名称
    val faceIndex: Int? = null      // 面索引（双面牌）
)
```

#### ServerMapper 优先使用中文:
```kotlin
fun CardInfoDto.toCardInfo(): CardInfo {
    return CardInfo(
        name = nameZh ?: name,                    // ✅ 中文名
        typeLine = typeLineZh ?: typeLine,        // ✅ 中文类型行
        oracleText = oracleTextZh ?: oracleText,  // ✅ 中文规则文本
        setName = setNameZh ?: setName,           // ✅ 中文系列名称
        // ...
    )
}
```

---

## 📊 测试结果

### 服务器 API 返回示例

```json
{
    "success": true,
    "cards": [{
        "id": 46469,
        "name": "Force of Negation",
        "nameZh": "否认之力",                    // ✅
        "manaCost": "{1}{U}{U}",
        "typeLine": "Instant",
        "typeLineZh": "瞬间",                    // ✅
        "oracleText": "If it's not your turn...",
        "oracleTextZh": "如果当前不是你的回合...", // ✅
        "setName": "Modern Horizons",
        "setNameZh": "摩登新篇"                   // ✅
    }]
}
```

### 应用日志验证

```
✅ 中文名称: 虚妄, 众望传谕, 灾祸卜算师格拉布
✅ 法术力值: {4}{U/B}{U/B}, {2}{G}, {B}{G}{U}
✅ 类型行: 生物 — 龙 // 法术 — 预兆
✅ 系列名称: (使用 setNameZh 字段)
```

---

## ⚠️ 待修复问题

### SearchViewModel 仍使用 MTGCH API

**受影响功能**:
- ❌ 卡牌搜索功能
- ❌ 印刷版本查询
- ❌ 双面牌背面详情

**日志证据**:
```
D SearchViewModel: 获取印刷版本: 9ae13026-960a-4d31-b775-d47209a1e313
E SearchViewModel: 获取印刷版本失败: 404

D SearchViewModel: 按名称搜索印刷版本: 篡位者亚丹
E SearchViewModel: 按名称搜索失败: 404
```

**需要修改的文件**:
- `SearchViewModel.kt` (第 94, 406, 435, 462 行)

---

## 🔍 API 对比

| 功能 | MTGCH API | 自有服务器 | 状态 |
|------|-----------|-----------|------|
| 赛事列表 | - | `/api/v1/events` | ✅ |
| 套牌详情 | - | `/api/v1/decklists/{id}` | ✅ |
| 卡牌搜索 | `/api/cards/search` (404) | `/api/cards/search` | ✅ |
| 单卡详情 | `/api/cards/{id}` (404) | ? | ⚠️ 需实现 |
| 印刷版本 | `/api/cards/{id}/printings` (404) | ? | ⚠️ 需实现 |

---

## 📝 修改文件列表

| 文件 | 操作 | 说明 |
|------|------|------|
| `ServerDto.kt` | 修改 | 添加 typeLineZh, oracleTextZh, setNameZh 字段 |
| `ServerMapper.kt` | 修改 | 优先使用中文字段进行映射 |
| `DecklistRepository.kt` | 修改 | 使用 ServerApi 替代 MTGCH API |
| `FIX_REPORT_CardDetails.md` | 新建 | 修复报告文档 |

---

## 🎯 下一步计划

### 立即需要（高优先级）
1. ⏳ 修复 SearchViewModel 使用 ServerApi
2. ⏳ 测试搜索功能
3. ⏳ 实现单卡详情 API（如服务器尚未支持）

### 短期优化
1. 实现卡牌信息缓存
2. 批量查询优化（减少 API 调用）
3. 离线支持

### 长期计划
1. 完全移除 MTGCH API 依赖
2. WebSocket 实时更新
3. 推送通知

---

## 💡 技术要点

### 中文字段优先级策略

```kotlin
// 正确的做法 ✅
name = nameZh ?: name
typeLine = typeLineZh ?: typeLine
oracleText = oracleTextZh ?: oracleText
setName = setNameZh ?: setName

// 错误的做法 ❌
name = name  // 忽略中文字段
typeLine = typeLine
```

### API 调用对比

```kotlin
// 旧代码 (MTGCH API - 返回 404) ❌
val response = mtgchApi.searchCard(
    query = cardName,
    limit = 20
)

// 新代码 (自有服务器 - 正常工作) ✅
val response = serverApi.searchCard(
    q = cardName,
    limit = 20
)
```

---

## 📌 总结

**已修复** ✅:
- 卡牌详情页面正常加载
- 中文名称、类型行、规则文本、系列名称全部使用中文
- 法术力值格式正确显示

**待修复** ⚠️:
- SearchViewModel 仍需改用 ServerApi
- 搜索功能和印刷版本查询暂时不可用

**用户可以正常使用**:
- ✅ 浏览赛事和套牌
- ✅ 查看套牌详情
- ✅ 点击卡牌查看详情
- ✅ 收藏功能
- ❌ 搜索卡牌（待修复）

---

**状态**: 等待用户测试反馈
**下一步**: 修复 SearchViewModel（如用户确认卡牌详情正常）
