# MTGO Decklist Manager v4.2.0 开发计划

## 📋 版本信息
- **版本号：** 4.2.0
- **版本代码：** 78
- **开发分支：** dev/v4.2.0
- **基于版本：** v4.1.0
- **开始日期：** 2026-02-05
- **预计完成：** 2026-02-XX

---

## 🎯 核心功能：套牌分析

### 功能概述
为用户提供套牌统计分析功能，帮助玩家更好地理解自己的套牌构成。

### 主要功能模块

#### 1. 法术力曲线分析
- **功能描述：** 显示套牌中各法术力值的卡牌数量分布
- **显示方式：** 柱状图或折线图
- **数据维度：**
  - 0-6+ 法术力值
  - 主牌和备牌分别统计
  - 可按颜色筛选

#### 2. 颜色分布分析
- **功能描述：** 显示套牌中各颜色卡牌的数量和占比
- **显示方式：** 饼图或环形图
- **数据维度：**
  - 五色（白蓝黑红绿）
  - 无色
  - 多色卡牌的处理

#### 3. 类型分布分析
- **功能描述：** 显示套牌中各类型卡牌的数量
- **显示方式：** 横向柱状图
- **数据维度：**
  - 生物
  - 法术
  - 陷阱
  - 结界
  - 武具/神器
  - 旅法师
  - 地陆

#### 4. 套牌统计摘要
- **功能描述：** 显示套牌的基本统计信息
- **显示内容：**
  - 总卡牌数量（主牌/备牌）
  - 法术力值平均数
  - 地陆数量
  - 非陆地数量
  - 稀有度分布

---

## 🏗️ 技术设计

### 数据模型

```kotlin
/**
 * 套牌分析数据模型
 */
data class DeckAnalysis(
    val decklistId: Long,
    val decklistName: String,
    val manaCurve: ManaCurve,
    val colorDistribution: ColorDistribution,
    val typeDistribution: TypeDistribution,
    val statistics: DeckStatistics
)

data class ManaCurve(
    val curve: Map<Int, Int>,  // 法术力值 -> 数量
    val averageManaValue: Double
)

data class ColorDistribution(
    val colors: Map<ManaColor, Int>,  // 颜色 -> 数量
    val totalCards: Int
)

data class TypeDistribution(
    val types: Map<CardType, Int>,  // 类型 -> 数量
    val totalCards: Int
)

data class DeckStatistics(
    val mainDeckCount: Int,
    val sideboardCount: Int,
    val landCount: Int,
    val nonLandCount: Int,
    val averageManaValue: Double,
    val rarityDistribution: Map<Rarity, Int>
)

enum class ManaColor {
    WHITE, BLUE, BLACK, RED, GREEN, COLORLESS
}

enum class CardType {
    CREATURE, INSTANT, SORCERY, ENCHANTMENT, ARTIFACT,
    PLANESWALKER, LAND, OTHER
}

enum class Rarity {
    COMMON, UNCOMMON, RARE, MYTHIC, SPECIAL
}
```

### 分析逻辑

```kotlin
/**
 * 套牌分析器
 */
class DeckAnalyzer @Inject constructor(
    private val cardDao: CardDao
) {
    suspend fun analyze(decklistId: Long): DeckAnalysis {
        val cards = cardDao.getCardsByDecklistId(decklistId)
        val mainDeck = cards.filter { it.location == "main" }
        val sideboard = cards.filter { it.location == "sideboard" }

        return DeckAnalysis(
            decklistId = decklistId,
            decklistName = "", // 从 Decklist 获取
            manaCurve = calculateManaCurve(mainDeck),
            colorDistribution = calculateColorDistribution(mainDeck),
            typeDistribution = calculateTypeDistribution(mainDeck),
            statistics = calculateStatistics(mainDeck, sideboard)
        )
    }

    private fun calculateManaCurve(cards: List<CardEntity>): ManaCurve {
        // 实现法术力曲线计算
    }

    private fun calculateColorDistribution(cards: List<CardEntity>): ColorDistribution {
        // 实现颜色分布计算
    }

    private fun calculateTypeDistribution(cards: List<CardEntity>): TypeDistribution {
        // 实现类型分布计算
    }

    private fun calculateStatistics(
        mainDeck: List<CardEntity>,
        sideboard: List<CardEntity>
    ): DeckStatistics {
        // 实现统计计算
    }
}
```

### UI 设计

#### 新页面：DeckAnalysisActivity
- **入口：** 套牌详情页面的浮动按钮或菜单项
- **布局：**
  - 顶部：套牌名称和统计摘要
  - 中部：Tab 切换（法术力曲线/颜色分布/类型分布）
  - 底部：图表显示区域

#### 图表库选择
- **选项 1：** MPAndroidChart（推荐）
  - 功能强大
  - 文档完善
  - 社区活跃
- **选项 2：** AnyChart
- **选项 3：** 自定义绘制

### 依赖添加

```gradle
// MPAndroidChart
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

---

## 📅 开发计划

### 阶段 1：数据模型和分析器（1-2天）
- [ ] 创建数据模型
- [ ] 实现 DeckAnalyzer 核心逻辑
- [ ] 编写单元测试

### 阶段 2：UI 基础（1天）
- [ ] 创建 DeckAnalysisActivity
- [ ] 创建布局文件
- [ ] 添加导航入口

### 阶段 3：图表显示（1-2天）
- [ ] 集成 MPAndroidChart
- [ ] 实现法术力曲线图表
- [ ] 实现颜色分布图表
- [ ] 实现类型分布图表

### 阶段 4：优化和测试（1天）
- [ ] UI 调整和美化
- [ ] 性能优化
- [ ] 测试各种套牌类型

### 阶段 5：发布准备
- [ ] 更新版本号
- [ ] 编写发布说明
- [ ] 构建Release APK

---

## 📝 待办事项

### 高优先级
- [x] 创建开发分支
- [x] 编写开发计划
- [ ] 实现数据模型
- [ ] 实现 DeckAnalyzer

### 中优先级
- [ ] 添加图表库依赖
- [ ] 创建分析页面 UI
- [ ] 实现图表显示

### 低优先级
- [ ] 添加动画效果
- [ ] 导出分析报告
- [ ] 价格估算（v4.3.0）

---

## 🎨 UI 参考图

```
┌─────────────────────────────────────┐
│  ← 套牌分析              [分享]     │
├─────────────────────────────────────┤
│                                     │
│  Modern Event - MTGO League        │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ 📊 统计摘要                   │  │
│  │ 主牌: 60  备牌: 15            │  │
│  │ 平均法术力: 2.3               │  │
│  │ 地陆: 24  非地: 36            │  │
│  └───────────────────────────────┘  │
│                                     │
│  [法术力曲线] [颜色] [类型]        │
│                                     │
│  ┌───────────────────────────────┐  │
│  │                               │  │
│  │   ▓                           │  │
│  │   ▓   ▓                       │  │
│  │   ▓   ▓   ▓   ▓               │  │
│  │   ▓   ▓   ▓   ▓   ▓           │  │
│  │   0   1   2   3   4   5+      │  │
│  │                               │  │
│  └───────────────────────────────┘  │
│                                     │
└─────────────────────────────────────┘
```

---

## 🔗 相关链接

### 图表库文档
- [MPAndroidChart GitHub](https://github.com/PhilJay/MPAndroidChart)
- [MPAndroidChart Wiki](https://github.com/PhilJay/MPAndroidChart/wiki)

### 参考项目
- MTGMelee (Web)
- Archidekt (Web)
- Moxfield (Web)

---

**最后更新：** 2026-02-05
**状态：** 开发中 🚧
