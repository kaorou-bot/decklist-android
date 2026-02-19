# 套牌下载功能迁移到服务端 - 技术说明文档

## 概述

本文档介绍 Android 应用当前的套牌下载功能，以便将其迁移到服务端，让安卓端直接读取服务端数据。

---

## 当前架构

### 数据流程

```
┌─────────────┐      爬取        ┌──────────────┐
│  MTGTop8    │ ──────────────► │   Android    │
│  .com       │                 │   App        │
└─────────────┘                 └──────┬───────┘
                                        │
                                        ▼
                                 ┌──────────────┐
                                 │  Local DB    │
                                 │  (Room)      │
                                 └──────────────┘
```

**目标架构**：

```
┌─────────────┐      爬取        ┌──────────────┐      API        ┌──────────────┐
│  MTGTop8    │ ──────────────► │   自建服务端  │ ──────────────►│  Android App │
│  .com       │                 │   (Python/Go)│                 │              │
└─────────────┘                 └──────────────┘                 └──────┬───────┘
                                                                        │
                                                                        ▼
                                                                 ┌──────────────┐
                                                                 │  Local DB    │
                                                                 │  (Cache)     │
                                                                 └──────────────┘
```

---

## 核心数据模型

### 1. Event（赛事）

代表一个 MTG 比赛/赛事，如 "MTGO Challenge 32" 或 "SCG Indianapolis"。

**数据库表**：`events`

```kotlin
// EventEntity
data class EventEntity(
    val id: Long,                    // 主键（自增）
    val eventName: String,           // 赛事名称，如 "MTGO Challenge 32"
    val eventType: String?,          // 赛事类型，如 "Online", "Paper", "Premier"
    val format: String,              // 赛制，如 "Modern", "Standard", "Legacy"
    val date: String,                // 日期，YYYY-MM-DD 格式
    val sourceUrl: String?,          // 源 URL（MTGTop8 赛事页面）
    val source: String,              // 数据源，如 "MTGTop8", "MTGO"
    val deckCount: Int,              // 该赛事包含的卡组数量
    val createdAt: Long              // 创建时间戳
)
```

**示例数据**：
```json
{
  "id": 1,
  "eventName": "MTGO Challenge 32",
  "eventType": "Online",
  "format": "Modern",
  "date": "2025-01-15",
  "sourceUrl": "https://mtgtop8.com/event?e=12345&f=MO",
  "source": "MTGTop8",
  "deckCount": 64,
  "createdAt": 1736899200000
}
```

---

### 2. Decklist（卡组）

代表一个完整的卡组列表。

**数据库表**：`decklists`

```kotlin
data class DecklistEntity(
    val id: Long,                    // 主键（自增）
    val eventId: Long?,              // 所属赛事 ID
    val eventName: String,           // 赛事名称
    val deckName: String?,           // 卡组名称，如 "Izzet Phoenix"
    val format: String,              // 赛制
    val date: String,                // 日期 YYYY-MM-DD
    val playerName: String?,         // 玩家名称
    val record: String?,             // 战绩，如 "8-2", "12-3", "1st"
    val url: String?,                // 卡组 URL
    val source: String?,             // 数据源
    val isFavorite: Boolean = false, // 是否收藏
    val createdAt: Long              // 创建时间戳
)
```

**示例数据**：
```json
{
  "id": 1,
  "eventId": 1,
  "eventName": "MTGO Challenge 32",
  "deckName": "Izzet Phoenix",
  "format": "Modern",
  "date": "2025-01-15",
  "playerName": "PlayerOne",
  "record": "8-2",
  "url": "https://mtgtop8.com/deck?id=12345",
  "source": "MTGTop8"
}
```

---

### 3. Card（卡牌）

卡组中的单张卡牌。

**数据库表**：`cards`

```kotlin
data class CardEntity(
    val id: Long,                    // 主键（自增）
    val decklistId: Long,            // 所属卡组 ID
    val cardName: String,            // 卡牌名称（英文，如 "Force of Negation"）
    val quantity: Int,               // 数量
    val location: String,            // 位置："main" 或 "sideboard"
    val cardOrder: Int,              // 排序
    val manaCost: String?,           // 法术力值，如 "{2}{U}{U}"
    val displayName: String?,        // 显示名称（中文，如 "否认之力"）
    val rarity: String?,             // 稀有度
    val color: String?,              // 颜色
    val cardType: String?,           // 类型
    val cardSet: String?             // 系列
)
```

**示例数据**：
```json
{
  "id": 1,
  "decklistId": 1,
  "cardName": "Force of Negation",
  "quantity": 4,
  "location": "main",
  "cardOrder": 0,
  "manaCost": "{2}{U}{U}",
  "displayName": "否认之力",
  "rarity": "Mythic",
  "color": "U",
  "cardType": "Instant",
  "cardSet": "Modern Horizons 2"
}
```

---

## 当前爬取流程

