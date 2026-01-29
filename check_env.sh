#!/bin/bash

# MTGO Decklist Manager - 项目状态检查脚本

echo "=================================================="
echo "MTGO Decklist Manager v2.0.0 - 环境检查"
echo "=================================================="
echo ""

# 检查 Java
echo "1. 检查 Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo "   ✅ Java 已安装: $JAVA_VERSION"
else
    echo "   ❌ Java 未安装"
    echo "   安装命令: sudo apt install openjdk-17-jdk"
fi

echo ""

# 检查 Android SDK
echo "2. 检查 Android SDK..."
if [ -n "$ANDROID_HOME" ]; then
    echo "   ✅ ANDROID_HOME: $ANDROID_HOME"
else
    echo "   ⚠️  ANDROID_HOME 环境变量未设置"
    echo "   请安装 Android SDK 或使用 Android Studio"
fi

echo ""

# 检查 Gradle
echo "3. 检查 Gradle..."
if command -v gradle &> /dev/null; then
    GRADLE_VERSION=$(gradle --version | grep "Gradle" | head -n 1)
    echo "   ✅ Gradle 已安装: $GRADLE_VERSION"
else
    echo "   ℹ️  Gradle 未安装（项目会自动下载）"
fi

echo ""

# 检查项目文件
echo "4. 检查项目文件..."
if [ -f "build.gradle" ]; then
    echo "   ✅ build.gradle"
fi
if [ -f "settings.gradle" ]; then
    echo "   ✅ settings.gradle"
fi
if [ -f "app/build.gradle" ]; then
    echo "   ✅ app/build.gradle"
fi
if [ -f "gradlew" ]; then
    echo "   ✅ gradlew (已创建)"
fi

echo ""
echo "=================================================="
echo "5. 项目统计"
echo "=================================================="

# 统计文件
KOTLIN_COUNT=$(find app/src/main/java -name "*.kt" 2>/dev/null | wc -l)
XML_COUNT=$(find app/src/main/res/layout -name "*.xml" 2>/dev/null | wc -l)
TOTAL_COUNT=$(find app/src/main/java -name "*.kt" -exec wc -l {} + 2>/dev/null | tail -n 1 | awk '{print $1}')

echo "Kotlin 文件: $KOTLIN_COUNT 个"
echo "XML 布局文件: $XML_COUNT 个"
echo "代码总行数: $TOTAL_COUNT 行"

echo ""
echo "=================================================="
echo "6. 构建建议"
echo "=================================================="
echo ""
echo "推荐方案：使用 Android Studio"
echo "-----------------------------------"
echo "1. 安装 Android Studio"
echo "   https://developer.android.com/studio"
echo ""
echo "2. 打开项目"
echo "   File -> Open -> 选择当前目录"
echo ""
echo "3. 等待自动同步"
echo "   Android Studio 会自动："
echo "   - 下载 Gradle"
echo "   - 下载依赖"
echo "   - 配置项目"
echo ""
echo "4. 构建运行"
echo "   点击 Run 按钮 (绿色三角形)"
echo ""

echo "替代方案：使用命令行"
echo "-----------------------------------"
echo "需要先安装："
echo "  - Android SDK"
echo "  - Android SDK Build-Tools"
echo "  - Android SDK Platform-Tools"
echo ""
echo "然后运行："
echo "  ./gradlew assembleDebug"
echo ""

echo "=================================================="
