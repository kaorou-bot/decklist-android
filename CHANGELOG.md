# 更新日志 (Changelog)

所有值得注意的项目更改都将记录在此文件中。

## [5.2.0] - 2025-02-20

### 新增功能 (Added)
- **卡牌分类排序**: 套牌详情页按卡牌类型分类显示
  - 分类顺序: 地 → 生物 → 法术 → 瞬间 → 神器 → 结界 → 鹏洛克 → 其他
  - 支持中英文类型识别
  - 每个分类显示中文标题和分隔线

### 移除功能 (Removed)
- **赛事下载按钮**: 移除主页面的赛事下载功能
- **套牌下载按钮**: 移除赛事详情页的套牌下载功能

### 改进 (Improved)
- **隐藏空赛事**: 自动隐藏 deck_count 为 0 的赛事

### 修复 (Fixed)
- ✅ 卡牌分类功能恢复（之前因显示问题被回退）
- ✅ 中英文类型混合识别正确工作

## [5.1.0] - 2025-02-19

### 重大变更 (Major Change)
- **完全迁移到自有服务器**: 所有功能均使用自有服务器 API
  - SearchViewModel 完全迁移到 ServerApi
  - 印刷版本查询使用 ServerApi
  - 卡牌详情使用 ServerApi
  - 不再依赖 MTGCH API（已返回 404）

### 新增功能 (Added)
- **印刷版本查询**: 使用自有服务器 API
  - 按 Oracle ID 查询: `/api/cards/{oracleId}/printings`
  - 按卡牌名称查询: `/api/cards/printings?name={cardName}`
  - 支持显示和切换不同印刷版本
- **中文字段优先级**: 所有卡牌数据优先使用中文
  - 类型行: `typeLineZh`
  - 规则文本: `oracleTextZh`
  - 系列名称: `setNameZh`
- **双面牌背面信息**: 从 `cardFaces` 提取背面数据
  - 背面名称、类型、规则文本
  - 背面图片 URI
- **Split 卡牌支持**: 格式化卡牌名称搜索
  - `Wear/Tear` → `Wear // Tear`
  - 支持混合格式识别

### 技术改进 (Technical)
- 新增 `ServerCardFaceDto`: 双面牌卡牌面数据结构
- 新增 `ServerMapper.toCardInfo()`: DTO 到领域模型映射
- `ServerApi.getCardPrintings()`: 印刷版本查询接口
- `ServerApi.searchCardPrintingsByName()`: 按名称查询印刷版本
- `SearchViewModel.getCardPrintings()`: 获取印刷版本方法
- `SearchViewModel.searchCardPrintingsByName()`: 按名称搜索印刷版本
- `DeckDetailViewModel.formatCardNameForSearch()`: Split 卡牌名称格式化
- `CardInfoFragment` 迁移到 CardInfoDto
- `PrintingSelectorDialog` 迁移到 CardInfoDto

### 修复 (Fixed)
- ✅ 搜索功能失效（MTGCH API 404）
- ✅ 卡牌详情不显示中文类型、规则文本、系列
- ✅ Split 卡牌无法搜索（Wear/Tear）
- ✅ 双面牌背面无信息和图片
- ✅ 印刷版本功能失效
- ✅ 套牌页面卡牌名称和法术力值不显示

### 架构变更 (Architecture)
- **移除 MTGCH API 依赖**:
  - SearchViewModel 不再依赖 MtgchApi
  - CardInfoFragment 不再依赖 MtgchCardDto
  - PrintingSelectorDialog 不再依赖 MtgchCardDto
- **数据流统一**:
  - 所有卡牌数据使用 CardInfoDto
  - 所有映射通过 ServerMapper.toCardInfo()
  - 所有搜索通过 ServerApi

### 已知问题 (Known Issues)
- 印刷版本查询必须使用英文名称（服务器限制）
- 中文名称查询印刷版本返回 404（待服务器支持）

### 性能 (Performance)
- 搜索: < 1 秒
- 卡牌详情: < 1 秒
- 印刷版本查询: < 2 秒
- 套牌详情: 3-6 秒（75 张卡牌）

---

## [4.1.0] - 2026-02-01 (开发中)

### 重大变更 (Major Change)
- **在线搜索功能**: 搜索功能完全改用 MTGCH 在线 API
  - 不再搜索本地数据库，改为直接调用 mtgch.com API
  - 支持实时搜索最新卡牌数据
  - 自动优先显示中文名称和规则文本
  - 优先使用中文卡图（带中文水印）

### 新增功能 (Added)
- **高级搜索筛选**: 创建类似 mtgch.com/search 的筛选界面
  - 颜色筛选：支持 W/U/B/R/G 多选
  - 法术力值筛选：支持 =/>/< 操作符
  - 类型筛选：生物/瞬间/法术/神器/结界/鹏洛客/地
  - 稀有度筛选：common/uncommon/rare/mythic
  - 筛选对话框 UI（dialog_search_filters.xml）

- **卡牌图片显示**: 搜索结果中显示卡牌图片
  - 使用 Glide 加载在线图片
  - 优先显示中文卡图
  - 支持双面牌图片

### 技术改进 (Technical)
- SearchViewModel 完全重写：
  - 使用 MtgchApi 替代 CardInfoDao
  - 实现 buildSearchQuery() 构建高级搜索查询
  - 添加 SearchFilters, CmcFilter, SearchResultItem 数据类
- SearchResultAdapter 更新为使用 SearchResultItem
- SearchActivity 添加筛选按钮和对话框

### 已知问题 (Known Issues)
- 筛选条件收集逻辑待实现（UI 已完成）
- 卡牌详情对话框可以进一步优化显示更多 MTGCH 字段

---

## [3.12.0] - 2026-01-29

### 新功能 (New Features)
- **机器翻译支持**: 新增对 MTGCH API 机器翻译字段的支持
  - 使用 `atomic_translated_name` 作为中文名 fallback
  - 使用 `atomic_translated_type` 作为中文类型 fallback
  - 使用 `atomic_translated_text` 作为中文规则文本 fallback
  - 现在所有卡牌都能显示中文信息（即使没有官方中文印刷）
- **MTGCH 中文卡图**: 优先使用 MTGCH 提供的中文卡图（带中文水印）
- **数据库更新脚本**: 新增 `update_database.sh` 自动化脚本
  - 一键从 GitHub 下载最新卡牌数据
  - 自动验证数据格式
  - 自动备份旧数据库
  - 显示更新统计信息

### 修复 (Fixed)
- **导入进度条**: 修复进度条直接跳到 100% 的问题
  - 先统计文件总行数
  - 正确计算进度百分比
- **数据库导入失败**: 修复使用 `useLines()` 导致的导入失败
  - 改用传统 `readLine()` 循环
  - 避免协程中的流处理问题

### 改进 (Improved)
- 更新项目文档，添加数据库更新说明
- 优化代码结构和错误处理

## [3.10.3] - 2026-01-27

