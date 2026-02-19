# 服务端赛事数据集成 - 实现说明

## 概述

已实现从服务端 API 获取赛事数据和套牌列表，支持自动加载最新赛事、向下滑动分页加载历史赛事，以及查看赛事详情时自动下载该赛事的套牌列表。

---

## 新增文件

### 1. ServerApi.kt
**路径**: `app/src/main/java/com/mtgo/decklistmanager/data/remote/api/ServerApi.kt`

服务端 API 接口定义，包含以下方法：
- `getEvents()` - 获取赛事列表（支持赛制、日期、分页筛选）
- `getEventDetail()` - 获取单个赛事详情
- **`getEventDecklists()`** - 获取赛事下的套牌列表（新增）
- `getDecklistDetail()` - 获取卡组详情（含卡牌）

```kotlin
@GET("api/v1/events/{eventId}/decklists")
suspend fun getEventDecklists(
    @Path("eventId") eventId: Long,
    @Query("limit") limit: Int = 50,
    @Query("offset") offset: Int = 0
): Response<DecklistsResponse>
```

---

### 2. ServerDto.kt
**路径**: `app/src/main/java/com/mtgo/decklistmanager/data/remote/api/dto/ServerDto.kt`

服务端返回数据的数据传输对象：
- `EventsResponse` / `EventsData` - 赛事列表响应
- `EventDto` - 赛事对象
- `DecklistsResponse` / `DecklistsData` - 套牌列表响应
- `DecklistDto` - 套牌对象
- `DecklistDetailDto` - 套牌详情（含卡牌列表）
- `CardDto` - 卡牌对象

---

## 修改的文件

### 1. AppModule.kt
**修改内容**: 添加 `ServerApi` 的依赖注入

```kotlin
@Provides
@Singleton
fun provideServerApi(retrofit: Retrofit): ServerApi {
    return retrofit.create(ServerApi::class.java)
}
```

---

### 2. MainViewModel.kt
**修改内容**:

#### 新增字段（分页状态）：
```kotlin
private val _currentOffset = MutableStateFlow(0)
val currentOffset: StateFlow<Int> = _currentOffset.asStateFlow()

private val _hasMore = MutableStateFlow(true)
val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

private val _isLoadingMore = MutableStateFlow(false)
val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

private val _totalEvents = MutableStateFlow<Int?>(null)
val totalEvents: StateFlow<Int?> = _totalEvents.asStateFlow()
```

#### 新增方法：
1. **`loadEvents(refresh: Boolean = true)`**
   - 从服务器 API 加载赛事数据
   - 支持刷新模式（`refresh=true`）和加载更多模式（`refresh=false`）
   - 自动保存数据到本地数据库

2. **`loadMoreEvents()`**
   - 向下滑动时调用，加载更多历史赛事

3. **`refreshEvents()`**
   - 刷新赛事列表（重置分页）

4. **`loadEventsFromLocal()`**
   - 从本地数据库加载赛事（作为后备方案）

#### 修改的方法：
- `applyDateFilter()` - 改为调用 `refreshEvents()` 而不是 `loadEvents()`

---

### 3. MainActivity.kt
**修改内容**:

#### 1. 添加滚动监听器（在 `setupRecyclerView()` 中）
```kotlin
rvDecklists.apply {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            // 只在向下滑动时加载更多
            if (dy > 0 && currentTab == TAB_EVENT_LIST) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                if (layoutManager != null) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    // 当滚动到距离底部还有 5 个 item 时开始加载
                    if (visibleItemCount + firstVisibleItemPosition + 5 >= totalItemCount) {
                        viewModel.loadMoreEvents()
                    }
                }
            }
        }
    })
}
```

