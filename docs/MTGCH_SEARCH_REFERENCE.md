# MTGCH 高级搜索字段完整参考

> 基于 mtgch.com/search 的高级搜索页面完整分析
>
> **创建时间：** 2026-02-03
> **用途：** 完整复制 MTGCH 的搜索功能到 Android 应用

---

## 📋 一、字段总览表

| 序号 | 字段名称 | 代码参数 | 字段类型 | 是否已实现 |
|------|----------|----------|----------|------------|
| 1 | 名称 | `name` | 文本输入 | ✅ 已实现 |
| 2 | 规则概述 | `o`, `text` | 文本输入 | ❌ 缺失 |
| 3 | 类别 | `t`, `type` | 文本输入 | ✅ 部分实现 |
| 4 | 颜色/标识色 | `c`, `color` / `ci`, `color_identity` | 复选框+单选按钮+开关 | ⚠️ 部分实现 |
| 5 | 法术力值 | `mv`, `mana_value` | 下拉菜单+文本输入 | ✅ 已实现 |
| 6 | 力量/防御力 | `po`, `power` / `to`, `toughness` | 下拉菜单+文本输入 | ❌ 缺失 |
| 7 | 限制 | `s`, `set` | 双下拉菜单（赛制+可用性） | ❌ 缺失 |
| 8 | 系列 | `s`, `set` | 下拉菜单 | ✅ 已实现（文本输入） |
| 9 | 稀有度 | `r`, `rarity` | 复选框组 | ✅ 已实现 |
| 10 | 背景叙述 | `ft`, `flavor_text` | 文本输入 | ❌ 缺失 |
| 11 | 画师 | `a`, `artist` | 文本输入 | ❌ 缺失 |
| 12 | 游戏平台 | `game` | 单选按钮组 | ❌ 缺失 |
| 13 | 搜索额外卡牌 | `extras` | 复选框 | ❌ 缺失 |

---

## 📖 二、字段详细说明

### 1. 名称（name）✅ 已实现

**代码参数：** `name`

**字段类型：** 文本输入框

**输入方式：**
- 支持中英文输入
- 用于搜索卡牌名称（如"Lightning Bolt"、"闪电")

**示例查询：**
```
name:"Lightning Bolt"
name:"闪电"
```

**当前实现状态：** ✅ 在 SearchActivity 的搜索栏中

---

### 2. 规则概述（o, text）❌ 缺失

**代码参数：** `o`, `text`, `oracle`

**字段类型：** 文本输入框

**输入方式：**
- 支持中英文输入
- 用于搜索卡牌规则文本（Oracle Text）
- 搜索技能关键词（如"flying"、"haste"、"deathtouch"）

**示例查询：**
```
o:"flying"
o:"当此生物进场时"
o:"deals 3 damage"
```

**优先级：** 🔥 高 - 常用功能

---

### 3. 类别（t, type）⚠️ 部分实现

**代码参数：** `t`, `type`

**字段类型：** 文本输入框

**输入方式：**
- 支持中英文输入
- 当前实现为 Chip 预选（生物/瞬间/法术等）
- MTGCH 支持自由文本输入

**当前实现：** Chip 组选择（7种基本类型）
**MTGCH 实现：** 自由文本输入，支持更精确的类型搜索

**示例查询：**
```
t:"creature"
t:"creature — elf warrior"
t:"instant"
t:"legendary creature"
```

**建议：** 保留 Chip 快捷选择，增加自由文本输入

---

### 4. 颜色/标识色（c, ci）⚠️ 部分实现

**代码参数：**
- `c`, `color` - 颜色
- `ci`, `color_identity` - 颜色标识

**字段类型：**
- 颜色选择：6个复选框（白W、蓝U、黑B、红R、绿G、无色C）
- 颜色逻辑：3个单选按钮
  - "正好为所选颜色" → `c=wu`（精确匹配）
  - "至多为所选颜色" → `c<=wu`（最多这些颜色）
  - "至少包括所选颜色" → `c>=wu`（至少这些颜色）
- 标识色开关：切换是否搜索 `color_identity`

**当前实现：** 仅支持基本的颜色多选，缺少：
- ❌ 颜色逻辑选择（正好/至多/至少）
- ❌ 标识色开关