### 爬虫代码位置

主要爬虫类：`MtgTop8Scraper.kt`

### 爬取流程

#### 第一步：获取赛事列表

```kotlin
suspend fun fetchEventList(
    format: String,      // 赛制代码：ST(标准), MO(摩登), LE(特选), etc.
    date: String?,       // 日期筛选 YYYY-MM-DD
    maxEvents: Int       // 最大赛事数量
): List<MtgTop8EventDto>
```

**访问 URL**：
```
https://mtgtop8.com/format?f=MO
```

**返回数据结构**：
```kotlin
data class MtgTop8EventDto(
    val eventId: String,        // 赛事 ID，从 URL 提取，如 "12345"
    val eventName: String,      // 赛事名称，如 "MTGO Challenge 32"
    val eventDate: String,      // 日期 YYYY-MM-DD
    val format: String,         // 赛制代码
    val eventUrl: String,       // 赛事 URL
    val deckCount: Int,         // 卡组数量（初始为 0）
    val eventType: String?      // 赛事类型
)
```

#### 第二步：获取赛事下的卡组列表

```kotlin
suspend fun fetchEventDecklists(
    eventUrl: String,     // 赛事 URL
    maxDecks: Int         // 最大卡组数量
): MtgTop8EventDecklistsDto?
```

**访问 URL**：
```
https://mtgtop8.com/event?e=12345
```

**逻辑**：
1. 从赛事页面提取所有 `d` 参数（卡组 ID）
2. 并发检查每个卡组 ID 是否有效
3. 构建卡组列表

**返回数据结构**：
```kotlin
data class MtgTop8EventDecklistsDto(
    val event: MtgTop8EventDto,
    val decklists: List<MtgTop8DecklistDto>
)

data class MtgTop8DecklistDto(
    val deckId: String,       // 卡组 ID
    val deckName: String,     // 卡组名称
    val playerName: String,   // 玩家名称
    val eventName: String,    // 赛事名称
    val eventDate: String,    // 日期
    val record: String,       // 战绩
    val format: String,       // 赛制
    val url: String           // 卡组 URL
)
```

#### 第三步：获取卡组详情（卡牌列表）

```kotlin
suspend fun fetchDecklistDetail(url: String): MtgTop8DecklistDetailDto?
```

**访问 URL**：
```
https://mtgtop8.com/event?e=12345&d=0
```

**HTML 结构**：
```html
<!-- 主牌 -->
<div id="md0">4 Force of Negation</div>
<div id="md1">4 Arid Mesa</div>
...

<!-- 备牌 -->
<div id="sb0">2 Chalice of the Void</div>
<div id="sb1">1 Dress Down</div>
...
```

**返回数据结构**：
```kotlin
data class MtgTop8DecklistDetailDto(
    val mainDeck: List<MtgTop8CardDto>,      // 主牌列表
    val sideboardDeck: List<MtgTop8CardDto>  // 备牌列表
)

data class MtgTop8CardDto(
    val quantity: Int,        // 数量
    val name: String          // 卡牌名称（英文）
)
```

---

## 赛制代码映射

| 代码 | 赛制 | 英文 |
|------|------|------|
| ST | 标准 | Standard |
| MO | 摩登 | Modern |
| LE | 特选 | Legacy |
| VI | 薇薇 | Vintage |
| PA | 贫民 | Pauper |
| PI | 先驱 | Pioneer |
| CMD | 指挥官 | Commander |
| CEDH | 竞技指挥官 | Competitive Commander |

---

## 建议的服务端 API 设计

### 1. 获取赛事列表

**请求**：
```
GET /api/v1/events
Query Parameters:
  - format: string (赛制代码，可选)
  - date: string (YYYY-MM-DD，可选)
  - limit: integer (默认 100)
  - offset: integer (默认 0)
```

**响应**：
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
    "limit": 100,
    "offset": 0
  }
}
```

---

### 2. 获取赛事详情

**请求**：
```
GET /api/v1/events/{eventId}
```

**响应**：
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

### 3. 获取赛事下的卡组列表

**请求**：
```
GET /api/v1/events/{eventId}/decklists
Query Parameters:
  - limit: integer (默认 100)
  - offset: integer (默认 0)
