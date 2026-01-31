#!/bin/bash

# MTGO Decklist Manager - 快速恢复开发脚本
# 使用方法：./quick_resume.sh

echo "=========================================="
echo "  MTGO Decklist Manager - 快速恢复"
echo "=========================================="
echo ""

# 检查是否在项目根目录
if [ ! -f "PROJECT_STATUS.md" ]; then
    echo "❌ 错误：请在项目根目录运行此脚本"
    echo "   当前目录：$(pwd)"
    exit 1
fi

echo "✅ 已找到项目文件"
echo ""

# 显示当前状态
echo "📊 当前开发状态："
echo "----------------------------------------"
cat PROJECT_STATUS.md | grep -A 20 "## 📊 当前开发状态"
echo ""

# 显示当前任务
echo "🎯 当前任务："
echo "----------------------------------------"
cat CURRENT_TASK.md | grep -A 30 "## 🎯 当前任务"
echo ""

# 显示上次会话信息
echo "📅 上次会话："
echo "----------------------------------------"
tail -50 SESSION_LOG.md | grep -A 10 "## 📅 会话"
echo ""

# 显示快速恢复命令
echo "=========================================="
echo "  🚀 快速恢复 Claude Code 开发"
echo "=========================================="
echo ""
echo "复制以下命令给 Claude："
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "请帮我快速了解项目状态并继续开发："
echo ""
echo "1. 请阅读 PROJECT_STATUS.md 了解整体进度"
echo "2. 请阅读 SESSION_LOG.md 了解上次会话做了什么"
echo "3. 请阅读 CURRENT_TASK.md 了解当前正在做的任务"
echo "4. 继续完成当前任务"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 显示Git状态
echo "📁 Git 状态："
echo "----------------------------------------"
git branch
echo ""
git status --short
echo ""

# 提供操作选项
echo "=========================================="
echo "  💡 下一步操作"
echo "=========================================="
echo ""
echo "1. 如果要开始新任务："
echo "   git checkout dev/v4.1.0"
echo ""
echo "2. 如果要查看详细规范："
echo "   cat docs/V4.1.0_DEVELOPMENT_SPEC.md"
echo ""
echo "3. 如果要查看任务清单："
echo "   cat TASK_CHECKLIST.md"
echo ""
echo "4. 如果要开始编码："
echo "   打开 Claude Code，使用上面的快速恢复命令"
echo ""
echo "=========================================="
