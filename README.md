# MTGO Decklist Manager

[![Android](https://img.shields.io/badge/Android-5.0%2B-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android.com)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一个用于浏览、搜索和分析 Magic: The Gathering Online (MTGO) 竞技牌组数据的 Android 应用程序。

## 项目概述

MTGO Decklist Manager 允许用户：
- 📋 浏览 MTGO 竞技牌组数据
- 🔍 按格式和日期筛选牌组
- 📖 查看牌组详细卡表
- 🔮 查询单卡详细信息（使用 Scryfall API）
- 🖼️ 查看高清卡牌图片
- 📊 查看数据库统计信息
- 🌐 从 MTGO 官网爬取最新数据

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin + Java |
| 架构 | MVVM + Clean Architecture |
| UI | Material Design 3 |
| 数据库 | Room |
| 网络 | Retrofit + OkHttp |
| HTML解析 | Jsoup |
| 异步 | Coroutines + Flow |
| 依赖注入 | Hilt |
| 图片加载 | Glide |
| 构建工具 | Gradle |

## 项目结构

```
app/
├── src/main/java/com/mtgo/decklistmanager/
│   ├── data/
│   │   ├── local/
│   │   │   ├── entity/         # Room 数据实体
│   │   │   ├── dao/            # 数据访问对象
│   │   │   └── database/       # 数据库配置
│   │   ├── remote/
│   │   │   └── api/            # API 接口和爬虫
│   │   └── repository/         # 数据仓库
│   ├── domain/model/           # 领域模型
│   ├── ui/                     # UI 层
│   │   ├── decklist/           # 牌组列表
│   │   └── carddetail/         # 卡牌详情
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

## 主要功能

### 牌组浏览
- 显示所有已爬取的牌组
- 按日期降序排列
- 点击查看详情

### 筛选功能
- 按格式筛选（Modern、Standard、Pioneer 等）
- 按日期筛选
- 组合筛选

### 牌组详情
- 主牌列表
- 备牌列表
- 卡牌数量统计
- 点击卡牌名称查看详细信息

### 单卡查询
- 集成 Scryfall API
- 完整卡牌信息
- 法力费用、规则文本
- 合法性信息
- 价格信息
- 高清卡牌图片

### 数据爬取
- 从 MTGO 官网爬取
- 支持格式和日期过滤
- 可自定义爬取数量
- 自动去重

### 数据统计
- 总牌组数量
- 总卡牌数量
- 格式数量
- 缓存卡牌数量

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

使用 Room 数据库，包含三个表：

### decklists 表
牌组主表，存储牌组基本信息。

### cards 表
卡牌表，存储牌组中的卡牌。

### card_info 表
单卡信息缓存表，存储 Scryfall API 查询结果。

## API 集成

### Scryfall API
- **用途**: 单卡查询
- **文档**: https://scryfall.com/docs/api
- **速率限制**: 10 req/s
- **认证**: 无需 API Key

### MTGO 官网
- **用途**: 牌组数据爬取
- **URL**: https://www.mtgo.com/decklists
- **方法**: HTML 解析

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

### v2.0.0 (2026-01-12)
- 🔄 从 Python (Kivy) 完全重写为 Kotlin/Java
- ✨ 采用 MVVM 架构
- 🚀 使用 Room 数据库
- 📱 Material Design 3 UI
- ⚡ 性能大幅提升

### v1.2.1 (Python版本)
- 异步图片加载优化

### v1.2.0 (Python版本)
- 卡牌图片显示功能

### v1.1.0 (Python版本)
- 单卡查询功能

### v1.0.0 (Python版本)
- 初始版本

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

- [ ] 完善 MTGO 爬虫 JSON 解析
- [ ] 添加单元测试
- [ ] 添加 UI 自动化测试
- [ ] 实现深色模式
- [ ] 添加搜索功能
- [ ] 添加收藏功能
- [ ] 数据导出功能

## 已知问题

- MTGO 爬虫需要完整的 JSON 解析实现
- 部分边缘情况的错误处理需要完善
- 离线模式需要优化

## 许可证

本项目仅供学习和个人使用。

## 鸣谢

- [MTGO 官方网站](https://www.mtgo.com) 提供数据源
- [Scryfall](https://scryfall.com) 提供卡牌数据 API
- [Android Architecture Components](https://developer.android.com/topic/architecture)
- [Material Design](https://material.io/design)

## 联系方式

如有问题或建议，欢迎提交 Issue。

---

**注意**: 本项目仅供学习交流使用，请勿用于商业用途。