```

**响应**：
```json
{
  "success": true,
  "data": {
    "decklists": [
      {
        "id": 1,
        "eventId": 1,
        "eventName": "MTGO Challenge 32",
        "deckName": "Izzet Phoenix",
        "format": "Modern",
        "date": "2025-01-15",
        "playerName": "PlayerOne",
        "record": "8-2",
        "url": "https://mtgtop8.com/deck?id=12345"
      }
    ],
    "total": 64,
    "limit": 100,
    "offset": 0
  }
}
```

---

### 4. 获取卡组详情（含卡牌）

**请求**：
```
GET /api/v1/decklists/{decklistId}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "id": 1,
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
      },
      {
        "cardName": "Arid Mesa",
        "quantity": 4,
        "manaCost": null,
        "displayName": "不毛高地",
        "rarity": "Rare",
        "color": "R,W",
        "cardType": "Land",
        "cardSet": "Zendikar Rising"
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

### 5. 触发爬取（管理接口）

**请求**：
```
POST /api/v1/scrape/events
Body:
{
  "format": "MO",
  "date": "2025-01-15",  // 可选
  "maxEvents": 10
}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "message": "Scraping started",
    "taskId": "scrape_task_12345"
  }
}
```

---

## 服务端实现建议

### 数据库表设计

**events 表**：
```sql
CREATE TABLE events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_name VARCHAR(255) NOT NULL,
    event_type VARCHAR(100),
    format VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    source_url TEXT,
    source VARCHAR(50) NOT NULL,
    deck_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_format (format),
    INDEX idx_date (date),
    INDEX idx_event_name (event_name)
);
```

**decklists 表**：
```sql
CREATE TABLE decklists (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT,
    event_name VARCHAR(255) NOT NULL,
    deck_name VARCHAR(255),
    format VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    player_name VARCHAR(255),
    record VARCHAR(50),
    url TEXT,
    source VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id),
    INDEX idx_event_id (event_id),
    INDEX idx_format (format),
    INDEX idx_date (date)
);
```

**cards 表**：
```sql
CREATE TABLE cards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    decklist_id BIGINT NOT NULL,
    card_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    location ENUM('main', 'sideboard') NOT NULL,
    card_order INT NOT NULL,
    mana_cost VARCHAR(100),
    display_name VARCHAR(255),
    rarity VARCHAR(50),
    color VARCHAR(50),
    card_type VARCHAR(255),
    card_set VARCHAR(255),
    FOREIGN KEY (decklist_id) REFERENCES decklists(id),
    INDEX idx_decklist_id (decklist_id),
    INDEX idx_card_name (card_name)
);
```

**card_info 表**（卡牌详情缓存）：
```sql
CREATE TABLE card_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    oracle_id VARCHAR(100) UNIQUE,
    name VARCHAR(255) NOT NULL,
    en_name VARCHAR(255),
    mana_cost VARCHAR(100),
    cmc DECIMAL(10,1),
    type_line TEXT,
    oracle_text TEXT,
    colors VARCHAR(50),
    color_identity VARCHAR(50),
    power VARCHAR(10),
    toughness VARCHAR(10),
    loyalty VARCHAR(10),
    rarity VARCHAR(50),
    set_code VARCHAR(50),
    set_name VARCHAR(255),
    artist VARCHAR(255),
    image_uri_normal TEXT,
    is_dual_faced BOOLEAN DEFAULT FALSE,
    front_face_name VARCHAR(255),
    back_face_name VARCHAR(255),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_oracle_id (oracle_id)
);
```

---

## 技术要点

### 1. HTML 解析

服务端可使用类似 Jsoup 的库：
- Python: `BeautifulSoup4` + `requests`
- Go: `goquery`

### 2. 并发控制

- 限制并发请求数量（建议 2-5 个）
- 添加请求间隔（建议 500ms-1s）
- 实现指数退避重试

### 3. 数据同步

- 实现增量更新（只爬取新赛事）
- 定时任务（每小时/每天爬取最新数据）
- 失败重试机制

### 4. 缓存策略

- CardInfo 数据可长期缓存
- Event 和 Decklist 数据按日期缓存
- 实现缓存失效和更新机制

---

## 注意事项

1. **遵守 robots.txt**：检查 MTGTop8 的爬取规则
2. **请求频率**：避免过于频繁的请求，建议添加随机延迟
3. **错误处理**：网络超时、HTML 结构变化等情况需要妥善处理
4. **数据一致性**：确保事务性，避免部分数据插入失败
5. **日志记录**：详细记录爬取过程，便于排查问题

---

## 相关文件

- 爬虫实现：`app/src/main/java/com/mtgo/decklistmanager/data/remote/api/MtgTop8Scraper.kt`
- 赛事实体：`app/src/main/java/com/mtgo/decklistmanager/data/local/entity/EventEntity.kt`
- 赛事模型：`app/src/main/java/com/mtgo/decklistmanager/domain/model/Event.kt`
- Repository：`app/src/main/java/com/mtgo/decklistmanager/data/repository/DecklistRepository.kt`
- 赛事列表 ViewModel：`app/src/main/java/com/mtgo/decklistmanager/ui/decklist/EventListViewModel.kt`
- 赛事详情 ViewModel：`app/src/main/java/com/mtgo/decklistmanager/ui/decklist/EventDetailViewModel.kt`

---

## 下一步

1. 确认服务端技术栈（Python Flask/Django 或 Go Gin/Echo）
2. 设计数据库表结构
3. 实现爬虫逻辑
4. 实现 RESTful API
5. 安卓端适配 API
