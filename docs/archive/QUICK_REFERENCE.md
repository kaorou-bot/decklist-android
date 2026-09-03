# 快速参考 - MTGO Decklist Manager

> 为开发者准备的快速参考指南

---

## 🚀 快速开始

```bash
# 进入项目目录
cd ~/decklist-android

# 构建项目
./gradlew assembleDebug

# 安装到设备
adb install -r app/build/outputs/apk/debug/*.apk

# 查看日志
adb logcat | grep "Decklist"
```

---

## 📊 项目信息

- **包名**: `com.mtgo.decklistmanager`
- **版本**: v3.8.0 (versionCode: 56)
- **最小SDK**: 21 (Android 5.0)
- **目标SDK**: 34 (Android 14)
- **数据库**: Room v5

---

## 🏗️ 项目结构

```
app/src/main/java/com/mtgo/decklistmanager/
├── data/
│   ├── local/          # 本地数据（Room数据库）
│   ├── remote/         # 远程数据（爬虫）
│   └── repository/     # 数据仓库
├── domain/model/       # 领域模型
├── ui/                 # UI层
│   ├── decklist/       # 赛事和套牌界面
│   └── carddetail/     # 卡牌详情界面
├── util/               # 工具类
└── di/                 # 依赖注入
```

---

## 🔧 常用命令

### Gradle命令
```bash
./gradlew assembleDebug              # 构建Debug版本
./gradlew assembleRelease            # 构建Release版本
./gradlew clean                      # 清理构建
./gradlew build                      # 完整构建
```

### ADB命令
```bash
adb devices                          # 查看连接的设备
adb install -r <apk路径>             # 安装APK（-r表示替换）
adb uninstall com.mtgo.decklistmanager  # 卸载应用
adb logcat | grep "Decklist"         # 查看应用日志
adb shell pm list packages           # 查看已安装的应用
```

### Git命令
```bash
git status                           # 查看状态
git pull origin main                 # 拉取最新代码
git push origin main                 # 推送代码
git checkout -b feature/name         # 创建功能分支
git add .                            # 添加所有修改
git commit -m "message"              # 提交
```

### 数据库调试
```bash
adb shell
run-as com.mtgo.decklistmanager
sqlite3 databases/decklist.db
.tables
.schema events
SELECT * FROM events LIMIT 5;
```

---

## 📱 应用架构

### MVVM架构
```
View (Activity/Fragment)
    ↓ observes
ViewModel
    ↓ uses
Repository
    ↓ manages
Local DB (Room) + Remote Source (Jsoup)
```

### 数据模型
```
Event (赛事)
  ↓ 包含
Decklist (套牌)
  ↓ 包含
Card (卡牌)
  ↓ 关联
CardInfo (卡牌详细信息)
```

---

## 🔑 关键文件

### 配置文件
- `app/build.gradle` - 应用构建配置
- `build.gradle` - 项目构建配置
- `gradle.properties` - Gradle属性
- `settings.gradle` - 项目设置

### 核心代码
- `DecklistRepository.kt` - 数据仓库
- `MtgTop8Scraper.kt` - MTGTop8爬虫
- `AppDatabase.kt` - 数据库配置
- `MainActivity.kt` - 主界面
- `EventDetailActivity.kt` - 赛事详情
- `CardDetailActivity.kt` - 卡牌详情

### 实体类
- `EventEntity.kt` - 赛事实体
- `DecklistEntity.kt` - 套牌实体
- `CardEntity.kt` - 卡牌实体
- `CardInfoEntity.kt` - 卡牌信息实体
- `FavoriteDecklistEntity.kt` - 收藏实体

---

## 🎨 UI组件

### Activities
- `MainActivity` - 主界面（赛事列表）
- `EventDetailActivity` - 赛事详情（套牌列表）
- `DeckDetailActivity` - 套牌详情（卡牌列表）
- `CardDetailActivity` - 卡牌详情

### Fragments
- `CardInfoFragment` - 卡牌信息片段

### Adapters
- `EventAdapter` - 赛事列表适配器
- `DecklistTableAdapter` - 套牌列表适配器（表格）
- `CardAdapter` - 卡牌列表适配器

---

## 💾 数据库Schema

### 表结构

#### events (赛事表)
```sql
id INTEGER PRIMARY KEY
name TEXT
event_type TEXT
format TEXT
date TEXT
source_url TEXT UNIQUE
decklist_count INTEGER
```

#### decklists (套牌表)
```sql
id INTEGER PRIMARY KEY
event_id INTEGER
player_name TEXT
deck_name TEXT
standing TEXT
source_url TEXT UNIQUE
```

#### cards (卡牌表)
```sql
id INTEGER PRIMARY KEY
decklist_id INTEGER
card_name TEXT
quantity INTEGER
is_sideboard INTEGER
position INTEGER
```

#### card_info (卡牌信息表)
```sql
card_name TEXT PRIMARY KEY
mana_cost TEXT
colors TEXT
type_line TEXT
oracle_text TEXT
power TEXT
toughness TEXT
image_url TEXT
...
```

---

## 🌐 API集成

### Scryfall API
```bash
# 获取单卡信息
curl "https://api.scryfall.com/cards/named?fuzzy={card_name}"

# 示例
curl "https://api.scryfall.com/cards/named?fuzzy=Lightning+Bolt"
```

### MTGTop8
- URL: https://mtgtop8.com
- 方法: HTML解析（Jsoup）
- 支持格式: Modern, Standard, Legacy, Vintage, Pauper, Pioneer等

---

## 🐛 调试技巧

### 启用详细日志
```kotlin
AppLogger.d("TAG", "Debug message")
AppLogger.e("TAG", "Error message", exception)
```

### 查看数据库
```bash
# 导出数据库
adb shell run-as com.mtgo.decklistmanager cat databases/decklist.db > decklist.db
sqlite3 decklist.db
```

### 清除应用数据
```bash
adb shell pm clear com.mtgo.decklistmanager
```

### 重启应用
```bash
adb shell am force-stop com.mtgo.decklistmanager
adb shell monkey -p com.mtgo.decklistmanager -c android.intent.category.LAUNCHER 1
```

---

## 📝 版本发布检查清单

- [ ] 更新 `versionCode` 和 `versionName` in `app/build.gradle`
- [ ] 更新 `CHANGELOG.md`
- [ ] 运行所有测试
- [ ] 检查ProGuard混淆
- [ ] 测试APK在真实设备上
- [ ] 更新文档
- [ ] 创建Git标签
- [ ] 推送到GitHub

---

## 🔗 有用的链接

- [Android Developers](https://developer.android.com)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://dagger.dev/hilt/)
- [Scryfall API Documentation](https://scryfall.com/docs/api)
- [Jsoup Documentation](https://jsoup.org/)

---

## 📞 获取帮助

### 查看日志
```bash
# 应用日志
adb logcat | grep "Decklist"

# 爬虫日志
adb logcat | grep "MtgTop8Scraper"

# 所有错误
adb logcat *:E
```

### 常见问题

**Q: 构建失败？**
A: 清理构建缓存 `./gradlew clean cleanBuildCache`

**Q: APK无法安装？**
A: 检查版本号，使用 `-r` 参数替换已安装的版本

**Q: 数据库错误？**
A: 清除应用数据或卸载重装

---

**快速参考版本**: v1.0
**最后更新**: 2026-01-23
