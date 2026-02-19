# MTGCH API 迁移状态报告

## 📋 概述

**日期**: 2025-02-19
**版本**: v5.0
**目标**: 完全放弃外部 MTGCH API，使用自有服务器数据

---

## ✅ 已完成迁移

### 1. **赛事列表 (Events)**
- **API端点**: `GET /api/v1/events`
- **实现文件**:
  - `MainViewModel.kt` - 添加分页加载逻辑
  - `MainActivity.kt` - 添加滚动监听器
  - `ServerApi.kt` - Retrofit 接口定义
- **功能**:
  - ✅ 自动下载最新赛事
  - ✅ 滚动加载历史赛事（分页）
  - ✅ 按赛制和日期筛选
  - ✅ 完全使用服务器数据

### 2. **赛事详情 (Event Detail)**
- **API端点**: `GET /api/v1/events/{eventId}`
- **实现文件**:
  - `EventDetailViewModel.kt` - 从服务器获取赛事详情
- **功能**:
  - ✅ 显示赛事完整信息
  - ✅ 自动加载该赛事下的所有套牌
  - ✅ 完全使用服务器数据

### 3. **套牌列表 (Decklists)**
- **API端点**: `GET /api/v1/events/{eventId}/decklists`
- **实现文件**:
  - `EventDetailViewModel.kt` - 获取套牌列表
- **功能**:
  - ✅ 显示赛事下所有套牌
  - ✅ 完全使用服务器数据

### 4. **套牌详情 (Decklist Detail + Cards)**
- **API端点**: `GET /api/v1/decklists/{decklistId}`
- **实现文件**:
  - `DeckDetailViewModel.kt` - **v5.0重大更新**
- **功能**:
  - ✅ 从服务器获取完整卡牌数据
  - ✅ 包括：中文名、法术力值、颜色、稀有度、类型、系列
  - ✅ **不再调用 MTGCH API**
  - ✅ 数据库作为缓存层

**关键代码变更** (`DeckDetailViewModel.kt:147-236`):
```kotlin
// OLD: 只保存 cardName，调用 MTGCH API 填充其他字段
CardEntity(
    decklistId = decklistId,
    cardName = cardDto.cardName,
    quantity = cardDto.quantity,
    manaCost = null,  // 让 MTGCH API 填充
    displayName = null,  // 让 MTGCH API 填充
    // ... 其他 null
)

// NEW: 直接使用服务器返回的完整数据
CardEntity(
    decklistId = decklistId,
    cardName = cardDto.cardName,
    quantity = cardDto.quantity,
    manaCost = cardDto.manaCost,      // 使用服务器数据
    displayName = cardDto.displayName, // 使用服务器数据
    rarity = cardDto.rarity,          // 使用服务器数据
    color = cardDto.color,            // 使用服务器数据
    cardType = cardDto.cardType,      // 使用服务器数据
    cardSet = cardDto.cardSet         // 使用服务器数据
)
```

---

## ⚠️ 仍需依赖 MTGCH API 的功能

