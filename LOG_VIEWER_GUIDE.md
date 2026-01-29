# 日志查看器使用指南

## 如何打开日志查看器

有两种方法可以查看应用日志：

### 方法1: 应用内日志查看器（推荐）

1. **打开应用**
2. **长按主界面右上角的"中文/English"按钮**（按住2秒）
3. 会自动打开日志查看器界面

### 方法2: Android Studio Logcat

1. 打开 Android Studio
2. 连接设备或启动模拟器
3. 点击底部的 "Logcat" 标签
4. 在过滤框中输入以下关键词：
   - `EventDetailViewModel` - 查看赛事详情相关日志
   - `DecklistRepository` - 查看卡牌搜索相关日志
   - `AppLogger` - 查看所有应用日志

## 日志查看器功能

### 刷新按钮
- 点击"刷新"按钮重新加载最新日志
- 每次刷新会显示最近500条相关日志

### 清除按钮
- 点击"清除日志"按钮清空所有日志
- 清除后可以重新执行操作，获得干净的日志输出

### 复制按钮
- 点击"复制"按钮将所有日志复制到剪贴板
- 可以粘贴到其他应用分享或保存

## 调试当前问题

### 问题1: 下载提示不显示

**重现步骤：**
1. 清除日志
2. 进入一个没有套牌的赛事页面
3. 刷新日志查看器
4. 查找包含 `shouldShowDownloadDialog` 的日志行

**期望日志：**
```
EventDetailViewModel: shouldShowDownloadDialog: true (hasShown: false, decklists: 0)
EventDetailActivity: Showing download dialog for empty event
```

**如果没有看到这些日志，说明：**
- 赛事页面可能已经有套牌数据
- 或者 `_decklists.value` 还是 null

### 问题2: 卡牌详情显示"not found"

**重现步骤：**
1. 清除日志
2. 点击任意卡牌查看详情
3. 刷新日志查看器
4. 查找包含 `getCardInfo` 的日志行

**期望日志：**
```
DecklistRepository: getCardInfo called for: [卡牌名称]
DecklistRepository: Not found in cache, fetching from API
DecklistRepository: API query: ![卡牌名称]
DecklistRepository: API response code: 200
DecklistRepository: Found [X] cards in response
```

**如果看到错误，可能是：**
- API 请求失败（检查网络连接）
- 卡牌名称格式不匹配
- mtgch.com API 返回空结果

## 分享日志

如果需要帮助调试：
1. 重现问题
2. 打开日志查看器
3. 点击"复制"按钮
4. 将日志粘贴到文本文件或聊天工具中分享

## 权限说明

应用已申请 `READ_LOGS` 权限，但在某些设备上可能无法读取日志。如果日志查看器显示"读取日志失败"，请使用 Android Studio 的 Logcat。

## 命令行查看日志（高级）

如果你熟悉命令行，也可以使用 adb 命令：

```bash
# 查看实时日志
adb logcat | grep -E "(EventDetailViewModel|DecklistRepository)"

# 查看最近的日志
adb logcat -d | grep -E "(EventDetailViewModel|DecklistRepository)" | tail -100

# 清除日志
adb logcat -c
```

## 常见日志关键词

- `EventDetailViewModel` - 赛事详情页面逻辑
- `DecklistRepository` - 数据仓库，处理卡牌搜索
- `MtgchApi` - API 调用相关
- `shouldShowDownloadDialog` - 下载提示判断
- `getCardInfo` - 卡牌信息获取
- `API response code` - API 响应状态码
- `Found in cache` - 从缓存中找到
- `Not found in cache` - 缓存中没有，需要从 API 获取
