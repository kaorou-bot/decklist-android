# 双面牌显示问题 - 完整诊断报告

**诊断日期**: 2026-02-16
**问题状态**: 🔴 严重 - 服务端数据不完整
**影响范围**: 所有双面牌的图片和详细属性显示

---

## 📋 执行摘要

根据客户端日志分析和 DOUBLE_FACED_CARDS_GUIDE.md 文档规范，发现**自有服务端返回的双面牌数据严重不完整**，导致客户端无法正确显示双面牌图片和属性。

---

## 🔍 问题详情

### 1. 核心问题

**服务端返回的双面牌 `card_faces` 数组中，所有关键字段均为 `null`**：

| 字段 | 期望值 | 实际值 | 状态 |
|------|--------|--------|------|
| `card_faces[].image_uris` | 图片 URL 对象 | `null` | ❌ 缺失 |
| `card_faces[].mana_cost` | 法术力费用 | `null` | ❌ 缺失 |
| `card_faces[].oracle_text` | 规则文本 | `null` | ❌ 缺失 |
| `card_faces[].power` | 攻击力 | `null` | ❌ 缺失 |
| `card_faces[].toughness` | 防御力 | `null` | ❌ 缺失 |
| `other_faces[].image_uris` | 图片 URL 对象 | `null` | ❌ 缺失 |

### 2. 实际日志数据对比

#### 期望的数据格式（来自 DOUBLE_FACED_CARDS_GUIDE.md）

```json
{
  "card_faces": [
    {
      "name": "Reckless Waif",
      "name_zh": "鲁莽流浪儿",
      "mana_cost": "{R}",
      "type_line": "Creature — Human Rogue Werewolf",
      "type_line_zh": "生物 — 人类 浪客 狼人",
      "oracle_text": "At the beginning of each upkeep, if no spells were cast last turn, ...",
      "oracle_text_zh": "在每个维持开始时，如果上一回合没有施放法术，...",
      "power": "1",
      "toughness": "1",
      "colors": ["R"],
      "image_uris": {
        "small": "https://cards.scryfall.io/small/front/...",
        "normal": "https://cards.scryfall.io/normal/front/...",
        "large": "https://cards.scryfall.io/large/front/...",
        "png": "https://cards.scryfall.io/png/front/..."
      }
    }
  ]
}
```

#### 实际的服务端响应（来自日志）

```json
{
  "name": "Delver of Secrets // Insectile Aberration",
  "is_double_faced": true,
  "layout": "transform",
  "card_faces": [
    {
      "name": "Delver of Secrets",
      "name_zh": "掘密师",
      "mana_cost": null,              // ❌ 应该是 "{U}"
      "type_line": "Creature — Human Wizard // Creature — Human Insect",
      "type_line_zh": null,           // ❌ 缺失
      "oracle_text": null,            // ❌ 应该有规则文本
      "oracle_text_zh": null,         // ❌ 缺失
      "power": null,                  // ❌ 应该是 "1"
      "toughness": null,              // ❌ 应该是 "1"
      "loyalty": null,
      "colors": [],
      "image_uris": null              // ❌ 应该有图片 URL 对象
    },
    {
      "name": "Insectile Aberration",
      "name_zh": "昆虫变体",
      "mana_cost": null,              // ❌ 正确（背面没有法术力费用）
      "type_line": "Creature — Human Wizard // Creature — Human Insect",
      "type_line_zh": null,           // ❌ 缺失
      "oracle_text": null,            // ❌ 应该有规则文本
      "oracle_text_zh": null,         // ❌ 缺失
      "power": null,                  // ❌ 应该是 "3"
      "toughness": null,              // ❌ 应该是 "2"
      "loyalty": null,
      "colors": [],
      "image_uris": null              // ❌ 应该有图片 URL 对象
    }
  ],
  "other_faces": [
    {
      "name": "Insectile Aberration",
      "name_zh": "昆虫变体",
      "mana_cost": null,
      "type_line": "Creature — Human Wizard // Creature — Human Insect",
      "oracle_text": null,
      "power": null,
      "toughness": null,
      "loyalty": null,
      "colors": [],
      "image_uris": null              // ❌ 应该有图片 URL 对象
    }
  ]
}
```

### 3. 客户端检测日志

```
DecklistRepository: Cache check for Jwari Disruption:
  hasAnyBackData: false              // ❌ 检测到缺少背面数据
  backTypeLine:                        // ❌ 空字符串
  backIsCreature: false, backIsPlaneswalker: false
  backPower: null, backLoyalty: null
  backPowerMissing: false, backLoyaltyMissing: false
⚠ Dual-faced card needs refresh      // ⚠️  客户端标记需要刷新
```

