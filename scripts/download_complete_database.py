#!/usr/bin/env python3
"""
MTGCH 完整卡牌数据库下载脚本
使用所有字母搜索以确保覆盖所有卡牌
"""

import json
import time
import requests
from pathlib import Path
from typing import List, Dict, Set
import sys

# 配置
API_BASE = "https://mtgch.com/api/v1"
OUTPUT_FILE = "mtgch_cards.jsonl"
PROGRESS_FILE = "download_progress.txt"
BATCH_SIZE = 100  # 每页卡牌数量

# 使用所有字母搜索
SEARCH_QUERIES = list("abcdefghijklmnopqrstuvwxyz") + ["1", "2"]  # 数字搜索

def download_cards_for_query(query: str) -> List[Dict]:
    """下载指定搜索词的所有卡牌"""
    all_cards = []

    # 获取总数
    try:
        response = requests.get(f"{API_BASE}/result", params={
            "q": query,
            "page_size": 1,
            "page": 1
        }, timeout=30)

        if response.status_code != 200:
            print(f"  ❌ 搜索 '{query}' 失败: {response.status_code}")
            return all_cards

        data = response.json()
        total_count = data.get('count', 0)
        total_pages = (total_count + BATCH_SIZE - 1) // BATCH_SIZE

        if total_count == 0:
            return all_cards

        print(f"  📊 搜索 '{query}': {total_count:,} 张卡牌, {total_pages} 页")

        # 分页下载
        for page in range(1, total_pages + 1):
            try:
                response = requests.get(f"{API_BASE}/result", params={
                    "q": query,
                    "page_size": BATCH_SIZE,
                    "page": page
                }, timeout=30)

                if response.status_code != 200:
                    print(f"    ❌ 第 {page}/{total_pages} 页失败")
                    continue

                data = response.json()
                cards = data.get('items', [])

                if not cards:
                    break

                all_cards.extend(cards)

                # 显示进度
                if page % 5 == 0 or page == total_pages:
                    print(f"    ⬇️  {page}/{total_pages} 页 ({len(all_cards)}/{total_count} 张)")

                time.sleep(0.1)

            except Exception as e:
                print(f"    ❌ 第 {page} 页出错: {e}")
                continue

        return all_cards

    except Exception as e:
        print(f"  ❌ 搜索 '{query}' 出错: {e}")
        return all_cards

def download_all_cards():
    """使用所有字母搜索下载完整卡牌数据库"""
    all_cards = []
    seen_ids = set()  # 用于去重

    print("=" * 60)
    print("🎴 MTGCH 完整卡牌数据库下载器")
    print("=" * 60)
    print()

    # 检查进度文件
    start_index = 0
    if Path(PROGRESS_FILE).exists():
        with open(PROGRESS_FILE, 'r') as f:
            start_index = int(f.read().strip())
            print(f"🔄 从第 {start_index} 个搜索词继续...")
            print()

    # 对每个搜索词进行下载
    for i, query in enumerate(SEARCH_QUERIES[start_index:], start=start_index):
        print(f"[{i+1}/{len(SEARCH_QUERIES)}] 搜索 '{query}'...")

        cards = download_cards_for_query(query)

        # 去重并追加
        new_cards = [card for card in cards if card.get('id') not in seen_ids]
        for card in new_cards:
            seen_ids.add(card.get('id'))

        print(f"  ✨ 新增: {len(new_cards)} 张卡牌 (去重后)")

        # 追加到文件
        if new_cards:
            append_to_jsonl(new_cards)

        # 保存进度
        with open(PROGRESS_FILE, 'w') as f:
            f.write(str(i))

        all_cards.extend(new_cards)
        print()

    print("=" * 60)
    print(f"✅ 下载完成！")
    print(f"   总计下载: {len(all_cards):,} 张卡牌")
    print(f"   唯一卡牌: {len(seen_ids):,} 张")
    print(f"   输出文件: {OUTPUT_FILE}")
    print(f"   文件大小: {Path(OUTPUT_FILE).stat().st_size / 1024 / 1024:.1f} MB")
    print("=" * 60)

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

    # 检查特定卡牌
    print(f"\n🔍 检查特定卡牌:")
    test_cards = ["Solitude", "Supreme Verdict"]
    with open(cards_file, 'r', encoding='utf-8') as f:
        cards_data = [json.loads(line) for line in f]

    for test_name in test_cards:
        found = any(card.get('name', '').lower() == test_name.lower() for card in cards_data)
        status = "✓" if found else "✗"
        print(f"   {status} {test_name}")

if __name__ == "__main__":
    # 检查是否已有下载文件
    if Path(OUTPUT_FILE).exists():
        print(f"⚠️  发现已存在的文件: {OUTPUT_FILE}")
        print("选项:")
        print("  1. 继续下载（从上次的搜索词继续）")
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
