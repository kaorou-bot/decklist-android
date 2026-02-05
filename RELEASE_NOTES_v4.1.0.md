# MTGO Decklist Manager v4.1.0 发布说明

## 📦 发布信息

- **版本号：** 4.1.0
- **版本代码：** 77
- **发布日期：** 2026-02-05
- **分支：** dev/v4.1.0

---

## ✨ 新功能

### 1. 套牌导出功能
支持多种格式导出套牌：
- **MTGO 格式** - 用于 MTGO 客户端
- **Arena 格式** - 用于 MTG Arena 客户端
- **文本格式** - 纯文本格式，易于阅读和分享
- **Moxfield 分享** - 生成 Moxfield 分享链接

**实现细节：**
- 创建 `DecklistExporter` 接口
- 实现多种导出格式：`MtgoFormatExporter`, `ArenaFormatExporter`, `TextFormatExporter`
- 实现 `MoxfieldShareGenerator` 生成分享链接
- 集成文件保存和分享功能

### 2. 在线卡牌搜索
完整复制 MTGCH (MTGHelper.cn) 高级搜索功能，支持 13 个搜索字段：

#### 基础字段
1. **卡牌名称** (name) - 支持中英文搜索
2. **规则概述** (oracle) - 搜索规则文本
3. **类别** (type) - 搜索卡牌类型

#### 高级字段
4. **颜色/标识色** (color/identity) - 支持 5 种颜色和无色
   - 颜色匹配模式：正好、至多、至少
   - 标识色开关
5. **法术力值** (CMC) - 支持数值和操作符（=, >, <, >=, <=）
6. **力量/防御力** (P/T) - 分别筛选力量和防御力
7. **赛制合法性** (format/legality) - 筛选特定赛制的合法卡牌
   - 支持 11 种赛制（Standard, Pioneer, Modern, Legacy, Vintage, Pauper, Commander, etc.）
8. **系列** (set) - 按系列代码搜索
9. **稀有度** (rarity) - Common, Uncommon, Rare, Mythic, Special
10. **背景叙述** (flavor) - 搜索背景故事文本
11. **画师** (artist) - 按画师搜索
12. **游戏平台** (game) - Paper, MTGO, Arena
13. **额外卡牌** (is:extra) - 搜索补充包特有卡牌

#### 搜索历史
- 自动保存搜索历史
- 快速重复之前的搜索
- 支持删除历史记录

### 3. 双面牌完整支持
完美支持双面牌和连体牌：

#### 双面牌背面数据
- ✅ 背面名称显示（含中文翻译）
- ✅ 背面类型显示
- ✅ 背面规则文本显示（含中文翻译）
- ✅ 背面力量/防御力显示
- ✅ 背面忠诚度显示
- ✅ 背面卡图显示

#### 中文翻译优化
- 优先使用官方中文翻译
- 如果官方翻译缺失，使用机器翻译作为后备
- 确保所有双面牌都有可读的中文显示

#### 性能优化
- 数据库缓存双面牌数据
- 二次打开速度 < 50ms（从 2000ms 优化）
- 持久化缓存，页面重建不影响

---

## 🐛 Bug 修复

### 1. 修复 "Unknown Deck" 显示问题
**问题：** 某些套牌在列表中显示 "Unknown Deck"
**原因：** MTGTop8 爬虫无法提取套牌名称和玩家名称
**修复：**
- 优化显示逻辑，采用三层回退机制
- 优先显示套牌名称 > 玩家名称 > 赛事名称
- 避免显示不友好的 "Unknown Deck" 文本

**效果对比：**
| 套牌名称 | 玩家名称 | 修复前 | 修复后 |
|---------|---------|--------|--------|
| "Pinnacle Affinity" | "RootBeerAddict02" | "Pinnacle Affinity" | "Pinnacle Affinity" |
| "Unknown Deck" | "RootBeerAddict02" | "Unknown Deck" | "RootBeerAddict02" |
| "Unknown Deck" | "Unknown" | "Unknown Deck" | "Modern event - MTGO League" |

### 2. 修复卡牌详情缓存问题
- 简化缓存检查逻辑
- 修复双面牌缓存未命中问题
- 优化套牌页面卡牌加载性能

### 3. 修复双面牌图片显示
- 调整图片优先级，中文图片优先
- 修复双面牌背面图片显示问题

### 4. 修复日期格式
- 统一日期格式为中文格式（YYYY年MM月DD日）
- 修复赛事日期格式转换

### 5. 修复套牌页面卡牌中文名称缺失问题
**问题：** 某些卡牌在套牌页面显示英文名，但在详情页显示中文名
**原因：** `cards` 表中有些记录的 `display_name` 字段为 NULL
**修复：**
- 添加 `updateDisplayNameByName()` 方法
- 添加 `getCardsWithNullDisplayName()` 和 `getCardsWithMissingData()` 查询方法
- 在应用启动和套牌详情加载时自动修复