---

## 🎯 问题影响

### 受影响的功能

| 功能模块 | 影响程度 | 具体表现 |
|---------|---------|---------|
| 双面牌图片显示 | 🔴 严重 | 无法显示任何一面的图片 |
| 双面牌详情查看 | 🔴 严重 | 无法显示攻击力、防御力、规则文本 |
| 双面牌搜索 | 🟡 中等 | 可以搜索到，但数据不完整 |
| 单面牌功能 | ✅ 正常 | 不受影响 |

### 受影响的双面牌类型

| 布局类型 | 示例 | 状态 |
|---------|------|------|
| `transform` | Delver of Secrets // Insectile Aberration | ❌ 受影响 |
| `modal_dfc` | Jwari Disruption // Jwari Ruins | ❌ 受影响 |
| `modal_dfc` | Sea Gate Restoration // Sea Gate, Reborn | ❌ 受影响 |
| `modal_dfc` | Beyeen Veil // Beyeen Coast | ❌ 受影响 |

---

## 🛠️ 解决方案

### 方案 1：修复服务端数据填充（推荐 ⭐）

#### 需要修复的问题

服务端在返回双面牌数据时，需要完整填充 `card_faces` 数组中的所有字段。

#### 推荐的数据源

使用 Scryfall API 获取完整的双面牌数据：

```javascript
// 伪代码示例
async function fetchDoubleFacedCard(cardId) {
  // 从 Scryfall 获取完整数据
  const scryfallData = await fetchFromScryfall(cardId);

  return {
    id: scryfallData.id,
    oracle_id: scryfallData.oracle_id,
    name: scryfallData.name,
    is_double_faced: scryfallData.layout === 'transform' ||
                     scryfallData.layout === 'modal_dfc',
    card_faces: scryfallData.card_faces.map(face => ({
      name: face.name,
      name_zh: getChineseTranslation(face.name),  // 需要翻译服务
      mana_cost: face.mana_cost,
      type_line: face.type_line,
      type_line_zh: getChineseTranslation(face.type_line),
      oracle_text: face.oracle_text,
      oracle_text_zh: getChineseTranslation(face.oracle_text),
      power: face.power,
      toughness: face.toughness,
      loyalty: face.loyalty,
      colors: face.colors,
      image_uris: face.image_uris  // ✅ 关键：必须有图片 URL
    })),
    other_faces: scryfallData.card_faces.slice(1).map(face => ({
      name: face.name,
      name_zh: getChineseTranslation(face.name),
      mana_cost: face.mana_cost,
      type_line: face.type_line,
      oracle_text: face.oracle_text,
      power: face.power,
      toughness: face.toughness,
      colors: face.colors,
      image_uris: face.image_uris  // ✅ 关键：必须有图片 URL
    }))
  };
}
```

#### 数据完整性检查清单

服务端在返回双面牌数据前，必须确保：

- [ ] `card_faces[].image_uris` 不为 `null`
- [ ] `card_faces[].mana_cost` 正确填充（正面有值，背面可能为 `null`）
- [ ] `card_faces[].oracle_text` 不为 `null`（如果有规则文本）
- [ ] `card_faces[].power` 不为 `null`（如果是生物）
- [ ] `card_faces[].toughness` 不为 `null`（如果是生物）
- [ ] `other_faces[].image_uris` 不为 `null`

### 方案 2：客户端回退机制（临时方案）

如果服务端暂时无法修复，客户端可以实现回退逻辑：

```kotlin
/**
 * 获取双面牌的图片 URL，如果服务端数据不完整，从 Scryfall 获取
 */
suspend fun getCardFaceImageUrl(card: MtgchCardDto, faceIndex: Int): String? {
    // 尝试从服务端数据获取
    if (card.cardFaces != null && card.cardFaces.size > faceIndex) {
        val face = card.cardFaces[faceIndex]
        if (face.imageUris != null && face.imageUris?.normal != null) {
            return face.imageUris?.normal
        }
    }

    // 服务端数据不完整，从 Scryfall 获取
    return fetchFromScryfall(card.oracleId, faceIndex)
}

private suspend fun fetchFromScryfall(oracleId: String, faceIndex: Int): String? {
    try {
        val response = scryfallApi.getCardByOracleId(oracleId)
        if (response.isSuccessful && response.body() != null) {
            val card = response.body()!!
            if (card.card_faces != null && card.card_faces.size > faceIndex) {
                val face = card.card_faces[faceIndex]
                return face.image_uris?.normal
            }
        }
    } catch (e: Exception) {
        AppLogger.e("DoubleFacedCard", "Failed to fetch from Scryfall", e)
    }
    return null
}
```