**示例查询：**
```
c:wu              # 正好为白蓝
c<=wu             # 至多为白蓝
c>=wu             # 至少为白蓝
ci:wubrg          # 标识色为五色
c:c               # 无色
```

**优先级：** 🚀 中 - 需要增强

---

### 5. 法术力值（mv, mana_value）✅ 已实现

**代码参数：** `mv`, `cmc`, `mana_value`

**字段类型：**
- 下拉菜单：选择比较运算符（=, <, >, <=, >=）
- 文本输入框：输入数值
- 清除按钮：清空输入

**示例查询：**
```
mv:3              # 法术力值等于3
mv>2              # 法术力值大于2
mv<=4             # 法术力值小于等于4
```

**当前实现状态：** ✅ 完整实现

---

### 6. 力量/防御力（po, power / to, toughness）❌ 缺失

**代码参数：**
- `po`, `power` - 力量
- `to`, `toughness` - 防御力

**字段类型：**
- 两个独立的"下拉菜单+文本输入框+清除按钮"组合
- 中间用"/"分隔

**示例查询：**
```
po:3              # 力量等于3
to:3              # 防御力等于3
po>=2             # 力量大于等于2
to<5              # 防御力小于5
po:3 to:3         # 2/2生物
```

**优先级：** 🔥 高 - 常用功能

---

### 7. 限制（format, legality）❌ 缺失

**代码参数：** `f`, `format`, `legality`

**字段类型：** 双下拉菜单
- 第一个下拉菜单：选择赛制
  - Standard（标准赛制）
  - Pioneer（先驱赛制）
  - Modern（摩登赛制）
  - Legacy（薪传赛制）
  - Vintage（复古赛制）
  - Commander（指挥官赛制）
  - Pauper（穷神赛制）
- 第二个下拉菜单：选择可用性
  - Legal（合法）
  - Banned（禁用）
  - Restricted（限用）

**示例查询：**
```
f:modern          # Modern合法的卡牌
f:standard l:legal # Standard合法的卡牌
f:commander l:banned # Commander被禁的卡牌
```

**优先级：** 🔥 高 - 非常常用

---

### 8. 系列（s, set）✅ 已实现（文本输入）

**代码参数：** `s`, `set`, `code`

**字段类型：** 下拉菜单（MTGCH）/ 文本输入框（当前实现）

**输入方式：**
- 当前：文本输入 3-4 字母系列代码（如"MOM"、"ONE"、"MOM2"）
- MTGCH：下拉菜单选择所有系列

**示例查询：**
```
s:MOM             # March of the Machine
s:ONE             # One Ring
s:CMR             # Commander Masters
```

**当前实现状态：** ✅ 文本输入可用
**建议：** 考虑添加系列选择器（可选）

---

### 9. 稀有度（r, rarity）✅ 已实现

**代码参数：** `r`, `rarity`

**字段类型：** 复选框组（5个选项）

**选项：**
- common（普通）
- uncommon（非普通）
- rare（稀有）
- mythic（秘稀）
- special（特殊）- ❌ 当前缺失

**示例查询：**
```
r:common          # 普通
r:rare            # 稀有
r:mythic          # 秘稀
```

**当前实现状态：** ✅ 已实现（缺少"特殊"选项）

---

### 10. 背景叙述（ft, flavor_text）❌ 缺失

**代码参数：** `ft`, `flavor`, `flavor_text`

**字段类型：** 文本输入框

**输入方式：**
- 支持中英文输入
- 搜索卡牌的背景文本（Flavor Text）

**示例查询：**
```
ft:"bolt of lightning"
ft:"The Gathering"
```

**优先级：** 🎨 低 - 锦上添花

---

### 11. 画师（a, artist）❌ 缺失

**代码参数：** `a`, `artist`

**字段类型：** 文本输入框

**输入方式：**
- 支持中英文输入
- 搜索卡牌画师姓名

**示例查询：**
```
a:"John Avon"
a:"Kev Walker"
a:"Terese Nielsen"
```

**优先级：** 🎨 低 - 锦上添花

---

### 12. 游戏平台（game）❌ 缺失

**代码参数：** `game`