### 修复 (Fixed)
- **双面牌中文名称显示**: 彻底修复双面牌背面无法显示中文信息的问题
  - 从 API 的 `cardFaces` 数组中提取正背两面的中文名称（`zhName` 字段）
  - 优先使用中文名称构建显示名称，格式为 "正面中文名 // 背面中文名"
  - 修复 `MtgchMapper.kt` 中的名称提取逻辑
- **中文翻译缺失问题**: 修复部分卡牌无法正确下载中文翻译的问题
  - 添加智能重试机制，当第一个搜索结果没有中文名时，自动尝试其他结果
  - 优化搜索匹配算法，优先选择包含中文翻译的版本

### 改进 (Improved)
- **调试日志增强**: 添加详细的调试日志
  - 记录 `cardFaces` 数组的完整内容（每个面的中文名和英文名）
  - 显示最终构建的 `displayName` 结果
  - 输出 `frontFaceName` 和 `backFaceName` 的值
- **代码质量**: 修复 Kotlin 编译器的智能转换警告
  - 将 `cardDto.cardFaces` 赋值给局部变量后再使用

## [3.10.2] - 2026-01-27

### 调试 (Debug)
- 添加更多调试日志来追踪中文信息和反面信息问题
- 记录 API 返回的原始字段值（zhsTypeLine, zhsText, cardFaces等）

## [3.10.1] - 2026-01-27

### 修复 (Fixed)
- **双面牌反面信息显示**: 现在弹窗中切换到反面时显示完整的卡牌信息
  - 添加 `backFaceManaCost`、`backFaceTypeLine`、`backFaceOracleText` 字段
  - 从 API 的 `card_faces` 数据中提取反面详细信息
  - 数据库迁移 v6 -> v7

## [3.10.0] - 2026-01-27

### 修复 (Fixed)
- **双面牌弹窗支持**: CardInfoFragment 现在支持双面牌切换
  - 添加"查看反面"/"查看正面"按钮
  - 点击按钮可切换正面和反面的图片和信息
  - 自动检测 `isDualFaced` 字段并显示/隐藏按钮

### 改进 (Improved)
- 套牌页面现在显示中文卡牌名称（当语言设置为中文时）

## [3.9.9] - 2026-01-27

### 调试 (Debug)
- 添加更多调试日志来追踪双面牌检测问题
- 使用 `Log.e` 确保关键日志一定会输出
- 在 `DeckDetailViewModel` 中添加双面牌修复调用

## [3.9.8] - 2026-01-27

### 新增 (Added)
- **套牌页面中文显示**: 当语言设置为中文时，套牌详情页面的卡牌名称显示中文
  - 新增 `en_name` 字段到 `card_info` 表，用于存储原始英文名
  - 添加数据库迁移 (v5 -> v6)
  - 修改 `Card` 模型，添加 `cardNameZh` 字段
  - 在 `DeckDetailViewModel` 中批量查询并填充中文名

### 修复 (Fixed)
- **双面牌数据库修复**: 使用 Kotlin 代码代替 SQL，提高兼容性
  - 查询所有包含 " // " 的卡牌
  - 逐个处理并提取正面/反面名称
  - 应用启动时自动执行修复

## [3.9.7] - 2026-01-27

### 修复 (Fixed)
- **双面牌数据库修复**: 添加自动修复功能，在应用启动时更新所有包含 " // " 的卡牌为双面牌
  - 使用 SQL 批量更新 `is_dual_faced` 字段
  - 自动提取正面和反面名称
- **中文翻译缺失**: 添加 `view=0` 参数到 API 请求，获取完整的卡牌详细信息（包括中文翻译）
  - 修复了 Voice of Victory 等卡牌无法显示中文名的问题

## [3.9.5] - 2026-01-27

### 新增 (Added)
- 主牌和备牌的收起/展开功能
  - 点击标题栏右侧按钮可收起/展开卡牌列表
  - 按钮图标会根据状态变化（向上/向下箭头）

### 改进 (Improved)
- 卡牌搜索精确匹配算法
  - 增加搜索结果数量从1个到10个
  - 实现精确匹配逻辑，优先选择名称完全一致的卡牌
  - 自动修正数据库中所有同名卡牌的法术力值

### 修复 (Fixed)
- 换行符显示问题：`\n` 现在正确转换为换行符
- 完全移除价格显示（包括对话框中）
- 双面牌检测和背面图片提取逻辑改进
- 修复基础地被错误识别为其他卡牌的问题（如 Swamp → Leatherhead, Swamp Stalker）
- 修复法术力值不显示的问题：查看卡牌详情会自动更新数据库

## [3.9.4] - 2026-01-26

### 重大变更 (Major Change)
- **替换 API 数据源**: 将 Scryfall API 替换为 mtgch.com API（大学院废墟）
  - 使用中文万智牌数据库，优先显示中文卡牌信息
  - 支持中文卡牌名称、规则文本、类型行
  - 提供中文版卡牌图片
  - 使用 `!` 前缀进行精确卡牌查询
  - 完整支持双面牌、多面牌等特殊卡牌类型

### 新增 (Added)
- MtgchApi.kt: mtgch.com API 接口定义
- MtgchCardDto.kt: API 数据传输对象
- MtgchMapper.kt: DTO 到 Entity 的映射转换

### 改进 (Improved)
- 卡牌搜索现在优先返回中文结果
- 卡牌详情页面显示中文规则文本和类型
- 双面牌背面图片现在正确显示
- 法术力值符号彩色渲染

### 修复 (Fixed)
- 空赛事自动提示下载套牌（显示对话框让用户选择）
- 修复下载套牌时赛事名称错误匹配的问题
- 卡牌详情页面加载逻辑

---

### 修复 (Fixed)
- **修复卡牌详情加载**: 实现卡牌详情页面的数据加载逻辑
  - CardDetailViewModel 现在正确从 repository 加载卡牌信息
  - 修复双面牌背面图片无法显示的问题
  - 修复卡牌信息（法术力值、类型、规则文本）无法显示的问题

- **修复套牌法术力值显示**: 添加法术力值符号渲染功能
  - 创建 ManaSymbolRenderer 工具类，将 {2}{U}{B} 等格式转换为彩色符号
  - 支持所有MTG法术力符号（单色、双色、混色、数字等）
  - 在 DeckDetailActivity 和 CardAdapter 中应用新渲染器
  - 法术力值现在显示为带颜色的粗体文本

- **空赛事自动下载提示**: 进入没有套牌的赛事页面时自动提示下载
  - 添加 shouldShowDownloadDialog() 方法
  - 只在第一次进入空赛事时显示一次提示对话框
  - 改进用户体验，避免用户不知道如何下载套牌

- **修复下载套牌错误匹配**: 修复下载比赛套牌时显示为其他比赛的问题
  - 改进 extractEventInfo() 函数的逻辑
  - 优先从页面标题提取赛事名称，避免将卡组标题误认为赛事名称
  - 添加智能判断：如果标题包含 " by " 或过多的"-"分隔，说明可能是卡组页面
  - 确保所有卡组都使用同一个正确的赛事名称

