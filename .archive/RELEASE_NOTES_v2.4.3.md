# 版本发布说明 - v2.4.3

## 📦 版本信息
- **版本号**: v2.4.3
- **版本代码**: 17
- **发布日期**: 2026-01-14
- **类型**: 🔧 改进 + 调试

---

## 🔧 改进内容

### MTGTop8 爬虫大幅改进 ✅

**问题**：
用户反馈 "仍无法下载任何卡组" - MTGTop8 爬虫无法成功下载牌组数据

**改进措施**：

#### 1. 详细日志输出
```kotlin
Log.d(TAG, "========== MTGTop8 Scraping Started ==========")
Log.d(TAG, "Format: $format")
Log.d(TAG, "Date filter: $date")
Log.d(TAG, "Max events: $maxEvents")
Log.d(TAG, "Fetching URL: $url")
Log.d(TAG, "Page fetched successfully, title: ${doc.title()}")
Log.d(TAG, "Page HTML length: ${doc.html().length}")
```

**用途**：
- 可以在 Logcat 中搜索 "MtgTop8Scraper" 查看完整日志
- 帮助诊断网络问题、HTML 解析问题
- 显示每一步的执行状态

#### 2. 更新 User-Agent
```kotlin
.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
.referrer("https://www.google.com")
.timeout(30000)
.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
.header("Accept-Language", "en-US,en;q=0.9")
```

**效果**：
- 模拟真实浏览器请求
- 避免 MTGTop8 反爬虫机制
- 添加 Referer 和其他 HTTP 头

#### 3. 多种 CSS 选择器策略
```kotlin
val possibleSelectors = listOf(
    "tr.hover_tr",           // 带有 hover_tr 类的行
    "tr[style*=\"hover\"]",  // 带 hover 样式的行
    "table.Stable tr",       // Stable 表格中的行
    "tr:has(td)",            // 包含 td 的行
    "tr"                     // 所有行（兜底）
)

for (selector in possibleSelectors) {
    val rows = doc.select(selector)
    Log.d(TAG, "Selector '$selector' found ${rows.size} rows")
    // 尝试解析...
}
```

**用途**：
- MTGTop8 网站结构可能变化
- 尝试多种选择器提高成功率
- 记录每个选择器找到的行数

#### 4. 改进数据提取逻辑
```kotlin
val cells = event.select("td")
if (cells.size < 3) {
    Log.v(TAG, "Skipping row with only ${cells.size} cells")
    continue
}

val col0 = cells[0].text().trim()
val col1 = cells[1].text().trim()
val col2 = if (cells.size > 2) cells[2].text().trim() else ""

Log.v(TAG, "Row data: [$col0] [$col1] [$col2]")

// 查找牌组链接
val links = cells[1].select("a")
for (link in links) {
    val href = link.attr("href")
    val linkText = link.text().trim()

    if (href.isNotEmpty() && (href.contains("deck") || href.contains("event"))) {
        Log.d(TAG, "Found link: $linkText -> $href")
        // 处理链接...
    }
}
```

**改进**：
- 验证单元格数量
- 记录每行数据
- 支持多种链接格式（deck?id=, deck?e=）
- 完整的 URL 处理

#### 5. 更好的错误处理
```kotlin
} catch (e: Exception) {
    Log.e(TAG, "========== MTGTop8 Scraping Failed ==========")
    Log.e(TAG, "Error: ${e.message}")
    Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
    e.printStackTrace()
}
```

---

## 📝 代码变更

### 修改的文件
```
✅ MtgTop8Scraper.kt
   ├── 添加详细的日志输出（30+ 新日志行）
   ├── 更新 User-Agent 为 Chrome 120
   ├── 添加多种 CSS 选择器策略
   ├── 改进 HTML 解析逻辑
   ├── 改进错误处理和日志记录
   └── 更好地处理 URL 构建和链接提取

✅ app/build.gradle
   └── 版本号更新到 v2.4.3 (versionCode: 17)
```

### 代码行数变化
```
+53 行（新日志和改进的解析逻辑）
-0 行
净变化：+53 行
```

---

## 🧪 测试指南

### 重要：需要查看 Logcat 日志来诊断问题！

#### 测试步骤

1. **安装 APK**
   ```bash
   adb install -r app/build/outputs/apk/debug/decklist-manager-v2.4.3-debug.apk
   ```

2. **打开 Logcat 窗口**
   ```bash
   adb logcat -s MtgTop8Scraper:D MainActivity:D MainViewModel:D
   ```

   或在 Android Studio 中：
   - 打开 Logcat 窗口
   - 过滤器输入：`MtgTop8Scraper`

3. **在应用中测试爬取**
   - 打开应用
   - 点击 "Web Scraping" 按钮
   - 选择格式（如 Modern）
   - 不选日期（或选择一个日期）
   - 设置最大牌组数为 5
   - 点击 "Start Scraping"

