# 服务端 API 接口规范

本文档说明安卓端需要服务端实现的 API 接口规范。

**基础 URL**: `http://182.92.109.160/`

---

## 接口列表

### 1. 获取赛事列表

**接口**: `GET /api/v1/events`

**说明**: 获取 MTG 比赛赛事列表，用于主页展示

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|-------|------|------|------|
| format | string | 否 | 赛制代码（MO=摩登, ST=标准, LE=特选等）|
| date | string | 否 | 日期筛选，格式：YYYY-MM-DD |
| limit | integer | 否 | 每页数量，默认 50 |
| offset | integer | 否 | 偏移量，默认 0 |

**赛制代码对照表**:
```
ST  - Standard (标准)
MO  - Modern (摩登)
LE  - Legacy (特选)
VI  - Vintage (薇薇)
PA  - Pauper (贫民)
PI  - Pioneer (先驱)
CMD - Commander (指挥官)
CEDH - Competitive Commander (竞技指挥官)
```

**响应格式**:
```json
{
  "success": true,
  "data": {
    "events": [
      {
        "id": 1,
        "eventName": "MTGO Challenge 32",
        "eventType": "Online",
        "format": "MO",
        "date": "2025-01-15",
        "sourceUrl": "https://mtgtop8.com/event?e=12345&f=MO",
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

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 赛事唯一标识 |
| eventName | string | 赛事名称 |
| eventType | string | 赛事类型（Online/Paper/Premier等）|
| format | string | 赛制代码 |
| date | string | 比赛日期，格式：YYYY-MM-DD |
| sourceUrl | string | 源 URL（MTGTop8赛事页面）|
| source | string | 数据源（MTGTop8/MTGO等）|
| deckCount | integer | 该赛事包含的套牌数量 |
| total | integer | 总赛事数量（用于分页）|
| limit | integer | 每页数量 |
| offset | integer | 当前偏移量 |

**要求**:
- 赛事按日期降序排列（最新的在前）
- 必须返回 `total` 字段用于分页
- 支持赛制和日期筛选

---

### 2. 获取赛事详情

**接口**: `GET /api/v1/events/{eventId}`

**说明**: 获取单个赛事的详细信息

**路径参数**:
- `eventId` - 赛事 ID（Long）

**响应格式**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "eventName": "MTGO Challenge 32",
    "eventType": "Online",
    "format": "MO",
    "date": "2025-01-15",
    "sourceUrl": "https://mtgtop8.com/event?e=12345&f=MO",
    "source": "MTGTop8",
    "deckCount": 64
  }
}
```

**字段说明**: 同接口 1

---

### 3. 获取赛事下的套牌列表

**接口**: `GET /api/v1/events/{eventId}/decklists`

**说明**: 获取指定赛事的所有套牌

**路径参数**:
- `eventId` - 赛事 ID（Long）

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|-------|------|------|------|
| limit | integer | 否 | 每页数量，默认 200 |
| offset | integer | 否 | 偏移量，默认 0 |

**响应格式**:
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
        "format": "MO",
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

**字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 套牌唯一标识 |
| eventId | Long | 所属赛事 ID |
| eventName | string | 赛事名称 |
| deckName | string | 套牌名称（如 "Izzet Phoenix"）|
| format | string | 赛制代码 |
| date | string | 日期，格式：YYYY-MM-DD |
| playerName | string | 玩家名称 |
| record | string | 战绩（如 "8-2", "12-3", "1st"）|
| url | string | 套牌 URL（用于获取卡牌）|
| total | integer | 总套牌数量 |
| limit | integer | 每页数量 |
| offset | integer | 当前偏移量 |

**要求**:
- 返回该赛事的所有套牌（建议一次返回全部，不要分页）
- 按排名排序（如果有）

---

### 4. 获取套牌详情（含卡牌）⚠️ 重要

**接口**: `GET /api/v1/decklists/{decklistId}`

**说明**: 获取套牌的完整信息，包括主牌和备牌的所有卡牌

**路径参数**:
- `decklistId` - 套牌 ID（Long）

