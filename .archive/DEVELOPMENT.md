# 开发进度日志 (Development Progress)

## 当前版本: v3.5.3 (2026-01-21)

---

## ✅ 已完成的功能

### 1. UI 完全重构 (v3.1.0)
- ✅ 移除顶部 TabLayout，改用底部导航栏
- ✅ 实现 4 张 UI 草稿的完整设计
- ✅ 底部导航：赛事列表、我的收藏
- ✅ 统一卡片样式：白色背景、灰色边框、浅灰背景 (#F5F5F5)
- ✅ DecklistTableAdapter 支持表格式数据显示

### 2. 下载逻辑重构 (v3.2.0)
- ✅ **修正下载逻辑**：分离赛事下载和卡组下载
  - "下载赛事" 只下载比赛列表，不下载卡组
  - 下载速度大幅提升（从几分钟到几秒）
  - 用户可以快速浏览比赛列表
  - 点击比赛进入详情页后，再决定是否下载该比赛的卡组

### 3. UI 重大重构 (v3.4.0)
- ✅ 移除 EventListActivity
- ✅ 筛选按钮移至主页面上方
- ✅ 底部导航简化为2个按钮
- ✅ 右滑删除赛事功能（显示"删除"文字）

### 4. 双面牌支持 (v3.5.0)
- ✅ 从 Scryfall API 获取双面牌的 card_faces 数据
- ✅ 同时显示正面和反面的名称、法术力、类型和规则文本
- ✅ 数据库新增双面牌相关字段
- ✅ 数据库版本从 v3 升级到 v4

### 5. 智能卡组计数 (v3.5.1)
- ✅ 添加 countDecklistsInEvent() 方法
- ✅ 从赛事页面统计实际卡组数量
- ✅ 只枚举实际存在的卡组数量

### 6. 交互优化 (v3.5.2)
- ✅ 删除赛事改为滑动后显示确认对话框
- ✅ 移除筛选时的下载提示

### 7. Bug 修复 (v3.5.3) - **当前版本**
- ✅ 修复套牌名称显示错误（显示套牌名称而非玩家名称）
- ✅ 修复录制成绩显示（支持 #5-8 范围格式）
- ✅ 修复赛事内套牌排序（按 URL 中 d= 参数排序）
- ✅ 修复多次弹窗问题（StateFlow 改用 .value 读取）

---

## 🎯 当前状态

### 已知问题
- 无已知问题

### 技术债务
- ⚠️ MTGTop8 爬虫可能因网站结构变化而失效（已通过多选择器策略增强容错性）
- ⚠️ 网络请求可能被防火墙或 VPN 阻止（已在错误提示中说明）

---

## 📋 后续开发计划

### 优先级 1: 功能完善
1. **收藏功能优化**
   - 在赛事列表页显示收藏状态
   - 支持批量收藏/取消收藏
   - 收藏夹支持排序和筛选

2. **卡组详情页优化**
   - 添加卡牌图片显示（使用 Scryfall 图片）
   - 添加卡牌法术力值显示（已从 Scryfall 获取）
   - 添加卡牌颜色标识
   - 支持按法术力值、颜色、类型排序

3. **赛事详情页优化**
   - 显示每个卡组的获胜记录
   - 支持按战绩排序卡组
   - 添加"下载所有卡组"的进度显示

### 优先级 3: 性能优化
1. **缓存优化**
   - 实现 Scryfall 卡牌信息的智能缓存（已部分完成）
   - 添加缓存过期机制
   - 支持离线浏览已下载的卡组

2. **下载优化**
   - 并发下载控制（已实现 Semaphore 限流）
   - 支持暂停/恢复下载
   - 添加下载队列管理

### 优先级 4: 用户体验
1. **错误处理**
   - 更友好的错误提示（已完成部分）
   - 添加"重试"按钮（已实现自动重试）
   - 错误报告功能

2. **帮助文档**
   - 添加应用内帮助页面
   - 首次使用引导
   - FAQ 常见问题

3. **设置页面**
   - 允许用户自定义超时时间
   - 选择下载源（MTGTop8, Magic.gg 等）
   - 清除缓存选项

---

## 🔧 技术栈总结

### 架构模式
- **MVVM + Clean Architecture**
- **Repository Pattern**（数据层）
- **Hilt Dependency Injection**（依赖注入）

### 主要技术
- **Kotlin Coroutines**（异步操作）
- **Room Database**（本地数据库，v4）
- **Jsoup**（HTML 解析）
- **Glide**（图片加载）
- **Material Design 3**（UI 组件）

### 数据模型
- **三级架构**: Event → Decklist → Card
- **双面牌支持**: CardInfoEntity 新增双面牌字段
- **Scryfall 集成**: 自动获取卡牌详情（法术力值、颜色、稀有度等）

---

## 📊 版本历史

### v3.5.3 (2026-01-21) - **当前版本**
- 修复套牌名称显示错误
- 修复录制成绩显示（支持 #5-8 范围格式）
- 修复赛事内套牌排序（按 URL 中 d= 参数排序）
- 修复多次弹窗问题（StateFlow 改用 .value 读取）

### v3.5.2 (2026-01-21)
- 优化删除交互（滑动后显示确认对话框）
- 移除筛选时的下载提示

### v3.5.1 (2026-01-21)
- 修复卡组枚举数量问题
- 添加智能卡组计数功能

### v3.5.0 (2026-01-21)
- 双面牌支持
- 数据库升级到 v4

### v3.4.0 (2026-01-21)
- 重大UI重构
- 移除 EventListActivity
- 简化底部导航
- 右滑删除赛事

### v3.0.0 (2026-01-20)
- 收藏功能
- 重新设计UI
- 数据库升级到 v3

---

## 🚀 部署状态

### 当前 APK
- 文件: `decklist-manager-v3.5.3-debug.apk`
- 位置: `app/build/outputs/apk/debug/`
- 签名: debug.keystore
- 大小: 8.2 MB

### 版本信息
- versionCode: 50
- versionName: "3.5.3"
- minSdk: 21
- targetSdk: 34

---

## 📝 备注

### 调试命令
```bash
# 查看应用日志
adb logcat | grep -E "DecklistRepository|MtgTop8Scraper"

# 查看 MtgTop8 爬虫日志
adb logcat | grep "MtgTop8Scraper"

# 查看所有错误
adb logcat *:E
```

### 关键文件位置
- MtgTop8Config.kt: `/app/src/main/java/com/mtgo/decklistmanager/data/remote/api/`
- MtgTop8Scraper.kt: 同上
- FormatMapper.kt: `/app/src/main/java/com/mtgo/decklistmanager/util/`
- DecklistRepository.kt: `/app/src/main/java/com/mtgo/decklistmanager/data/repository/`
- MainActivity.kt: `/app/src/main/java/com/mtgo/decklistmanager/ui/decklist/`
- EventDetailActivity.kt: 同上

### 数据库位置
- 数据库文件: `/data/data/com.mtgo.decklistmanager/databases/`
- Schema 导出: `/app/schemas/`
- 当前版本: v4

---

**最后更新**: 2026-01-21
**下次更新**: 根据测试反馈和开发进展
