#!/bin/bash

# MTGO Decklist Manager v4.1.0 推送脚本
# 用于手动推送到 GitHub 和创建 Release

set -e

echo "======================================"
echo "MTGO Decklist Manager v4.1.0 发布脚本"
echo "======================================"
echo ""

# 检查当前分支
CURRENT_BRANCH=$(git branch --show-current)
echo "当前分支: $CURRENT_BRANCH"

if [ "$CURRENT_BRANCH" != "dev/v4.1.0" ]; then
    echo "错误：请切换到 dev/v4.1.0 分支"
    echo "运行: git checkout dev/v4.1.0"
    exit 1
fi

echo ""
echo "步骤 1: 推送到 GitHub..."
echo "----------------------------"
git push origin dev/v4.1.0

echo ""
echo "步骤 2: 打标签..."
echo "----------------------------"
git tag -a v4.1.0 -m "Release v4.1.0

- 套牌导出功能（MTGO/Arena/文本/Moxfield）
- 在线卡牌搜索（完整复制 MTGCH 高级搜索）
- 双面牌完整支持
- 优化 UI 显示（修复 Unknown Deck 问题）
- 性能优化和 Bug 修复"

echo ""
echo "步骤 3: 推送标签..."
echo "----------------------------"
git push origin v4.1.0

echo ""
echo "======================================"
echo "✅ 推送完成！"
echo "======================================"
echo ""
echo "APK 文件位置:"
echo "  app/build/outputs/apk/release/decklist-manager-v4.1.0-release.apk"
echo ""
echo "下一步："
echo "1. 访问 GitHub 创建 Release:"
echo "   https://github.com/kaorou-bot/decklist-android/releases/new"
echo ""
echo "2. 选择标签: v4.1.0"
echo ""
echo "3. 上传 APK 文件:"
echo "   - Title: decklist-manager-v4.1.0-release.apk"
echo "   - Description: 见 RELEASE_NOTES_v4.1.0.md"
echo ""
echo "4. 发布 Release"
echo ""
