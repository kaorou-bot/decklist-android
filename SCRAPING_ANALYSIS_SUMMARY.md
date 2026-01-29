# MTGO 牌组爬取功能分析报告

**日期**: 2026-01-13  
**版本**: v1.0  
**状态**: ✅ 分析完成

---

## 📋 执行摘要

经过全面分析，MTGO官网 (https://www.mtgo.com/decklists) 使用JavaScript动态渲染内容，**无法使用Jsoup爬取**。建议采用**Magic.gg**作为替代数据源。

---

## 🔍 数据源分析结果

### 1. MTGO官网 (mtgo.com)
- **URL**: https://www.mtgo.com/decklists
- **状态**: ❌ 不可爬取
- **原因**: 
  - 使用JavaScript动态渲染内容
  - 无公开REST API
  - Jsoup只能获取静态HTML骨架
- **数据质量**: ⭐⭐⭐⭐⭐ (最新最全)
- **技术难度**: 🔴 高（需要Selenium）

### 2. Magic.gg
- **URL**: https://magic.gg/decklists
- **状态**: ✅ **可爬取 - 推荐**
- **数据源**:
  - MTGO Champions Showcase Season 1-3 (2025)
  - 每赛季8个Modern牌组
  - 其他官方赛事
- **优势**:
  - 静态HTML，可用Jsoup
  - 无反爬虫保护
  - 官方数据，质量高
- **限制**: 仅限特定赛事，不是完整的MTGO联赛数据
- **技术难度**: 🟢 低

### 3. MTGDecks.net
- **URL**: https://mtgdecks.net/
- **状态**: ❌ 有Cloudflare保护
- **原因**: 
  - Cloudflare JavaScript挑战
  - 需要Selenium或bypass服务
  - APK体积增加30-50MB
- **数据质量**: ⭐⭐⭐⭐ (丰富但难获取)
- **技术难度**: 🔴 高

### 4. Spicerack API
- **URL**: https://api.spicerack.gg/v1/decklists
- **状态**: ❌ 404错误
- **原因**: 可能需要认证或不同路径
- **技术难度**: 🟡 中（需要获取API文档）

### 5. Scryfall API
- **URL**: https://api.scryfall.com/
- **状态**: ✅ 可用
- **用途**: 卡牌数据和图片
- **限制**: 不是完整牌组数据
- **技术难度**: 🟢 低

---

## 💡 推荐方案

### ⭐ 最佳方案：Magic.gg 爬虫

**优势**:
1. ✅ 技术实现简单（Jsoup即可）
2. ✅ 不增加APK体积
3. ✅ 官方赛事数据，质量高
4. ✅ 无反爬虫保护

**数据内容**:
- MTGO Champions Showcase 2025 Season 1-3
- 每赛季8个Modern牌组
- 可扩展到其他赛事

**实现示例**:
```kotlin
suspend fun scrapeMagicShowcase(): List<Decklist> {
    val url = "https://magic.gg/decklists/2025-magic-online-champions-showcase-season-3-modern-decklists"
    val doc = Jsoup.connect(url).get()
    // 解析HTML获取牌组数据
    // ...
}
```

---

## 📊 方案对比

| 方案 | 技术难度 | APK增加 | 数据质量 | 数据量 | 推荐度 |
|------|---------|---------|---------|--------|--------|
| **Magic.gg** | 🟢 低 | 0MB | ⭐⭐⭐⭐ | 中等 | ⭐⭐⭐⭐⭐ |
| MTGO官网+Selenium | 🔴 高 | 30-50MB | ⭐⭐⭐⭐⭐ | 全部 | ⭐⭐ |
| MTGDecks.net | 🔴 高 | 30-50MB | ⭐⭐⭐⭐ | 全部 | ⭐ |
| Spicerack API | 🟡 中 | 0MB | ⭐⭐⭐⭐⭐ | 未知 | ⭐⭐⭐ |
| 模拟数据 | 🟢 低 | 0MB | ⭐⭐⭐ | 有限 | ⭐⭐⭐ |

---

## 🎯 实施计划

### 阶段1：立即实施（保持现状）
- [x] 使用模拟数据降级策略
- [x] 定期更新模拟数据
- [ ] **添加手动导入功能**（新功能）
  - 支持粘贴牌组文本
  - 支持 .txt / .dek 文件导入

### 阶段2：下一个版本（推荐）
- [ ] 实现 Magic.gg 爬虫
- [ ] 添加数据源选择功能
  - 模拟数据
  - Magic.gg
  - 混合模式
- [ ] 实现增量更新

### 阶段3：长期目标
- [ ] 研究 Spicerack API
- [ ] 考虑 MTGO官网无头浏览器方案
- [ ] 构建用户社区

---

## 📝 技术参考

### 实现Magic.gg爬虫的关键代码
```kotlin
// MtgoScraper.kt
suspend fun fetchMagicShowcaseDecklists(): List<MtgoDecklistLinkDto> {
    return try {
        val url = "https://magic.gg/decklists/2025-magic-online-champions-showcase-season-3-modern-decklists"
        val doc = Jsoup.connect(url)
            .timeout(30000)
            .userAgent("Mozilla/5.0 (Android 13; Mobile)")
            .get()
        
        // 解析牌组链接
        parseMagicDecklistLinks(doc)
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
```

### 支持的数据格式
- MTGO格式：`.dek` 文件
- 文本格式：`.txt` 文件（Magic.gg导出）
- JSON格式：自定义结构

---

## 🔗 相关链接

- [MTGO官网](https://www.mtgo.com/decklists)
- [Magic.gg Decklists](https://magic.gg/decklists)
- [MTGDecks.net](https://mtgdecks.net/) (有Cloudflare保护)
- [Spicerack API文档](https://docs.spicerack.gg/api-reference/public-decklist-database)
- [Scryfall API](https://scryfall.com/docs/api)

---

## 📌 结论

**当前爬取功能无法工作的根本原因**：
- MTGO官网使用JavaScript动态渲染
- Jsoup无法获取动态加载的数据

**最佳解决方案**：
- ⭐ 实现 Magic.gg 爬虫（推荐）
- 保持模拟数据作为备用
- 添加手动导入功能

**不推荐的方案**：
- ❌ MTGDecks.net（Cloudflare保护，需要Selenium）
- ❌ MTGO官网+Selenium（APK体积增加30-50MB）

---

**报告生成时间**: 2026-01-13  
**下次更新**: 实施Magic.gg爬虫后
