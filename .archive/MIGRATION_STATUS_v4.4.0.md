# MTG Card Server API 迁移状态

> **更新日期**: 2026-02-14
> **当前版本**: v4.3.0 → v4.4.0
> **状态**: 进行中

---

## ✅ 已完成的工作

### 1. 新 API 接口文件创建

#### MtgCardServerApi.kt
**位置**: `app/src/main/java/com/mtgo/decklistmanager/data/remote/api/mtgserver/MtgCardServerApi.kt`

- ✅ 定义了 MTG Card Server API 接口
- ✅ 支持搜索、单卡详情、随机卡牌、系列列表、热门卡牌等端点
- ✅ Base URL: `http://182.92.109.160:3000/`

**主要方法**:
- `searchCard(query, page, pageSize, unique)` - 搜索卡牌
- `getCard(id)` - 获取单卡详情
- `getRandomCard()` - 获取随机卡牌
- `getAllSets()` - 获取所有系列
- `getPopularCards(limit)` - 获取热门卡牌

#### MtgCardServerDto.kt
**位置**: `app/src/main/java/com/mtgo/decklistmanager/data/remote/api/mtgserver/MtgCardServerDto.kt`

- ✅ 定义了 MTG Card Server 数据传输对象
- ✅ 包含所有必要字段：ID、名称、法术力、类型、颜色、稀有度等
- ✅ 支持双面牌标识
- ✅ 提供工具方法：`getDisplayName()`, `isDualFaced()`, `getScryfallImageUrl()`

#### MtgCardServerResponse.kt
**位置**: `app/src/main/java/com/mtgo/decklistmanager/data/remote/api/mtgserver/MtgCardServerResponse.kt`

- ✅ 定义了 API 响应格式
- ✅ 包含 `success`, `count`, `page`, `total_pages`, `items` 字段

#### MtgCardServerMapper.kt
**位置**: `app/src/main/java/com/mtgo/decklistmanager/data/remote/api/mtgserver/MtgCardServerMapper.kt`

- ✅ 提供了 DTO 到 Entity 的转换
- ✅ 提供了 DTO 到 SearchResultItem 的转换
- ✅ 正确处理中文名称优先逻辑

#### ScryfallImageApi.kt
**位置**: `app/src/main/java/com/mtgo/decklistmanager/data/remote/api/mtgserver/ScryfallImageApi.kt`

- ✅ 定义了 Scryfall 图片 API 接口（用于获取卡牌图片）
- ✅ Base URL: `https://api.scryfall.com/`
- ✅ 支持根据 ID 和 Oracle ID 获取卡牌详情

---

### 2. 依赖注入更新

#### AppModule.kt
**位置**: `app/src/main/java/com/mtgo/decklistmanager/di/AppModule.kt`

**已更新的配置**:
- ✅ 添加了 `provideMtgCardServerRetrofit()` - MTG Card Server Retrofit 实例
- ✅ 添加了 `provideScryfallRetrofit()` - Scryfall Retrofit 实例
- ✅ 添加了 `provideMtgCardServerApi()` - MTG Card Server API Provider
- ✅ 添加了 `provideScryfallImageApi()` - Scryfall Image API Provider
- ✅ 保留了旧的 `provideMtgchRetrofit()` 用于兼容（后续可移除）

---

### 3. SearchViewModel 更新

#### SearchViewModel.kt
**位置**: `app/src/main/java/com/mtgo/decklistmanager/ui/search/SearchViewModel.kt`

**已更新的功能**:
- ✅ 构造函数改为使用 `MtgCardServerApi` 替代 `MtgchApi`
- ✅ `search()` 方法更新为调用新 API
  - 使用 `mtgCardServerApi.searchCard()` 替代 `mtgchApi.searchCard()`
  - 检查响应格式：`response.body()?.success == true`
- ✅ `buildSearchQuery()` 方法简化为服务端支持的语法
  - 暂时仅支持基础筛选（颜色、类型、稀有度、CMC、系列）
  - TODO: 等待服务端实现高级筛选后更新
- ✅ `getFullCardDetails()` 方法更新为使用新 API（参数类型从 String 改为 Long）
- ✅ `SearchResultItem` 数据类更新为匹配新 API 结构

---

### 4. DecklistRepository 部分更新

#### DecklistRepository.kt
**位置**: `app/src/main/java/com/mtgo/decklistmanager/data/repository/DecklistRepository.kt`

**已更新的内容**:
- ✅ 导入语句更新：
  - 添加 `MtgCardServerApi` 导入
  - 添加 `ScryfallImageApi` 导入
  - 移除 `MtgchApi` 导入
- ✅ 构造函数参数更新：
  - 添加 `mtgCardServerApi: MtgCardServerApi`
  - 添加 `scryfallImageApi: ScryfallImageApi`
  - 移除 `mtgchApi: MtgchApi`

