# 数据库导入问题诊断与解决方案

## 🔍 问题：卡牌数据库导入失败

### ✅ 已实施的修复

#### 1. 修复 readLines() 问题
**问题**: `BufferedReader.readLines()` 会消耗整个流，导致后续无法读取

**解决方案**:
```kotlin
// ❌ 旧代码（错误）
val allLines = reader.readLines()
for (line in allLines) { ... }

// ✅ 新代码（正确）
val allText = reader.readText()
val allLines = allText.lines()
for (line in allLines) { ... }
```

#### 2. 跳过空行
```kotlin
for (line in allLines) {
    if (line.isBlank()) continue  // 跳过空行
    // 处理数据...
}
```

#### 3. 增强错误日志
```kotlin
} catch (e: Exception) {
    errorCount++
    Log.w(TAG, "导入第 $totalCards 张卡牌失败: ${e.message}")
    Log.d(TAG, "失败的数据预览: ${line.take(100)}...")
}
```

#### 4. 安全的进度更新
```kotlin
try {
    setProgressAsync(...)
} catch (e: Exception) {
    Log.w(TAG, "更新进度失败: ${e.message}")
}
```

---

## 🧪 如何测试数据库导入

### 测试步骤

#### 方法 1: 清除应用数据
```bash
adb shell pm clear com.mtgo.decklistmanager
adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity
```

#### 方法 2: 重装应用
```bash
adb uninstall com.mtgo.decklistmanager
adb install app/build/outputs/apk/debug/decklist-manager-v3.11.1-debug.apk
```

#### 方法 3: 清除数据库标记（如果应用已安装）
```kotlin
// 在 MainActivity.kt 的 checkOfflineDatabase() 中临时添加：
dbManager.clearDatabase()
// 重启应用
```

### 查看导入日志

```bash
# 实时查看日志
adb logcat | grep -E "CardDatabase|MainActivity"

# 过滤导入进度
adb logcat | grep "进度:"

# 查看错误
adb logcat | grep -E "导入失败|ERROR|WTF"
```

### 预期的日志输出

```
D/CardDatabaseDownload: 开始导入卡牌数据库
D/CardDatabaseDownload: 文件包含 66504 行, 大小: 150 MB
D/CardDatabaseDownload: 进度: 10% (6650/66504) - 3252 张/秒
D/CardDatabaseDownload: 进度: 20% (13300/66504) - 3180 张/秒
...
D/CardDatabaseDownload: ✅ 导入完成: 总计 66504 张, 成功 66504 张, 失败 0 张, 用时 23 秒
D/CardDatabaseDownload: 卡牌数据库导入完成
```

---

## 🐛 常见问题排查

### 问题 1: 导入进度卡在 0%
**症状**: 进度条不更新，一直显示 0%

**原因**: WorkManager 标签不匹配

**检查**:
```kotlin
// 在 CardDatabaseManager.kt 中确认：
companion object {
    const val WORK_TAG = "card_database_import"  // ✅ 标签一致
}

// 在 MainActivity.kt 中确认：
WorkManager.getInstance(this)
    .getWorkInfosByTagLiveData(CardDatabaseManager.WORK_TAG)  // ✅ 使用相同标签
```

**解决方案**:
- 确保 WORK_TAG 常量在两个地方一致
- 清除应用数据重试

---

### 问题 2: 导入失败，显示 Toast "导入失败"
**症状**: 导入过程出错，Toast 提示失败

**排查步骤**:
1. 查看 logcat 日志：
   ```bash
   adb logcat | grep -E "导入数据库失败|ERROR|WTF" | head -50
   ```

2. 检查 assets 文件：
   ```bash
   ls -lh app/src/main/assets/mtgch_cards.jsonl
   wc -l app/src/main/assets/mtgch_cards.jsonl
   ```

3. 验证 JSON 格式：
   ```bash
   head -1 app/src/main/assets/mtgch_cards.jsonl | python3 -m json.tool
   ```

---

### 问题 3: 进度条显示但进度不更新
**症状**: 进度对话框显示但一直是 0%

