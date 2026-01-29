#!/usr/bin/env python3
"""
MTGCH 完整卡牌数据库下载脚本（非交互式）
使用所有字母和数字搜索以确保覆盖所有卡牌
"""

import json
import time
import requests
from pathlib import Path
from typing import List, Dict, Set

# 配置
API_BASE = "https://mtgch.com/api/v1"
OUTPUT_FILE = "mtgch_cards.jsonl"
BATCH_SIZE = 100

# 使用所有字母和数字搜索
SEARCH_QUERIES = list("abcdefghijklmnopqrstuvwxyz") + ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"]

def download_cards_for_query(query: str) -> List[Dict]:
    """下载指定搜索词的所有卡牌"""
    all_cards = []

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

        if total_count == 0:
            return all_cards

        total_pages = (total_count + BATCH_SIZE - 1) // BATCH_SIZE
        print(f"  📊 '{query}': {total_count:,} 张, {total_pages} 页")

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

                if page % 10 == 0 or page == total_pages:
                    print(f"    ⬇️  {page}/{total_pages} 页 ({len(all_cards)}/{total_count} 张)")

                time.sleep(0.1)

            except Exception as e:
                print(f"    ❌ 第 {page} 页出错: {e}")
                continue

        return all_cards

    except Exception as e:
        print(f"  ❌ 搜索 '{query}' 出错: {e}")
        return all_cards

def append_to_jsonl(cards: List[Dict]):
    """追加卡牌数据到 JSONL 文件"""
    with open(OUTPUT_FILE, 'a', encoding='utf-8') as f:
        for card in cards:
            card_clean = clean_card_data(card)
            f.write(json.dumps(card_clean, ensure_ascii=False) + '\n')

def clean_card_data(card: Dict) -> Dict:
    """清理卡牌数据，移除不必要的字段"""
    fields_to_keep = {
        'id', 'name', 'face_name', 'lang',
        'oracle_id', 'scryfall_uri',
        'mana_cost', 'cmc', 'type_line', 'oracle_text',
        'power', 'toughness', 'loyalty', 'defense',
        'colors', 'color_identity', 'legalities',
        'set', 'set_name', 'collector_number', 'rarity',
        'artist', 'released_at',
        'layout', 'card_faces', 'other_faces',
        'zhs_name', 'zhs_face_name', 'zhs_type_line', 'zhs_text',
        'keywords', 'image_uris',
    }

    cleaned = {}
    for field in fields_to_keep:
        if field in card:
            cleaned[field] = card[field]

    return cleaned

if __name__ == "__main__":
    print("=" * 60)
    print("🎴 MTGCH 完整卡牌数据库下载器（非交互式）")
    print("=" * 60)
    print()

    # 删除旧文件
    if Path(OUTPUT_FILE).exists():
        Path(OUTPUT_FILE).unlink()
        print("🗑️  已删除旧文件")
        print()

    all_cards = []
    seen_ids = set()

    for i, query in enumerate(SEARCH_QUERIES, 1):
        print(f"[{i}/{len(SEARCH_QUERIES)}] 搜索 '{query}'...")

        cards = download_cards_for_query(query)

        # 去重
        new_cards = [card for card in cards if card.get('id') not in seen_ids]
        for card in new_cards:
            seen_ids.add(card.get('id'))

        print(f"  ✨ 新增: {len(new_cards)} 张卡牌 (累计: {len(seen_ids)} 张)")

        if new_cards:
            append_to_jsonl(new_cards)

        print()

    print("=" * 60)
    print(f"✅ 下载完成！")
    print(f"   唯一卡牌: {len(seen_ids):,} 张")
    print(f"   文件大小: {Path(OUTPUT_FILE).stat().st_size / 1024 / 1024:.1f} MB")
    print("=" * 60)

    # 检查特定卡牌
    print(f"\n🔍 检查特定卡牌:")
    test_cards = ["Solitude", "Supreme Verdict"]
    with open(OUTPUT_FILE, 'r', encoding='utf-8') as f:
        cards_data = [json.loads(line) for line in f]

    for test_name in test_cards:
        found = any(card.get('name', '').lower() == test_name.lower() for card in cards_data)
        status = "✓" if found else "✗"
        print(f"   {status} {test_name}")

    print("\n✨ 完成！")
