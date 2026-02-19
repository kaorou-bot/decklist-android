feat: v5.1.0 - 完全迁移到自有服务器 API

## 概述
完成 SearchViewModel 和印刷版本功能到自有服务器 API 的完整迁移，
移除对 MTGCH API 的所有依赖。

## 主要变更

### API 层
- ServerApi.kt: 添加印刷版本查询接口
  - GET /api/cards/{oracleId}/printings
  - GET /api/cards/printings?name={cardName}

### 数据层
- ServerDto.kt: 添加 ServerCardFaceDto 和中文字段
  - cardFaces: 双面牌卡牌面列表
  - typeLineZh, oracleTextZh, setNameZh: 中文字段

- ServerMapper.kt: 新建 DTO 到领域模型映射
  - toCardInfo(): CardInfoDto → CardInfo
  - 优先使用中文字段
  - 从 cardFaces 提取双面牌背面信息

- DecklistRepository.kt: 使用 ServerApi
  - fetchCardInfoFromApi(): 改用 serverApi.searchCard()
  - formatCardNameForSearch(): Split 卡牌名称格式化

- DeckDetailViewModel.kt: 添加卡牌名称格式化
  - formatCardNameForSearch(): "/" → " // "

### 搜索功能
- SearchViewModel.kt: 完全迁移到 ServerApi
  - 依赖注入: serverApi 替代 mtgchApi
  - search(): 使用 serverApi.searchCard()
  - getCardPrintings(): 查询印刷版本（新方法）
  - searchCardPrintingsByName(): 按名称查询印刷版本（新方法）
  - toSearchResultItem(): CardInfoDto → SearchResultItem

- SearchActivity.kt: 直接使用 ServerMapper
  - showCardDetail(): serverCard.toCardInfo()

### 卡牌详情
- CardInfoFragment.kt: 迁移到 CardInfoDto
  - printings: List<CardInfoDto> 替代 MtgchCardDto
  - loadPrintings(): 使用 searchViewModel.getCardPrintings()
  - switchToPrinting(): 使用 newCard.toCardInfo()
  - 添加 originalChineseSetName 保留用户语言偏好

- PrintingSelectorDialog.kt: 迁移到 CardInfoDto
  - printings: List<CardInfoDto> 替代 MtgchCardDto

## 修复的问题

1. ✅ 搜索功能失效（MTGCH API 404）
2. ✅ 卡牌详情不显示中文类型、规则文本、系列
3. ✅ Split 卡牌无法搜索（Wear/Tear）
4. ✅ 双面牌背面无信息和图片
5. ✅ 印刷版本功能失效
6. ✅ 套牌页面卡牌名称和法术力值不显示

## 技术改进

### 中文字段优先级
```kotlin
name = nameZh ?: name
typeLine = typeLineZh ?: typeLine
oracleText = oracleTextZh ?: oracleText
setName = setNameZh ?: setName
```

### Split 卡牌处理
```kotlin
// Wear/Tear → Wear // Tear
formatCardNameForSearch()
```

### 双面牌背面信息
```kotlin
val backFace = cardFaces?.getOrNull(1)
backFaceName = backFace?.nameZh ?: backFace?.name
backImageUri = backFace?.imageUris?.normal
```

## 文件变更清单

### 修改
- app/src/main/java/.../data/remote/api/ServerApi.kt
- app/src/main/java/.../data/remote/api/dto/ServerDto.kt
- app/src/main/java/.../data/repository/DecklistRepository.kt
- app/src/main/java/.../ui/carddetail/PrintingSelectorDialog.kt
- app/src/main/java/.../ui/decklist/CardInfoFragment.kt
- app/src/main/java/.../ui/decklist/DeckDetailViewModel.kt
- app/src/main/java/.../ui/search/SearchActivity.kt
- app/src/main/java/.../ui/search/SearchViewModel.kt

### 新增
- app/src/main/java/.../data/remote/api/dto/ServerMapper.kt

### 文档
- PROJECT_STATUS_v5.1.0.md: 项目状态文档
- CHANGELOG.md: 更新日志
- docs/archive/: 归档旧的修复报告

## 测试状态

- ✅ 编译成功
- ✅ 安装成功
- ⏳ 用户测试中

## 下一步

等待用户测试验证以下功能：
1. 搜索卡牌（中文名称）
2. 高级筛选
3. 查看卡牌详情
4. 查看印刷版本
5. 切换印刷版本
6. 双面牌正面和背面
7. Split 卡牌（Wear // Tear）

---

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
