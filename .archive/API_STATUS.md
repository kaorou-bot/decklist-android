# 当前接口状态与后续工作

## 已实现的接口

### 1. 获取赛事列表 ✅
**API**: `GET /api/v1/events`

**用途**: 主页显示赛事列表，支持滚动分页加载

**请求参数**:
- `format` - 赛制筛选（可选）
- `date` - 日期筛选（可选）
- `limit` - 每页数量（默认 50）
- `offset` - 偏移量（默认 0）

**响应**:
```json
{
  "success": true,
  "data": {
    "events": [...],
    "total": 1500,
    "limit": 50,
    "offset": 0
  }
}
```

---

### 2. 获取赛事详情 ✅
**API**: `GET /api/v1/events/{eventId}`

**用途**: 获取单个赛事的详细信息

**响应**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "eventName": "MTGO Challenge 32",
    "eventType": "Online",
    "format": "Modern",
    "date": "2025-01-15",
    "sourceUrl": "https://mtgtop8.com/event?e=12345",
    "source": "MTGTop8",
    "deckCount": 64
  }
}
```

---

### 3. 获取赛事下的套牌列表 ✅
**API**: `GET /api/v1/events/{eventId}/decklists`

**用途**: 点击赛事进入详情页时，自动加载该赛事的套牌列表

**请求参数**:
- `eventId` - 赛事 ID（路径参数）
- `limit` - 每页数量（默认 200）
- `offset` - 偏移量（默认 0）

**响应**:
```json
{
  "success": true,
  "data": {
    "decklists": [
      {
        "id": 101,
        "eventId": 1,
        "eventName": "MTGO Challenge 32",
        "deckName": "Izzet Phoenix",
        "format": "Modern",
        "date": "2025-01-15",
        "playerName": "PlayerOne",
        "record": "8-2",
        "url": "https://mtgtop8.com/deck?id=101"
      }
    ],
    "total": 64,
    "limit": 200,
    "offset": 0
  }
}
```

**说明**:
- 安卓端点击赛事后会自动调用此接口
- 套牌列表会保存到本地数据库
- 无需手动点击"下载"按钮

---

### 4. 获取套牌详情（含卡牌）⚠️ 已定义但未使用
**API**: `GET /api/v1/decklists/{decklistId}`

**用途**: 获取套牌的完整信息，包括主牌和备牌的卡牌列表

**响应**:
```json
{
  "success": true,
  "data": {
    "id": 101,
    "eventId": 1,
    "eventName": "MTGO Challenge 32",
    "deckName": "Izzet Phoenix",
    "format": "Modern",
    "date": "2025-01-15",
    "playerName": "PlayerOne",
    "record": "8-2",
    "mainDeck": [
      {
        "cardName": "Force of Negation",
        "quantity": 4,
        "manaCost": "{2}{U}{U}",
        "displayName": "否认之力",
        "rarity": "Mythic",
        "color": "U",
        "cardType": "Instant",
        "cardSet": "Modern Horizons 2"
      }
    ],
    "sideboard": [...]
  }
}
```

**状态**:
- ✅ API 接口已定义（`ServerApi.kt`）
- ✅ DTO 已定义（`DecklistDetailDto`）
- ❌ 安卓端尚未使用（`DeckDetailViewModel` 仍使用本地数据库）

---

## 当前实现流程

### 主页（赛事列表）
```
用户打开应用
    ↓
MainActivity.onCreate()
    ↓
MainViewModel.loadEvents(refresh=true)
    ↓
ServerApi.getEvents() ← 服务端 API
    ↓
EventDao.insertAll() ← 保存到本地
    ↓
显示赛事列表
```

### 向下滑动加载更多
```
用户向下滑动
    ↓
RecyclerView.OnScrollListener
    ↓
MainViewModel.loadMoreEvents()
    ↓
ServerApi.getEvents(offset=50)
    ↓
追加到现有列表
```

### 赛事详情（套牌列表）
```
用户点击赛事
    ↓
EventDetailActivity.onCreate()
    ↓
EventDetailViewModel.loadEventDetail(eventId, fromServer=true)
    ↓
ServerApi.getEventDecklists(eventId) ← 服务端 API
    ↓
DecklistDao.insertAll() ← 保存到本地
    ↓
显示套牌列表
```

### 套牌详情（卡牌列表）
```
用户点击套牌
    ↓
DeckDetailActivity.onCreate()
    ↓
DeckDetailViewModel.loadDecklistDetail(decklistId)
    ↓
CardDao.getCardsByDecklistId() ← 从本地数据库读取
    ↓
