# 双面牌识别逻辑实现报告

**更新日期**: 2026-02-16
**版本**: v4.3.0
**状态**: ✅ 已完成

---

## 📋 概述

根据用户提供的需求，实现了精细化的双面牌识别逻辑，正确区分**真双面牌**和**伪双面牌**。

---

## 🎯 核心概念

### 1. 真双面牌（Real Dual-Faced Cards）

**定义**：具有正反两个独立面，需要显示背面信息和翻转功能。

**布局类型**：
- `transform` - 标准双面牌（如：狼人双面牌）
- `modal_dfc` - 模态双面牌（如：札尔琴的地窖）
- `double_faced_token` - 双面指示物

**特征**：
- 正面和背面都是完整的卡牌
- 背面有独立的图片、规则文本、属性
- 需要支持翻转查看背面

### 2. 伪双面牌（Pseudo Dual-Faced Cards）

**定义**：单张卡牌，但名称中包含 " // "，不需要显示背面。

**布局类型**：
- `split` - 分面牌（如：Consecrate // Consume）
- `adventure` - 冒险牌
- `flip` - 翻转牌

**特征**：
- 名称包含 " // " 分隔符
- 实际上是单张卡牌
- 不需要显示背面（因为所有信息都在正面）

---

## 🔧 实现细节

### 代码位置

`app/src/main/java/com/mtgo/decklistmanager/data/remote/api/mtgch/MtgchMapper.kt`

### 判断逻辑

```kotlin
// 真双面牌（需要显示背面）
val realDualFaceLayouts = listOf(
    "transform",           // 标准双面牌（如：狼人）
    "modal_dfc",           // 模态双面牌（如：札尔琴的地窖）
    "double_faced_token"   // 双面指示物
)

// 伪双面牌（名字包含"//"但是单张牌）
val pseudoDualFaceLayouts = listOf(
    "split",               // 分面牌（如：Consecrate // Consume）
    "adventure",           // 冒险牌
    "flip"                // 翻转牌
)

// 判断是否为真双面牌（需要显示背面）
val isDoubleFaced = layout in realDualFaceLayouts

// 判断是否为伪双面牌（名字包含"//"但是单张牌）
val isPseudoDoubleFaced = layout in pseudoDualFaceLayouts

// 判断是否为任何类型的双面牌（包括真双面和伪双面）
val isAnyDualFaced = isDoubleFaced || isPseudoDoubleFaced
    || (layout in otherLayouts)
    || (cardFaces != null && cardFaces.isNotEmpty())
    || (otherFaces != null && otherFaces.isNotEmpty())
    || (imageUris == null && zhsImageUris == null)
    || (name?.contains(" // ") == true)
    || (zhsName?.contains("//") == true)

// 兼容旧代码：使用 isAnyDualFaced 作为主要判断
val isDualFaced = isAnyDualFaced
```

### 数据提取规则

#### 1. 正面数据（仅针对真双面牌）

```kotlin
// 法术力值：真双面牌从 card_faces[0] 提取，伪双面牌使用顶层
val finalManaCost = if (isDoubleFaced && cardFaces != null && cardFaces.isNotEmpty()) {
    cardFaces[0].manaCost  // 使用正面的法术力值
} else {
    manaCost  // 单面牌或伪双面牌使用顶层法术力值
}

// 攻击力：真双面牌从 card_faces[0] 提取
val finalPower = if (isDoubleFaced && cardFaces != null && cardFaces.isNotEmpty() && cardFaces[0].power != null) {
    cardFaces[0].power
} else {
    power
}

// 防御力：真双面牌从 card_faces[0] 提取
val finalToughness = if (isDoubleFaced && cardFaces != null && cardFaces.isNotEmpty() && cardFaces[0].toughness != null) {
    cardFaces[0].toughness
} else {
    toughness
}

// 忠诚度：真双面牌从 card_faces[0] 提取
val finalLoyalty = if (isDoubleFaced && cardFaces != null && cardFaces.isNotEmpty() && cardFaces[0].loyalty != null) {
    cardFaces[0].loyalty
} else {
    loyalty
}

// 规则文本：真双面牌从 card_faces[0] 提取
val finalOracleText = if (isDoubleFaced && cardFaces != null && cardFaces.isNotEmpty()) {
    // 优先使用中文规则文本，其次英文
    cardFaces[0].zhText ?: cardFaces[0].oracleText
} else {
    zhsText ?: atomicTranslatedText ?: oracleText
}
```

