# MTGCH 数据库更新说明

## ⚠️ 重要提示

**当前数据库不包含机器翻译字段**。要使用机器翻译功能，需要从 MTGCH 官网获取最新数据库。

## 数据库获取方法

### 方法 1：从 MTGCH 官网获取（推荐）

1. 访问 MTGCH 官网：https://mtgch.com

2. 联系 MTGCH 团队获取完整的卡牌数据库文件（包含 `atomic_translated_*` 字段）

3. 将获取的 `mtgch_cards.jsonl` 文件放到：
   ```
   app/src/main/assets/mtgch_cards.jsonl
   ```

### 方法 2：使用脚本（需要有效端点）

```bash
./update_database.sh
```

**注意**：当前 MTGCH API 的 `/api/v1/cardlist` 端点可能不可用或需要认证。如果脚本失败，请使用方法 1。

## 更新后必须重新编译

数据库更新后，**必须重新编译应用**：

```bash
./gradlew assembleDebug
```

然后在设备上清除应用数据：

```bash
adb shell pm clear com.mtgo.decklistmanager
```

## 验证数据库是否包含机器翻译

运行以下命令检查：

```bash
head -1 app/src/main/assets/mtgch_cards.jsonl | python3 -c "import sys, json; d=json.loads(sys.stdin.read()); print('atomic_translated_name:', d.get('atomic_translated_name')); print('zhs_image_uris:', 'zhs_image_uris' in d)"
```

应该看到：
- `atomic_translated_name` 有值（例如："极光醒眠师"）
- `zhs_image_uris: True`

## 数据库字段说明

v3.12.0 及以后版本使用的数据库字段：

| 字段 | 用途 | 示例 |
|------|------|------|
| `zhs_name` | 官方中文翻译 | "热恋野兽 // 心之所爱" |
| `atomic_translated_name` | AI 翻译（fallback） | "极光醒眠师" |
| `zhs_image_uris` | MTGCH 中文卡图 | https://images.mtgch.com/... |
| `image_uris` | Scryfall 英文卡图 | https://cards.scryfall.io/... |

## 数据来源

- **API 端点**: `https://mtgch.com/api/v1/cardlist`
- **数据提供**: MTGCH (大学院废墟)
- **更新频率**: 建议每月更新一次
- **数据量**: 约 66,000 张卡牌
- **文件大小**: 约 150 MB (JSONL 格式)

## 常见问题

### Q: 为什么有些卡牌还是显示英文？

A: 这些卡牌可能：
1. 没有官方中文印刷
2. MTGCH API 也未提供 AI 翻译
3. 数据库文件是旧版本

### Q: 如何查看导入的卡牌数量？

A: v3.12.0 版本会在导入完成后显示具体数量，例如：
```
✅ 卡牌数据库导入完成！
成功导入 66504 张卡牌
```

### Q: 卡图为什么还是英文的？

A: 请确保：
1. 使用 v3.12.0 或更高版本
2. 数据库文件包含 `zhs_image_uris` 字段
3. 已重新编译并安装应用
4. 已清除应用数据重新导入

## 技术支持

如遇到问题，请检查：
1. 网络连接是否正常
2. MTGCH API 是否可访问：https://mtgch.com
3. 数据库文件格式是否正确（JSONL）
4. 应用日志是否有错误信息
