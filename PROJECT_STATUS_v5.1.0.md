# 项目状态 - v5.1.0

**日期**: 2025-02-19
**版本**: v5.1.0
**状态**: ✅ 完全迁移到自有服务器

---

## 📋 迁移完成状态

### 核心功能
- ✅ 搜索功能 - 完全迁移到 ServerApi
- ✅ 卡牌详情 - 支持中文字段
- ✅ 印刷版本查询 - 新 API 接口
- ✅ 双面牌支持 - cardFaces 数据提取
- ✅ Split 卡牌 - 格式化卡牌名称搜索
- ✅ 套牌加载 - DecklistRepository 使用 ServerApi

### API 迁移状态

| 模块 | MTGCH API | ServerApi | 状态 |
|------|-----------|-----------|------|
| SearchViewModel | ❌ 404 错误 | ✅ 正常 | 已迁移 |
| DecklistRepository | ❌ 不稳定 | ✅ 正常 | 已迁移 |
| DeckDetailViewModel | ⚠️ 混合使用 | ✅ 正常 | 已迁移 |
| 印刷版本查询 | ❌ 404 错误 | ✅ 正常 | 已迁移 |

---

## 🔧 技术改进

### 1. 中文字段优先级

所有卡牌数据现在优先使用中文字段：

```kotlin
// ServerMapper.kt
name = nameZh ?: name
typeLine = typeLineZh ?: typeLine
oracleText = oracleTextZh ?: oracleText
setName = setNameZh ?: setName
```

### 2. Split 卡牌处理

处理单斜杠到双斜杠的转换：

```kotlin
// formatCardNameForSearch()
Wear/Tear → Wear // Tear
```

### 3. 双面牌背面信息

从 cardFaces 提取背面数据：

```kotlin
val backFace = cardFaces?.getOrNull(1)
backFaceName = backFace?.nameZh ?: backFace?.name
backImageUri = backFace?.imageUris?.normal
```

### 4. 印刷版本 API

新增两个端点：

```kotlin
// 按 Oracle ID 查询
GET /api/cards/{oracleId}/printings?limit=100

// 按卡牌名称查询
GET /api/cards/printings?name={cardName}&limit=100
```

---

## 📁 关键文件修改

### 数据层
- `ServerDto.kt` - 添加 ServerCardFaceDto 和中文字段
- `ServerApi.kt` - 添加印刷版本接口
- `ServerMapper.kt` - 新建，映射 DTO 到领域模型
- `DecklistRepository.kt` - 使用 ServerApi，格式化卡牌名称

### UI 层
- `SearchViewModel.kt` - 完全迁移到 ServerApi
- `SearchActivity.kt` - 直接使用 ServerMapper
- `CardInfoFragment.kt` - 使用 CardInfoDto
- `PrintingSelectorDialog.kt` - 使用 CardInfoDto
- `DeckDetailViewModel.kt` - 添加 formatCardNameForSearch()

---

## 🐛 已修复问题

### 问题 1: 卡牌详情不显示中文
**症状**: 类型、规则文本、系列名称显示英文
**修复**: ServerMapper 优先读取 `typeLineZh`、`oracleTextZh`、`setNameZh`

### 问题 2: Split 卡牌无法搜索
**症状**: "Wear/Tear" 搜索返回空结果
**修复**: 添加 `formatCardNameForSearch()` 转换为 "Wear // Tear"

### 问题 3: 双面牌背面无信息
**症状**: 翻转到背面时无文字和图片
**修复**: 从 `cardFaces[1]` 提取背面数据

### 问题 4: 印刷版本功能失效
**症状**: MTGCH API 返回 404
**修复**: 实现 ServerApi 印刷版本接口

### 问题 5: 切换印刷版本后系列名称不变
**症状**: 切换到不同系列的版本时，仍显示原系列名称
**修复**: `switchToPrinting()` 中移除 `originalChineseSetName` 的保留逻辑，使用 `tempCardInfo.setName`

---

## 📊 测试清单

### 搜索功能
- [ ] 中文名称搜索
- [ ] 英文名称搜索
- [ ] 高级筛选（颜色、法术力、类型等）
- [ ] Split 卡牌搜索（Wear // Tear）

### 卡牌详情
- [ ] 中文字段显示（名称、类型、规则文本）
- [ ] 双面牌翻转
- [ ] 双面牌背面信息和图片
- [ ] Split 卡牌两部分同时显示

### 印刷版本
- [ ] 显示所有印刷版本
- [ ] 切换不同版本
- [ ] 保留中文语言偏好

### 套牌加载
- [ ] 套牌列表显示
- [ ] 卡牌详情显示
- [ ] 法术力值显示

---

## 🚀 下一步

### 短期
1. 用户测试验证所有功能
2. 根据反馈修复问题
3. 提交到 GitHub

### 中期
1. 移除 MTGCH API 相关代码
2. 清理未使用的依赖
3. 优化错误处理

### 长期
1. 添加离线缓存
2. 优化图片加载
3. 性能优化

---

## 📄 相关文档

- [CHANGELOG.md](./CHANGELOG.md) - 版本更新记录
- [SERVER_API_SPEC.md](./SERVER_API_SPEC.md) - 服务器 API 规范
- [API_DOCUMENTATION.md](./API_DOCUMENTATION.md) - 卡牌服务器 API
- [MIGRATION_v5.0_CORRECTED.md](./MIGRATION_v5.0_CORRECTED.md) - v5.0 架构文档
- [README.md](./README.md) - 项目说明

---

**当前状态**: 等待用户测试
**分支**: dev/v4.2.0
**提交**: 待测试通过后提交
