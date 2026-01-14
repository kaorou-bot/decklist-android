#!/bin/bash

# MTGO Decklist Manager - 自动构建脚本

set -e

echo "========================================"
echo "  MTGO Decklist Manager 自动构建"
echo "========================================"
echo ""

# 设置颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 项目目录
PROJECT_DIR="/home/dministrator/decklist-android"
cd "$PROJECT_DIR"

# 设置 Android SDK
export ANDROID_HOME=/home/dministrator/Android
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0

echo -e "${GREEN}✓${NC} 项目目录: $PROJECT_DIR"
echo -e "${GREEN}✓${NC} Android SDK: $ANDROID_HOME"
echo ""

# 检查必要文件
echo "检查项目文件..."
if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo -e "${GREEN}✓${NC} Gradle Wrapper JAR 已就绪"
else
    echo -e "${RED}✗${NC} Gradle Wrapper JAR 缺失"
    exit 1
fi

if [ -f "build.gradle" ]; then
    echo -e "${GREEN}✓${NC} build.gradle 存在"
fi

if [ -f "app/build.gradle" ]; then
    echo -e "${GREEN}✓${NC} app/build.gradle 存在"
fi

echo ""

# 检查 Java
echo "检查 Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | grep version | awk '{print $3}')
    echo -e "${GREEN}✓${NC} Java: $JAVA_VERSION"
else
    echo -e "${RED}✗${NC} Java 未安装"
    exit 1
fi

echo ""
echo "========================================"
echo "开始构建..."
echo "========================================"
echo ""

# 给 Gradle wrapper 添加执行权限
chmod +x gradlew 2>/dev/null || true

# 尝试构建
echo "运行: ./gradlew assembleDebug"
echo ""

if ./gradlew assembleDebug --stacktrace; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}         构建成功！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""

    # 显示 APK 信息
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        echo "APK 位置: $APK_PATH"
        echo "APK 大小: $APK_SIZE"
        echo ""
        echo "安装到设备:"
        echo "  adb install -r $APK_PATH"
        echo ""
    fi
else
    echo ""
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}         构建失败${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    echo "可能的原因:"
    echo "1. Android SDK 未完全安装"
    echo "2. 缺少构建工具"
    echo "3. 网络问题导致依赖下载失败"
    echo ""
    echo "建议使用 Android Studio 构建:"
    echo "  1. 下载: https://developer.android.com/studio"
    echo "  2. 打开项目: $PROJECT_DIR"
    echo "  3. 等待自动同步"
    echo "  4. 点击 Run 按钮"
    echo ""
    exit 1
fi