显示卡牌列表
```

---

## 后续需要做的工作

### 1. 套牌卡牌列表从服务器获取

**当前状态**: 套牌详情页仍从本地数据库读取卡牌

**需要实现**:
1. 修改 `DeckDetailViewModel`，添加从服务器获取卡牌列表的逻辑
2. 调用 `ServerApi.getDecklistDetail(decklistId)` 获取完整卡牌数据
3. 将卡牌数据保存到本地数据库（`CardDao`）

**实现步骤**:
```kotlin
// DeckDetailViewModel.kt
fun loadDecklistDetail(decklistId: Long, fromServer: Boolean = true) {
    viewModelScope.launch {
        if (fromServer) {
            // 从服务器获取
            val response = serverApi.getDecklistDetail(decklistId)
            if (response.isSuccessful && response.body()?.success == true) {
                val detail = response.body()!!.data!!

                // 保存套牌信息
                val decklistEntity = DecklistEntity(...)
                decklistDao.insert(decklistEntity)

                // 保存卡牌信息
                val mainDeckCards = detail.mainDeck.map { ... }
                val sideboardCards = detail.sideboard.map { ... }
                cardDao.insertAll(mainDeckCards + sideboardCards)

                // 显示数据
                _decklist.value = decklistEntity.toDomainModel()
                _mainDeck.value = mainDeckCards.map { ... }
                _sideboard.value = sideboardCards.map { ... }
            }
        } else {
            // 从本地数据库获取（后备）
            loadFromLocal(decklistId)
        }
    }
}
```

### 2. 服务端 API 需要实现的接口

#### ✅ 已实现
1. `GET /api/v1/events` - 赛事列表
2. `GET /api/v1/events/{eventId}` - 赛事详情
3. `GET /api/v1/events/{eventId}/decklists` - 套牌列表

#### ⚠️ 需要实现
4. `GET /api/v1/decklists/{decklistId}` - 套牌详情（含卡牌）

**注意**: 这个接口很重要，因为：
- 当前安卓端仍通过本地爬虫获取卡牌
- 如果用户第一次打开某个套牌，本地数据库没有卡牌数据
- 需要从服务器获取完整的卡牌列表

---

## 数据同步策略

### 当前策略
1. **赛事列表**: 每次打开应用从服务器获取最新的 50 个
2. **套牌列表**: 点击赛事时从服务器获取该赛事的所有套牌
3. **卡牌列表**: 从本地数据库读取（如果不存在则爬取）

### 建议策略
1. **赛事列表**: 保持当前策略
2. **套牌列表**: 保持当前策略
3. **卡牌列表**: 优先从服务器获取，如果失败则使用本地爬虫

---

## 接口优先级

### 高优先级 ⚠️ 重要
1. **套牌详情接口** (`/api/v1/decklists/{decklistId}`)
   - 用户查看套牌详情时必须要有卡牌数据
   - 当前依赖本地爬虫，不稳定且慢
   - 建议优先实现

### 中优先级
2. **卡牌搜索接口** (`/api/v1/cards/search`)
   - 已有 MTGCH API，可以复用
   - 用于获取卡牌详情（中文名、法术力等）

### 低优先级
3. **触发爬取接口** (`POST /api/v1/scrape`)
   - 可选，用于手动触发服务器爬虫
   - 如果服务器自动爬取，则不需要此接口

---

## 测试检查清单

### 赛事列表 ✅
- [ ] 打开应用，显示最新赛事
- [ ] 向下滑动，加载更多赛事
- [ ] 赛制筛选，只显示该赛制的赛事
- [ ] 日期筛选，只显示该日期的赛事

### 赛事详情 ✅
- [ ] 点击赛事，进入详情页
- [ ] 自动加载该赛事的套牌列表
- [ ] 套牌列表显示玩家名称、套牌名称、战绩

### 套牌详情 ⚠️
- [ ] 点击套牌，进入详情页
- [ ] 显示主牌卡牌列表
- [ ] 显示备牌卡牌列表
- [ ] 卡牌中文名称正确显示
- [ ] 卡牌法术力值正确显示

### 本地缓存 ✅
- [ ] 赛事数据保存到本地
- [ ] 套牌数据保存到本地
- [ ] 离线状态下可以查看已缓存的数据

---

## 服务端开发者需要注意

### 数据格式要求

1. **日期格式**: `YYYY-MM-DD`（如 `2025-01-15`）

2. **ID 类型**:
   - 赛事 ID: Long
   - 套牌 ID: Long
   - 卡牌 ID: Long

3. **分页参数**:
   - `limit`: 每页数量（建议 50-200）
   - `offset`: 偏移量（从 0 开始）

4. **响应格式**:
   ```json
   {
     "success": true,
     "data": { ... }
   }
   ```

### 性能要求

1. **响应时间**:
   - 赛事列表: < 2 秒
   - 套牌列表: < 3 秒
   - 套牌详情: < 5 秒

2. **并发支持**:
   - 支持多个客户端同时请求
   - 建议添加缓存机制

3. **数据完整性**:
   - 确保赛事和套牌的关联正确
   - 确保套牌和卡牌的关联正确
   - 确保玩家名称、战绩等信息准确

---

## 总结

### 已完成 ✅
1. 主页从服务器加载赛事列表
2. 支持向下滑动分页加载
3. 支持赛制和日期筛选
4. 点击赛事自动加载套牌列表
5. 数据保存到本地数据库

### 待完成 ⚠️
1. 套牌详情从服务器获取卡牌列表
2. 优化错误处理和重试机制
3. 添加加载进度指示器

### 接口状态
- **赛事列表**: ✅ 已实现并使用
- **套牌列表**: ✅ 已实现并使用
- **套牌详情**: ⚠️ 已定义接口，安卓端尚未使用