**响应格式**:
```json
{
  "success": true,
  "data": {
    "id": 101,
    "eventId": 1,
    "eventName": "MTGO Challenge 32",
    "deckName": "Izzet Phoenix",
    "format": "MO",
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

**字段说明**:

**套牌基本信息**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 套牌唯一标识 |
| eventId | Long | 所属赛事 ID |
| eventName | string | 赛事名称 |
| deckName | string | 套牌名称 |
| format | string | 赛制代码 |
| date | string | 日期，格式：YYYY-MM-DD |
| playerName | string | 玩家名称 |
| record | string | 战绩 |

**卡牌信息**:

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| cardName | string | 卡牌英文名称 | "Force of Negation" |
| quantity | integer | 数量 | 4 |
| manaCost | string | 法术力值（原生格式） | "{2}{U}{U}" 或 null（土地）|
| displayName | string | 中文名称 | "否认之力" |
| rarity | string | 稀有度 | "Common", "Uncommon", "Rare", "Mythic" |
| color | string | 颜色（逗号分隔） | "U", "R,W", "B,G", "U,B,R", "C"（无色）|
| cardType | string | 类型 | "Instant", "Creature", "Land", "Artifact", 等 |
| cardSet | string | 系列 | "Modern Horizons 2" |

**特殊卡牌处理**:

1. **双面牌**（如 "Bounding // Aligned"):
   - `manaCost`: `"{G} // {W}"` 或类似格式
   - `cardName`: "Bounding // Aligned"

2. **连体牌**（如 "Wear // Tear"）:
   - `manaCost`: `"{1}{R} // {W}"`
   - `cardName`: "Wear // Tear"

3. **冒险牌**（如 "Brazen Borrower // Brazen Quartermaster"）:
   - `manaCost`: `"{U}"` 或 `"{U} // {2}{W}"`
   - `cardName`: "Brazen Borrower // Brazen Quartermaster"

**要求**:
- `cardName` 必须是**英文名称**（用于 API 搜索）
- `displayName` 是**中文名称**（用于显示）
- `manaCost` 格式必须正确，使用花括号包围符号（如 `{U}`, `{2}`）
- 土地卡的 `manaCost` 为 `null`
- 颜色使用标准代码：W（白）、U（蓝）、B（黑）、R（红）、G（绿）、C（无色）
- 多色用逗号分隔：`"R,W"`、`"U,B,R"` 等

---

## 通用响应格式

所有接口的响应都必须遵循以下格式：

```json
{
  "success": true/false,
  "data": { ... },
  "error": "错误信息（仅当 success=false 时）"
}
```

**成功示例**:
```json
{
  "success": true,
  "data": {
    "events": [...]
  }
}
```

**失败示例**:
```json
{
  "success": false,
  "error": "Invalid format code"
}
```

---

## 错误处理

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 常见错误场景

1. **赛事不存在**
   ```json
   {
     "success": false,
     "error": "Event not found: 999"
   }
   ```

2. **套牌不存在**
   ```json
   {
     "success": false,
     "error": "Decklist not found: 999"
   }
   ```

3. **无效的赛制代码**
   ```json
   {
     "success": false,
     "error": "Invalid format code: XXX"
   }
   ```

4. **数据库查询失败**
   ```json
   {
     "success": false,
     "error": "Database error: ..."
   }
   ```

---

## 数据来源说明

### 当前数据来源
- **MTGTop8.com** - 主要数据源
- 爬虫已迁移到服务端

### 数据要求
1. **准确性**: 确保玩家名称、套牌名称、战绩准确
2. **完整性**: 确保每个赛事的所有套牌都被爬取
3. **时效性**: 建议每小时更新一次数据

---

## 性能要求

1. **响应时间**
   - 赛事列表: < 2 秒
   - 套牌列表: < 3 秒
   - 套牌详情: < 5 秒

2. **并发**
   - 支持至少 50 个并发请求
   - 建议添加缓存机制

3. **分页**
   - 赛事列表支持分页（limit/offset）
   - 套牌列表建议一次返回全部（通常 < 200 个）

---

## 测试用例

### 测试 1: 获取摩登赛事列表
```bash
GET /api/v1/events?format=MO&limit=10&offset=0
```

预期结果: 返回最近 10 个摩登赛事

### 测试 2: 获取指定日期的赛事
```bash
GET /api/v1/events?date=2025-01-15&limit=50&offset=0
```

预期结果: 返回 2025-01-15 的所有赛事

### 测试 3: 获取赛事详情
```bash
GET /api/v1/events/1
```

预期结果: 返回 ID 为 1 的赛事详细信息

### 测试 4: 获取赛事的套牌列表
```bash
GET /api/v1/events/1/decklists
```

预期结果: 返回该赛事的所有套牌（通常 32-128 个）

### 测试 5: 获取套牌详情（含卡牌）
```bash
GET /api/v1/decklists/101
```

预期结果: 返回套牌的完整信息，包括主牌和备牌的所有卡牌

---

## 数据库表结构参考

### events 表
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
    INDEX idx_date (date)
);
```