**待更新的方法**:
- ⚠️ `fetchScryfallDetails()` 方法仍使用旧的 API 调用
  - 需要将 `mtgchApi.searchCard()` 更新为 `mtgCardServerApi.searchCard()`
  - 需要适配新的响应格式和数据结构

---

## ⚠️ 待完成的工作

### 1. DecklistRepository.fetchScryfallDetails() 更新

**文件**: `app/src/main/java/com/mtgo/decklistmanager/data/repository/DecklistRepository.kt`
**方法**: `fetchScryfallDetails(decklistId: Long)` (第 220 行左右)

**需要修改的内容**:
```kotlin
// 当前代码（约第 289 行）
val response = mtgchApi.searchCard(
    query = formattedCardName,
    pageSize = 20,
    priorityChinese = true
)

// 需要改为
val response = mtgCardServerApi.searchCard(
    query = formattedCardName,
    pageSize = 20,
    unique = true
)
```

**响应格式适配**:
```kotlin
// 当前代码（约第 294-296 行）
if (response.isSuccessful && response.body() != null) {
    val searchResponse = response.body()!!
    val results = searchResponse.data

// 需要改为
if (response.isSuccessful && response.body()?.success == true) {
    val searchResponse = response.body()!!
    val results = searchResponse.items
```

**卡牌字段适配**:
```kotlin
// 当前代码（约第 306-320 行）
val exactMatch = results.find { card ->
    val nameMatch = card.name?.equals(formattedCardName, ignoreCase = true) == true
    val zhNameMatch = card.zhsName?.equals(cardName, ignoreCase = true) == true
    val translatedNameMatch = card.atomicTranslatedName?.equals(cardName, ignoreCase = true) == true
    // ...
}

// 需要改为（使用新字段）
val exactMatch = results.find { card ->
    val nameMatch = card.name?.equals(formattedCardName, ignoreCase = true) == true
    val zhNameMatch = card.zhName?.equals(cardName, ignoreCase = true) == true
    val faceNameMatch = card.faceName?.equals(cardName, ignoreCase = true) == true
    // ...
}
```

**数据转换**:
```kotlin
// 当前代码（约第 324-350 行）
val mtgchCard = exactMatch
var displayName = mtgchCard.zhsName
    ?: mtgchCard.atomicTranslatedName
    ?: mtgchCard.name

// 需要改为
val mtgCard = exactMatch
var displayName = mtgCard.zhName
    ?: mtgCard.faceName
    ?: mtgCard.name
```

### 2. 其他可能需要更新的文件

根据项目结构，以下文件可能需要适配：

- **DeckDetailViewModel.kt** - 套牌详情页面的 ViewModel
- **CardDetailActivity.kt** - 卡牌详情页面
- **CardDetailViewModel.kt** - 卡牌详情的 ViewModel（如果存在）

这些文件如果直接使用了 `MtgchApi` 或 `MtgchCardDto`，需要更新为使用新的 API。

---

## 🔧 需要服务端配合的功能

### 高级筛选支持

当前 Android 端的搜索功能支持以下筛选条件（见 `buildSearchQuery()` 方法）：

1. **颜色筛选**: `color:U,R` (多选)
2. **法术力值**: `cmc>3`, `cmc=2`, `cmc<5` (带操作符)
3. **类型筛选**: `type:creature`, `type:instant`
4. **稀有度筛选**: `rarity:rare`, `rarity:mythic`
5. **系列筛选**: `set:NEO`, `set:MOM`
6. **颜色标识**: `ci:WUB` (精确匹配)
7. **法术力值范围**: `mv` (用于筛选)
8. **力量/防御力**: `po>3`, `to=3`
9. **赛制合法性**: `f:modern l:legal`
10. **名称筛选**: `name:"Lightning Bolt"`
11. **规则文本**: `o:"deal 3 damage"`
12. **画师**: `a:"John Avon"`
13. **背景文字**: `ft:"flavor text"`

**当前状态**: ⚠️ 服务端可能仅支持基础查询（关键词 + 基础筛选）

**建议**:
1. 先测试基础搜索功能
2. 根据服务端实际支持的参数逐步添加高级筛选
3. 参考 API_MIGRATION_GUIDE.md 中的服务端实现章节

### 双面牌详细信息

**当前状态**: ⚠️ 服务端仅提供 `is_double_faced` 标识（0/1）

**需要补充**:
- `back_face_name` - 背面名称
- `back_face_mana_cost` - 背面法术力
- `back_face_type_line` - 背面类型
- `back_face_oracle_text` - 背面规则文本
- `back_face_power` / `back_face_toughness` - 背面攻防
- `card_faces_json` - 完整双面牌数据（JSON）

### 图片 URLs

**当前状态**: ❌ 服务端不返回图片 URLs

**解决方案**:
1. **方案 A（推荐）**: 服务端添加 Scryfall 图片 URL 生成
   ```json
   {
     "scryfall_id": "abc123",
     "image_uris": {
       "normal": "https://api.scryfall.com/cards/abc123?format=image&version=normal"
     }
   }
   ```