---

## 📊 测试验证

### 测试用例

#### 用例 1：验证 Delver of Secrets 数据完整性

```bash
curl "http://182.92.109.160/api/result?q=Delver" | jq '.items[] | select(.is_double_faced == true) | {
  name,
  card_faces: [
    {
      name: .card_faces[0].name,
      image_uris: .card_faces[0].image_uris,
      mana_cost: .card_faces[0].mana_cost,
      power: .card_faces[0].power,
      toughness: .card_faces[0].toughness
    },
    {
      name: .card_faces[1].name,
      image_uris: .card_faces[1].image_uris,
      power: .card_faces[1].power,
      toughness: .card_faces[1].toughness
    }
  ]
}'
```

**期望输出**：
```json
{
  "name": "Delver of Secrets // Insectile Aberration",
  "card_faces": [
    {
      "name": "Delver of Secrets",
      "image_uris": {
        "small": "https://cards.scryfall.io/small/front/...",
        "normal": "https://cards.scryfall.io/normal/front/...",
        "large": "https://cards.scryfall.io/large/front/...",
        "png": "https://cards.scryfall.io/png/front/..."
      },
      "mana_cost": "{U}",
      "power": "1",
      "toughness": "1"
    },
    {
      "name": "Insectile Aberration",
      "image_uris": {
        "small": "https://cards.scryfall.io/small/back/...",
        "normal": "https://cards.scryfall.io/normal/back/...",
        "large": "https://cards.scryfall.io/large/back/...",
        "png": "https://cards.scryfall.io/png/back/..."
      },
      "power": "3",
      "toughness": "2"
    }
  ]
}
```

#### 用例 2：检查 image_uris 是否为 null

```bash
# 检查所有双面牌的 image_uris
curl "http://182.92.109.160/api/result?q=Jwari" | jq '.items[] | select(.is_double_faced == true) |
  .card_faces[] | .image_uris'
```

**期望输出**：不应该有 `null` 值

---

## 📝 相关文档

- `DOUBLE_FACED_CARDS_GUIDE.md` - 双面牌 API 使用指南
- `API_DOCUMENTATION.md` - 自有服务端 API 文档
- `SelfHosted_API_Migration_Report.md` - API 迁移报告

---

## 🚀 行动计划

### 立即行动（P0 - 今日）

1. **联系服务端开发者**
   - 报告双面牌数据不完整问题
   - 提供本文档和日志
   - 说明需要填充的字段

2. **提供数据格式示例**
   - 分享 DOUBLE_FACED_CARDS_GUIDE.md
   - 提供期望的 JSON 响应格式
   - 说明 Scryfall API 的正确返回格式

3. **协助修复验证**
   - 测试修复后的 API
   - 验证双面牌数据完整性
   - 确认图片 URL 可访问

### 短期行动（P1 - 本周）

1. **实现客户端回退机制**
   - 检测 `image_uris` 为 `null`
   - 自动从 Scryfall 获取完整数据
   - 缓存完整数据

2. **增强错误处理**
   - 添加数据完整性检查
   - 显示友好的错误提示
   - 记录详细的错误日志

### 长期行动（P2 - 本月）

1. **完善数据同步**
   - 确保数据库中所有双面牌数据完整
   - 定期验证数据完整性
   - 自动修复不完整的数据

2. **优化用户体验**
   - 双面牌翻转动画
   - 双面牌并排显示
   - 双面牌对比功能

---

## 📞 联系信息

**服务端问题**: 需要服务端开发者修复数据填充逻辑
**客户端适配**: 可以实现临时回退机制
**测试验证**: 使用上述测试用例验证修复效果

**API 文档参考**:
- DOUBLE_FACED_CARDS_GUIDE.md（第 75-117 行）
- API_DOCUMENTATION.md（第 446-515 行）

---

**诊断结论**: 🔴 **服务端双面牌数据严重不完整 - `card_faces[].image_uris` 和所有属性字段均为 `null`，导致客户端无法显示双面牌图片和详情。**

**推荐方案**: ⭐ **立即修复服务端数据填充逻辑，确保 `card_faces` 数组包含完整的图片 URL 和属性数据。**

**临时方案**: 🔧 **客户端实现回退机制，当检测到数据不完整时，自动从 Scryfall API 获取完整数据。**

---

**报告生成时间**: 2026-02-16
**报告版本**: v1.0
**下次更新**: 服务端修复后
