# 📚 MTGO Decklist Manager - 文档索引

**项目**: MTGO Decklist Manager (Android)  
**版本**: v2.2.1  
**最后更新**: 2026-01-13  

---

## 🎯 快速导航

### 开始开发
- **[⚡ 快速开始（明天）](QUICK_START_NEXT_TIME.md)** ⭐ 推荐首先查看
- **[📅 每日进度（今天）](DAILY_PROGRESS_2026-01-13.md)** 今天完成的所有工作

### 核心文档
- **[👨‍💻 开发者指南](DEVELOPER_GUIDE.md)** 完整的开发文档
- **[🚀 快速开始](QUICK_START.md)** 项目快速开始指南
- **[📋 数据源分析](SCRAPING_ANALYSIS_SUMMARY.md)** Magic.gg 数据源分析

### 项目说明
- **[README.md](README.md)** 项目说明（英文）
- **[README_CN.md](README_CN.md)** 项目说明（中文）

### 历史文档
- **[项目完成总结](PROJECT_COMPLETION_SUMMARY.md)** v1.0 项目总结
- **[项目就绪说明](PROJECT_READY.md)** v1.0 就绪状态
- **[重构总结](REFACTORING_SUMMARY.md)** v2.0 重构总结

---

## 📋 版本历史

| 版本 | 日期 | 主要变更 | 文档 |
|------|------|----------|------|
| v2.2.1 | 01-13 22:20 | Bug修复+UI改进 | [日志](DEVELOPER_GUIDE.md#v221-2026-01-13--_bug-fixes-improvements) |
| v2.2.0 | 01-13 21:52 | Scryfall集成 | [日志](DEVELOPER_GUIDE.md#v220-2026-01-13--major-update-scryfall-integration-smart-filtering) |
| v2.1.2 | 01-13 21:30 | 爬取修复 | [日志](DEVELOPER_GUIDE.md#v212-2026-01-13--critical-bug-fix) |
| v2.1.1 | 01-13 21:18 | Bug修复 | [日志](DEVELOPER_GUIDE.md#v211-2026-01-13--bug-fixes) |
| v2.1.0 | 01-13 20:44 | Magic.gg数据源 | [日志](DEVELOPER_GUIDE.md#v210-2026-01-13--major-update-magicgg-data-source) |

---

## 🔨 开发命令

### 构建
```bash
cd /home/dministrator/decklist-android
./gradlew assembleDebug
```

### 安装到设备
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 查看日志
```bash
adb logcat | grep -E "Decklist|MTGO|Scryfall"
```

### 查看数据库
```bash
adb shell
sqlite3 /data/data/com.mtgo.decklistmanager/databases/decklist.db
.tables
SELECT * FROM decklists LIMIT 5;
SELECT * FROM cards LIMIT 10;
```

---

## 📂 项目结构

```
/home/dministrator/decklist-android/
├── app/
│   ├── src/main/java/com/mtgo/decklistmanager/
│   │   ├── data/          # 数据层
│   │   ├── domain/        # 领域层
│   │   ├── ui/            # UI层
│   │   └── di/            # 依赖注入
│   ├── build.gradle       # v2.2.1
│   └── src/main/res/      # 资源文件
├── apk-archive/           # APK归档
├── *.md                   # 文档文件
└── build.gradle           # 项目构建文件
```

---

## 🐛 待解决问题

### 🔴 高优先级（需要用户反馈）

1. **卡牌显示问题**
   - 反馈："一个套牌所有单卡都混在了一起"
   - 反馈："单卡详情依然无法点击"
   - 状态：❓ 待确认
   - 需要：截图或详细描述

### 🟡 中优先级（计划中）

2. **扩展数据源**
   - 添加其他赛事类型
   - 支持其他格式

3. **性能优化**
   - 优化 Scryfall API 调用
   - 添加爬取进度显示

---

## ✅ 最近更新

### 今天完成的功能

1. ✅ 自动获取 Scryfall 卡牌详情（法术力值、颜色等）
2. ✅ 实现日历选择器
3. ✅ 添加去重逻辑
4. ✅ 修复 Loading 框一直显示
5. ✅ 移除 "All" 选项，强制选择具体日期

### 技术改进

- MagicScraper.kt：完全重写，支持 Magic.gg
- CardDao.kt：添加 updateDetails() 方法
- DecklistDao.kt：添加 getDecklistByUrl() 方法
- MainActivity.kt：实现 DatePickerDialog
- CardAdapter.kt：恢复法术力值显示

---

## 📞 下次开发开始

### 第一步
```bash
cd /home/dministrator/decklist-android
cat QUICK_START_NEXT_TIME.md
```

### 第二步
根据快速开始文档：
1. 构建项目
2. 优先解决卡牌显示问题（需要用户反馈）
3. 如果问题明确，直接修复

---

## 📖 重要提醒

1. **当前版本**: v2.2.1 (versionCode: 10)
2. **数据源**: Magic.gg Champions Showcase
3. **可用牌组**: 32个（4个赛季 × 8个牌组）
4. **关键问题**: 卡牌显示需要用户确认细节

---

**最后更新**: 2026-01-13  
**文档版本**: 1.0
