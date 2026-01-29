# MTGO Decklist Manager

[![Android](https://img.shields.io/badge/Android-5.0%2B-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android.com)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-3.12.0-brightgreen.svg)](CHANGELOG.md)

一个用于浏览、搜索和分析 Magic: The Gathering 竞技牌组数据的 Android 应用程序。

## 项目概述

MTGO Decklist Manager 允许用户：
- 📋 浏览 MTGTop8 竞技赛事和牌组数据
- 🔍 按格式和日期筛选赛事
- 📖 查看牌组详细卡表
- 🔮 查询单卡详细信息（使用 mtgch.com API - 大学院废墟）
- 🖼️ 查看高清卡牌图片（包括双面牌，支持中文图片）
- ⭐ 收藏喜欢的牌组
- 🗑️ 滑动删除赛事
- 🌐 从 MTGTop8.com 爬取最新数据

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin |
| 架构 | MVVM + Clean Architecture |
| UI | Material Design 3 |
| 数据库 | Room (v4) |
| 网页解析 | Jsoup |
| 异步 | Coroutines + StateFlow |
| 依赖注入 | Hilt |
| 图片加载 | Glide |
| 构建工具 | Gradle 8.1.2 |

## 项目结构

```
app/
├── src/main/java/com/mtgo/decklistmanager/
│   ├── data/
│   │   ├── local/
│   │   │   ├── entity/         # Room 数据实体
│   │   │   ├── dao/            # 数据访问对象
│   │   │   └── database/       # 数据库配置 (v4)
│   │   ├── remote/
│   │   │   └── api/            # MTGTop8 爬虫
│   │   └── repository/         # 数据仓库
│   ├── domain/model/           # 领域模型
│   ├── ui/                     # UI 层
│   │   ├── decklist/           # 主界面、赛事列表、赛事详情
│   │   └── carddetail/         # 卡牌详情
│   ├── util/                   # 工具类 (FormatMapper, AppLogger)
│   └── di/                     # 依赖注入模块
└── build.gradle
```

## 环境要求

- **Android Studio**: Hedgehog (2023.1.1) 或更高
- **JDK**: 17
- **Android SDK**: API 34
- **Gradle**: 8.1.2
- **最低 Android 版本**: 5.0 (API 21)
- **目标 Android 版本**: 14 (API 34)

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd decklist-android
```

### 2. 打开项目

使用 Android Studio 打开项目目录，等待 Gradle 同步完成。

### 3. 构建项目

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease
```

### 4. 运行应用

```bash
# 连接设备后
./gradlew installDebug
```

或在 Android Studio 中点击运行按钮。

## 更新卡牌数据库

应用包含超过 **66,000 张卡牌**的离线数据库，使用 MTGCH API 数据。

### 自动更新数据库

首次启动应用时，会自动导入数据库。如需重新导入：

1. 在设置中选择"重新导入数据库"
2. 或清除应用数据后重启应用

### 手动更新数据库

项目提供了便捷的更新脚本：

```bash
# 运行更新脚本
./update_database.sh
```

该脚本会：
- ✅ 从 MTGCH GitHub 仓库下载最新的卡牌数据
- ✅ 验证数据格式和完整性
- ✅ 自动备份旧数据库
- ✅ 更新本地数据库文件
- ✅ 显示更新统计信息

**更新后需要重新编译应用**：
```bash
./gradlew assembleDebug
```

### 数据源说明

卡牌数据来自 [MTGCH (大学院废墟)](https://mtgch.com/) API，包含：

- **官方中文翻译**：有中文印刷的卡牌（如艾卓王权系列）
- **AI 翻译**：无官方中文的卡牌使用机器翻译作为 fallback
- **中文卡图**：MTGCH 提供的带中文水印的卡牌图片

支持的卡牌属性：
- 中文名称（`zhs_name` 或 `atomic_translated_name`）
- 中文类型和规则文本
- 法力费用、力量/防御力
- 赛制合法性信息
- 高清卡牌图片（包括双面牌）

## 主要功能

### 赛事浏览
- 三级架构：赛事列表 → 赛事详情 → 套牌详情
- 按日期降序排列
- 滑动删除赛事（需确认）

### 筛选功能
- 按格式筛选（Modern、Standard、Pioneer、Legacy 等 18 种赛制）
- 按日期筛选
- 组合筛选

### 套牌详情
- 主牌列表（动态显示卡牌数量）
- 备牌列表
- 卡牌详情查看
- 收藏功能

### 单卡查询
- 集成 Scryfall API
- 完整卡牌信息
- 法力费用、规则文本
- 合法性信息
- 高清卡牌图片
- **双面牌支持**：显示正面和反面信息

### 收藏功能
- 收藏喜欢的套牌
- 查看收藏列表
- 底部导航显示收藏数量

### 数据爬取
- 从 MTGTop8.com 爬取赛事数据
- 支持格式和日期过滤
- 可自定义爬取数量（5/10/20）
- 智能卡组计数
- 自动提取赛事信息、玩家名称、套牌名称和排名

## MVVM 架构

项目采用标准的 MVVM 架构模式：

```
┌─────────────┐
│   View      │  (Activity/Fragment)
│             │
└──────┬──────┘
       │ observes
       ↓
┌─────────────┐
│  ViewModel  │  (业务逻辑、UI状态)
│             │
└──────┬──────┘
       │ uses
       ↓
┌─────────────┐
│ Repository  │  (数据协调)
│             │
└──────┬──────┘
       │
       ├──────────────┬─────────────┐
       ↓              ↓             ↓
┌─────────────┐ ┌───────────┐ ┌──────────┐
│ Room DB     │ │ Retrofit  │ │ Jsoup    │
│ (本地数据)  │ │ (API)     │ │ (爬虫)   │
└─────────────┘ └───────────┘ └──────────┘
```

## 数据库

使用 Room 数据库 v4，包含四个表：

### events 表
赛事主表，存储赛事基本信息。
- 赛事名称、类型、格式、日期
- 来源URL
- 卡组数量统计

### decklists 表
套牌表，存储套牌基本信息。
- 套牌名称、玩家名称、赛事ID
- 录制成绩（如 #5-8）
- 外键关联到 events 表

### cards 表
卡牌表，存储套牌中的卡牌。
- 卡牌名称、数量、位置（主牌/备牌）
- 外键关联到 decklists 表

### card_info 表
单卡信息缓存表，存储 Scryfall API 查询结果。
- 法力费用、类型、规则文本
- 颜色、势力、攻防
- 图片URL（包括双面牌的正面和反面）
- 双面牌支持（card_faces_json）

### favorites 表
收藏表，存储用户收藏的套牌。
- 关联到 decklists 表

## API 集成

### Scryfall API
- **用途**: 单卡查询
- **文档**: https://scryfall.com/docs/api
- **速率限制**: 10 req/s
- **认证**: 无需 API Key

### MTGTop8.com
- **用途**: 赛事和套牌数据爬取
- **URL**: https://mtgtop8.com
- **方法**: HTML 解析 (Jsoup)
- **支持格式**: Modern, Standard, Legacy, Vintage, Pauper, Pioneer, Historic, Alchemy, Premodern 等

## 依赖注入

使用 Hilt 进行依赖注入：

```kotlin
@HiltAndroidApp
class DecklistApplication : Application()

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DecklistRepository
) : ViewModel()
```

## 异步处理

使用 Kotlin Coroutines 处理异步操作：

```kotlin
viewModelScope.launch {
    val decklists = repository.getDecklists(format, date)
    _decklists.value = decklists
}
```

## 图片加载

使用 Glide 加载卡牌图片：

```kotlin
Glide.with(context)
    .load(imageUrl)
    .placeholder(R.drawable.ic_placeholder)
    .error(R.drawable.ic_error)
    .into(imageView)
```

## 构建配置

### Debug 版本
- 使用 debug 签名
- 包含调试信息
- 未优化代码

### Release 版本
- 需要配置签名
- 代码混淆
- 体积优化

## 版本历史

最新版本：**v3.10.2** (2026-01-27)

### v3.10.2 (2026-01-27)
- 🐛 修复双面牌背面无法显示中文信息的问题
- 🐛 修复部分卡牌无法正确下载中文翻译的问题
- 🔧 优化双面牌中文名称提取逻辑，从 cardFaces 数组中获取中文翻译
- 🔧 添加智能重试机制，当中文名缺失时尝试其他搜索结果
- 📝 增强调试日志，便于诊断卡牌信息获取问题
- 🎨 改进双面牌显示，完整展示正面和背面的中文信息

### v3.9.4 (2026-01-26)
- 🔄 将 Scryfall API 替换为 mtgch.com API（大学院废墟）
- 🇨🇳 优先显示中文卡牌信息
- 🖼️ 支持中文版卡牌图片
- 💬 空赛事自动提示下载套牌

### v3.9.3 (2026-01-26)
- 🐛 修复卡牌详情页面无法加载的问题
- 🎨 添加法术力值符号彩色渲染
- 💬 空赛事自动提示下载套牌
- 🐛 修复下载套牌时赛事名称错误匹配的问题

### v3.9.2 (2026-01-23)
- 🐛 修复套牌名称显示错误
- 🐛 修复录制成绩显示（支持 #5-8 格式）
- 🐛 修复赛事内套牌排序
- 🐛 修复多次弹窗问题

### v3.5.2 (2026-01-21)
- ✨ 优化删除交互（滑动后显示确认对话框）
- 🚫 移除筛选时的下载提示

### v3.5.1 (2026-01-21)
- 🐛 修复卡组枚举数量问题
- 🚀 添加智能卡组计数功能

### v3.5.0 (2026-01-21)
- ✨ 双面牌支持
- 📊 数据库升级到 v4

### v3.4.0 (2026-01-21)
- 🎨 重大UI重构
- 📋 移除 EventListActivity
- 🔘 简化底部导航
- 🗑️ 右滑删除赛事

### v3.0.0 (2026-01-20)
- ⭐ 收藏功能
- 📱 重新设计UI
- 💾 数据库升级到 v3

### v2.0.0 (2026-01-12)
- 🔄 从 Python (Kivy) 完全重写为 Kotlin
- ✨ 采用 MVVM 架构
- 🚀 使用 Room 数据库
- 📱 Material Design 3 UI
- ⚡ 性能大幅提升

查看完整更新日志：[CHANGELOG.md](CHANGELOG.md)

## 贡献指南

欢迎贡献代码、报告问题或提出建议！

### 开发流程

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范

- 遵循 Kotlin 代码规范
- 使用 MVVM 架构
- 编写单元测试
- 更新相关文档

## 待办事项

- [ ] 添加单元测试
- [ ] 添加 UI 自动化测试
- [ ] 实现深色模式
- [ ] 添加搜索功能
- [ ] 数据导出功能
- [ ] 优化爬虫性能

## 已知问题

- MTGTop8 网站结构可能随时变化，需要维护爬虫
- 网络不稳定时可能导致下载失败
- 部分边缘情况的错误处理需要完善

## 许可证

本项目仅供学习和个人使用。

## 鸣谢

- [MTGTop8.com](https://mtgtop8.com) 提供赛事和套牌数据源
- [Scryfall](https://scryfall.com) 提供卡牌数据 API
- [Android Architecture Components](https://developer.android.com/topic/architecture)
- [Material Design](https://material.io/design)

## 联系方式

如有问题或建议，欢迎提交 Issue。

---

**注意**: 本项目仅供学习交流使用，请勿用于商业用途。
