# MTGO Decklist Manager 开发路线图

## 版本规划总览

从 v4.0.0（在线模式）到 v5.0.0（全功能竞技工具）的完整升级路径。

---

## 📋 阶段一：核心功能补齐（v4.1.0 - v4.3.0）

### v4.1.0 - 导出与搜索（预计 2 周）

**目标：** 让用户能"带走"套牌，快速查找卡牌

#### 功能模块 1.1：套牌导出功能
**优先级：** P0-1 🔥

**技术实现：**
- [ ] 创建 `DecklistExporter.kt` 工具类
- [ ] 支持导出格式：
  - [ ] MTGO `.dek` 格式
  - [ ] Arena `.json` 格式
  - [ ] 纯文本 `.txt` 格式
  - [ ] Moxfield 分享链接生成
- [ ] 在 `DeckDetailActivity` 添加导出按钮
- [ ] 创建导出格式选择对话框
- [ ] 实现分享功能（分享到微信/QQ等）
- [ ] 添加文件保存到本地存储

**UI 改动：**
```
DeckDetailActivity 菜单添加：
- 导出为 MTGO 格式
- 导出为 Arena 格式
- 导出为文本
- 生成 Moxfield 链接
- 复制到剪贴板
```

**涉及文件：**
- `ui/decklist/DeckDetailActivity.kt`
- `utils/DecklistExporter.kt` (新建)
- `data/repository/DecklistRepository.kt`

---

#### 功能模块 1.2：卡牌搜索功能
**优先级：** P0-2 🔥

**技术实现：**
- [ ] 创建 `SearchActivity.kt` 全局搜索界面
- [ ] 在 `MainActivity` 顶部添加搜索栏
- [ ] 实现 `SearchViewModel.kt`
- [ ] 集成 MTGCH API 搜索接口
- [ ] 实现搜索历史记录（Room 新增表）
- [ ] 支持中英文名搜索

**搜索功能：**
- [ ] 基础搜索：输入卡牌名
- [ ] 模糊搜索：支持部分匹配
- [ ] 高级筛选对话框：
  - [ ] 颜色筛选（WUBRG 多选）
  - [ ] 法术力值范围（0-16+）
  - [ ] 卡牌类型（生物、结界、法术等）
  - [ ] 稀有度（普通、非普、稀有、秘稀）
  - [ ] 赛制合法性

**UI 改动：**
```
MainActivity 顶部添加：
- SearchView（可展开/收起）
- 搜索结果卡片网格
- 筛选按钮
```

**涉及文件：**
- `ui/search/SearchActivity.kt` (新建)
- `ui/search/SearchViewModel.kt` (新建)
- `ui/search/SearchAdapter.kt` (新建)
- `ui/search/FilterDialog.kt` (新建)
- `data/local/entity/SearchHistoryEntity.kt` (新建)
- `data/local/dao/SearchHistoryDao.kt` (新建)
- `ui/decklist/MainActivity.kt` (修改)

---

### v4.2.0 - 套牌分析（预计 3 周）

**目标：** 提供专业的套牌分析工具

#### 功能模块 2.1：法术力曲线
**优先级：** P0-3 🔥

**技术实现：**
- [ ] 创建 `ManaCurveAnalyzer.kt`
- [ ] 使用 **MPAndroidChart** 库绘制图表
- [ ] 添加依赖：`implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'`

**分析内容：**
- [ ] 法术力值分布（0-16+）
- [ ] 颜色符号需求统计
- [ ] 横轴：CMC，纵轴：卡牌数量

**UI 位置：**
```
DeckDetailActivity 添加 "法术力曲线" 标签页
- 柱状图显示
- 支持点击查看某CMC的所有卡牌
```

---

#### 功能模块 2.2：色曲线统计
**优先级：** P0-3 🔥

**技术实现：**
- [ ] 创建 `ColorDistributionAnalyzer.kt`
- [ ] 分析法术力符号来源（卡牌费用 + 其他能力）

**统计内容：**
- [ ] 各颜色法术力需求数量
- [ ] 无色法术力需求数量
- [ ] 饼图展示

---

#### 功能模块 2.3：单卡统计
**优先级：** P0-3 🔥

**技术实现：**
- [ ] 创建 `CardStatisticsAnalyzer.kt`
- [ ] 跨套牌统计（需要新建统计表）
- [ ] 显示卡牌出现频率

**统计维度：**
- [ ] 当前套牌内单卡数量排名
- [ ] 同赛制下最近30天卡牌热度
- [ ] 收藏的套牌中单卡出现频率

---

