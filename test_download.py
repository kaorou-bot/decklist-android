#!/usr/bin/env python3
import requests
import json
import time

API_BASE = "https://mtgch.com/api/v1"
BATCH_SIZE = 100

# Test one query
query = "e"
print(f"Testing query: {query}")

response = requests.get(f"{API_BASE}/result", params={
    "q": query,
    "page_size": 1,
    "page": 1
}, timeout=30)

print(f"Status: {response.status_code}")
data = response.json()
total_count = data.get('count', 0)
print(f"Total cards: {total_count}")

if total_count > 0:
    total_pages = min(3, (total_count + BATCH_SIZE - 1) // BATCH_SIZE)  # Just first 3 pages
    print(f"Downloading first {total_pages} pages...")
    
    for page in range(1, total_pages + 1):
        response = requests.get(f"{API_BASE}/result", params={
            "q": query,
            "page_size": BATCH_SIZE,
            "page": page
        }, timeout=30)
        
        data = response.json()
        cards = data.get('items', [])
        print(f"Page {page}: {len(cards)} cards")
        time.sleep(0.2)

print("Test complete!")