**字段类型：** 单选按钮组

**选项：**
- 纸牌（Paper）
- MTGO（万智牌在线版）
- Arena（万智牌竞技场）

**用途：** 筛选特定平台上可用的卡牌

**示例查询：**
```
game:paper
game:arena
game:mtgo
```

**优先级：** 🚀 中 - 部分用户需要

---

### 13. 搜索额外卡牌（extras）❌ 缺失

**代码参数：** `extras`, `is`

**字段类型：** 复选框

**用途：** 是否包含特殊卡牌类型
- 命令者（Commander）
- 时空裂隙卡牌（Planar cards）
- 王牌卡（Token）
- 变异卡（Transform cards）

**示例查询：**
```
is:commander
is:token
is:transform
```

**优先级：** 🚀 中 - 特定赛制需要

---

## 🎯 三、UI 布局参考

### 整体布局结构

```
┌─────────────────────────────────────┐
│           高级搜索                    │
├─────────────────────────────────────┤
│ 1. 名称                    [输入框]  │
│ 2. 规则概述                [输入框]  │
│ 3. 类别                    [输入框]  │
│ 4. 颜色/标识色                       │
│    ☑W ☑U ☑B ☑R ☑G ☑C              │
│    ⚪正好为所选 ⚪至多为所选 ⚪至少包括 │
│    [搜索标识色开关]                  │
│ 5. 法术力值                          │
│    [任意 ▼] [输入框] [清除]         │
│ 6. 力量/防御力                       │
│    [任意 ▼] [输入框] / [任意 ▼] [输入框] │
│ 7. 限制                              │
│    [选择赛制 ▼] [可用性 ▼]          │
│ 8. 系列                    [下拉菜单] │
│ 9. 稀有度                           │
│    ☑普通 ☑非普通 ☑稀有 ☑秘稀 ☑特殊  │
│ 10. 背景叙述              [输入框]   │
│ 11. 画师                  [输入框]   │
│ 12. 游戏平台                         │
│    ⚪纸牌 ⚪MTGO ⚪Arena             │
│ 13. 搜索额外卡牌                    │
│    ☑ 包含额外卡牌                    │
├─────────────────────────────────────┤
│ [添加到条件组] [清除已选] [搜索]     │
│ [OR] [NOT] [()]                     │
│ [以当前条件组搜索]                   │
└─────────────────────────────────────┘
```

---

## 🔧 四、搜索查询语法参考

### 基础语法

```
field:value          # 精确匹配
field:"text"         # 文本匹配
field>=value         # 大于等于
field<=value         # 小于等于
field>value          # 大于
field<value          # 小于
field=value          # 等于
```

### 逻辑运算符

```
c:w c:u             # AND（默认）
c:w OR c:u          # OR
c:w -c:u            # NOT（排除）
(c:w c:u) OR c:b    # 括号分组
```

### 复合查询示例

```
# 搜索蓝色法术，法术力值<=3
c:u t:instant mv<=3

# 搜索红绿双色生物，力量>=3
c:rg t:creature po>=3

# 搜索 Modern 合法的飞行生物
f:modern o:"flying" t:creature

# 搜索 John Avon 画的稀有卡
a:"John Avon" r:rare

# 搜索 Commander 未禁的绿色卡牌
f:commander -l:banned c:g

# 搜索双面牌
layout:transform

# 搜索特定系列
s:MOM r:mythic
```

---

## 📊 五、实现优先级建议

### 🔥 P0 - 立即实现（高优先级）

1. **力量/防御力筛选** - 生物类套牌必用
2. **规则概述搜索** - 搜索技能关键词（飞行、践踏等）
3. **赛制合法性筛选** - Modern/Standard/Pioneer/Commander

### 🚀 P1 - 后续实现（中优先级）

4. **颜色逻辑增强** - 添加"正好/至多/至少"逻辑
5. **颜色标识开关** - 独立的标识色搜索
6. **游戏平台筛选** - Paper/MTGO/Arena
7. **额外卡牌选项** - 命令者、Token等

### 🎨 P2 - 可选实现（低优先级）

8. **背景叙述搜索** - Flavor Text
9. **画师搜索** - Artist
10. **类型自由输入** - 增强当前Chip选择