### 1. **卡牌搜索 (Card Search)**
- **原因**: 服务器的 `/api/cards/search` 端点未实现，返回空数据
- **现状**: 仍使用 `MtgchApi` (https://mtgch.com/)
- **影响功能**:
  - 搜索页面的卡牌搜索
  - `SearchViewModel.kt`
  - `DecklistRepository.getCardInfo()` - 单卡详情查询

**服务器端待实现**:
```sql
-- 需要实现的搜索端点
GET /api/cards/search?q={query}&limit=20&offset=0

-- 需要实现的详情端点
GET /api/cards/{oracleId}
GET /api/cards/{oracleId}/printings
```

---

## 🚨 已知问题

### 1. **服务器 `displayName` 字段返回英文**
- **问题描述**: 测试 `/api/v1/decklists/35` 发现：
  ```json
  {
    "cardName": "All That Glitters",
    "displayName": "All That Glitters",  // ❌ 应该是 "熠熠生辉"
    ...
  }
  ```
- **影响**: 套牌详情页面的卡牌显示英文名而非中文
- **解决方案**: 需要服务器端修复数据库：
  ```sql
  UPDATE cards
  SET displayName = COALESCE(nameZh, cardName)
  WHERE displayName = cardName OR displayName IS NULL;
  ```
- **临时处理**: 客户端会检测此问题并输出警告日志

---

## 📊 数据流对比

### 旧架构 (v4.x)
```
赛事列表 → MTGTop8 爬虫 → 数据库
赛事详情 → MTGTop8 爬虫 → 数据库
套牌列表 → MTGTop8 爬虫 → 数据库
套牌详情 → MTGTop8 爬虫 → 卡牌名称
卡牌详情 → MTGCH API → 中文名、法术力值等
卡牌搜索 → MTGCH API → 搜索结果
```

### 新架构 (v5.0)
```
赛事列表 → Server API → 数据库 ✅
赛事详情 → Server API → 显示 ✅
套牌列表 → Server API → 数据库 ✅
套牌详情 → Server API → 完整卡牌数据 ✅
卡牌详情 → MTGCH API → 单卡查询 ⚠️ (服务器未实现)
卡牌搜索 → MTGCH API → 搜索结果 ⚠️ (服务器未实现)
```

---

## 🔧 技术实现细节

### 依赖注入配置 (`AppModule.kt`)

```kotlin
@Provides
@Singleton
@Named("mtgch")
fun provideMtgchRetrofit(okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder()
        .baseUrl("https://mtgch.com/")  // 外部 MTGCH - 用于卡牌搜索
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

@Provides
@Singleton
@Named("server")
fun provideServerRetrofit(okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder()
        .baseUrl("http://182.92.109.160/")  // 自有服务器 - 赛事和套牌
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

@Provides
@Singleton
fun provideMtgchApi(@Named("mtgch") retrofit: Retrofit): MtgchApi {
    return retrofit.create(MtgchApi::class.java)
}

@Provides
@Singleton
fun provideServerApi(@Named("server") retrofit: Retrofit): ServerApi {
    return retrofit.create(ServerApi::class.java)
}
```

### API 端点映射

| 功能 | API 端点 | 数据源 | 状态 |
|------|----------|--------|------|
| 赛事列表 | `GET /api/v1/events` | 自有服务器 | ✅ |
| 赛事详情 | `GET /api/v1/events/{id}` | 自有服务器 | ✅ |
| 套牌列表 | `GET /api/v1/events/{id}/decklists` | 自有服务器 | ✅ |
| 套牌详情 | `GET /api/v1/decklists/{id}` | 自有服务器 | ✅ |
| 卡牌搜索 | `GET /api/cards/search` | MTGCH | ⚠️ 待实现 |
| 卡牌详情 | `GET /api/cards/{oracleId}` | MTGCH | ⚠️ 待实现 |
| 印刷版本 | `GET /api/cards/{oracleId}/printings` | MTGCH | ⚠️ 待实现 |

---

## 📝 迁移完成的文件清单

### 修改的文件
1. `app/src/main/java/com/mtgo/decklistmanager/di/AppModule.kt`
   - 添加 `@Named("mtgch")` 和 `@Named("server")` Retrofit 配置

2. `app/src/main/java/com/mtgo/decklistmanager/ui/decklist/MainViewModel.kt`
   - 添加分页加载逻辑
   - 集成 `ServerApi`

3. `app/src/main/java/com/mtgo/decklistmanager/ui/decklist/MainActivity.kt`
   - 添加滚动监听器实现加载更多

4. `app/src/main/java/com/mtgo/decklistmanager/ui/decklist/EventDetailViewModel.kt`
   - 从本地爬虫改为 `ServerApi`
   - 自动下载赛事套牌

5. `app/src/main/java/com/mtgo/decklistmanager/ui/decklist/DeckDetailViewModel.kt`
   - **v5.0 核心变更**
   - 完全放弃 MTGCH API
   - 直接使用服务器卡牌数据

### 新增的文件
1. `app/src/main/java/com/mtgo/decklistmanager/data/remote/api/ServerApi.kt`
   - Retrofit 接口定义

2. `app/src/main/java/com/mtgo/decklistmanager/data/remote/api/dto/ServerDto.kt`
   - 数据传输对象
   - `EventsResponse`
   - `EventDetailResponse`
   - `DecklistsResponse`
   - `DecklistDetailResponse`
   - `CardDto`

---

## 🎯 下一步计划

### 短期 (服务器端)
1. **修复 `displayName` 字段**
   ```sql
   UPDATE cards SET displayName = COALESCE(nameZh, cardName)
   WHERE displayName = cardName OR displayName IS NULL;
   ```

2. **实现卡牌搜索 API**
   - `GET /api/cards/search?q={query}` - 支持模糊搜索
   - `GET /api/cards/{oracleId}` - 获取单卡详情
   - `GET /api/cards/{oracleId}/printings` - 获取所有印刷版本

### 长期 (客户端)
1. 当服务器实现卡牌搜索后，更新 `SearchViewModel.kt` 使用服务器 API
2. 当服务器实现单卡查询后，更新 `DecklistRepository.getCardInfo()` 使用服务器 API
3. 完全移除 `MtgchApi` 依赖

---

## 📌 注意事项

1. **数据库缓存**: 所有服务器数据都会缓存到本地数据库
2. **离线支持**: 已缓存的数据可在离线状态下访问
3. **数据同步**: 每次打开套牌详情时会从服务器重新获取最新数据
4. **向后兼容**: 保留了旧的爬虫代码作为备份

---

## 🔗 相关文档

- [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) - 服务器 API 文档
- [SERVER_API_SPEC.md](./SERVER_API_SPEC.md) - API 规范（待创建）
- [API_MIGRATION_GUIDE.md](./docs/API_MIGRATION_GUIDE.md) - 迁移指南
