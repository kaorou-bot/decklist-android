# v4.2.1 测试清单

## 版本信息
- **版本号**: v4.2.1
- **构建时间**: 2026-02-06
- **APK**: `decklist-manager-v4.2.1-debug.apk`

## 新功能测试

### 1. 深色模式 (Dark Mode)

#### 测试步骤:
1. 打开应用
2. 点击右上角菜单 (⋮)
3. 选择 "Settings"
4. 在 "主题" 中选择不同的模式:
   - 跟随系统
   - 浅色模式
   - 深色模式

#### 预期结果:
- **浅色模式**: 所有界面使用浅色背景
- **深色模式**: 所有界面使用深色背景 (#1C1B1F), 文字颜色变浅
- **跟随系统**: 根据系统设置自动切换

#### 需要检查的界面:
- [ ] 主界面 (赛事列表/收藏列表)
- [ ] 赛事详情页 (EventDetailActivity)
- [ ] 套牌详情页 (DeckDetailActivity)
- [ ] 套牌分析页 (DeckAnalysisActivity)
- [ ] 搜索页 (SearchActivity)
- [ ] 设置页 (SettingsActivity)

---

### 2. 手势操作 - 滑动删除赛事

#### 测试步骤:
1. 在主界面 "赛事列表" 标签页
2. 向左或向右滑动任意赛事项
3. 确认删除对话框出现

#### 预期结果:
- ✓ 滑动时显示红色背景和 "删除" 文字
- ✓ 松手后显示确认对话框
- ✓ 确认后赛事被删除
- ✓ 取消后视图恢复原状

---

### 3. 收藏页面滚动修复

#### 问题描述:
v4.2.0 版本中收藏页面无法垂直滚动

#### 测试步骤:
1. 切换到 "我的收藏" 标签页
2. 尝试上下滚动列表
3. 尝试向左滑动 (不应该有任何反应)

#### 预期结果:
- ✓ 可以正常垂直滚动
- ✓ 左滑不会触发删除操作
- ✓ 滚动流畅,无卡顿

---

### 4. 收藏红心功能

#### 测试场景 4.1: 收藏列表页面的红心点击

**测试步骤:**
1. 切换到 "我的收藏" 标签页
2. 点击任意套牌右侧的红心图标

**预期结果:**
- ✓ 红心可以点击
- ✓ 点击后显示 Toast: "Removed from favorites"
- ✓ 套牌从列表中移除
- ✓ 红心图标从实心变为空心

#### 测试场景 4.2: 套牌详情页的红心同步

**测试步骤:**
1. 进入任意套牌详情页
2. 点击右上角的红心按钮 (收藏)
3. 点击返回按钮返回主界面
4. 再次进入同一个套牌详情页

**预期结果:**
- ✓ 第2步: 红心从空心变为实心,显示 "Added to favorites"
- ✓ 第3步: 正常返回
- ✓ 第4步: 红心仍然保持实心状态 (已同步)

**反向测试:**
1. 在套牌详情页点击红心 (取消收藏)
2. 返回主界面
3. 再次进入详情页

**预期结果:**
- ✓ 红心变为空心,显示 "Removed from favorites"
- ✓ 返回后再次进入,红心保持空心状态

#### 测试场景 4.3: 从收藏列表进入详情页

**测试步骤:**
1. 在 "我的收藏" 列表中点击套牌名称进入详情
2. 观察右上角红心状态

**预期结果:**
- ✓ 红心显示为实心 (已收藏状态)

#### 测试场景 4.4: 从赛事列表进入详情页

**测试步骤:**
1. 在赛事详情页中点击未收藏的套牌
2. 观察右上角红心状态

**预期结果:**
- ✓ 红心显示为空心 (未收藏状态)
- ✓ 点击后可以收藏

---

### 5. 日志验证

#### 查看收藏状态刷新日志:
```bash
adb logcat | grep "DeckDetailActivity"
```

**预期看到的日志:**
```
D DeckDetailActivity: refreshFavoriteStatus: wasFavorite=false, newFavoriteState=true
D DeckDetailActivity: refreshFavoriteStatus: wasFavorite=true, newFavoriteState=false
```

---

## 已知问题

无

---

## 性能优化测试 (待实施)

### 列表滚动性能
- [ ] 赛事列表流畅滚动 (100+ 项)
- [ ] 收藏列表流畅滚动 (100+ 项)
- [ ] 卡牌列表流畅滚动 (100+ 张)

### 图片加载优化
- [ ] 卡牌图片加载不阻塞UI
- [ ] 滚动时暂停图片加载
- [ ] 内存占用稳定

### 搜索防抖
- [ ] 搜索输入有 300ms 防抖
- [ ] 不输入时不触发搜索

---

## 回归测试

### 基本功能
- [ ] 下载赛事列表
- [ ] 查看赛事详情
- [ ] 下载套牌
- [ ] 查看套牌详情
- [ ] 导出套牌 (MTGO/Arena/Text)
- [ ] 分享到 Moxfield
- [ ] 复制到剪贴板
- [ ] 套牌分析
- [ ] 搜索功能
- [ ] 语言切换 (中文/English)

---

## 测试报告模板

**测试日期**: ___________
**测试人员**: ___________
**设备型号**: ___________
**Android 版本**: ___________

### 测试结果

| 测试项 | 通过 | 失败 | 备注 |
|--------|------|------|------|
| 深色模式 | ☐ | ☐ | |
| 滑动删除赛事 | ☐ | ☐ | |
| 收藏页面滚动 | ☐ | ☐ | |
| 收藏列表红心点击 | ☐ | ☐ | |
| 套牌详情页红心同步 | ☐ | ☐ | |
| 日志验证 | ☐ | ☐ | |

### 发现的问题:

1.

2.

3.

---

## 代码变更摘要

### 新增文件:
1. `app/src/main/res/values-night/colors.xml` - 深色模式颜色定义
2. `app/src/main/res/values-night/themes.xml` - 深色模式主题配置
3. `app/src/main/java/com/mtgo/decklistmanager/ui/settings/SettingsActivity.kt` - 设置页面
4. `app/src/main/res/layout/activity_settings.xml` - 设置页面布局
5. `app/src/main/res/xml/settings_preferences.xml` - 设置选项配置

### 修改文件:
1. `MainActivity.kt`
   - 添加设置菜单项
   - 修复 `getSwipeDirs()` 仅在赛事列表页启用滑动
   - 为 `DecklistAdapter` 添加 `onFavoriteClick` 回调

2. `DecklistAdapter.kt`
   - 添加 `onFavoriteClick` 参数
   - 在 `ViewHolder.bind()` 中显示收藏状态
   - 为红心图标添加点击事件

3. `MainViewModel.kt`
   - 将 `toggleFavorite()` 改为 suspend 函数并返回 Boolean
   - 添加 `isFavorite(decklistId)` 方法

4. `DeckDetailActivity.kt`
   - 添加 `onStart()` 生命周期方法
   - 修改 `onResume()` 调用 `refreshFavoriteStatus()`
   - 添加 `refreshFavoriteStatus()` 辅助方法
   - 添加调试日志

5. `item_decklist.xml`
   - 添加红心图标 `ImageView`

6. `app/build.gradle`
   - 版本更新至 4.2.1
   - 添加 androidx.preference 依赖

7. `strings.xml`
   - 添加设置相关字符串
   - 添加主题选项数组

8. `main_menu.xml`
   - 添加设置菜单项

9. `AndroidManifest.xml`
   - 注册 `SettingsActivity`

---

**重要提示**:
- 在测试红心功能时,注意观察 Logcat 中的调试日志
- 测试深色模式时,建议在不同系统设置下验证
- 测试滚动功能时,建议在有 20+ 项数据的列表上进行