#### 2. 背面数据（仅针对真双面牌）

```kotlin
// 背面法术力值
val backFaceManaCost = if (isDoubleFaced) {
    when {
        cardFaces != null && cardFaces.size > 1 -> {
            cardFaces.getOrNull(1)?.manaCost
        }
        otherFaces != null && otherFaces.isNotEmpty() -> {
            otherFaces[0].manaCost
        }
        else -> null
    }
} else null

// 背面类型行
val backFaceTypeLine = if (isDoubleFaced) {
    when {
        cardFaces != null && cardFaces.size > 1 -> {
            cardFaces.getOrNull(1)?.zhTypeLine ?: cardFaces.getOrNull(1)?.typeLine
        }
        otherFaces != null && otherFaces.isNotEmpty() -> {
            otherFaces[0].zhsTypeLine ?: otherFaces[0].atomicTranslatedType ?: otherFaces[0].typeLine
        }
        else -> null
    }
} else null

// 背面规则文本
val backFaceOracleText = if (isDoubleFaced) {
    when {
        cardFaces != null && cardFaces.size > 1 -> {
            // 优先使用中文规则文本
            cardFaces.getOrNull(1)?.zhText ?: cardFaces.getOrNull(1)?.oracleText
        }
        otherFaces != null && otherFaces.isNotEmpty() -> {
            // 优先使用官方中文，其次机器翻译，最后英文原文
            otherFaces[0].zhsText ?: otherFaces[0].atomicTranslatedText ?: otherFaces[0].oracleText
        }
        else -> null
    }
} else null

// 背面攻击力（仅生物）
val backFacePower = if (isDoubleFaced) {
    when {
        cardFaces != null && cardFaces.size > 1 -> {
            cardFaces.getOrNull(1)?.power
        }
        otherFaces != null && otherFaces.isNotEmpty() -> {
            otherFaces[0].power
        }
        else -> null
    }
} else null

// 背面防御力（仅生物）
val backFaceToughness = if (isDoubleFaced) {
    when {
        cardFaces != null && cardFaces.size > 1 -> {
            cardFaces.getOrNull(1)?.toughness
        }
        otherFaces != null && otherFaces.isNotEmpty() -> {
            otherFaces[0].toughness
        }
        else -> null
    }
} else null

// 背面忠诚度（仅鹏洛客）
val backFaceLoyalty = if (isDoubleFaced) {
    when {
        cardFaces != null && cardFaces.size > 1 -> {
            cardFaces.getOrNull(1)?.loyalty
        }
        otherFaces != null && otherFaces.isNotEmpty() -> {
            otherFaces[0].loyalty
        }
        else -> null
    }
} else null
```

---

## 📊 测试用例

### 测试卡牌示例

#### 1. Ajani, Nacatl Pariah（真双面牌）

**布局**: `transform`
**预期行为**:
- ✅ 显示正面法术力值：{1}{W}
- ✅ 显示正面攻防：1/2
- ✅ 显示正面规则文本（中文）
- ✅ 可以翻转查看背面
- ✅ 背面显示中文文本

#### 2. Wear // Tear（伪双面牌 - split）

**布局**: `split`
**预期行为**:
- ✅ 名称显示为 "Wear // Tear"
- ✅ 法术力值从顶层获取（不使用 card_faces[0]）
- ✅ 不显示背面（因为不是真双面牌）

#### 3. Jwari Disruption（伪双面牌 - modal_dfc）

**布局**: `modal_dfc`
**预期行为**:
- ✅ 识别为真双面牌（modal_dfc 在 realDualFaceLayouts 中）
- ✅ 显示正面数据
- ✅ 可以翻转查看背面

#### 4. Bramble Trapper（冒险牌 - adventure）