### 改进 (Improved)
- 法术力值显示：从原始字符串（{2}{U}{B}）升级为彩色符号显示
- 卡牌详情页面：现在可以正确显示卡牌的完整信息
- 套牌下载：更准确的赛事名称匹配

### 技术细节 (Technical)
- CardDetailViewModel.kt: 实现 loadCardDetail() 方法
- ManaSymbolRenderer.kt: 新增法术力值符号渲染工具类
- DeckDetailActivity.kt: 应用法术力值渲染
- CardAdapter.kt: 应用法术力值渲染
- EventDetailActivity.kt: 添加自动下载提示逻辑
- EventDetailViewModel.kt: 添加 shouldShowDownloadDialog() 方法
- MtgTop8Scraper.kt: 改进 extractEventInfo() 函数逻辑

---

### 修复 (Fixed)
- **修复Loading闪烁问题**: 修复底部导航栏循环触发导致的无限loading
  - 添加 isProgrammaticNav 标志防止编程式导航触发监听器
  - 避免重复调用 loadEvents() 和 loadFavoriteDecklists()
  - 消除画面闪烁和交互阻塞问题
  - 确保应用启动后正常响应

### 技术细节 (Technical)
- MainActivity.kt:
  - 添加 isProgrammaticNav 标志变量
  - 更新 setupBottomNavigation() 检查编程式导航
  - 更新 switchToTab() 使用标志防止循环
  - 添加 currentTab 检查避免重复切换

---

## [3.9.1] - 2026-01-23

### 改进 (Improved)
- **赛事页面下载动画**: 添加内联进度指示器，替代全屏遮罩
  - 显示下载进度文字（如 "正在下载 0/32"）
  - 改进用户反馈，不再阻塞界面

- **底部导航栏统一**: 统一主页和赛事页面的底部导航
  - 使用Material BottomNavigationView
  - 确保导航选中状态正确显示
  - 修复"我的收藏"文字显示

- **双面牌下载优化**: 改进双面牌图片提取逻辑
  - 优先使用card_faces中的图片
  - 正确处理单面和双面牌的图片URL
  - 完善双面牌元数据提取

- **导出按钮文案**: 将导出菜单改为中文
  - "Copy as Text" → "复制为文本"
  - "Copy as JSON" → "复制为JSON"
  - "Add to Favorites" → "收藏套牌"

- **套牌页面后退箭头**: 添加后退箭头图标
  - 使用MD3配色（黑色箭头）
  - 确保在白色背景上清晰可见

### 技术细节 (Technical)
- activity_event_detail.xml: 添加进度容器和进度文字
- EventDetailActivity.kt: 更新为使用BottomNavigationView和inline progress
- MainActivity.kt: 添加导航项选中状态设置
- DecklistRepository.kt: 改进双面牌图片URL提取逻辑
- menu_deck_detail.xml: 菜单项中文化
- activity_deck_detail.xml: 更新Toolbar配色
- ic_arrow_back.xml: 使用MD3 on_surface颜色

---

## [3.9.0] - 2026-01-23

### 新增功能 (Added)
- **全新Material Design 3主题**: 采用现代化的MD3配色方案
  - 更新颜色系统，使用紫色主题（#6750A4）
  - 统一主色调、次要色调和错误颜色
  - 新增表面颜色和边框颜色变体

### 改进 (Improved)
- **底部导航栏重构**: 使用Material BottomNavigationView替代自定义按钮
  - 添加选中状态指示器
  - 优化图标和文字排版
  - 改进视觉反馈和交互体验

- **筛选按钮优化**: 重新设计筛选按钮布局
  - 使用卡片容器包裹按钮组
  - 简化按钮文字（"赛制"、"日期"、"下载"）
  - 添加图标和更好的视觉层次

- **赛事卡片样式升级**: 改进赛事列表项的视觉设计
  - 使用Chip组件显示赛制
  - 添加圆角徽章显示套牌数量
  - 优化间距和字体大小
  - 移除不必要的信息

- **空状态支持**: 添加空状态布局模板
  - 创建统一的空状态设计
  - 支持自定义标题和提示信息
  - 可选操作按钮

### 技术细节 (Technical)
- colors.xml: 完整重写，符合MD3规范
- activity_main.xml: 使用BottomNavigationView
- item_event.xml: 使用Chip组件和新的样式
- layout_empty_state.xml: 新增空状态布局
- drawable/deck_count_background.xml: 新增徽章背景
- menu/bottom_navigation_menu.xml: 新增导航菜单

---

## [3.8.0] - 2026-01-22

### 新增功能 (Added)
- **双面牌支持**: 完整实现双面牌显示和切换功能
  - 卡牌详情页添加正反面切换按钮
  - 双面牌数据模型已完善，支持正反面图片和信息切换
  - Scryfall API 数据映射正确处理双面牌的 card_faces

### 改进 (Improved)
- **赛事页面优化**: 卡组下载时在列表项显示 loading 图标，移除全屏遮罩 loading 动画
- **导出功能**: 套牌导出改为导出到剪贴板，更方便用户分享
- **导航图标**: 套牌详情页左上角后退按钮添加箭头图标
- **界面简化**: 移除赛事列表页面的比赛类型显示（league、challenge等）
- **收藏按钮**: 我的收藏按钮不再显示数字，界面更简洁

### 技术细节 (Technical)
- Decklist.kt: 添加 isLoading 字段支持 loading 状态
- DecklistTableAdapter.kt: 添加 loading 状态显示
- CardDetailActivity.kt: 实现双面牌切换逻辑，添加 updateCardDisplay() 方法
- DeckDetailActivity.kt: 导出功能使用 ClipboardManager 复制到剪贴板
- item_decklist_table.xml: 添加 ProgressBar 显示 loading 状态
- item_event.xml: 移除 tvEventType 显示
- activity_event_detail.xml: 移除 tvEventType 显示
- menu_deck_detail.xml: 菜单项文本改为 "Copy as Text" 和 "Copy as JSON"

---

## [3.7.0] - 2026-01-22

### 新增功能 (Added)
- **套牌名称显示**: 添加套牌名称存储和显示功能
  - DecklistEntity 添加 deck_name 字段
  - Domain Model 添加 deckName 属性
  - UI 优先显示套牌名称而非玩家名称

### 改进 (Improved)
- **赛事详情页**: 套牌列表现在优先显示套牌名称（如 "Landless Belcher"）而非玩家名称
  - 如果套牌名称不可用，则回退显示玩家名称
  - 收藏列表同样优先显示套牌名称

### 技术细节 (Technical)
- 数据库升级：v4 → v5
- MIGRATION_4_5: 为 decklists 表添加 deck_name 列
- DecklistEntity.kt: 添加 deckName 字段
- Decklist.kt: 添加 deckName 属性
- 所有 Repository 方法更新以保存 deckName
- 所有 ViewModel 和 Adapter 更新以显示 deckName

---

## [3.6.3] - 2026-01-22

