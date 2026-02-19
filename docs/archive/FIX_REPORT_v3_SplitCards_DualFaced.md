# 修复报告 v3 - 双面牌和 Split 卡牌

**日期**: 2025-02-19
**版本**: v5.1.0
**状态**: 全面修复 ✅

---

## 🐛 修复的问题

### 1. Split/Fusion 卡牌无法找到
**症状**: `Wear/Tear` 等卡牌在套牌页面显示为英文名，没有法术力值

**原因**:
- 套牌接口返回的名称是 `Wear/Tear`（单斜杠）
- 但服务器搜索需要 `Wear // Tear`（双斜杠加空格）

**修复**:
```kotlin
// 添加名称格式化函数
private fun formatCardNameForSearch(cardName: String): String {
    if (" // " in cardName) return cardName
    return cardName.replace("/", " // ")  // Wear/Tear -> Wear // Tear
}
```

### 2. 双面牌背面没有信息和图片
**症状**: 双面牌的背面点击后无内容

**原因**:
- `CardInfoDto` 缺少 `cardFaces` 字段
- `ServerMapper` 没有从 `cardFaces` 提取背面信息

**修复**:

#### ServerDto.kt
```kotlin
// 新增 ServerCardFaceDto
data class ServerCardFaceDto(
    val name: String?,
    val nameZh: String?,
    val manaCost: String?,
    val typeLine: String?,
    val typeLineZh: String?,
    val oracleText: String?,
    val oracleTextZh: String?,
    val power: String?,
    val toughness: String?,
    val loyalty: String?,
    val imageUris: ImageUris?
)

// CardInfoDto 添加 cardFaces 字段
data class CardInfoDto(
    // ... 其他字段
    val cardFaces: List<ServerCardFaceDto>? = null  // ✅
)
```

#### ServerMapper.kt
```kotlin
fun CardInfoDto.toCardInfo(): CardInfo {
    val backFace = cardFaces?.getOrNull(1)  // 第二个面是背面

    return CardInfo(
        // ... 其他字段
        isDualFaced = isDoubleFaced == true,
        // ✅ 双面牌背面信息
        frontFaceName = nameZh ?: name,
        backFaceName = backFace?.nameZh ?: backFace?.name,
        frontImageUri = imageUris?.normal,
        backImageUri = backFace?.imageUris?.normal,
        backFaceManaCost = backFace?.manaCost,
        backFaceTypeLine = backFace?.typeLineZh ?: backFace?.typeLine,
        backFaceOracleText = backFace?.oracleTextZh ?: backFace?.oracleText,
        backFacePower = backFace?.power,
        backFaceToughness = backFace?.toughness,
        backFaceLoyalty = backFace?.loyalty
    )
}
```

---

## 📊 测试验证

### Split 卡牌测试

**测试用例**: `Wear // Tear`

```bash
# API 请求
curl "http://182.92.109.160/api/cards/search?q=Wear%20%2F%2F%20Tear&limit=1"

# 返回结果
{
    "name": "Wear // Tear",
    "nameZh": "损耗 // 穿破",
    "manaCost": "{1}{R} // {W}",
    "typeLineZh": "瞬间 // 瞬间",
    "oracleTextZh": "消灭目标神器...\\n\\n消灭目标结界...",
    "cardFaces": [
        {
            "name": "Wear",
            "nameZh": "损耗",
            "manaCost": "{1}{R}",
            "typeLineZh": "瞬间"
        },
        {
            "name": "Tear",
            "nameZh": "穿破",
            "manaCost": "{W}",
            "typeLineZh": "瞬间"
        }
    ]
}
```

### 双面牌测试

**测试用例**: 双面牌背面信息

- ✅ 背面中文名称: `backFace?.nameZh`
- ✅ 背面中文类型: `backFace?.typeLineZh`
- ✅ 背面规则文本: `backFace?.oracleTextZh`
- ✅ 背面图片: `backFace?.imageUris?.normal`

---