**原因**:
- setProgressAsync 调用失败
- WorkInfo.progress 未正确传递

**解决方案**:
```kotlin
// 在 importCardsFromReader 中确保：
try {
    setProgressAsync(
        workDataOf(
            KEY_PROGRESS to progress,
            KEY_CURRENT to totalCards,
            KEY_TOTAL to totalLines
        )
    )
} catch (e: Exception) {
    Log.w(TAG, "更新进度失败: ${e.message}")
}
```

---

### 问题 4: 部分卡牌无法找到
**症状**: 搜索某些卡牌显示 "not found"

**原因**: 可能是 JSON 解析失败

**排查**:
```bash
# 测试特定卡牌是否在数据库中
grep -i "\"name\": \"Solitude\"" app/src/main/assets/mtgch_cards.jsonl

# 统计总行数
wc -l app/src/main/assets/mtgch_cards.jsonl
```

**预期结果**:
- 总行数应该是 66,504（或接近）
- 应该找到 "Solitude"

---

## 📊 性能基准

### 导入性能指标

| 设备 | 时间 | 速度 |
|------|------|------|
| 模拟器 (ARM64) | ~20-30 秒 | ~3,000 张/秒 |
| 真机 (中端) | ~10-20 秒 | ~6,000 张/秒 |
| 真机 (高端) | ~5-10 秒 | ~10,000 张/秒 |

**测试数据**:
- 总卡牌数: 66,504
- 文件大小: ~150 MB (未压缩)
- APK 大小: 36 MB (压缩后)

---

## 🔧 调试技巧

### 1. 添加详细日志

在 `CardDatabaseDownloadWorker.kt` 中：

```kotlin
private const val TAG = "CardDatabaseDownload"
private const val DEBUG = true  // 开启调试模式

private fun debugLog(message: String) {
    if (DEBUG) {
        Log.d(TAG, message)
    }
}

// 使用
debugLog("当前处理: $totalCards")
```

### 2. 监控内存使用

```bash
# 查看内存使用情况
adb shell dumpsys meminfo com.mtgo.decklistManager
```

### 3. 检查数据库导入结果

```bash
# 连接到设备
adb shell

# 进入数据库
run-as com.mtgo.decklistManager
cd databases

# 使用 sqlite3 查询
sqlite3 mtg-decklist.db "SELECT COUNT(*) FROM card_info"
# 应该返回: 66504
```

---

## ✅ 验证清单

导入成功的标志：

- [ ] Toast 显示 "✅ 卡牌数据库导入完成！"
- [ ] 日志显示 "✅ 导入完成: 总计 66504 张"
- [ ] 失败数为 0
- [ ] 进度条达到 100%
- [ ] 搜索 "Solitude" 能找到
- [ ] 搜索 "Supreme Verdict" 能找到

---

## 🆘 如果还是失败

### 获取完整诊断信息

1. **导出完整日志**:
   ```bash
   adb logcat -d > full_log.txt
   ```

2. **检查应用数据目录**:
   ```bash
   adb shell run-as com.mtgo.decklistManager ls -la files/
   ```

3. **查看数据库状态**:
   ```bash
   adb shell run-as com.mtgo.decklistManager ls -la databases/
   ```

4. **查看 SharedPreferences**:
   ```bash
   adb shell run-as com.mtgo.decklistManager cat shared_prefs/card_database.xml
   ```

### 手动触发导入

在 MainActivity.kt 的 `onCreate()` 最后添加：

```kotlin
// 临时测试代码：强制重新导入
if (false) {  // 改为 true 启用
    Thread {
        Looper.prepare()
        val dbManager = CardDatabaseManager(this)
        dbManager.clearDatabase()
        dbManager.startDownload()
        Looper.loop()
    }.start()
}
```

---

## 📞 报告问题时请提供

1. 设备信息（品牌/型号/Android 版本）
2. 完整的 logcat 日志（特别是导入部分）
3. 失败步骤描述
4. 截图（如果可能）

---

**最后更新**: 2026-01-28
**版本**: v3.11.1 (build 71)
**状态**: 所有修复已提交 (commit 434dfa4)