### 修复 (Fixed)
- **修复自动下载功能**: 修复赛事详情页自动下载无法触发的问题
  - 根本原因：EventItem 数据类缺少 sourceUrl 字段
  - MainViewModel.EventItem 和 EventListViewModel.EventItem 添加 sourceUrl 字段
  - MainActivity 和 EventListActivity 传递正确的 sourceUrl 到 EventDetailActivity
  - EventDetailViewModel 改用 setValue() 而非 postValue() 确保立即更新数据

- **修复 LiveData 时序问题**: 改进 loadEventDetail() 的数据更新方式
  - 将 postValue() 改为 setValue() 确保同步更新
  - 避免因 postValue() 异步特性导致的 shouldAutoDownload() 检查失败
  - 确保在观察者触发时数据已完全更新

### 技术细节 (Technical)
- MainViewModel.kt: EventItem 添加 sourceUrl 字段
- EventListViewModel.kt: EventItem 添加 sourceUrl 字段
- EventDetailViewModel.kt: loadEventDetail() 改用 setValue()
- MainActivity.kt: 传递正确的 sourceUrl 到 Event 对象
- EventListActivity.kt: 传递正确的 sourceUrl 到 Event 对象

---

## [3.6.2] - 2026-01-21

### 修复 (Fixed)
- **改进套牌名称提取**: 增强套牌名称和玩家名称的提取逻辑
  - 添加多种提取策略（div.event_title、页面标题、h3标签）
  - 添加详细的调试日志以便排查问题
  - 验证提取的套牌名称不是玩家名称
  - 支持更多HTML结构变化

- **修复自动下载逻辑**: 改进赛事详情页的自动下载触发
  - 添加详细的调试日志
  - 确保sourceUrl存在时才触发下载
  - 记录自动下载的触发状态

- **修复日期格式转换**: 统一日期格式为YYYY-MM-DD
  - 将MTGTop8的DD/MM/YY格式转换为标准YYYY-MM-DD格式
  - 确保日期筛选功能正常工作
  - 添加convertDateToStandard()方法处理日期转换

### 技术细节 (Technical)
- MtgTop8Scraper.kt: 改进extractPlayerAndDeckName()方法，添加多种提取策略
- MtgTop8Scraper.kt: 添加convertDateToStandard()方法进行日期格式转换
- EventDetailActivity.kt: 添加AppLogger导入和详细日志

---

## [3.6.1] - 2026-01-21

### 修复 (Fixed)
- **修复收藏数量更新**: 删除赛事后正确更新收藏数量
  - 删除赛事时同步删除相关收藏记录
  - 删除后自动刷新收藏数量显示
  - 确保收藏数量与实际数据一致

- **完善删除级联**: 删除赛事时删除所有关联数据
  - 删除卡牌数据
  - 删除收藏记录（新增）
  - 删除套牌数据
  - 删除赛事数据

### 技术细节 (Technical)
- FavoriteDecklistDao.kt: 新增 deleteByDecklistIds() 批量删除方法
- DecklistRepository.kt: deleteEvent() 添加收藏记录删除步骤
- MainViewModel.kt: deleteEvent() 添加 loadFavoriteCount() 调用

---

## [3.6.0] - 2026-01-21

### 新增功能 (Added)
- **自动下载套牌**: 进入赛事详情页面后自动下载该赛事的套牌
  - 无需手动点击下载按钮
  - 只在首次进入且没有套牌时自动下载
  - 避免重复下载已有数据

### 改进 (Improved)
- **按钮文字动态显示**: 筛选按钮显示当前选择
  - 筛选赛制按钮：显示 "赛制: Modern" 而非固定 "筛选赛制"
  - 筛选日期按钮：显示 "日期: 20/01/26" 而非固定 "筛选日期"
  - 未筛选时显示默认文字

### 修复 (Fixed)
- **修复赛事日期显示**: 下载赛事信息时正确显示日期
  - 修正HTML解析逻辑，日期从第4列（col3）而非第3列（col2）提取
  - 日期格式：DD/MM/YY

- **修复套牌名称提取**: 改进套牌名称和玩家名称的提取逻辑
  - 跳过第一个div.event_title（通常是赛事名称）
  - 验证提取的套牌名称不是玩家名称
  - 添加双重验证和回退机制

- **改进套牌名称验证**: 防止将玩家名称误认为套牌名称
  - 检查提取名称是否与玩家名称相同
  - 检查是否为排名格式
  - 确保提取的是有效的套牌名称

---

## [3.5.3] - 2026-01-21

### 修复 (Fixed)
- **修复套牌名称显示错误**: 套牌名称现在正确显示套牌名称而不是玩家名称
  - 例如：显示 "Stoneblade" 而不是 "Zihan Zhao"
  - 改进HTML解析逻辑，正确提取 "#5-8 Stoneblade - Player Name" 格式中的套牌名称

