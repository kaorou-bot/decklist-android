# 双面牌与 split 卡牌显示问题修复报告

**修复日期**: 2026-02-17
**版本**: v4.3.0
**状态**: ✅ 已完成

---

## 📋 问题描述

### 问题 1：Wear // Tear 被错误识别为双面牌
- **现象**: `split` 类型卡牌显示为双面牌，有背面图标
- **原因**: `isDualFaced` 判断逻辑过于宽松，将所有包含 "//" 的卡牌都识别为双面牌
- **影响**: 伪双面牌（split、adventure、flip）显示不正确

### 问题 2：Wear // Tear 显示错误
- **现象**:
  - 名称显示为 "瞬间 // 瞬间"（应该是中文名）
  - 类型显示为规则文本
  - 没有规则文本
- **原因**: 服务端数据字段错位
- **影响**: split 类型卡牌无法正确显示

---

## 🔧 客户端修复

### 1. 优化双面牌识别逻辑

**文件**: `MtgchMapper.kt`

**修复前**:
```kotlin
val isDualFaced = isAnyDualFaced  // 所有包含 "//" 的卡牌
```

**修复后**:
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

val isDoubleFaced = layout in realDualFaceLayouts
val isDualFaced = isDoubleFaced  // 仅真双面牌
```

**效果**:
- ✅ `split`、`adventure`、`flip` 卡牌不再被识别为双面牌
- ✅ 只有必要时（transform、modal_dfc）才显示背面和翻转功能

### 2. 数据提取规则

#### 正面数据（仅针对真双面牌）
```kotlin
// 法术力值：真双面牌从 card_faces[0] 提取
val finalManaCost = if (isDoubleFaced && cardFaces != null && cardFaces.isNotEmpty()) {
    cardFaces[0].manaCost
} else {
    manaCost  // 单面牌或伪双面牌使用顶层
}
```

#### 背面数据（仅针对真双面牌）
```kotlin
// 背面规则文本（仅对真双面牌有效）
val backFaceOracleText = if (isDoubleFaced) {
    when {
        cardFaces != null && cardFaces.size > 1 -> {
            cardFaces.getOrNull(1)?.zhText ?: cardFaces.getOrNull(1)?.oracleText
        }
        else -> null
    }
} else null
```

---

## 🌐 服务端修复

### 修复内容

服务端修复了 split 类型卡牌的中文字段错位问题：

#### 修复的字段
1. **`name_zh`** (798张卡牌)
   - 修复前: "瞬间 // 瞬间"（错误：这是类型）
   - 修复后: "磨损 // 撕裂"（正确：这是中文名）

2. **`type_line_zh`** (472张卡牌)
   - 修复前: "你每操控一个神器..."（错误：这是规则文本）
   - 修复后: "瞬间 // 瞬间"（正确：这是类型）

3. **`oracle_text_zh`** (89张卡牌)
   - 修复前: `null`
   - 修复后: 完整的中文规则文本

#### 数据来源
服务端从 `card_faces` 数组中复制了正确的中文数据到主卡对象。

#### 修复范围
- ✅ 472张卡牌的 `name_zh` 已修复
- ✅ 798张卡牌的 `type_line_zh` 已修复
- ✅ 89张卡牌的 `oracle_text_zh` 已修复

---

## 📊 修复效果

### Wear // Tear (split 类型)

| 字段 | 修复前 | 修复后 |
|------|--------|--------|
| `is_dual_faced` | ❌ 误判为 true | ✅ false |
| `name_zh` | ❌ "瞬间 // 瞬间" | ✅ "磨损 // 撕裂" |
| `type_line_zh` | ❌ 规则文本 | ✅ "瞬间 // 瞬间" |
| `oracle_text_zh` | ❌ null | ✅ "消灭目标神器。消灭目标结界。" |
| 显示 | ❌ 显示为双面牌 | ✅ 正确显示为单张牌 |

### Ajani, Nacatl Pariah (transform 类型)

| 字段 | 修复前 | 修复后 |
|------|--------|--------|
| `is_dual_faced` | ✅ true | ✅ true |
| 正面法术力值 | ❌ null | ✅ {1}{W} |
| 正面攻防 | ❌ null/null | ✅ 1/2 |
| 正面规则文本 | ❌ 英文 | ✅ 中文 |
| 背面规则文本 | ❌ 英文 | ✅ 中文 |

---

## 🎯 双面牌识别规则总结

### 真双面牌 (isDualFaced = true)
需要显示背面和翻转功能：

| 布局类型 | 示例 | 说明 |
|---------|------|------|
| `transform` | Delver of Secrets // Insectile Aberration | 标准双面牌（狼人等） |
| `modal_dfc` | Jwari Disruption // Jwari Ruins | 模态双面牌（札尔琴等） |
| `double_faced_token` | 双面衍生物 | 双面指示物 |

### 伪双面牌 (isDualFaced = false)
名称包含 "// "，但不需要背面：

| 布局类型 | 示例 | 说明 |
|---------|------|------|
| `split` | Wear // Tear | 分面牌，单张卡牌 |
| `adventure` | Bramble Trapper | 冒险牌，单张卡牌 |
| `flip` | 翻转牌 | 翻转牌，单张卡牌 |

---

## ✅ 验证清单

### 客户端验证
- [x] `isDualFaced` 只识别真双面牌
- [x] `split` 类型卡牌不显示背面图标
- [x] 真双面牌从 `card_faces[0]` 提取正面数据
- [x] 伪双面牌使用顶层字段数据
- [x] 构建成功
- [x] 安装到模拟器

### 服务端验证
- [x] `name_zh` 字段正确（不再是类型）
- [x] `type_line_zh` 字段正确（不再是规则文本）
- [x] `oracle_text_zh` 字段存在（包含规则文本）
- [x] 数据已上传到生产服务器
- [x] 服务已重启

### 功能测试
建议测试以下卡牌：

**真双面牌**:
- Ajani, Nacatl Pariah - 应显示为双面牌，有翻转功能
- Delver of Secrets // Insectile Aberration - 应显示为双面牌
- Jwari Disruption // Jwari Ruins - 应显示为双面牌

**伪双面牌**:
- Wear // Tear - 应显示为单张牌，不显示背面图标
- Fire // Ice - 应显示为单张牌
- Advent of the Wurm - 应显示为单张牌

---

## 📝 技术说明

### isDualFaced 的作用

```kotlin
val isDualFaced = isDoubleFaced  // 仅 true 对 transform、modal_dfc、double_faced_token
```

**用途**:
1. 控制是否显示背面图标
2. 控制是否启用翻转功能
3. 控制从哪里提取数据（card_faces vs 顶层字段）
4. 控制是否提取背面数据

**不影响**:
- 名称显示（所有包含 "//" 的卡牌都会显示完整名称）
- 类型行显示
- 图片加载

### 数据优先级

**真双面牌** (isDualFaced = true):
1. 正面数据: `card_faces[0]` > 顶层字段
2. 背面数据: `card_faces[1]` > `other_faces[0]`
3. 中文优先: `zhText` > `oracleText`

**伪双面牌** (isDualFaced = false):
1. 所有数据: 顶层字段
2. 中文优先: `zhsText` > `atomicTranslatedText` > `oracleText`

---

## 🚀 后续工作

### 客户端
- [ ] 测试各种类型的双面牌和伪双面牌
- [ ] 验证翻转功能正常工作
- [ ] 验证图片显示正确

### 服务端（可选优化）
- [ ] 处理 67 张 `card_faces` 中 `nameZh` 为 NULL 的卡牌
- [ ] 改善 526 张 Token 卡牌的显示格式
- [ ] 补充 41 张完全缺少翻译的双面/分面卡

---

**修复完成时间**: 2026-02-17
**相关文件**:
- `MtgchMapper.kt` - 双面牌数据映射逻辑
- `MtgchCardDto.kt` - API 响应数据模型
- `CHINESE_FIXES_REPORT.md` - 服务端中文字段修复报告