#### 功能模块 2.4：价格估算
**优先级：** P0-3 🔥

**技术实现：**
- [ ] 接入 **CardMarket API** 或 **TCGplayer API**
- [ ] 创建 `PriceProvider.kt` 接口
- [ ] 实现缓存机制（避免频繁请求）

**API 选择：**
```kotlin
// 推荐使用 CardMarket (MKM) API
// 1. 注册 MKM API Token
// 2. 创建 `CardMarketApi.kt`
// 3. 实现价格查询和缓存
```

**显示内容：**
- [ ] 总套牌价格
- [ ] 单卡价格
- [ ] 最贵的10张卡
- [ ] 货币选择（CNY/USD/EUR）

**涉及文件：**
- `data/remote/api/CardMarketApi.kt` (新建)
- `domain/model/PriceInfo.kt` (新建)
- `ui/decklist/PriceFragment.kt` (新建)

---

### v4.3.0 - 元游戏分析（预计 3 周）

**目标：** 让用户了解环境趋势

#### 功能模块 3.1：Meta 统计
**优先级：** P1-4 🚀

**技术实现：**
- [ ] 创建 `MetaStatisticsRepository.kt`
- [ ] 新建数据库表：
  - `meta_stats`：存储每日统计数据
  - `deck_archetype`：套牌 archetype 分类
- [ ] 后台任务：每日自动统计

**统计内容：**
- [ ] 最受欢迎套牌 Top 20
- [ ] 最受欢迎单卡 Top 50
- [ ] 套牌 archetype 占比饼图
- [ ] 时间范围：近7天/30天/90天

**UI 位置：**
```
MainActivity 添加 "Meta分析" 底部标签页
- 套牌排行榜
- 单卡排行榜
- 赛制选择器
```

**涉及文件：**
- `data/local/entity/MetaStatsEntity.kt` (新建)
- `data/repository/MetaStatisticsRepository.kt` (新建)
- `ui/meta/MetaAnalysisActivity.kt` (新建)
- `ui/meta/TopDecksFragment.kt` (新建)
- `ui/meta/TopCardsFragment.kt` (新建)

---

## 📱 阶段二：体验优化（穿插在 v4.1.0 - v4.3.0）

### UI/UX 优化任务（每个版本都穿插）

#### 优化点 1：深色模式
**优先级：** P2-1 🎨

**技术实现：**
- [ ] 创建 `night/themes.xml`
- [ ] 所有颜色资源定义 `values-night/colors.xml`
- [ ] 设置中添加深色模式开关
- [ ] 跟随系统主题

**时间安排：** v4.1.0 开发期间完成

---

#### 优化点 2：手势操作增强
**优先级：** 体验优化

**实现内容：**
- [ ] 左滑收藏套牌（MainActivity）
- [ ] 右滑删除套牌（已有，优化动画）
- [ ] 长按卡片快速预览
- [ ] 双击放大卡牌图片

**时间安排：** v4.1.5 小版本更新

---

#### 优化点 3：快捷方式和小部件
**优先级：** 体验优化

**实现内容：**
- [ ] 桌面快捷方式（搜索卡牌）
- [ ] 桌面小部件显示"今日热门套牌"
- [ ] 通知栏快捷搜索

**时间安排：** v4.2.5 小版本更新

---

## 🎯 阶段三：增强功能（v4.4.0 - v4.5.0）

### v4.4.0 - 笔记与对比（预计 2 周）

#### 功能模块 4.1：笔记功能
**优先级：** P1-5 🚀

**技术实现：**
- [ ] 新建 `notes` 数据库表
- [ ] 创建 `NoteRepository.kt`
- [ ] 单卡笔记（CardDetailActivity）
- [ ] 套牌笔记（DeckDetailActivity）

**笔记功能：**
- [ ] 富文本编辑（支持 Markdown）
- [ ] 笔记分类标签
- [ ] 笔记搜索
- [ ] 导出笔记

**涉及文件：**
- `data/local/entity/NoteEntity.kt` (新建)
- `data/local/dao/NoteDao.kt` (新建)
- `ui/notes/NoteActivity.kt` (新建)

---

#### 功能模块 4.2：套牌对比
**优先级：** P1-6 🚀

**技术实现：**
- [ ] 长按两副牌进入对比模式
- [ ] 创建 `DeckComparisonActivity.kt`
- [ ] 并排显示两副套牌

**对比内容：**
- [ ] 主牌差异高亮
- [ ] 备牌差异高亮
- [ ] 数量不同的卡牌标记
- [ ] 独有卡牌标记

