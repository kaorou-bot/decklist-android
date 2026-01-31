#!/bin/bash

# WSA 连接助手 - 连接到 Windows Subsystem for Android

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║   🪟 Windows Subsystem for Android (WSA) 连接助手            ║"
echo "║                                                                ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# 检查 ADB
if ! command -v adb &> /dev/null; then
    echo -e "${RED}❌ ADB 未安装${NC}"
    echo "   请先运行：sudo apt install android-tools-adb"
    exit 1
fi

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}💡 WSA 连接说明：${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "在开始之前，请确保："
echo ""
echo "1️⃣  已安装 Windows Subsystem for Android"
echo "2️⃣  已启用 WSA 的开发者模式"
echo "3️⃣  已在 WSA 设置中启用「允许来自这台电脑的 USB 调试」"
echo ""
echo "如果还没设置，请按以下步骤操作："
echo ""
echo "   Windows PowerShell (管理员) →"
echo "   打开 WSA → Settings →"
echo "   → 开发者模式 → 开启"
echo "   → 允许 USB 调试 → 开启"
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 尝试常见的 WSA 端口
WSA_PORTS=("58526" "58525" "58524")

echo -e "${YELLOW}🔍 正在搜索 WSA...${NC}"
echo ""

FOUND=false
for PORT in "${WSA_PORTS[@]}"; do
    echo -n "   尝试连接 127.0.0.1:$PORT ... "
    if adb connect "127.0.0.1:$PORT" &> /dev/null; then
        echo -e "${GREEN}✅ 成功！${NC}"
        FOUND=true
        break
    else
        echo -e "${YELLOW}失败${NC}"
    fi
done

echo ""

if [ "$FOUND" = false ]; then
    echo -e "${RED}❌ 无法自动连接到 WSA${NC}"
    echo ""
    echo -e "${YELLOW}💡 手动连接步骤：${NC}"
    echo ""
    echo "1. 在 Windows 中打开 WSA"
    echo "2. 查看 WSA 的 IP 地址和端口"
    echo "3. 手动连接："
    echo ""
    echo "   adb connect <WSA_IP>:<PORT>"
    echo ""
    echo "   例如：adb connect 127.0.0.1:58526"
    echo ""
    echo "4. 验证连接："
    echo "   adb devices"
    echo ""
    exit 1
fi

# 显示已连接的设备
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ 已连接的设备：${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
adb devices -l
echo ""

# 测试连接
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🎯 测试连接...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

adb shell echo "✅ WSA 连接正常！"

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                                                                ║${NC}"
    echo -e "${GREEN}║   ✅ WSA 连接成功！                                           ║${NC}"
    echo -e "${GREEN}║                                                                ║${NC}"
    echo -e "${GREEN}║   现在可以运行部署脚本：                                      ║${NC}"
    echo -e "${GREEN}║   ./deploy_to_windows.sh                                     ║${NC}"
    echo -e "${GREEN}║                                                                ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
else
    echo -e "${RED}❌ 连接测试失败${NC}"
    exit 1
fi