#### 2. 添加状态监听（在 `setupObservers()` 中）
```kotlin
// 监听加载更多状态
collectFlow(viewModel.isLoadingMore) { isLoading ->
    if (isLoading) {
        AppLogger.d("MainActivity", "Loading more events...")
    }
}

// 监听是否还有更多数据
collectFlow(viewModel.hasMore) { hasMore ->
    AppLogger.d("MainActivity", "Has more events: $hasMore")
}
```

#### 3. 修改的方法：
- `onResume()` - 改为调用 `viewModel.refreshEvents()`
- `showFormatFilterDialog()` - 筛选后调用 `viewModel.refreshEvents()`

---

### 4. EventDetailViewModel.kt
**修改内容**: 从服务器 API 获取赛事套牌列表

#### 新增依赖：
```kotlin
class EventDetailViewModel @Inject constructor(
    private val eventDao: EventDao,
    private val decklistDao: DecklistDao,
    private val serverApi: ServerApi  // 新增服务端 API
) : ViewModel()
```

#### 修改的方法：
1. **`loadEventDetail(eventId: Long, fromServer: Boolean = true)`**
   - 新增 `fromServer` 参数，默认为 `true`
   - 优先从服务器 API 加载赛事套牌列表
   - 自动保存到本地数据库
   - 支持从本地数据库加载（后备方案）

2. **`loadEventDetailFromServer(eventId: Long)`** - 新增
   - 从服务器获取赛事信息
   - 从服务器获取该赛事的套牌列表
   - 保存到本地数据库

3. **`loadEventDetailFromLocal(eventId: Long)`** - 新增
   - 从本地数据库加载赛事和套牌

4. **`downloadEventDecklists()`** - 已废弃
   - 标记为 `@Deprecated`
   - 现在通过 `loadEventDetail(eventId, fromServer=true)` 自动获取

---

## API 接口规范

### 基础 URL
```
http://182.92.109.160/
```

### 1. 获取赛事列表
**请求**:
```
GET /api/v1/events?format=MO&date=2025-01-15&limit=50&offset=0
```

**响应**:
```json
{
  "success": true,
  "data": {
    "events": [
      {
        "id": 1,
        "eventName": "MTGO Challenge 32",
        "eventType": "Online",
        "format": "Modern",
        "date": "2025-01-15",
        "sourceUrl": "https://mtgtop8.com/event?e=12345",
        "source": "MTGTop8",
        "deckCount": 64
      }
    ],
    "total": 1500,
    "limit": 50,
    "offset": 0
  }
}
```

### 2. 获取赛事下的套牌列表（新增）
**请求**:
```
GET /api/v1/events/{eventId}/decklists?limit=200&offset=0
```

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

### 3. 获取套牌详情（含卡牌）
**请求**:
```
GET /api/v1/decklists/{decklistId}
```

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
    "sideboard": [
      {
        "cardName": "Chalice of the Void",
        "quantity": 2,
        "manaCost": "{2}",
        "displayName": "虚空圣杯",
        "rarity": "Rare",
        "color": "C",
        "cardType": "Artifact",
        "cardSet": "Fifth Dawn"
      }
    ]
  }
}
```

---

## 功能说明

### 1. 自动加载最新赛事
- 应用启动时自动调用 `loadEvents(refresh=true)`
- 从服务器获取最新的 50 个赛事
- 数据同时保存到本地数据库

### 2. 向下滑动加载更多
- 用户向下滑动 RecyclerView 时触发
- 调用 `loadMoreEvents()`，自动传入 `refresh=false`
- 新数据追加到现有列表末尾
- 当 `hasMore=false` 时停止加载

### 3. 筛选功能
- 赛制筛选：调用 `refreshEvents()` 重新加载
- 日期筛选：调用 `refreshEvents()` 重新加载
- 筛选参数通过 API 查询传递给服务器

### 4. 赛事套牌下载（新增）
- 点击赛事进入详情页时，自动从服务器获取该赛事的套牌列表
- 套牌列表自动保存到本地数据库
- 无需手动点击下载按钮

### 5. 分页参数
- 赛事列表：每页 50 个
- 套牌列表：每页 200 个（可配置）
- `offset`: 偏移量（根据已加载数量计算）
- `total`: 服务器返回的总数，用于判断是否还有更多数据

---

## 数据流程

### 赛事列表
```
┌─────────────┐
│   用户操作   │ (滚动、筛选)
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│  MainActivity       │
│  - 滚动监听         │
│  - 筛选操作         │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  MainViewModel      │
│  - loadEvents()     │
│  - loadMoreEvents() │
│  - refreshEvents()  │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  ServerApi          │
│  (Retrofit)         │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  服务端 API         │
│  /api/v1/events     │
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  EventDao           │
│  (本地数据库)       │
└─────────────────────┘
```

### 赛事套牌列表（新增）
```
┌─────────────┐
│ 点击赛事     │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│  EventDetailActivity│
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  EventDetailViewModel│
│  - loadEventDetail()│
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  ServerApi          │
│  /api/v1/events/    │
│  {eventId}/decklists│
└──────┬──────────────┘
       │
       ▼