**效果：**
- 所有套牌中的同名卡牌都会显示中文名称
- Subtlety 显示为"锐敏"，Meticulous Archive 显示为"整洁档案库"

### 6. 修复套牌页面法术力值不显示问题
**问题：** 某些卡牌（Subtlety, Force of Negation 等）不显示法术力值
**原因：** `cards` 表中有些记录的 `mana_cost` 字段为 NULL
**修复：**
- 添加 `updateCardDetails()` 同时更新 `display_name` 和 `mana_cost`
- 查询条件改为：`display_name IS NULL OR mana_cost IS NULL`

**效果：**
- Subtlety 现在显示法术力值 {2}{U}{U}
- Force of Negation 显示法术力值 {1}{U}{U}

### 7. 修复连体牌名称匹配问题
**问题：** 连体牌（如 Wear // Tear）在套牌页面不显示信息
**原因：** 名称格式不匹配（`Wear / Tear` vs `Wear // Tear`）
**修复：**
- 修复 `normalizeCardName()` 方法，正确处理所有连体牌格式
- 添加 `generateAlternativeNames()` 生成多种可能的名称变体

**效果：**
- Wear // Tear 成功显示：损耗 // 穿破 {1}{R}
- 所有连体牌都能正确匹配

### 8. 加深白色法术力符号颜色
**问题：** 白色法术力符号颜色 #F8F6D8 太浅，看不清
**修复：** 改为 #E8D8A0（更深的金黄色）
**效果：** 白色法术力符号现在更容易识别

### 9. 优化双面牌切换按钮文案
**问题：** 按钮文案切换显示（"查看反面"/"查看正面"）
**修复：** 统一使用固定文案"查看其他部分"
**效果：** 用户体验更一致，文案更简洁

---

## 🔧 技术改进

### 数据库升级
- 数据库版本升级到 10
- 添加双面牌背面字段（`backFacePower`, `backFaceToughness`, `backFaceLoyalty`）

### 代码优化
- 创建 `CardDetailHelper` 统一卡牌详情构建逻辑
- 移除重复代码（~120 行）
- 优化 API 并发控制（避免 429 错误）

### UI 改进
- 高级搜索改用底部表单（BottomSheetDialog）
- 优化筛选按钮状态显示（有筛选时显示"筛选*"）
- 添加清除按钮（法术力值、力量/防御力）
- 添加重置按钮（清空所有筛选）

---

## 📱 APK 文件

**文件名：** `decklist-manager-v4.1.0-release.apk`
**大小：** 7.0 MB
**位置：** `app/build/outputs/apk/release/decklist-manager-v4.1.0-release.apk`

**系统要求：**
- Android 5.0 (API 21) 或更高版本
- 目标 SDK：Android 14 (API 34)

---

## 🚀 安装说明

### 方法 1：直接安装
```bash
adb install decklist-manager-v4.1.0-release.apk
```

### 方法 2：通过 GitHub Release
1. 下载 `decklist-manager-v4.1.0-release.apk`
2. 在 Android 设备上打开文件管理器
3. 找到下载的 APK 文件并点击安装

### 方法 3：从 Google Play（未来）
- 计划在未来版本发布到 Google Play

---

## 📝 升级指南

### 从 v4.0.0 升级
1. **数据库升级**：自动从版本 9 升级到 10
2. **数据迁移**：所有现有数据保留
3. **无需手动操作**：首次启动自动完成升级

### 从更早版本升级
建议先卸载旧版本，然后安装 v4.1.0（数据将丢失）

---

## 🔄 Git 提交历史

```
c024360 - docs: 更新项目文档 - 修复 Unknown Deck 显示问题
b5419c9 - fix: 优化"Unknown Deck"显示逻辑
0bec2eb - feat: 完整修复双面牌功能 (v4.1.0)
4e454fa - feat: 添加连体牌支持和版本切换功能
2db92fc - fix: 修复卡牌详情显示和双面牌切换功能
bb2affa - fix: 优化卡牌详情缓存和显示问题
f35b986 - fix: 修复搜索和套牌页面的用户体验问题
377a489 - feat: 完整复制MTGCH高级搜索功能
921f04e - docs: 更新README至v4.1.0开发状态
741315b - feat: 重做高级搜索并优化套牌详情卡牌加载
```

---

## 🙏 致谢

感谢所有为这个项目做出贡献的用户！

---

## 📞 反馈与支持

### 问题反馈
如果您遇到任何问题，请在 GitHub 上提交 Issue：
https://github.com/kaorou-bot/decklist-android/issues

### 功能建议
欢迎提出新的功能建议和改进意见！

---

## 🎯 下个版本预告

### v4.1.5 - 深色模式优化（计划中）
- 实现完整的深色主题
- 优化夜间模式阅读体验
- 添加自动切换主题功能

### v4.2.0 - 套牌分析功能（规划中）
- 法术力曲线分析
- 颜色分布分析
- 价格估算（CardMarket API）
- 套牌相似度对比

---

**祝您游戏愉快！🎴**