4. **查看 Logcat 输出**

   **如果成功**，应该看到：
   ```
   D/MtgTop8Scraper: ========== MTGTop8 Scraping Started ==========
   D/MtgTop8Scraper: Format: MO
   D/MtgTop8Scraper: Date filter: null
   D/MtgTop8Scraper: Max events: 5
   D/MtgTop8Scraper: Fetching URL: https://mtgtop8.com/format?f=MO
   D/MtgTop8Scraper: Page fetched successfully, title: Modern Top 8
   D/MtgTop8Scraper: Page HTML length: 45678
   D/MtgTop8Scraper: Selector 'tr.hover_tr' found 0 rows
   D/MtgTop8Scraper: Selector 'tr[style*="hover"]' found 0 rows
   D/MtgTop8Scraper: Selector 'table.Stable tr' found 50 rows
   D/MtgTop8Scraper: Found link: deck?e=12345 -> event
   D/MtgTop8Scraper: Row data: [01/14] [Event Name] [Modern]
   D/MtgTop8Scraper: Successfully parsed 5 decklists
   D/MtgTop8Scraper: ========== MTGTop8 Scraping Completed ==========
   D/MtgTop8Scraper: Total decklists found: 5
   ```

   **如果失败**，会看到：
   ```
   D/MtgTop8Scraper: ========== MTGTop8 Scraping Started ==========
   E/MtgTop8Scraper: ========== MTGTop8 Scraping Failed ==========
   E/MtgTop8Scraper: Error: Connection timeout
   E/MtgTop8Scraper: Error type: SocketTimeoutException
   ```

### 日志解读

| 日志内容 | 含义 |
|---------|------|
| `Page fetched successfully` | 成功获取页面 |
| `Page HTML length: 45678` | HTML 内容长度（非 0 表示成功） |
| `Selector 'xxx' found N rows` | 某个选择器找到了 N 行数据 |
| `Found link: xxx` | 找到牌组链接 |
| `Successfully parsed N decklists` | 成功解析 N 个牌组 |
| `No table rows found with any selector!` | 所有选择器都失败 |
| `Failed to fetch URL: xxx` | 网络请求失败 |

### 可能的结果

#### ✅ 成功场景
- 日志显示 "Successfully parsed N decklists"
- 应用显示 "Scraped N decklists from MTGTop8"
- 牌组列表中显示新下载的牌组

#### ❌ 失败场景 1：网络问题
```
E/MtgTop8Scraper: Failed to fetch URL: Failed to connect to mtgtop8.com
```
**解决**：检查网络连接，确保设备可以访问 mtgtop8.com

#### ❌ 失败场景 2：HTML 结构变化
```
D/MtgTop8Scraper: Selector 'tr.hover_tr' found 0 rows
D/MtgTop8Scraper: Selector 'tr[style*="hover"]' found 0 rows
...
D/MtgTop8Scraper: No table rows found with any selector!
```
**解决**：MTGTop8 网站结构已变化，需要手动检查 HTML 并更新选择器

#### ❌ 失败场景 3：解析失败
```
D/MtgTop8Scraper: Selector 'table.Stable tr' found 50 rows
D/MtgTop8Scraper: Row data: [] [] []
D/MtgTop8Scraper: Skipping row with only 0 cells
```
**解决**：HTML 表格结构与预期不符，需要调整解析逻辑

---

## 📦 APK 信息

### 文件详情
- 📱 **文件名**: `decklist-manager-v2.4.3-debug.apk`
- 📏 **大小**: 8.0 MB
- 📍 **位置**: `app/build/outputs/apk/debug/`
- 📦 **归档**: `apk-archive/decklist-manager-v2.4.3-debug.apk`

### 安装方式
```bash
# 通过 ADB 安装
adb install -r app/build/outputs/apk/debug/decklist-manager-v2.4.3-debug.apk

# 查看日志
adb logcat -s MtgTop8Scraper:D
```

---

## 🔄 从 v2.4.2 升级

### 升级步骤
1. 卸载旧版本（可选，可以直接覆盖安装）
2. 安装新的 v2.4.3 APK
3. 数据库保持不变

### 数据兼容性
- ✅ 数据库结构无变化
- ✅ 已有数据完全兼容
- ✅ 无需清空数据

---

## 📊 Git 提交

```
5b2dccf fix: 改进MTGTop8爬虫日志和解析逻辑 v2.4.3
```

---

## 🔮 下一步计划

根据测试结果，可能需要：

### 如果测试成功 ✅
- 保持当前实现
- 可以考虑添加更多功能

### 如果测试失败 ❌
需要根据 Logcat 日志分析：
1. **网络问题** → 添加重试机制、更长的超时时间
2. **HTML 结构变化** → 手动检查 MTGTop8.com 的 HTML 源码，更新选择器
3. **解析逻辑错误** → 调整数据提取逻辑

### 调试工具
如果需要手动检查 HTML 结构：
```bash
# 使用 curl 获取页面
curl -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" \
     https://mtgtop8.com/format?f=MO > mtgtop8.html

# 查看页面结构
cat mtgtop8.html | grep -A 5 "<tr"
```

---

## 💡 使用提示

### 测试 MTGTop8 爬虫的最佳实践

1. **从小数量开始**：
   - 首次测试设置最大牌组数为 3-5
   - 成功后再增加数量

2. **选择活跃格式**：
   - Modern (MO) 和 Standard (ST) 通常有最多数据
   - Legacy (LE) 和 Vintage (VI) 数据较少

3. **日期筛选**：
   - 不选日期：获取最新牌组
   - 选择日期：只获取匹配的牌组
   - 日期格式：YYYY-MM-DD

4. **网络要求**：
   - 确保设备有稳定的网络连接
   - MTGTop8.com 在某些地区可能访问缓慢

---

## 🎉 总结

v2.4.3 的主要改进：
- ✅ 添加了详细的日志输出（便于调试）
- ✅ 更新 User-Agent 模拟真实浏览器
- ✅ 实现多种 CSS 选择器策略（提高成功率）
- ✅ 改进 HTML 解析逻辑
- ✅ 更好的错误处理

**关键**：请务必查看 Logcat 日志来诊断下载失败的原因！
日志会显示爬虫的每一步执行情况，帮助定位问题。

---

**发布日期**: 2026-01-14
**上一版本**: v2.4.2
**下一版本**: 根据测试结果确定
