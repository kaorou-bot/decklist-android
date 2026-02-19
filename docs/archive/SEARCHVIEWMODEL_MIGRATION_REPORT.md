# SearchViewModel 迁移报告

**日期**: 2025-02-19
**版本**: v5.1.0
**状态**: 核心搜索功能已迁移 ⚠️ (印刷版本功能待实现)

---

## ✅ 已完成

### 1. SearchViewModel 核心搜索功能

**修改前**:
```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    val mtgchApi: MtgchApi,  // ❌ MTGCH API 返回 404
    private val searchHistoryDao: SearchHistoryDao
)
```

**修改后**:
```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val serverApi: ServerApi,  // ✅ 自有服务器
    private val searchHistoryDao: SearchHistoryDao
)
```

### 2. 搜索方法迁移

**SearchViewModel.search()**:
```kotlin
// 旧代码（MTGCH API）
val response = mtgchApi.searchCard(
    query = searchQuery,
    offset = offset,
    limit = limit
)

// 新代码（ServerApi）
val response = serverApi.searchCard(
    q = searchQuery,
    limit = limit
)
```

### 3. 数据模型迁移

**toSearchResultItem() 扩展函数**:
```kotlin
// 旧代码
private fun MtgchCardDto.toSearchResultItem(): SearchResultItem

// 新代码
private fun CardInfoDto.toSearchResultItem(): SearchResultItem {
    return SearchResultItem(
        name = nameZh ?: name,           // ✅ 中文名
        typeLine = typeLineZh ?: typeLine, // ✅ 中文类型
        oracleText = oracleTextZh ?: oracleText, // ✅ 中文规则文本
        setName = setNameZh ?: setName,  // ✅ 中文系列
        serverCard = this  // ✅ 使用 CardInfoDto
    )
}
```

### 4. SearchActivity.kt 更新

**显示卡牌详情**:
```kotlin
// 旧代码
val mtgchCard = result.mtgchCard ?: return
val cardInfo = CardDetailHelper.buildCardInfo(mtgchCard, ...)

// 新代码
val serverCard = result.serverCard ?: return
val cardInfo = serverCard.toCardInfo()  // 直接使用 ServerMapper
```

---

## ⚠️ 待实现功能

### 印刷版本查询

以下功能因服务器尚未提供相应 API，暂时禁用：

1. **getCardPrintings()**
   - 用途：获取卡牌的所有印刷版本
   - 状态：返回 null
   - 影响：CardInfoFragment 无法显示印刷版本选择器

2. **searchCardPrintingsByName()**
   - 用途：按名称搜索印刷版本
   - 状态：返回空列表
   - 影响：无法通过卡牌名称查找其他印刷版本

### 编译错误

由于以下文件仍引用 `MtgchCardDto` 和已删除的方法，存在编译错误：

- `CardInfoFragment.kt` - 使用 `getCardPrintings()` 和 `searchCardPrintingsByName()`
- `PrintingSelectorDialog.kt` - 期望 `List<MtgchCardDto>` 但收到 `List<CardInfoDto>`

---

## 🔧 解决方案

### 选项 1：快速修复（推荐）

**禁用印刷版本功能**，注释掉 CardInfoFragment 中的相关代码：
- 第 137-150 行：加载印刷版本
- 第 207-220 行：回退到直接搜索
- 第 244-265 行：印刷版本回退逻辑

**优点**：
- 快速恢复编译
- 核心搜索功能正常工作
- 用户可以搜索和查看卡牌详情

**缺点**：
- 暂时无法查看卡牌的其他印刷版本
- 无法切换不同版本的卡牌图片

### 选项 2：完整修复

**在服务器实现印刷版本 API**：
```
GET /api/cards/{oracleId}/printings?limit=20&offset=0
GET /api/cards/printings?name={cardName}&limit=100
```

**优点**：
- 完整功能
- 所有代码使用统一的数据源

**缺点**：
- 需要服务器开发
- 时间较长

---

## 📊 迁移状态

| 功能 | MTGCH API | ServerApi | 状态 |
|------|-----------|-----------|------|
| 卡牌搜索 | ✅ (404) | ✅ | ✅ 已迁移 |
| 搜索结果显示 | ✅ | ✅ | ✅ 已迁移 |
| 卡牌详情弹窗 | ✅ | ✅ | ✅ 已迁移 |
| 高级筛选 | ✅ | ✅ | ✅ 已迁移 |
| 印刷版本查询 | ✅ (404) | ❌ | ⚠️ 待实现 |
| 印刷版本选择器 | ✅ | ❌ | ⚠️ 待实现 |

---

## 📝 修改文件列表

### 核心文件（已修改）
- `SearchViewModel.kt` - ✅ 核心搜索功能已迁移
- `SearchActivity.kt` - ✅ 使用 ServerMapper
- `SearchResultItem` - ✅ 使用 `serverCard` 字段

### 待修复文件（存在编译错误）
- `CardInfoFragment.kt` - ⚠️ 引用已删除的方法
- `PrintingSelectorDialog.kt` - ⚠️ 期望 MtgchCardDto

---

## 🎯 下一步行动

### 立即执行
1. 禁用 CardInfoFragment 中的印刷版本功能
2. 验证编译通过
3. 测试搜索功能是否正常工作

### 短期计划
1. 服务器实现印刷版本 API
2. 恢复 CardInfoFragment 印刷版本功能
3. 重新启用印刷版本选择器

---

## 💡 技术要点

### API 响应格式对比

**MTGCH API**:
```json
{
    "success": true,
    "cards": [MtgchCardDto],
    "total": 123
}
```

**ServerApi** (相同格式):
```json
{
    "success": true,
    "cards": [CardInfoDto],
    "total": 123
}
```

### 数据字段映射

| 功能 | MtgchCardDto | CardInfoDto |
|------|--------------|-------------|
| 中文名 | `nameZh` | `nameZh` ✅ |
| 中文类型 | `typeLineZh` | `typeLineZh` ✅ |
| 中文规则 | `oracleTextZh` | `oracleTextZh` ✅ |
| 中文系列 | `setNameZh` | `setNameZh` ✅ |
| 卡牌面 | `cardFaces` | `cardFaces` ✅ |
| 布局 | `layout` | `layout` ✅ |

---

## 📌 总结

**已完成** ✅:
- SearchViewModel 核心搜索功能迁移到 ServerApi
- SearchResultItem 使用 CardInfoDto
- SearchActivity 直接使用 ServerMapper
- 所有中文相关字段正确映射

**待完成** ⚠️:
- 禁用 CardInfoFragment 印刷版本功能（快速修复）
- 或实现服务器印刷版本 API（完整修复）

**建议**:
采用选项 1（快速修复），先禁用印刷版本功能，确保核心搜索功能可用。

---

**状态**: 等待用户确认修复方案
