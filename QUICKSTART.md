# 快速继续开发指南

> 当您想继续开发 MTGO Decklist Manager 项目时，请阅读本文件

---

## 🚀 快速开始

### 步骤1：启动新的对话

在开始新对话时，**复制并粘贴以下内容**：

```
请阅读 PROJECT_STATUS.md、SESSION_LOG.md 和 CURRENT_TASK.md，然后继续完成当前任务。
```

---

### 步骤2：了解当前状态

Claude 会自动读取三个关键文件：
- **PROJECT_STATUS.md** - 项目整体进度和架构信息
- **SESSION_LOG.md** - 最近会话的详细记录
- **CURRENT_TASK.md** - 当前任务状态

然后 Claude 会告诉您：
- 当前版本是什么
- 刚完成了什么
- 是否有待办任务
- 下一步该做什么

---

### 步骤3：开始工作

根据当前状态，您可以：

#### 如果有未完成的任务
Claude 会自动继续完成当前任务

#### 如果没有待办任务（当前状态）
您可以：
1. 查看可选功能（在 PROJECT_STATUS.md 的"未来版本计划"部分）
2. 提出新功能需求
3. 报告 bug 让 Claude 修复
4. 请求代码审查或优化建议

---

## 📚 关键文档说明

### PROJECT_STATUS.md
**用途**: 了解项目整体状态
**包含内容**:
- 当前版本和分支
- 各版本完成情况（v4.1.0, v4.2.0, v4.2.1）
- 未来版本计划
- 技术栈和架构
- 开发指南
- 代码规范

### SESSION_LOG.md
**用途**: 了解最近做了什么
**包含内容**:
- 最新会话的详细记录
- 遇到的问题和解决方案
- 代码变更摘要
- 经验总结

### CURRENT_TASK.md
**用途**: 快速恢复当前任务
**包含内容**:
- 当前任务状态
- 待办事项清单
- 快速开始指南
- 问题诊断方法

---

## 📋 项目状态快速查询

### 当前版本
```
版本: v4.2.1
版本代码: 79
状态: 已发布
分支: dev/v4.2.0
```

### 最近完成的功能
- ✅ 深色模式支持
- ✅ 手势操作（滑动删除赛事）
- ✅ 收藏功能完善
- ✅ Release 版本优化

### 待办任务
**无待办任务** - v4.2.1 已完成

---

## 🔧 常用命令

### 构建和运行
```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 安装到设备
adb install -r app/build/outputs/apk/debug/*.apk

# 启动应用
adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity
```

### 查看日志
```bash
# 查看所有日志
adb logcat

# 查看特定标签
adb logcat | grep "DeckDetailActivity"

# 清空日志
adb logcat -c
```

### Git 操作
```bash
# 查看最近提交
git log --oneline -10

# 查看当前状态
git status

# 查看当前分支
git branch

# 切换分支
git checkout <branch-name>
```

---

## 💡 开发提示

### 添加新功能时
1. 先在 PROJECT_STATUS.md 中创建新版本计划
2. 创建对应的 Activity/Fragment/ViewModel
3. 在 AndroidManifest.xml 中注册 Activity
4. 更新 strings.xml 添加文本资源
5. 测试功能
6. 更新版本号
7. 更新 PROJECT_STATUS.md 进度
8. 提交代码

### 发布新版本时
1. 更新版本号（app/build.gradle）
2. 测试所有功能
3. 构建 release 版本
4. 复制 APK 到 releases/ 目录
5. 创建发布文档
6. 更新 PROJECT_STATUS.md
7. 提交代码

---

## 🆘 遇到问题

### 构建失败
```bash
./gradlew clean
./gradlew assembleDebug --stacktrace
```

### 应用崩溃
```bash
adb logcat | grep -E "AndroidRuntime|FATAL"
```

### 找不到文件
使用 `find` 或 `grep` 搜索：
```bash
# 查找文件
find . -name "*.kt" | grep -i "deck"

# 搜索代码
grep -r "toggleFavorite" --include="*.kt"
```

---

## 📞 获取帮助

如果上述方法都无法解决问题：
1. 查看 PROJECT_STATUS.md 的"架构信息"部分
2. 查看 SESSION_LOG.md 的"经验总结"部分
3. 在对话中描述问题，Claude 会帮助您

---

**最后更新：2026-02-06 23:25**

---

## 📝 快速开始（模板）

复制以下内容到新对话：

```
请阅读 PROJECT_STATUS.md、SESSION_LOG.md 和 CURRENT_TASK.md，然后继续完成当前任务。
```

就这么简单！🎉
