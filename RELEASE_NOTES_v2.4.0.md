# 版本发布说明 - v2.4.0

## 📦 版本信息
- **版本号**: v2.4.0
- **版本代码**: 14
- **发布日期**: 2026-01-14
- **类型**: ✨ 重大功能更新

---

## 🆕 新功能

### 1. MTGTop8 数据源集成 ✨

**功能描述**：
- 添加 MTGTop8.com 作为新的牌组数据源
- 支持 MTGTop8 的大量比赛牌组数据
- 可选择不同的赛制格式

**支持的格式**：
- 🆕 **Modern** - 现代赛制
- 🆕 **Standard** - 标准赛制
- 🆕 **Pioneer** - 先驱赛制
- 🆕 **Legacy** - 薪传赛制
- 🆕 **Pauper** - 贫民赛制
- 🆕 **Vintage** - 古老赛制

**使用方式**：
1. 打开应用
2. 点击 "Web Scraping" 按钮
3. 选择数据源：**MTGTop8 (Recommended)**
4. 选择格式和最大牌组数
5. 点击 "Start Scraping"
6. 等待爬取完成

**优势**：
- ✅ 更大的牌组数据库
- ✅ 更多的比赛数据
- ✅ 无需选择日期
- ✅ 更快的爬取速度
- ✅ 自动集成 Scryfall 数据

---

## 🔧 技术改进

### 卡牌显示修复 v2.3.2
**问题**：之前版本中卡牌仍然堆叠显示
**解决方案**：
- 完全移除 RecyclerView
- 使用 LinearLayout 动态添加卡牌视图
- 100% 可靠的布局方式

**修改文件**：
- `activity_deck_detail.xml` - RecyclerView → LinearLayout
- `DeckDetailActivity.kt` - 重写卡牌列表逻辑

---

## 📝 技术变更

### 新增文件
```
✅ MtgTop8Scraper.kt
   - MTGTop8 爬虫实现
   - 支持 Jsoup HTML 解析
   - 自动处理 User-Agent

✅ MtgTop8DecklistDto.kt
   - MTGTop8 数据传输对象
   - 牌组链接 DTO
   - 牌组详情 DTO
```

### 修改文件
```
✅ DecklistRepository.kt
   - 新增 scrapeFromMtgTop8() 方法
   - 新增 saveMtgTop8DecklistData() 方法
   - 注入 MtgTop8Scraper

✅ MainActivity.kt
   - 更新爬取对话框
   - 添加数据源选择
   - 添加最大牌组数输入

✅ MainViewModel.kt
   - 新增 startMtgTop8Scraping() 方法
   - 保留原有 startScraping() 方法（Magic.gg）

✅ AppModule.kt
   - 添加 MtgTop8Scraper 依赖注入
```

---

## 🎯 功能对比

| 功能 | Magic.gg | MTGTop8 |
|------|----------|---------|
| 数据量 | 有限 | 大量 ✅ |
| 格式支持 | 有限 | 全面 ✅ |
| 日期要求 | 必需 | 不需要 ✅ |
| 爬取速度 | 慢 | 快 ✅ |
| 数据质量 | 官方 | 社区 |
| 推荐使用 | ❌ | ✅ |

---

## 📦 APK 信息

### 文件详情
- 📱 **文件名**: `decklist-manager-v2.4.0-debug.apk`
- 📏 **大小**: 8.0 MB
- 📍 **位置**: `app/build/outputs/apk/debug/`
- 📦 **归档**: `apk-archive/decklist-manager-v2.4.0-debug.apk`

### 安装方式
```bash
# 通过 ADB 安装
adb install -r app/build/outputs/apk/debug/decklist-manager-v2.4.0-debug.apk

# 或直接复制 APK 到手机安装
```

---

## 🔄 升级指南

### 从 v2.3.x 升级
1. 卸载旧版本（可选，可以直接覆盖安装）
2. 安装新的 v2.4.0 APK
3. 数据库会自动迁移（如需要）

### 数据兼容性
- ✅ 数据库结构无变化
- ✅ 已有数据完全兼容
- ✅ 无需清空数据
- ✅ 可以同时使用两个数据源

---

## 🧪 测试建议

### MTGTop8 爬取测试
1. **基本功能测试**：
   - 打开应用 → 点击 "Web Scraping"
   - 选择 "MTGTop8 (Recommended)"
   - 选择 "Modern" 格式
   - 设置最大牌组数为 5
   - 点击 "Start Scraping"
   - 等待完成

2. **预期结果**：
   - ✅ 显示进度对话框
   - ✅ 成功爬取 5 个牌组
   - ✅ 自动获取 Scryfall 数据
   - ✅ 牌组列表正确显示
   - ✅ 卡牌正确分行显示

3. **卡牌显示测试**：
   - 点击任意牌组
   - 确认主牌和备牌都正确分行显示
   - 点击卡牌名称查看详情

---

## 📊 Git 提交历史

```
8725114 feat: 添加 MTGTop8 数据源支持 v2.4.0
b9c8b1b fix: 彻底修复卡牌显示堆叠问题 v2.3.2
dc4c4f5 docs: add release notes for v2.3.1
```

---

## 🔮 已知问题

### MTGTop8 爬虫限制
- ⚠️ 网站结构可能随时变化
- ⚠️ 需要稳定的网络连接
- ⚠️ 爬取速度受网络限制

### 解决方案
- ✅ 实现了错误处理和重试机制
- ✅ 提供了详细日志
- ✅ 保留了 Magic.gg 作为备用

---

## 💡 使用提示

### 推荐设置
**Modern 格式**：
- 最大牌组数：10
- 爬取时间：约 30-60 秒

**Standard 格式**：
- 最大牌组数：15
- 爬取时间：约 45-90 秒

### 最佳实践
1. 首次使用建议先爬取少量牌组测试
2. 定期清空数据重新爬取获取最新牌组
3. 使用搜索功能快速找到目标牌组
4. 导出功能可分享牌组给朋友

---

## 📞 反馈

如果遇到任何问题，请通过以下方式反馈：
- GitHub Issues: https://github.com/kaorou-bot/decklist-android/issues
- Email: 496291727@qq.com

---

## 🎉 总结

v2.4.0 是一个重大功能更新版本，添加了 MTGTop8 数据源支持，大大扩展了可用的牌组数据。同时修复了卡牌显示问题，确保了良好的用户体验。

**推荐所有用户升级到此版本！**

---

## 🔗 参考资料

### MTGTop8 相关项目
- [MtgTop8Scraper](https://github.com/creepymooy1/MtgTop8Scraper) - Python 参考实现
- [mtgtop8-scrapper](https://github.com/kammradt/mtgtop8-scrapper) - 另一个实现
- [MTGTop8 官网](https://mtgtop8.com/) - 数据源网站

### 技术文档
- [Jsoup 文档](https://jsoup.org/) - HTML 解析库
- [Scryfall API](https://scryfall.com/docs/api) - 卡牌数据 API

---

**发布日期**: 2026-01-14
**下一版本**: v2.5.0 (计划中)
**路线图**: 深色模式、收藏功能、单元测试
