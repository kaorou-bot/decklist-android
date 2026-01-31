# 📚 持续开发文档索引

> Claude Code 持续开发的快速导航索引

---

## 🚀 每次会话必读（核心4文件）

### 1️⃣ 快速恢复
```bash
# 运行快速恢复脚本
./quick_resume.sh
```

### 2️⃣ 核心追踪文件
- **PROJECT_STATUS.md** 📍 - 整体进度状态
- **SESSION_LOG.md** 📝 - 会话日志记录
- **CURRENT_TASK.md** 🎯 - 当前任务详情
- **TASK_CHECKLIST.md** ✅ - 完整任务清单

---

## 📖 使用指南

### 如何使用这套系统？
👉 **CLAUDE_CONTINUATION_GUIDE.md** - 📖 **必读！**
- 完整的持续开发指南
- 每次会话的标准流程
- 常用提示词模板

---

## 🗺️ 开发规划

### 路线图和计划
- **DEVELOPMENT_ROADMAP.md** - 完整路线图（v4.1.0 ~ v5.0.0）
- **DEVELOPMENT_QUICKSTART.md** - 5分钟快速上手

### 快速参考
- **DEVELOPMENT_REFERENCE.md** - 快速参考指南
  - 核心文件位置
  - 常用代码模式
  - 调试技巧
  - 常见问题

---

## 📦 详细技术规范

### v4.1.0 - 导出与搜索
- **docs/V4.1.0_DEVELOPMENT_SPEC.md** (30KB)
  - 套牌导出功能完整代码
  - 卡牌搜索功能完整代码
  - UI 实现完整代码

### v4.2.0 - 套牌分析
- **docs/V4.2.0_DEVELOPMENT_SPEC.md** (31KB)
  - 法术力曲线分析完整代码
  - 颜色分布分析完整代码
  - 价格估算功能完整代码

---

## 🎯 按场景查找

### 场景 1：每次会话开始
```
1. 运行：./quick_resume.sh
2. 阅读：PROJECT_STATUS.md
3. 阅读：SESSION_LOG.md
4. 阅读：CURRENT_TASK.md
```

### 场景 2：实现功能
```
1. 查看：docs/V4.X.X_DEVELOPMENT_SPEC.md
2. 参考：DEVELOPMENT_REFERENCE.md
3. 勾选：TASK_CHECKLIST.md
```

### 场景 3：遇到问题
```
1. 查看：CURRENT_TASK.md 的问题记录
2. 参考：DEVELOPMENT_REFERENCE.md 的常见问题
3. 搜索：grep -r "关键词" *.md docs/
```

### 场景 4：准备发布
```
1. 查看：PROJECT_STATUS.md 的发布清单
2. 查看：TASK_CHECKLIST.md 的测试清单
3. 更新：CHANGELOG.md
```

---

## 📋 文档清单

### 进度追踪（4个）
- ✅ PROJECT_STATUS.md - 整体进度
- ✅ SESSION_LOG.md - 会话日志
- ✅ CURRENT_TASK.md - 当前任务
- ✅ TASK_CHECKLIST.md - 任务清单

### 规划文档（4个）
- ✅ DEVELOPMENT_ROADMAP.md - 路线图
- ✅ DEVELOPMENT_QUICKSTART.md - 快速上手
- ✅ DEVELOPMENT_REFERENCE.md - 快速参考
- ✅ CLAUDE_CONTINUATION_GUIDE.md - 使用指南

### 技术规范（2个）
- ✅ docs/V4.1.0_DEVELOPMENT_SPEC.md
- ✅ docs/V4.2.0_DEVELOPMENT_SPEC.md

### 工具脚本（1个）
- ✅ quick_resume.sh - 快速恢复脚本

---

## 🔍 快速搜索

### 常用关键词
```bash
# 搜索"进度"
grep "进度" PROJECT_STATUS.md

# 搜索"导出"
grep -r "导出" docs/

# 搜索"分析"
grep -r "分析" docs/

# 搜索所有 TODO
grep -r "TODO" *.md docs/
```

---

## 💡 使用技巧

### 技巧 1：记住核心3文件
每次会话必读：
1. PROJECT_STATUS.md
2. SESSION_LOG.md
3. CURRENT_TASK.md

### 技巧 2：使用快速恢复
```bash
./quick_resume.sh
```

### 技巧 3：善用提示词
复制 CLAUDE_CONTINUATION_GUIDE.md 中的提示词模板

### 技巧 4：保持更新
每次会话结束时更新所有追踪文件

---

## 📊 当前状态

**最后更新：** 2026-01-31
**当前版本：** v4.0.0 (online mode)
**开发分支：** dev/v4.1.0 (待创建)
**整体进度：** 0% [░░░░░░░░░░]

**当前任务：** 准备开始 v4.1.0 开发

---

## 🚀 快速命令

```bash
# 快速恢复
./quick_resume.sh

# 查看所有文档
ls -lh *.md docs/*.md | grep -E "(PROJECT|SESSION|CURRENT|TASK|ROADMAP|QUICKSTART|REFERENCE|GUIDE|SPEC)"

# 查看核心文档
cat PROJECT_STATUS.md SESSION_LOG.md CURRENT_TASK.md

# 查看开发规范
cat docs/V4.1.0_DEVELOPMENT_SPEC.md

# 搜索功能
grep -r "导出" docs/V4.1.0_DEVELOPMENT_SPEC.md
```

---

## 📞 获取帮助

### 如果不了解如何使用
```
请阅读 CLAUDE_CONTINUATION_GUIDE.md
```

### 如果找不到文档
```bash
ls -lh *.md docs/*.md
```

### 如果需要快速恢复
```bash
./quick_resume.sh
```

---

## ✅ 最佳实践

1. ✅ 每次会话开始：运行 `./quick_resume.sh`
2. ✅ 每次会话开始：读取核心3文件
3. ✅ 每次会话结束：更新所有追踪文件
4. ✅ 完成任务后：勾选 TASK_CHECKLIST.md
5. ✅ 遇到问题时：记录到 CURRENT_TASK.md

---

**创建时间：** 2026-01-31
**文档总数：** 11+ 个
**总大小：** 200+ KB

**祝你开发顺利！** 🎉