### decklists 表
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id),
    INDEX idx_event_id (event_id),
    INDEX idx_format (format)
);
```

### cards 表（卡牌，如果服务端需要存储）
```sql
CREATE TABLE cards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    decklist_id BIGINT NOT NULL,
    card_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    location ENUM('main', 'sideboard') NOT NULL,
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

---

## 联系方式

如有疑问，请联系：
- **安卓端开发**: [你的联系方式]
- **接口问题**: 请提供具体的接口 URL 和请求参数
- **数据问题**: 请提供赛事 ID 或套牌 ID

---

## 附录：卡牌数据示例

### 示例套牌：Izzet Phoenix

```json
{
  "id": 101,
  "eventId": 1,
  "eventName": "MTGO Challenge 32",
  "deckName": "Izzet Phoenix",
  "format": "MO",
  "date": "2025-01-15",
  "playerName": "PlayerOne",
  "record": "8-2",
  "mainDeck": [
    {
      "cardName": "Arclight Phoenix",
      "quantity": 4,
      "manaCost": "{2}{U}{R}",
      "displayName": "曙光凤凰",
      "rarity": "Mythic",
      "color": "U,R",
      "cardType": "Creature — Phoenix",
      "cardSet": "Modern Horizons 2"
    },
    {
      "cardName": "Treasure Cruise",
      "quantity": 4,
      "manaCost": "{U}",
      "displayName": "宝船巡航",
      "rarity": "Common",
      "color": "U",
      "cardType": "Sorcery",
      "cardSet": "Modern Horizons 2"
    },
    {
      "cardName": "Steam Vents",
      "quantity": 4,
      "manaCost": null,
      "displayName": "蒸汽吐口",
      "rarity": "Common",
      "color": "U,R",
      "cardType": "Land",
      "cardSet": "Modern Horizons 2"
    },
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
      "cardName": "Dress Down",
      "quantity": 2,
      "manaCost": "{1}{U}",
      "displayName": "压制",
      "rarity": "Rare",
      "color": "U",
      "cardType": "Enchantment",
      "cardSet": "Modern Horizons 2"
    },
    {
      "cardName": "Flame Buster",
      "quantity": 2,
      "manaCost": "{1}{R}",
      "displayName": "烈焰爆破",
      "rarity": "Uncommon",
      "color": "R",
      "cardType": "Instant",
      "cardSet": "Modern Horizons 2"
    }
  ]
}
```

### 特殊卡牌示例

#### 双面牌示例
```json
{
  "cardName": "Bounding // Aligned",
  "quantity": 4,
  "manaCost": "{G} // {W}",
  "displayName": "边界 // 对齐",
  "rarity": "Rare",
  "color": "G,W",
  "cardType": "Instant // Sorcery",
  "cardSet": "Kaldheim"
}
```

#### 连体牌示例
```json
{
  "cardName": "Wear // Tear",
  "quantity": 2,
  "manaCost": "{1}{R} // {W}",
  "displayName": "损耗 // 穿破",
  "rarity": "Rare",
  "color": "R,W",
  "cardType": "Instant // Instant",
  "cardSet": "Dragon's Maze"
}
```

#### 土地示例
```json
{
  "cardName": "Steam Vents",
  "quantity": 4,
  "manaCost": null,
  "displayName": "蒸汽吐口",
  "rarity": "Common",
  "color": "U,R",
  "cardType": "Land",
  "cardSet": "Modern Horizons 2"
}
```
