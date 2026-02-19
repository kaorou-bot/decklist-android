# MTGTop8 Format 参数对应关系

本文档记录了MTGTop8网站的实际URL参数，确保format code的正确性。

## 实际参数验证

通过访问 https://mtgtop8.com/ 并分析HTML，获取到以下实际使用的format参数：

```
f=ALCH
f=BL
f=CHL
f=EDH
f=EX
f=EXP
f=HI
f=HIGH
f=LE
f=MO
f=PAU
f=PEA
f=PI
f=PREM
f=ST
f=VI
f=cEDH
```

## Format Name → Code 映射表

| Format Name | Format Code | URL示例 | 说明 |
|-------------|-------------|---------|------|
| Modern | MO | f=MO | 现代赛制 |
| Standard | ST | f=ST | 标准赛制 |
| Legacy | LE | f=LE | 薪传赛制 |
| Vintage | VI | f=VI | 复古赛制 |
| Pauper | PAU | f=PAU | 穷人赛制 |
| Pioneer | PI | f=PI | 先驱赛制 |
| Historic | HI | f=HI | 历史赛制 |
| Alchemy | ALCH | f=ALCH | 炼金赛制 |
| Premodern | PREM | f=PREM | 前现代赛制 |
| Explorer | EXP | f=EXP | 探索者赛制 |
| Block | BL | f=BL | 赛块赛制 |
| Highlander | HIGH | f=HIGH | 高地人赛制 |
| Peasant | PEA | f=PEA | 农民赛制 |
| cEDH | cEDH | f=cEDH | 竞争EDH |
| EDH | EDH | f=EDH | EDH/指挥官 |
| Limited | format_limited | f=format_limited | 限制赛制 |

## URL示例

- Modern: https://mtgtop8.com/format?f=MO
- Standard: https://mtgtop8.com/format?f=ST
- Legacy: https://mtgtop8.com/format?f=LE
- Premodern: https://mtgtop8.com/format?f=PREM
- Pauper: https://mtgtop8.com/format?f=PAU

## 代码实现

在 `FormatMapper.kt` 中实现：

```kotlin
private val formatMap = mapOf(
    "Modern" to "MO",
    "Standard" to "ST",
    "Legacy" to "LE",
    "Vintage" to "VI",
    "Pauper" to "PAU",
    "Pioneer" to "PI",
    "Historic" to "HI",
    "Alchemy" to "ALCH",
    "Block" to "BL",
    "Explorer" to "EXP",
    "Highlander" to "HIGH",
    "Peasant" to "PEA",
    "Premodern" to "PREM",
    "cEDH" to "cEDH",
    "EDH" to "EDH",
    "Limited" to "format_limited"
)
```

## 重要修复

### 修复前（错误）
```kotlin
"Premodern" to "PM"  // 错误！MTGTop8使用的是PREM
"Pauper" to "PA"     // 错误！MTGTop8使用的是PAU
"Alchemy" to "AL"    // 错误！MTGTop8使用的是ALCH
```

### 修复后（正确）
```kotlin
"Premodern" to "PREM"  // ✅ 正确
"Pauper" to "PAU"      // ✅ 正确
"Alchemy" to "ALCH"    // ✅ 正确
```

## 验证方法

使用curl命令验证MTGTop8的实际参数：

```bash
curl -s "https://mtgtop8.com/" | grep -oP 'f=\w+' | sort -u
```

## 更新日期

2026-01-21 - 通过实际网页验证并更新所有参数
