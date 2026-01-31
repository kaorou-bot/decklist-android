#!/bin/bash

# 快速部署脚本 - Android Studio 模拟器
# 修改代码后运行此脚本快速测试

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PACKAGE_NAME="com.mtgo.decklistmanager"
APK_PATH=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║   ⚡ 快速部署 - Android Studio 模拟器                        ║"
echo "║                                                                ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# 检查设备
echo -e "${BLUE}🔍 检查模拟器状态...${NC}"
DEVICES=$(adb devices | grep -v "List" | grep "device" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    echo -e "${RED}❌ 未检测到模拟器或设备${NC}"
    echo ""
    echo -e "${YELLOW}💡 请先启动模拟器：${NC}"
    echo "   方式 1: 在 Android Studio 中启动"
    echo "   方式 2: emulator -avd <设备名> -gpu host &"
    echo ""
    exit 1
fi

echo -e "${GREEN}✅ 检测到 $DEVICES 个设备${NC}"
adb devices -l
echo ""

# 构建
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}📦 构建 APK...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

./gradlew assembleDebug

if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}❌ 构建失败${NC}"
    exit 1
fi

APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo -e "${GREEN}✅ 构建成功！APK 大小：${APK_SIZE}${NC}"
echo ""

# 安装
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}📲 安装到模拟器...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

adb install -r "$APK_PATH"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 安装失败${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 安装成功${NC}"
echo ""

# 启动应用
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}🚀 启动应用...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

adb shell am force-stop "$PACKAGE_NAME" 2>/dev/null || true
adb shell am start -n "$PACKAGE_NAME/.ui.decklist.MainActivity"

echo -e "${GREEN}✅ 应用已启动！${NC}"
echo ""

# 日志
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📋 实时日志 (Ctrl+C 退出)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 清除旧日志
adb logcat -c

# 显示日志
adb logcat -v time | grep -E "Decklist|MTGO|AndroidRuntime|FATAL"