**UI 设计：**
```
左右分栏布局：
- 左侧：套牌A
- 右侧：套牌B
- 绿色背景：独有卡
- 红色背景：数量不同
- 黄色高亮：差异部分
```

---

### v4.5.0 - 离线与多数据源（预计 2 周）

#### 功能模块 5.1：离线模式
**优先级：** P1-7 🚀

**技术实现：**
- [ ] 实现智能缓存策略
- [ ] 缓存最近查看的100个套牌
- [ ] 缓存最近查询的200张卡
- [ ] 提供"下载全部"按钮（可选）

**缓存策略：**
```kotlin
// LRU 缓存策略
- 最近查看的套牌（自动缓存）
- 收藏的套牌（优先缓存）
- 最近搜索的卡牌（自动缓存）
- 用户手动下载（全量缓存）
```

**涉及文件：**
- `data/cache/DeckCacheManager.kt` (新建)
- `data/cache/CardCacheManager.kt` (新建)

---

#### 功能模块 5.2：多数据源支持
**优先级：** P2-10 🎨

**新增数据源：**
- [ ] MTGMelee（补充赛事数据）
- [ ] EDHREC（指挥官专区）
- [ ] Goldfish（Meta分析）

**架构设计：**
```kotlin
interface DataSource {
    suspend fun getEvents(): List<Event>
    suspend fun getDecklists(eventId: String): List<Decklist>
}

class MtgTop8Source : DataSource { }
class MtgMeleeSource : DataSource { }
class EdhrecSource : DataSource { }

class DataSourceManager {
    private val sources = listOf(
        MtgTop8Source(),
        MtgMeleeSource()
    )

    suspend fun getAllEvents(): List<Event> {
        return sources.flatMap { it.getEvents() }
    }
}
```

---

## 🎮 阶段四：比赛工具（v4.6.0）

### v4.6.0 - 比赛辅助工具（预计 2 周）

#### 功能模块 6：生命计数器和换备工具
**优先级：** P3-13 🎮

**子功能 6.1：生命计数器**
**实现内容：**
- [ ] 2人/4人模式
- [ ] 生命值加减
- [ ] 毒指示物
- [ ] 能量指示物
- [ ] 历史记录（可回退）

**子功能 6.2：换备提醒器**
**实现内容：**
- [ ] 记录 sideboard 换入换出
- [ ] 针对不同 archetype 的换备指南
- [ ] 快速勾选卡牌
- [ ] 导出换备列表

**涉及文件：**
- `ui/tools/LifeCounterActivity.kt` (新建)
- `ui/tools/SideboardHelperActivity.kt` (新建)

---

## 🌐 阶段五：社交功能（v4.7.0）

### v4.7.0 - 社交与分享（预计 2 周）

#### 功能模块 7.1：分享功能
**优先级：** P2-8 🎨

**实现内容：**
- [ ] 生成套牌分享海报（带二维码）
- [ ] 分享到微信/朋友圈/QQ
- [ ] 分享到微博
- [ ] 复制分享链接

**海报设计：**
```
┌─────────────────────┐
│  [套牌名称]          │
│  [赛制]              │
│  [关键卡牌缩略图x3]   │
│  [胜率/成绩]         │
│  [二维码]            │
│  扫码查看完整卡表     │
└─────────────────────┘
```

**技术实现：**
- 使用 `Canvas` 绘制海报
- 生成二维码：`implementation 'com.google.zxing:core:3.5.1'`

---

#### 功能模块 7.2：笔记社区（可选）
**优先级：** P2-9

**实现内容：**
- [ ] 发布套牌笔记
- [ ] 查看他人笔记
- [ ] 点赞和评论
- [ ] 关注牌手

**注意：** 此功能需要后端服务器，建议暂缓或使用第三方平台（如 GitHub Issues）

---

## 🔄 阶段六：赛制扩充（v4.8.0）

### v4.8.0 - 指挥官与限制赛（预计 2 周）

#### 功能模块 8：新赛制支持
**优先级：** P2-11

