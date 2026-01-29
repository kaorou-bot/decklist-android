#!/usr/bin/env python3
"""
MTGCH 卡牌数据库下载脚本
下载所有卡牌数据并导出为 JSON 文件
"""

import json
import time
import requests
from pathlib import Path
from typing import List, Dict
import sys

# 配置
API_BASE = "https://mtgch.com/api/v1"
OUTPUT_FILE = "mtgch_cards.jsonl"
PROGRESS_FILE = "download_progress.txt"
BATCH_SIZE = 100  # 每页卡牌数量

def download_all_cards():
    """下载所有卡牌数据 - 使用字母'e'搜索（最常见的字母）"""
    all_cards = []

    # 获取总数 - 使用字母'e'获取所有卡牌
    print("🔍 获取卡牌总数...")
    response = requests.get(f"{API_BASE}/result", params={
        "q": "e",  # 使用'e'搜索（最常见的字母）
        "page_size": 1,
        "page": 1
    })

    if response.status_code != 200:
        print(f"❌ API 请求失败: {response.status_code}")
        return None

    data = response.json()
    total_count = data.get('count', 0)
    total_pages = (total_count + BATCH_SIZE - 1) // BATCH_SIZE

    print(f"📊 统计信息:")
    print(f"   总卡牌数: {total_count:,}")
    print(f"   总页数: {total_pages:,}")
    print(f"   每页大小: {BATCH_SIZE}")
    print(f"   预估时间: {total_pages * 0.2 / 60:.1f} 分钟")
    print()

    # 读取进度
    start_page = 1
    if Path(PROGRESS_FILE).exists():
        with open(PROGRESS_FILE, 'r') as f:
            start_page = int(f.read().strip()) + 1
            print(f"🔄 从第 {start_page} 页继续下载...")

    # 分页下载
    for page in range(start_page, total_pages + 1):
        try:
            print(f"⬇️  下载第 {page}/{total_pages} 页... ({page/total_pages*100:.1f}%)")

            response = requests.get(f"{API_BASE}/result", params={
                "q": "e",  # 使用'e'搜索
                "page_size": BATCH_SIZE,
                "page": page
            })

            if response.status_code != 200:
                print(f"❌ 第 {page} 页下载失败: {response.status_code}")
                continue

            data = response.json()
            cards = data.get('items', [])  # 使用 items 而不是 data

            if not cards:
                print(f"⚠️  第 {page} 页没有数据")
                continue

            # 追加到文件
            append_to_jsonl(cards)
            all_cards.extend(cards)

            # 保存进度
            with open(PROGRESS_FILE, 'w') as f:
                f.write(str(page))

            # 避免请求过快
            time.sleep(0.1)

        except Exception as e:
            print(f"❌ 第 {page} 页出错: {e}")
            # 保存进度以便恢复
            with open(PROGRESS_FILE, 'w') as f:
                f.write(str(page))
            continue

    print(f"\n✅ 下载完成！")
    print(f"   总计下载: {len(all_cards):,} 张卡牌")
    print(f"   输出文件: {OUTPUT_FILE}")

    # 清理进度文件
    if Path(PROGRESS_FILE).exists():
        Path(PROGRESS_FILE).unlink()

    return all_cards

def append_to_jsonl(cards: List[Dict]):
    """追加卡牌数据到 JSONL 文件"""
    with open(OUTPUT_FILE, 'a', encoding='utf-8') as f:
        for card in cards:
            # 移除图片 URL 以减小文件大小
            card_clean = clean_card_data(card)
            f.write(json.dumps(card_clean, ensure_ascii=False) + '\n')

def clean_card_data(card: Dict) -> Dict:
    """清理卡牌数据，移除不必要的字段"""
    # 保留的字段
    fields_to_keep = {
        # 基本信息
        'id', 'name', 'face_name', 'lang',
        'oracle_id', 'scryfall_uri',

        # 卡牌属性
        'mana_cost', 'cmc', 'type_line', 'oracle_text',
        'power', 'toughness', 'loyalty', 'defense',

        # 颜色和合法性
        'colors', 'color_identity', 'legalities',

        # 系列信息
        'set', 'set_name', 'collector_number', 'rarity',
        'artist', 'released_at',

        # 布局和双面牌
        'layout', 'card_faces', 'other_faces',

        # 中文信息
        'zhs_name', 'zhs_face_name', 'zhs_type_line', 'zhs_text',

        # 关键字
        'keywords',

        # 图片（只保留一个 normal 大小的 URL）
        'image_uris',
    }

    cleaned = {}
    for field in fields_to_keep:
        if field in card:
            cleaned[field] = card[field]

    return cleaned

def show_statistics(cards_file: str):
    """显示统计信息"""
    if not Path(cards_file).exists():
        print(f"❌ 文件不存在: {cards_file}")
        return

    print(f"\n📈 文件统计: {cards_file}")

    # 计算文件大小
    size_mb = Path(cards_file).stat().st_size / 1024 / 1024
    print(f"   文件大小: {size_mb:.1f} MB")

    # 计算卡牌数量
    with open(cards_file, 'r', encoding='utf-8') as f:
        count = sum(1 for _ in f)

    print(f"   卡牌数量: {count:,}")

    # 抽样检查
    print(f"\n🔍 抽样检查 (前5张卡牌):")
    with open(cards_file, 'r', encoding='utf-8') as f:
        for i, line in enumerate(f):
            if i >= 5:
                break
            card = json.loads(line)
            print(f"   [{i+1}] {card.get('name')} (set: {card.get('set')})")

if __name__ == "__main__":
    print("=" * 60)
    print("🎴 MTGCH 卡牌数据库下载器")
    print("=" * 60)
    print()

    # 检查是否已有下载文件
    if Path(OUTPUT_FILE).exists():
        print(f"⚠️  发现已存在的文件: {OUTPUT_FILE}")
        print("选项:")
        print("  1. 继续下载（跳过已下载的页）")
        print("  2. 重新下载（删除现有文件）")
        print("  3. 查看统计信息")
        print("  4. 退出")

        choice = input("\n请选择 (1-4): ").strip()

        if choice == "1":
            cards = download_all_cards()
            if cards:
                show_statistics(OUTPUT_FILE)
        elif choice == "2":
            Path(OUTPUT_FILE).unlink()
            print("🗑️  已删除旧文件")
            cards = download_all_cards()
            if cards:
                show_statistics(OUTPUT_FILE)
        elif choice == "3":
            show_statistics(OUTPUT_FILE)
        else:
            print("👋 再见！")
    else:
        cards = download_all_cards()
        if cards:
            show_statistics(OUTPUT_FILE)

    print("\n✨ 完成！")