- **修复录制成绩显示**: 录制成绩现在正确显示范围格式
  - 例如：显示 "#5-8" 而不是 "N/A"
  - 正则表达式从 `^#(\d+)\s+` 更新为 `^#([\d\-]+)\s+`
  - 支持纯数字 (#1) 和范围格式 (#5-8)

- **修复赛事内套牌排序**: 套牌现在按照 d= 参数正确排序
  - 从套牌URL中提取 d= 参数进行排序
  - 例如：e=79241?d=1 排在 e=79241?d=2 前面

- **修复多次弹窗问题**: 下载完成后不再弹出多个赛制选择窗口
  - 将 StateFlow 的 `collect` 改为 `value` 属性读取
  - showFormatFilterDialog、showDateFilterDialog、showDownloadEventDialog 已修复

---

## [3.5.2] - 2026-01-21

### 改进 (Improved)
- **优化删除交互**: 删除赛事改为滑动后显示确认对话框
  - 滑动赛事卡片后不会立即删除
  - 显示确认对话框询问是否删除
  - 明确告知用户将同时删除该赛事下的所有套牌和卡牌
  - 用户可以取消删除操作

- **移除下载提示**: 筛选赛制和日期时不显示"下载提示"
  - 筛选赛制后如果没有数据，不再提示"No formats available. Download data first."
  - 筛选日期后如果没有数据，不再提示"No dates available. Download data first."
  - 只有点击"下载赛事"按钮时才弹出下载对话框
  - 空列表状态不显示任何提示

---

## [3.5.1] - 2026-01-21

### 修复 (Fixed)
- **修复卡组枚举数量问题**: 添加智能卡组计数功能
  - 新增countDecklistsInEvent方法，从赛事页面统计实际卡组数量
  - 现在只枚举实际存在的卡组数量，不会多下载
  - 修复e=79243只有16套牌却下载50套的问题

---

## [3.5.0] - 2026-01-21

### 新增功能 (Added)
- **双面牌支持**: 卡牌详情页面现在可以正确显示双面牌
  - 从Scryfall API获取双面牌的card_faces数据
  - 同时显示正面和反面的名称、法术力、类型和规则文本
  - 数据库新增双面牌相关字段（isDualFaced, frontFaceName, backFaceName等）
  - 数据库版本从v3升级到v4

### 技术细节 (Technical)
- ScryfallCardDto.kt: 新增CardFaceDto数据类和cardFaces字段
- CardInfoEntity.kt: 新增双面牌相关字段
- CardInfo.kt: 新增双面牌相关属性
- AppDatabase.kt: 新增MIGRATION_3_4迁移
- DecklistRepository.kt: 更新toEntity()和toDomainModel()方法
- CardDetailActivity.kt: 更新UI以显示双面牌反面信息
- activity_card_detail.xml: 新增llBackFace容器显示反面信息

---

## [3.4.4] - 2026-01-21

### 修复 (Fixed)
- **修复套牌数量限制**: 增加套牌枚举上限从20到50
  - 修复32套牌的赛事只能下载19套的问题
  - 现在支持最多50套牌的大型赛事

- **改进套牌名称解析**: 优化套牌名称和成绩提取逻辑
  - 改进extractPlayerAndDeckName方法，遍历所有div.event_title查找排名格式
  - 添加详细的调试日志输出
  - 添加容错处理，支持多种HTML结构

---

## [3.4.3] - 2026-01-21

### 性能优化 (Performance)
- **大幅提升下载速度**: 优化卡组下载流程
  - 增加并发数从3到8，下载速度提升约2.5倍
  - 将Scryfall卡牌详情获取改为后台异步执行
  - 卡组数据保存完成后立即返回，Scryfall详情在后台获取
  - 用户可以立即查看卡组，无需等待Scryfall详情加载完成

---

## [3.4.2] - 2026-01-21

### 修复 (Fixed)
- **修复赛事信息丢失问题**: 修复下载套牌后赛事名称、赛制等信息消失的问题
  - 新增extractEventInfo方法，在下载套牌前先提取完整的赛事信息
  - 赛事名称现在从页面的div.event_title元素提取
  - 赛制从URL的f参数提取
  - 日期从meta区域提取

- **修复卡组枚举数量错误**: 修复下载卡组数量超过实际数量的问题
  - 从并发枚举改为串行枚举
  - 添加连续失败3次后停止的逻辑
  - 现在只下载实际存在的卡组，不会多下载

---

## [3.4.1] - 2026-01-21

### 修复 (Fixed)
- **修复筛选赛制功能**: 修复赛制筛选无法正确显示赛事的问题
  - 选择赛制后，现在正确显示已下载的该赛制赛事
  - 下载赛事对话框默认选中当前筛选的赛制
  - 筛选赛制和筛选日期时调用loadEvents()而非loadDecklists()
  - 新增selectedFormatName状态用于UI显示赛制名称

---

## [3.4.0] - 2026-01-21

### 重大UI重构 (Major UI Restructure)
- **移除EventListActivity**: 删除独立的下载比赛页面，所有功能整合到主页面
- **优化按钮布局**: 将筛选赛制、筛选日期、下载赛事按钮移至赛事列表页面上方
  - 三个按钮并排显示，操作更直观
  - 筛选按钮直接作用于赛事列表
  - 下载赛事按钮整合到主页面
- **简化底部导航**: 只保留"赛事列表"和"我的收藏"两个标签
  - 移除"下载赛事"标签
  - 减少页面跳转，提升用户体验

### 改进 (Improved)
- **右滑删除UI优化**: 滑动删除时红色背景增加"删除"文字显示
  - 左滑或右滑时，红色背景中央显示白色"删除"文字
  - 删除操作更直观明确

### 技术细节 (Technical)
- MainActivity.kt:
  - 移除TAB_DOWNLOAD和btnDownloadTournament
  - 添加btnDownloadEvent到filterBar
  - 添加showDownloadEventDialog、showDateSelectionDialog、showNumberOfEventsDialog方法
  - 优化onChildDraw方法，添加删除文字绘制
  - 新增startEventScraping方法
- activity_main.xml:
  - filterBar添加btnDownloadEvent按钮
  - 修改按钮文字为中文："筛选赛制"、"筛选日期"、"下载赛事"
  - 移除btnDownloadTournament，只保留btnEventList和btnFavorites
- MainViewModel.kt:
  - 新增startEventScraping方法，调用repository.scrapeEventsFromMtgTop8
  - 添加60秒超时控制
  - 下载完成后自动刷新赛事列表和筛选选项

---

## [3.3.1] - 2026-01-21

### 修复 (Fixed)
- **修复数据提取问题**: 修复从MTGTop8网站提取的数据显示不正确的问题
  - 套牌名称现在正确显示（例如："Yawgmoth" 而不是 "Unknown Deck"）
  - 玩家名称正确显示（例如："CptInglo" 而不是 "Unknown"）
  - 主牌和备牌数量动态显示（例如："Mainboard (60)" 和 "Sideboard (15)"）
  - 日期正确提取（例如："20/01/26"）
  - 排名正确显示（例如："#1"）

### 改进 (Improved)
- **测试连接功能**: 测试连接现在使用用户选择的format而不是硬编码的Modern
- **赛制筛选功能**: 筛选赛制现在显示所有MTGTop8支持的赛制
  - 之前只能筛选已下载的赛制
  - 现在可以查看所有18个支持的赛制（无论是否已下载）
  - 有数据的赛制优先显示在列表前面

### 技术细节 (Technical)
- MtgTop8Scraper.kt: 重写extractPlayerAndDeckName方法，提取playerName、deckName、record、date
- MtgTop8DecklistDto.kt: 添加record字段
- DecklistRepository.kt: 保存时正确保存record字段
- activity_deck_detail.xml: 添加tvMainboardCount和tvSideboardCount的ID
- DeckDetailActivity.kt: 添加updateCardCount方法动态计算并显示卡牌数量
- EventListActivity.kt: 测试连接使用用户选择的format
- MainViewModel.kt: 显示所有MTGTop8支持的赛制
- EventListViewModel.kt: 显示所有MTGTop8支持的赛制

---

## [3.3.0] - 2026-01-21

### 新增功能 (Added)
- **赛事列表页面重构**: 赛事列表现在以赛事聚合显示，而不是直接展示卡组
  - 点击赛事进入详情页查看该赛事的所有卡组
  - 赛事列表显示赛事名称、格式、日期、类型和卡组数量

### 新增功能 (Added)
- **右滑删除赛事**: 在赛事列表页面可以右滑删除赛事
  - 向左或向右滑动赛事卡片显示红色删除背景
  - 删除赛事会同时删除关联的所有卡组和卡牌
  - 自动刷新列表和筛选选项

### 技术细节 (Technical)
- EventDao.kt: 添加 deleteEventById 和 deleteDecklistsByEventId 方法
- CardDao.kt: 添加 deleteByDecklistIds 批量删除方法
- DecklistRepository.kt: 添加 deleteEvent 方法实现级联删除
- MainViewModel.kt: 添加 loadEvents 和 deleteEvent 方法，注入 EventDao 和 DecklistDao
- MainActivity.kt: 使用 EventAdapter 显示赛事列表，添加 ItemTouchHelper 实现滑动删除
- EventAdapter.kt: 添加 getItemAtPosition 方法

---

## [3.2.4] - 2026-01-20

### 修复 (Fixed)
- **修复 SocketTimeoutException**: 增加超时时间和重试机制
  - 将超时时间从 15 秒增加到 **45 秒**
  - 添加 **3 次自动重试**机制，每次重试间隔 2 秒
  - 改进错误提示，提供详细的故障排查建议
  - testConnection() 方法也增加了重试逻辑
  - fetchEventList() 方法增加一次重试机会

### 技术细节 (Technical)
- MtgTop8Config.kt: TIMEOUT_MS 从 15000 增加到 45000
- MtgTop8Scraper.kt:
  - testConnection() 添加 repeat(3) 重试循环
  - fetchEventList() 添加失败后 2 秒延迟重试
  - 增强错误消息，包含可能原因和解决建议

### 问题背景 (Background)
用户报告连接测试失败，显示 "SocketTimeoutException - Read timeout"。
这是由于 MTGTop8.com 网站响应时间超过 15 秒导致的。
通过增加超时时间和重试机制，大幅提高了连接成功率。

---

## [3.2.3] - 2026-01-20

### 调试增强 (Debug Enhancement)
- **增加更详细的日志**: 在每个关键步骤输出日志
  - 输出爬虫找到的第一个赛事详细信息
  - 输出保存过程的成功/失败状态
  - 使用Log.d输出到Android Logcat
  - 方便通过adb logcat查看详细运行情况

### 使用建议 (Recommendation)
如果下载仍然失败，请使用以下命令查看日志：
```bash
adb logcat | grep -E "DecklistRepository|MtgTop8Scraper"
```

---

## [3.2.2] - 2026-01-20

### 新增功能 (Added)
- **添加连接测试功能**: 可以测试MTGTop8网站连接状态
  - 在下载对话框中添加"测试连接"按钮
  - 显示详细的连接信息：页面标题、HTML长度、表格数量、赛事链接数量
  - 显示前3个赛事链接作为样本
  - 帮助诊断网络问题或网站结构变化

### 使用方法 (How to Use)
1. 打开应用
2. 点击"下载赛事"
3. 点击"测试连接"按钮
4. 查看连接结果
5. 如果显示"✓ 连接成功"，说明网站可访问，再尝试下载
6. 如果显示"✗ 连接失败"，查看具体错误信息

---

## [3.2.1] - 2026-01-20

### 调试改进 (Debug Improvements)
- **增强日志输出**: 添加详细的调试信息
  - 记录每一步的操作（开始爬取、获取页面、解析赛事、保存）
  - 输出页面标题和HTML长度
  - 失败时输出HTML样本（前2000字符）
  - 使用✓和✗标记成功/失败

- **改进错误提示**: 更清晰的错误信息
  - "Unable to fetch events from MTGTop8. The website structure may have changed or network connection failed."
  - "Failed to save any events. Database error occurred."
  - "Download failed: [具体原因]"

### 文档 (Documentation)
- 添加调试指南 (`调试指南.md`)
- 说明如何使用adb logcat查看日志
- 列出常见问题和解决方案

---

## [3.2.0] - 2026-01-20

### 重大改进 (Major Improvement)
- **修复下载逻辑**: 分离赛事下载和卡组下载
  - "下载赛事"现在只下载比赛列表，不下载卡组
  - 下载速度大幅提升（从几分钟到几秒）
  - 用户可以快速浏览比赛列表
  - 点击比赛进入详情页后，再决定是否下载该比赛的卡组

### UI优化 (UI Improvements)
- 更新对话框标题："下载比赛列表"
- 添加说明文字："选择赛制下载比赛列表。下载后点击比赛可以查看详情。"
- 页面标题改为"下载比赛"更清晰
- 成功提示改为："成功下载 X 个比赛！点击比赛查看详情"

### 技术细节 (Technical)
- 修改`scrapeEventsFromMtgTop8`只获取并保存赛事信息
- 移除了卡组下载逻辑（保留在`scrapeSingleEvent`中）
- 保留60秒超时控制和详细错误日志

---

## [3.1.4] - 2026-01-20

### 修复 (Fixed)
- **修复下载卡住问题**: 添加超时控制和更好的错误处理
  - 添加60秒总超时限制，防止无限等待
  - 减少单个请求超时从30秒到15秒
  - 添加详细的中文错误提示
  - 添加ignoreHttpErrors忽略HTTP错误
  - 改进状态提示："开始下载赛事，请稍候..."

### 技术细节 (Technical)
- 使用withTimeout包装整个下载过程
- 添加TimeoutCancellationException捕获
- 导入AppLogger用于详细日志

---

## [3.1.3] - 2026-01-20

### 修复 (Fixed)
- **修复MTGTop8爬虫**: 增强爬虫的容错性和日志输出
  - 添加多种选择器策略，提高网站结构变化的适应性
  - 添加详细的调试日志，方便排查问题
  - 添加followRedirects和maxBodySize配置
  - 改进错误处理和异常捕获
  - 添加parseEventRows辅助方法

### 改进 (Improved)
- 爬虫现在会尝试3种不同的策略来解析页面：
  1. 使用配置的CSS选择器
  2. 遍历所有表格查找赛事行
  3. 直接查找所有包含"event"的链接

---

## [3.1.2] - 2026-01-20

### 优化 (Improved)
- **优化按钮文字**: 将EventListActivity的按钮文字改为中文
  - "Web Scraping" → "下载赛事"
  - "Refresh" → "刷新"
  - "Filter Format" → "筛选赛制"
  - "Filter Date" → "筛选日期"
  - "Events View" → "赛事列表"
  - 使用更清晰的主题色

---

## [3.1.1] - 2026-01-20

### 修复 (Fixed)
- **移除搜索功能**: 从MainActivity移除搜索框和相关逻辑
  - 简化UI界面
  - 移除搜索按钮和输入框

- **修复下载功能**: 修复套牌下载无法工作的问题
  - 将AndroidManifest中的`usesCleartextTraffic`设置为true
  - 允许HTTP流量，确保MTGTop8爬虫正常工作

---

## [3.1.0] - 2026-01-20

### 重大UI重构 (Major UI Restructure)
- **完全重新设计UI架构**: 根据UI草稿完全重构应用界面
  - 移除顶部TabLayout，改用底部导航栏设计
  - 实现符合4张UI草稿的完整设计系统

### 新增功能 (Added)
- **底部导航栏**: MainActivity添加3个底部按钮
  - "下载赛事" - 导航到赛事下载页面
  - "赛事列表" - 显示所有赛事卡组
  - "我的收藏" - 显示收藏的卡组（带数量显示）

### UI优化 (UI Improvements)
- **赛事列表页面 (幻灯片1)**: 使用卡片样式布局
  - 白色背景，灰色边框的卡片设计
  - 浅灰色背景 (#F5F5F5)
  - 简洁的卡片列表显示赛事信息
  - 筛选按钮仅在选择"赛事列表"时显示

- **赛事详情页面 (幻灯片2)**: 表格式布局
  - 页面标题"赛事页面"
  - 提示文本"点击套牌栏进入套牌页面"
  - 套牌列表表格：套牌名称 + 录制成绩
  - 底部导航栏：赛事列表、我的收藏
  - 创建DecklistTableAdapter支持表格显示

- **套牌详情页面 (幻灯片3)**: 两栏布局（移动端适配）
  - 顶部工具栏：返回箭头 + 套牌名称 + 收藏按钮
  - Mainboard区域：带折叠按钮，显示"Mainboard 牌张数量"
  - Sideboard区域：带折叠按钮，显示"Sideboard 牌张数量"
  - 白色卡片 + 灰色边框设计
  - 简洁的卡牌列表展示

### 技术细节 (Technical)
- 创建DecklistTableAdapter用于表格式数据显示
- 更新所有Activity使用底部导航而非顶部标签
- MainActivity支持通过Intent切换标签页
- 创建item_decklist_table.xml用于表格行布局
- 所有页面统一样式：白色卡片、灰色边框、浅灰背景

### 修复 (Fixed)
- 修复导航流程，确保底部导航栏在所有页面正常工作
- 优化Intent传递，支持从其他页面直接导航到特定标签

---

## [3.0.1] - 2026-01-20

### UI优化 (UI Improvements)
- **重新设计MainActivity布局**: 添加AppBarLayout和MaterialToolbar
  - 顶部应用栏显示应用名称
  - 更专业的Material Design 3风格
  - 优化TabLayout样式和颜色

- **优化筛选栏**: 使用卡片样式，添加图标
  - Format按钮添加排序图标
  - Date按钮添加日历图标
  - Download按钮添加下载图标

- **优化搜索框**: 使用卡片样式，添加搜索按钮图标
  - 更现代的Material Design风格
  - 搜索按钮使用IconButton样式

- **优化状态栏**: 使用主题色背景，文字居中
  - 更好的视觉层次

- **添加菜单**: 工具栏添加刷新按钮
  - 快速刷新当前列表

### 新增功能 (Added)
- **收藏按钮**: DeckDetailActivity添加收藏功能
  - 工具栏添加收藏按钮
  - 动态显示收藏/取消收藏状态
  - 点击切换收藏状态
  - 显示Toast提示

### 修复 (Fixed)
- 修复资源引用错误（ThemeOverlay.App.Dark, md_primary_light）
- 优化DeckDetailViewModel添加收藏相关方法
- 修复DeckDetailActivity的收藏状态检查

---

## [3.0.0] - 2026-01-20

### 新增功能 (Added)
- **Favorites 收藏功能**: 可以收藏喜欢的卡组
  - 添加 FavoriteDecklistEntity 和 FavoriteDecklistDao
  - 数据库升级到 v3，新增 favorites 表
  - Repository 新增收藏相关方法：toggleFavorite, isFavorite, getFavoriteDecklists, getFavoriteCount
  - MainViewModel 新增 loadFavoriteDecklists, toggleFavorite, checkIsFavorite 方法

- **UI 重新设计**: 根据 UI 草稿重新设计主界面
  - 添加 TabLayout，支持 Events 和 Favorites 两个标签
  - 简化按钮布局：Format、Date、Download 三个按钮
  - Favorites 标签显示收藏数量
  - 切换标签时自动隐藏/显示相关按钮

- **DatabaseModule**: 创建 Hilt 模块显式提供 DAO
  - 解决 FavoriteDecklistDao 的 Hilt 绑定问题
  - 使用 @Provides 注解显式提供 DAO 实例

### 技术细节 (Technical)
- 数据库版本从 v2 升级到 v3
- MIGRATION_2_3 创建 favorites 表并添加索引
- MainActivity 重构，支持标签切换
- activity_main.xml 重新设计，添加 TabLayout

### 修复 (Fixed)
- 修复 FavoriteDecklistDao 的 Hilt 依赖注入问题
- 创建 DatabaseModule 显式提供 DAO 绑定

---

## [2.6.1] - 2026-01-20

### 性能优化 (Performance)
- **卡组枚举大幅加速**: 并发枚举卡组ID
  - 从串行枚举改为并发枚举（30个ID同时检查）
  - 枚举速度提升约 **20-30倍**
  - 下载卡组从卡顿变流畅

- **Scryfall 智能缓存**: 避免重复请求
  - 下载前先检查本地缓存
  - 只获取缓存中不存在的卡牌
  - 已下载的卡牌秒级响应

### 技术细节 (Technical)
- `fetchEventDecklists()` 使用并发枚举
- `fetchScryfallDetails()` 添加智能缓存检查
- 优化 AppLogger 日志输出

### 修复 (Fixed)
- 修复下载卡组时卡在 Loading 的问题
- 修复 Scryfall 重复请求导致的性能问题

---

## [2.6.0] - 2026-01-20

### 新增功能 (Added)
- **EventDetailActivity 下载按钮**: 在赛事详情页面添加"Download All Decklists"按钮
  - 点击按钮可直接下载该赛事的所有卡组
  - 自动识别赛制并使用正确的爬虫配置
  - 下载完成后自动刷新卡组列表

- **EventListActivity 日期筛选**: 下载赛事时可选择日期
  - 添加日期选择器，可按日期筛选赛事
  - 支持选择具体日期或全部日期
  - 简化下载流程，移除不必要的选项

- **单赛事下载方法**: 新增 `scrapeSingleEvent()` 方法
  - Repository 层新增专门的单赛事下载功能
  - 支持并发下载卡组，速度更快
  - 自动获取 Scryfall 卡牌详情

### 改进 (Improved)
- 赛制识别更准确：从赛事 format 字段转换为正确的赛制代码
- 下载流程优化：简化为 赛制 → 日期 → 数量
- UI 状态管理：新增 `Downloading` 状态

### 技术细节 (Technical)
- EventDetailViewModel 新增 `downloadEventDecklists()` 方法
- EventDetailActivity 添加下载按钮和确认对话框
- DecklistRepository 新增 `scrapeSingleEvent()` 方法

---

## [2.5.1] - 2026-01-20

### 性能优化 (Performance)
- **下载速度大幅提升**: 使用 Kotlin Coroutines 并发下载
  - Scryfall 卡牌详情并发获取（最多5个并发请求）
  - 同一赛事下的卡组并发下载（最多3个并发）
  - 移除卡组间延迟（从 1000ms → 0ms）
  - 减少赛事间延迟（从 500ms → 100ms）
  - 移除 Scryfall 卡牌间延迟（从 100ms → 0ms）

### 技术细节 (Technical)
- 使用 `async/awaitAll` 实现并发下载
- 使用 `Semaphore` 控制并发数量，避免服务器压力
- 使用 `coroutineScope` 提供协程作用域
- 预估性能提升：**3-5倍加速**（取决于网络和服务器响应）

### 改进 (Improved)
- 保持所有原有功能不变
- 智能并发控制，避免过多请求
- 更好的错误处理和异常捕获

---

## [2.5.0] - 2026-01-20

### 新增 (Added)
- **MTGTop8 多卡组下载功能**: 现在可以正确下载同一赛事下的多个卡组
  - 新增 `extractFirstDeckId()` 方法，从赛事页面提取首个卡组ID
  - 新增 `extractPlayerAndDeckName()` 方法，自动提取玩家名称和卡组名称
  - 实现 d 参数连续枚举，自动发现同一赛事下的所有卡组
  - 新增 `maxDecksPerEvent` 参数，支持控制每个赛事的最大抓取数量
  - EventListActivity 爬取对话框新增第三层选项：选择每个赛事的最大卡组数量

### 改进 (Improved)
- `fetchEventDecklists()` 方法现在支持枚举连续的 d 参数
- DecklistRepository.scrapeEventsFromMtgTop8() 新增 maxDecksPerEvent 参数
- EventListViewModel.startEventScraping() 新增 maxDecksPerEvent 参数
- 智能枚举策略：从首个卡组ID开始递增，连续失败3次后停止

### 技术细节 (Technical)
- 分析 MTGTop8 网站结构，发现 d 参数是连续递增的
- 例如：赛事 e=26215 的卡组ID范围为 399206-399218
- 自动跳过无效的卡组ID，继续枚举下一个
- 支持设置 0（无限制）、5、10、20 等不同抓取数量

### 已知问题 (Known Issues)
- MTGTop8 的部分老旧赛事可能枚举效率较低（需要多次尝试）

---

## [2.4.8] - 2026-01-14

### 新增 (Added)
- **三级架构实现**: Event → Decklist → Card 完整层级结构
  - 新增 EventEntity 和 EventDao 数据库层
  - 新增 EventListActivity 和 EventDetailActivity UI层
  - 新增 fetchEventList() 和 fetchEventDecklists() 爬虫方法
- 数据库迁移 (v1 → v2):
  - 创建 events 表
  - 为 decklists 表添加 event_id 外键
  - 自动聚合现有数据创建赛事记录

### 改进 (Improved)
- DecklistRepository 现在支持通过 eventId 查询卡组
- 重构 MtgTop8Scraper 以支持事件枚举
- 优化数据库外键关系，确保数据完整性

### 技术细节 (Technical)
- 修复数据库迁移 SQL 语法错误
- 添加 EventDao 到 Hilt 依赖注入模块
- 实现事件-卡组关联的完整数据流

### 已知问题 (Known Issues)
- MTGTop8 的 d 参数枚举需要进一步优化（当前遇到 HTTP 500 错误）
- d 参数不是简单数字序列，而是具体的卡组ID

---

## [2.4.7] - 2026-01-13

### 新增
- 初始版本的 MTGTop8 爬虫功能
- 基础的卡组数据导入和展示

### 改进
- 数据库基础架构搭建
- Scryfall API 集成获取卡牌详情

---

## 格式说明

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

### 变更类型
- `新增` - 新功能
- `改进` - 现有功能的改进
- `修复` - bug 修复
- `移除` - 功能移除
- `已知问题` - 尚未解决的问题

## [3.11.0] - 2025-01-27

### 重大新增功能 🎉
- **离线卡牌数据库**: 应用现在内置了完整的 25,578 张卡牌数据库，支持完全离线查询
  - 数据库大小：62 MB（压缩后 APK 约 21 MB）
  - 包含所有卡牌的法术力值、类型、规则文本等信息
  - 双面牌正面和背面信息完整支持
  - 中文翻译支持（优先显示中文名称）
  - 首次启动时自动提示导入数据库（约 2-3 分钟）

- **设置菜单**: 主页面添加了工具栏和菜单按钮
  - 重新导入卡牌数据库
  - 查看应用日志
  - 刷新当前列表

### 核心优化 🔧
- **完全本地化数据加载**: 所有单卡信息现在完全从本地数据库加载，不再联网（图片除外）
  - 大幅提升加载速度（从秒级到毫秒级）
  - 减少网络流量消耗
  - 支持完全离线使用
  - `getCardInfo()` 和 `searchCardsByName()` 不再调用 API

- **双面牌支持增强**: 改进了双面牌背面信息的提取逻辑
  - 支持 `transform`, `modal_dfc`, `reversible_card` 等多种布局
  - 从 `cardFaces` 和 `otherFaces` 字段正确提取背面信息
  - 背面的法术力值、类型、规则文本完整显示
  - 优化 `CardDatabaseDownloadWorker` 的数据映射

### UI改进 🎨
- **工具栏**: 主页面添加 Material Design 工具栏
  - 显示应用标题 "MTG 套牌管理"
  - 右上角显示三点菜单按钮
  - 统一的视觉风格

- **对话框布局优化**: 修复数据库导入对话框的按钮布局
  - 改为垂直排列，避免文字扭曲
  - 每个按钮高度至少 48dp
  - 更好的触摸目标

- **赛制默认值**: 主页面赛制筛选默认值设置为 "All Formats"

### 修复问题 🐛
- **双面牌背面信息**: 修复双面牌背面没有信息的问题
  - 改进 `otherFaces` 数据的提取逻辑
  - 支持从 `otherFaces[1]` 提取背面信息

- **赛事列表刷新**: 修复赛事列表下载后不及时刷新的问题
  - 下载成功后自动调用 `loadEvents()` 和 `loadFilterOptions()`
  - 确保 UI 立即更新

- **赛事日期下载**: 下载赛事信息时同时下载赛事日期
  - `saveEventData()` 正确保存 `date` 字段
  - 从 API 响应正确提取日期信息

### 技术细节 🔑
- 添加了 WorkManager 支持后台导入数据库
  - 创建 `CardDatabaseDownloadWorker` 处理导入逻辑
  - 使用 `assets` 打包 62 MB JSONL 数据文件
  
- 创建 `CardDatabaseManager` 管理数据库状态
  - 检查数据库是否已导入
  - 验证数据库中实际卡牌数量
  - 提供重新导入功能

- 实现新的 DAO 方法
  - `getCardInfoByNameOrEnName()`: 支持中英文名称查询
  - 优先使用本地数据库，不再回退到 API

- Repository 层完全本地化
  - `getCardInfo()`: 仅从本地查询
  - `searchCardsByName()`: 仅从本地搜索
  - 移除所有 API 调用逻辑

### 数据库变更 📊
- 数据库版本保持 v7（无结构变更）
- 新增 `mtgch_cards.jsonl` 到 assets (62 MB)
- APK 大小从 ~5 MB 增加到 ~21 MB

### 已知问题 ⚠️
- 卡牌图片仍需联网加载（由于文件大小考虑，未包含在数据库中）
- 首次导入数据库需要 2-3 分钟，建议在 Wi-Fi 环境下进行
- 部分双面牌的 `otherFaces` 数据可能不完整（取决于源 API）

### 性能指标 ⚡
- 卡牌信息查询：从 ~500ms（API）降低到 ~5ms（本地）
- 卡牌搜索：从 ~1000ms（API）降低到 ~10ms（本地）
- 离线可用性：100%（除图片外）
- 数据库导入速度：约 150-200 张/秒

---