---

## 📝 六、数据结构设计

### SearchFilters 数据类（完整版）

```kotlin
/**
 * 完整的搜索过滤器（参考 MTGCH）
 */
data class SearchFilters(
    // 1. 基础字段
    val name: String? = null,                    // 名称

    // 2. 规则与类型
    val oracleText: String? = null,              // 规则概述 (o, text)
    val type: String? = null,                    // 类别 (t, type)

    // 3. 颜色筛选
    val colors: List<String> = emptyList(),      // 颜色 (c:wubrg)
    val colorMode: ColorMatchMode = ColorMatchMode.ANY,  // 颜色匹配模式
    val colorIdentity: List<String>? = null,     // 颜色标识 (ci:wubrg)
    val searchColorIdentity: Boolean = false,    // 是否搜索标识色

    // 4. 数值筛选
    val manaValue: NumericFilter? = null,        // 法术力值 (mv)
    val power: NumericFilter? = null,            // 力量 (po)
    val toughness: NumericFilter? = null,        // 防御力 (to)

    // 5. 赛制与系列
    val format: String? = null,                  // 赛制 (f:modern)
    val legality: LegalityMode? = null,          // 可用性 (legal/banned/restricted)
    val setCode: String? = null,                 // 系列 (s:MOM)

    // 6. 其他属性
    val rarity: List<String>? = null,            // 稀有度 (r:mythic)
    val flavorText: String? = null,              // 背景叙述 (ft)
    val artist: String? = null,                  // 画师 (a)
    val game: GamePlatform? = null,              // 游戏平台
    val includeExtras: Boolean = false,          // 包含额外卡牌
)

/**
 * 颜色匹配模式
 */
enum class ColorMatchMode {
    EXACT,      // 正好为所选颜色 (c=wu)
    AT_MOST,    // 至多为所选颜色 (c<=wu)
    AT_LEAST    // 至少包括所选颜色 (c>=wu)
}

/**
 * 数值筛选器
 */
data class NumericFilter(
    val operator: CompareOperator,  // =, >, <, >=, <=
    val value: Int
)

enum class CompareOperator {
    EQUAL, GREATER, LESS, GREATER_EQUAL, LESS_EQUAL, ANY
}

/**
 * 合法性模式
 */
enum class LegalityMode {
    LEGAL,          // 合法
    BANNED,         // 禁用
    RESTRICTED      // 限用
}

/**
 * 游戏平台
 */
enum class GamePlatform {
    PAPER,      // 纸牌
    MTGO,       // 万智牌在线版
    ARENA       // 万智牌竞技场
}
```

---

## 🔧 七、查询构建函数（完整版）

