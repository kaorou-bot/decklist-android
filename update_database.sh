#!/bin/bash
###############################################################################
# MTGCH 数据库更新脚本
# 用途：从 MTGCH API 下载最新的卡牌数据库并更新到本地
###############################################################################

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目路径
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ASSETS_DIR="$PROJECT_ROOT/app/src/main/assets"
DB_FILE="$ASSETS_DIR/mtgch_cards.jsonl"
TEMP_FILE="/tmp/mtgch_cards_download.jsonl"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  MTGCH 数据库更新工具${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# 检查依赖工具
echo -e "${YELLOW}检查依赖工具...${NC}"

if ! command -v curl &> /dev/null; then
    echo -e "${RED}错误: 未找到 curl 命令${NC}"
    echo "请先安装 curl"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}警告: 未找到 jq 命令${NC}"
    echo "建议安装 jq 以验证数据格式: sudo apt-get install jq"
    echo "将继续运行但不验证 JSON 格式"
    HAS_JQ=false
else
    HAS_JQ=true
    echo -e "${GREEN}✓ curl 已安装${NC}"
    echo -e "${GREEN}✓ jq 已安装${NC}"
fi

echo ""
echo -e "${BLUE}MTGCH API 数据下载${NC}"
echo "------------------------"

# MTGCH API 端点（批量下载所有卡牌）
MTGCH_API_URL="https://mtgch.com/api/v1/cardlist"

echo -e "${YELLOW}正在从 MTGCH API 下载数据...${NC}"
echo "API 端点: $MTGCH_API_URL"
echo ""
echo "这可能需要几分钟，请稍候..."
echo ""

# 下载数据
if curl -s -L -o "$TEMP_FILE" "$MTGCH_API_URL" --progress-bar; then
    echo ""
    echo -e "${GREEN}✓ 数据下载完成${NC}"
else
    echo ""
    echo -e "${RED}✗ MTGCH API 下载失败${NC}"
    echo "可能的原因："
    echo "  1. 网络连接问题"
    echo "  2. MTGCH API 服务暂时不可用"
    echo "  3. API 端点已更改"
    echo ""
    echo "请稍后重试，或访问 https://mtgch.com 检查服务状态"
    exit 1
fi

# 检查下载的文件
if [ ! -f "$TEMP_FILE" ]; then
    echo -e "${RED}错误: 下载的文件不存在${NC}"
    exit 1
fi

# 获取文件信息
FILE_SIZE=$(du -h "$TEMP_FILE" | cut -f1)
LINE_COUNT=$(wc -l < "$TEMP_FILE")
echo ""
echo "文件信息:"
echo "  大小: $FILE_SIZE"
echo "  行数: $LINE_COUNT"

# 验证数据格式（如果安装了 jq）
if [ "$HAS_JQ" = true ]; then
    echo ""
    echo -e "${YELLOW}验证数据格式...${NC}"

    # 检查文件是否为空
    if [ ! -s "$TEMP_FILE" ]; then
        echo -e "${RED}✗ 错误: 下载的文件为空${NC}"
        rm -f "$TEMP_FILE"
        exit 1
    fi

    # 验证第一行是否是有效的 JSON
    FIRST_LINE=$(head -1 "$TEMP_FILE")
    if echo "$FIRST_LINE" | jq . > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 数据格式验证通过${NC}"

        # 显示第一张卡牌的信息作为示例
        CARD_NAME=$(echo "$FIRST_LINE" | jq -r '.name // "Unknown"')
        ZHS_NAME=$(echo "$FIRST_LINE" | jq -r '.zhs_name // .atomic_translated_name // "无官方翻译"')
        ATOMIC_NAME=$(echo "$FIRST_LINE" | jq -r '.atomic_translated_name // "无"')
        SET_NAME=$(echo "$FIRST_LINE" | jq -r '.set_name // "Unknown"')

        echo ""
        echo "示例卡牌:"
        echo "  英文名: $CARD_NAME"
        echo "  官方中文: $ZHS_NAME"
        if [ "$ATOMIC_NAME" != "无" ] && [ "$ATOMIC_NAME" != "$ZHS_NAME" ]; then
            echo "  AI 翻译: $ATOMIC_NAME"
        fi
        echo "  系列: $SET_NAME"
    else
        echo -e "${YELLOW}⚠ 警告: 数据格式验证失败，但将继续使用${NC}"
        echo "第一行内容预览:"
        echo "$FIRST_LINE" | head -c 300
        echo ""
    fi
else
    echo ""
    echo -e "${YELLOW}跳过数据格式验证（未安装 jq）${NC}"
fi

echo ""
echo -e "${BLUE}更新本地数据库${NC}"
echo "--------------------"

# 确保目标目录存在
if [ ! -d "$ASSETS_DIR" ]; then
    mkdir -p "$ASSETS_DIR"
    echo -e "${GREEN}✓ 创建 assets 目录${NC}"
fi

# 备份旧数据库
if [ -f "$DB_FILE" ]; then
    BACKUP_FILE="${DB_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
    cp "$DB_FILE" "$BACKUP_FILE"
    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "✓ 已备份旧数据库"
    echo "  备份位置: $BACKUP_FILE"
    echo "  备份大小: $BACKUP_SIZE"
else
    echo "首次安装数据库（无旧数据库需要备份）"
fi

# 复制新数据库
cp "$TEMP_FILE" "$DB_FILE"
NEW_DB_SIZE=$(du -h "$DB_FILE" | cut -f1)
echo ""
echo -e "${GREEN}✓ 数据库已更新${NC}"
echo "  新数据库大小: $NEW_DB_SIZE"
echo "  总卡牌数: $LINE_COUNT"

# 清理临时文件
rm -f "$TEMP_FILE"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  数据库更新成功！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}数据库位置:${NC} $DB_FILE"
echo -e "${BLUE}总卡牌数:${NC}   $LINE_COUNT"
echo ""
echo -e "${YELLOW}下一步操作:${NC}"
echo "1. 在 Android Studio 中重新编译项目"
echo "   或运行: ./gradlew assembleDebug"
echo ""
echo "2. 安装新版本到设备"
echo "   或运行: ./gradlew installDebug"
echo ""
echo "3. 清除应用数据以重新导入数据库"
echo "   adb shell pm clear com.mtgo.decklistmanager"
echo ""
echo -e "${BLUE}提示:${NC}"
echo "- 如需查看数据库内容，可以使用:"
echo "  head -20 $DB_FILE | jq"
echo ""
echo "- 如需搜索特定卡牌，可以使用:"
echo "  grep '\"name\": \"Solitude\"' $DB_FILE | jq"
echo ""
