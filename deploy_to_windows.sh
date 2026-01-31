#!/bin/bash

# MTGO Decklist Manager - Windows 部署脚本
# 自动构建、安装、启动应用并查看日志

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目配置
PROJECT_DIR="/home/dministrator/decklist-android"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="com.mtgo.decklistmanager"
MAIN_ACTIVITY="com.mtgo.decklistmanager.ui.decklist.MainActivity"

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║   🚀 MTGO Decklist Manager - Windows 部署工具                ║"
echo "║                                                                ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# 检查是否在项目目录
if [ ! -f "$PROJECT_DIR/app/build.gradle" ]; then
    echo -e "${RED}❌ 错误：请在项目根目录运行此脚本${NC}"
    echo "   当前目录：$(pwd)"
    exit 1
fi

# 进入项目目录
cd "$PROJECT_DIR"

# ========================================
# 步骤 1：构建 APK
# ========================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}📦 步骤 1：构建 APK${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

./gradlew assembleDebug

if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}❌ 构建失败：找不到 APK 文件${NC}"
    echo "   期望路径：$APK_PATH"
    exit 1
fi

APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo -e "${GREEN}✅ 构建成功！${NC}"
echo -e "   📱 APK 大小：${APK_SIZE}"
echo -e "   📍 位置：$APK_PATH"
echo ""

# ========================================
# 步骤 2：检查 ADB 是否安装
# ========================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}🔍 步骤 2：检查 ADB${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if ! command -v adb &> /dev/null; then
    echo -e "${YELLOW}⚠️  ADB 未安装，正在安装...${NC}"
    sudo apt update
    sudo apt install -y android-tools-adb android-tools-fastboot
    echo -e "${GREEN}✅ ADB 安装完成${NC}"
else
    echo -e "${GREEN}✅ ADB 已安装${NC}"
fi

echo ""

# ========================================
# 步骤 3：启动 ADB 服务器
# ========================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}🔌 步骤 3：启动 ADB 服务${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

adb start-server
echo -e "${GREEN}✅ ADB 服务已启动${NC}"
echo ""

# ========================================
# 步骤 4：检测设备
# ========================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}📱 步骤 4：检测设备${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${YELLOW}🔍 正在搜索 Android 设备...${NC}"
echo ""

# 等待设备检测
sleep 2

DEVICES=$(adb devices | grep -v "List" | grep "device" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    echo -e "${RED}❌ 未检测到 Android 设备${NC}"
    echo ""
    echo -e "${YELLOW}💡 解决方案：${NC}"
    echo ""
    echo "1️⃣  如果使用真实设备："
    echo "   - 启用开发者模式"
    echo "   - 启用 USB 调试"
    echo "   - USB 连接到电脑"
    echo ""
    echo "2️⃣  如果使用 Windows Subsystem for Android (WSA)："
    echo "   - 在 Windows PowerShell 中运行："
    echo "     wsa"
    echo "   - 启用开发者模式"
    echo "   - 记下 WSA 的 IP 地址（通常在 127.0.0.1:58526）"
    echo "   - 然后运行："
    echo "     adb connect 127.0.0.1:58526"
    echo ""
    echo "3️⃣  如果使用 Android Studio Emulator："
    echo "   - 启动模拟器"
    echo "   - 确保设备在线"
    echo ""
    echo "4️⃣  查看设备列表："
    echo "   adb devices -l"
    echo ""
    exit 1
fi

echo -e "${GREEN}✅ 检测到 $DEVICES 个设备：${NC}"
echo ""
adb devices -l
echo ""

# ========================================
# 步骤 5：卸载旧版本（如果存在）
# ========================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE🗑️  步骤 5：卸载旧版本（如果存在）${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo -e "${YELLOW}⚠️  检测到旧版本，正在卸载...${NC}"
    adb uninstall "$PACKAGE_NAME"
    echo -e "${GREEN}✅ 旧版本已卸载${NC}"
else
    echo -e "${GREEN}✅ 未检测到旧版本，跳过卸载${NC}"
fi
echo ""

# ========================================
# 步骤 6：安装 APK
# ========================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📲 步骤 6：安装 APK${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${YELLOW}⏳ 正在安装...${NC}"
adb install -r "$APK_PATH"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 安装成功！${NC}"
else
    echo -e "${RED}❌ 安装失败${NC}"
    echo ""
    echo -e "${YELLOW}💡 可能的原因：${NC}"
    echo "   - 设备存储空间不足"
    echo "   - APK 签名问题"
    echo "   - 设备不兼容"
    echo ""
    echo "   查看详细错误：adb install -r \"$APK_PATH\""
    exit 1
fi
echo ""

# ========================================
# 步骤 7：启动应用
# ========================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🎯 步骤 7：启动应用${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

adb shell am start -n "$PACKAGE_NAME/$MAIN_ACTIVITY"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 应用已启动！${NC}"
else
    echo -e "${YELLOW}⚠️  启动命令已执行，请手动在设备上打开应用${NC}"
fi
echo ""

# ========================================
# 步骤 8：查看日志
# ========================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📋 步骤 8：查看日志（Ctrl+C 退出）${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}💡 提示：${NC}"
echo "   - 按 Ctrl+C 停止查看日志"
echo "   - 日志会实时显示应用输出"
echo "   - 查看特定标签：adb logcat | grep -E 'Decklist|MTGO'"
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 清除旧的日志缓存
adb logcat -c

# 实时查看日志
adb logcat -v time | grep -E "Decklist|MTGO|AndroidRuntime|System.err"

# ========================================
# 完成
# ========================================
echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                                                                ║${NC}"
echo -e "${GREEN}║   ✅ 部署完成！                                               ║${NC}"
echo -e "${GREEN}║                                                                ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