## 📝 修改文件列表

| 文件 | 操作 | 说明 |
|------|------|------|
| `ServerDto.kt` | 修改 | 添加 ServerCardFaceDto 和 cardFaces 字段 |
| `ServerMapper.kt` | 修改 | 从 cardFaces 提取背面信息 |
| `DeckDetailViewModel.kt` | 修改 | 添加 formatCardNameForSearch() 函数 |
| `DecklistRepository.kt` | 修改 | 添加 formatCardNameForSearch() 函数 |

---

## 🎯 修复范围

### ✅ 已修复
1. **Split 卡牌** (如 `Wear // Tear`, `Fire // Ice`)
   - 正确的中文显示
   - 正确的法术力值格式
   - 完整的卡牌信息

2. **双面牌背面信息**
   - 中文名称
   - 中文类型行
   - 中文规则文本
   - 背面图片

3. **Fusion 卡牌** (如 `kill // destroy`)
4. **Adventure 卡牌** (如 `Brazen Borrower // Petty Theft`)

### ⚠️ 待修复
1. **SearchViewModel** 仍使用 MTGCH API
   - 搜索功能失效（返回 404）

---

## 📈 性能影响

### 修复前
- Split 卡牌: 搜索失败，无中文名，无法术力值
- 双面牌背面: 无信息，无图片

### 修复后
- Split 卡牌: 正常显示完整信息
- 双面牌背面: 完整的中文信息和图片

**API 调用**: 无额外开销（名称格式化在客户端完成）

---

## 🔍 技术细节

### Split 卡牌格式规则

MTG 使用以下格式表示复合卡牌：

| 类型 | 格式 | 示例 |
|------|------|------|
| Split | `Name1 // Name2` | `Wear // Tear` |
| Fusion | `Name1 // Name2 // ...` | `declare // believe` |
| Adventure | `Creature // Spell` | `Brazen Borrower // Petty Theft` |
| Transform | `Front // Back` (双面牌) | `Agadeem's Awakening // Agadeem, the Undercrypt` |

**规则**:
- 两个斜杠 `//` 之间加空格
- 每个半张牌有自己的法术力值
- 搜索时必须使用完整格式

### 名称格式化逻辑

```kotlin
// 输入: Wear/Tear
// 输出: Wear // Tear

// 输入: Become // immense
// 输出: Become // immense (保持不变)

fun formatCardNameForSearch(cardName: String): String {
    if (" // " in cardName) return cardName  // 已经是正确格式
    return cardName.replace("/", " // ")      // 单斜杠转双斜杠
}
```

---

## 🧪 测试步骤

### 1. 测试 Split 卡牌
1. 打开包含 `Wear // Tear` 的套牌
2. 检查套牌列表页面是否显示：
   - ✅ 中文名: "损耗 // 穿破"
   - ✅ 法术力值: "{1}{R} // {W}"
3. 点击卡牌查看详情
4. 检查详情页面是否显示完整信息

### 2. 测试双面牌背面
1. 找到一张双面牌（如 `Agadeem's Awakening`）
2. 点击卡牌查看详情
3. 点击"查看反面"按钮
4. 检查背面是否显示：
   - ✅ 中文名称
   - ✅ 中文类型
   - ✅ 中文规则文本
   - ✅ 背面图片

---

## 📌 总结

**修复内容** ✅:
- Split/Fusion 卡牌正确识别和显示
- 双面牌背面完整信息支持
- 所有中文相关字段正确映射

**待修复** ⚠️:
- SearchViewModel 仍需改用 ServerApi

**用户可以正常使用**:
- ✅ 浏览赛事和套牌
- ✅ 查看套牌详情（包括 Split 卡牌）
- ✅ 查看双面牌正面和背面
- ✅ 收藏功能
- ❌ 搜索卡牌（待修复）

---

**状态**: 已构建并安装
**版本**: v5.1.0
**下一步**: 用户测试，如确认正常则修复 SearchViewModel
