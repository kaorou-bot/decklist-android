# MTG 套牌管理器 - v3.11.0 更新总结

## 更新日期
2026-01-28

## 主要改进

### 1. 完整卡牌数据库 ✅
- **卡牌数量**: 66,504 张（之前 25,578 张）
- **文件大小**: 158 MB（压缩后 APK 约 45 MB）
- **下载方法**: 使用所有字母 (a-z) 和数字 (0-9) 搜索，确保覆盖所有卡牌
- **验证**:
  - ✓ Solitude - 已找到
  - ✓ Supreme Verdict - 已找到
- **数据库位置**: `app/src/main/assets/mtgch_cards.jsonl`

### 2. UI 改进 ✅
- **语言切换**: 从按钮移至菜单（右上角菜单 → 语言）
- **按钮文本**:
  - "赛制" / "日期"（不再显示"筛选赛制"/"筛选日期"）
  - 选中时显示 "赛制: Modern" / "日期: 2026-01-15"
- **按钮样式**: 统一高度 48dp
- **移除冗余元素**: 删除了主界面的语言切换按钮

### 3. 数据库导入进度条 ✅
- **实时进度显示**:
  - 百分比进度（0-100%）
  - 已导入/总卡牌数
  - 导入速度（张/秒）
- **进度对话框**: 使用 Material 3 LinearProgressIndicator
- **WorkManager 集成**: 通过标签 `card_database_import` 监听进度
- **完成提示**: 导入完成后显示 Toast 提示

### 4. 卡牌查询优化 ✅
- **精确匹配**: 仅使用中文名或英文名精确匹配
- **移除模糊搜索**: 避免匹配错误的卡牌
- **日志完善**: 详细记录查询过程和结果

## 技术细节

### 数据库下载脚本
- **文件**: `scripts/download_full_noninteractive.py`
- **搜索策略**:
  - 26 个字母: a-z
  - 10 个数字: 0-9
  - 总共 36 个搜索查询
- **去重**: 使用卡牌 ID 去重
- **数据清理**: 仅保留必要字段，减小文件大小

### 进度报告机制
```kotlin
// Worker 中报告进度
setProgressAsync(
    workDataOf(
        KEY_PROGRESS to progressPercent,
        KEY_CURRENT to currentCard,
        KEY_TOTAL to totalCards
    )
)

// MainActivity 中监听进度
WorkManager.getInstance(context)
    .getWorkInfosByTagLiveData(CardDatabaseManager.WORK_TAG)
    .observe(this) { workInfos ->
        // 更新 UI
    }
```

## 文件变更

### 新增文件
- `scripts/download_full_noninteractive.py` - 完整数据库下载脚本
- `app/src/main/res/layout/dialog_database_import_progress.xml` - 进度对话框布局

### 修改文件
- `app/src/main/assets/mtgch_cards.jsonl` - 完整数据库（66,504 张）
- `app/src/main/java/.../CardDatabaseDownloadWorker.kt` - 添加进度报告
- `app/src/main/java/.../CardDatabaseManager.kt` - 添加 WORK_TAG
- `app/src/main/java/.../MainActivity.kt` - 进度监听和UI更新
- `app/src/main/java/.../DecklistRepository.kt` - 精确搜索

## APK 信息
- **版本**: v3.11.0 (versionCode: 70)
- **文件**: `app/build/outputs/apk/debug/decklist-manager-v3.11.0-debug.apk`
- **大小**: 45 MB
- **最小 SDK**: 24 (Android 7.0)
- **目标 SDK**: 34 (Android 14)

## 已知问题
- 无重大问题

## 后续计划
1. 实现日期分组赛事列表（Date → Event → Deck 三级结构）
2. 优化下载错误套牌的验证
3. 进一步优化性能

## 测试建议
1. **首次安装**: 验证数据库导入对话框显示
2. **导入进度**: 观察进度条和百分比更新
3. **卡牌查询**:
   - 搜索 "Solitude" - 应找到
   - 搜索 "Supreme Verdict" - 应找到
   - 搜索 "Fury" - 应找到
4. **语言切换**: 菜单 → 语言，验证切换成功
5. **按钮功能**: 验证赛制和日期筛选正常

---

**构建命令**:
```bash
./gradlew assembleDebug
```

**输出位置**:
```
app/build/outputs/apk/debug/decklist-manager-v3.11.0-debug.apk
```