2. **方案 B**: Android 端通过 Scryfall API 单独获取图片
   - 当前已实现 `ScryfallImageApi`
   - 在显示图片时调用 `scryfallImageApi.getCardById(scryfallId)`

---

## 📊 迁移进度

```
┌─────────────────────────────────────────────────────────────────┐
│                      API 迁移进度                               │
├─────────────────────────────────────────────────────────────────┤
│ 新 API 接口文件        [████████████████████████████] 100%   │
│ 数据模型和映射器      [████████████████████████████] 100%   │
│ 依赖注入配置          [████████████████████████████] 100%   │
│ SearchViewModel        [████████████████████████████] 100%   │
│ DecklistRepository     [████████████████░░░░░░░░░░░]  60%   │
│ 其他 UI 组件          [░░░░░░░░░░░░░░░░░░░░░░░░░]   0%   │
│ 测试和调试            [░░░░░░░░░░░░░░░░░░░░░░░░░]   0%   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🧪 测试计划

### 1. 基础搜索测试

**测试步骤**:
1. 确保服务端已启动并运行在 `http://182.92.109.160:3000`
2. 在应用中搜索简单关键词（如 "闪电箭" 或 "Lightning Bolt"）
3. 验证搜索结果正确显示

**预期结果**:
- ✅ 搜索返回结果
- ✅ 中文名称正确显示
- ✅ 法术力值、类型等信息正确

### 2. 图片加载测试

**测试步骤**:
1. 搜索并点击一张卡牌
2. 查看卡牌详情页面

**预期结果**:
- ✅ 卡牌图片正确加载
- ✅ 双面牌正面图片显示

### 3. 缓存功能测试

**测试步骤**:
1. 搜索一张卡牌
2. 再次搜索同一张卡牌

**预期结果**:
- ✅ 第二次搜索更快（从缓存读取）
- ✅ 日志显示 "Found in cache"

---

## 🚀 下一步行动

### 立即任务（高优先级）

1. **更新 DecklistRepository.fetchScryfallDetails()**
   - 修改 API 调用
   - 适配响应格式
   - 测试卡牌详情获取

2. **测试基础搜索功能**
   - 编译并运行应用
   - 测试关键词搜索
   - 验证数据显示

3. **检查其他使用旧 API 的文件**
   ```bash
   # 搜索所有导入 MtgchApi 的文件
   grep -r "import.*MtgchApi" app/src/main/java/
   grep -r "mtgchApi\." app/src/main/java/
   ```

### 短期任务（中优先级）

1. **实现图片加载**
   - 在 `DeckDetailViewModel` 中集成 `ScryfallImageApi`
   - 确保双面牌图片正确显示

2. **添加错误处理**
   - 网络错误提示
   - API 不可用时的降级方案

3. **性能优化**
   - 减少不必要的 API 调用
   - 优化缓存策略

### 长期任务（低优先级）

1. **高级筛选功能**
   - 等待服务端支持
   - 更新 `buildSearchQuery()` 方法

2. **双面牌完整支持**
   - 等待服务端返回双面牌详细信息
   - 更新 UI 显示双面牌背面

3. **移除旧代码**
   - 删除 `mtgch` 目录下的旧 API 文件
   - 清理未使用的导入

---

## 📝 备注

### API 端点对比

| 功能 | MTGCH API | MTG Card Server | 状态 |
|------|-----------|----------------|------|
| 基础搜索 | `/api/v1/result` | `/api/result` | ✅ 已迁移 |
| 单卡详情 | `/api/v1/card/{id}/` | `/api/cards/{id}` | ✅ 已迁移 |
| 随机卡牌 | `/api/v1/random` | `/api/random` | ✅ 已迁移 |
| 系列列表 | ❌ 无 | `/api/sets` | ✅ 新增 |
| 热门卡牌 | ❌ 无 | `/api/stats/popular` | ✅ 新增 |
| 高级筛选 | ✅ 支持 | ⚠️ 部分支持 | ⏳ 待服务端 |
| 图片 URLs | ✅ 包含 | ❌ 不包含 | ⏳ 待添加 |
| 双面牌详情 | ✅ 完整 | ⚠️ 仅标识 | ⏳ 待添加 |

### 数据字段映射

| MTGCH 字段 | MTG Card Server 字段 | 兼容性 |
|-----------|---------------------|--------|
| `id` (String) | `id` (Long), `oracle_id` (String) | ⚠️ 类型不同 |
| `zhs_name` | `zh_name`, `face_name` | ⚠️ 字段名不同 |
| `atomic_translated_name` | ❌ 无 | ❌ 缺失 |
| `cmc` (Int) | `cmc` (Double) | ⚠️ 类型不同 |
| `image_uris` | ❌ 无 | ❌ 需从 Scryfall 获取 |
| `set_translated_name` | ❌ 无 | ❌ 缺失 |
| `card_faces` | `is_double_faced` (0/1) | ⚠️ 详细程度不同 |

---

**最后更新**: 2026-02-14
**维护者**: Claude Code Assistant