```kotlin
/**
 * 构建完整的搜索查询字符串
 */
private fun buildSearchQuery(
    query: String,
    filters: SearchFilters?
): String {
    val parts = mutableListOf<String>()

    filters?.let {
        // 1. 名称（如果query不是名称）
        if (query.isNotBlank()) {
            parts.add("name:$query")
        }

        // 2. 规则概述 (o, text)
        it.oracleText?.let { text ->
            parts.add("o:\"$text\"")
        }

        // 3. 类型 (t, type)
        it.type?.let { type ->
            parts.add("t:\"$type\"")
        }

        // 4. 颜色筛选 (c)
        if (it.colors.isNotEmpty()) {
            val colorStr = it.colors.joinToString("")
            val operator = when (it.colorMode) {
                ColorMatchMode.EXACT -> "="
                ColorMatchMode.AT_MOST -> "<="
                ColorMatchMode.AT_LEAST -> ">="
            }
            parts.add("c$operator$colorStr")
        }

        // 5. 颜色标识 (ci)
        if (it.searchColorIdentity && it.colorIdentity != null) {
            val ciStr = it.colorIdentity.joinToString("")
            parts.add("ci:$ciStr")
        }

        // 6. 法术力值 (mv)
        it.manaValue?.let { mv ->
            parts.add("mv${mv.operator.op}${mv.value}")
        }

        // 7. 力量 (po)
        it.power?.let { p ->
            parts.add("po${p.operator.op}${p.value}")
        }

        // 8. 防御力 (to)
        it.toughness?.let { t ->
            parts.add("to${t.operator.op}${t.value}")
        }

        // 9. 赛制与合法性 (f, l)
        if (it.format != null && it.legality != null) {
            parts.add("f:${it.format} l:${it.legality.value}")
        } else if (it.format != null) {
            parts.add("f:${it.format}")
        }

        // 10. 系列 (s)
        it.setCode?.let { set ->
            parts.add("s:$set")
        }

        // 11. 稀有度 (r)
        it.rarity?.let { rarities ->
            rarities.forEach { rarity ->
                parts.add("r:$rarity")
            }
        }

        // 12. 背景叙述 (ft)
        it.flavorText?.let { ft ->
            parts.add("ft:\"$ft\"")
        }

        // 13. 画师 (a)
        it.artist?.let { artist ->
            parts.add("a:\"$artist\"")
        }

        // 14. 游戏平台
        it.game?.let { game ->
            parts.add("game:${game.value}")
        }

        // 15. 额外卡牌
        if (it.includeExtras) {
            parts.add("is:extra")
        }
    }

    // 如果没有任何筛选条件，直接返回query
    if (parts.isEmpty() && query.isNotBlank()) {
        return query
    }

    return parts.joinToString(" ")
}

/**
 * 比较运算符转字符串
 */
private val CompareOperator.op: String
    get() = when (this) {
        CompareOperator.EQUAL -> "="
        CompareOperator.GREATER -> ">"
        CompareOperator.LESS -> "<"
        CompareOperator.GREATER_EQUAL -> ">="
        CompareOperator.LESS_EQUAL -> "<="
        CompareOperator.ANY -> ""
    }

/**
 * 合法性模式转字符串
 */
private val LegalityMode.value: String
    get() = when (this) {
        LegalityMode.LEGAL -> "legal"
        LegalityMode.BANNED -> "banned"
        LegalityMode.RESTRICTED -> "restricted"
    }

/**
 * 游戏平台转字符串
 */
private val GamePlatform.value: String
    get() = when (this) {
        GamePlatform.PAPER -> "paper"
        GamePlatform.MTGO -> "mtgo"
        GamePlatform.ARENA -> "arena"
    }
```

---

## 📦 八、UI 组件清单

### 需要新增的 UI 组件

1. **高级搜索布局文件**（增强 `bottom_sheet_advanced_search.xml`）
   - 添加规则概述输入框
   - 添加力量/防御力筛选行
   - 添加赛制合法性双下拉菜单
   - 添加背景叙述输入框
   - 添加画师输入框
   - 添加游戏平台单选按钮
   - 添加额外卡牌复选框
   - 增强颜色区域（添加逻辑选择和标识色开关）

2. **数据类文件**
   - `SearchFilters.kt` - 完整的搜索过滤器数据类
   - `ColorMatchMode.kt` - 颜色匹配模式枚举
   - `NumericFilter.kt` - 数值筛选器数据类
   - `LegalityMode.kt` - 合法性模式枚举
   - `GamePlatform.kt` - 游戏平台枚举

3. **枚举类**
   - `CompareOperator.kt` - 比较运算符枚举

---

## 🎯 九、实施建议

### 阶段 1：核心字段补充（1-2天）
1. 力量/防御力筛选
2. 规则概述搜索
3. 赛制合法性筛选

### 阶段 2：颜色逻辑增强（1天）
4. 颜色匹配模式（正好/至多/至少）
5. 颜色标识开关

### 阶段 3：辅助字段（1天）
6. 背景叙述
7. 画师
8. 游戏平台
9. 额外卡牌

### 阶段 4：UI 优化（1天）
10. 重新设计高级搜索底部表单
11. 添加条件组功能（可选）
12. 添加逻辑运算符（可选）

---

## 📚 十、参考资料

- MTGCH 官网：https://mtgch.com/search
- Scryfall 搜索语法：https://scryfall.com/docs/syntax
- 当前实现：`app/src/main/res/layout/bottom_sheet_advanced_search.xml`
- 当前实现：`app/src/main/java/com/mtgo/decklistmanager/ui/search/SearchViewModel.kt`

---

**文档版本：** v1.0
**最后更新：** 2026-02-03
**维护者：** Claude Code