**布局**: `adventure`
**预期行为**:
- ✅ 识别为伪双面牌
- ✅ 不显示背面
- ✅ 使用顶层法术力值和属性

---

## 🔍 字段映射说明

### MtgchCardDto 顶层字段

| 字段名 | 说明 | 使用场景 |
|--------|------|---------|
| `layout` | 布局类型 | 判断是否为双面牌 |
| `manaCost` | 法术力费用 | 单面牌和伪双面牌 |
| `power` | 攻击力 | 单面牌和伪双面牌 |
| `toughness` | 防御力 | 单面牌和伪双面牌 |
| `loyalty` | 忠诚度 | 单面牌和伪双面牌 |
| `oracleText` | 规则文本 | 单面牌和伪双面牌 |
| `zhsText` | 中文规则文本 | 单面牌和伪双面牌 |
| `zhsTypeLine` | 中文类型行 | 单面牌和伪双面牌 |

### CardFace 字段

| 字段名 | 说明 | 使用场景 |
|--------|------|---------|
| `manaCost` | 面的法术力费用 | 真双面牌的正面和背面 |
| `power` | 面的攻击力 | 真双面牌的正面和背面 |
| `toughness` | 面的防御力 | 真双面牌的正面和背面 |
| `loyalty` | 面的忠诚度 | 真双面牌的正面和背面 |
| `oracleText` | 面的规则文本 | 真双面牌的正面和背面 |
| `zhText` | 面的中文规则文本 | 真双面牌的正面和背面 |
| `zhTypeLine` | 面的中文类型行 | 真双面牌的正面和背面 |
| `imageUris` | 面的图片 URL | 真双面牌的正面和背面 |

---

## 📝 注意事项

### 1. 数据提取优先级

**真双面牌**:
1. 优先从 `card_faces` 数组提取
2. 如果 `card_faces` 不可用，从 `other_faces` 提取
3. 中文优先：`zhText` > `oracleText`，`zhTypeLine` > `typeLine`

**伪双面牌**:
1. 直接使用顶层字段（`manaCost`, `power`, `toughness` 等）
2. 不需要提取背面数据

### 2. 字段名称差异

**CardFace** (card_faces 数组中的对象):
- `zhText` - 中文规则文本
- `zhTypeLine` - 中文类型行
- `zhName` - 中文名称

**MtgchCardDto** (otherFaces 中的对象):
- `zhsText` - 中文规则文本
- `zhsTypeLine` - 中文类型行
- `zhsName` - 中文名称
- `zhsFaceName` - 中文面名称

### 3. 兼容性

为了保持向后兼容，代码同时支持：
- `isDualFaced` - 宽松判断（包括伪双面牌）
- `isDoubleFaced` - 严格判断（仅真双面牌）

---

## ✅ 完成状态

- [x] 实现真双面牌识别逻辑
- [x] 实现伪双面牌识别逻辑
- [x] 修复正面数据提取（法术力值、攻防、规则文本）
- [x] 修复背面数据提取（仅真双面牌）
- [x] 修复字段映射（zhsTypeLine, zhsText）
- [x] 构建成功
- [x] 安装到模拟器

---

## 🚀 下一步

1. **测试验证**：在应用中搜索 "Ajani, Nacatl Pariah" 验证：
   - 正面法术力值显示为 {1}{W}
   - 正面攻防显示为 1/2
   - 正面规则文本显示中文
   - 可以翻转查看背面
   - 背面规则文本显示中文

2. **伪双面牌测试**：搜索 "Wear // Tear" 验证：
   - 识别为伪双面牌
   - 不显示背面
   - 使用顶层法术力值

3. **日志分析**：查看应用日志，确认数据提取逻辑正确

---

**报告生成时间**: 2026-02-16
**相关文件**:
- `MtgchMapper.kt` - 双面牌数据映射逻辑
- `MtgchCardDto.kt` - API 响应数据模型
- `DOUBLE_FACED_CARDS_GUIDE.md` - 双面牌使用指南
- `DOUBLE_FACED_CARDS_ISSUE_ANALYSIS.md` - 问题分析报告
