# 修复赛事页面套牌下载功能

**日期**: 2025-02-19
**问题**: 赛事页面无法下载套牌

---

## 🐛 问题

用户反馈：赛事页面点击"下载套牌"按钮后，套牌列表为空。

## 🔍 根本原因

`DecklistDto` 中定义了 `source` 字段：
```kotlin
data class DecklistDto(
    // ...
    val source: String?  // ❌ 服务器套牌列表接口不返回此字段
)
```

但服务器的 `/api/v1/events/{eventId}/decklists` 接口返回的数据中不包含 `source` 字段：
```json
{
  "id": 35,
  "eventId": 2,
  "eventName": "Modern League",
  "deckName": "6argamel's Deck",
  // ... 其他字段
  // ❌ 没有 "source" 字段
}
```

这导致 Gson 解析 JSON 时失败（或至少行为异常），套牌列表无法正确加载。

## ✅ 解决方案

从 `DecklistDto` 中移除 `source` 字段：
```kotlin
data class DecklistDto(
    val id: Long,
    val eventId: Long?,
    val eventName: String,
    val deckName: String?,
    val format: String,
    val date: String,
    val playerName: String?,
    val record: String?,
    val url: String?
    // 移除了 source 字段
)
```

## 📝 注意

- 赛事详情接口 (`/api/v1/events/{id}`) 返回的数据中包含 `source` 字段
- 套牌列表接口 (`/api/v1/events/{id}/decklists`) 返回的数据中不包含 `source` 字段
- 如果需要，可以从赛事信息中获取 `source`

## 🔧 修改文件

- `app/src/main/java/com/mtgo/decklistmanager/data/remote/api/dto/ServerDto.kt`
  - 移除 `DecklistDto.source` 字段

---

**状态**: 已修复并安装