┌─────────────────────┐
│  DecklistDao        │
│  (保存套牌列表)     │
└─────────────────────┘
```

---

## 测试要点

1. **首次加载**
   - 打开应用，验证是否自动加载最新赛事
   - 检查赛事列表是否显示

2. **滚动加载**
   - 向下滑动到底部
   - 验证是否自动加载更多赛事
   - 检查新数据是否追加到列表末尾

3. **赛事套牌下载**（新增）
   - 点击任意赛事
   - 验证是否自动加载该赛事的套牌列表
   - 检查套牌列表是否显示

4. **筛选功能**
   - 切换赛制筛选，验证是否刷新数据
   - 切换日期筛选，验证是否刷新数据

5. **网络异常**
   - 断网状态下测试
   - 验证是否有错误提示
   - 验证是否能从本地数据库加载

6. **本地缓存**
   - 检查赛事数据是否保存到本地数据库
   - 检查套牌数据是否保存到本地数据库
   - 离线状态下是否能从本地加载

---

## 下一步优化建议

1. **添加加载更多进度指示器**
   - 在列表底部显示 "正在加载..." 提示
   - 没有更多数据时显示 "已加载全部"

2. **错误处理优化**
   - 网络失败时显示重试按钮
   - 添加下拉刷新功能

3. **性能优化**
   - 添加图片加载缓存
   - 优化 RecyclerView 滚动性能

4. **套牌详情优化**
   - 从服务器获取套牌详情（含卡牌列表）
   - 减少本地爬虫的使用

---

## 服务端要求

确保服务端 API 实现符合以下规范：

1. **响应格式**
   - 必须包含 `success` 字段
   - `data` 字段包含实际数据
   - 支持分页参数 `limit` 和 `offset`

2. **赛事数据**
   - 按日期降序排列（最新的在前）
   - `total` 字段返回总数
   - 支持赛制和日期筛选

3. **套牌数据**（新增）
   - 按赛事 ID 返回套牌列表
   - 包含玩家名称、套牌名称、战绩等
   - 返回套牌详情 URL（用于获取卡牌列表）

4. **错误处理**
   - 返回适当的 HTTP 状态码
   - 错误信息包含详细描述

---

## 相关文件清单

### 新增
- `ServerApi.kt`
- `ServerDto.kt`

### 修改
- `AppModule.kt` - 添加 ServerApi 依赖注入
- `MainViewModel.kt` - 添加分页逻辑和服务器 API 调用
- `MainActivity.kt` - 添加滚动监听和状态观察
- `EventDetailViewModel.kt` - 使用服务端 API 获取套牌列表

### 保留
- `MtgTop8Scraper.kt` - 本地爬虫（作为后备方案）
- `EventListViewModel.kt` - 赛事列表页 ViewModel
- `DeckDetailViewModel.kt` - 套牌详情页 ViewModel（待更新）