**新增赛制：**
- [ ] 指挥官（Commander / EDH）
- [ ] 双头巨人（2HG）
- [ ** 限制轮抽（Sealed / Draft）

**特殊适配：**
- [ ] 指挥官：
  - [ ] 显示99张主牌 + 1张指挥官
  - [ ] 颜色身份显示
  - [ ] 基本地自动补全建议
- [ ] 限制赛：
  - [ ] 牌池评分工具
  - [ ] 卡牌pick建议

---

## 🤖 阶段七：AI 助手（v5.0.0）

### v5.0.0 - AI 驱动的智能建议（预计 4 周）

#### 功能模块 9：AI 分析助手
**优先级：** P3-12 🤖

**实现方案：**
- [ ] 集成 OpenAI API / Claude API
- [ ] 创建 `AIAdvisorService.kt`

**AI 功能：**
- [ ] 套牌弱点分析
- [ ] 改进建议（针对当前meta）
- [ ] Sideboard guide 自动生成
- [ ] 卡牌替换建议
- [ ] 自然语言查询："帮我找能对抗Inquisition的牌"

**API 集成：**
```kotlin
interface AIAdvisorService {
    suspend fun analyzeDeck(decklist: Decklist): DeckAnalysis
    suspend fun suggestSideboard(
        decklist: Decklist,
        opponentArchetype: String
    ): SideboardSuggestion
}

data class DeckAnalysis(
    val weaknesses: List<String>,
    val suggestions: List<CardSuggestion>,
    val powerLevel: Float,
    val metaMatchup: Map<String, Float>
)
```

**注意：**
- 需要用户自己提供 API Key
- 成本考虑：按量计费
- 隐私：套牌数据不会上传（仅hash）

---

## 📊 开发时间表

### 第 1-2 周：v4.1.0
- **周 1：** 套牌导出功能
  - 实现 MTGO/Arena/文本导出
  - 分享功能

- **周 2：** 卡牌搜索功能
  - 搜索界面
  - 基础搜索
  - 深色模式（穿插）

### 第 3-4 周：v4.1.5（体验优化）
- 手势操作增强
- 搜索性能优化
- Bug 修复

### 第 5-7 周：v4.2.0
- **周 5：** 法术力曲线 + 色曲线
- **周 6：** 单卡统计 + 价格估算
- **周 7：** 图表优化 + 快捷方式

### 第 8-9 周：v4.2.5（体验优化）
- 桌面小部件
- 缓存优化
- 动画优化

### 第 10-12 周：v4.3.0
- Meta 统计功能
- 数据分析后台任务
- 图表可视化

### 第 13-14 周：v4.4.0
- 笔记功能
- 套牌对比

### 第 15-16 周：v4.5.0
- 离线模式
- 多数据源支持

### 第 17-18 周：v4.6.0
- 生命计数器
- 换备工具

### 第 19-20 周：v4.7.0
- 分享海报
- 社交功能

### 第 21-22 周：v4.8.0
- 指挥官支持
- 限制赛支持

### 第 23-26 周：v5.0.0
- AI 助手
- 智能推荐
- 最终打磨

---

## 🛠️ 技术债务清理

每个版本都需要穿插的技术任务：

- [ ] 单元测试覆盖（目标 60%）
- [ ] UI 自动化测试
- [ ] 内存泄漏检查（LeakCanary）
- [ ] 性能优化（Profiler）
- [ ] 代码审查和重构
- [ ] 文档更新

---

## 📦 依赖项更新

需要在各阶段添加的依赖：

```gradle
// 图表库（v4.2.0）
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'

// 二维码（v4.7.0）
implementation 'com.google.zxing:core:3.5.1'
implementation 'com.journeyapps:zxing-android-embedded:4.3.0'

// AI API（v5.0.0）
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.aallam.openai:openai-client:3.0.0'

// 工作管理器（已有，增强）
implementation "androidx.work:work-runtime-ktx:2.9.0"

// 数据存储（已有，增强）
implementation "androidx.datastore:datastore-preferences:1.0.0"
```

---

## 🎯 里程碑

- **v4.1.0** - 核心功能补齐（导出 + 搜索）
- **v4.2.0** - 专业分析工具（曲线 + 价格）
- **v4.3.0** - Meta 分析平台
- **v4.5.0** - 离线可用
- **v4.7.0** - 社交分享
- **v5.0.0** - AI 智能助手

---

## 📝 备注

1. **优先级说明：**
   - 🔥 P0：核心功能，必须实现
   - 🚀 P1：增强体验，应该实现
   - 🎨 P2：锦上添花，可以实现
   - 🎮 P3：长期愿景，未来考虑

2. **时间安排灵活：**
   - 每个版本可根据实际情况调整
   - 穿插的优化任务可灵活调配
   - 技术债务清理持续进行

3. **用户反馈优先：**
   - 根据实际用户反馈调整优先级
   - 高频使用的功能优先完善

4. **性能监控：**
   - 使用 Firebase Performance 监控
   - Crashlytics 收集崩溃信息

---

**最后更新：** 2026-01-31
**当前版本：** v4.0.0 (online)
**目标版本：** v5.0.0 (AI-powered)
