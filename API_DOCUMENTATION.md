# MTG Card Server API 文档

## 基本信息

- **Base URL**: `http://182.92.109.160` 或 `http://your-server-ip`
- **API 版本**: v1.0.0
- **响应格式**: JSON
- **字符编码**: UTF-8

---

## 目录

1. [卡牌搜索](#1-卡牌搜索)
2. [随机卡牌](#2-随机卡牌)
3. [获取单张卡牌](#3-获取单张卡牌)
4. [系列相关](#4-系列相关)
5. [统计信息](#5-统计信息)
6. [数据模型](#6-数据模型)
7. [错误处理](#7-错误处理)

---

## 1. 卡牌搜索

### 1.1 MTGCH 兼容搜索接口（推荐用于 Android 客户端）

这是与原 mtgch 兼容的接口，适合 Android 客户端直接使用。

**接口地址**: `GET /api/result`

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| q | string | 否* | - | 搜索关键词（卡牌名称），支持中英文 |
| page | int | 否 | 1 | 页码（从 1 开始） |
| page_size | int | 否 | 20 | 每页数量（最大 100） |
| color | string | 否 | - | 颜色过滤：W,U,B,R,G |
| cmc | string | 否 | - | 法术力值过滤 |
| type | string | 否 | - | 类型过滤（如：Creature,Sorcery） |
| rarity | string | 否 | - | 稀有度过滤：common,uncommon,rare,mythic |
| set | string | 否 | - | 系列代码过滤（如：MID,NEO） |

*注：必须提供 `q` 参数或至少一个过滤参数

**请求示例**:

```bash
# 搜索卡牌
GET /api/result?q=lightning&page=1&page_size=20

# 中文搜索
GET /api/result?q=闪电&page=1&page_size=20

# 高级过滤
GET /api/result?q=dragon&color=red&rarity=mythic&page=1

# 仅使用过滤（无搜索词）
GET /api/result?color=red&type=creature&page=1
```

**响应示例**:

```json
{
  "success": true,
  "count": 150,
  "page": 1,
  "page_size": 20,
  "total_pages": 8,
  "items": [
    {
      "id": "123",
      "oracle_id": "abc123",
      "scryfall_id": "xyz789",
      "name": "Lightning Bolt",
      "zhs_name": "闪电",
      "face_name": null,
      "zhs_face_name": null,
      "lang": "zh",
      "mana_cost": "{R}",
      "cmc": 1,
      "type_line": "Instant",
      "zhs_type_line": "瞬间",
      "oracle_text": "Deal 3 damage to any target.",
      "zhs_text": "目标生物或牌手受到3点伤害。",
      "power": null,
      "toughness": null,
      "loyalty": null,
      "colors": ["R"],
      "color_identity": ["R"],
      "legalities": {
        "standard": "not_legal",
        "modern": "legal",
        "legacy": "legal",
        "commander": "legal"
      },
      "set": "2X2",
      "set_name": "Double Masters 2022",
      "set_translated_name": "Double Masters 2022",
      "collector_number": "123",
      "rarity": "common",
      "released_at": "2022-07-08T00:00:00.000Z",
      "image_uris": {
        "small": "https://cards.scryfall.io/small/front/...",
        "normal": "https://cards.scryfall.io/normal/front/...",
        "large": "https://cards.scryfall.io/large/front/...",
        "png": "https://cards.scryfall.io/png/front/...",
        "art_crop": "https://cards.scryfall.io/art_crop/front/...",
        "border_crop": "https://cards.scryfall.io/border_crop/front/..."
      },
      "zhs_image": "https://cards.scryfall.io/normal/front/...",
      "zhs_image_uris": {
        "small": "https://cards.scryfall.io/small/front/...",
        "normal": "https://cards.scryfall.io/normal/front/...",
        "large": "https://cards.scryfall.io/large/front/...",
        "png": "https://cards.scryfall.io/png/front/...",
        "art_crop": "https://cards.scryfall.io/art_crop/front/...",
        "border_crop": "https://cards.scryfall.io/border_crop/front/..."
      },
      "layout": "normal",
      "other_faces": [],
      "card_faces": null,
      "face_index": 0,
      "is_double_faced": false,
      "is_token": false
    }
  ]
}
```

**双面牌示例**:

```json
{
  "id": "536",
  "oracle_id": "ba57df46-11c9-4f28-8818-175fb52e67bc",
  "name": "Reckless Waif // Merciless Predator",
  "zhs_name": null,
  "lang": "en",
  "mana_cost": "{R}",
  "cmc": 1,
  "type_line": "Creature — Human Rogue Werewolf // Creature — Werewolf",
  "oracle_text": null,
  "colors": ["R"],
  "color_identity": ["R"],
  "set": "ISD",
  "set_name": "Innistrad",
  "collector_number": "159",
  "rarity": "uncommon",
  "layout": "transform",
  "card_faces": [
    {
      "name": "Reckless Waif",
      "nameZh": "鲁莽流浪儿",
      "mana_cost": "{R}",
      "type_line": "Creature — Human Rogue Werewolf",
      "oracle_text": "...",
      "power": "1",
      "toughness": "1",
      "image_uris": {
        "normal": "https://..."
      }
    },
    {
      "name": "Merciless Predator",
      "nameZh": "残酷掠食者",
      "mana_cost": null,
      "type_line": "Creature — Werewolf",
      "oracle_text": "...",
      "power": "3",
      "toughness": "2",
      "image_uris": {
        "normal": "https://..."
      }
    }
  ],
  "other_faces": [
    {
      "name": "Merciless Predator",
      "name_zh": "残酷掠食者",
      "mana_cost": null,
      "type_line": "Creature — Werewolf",
      "oracle_text": "...",
      "power": "3",
      "toughness": "2",
      "colors": ["R"],
      "image_uris": {
        "normal": "https://..."
      }
    }
  ],
  "is_double_faced": true,
  "face_index": 0
}
```

### 1.2 标准搜索接口

**接口地址**: `GET /api/cards/search`

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| q | string | 是 | - | 搜索关键词（卡牌名称） |
| limit | int | 否 | 20 | 返回数量（最大 100） |

**请求示例**:

```bash
GET /api/cards/search?q=lightning&limit=10
```

**响应示例**:

```json
{
  "success": true,
  "cards": [...],
  "total": 10
}
```

---

## 2. 随机卡牌

### 2.1 MTGCH 兼容随机接口（推荐）

**接口地址**: `GET /api/random`

**请求参数**: 无

**请求示例**:

```bash
GET /api/random
```

**响应示例**:

```json
{
  "id": "14967",
  "oracle_id": "dfbce321-d3c1-4b76-a3fa-27f875015085",
  "name": "Shaper Guildmage",
  "zhs_name": "塑行公会法师",
  "mana_cost": "{U}",
  "cmc": 1,
  "type_line": "Creature — Human Wizard",
  "...": "..."
}
```

### 2.2 标准随机接口

**接口地址**: `GET /api/cards/random`

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| count | int | 否 | 1 | 返回数量（最大 10） |

**请求示例**:

```bash
GET /api/cards/random?count=5
```

**响应示例**:

```json
{
  "success": true,
  "cards": [...],
  "total": 5
}
```

---

## 3. 获取单张卡牌

### 3.1 按 Oracle ID 获取

**接口地址**: `GET /api/cards/:oracleId`

**请求示例**:

```bash
GET /api/cards/abc123-def-456
```

**响应示例**:

```json
{
  "success": true,
  "card": {
    "id": 123,
    "oracleId": "abc123-def-456",
    "name": "Lightning Bolt",
    "nameZh": "闪电",
    "...": "..."
  }
}
```

### 3.2 按数据库 ID 获取

**接口地址**: `GET /api/cards/id/:id`

**请求示例**:

```bash
GET /api/cards/id/123
```

### 3.3 按系列和编号获取

**接口地址**: `GET /api/cards/set/:setCode/:collectorNumber`

**请求示例**:

```bash
GET /api/cards/set/MID/123
```

### 3.4 按名称获取（精确匹配）

**接口地址**: `GET /api/cards?name=:name`

**请求示例**:

```bash
GET /api/cards?name=Lightning Bolt
```

---

## 4. 系列相关

### 4.1 获取所有系列

**接口地址**: `GET /api/sets`

**请求示例**:

```bash
GET /api/sets
```

**响应示例**:

```json
{
  "success": true,
  "sets": [
    {
      "code": "MID",
      "name": "Innistrad: Midnight Hunt",
      "nameZh": "依尼翠：午夜猎杀",
      "releaseDate": "2021-09-24",
      "cardCount": 277
    }
  ],
  "total": 100
}
```

### 4.2 获取系列中的所有卡牌

**接口地址**: `GET /api/cards/set/:setCode`

**请求示例**:

```bash
GET /api/cards/set/MID
```

**响应示例**:

```json
{
  "success": true,
  "cards": [...],
  "total": 277
}
```

---

## 5. 统计信息

### 5.1 健康检查

**接口地址**: `GET /api/health`

**请求示例**:

```bash
GET /api/health
```

**响应示例**:

```json
{
  "success": true,
  "message": "MTG Card Server is running",
  "database": "connected",
  "timestamp": "2026-02-16T00:12:43.114Z"
}
```

### 5.2 热门卡牌

**接口地址**: `GET /api/stats/popular`

**请求参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | int | 否 | 10 | 返回数量（最大 50） |

**请求示例**:

```bash
GET /api/stats/popular?limit=20
```

**响应示例**:

```json
{
  "success": true,
  "cards": [...],
  "total": 20
}
```

---

## 6. 数据模型

### Card 对象

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string | 数据库 ID 或 Scryfall ID |
| oracle_id | string | Oracle ID（唯一标识符） |
| scryfall_id | string | Scryfall ID |
| name | string | 卡牌英文名称 |
| zhs_name | string\|null | 卡牌中文名称 |
| face_name | string\|null | 当前面的名称（双面牌） |
| zhs_face_name | string\|null | 当前面的中文名称 |
| lang | string | 语言标识："zh" 或 "en" |
| mana_cost | string\|null | 法术力费用（如 "{R}{R}"） |
| cmc | number\|null | 转化法术力值 |
| type_line | string | 类型行 |
| zhs_type_line | string\|null | 类型行中文 |
| oracle_text | string\|null | 规则文本 |
| zhs_text | string\|null | 规则文本中文 |
| power | string\|null | 攻击力 |
| toughness | string\|null | 防御力 |
| loyalty | string\|null | 忠诚度（鹏洛客） |
| colors | string[] | 颜色数组：["W","U","B","R","G"] |
| color_identity | string[] | 颜色身份 |
| legalities | object | 赛事合法性 |
| set | string | 系列代码 |
| set_name | string | 系列名称 |
| set_translated_name | string | 系列翻译名称 |
| collector_number | string | 收藏编号 |
| rarity | string | 稀有度：common, uncommon, rare, mythic |
| released_at | string\|null | 发布日期（ISO 8601） |
| image_uris | object\|null | 图片 URL 对象 |
| zhs_image | string\|null | 中文图片 URL |
| zhs_image_uris | object\|null | 中文图片 URL 对象 |
| layout | string | 布局类型：normal, transform, modal_dfc, etc. |
| other_faces | object[] | 其他面的信息（双面牌） |
| card_faces | object[]\|null | 所有面的信息（双面牌） |
| face_index | number | 当前面的索引（0=正面） |
| is_double_faced | boolean | 是否为双面牌 |
| is_token | boolean | 是否为衍生物 |

### ImageUris 对象

| 字段 | 类型 | 说明 |
|------|------|------|
| small | string | 小图 (68x96 px) |
| normal | string | 标准图 (203x285 px) |
| large | string| 大图 (242x337 px) |
| png | string | PNG 格式图片 |
| art_crop | string| 裁剪图（仅插图） |
| border_crop | string| 边框裁剪图 |

### CardFace 对象（双面牌的单面）

| 字段 | 类型 | 说明 |
|------|------|------|
| name | string | 面的名称 |
| nameZh | string\|null | 面的中文名称 |
| name_zh | string\|null | 面的中文名称（mtgch 格式） |
| mana_cost | string\|null | 法术力费用 |
| colors | string[] | 颜色 |
| type_line | string | 类型行 |
| oracle_text | string\|null | 规则文本 |
| power | string\|null | 攻击力 |
| toughness | string\|null| 防御力 |
| loyalty | string\|null| 忠诚度 |
| image_uris | object\|null | 图片 URL |

---

## 7. 错误处理

### 错误响应格式

```json
{
  "success": false,
  "error": "错误描述信息"
}
```

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源未找到 |
| 500 | 服务器内部错误 |

### 常见错误

| 错误信息 | 说明 | 解决方案 |
|----------|------|----------|
| Missing "q" parameter or filters | 缺少搜索关键词或过滤条件 | 提供 q 参数或至少一个过滤参数 |
| Card not found | 卡牌未找到 | 检查 ID 或名称是否正确 |
| Internal server error | 服务器内部错误 | 稍后重试或联系管理员 |

---

## 8. Android 客户端集成示例

### 使用 OkHttp

```java
OkHttpClient client = new OkHttpClient();

// 搜索卡牌
String url = "http://182.92.109.160/api/result?q=lightning&page=1&page_size=20";
Request request = new Request.Builder()
    .url(url)
    .build();

try (Response response = client.newCall(request).execute()) {
    if (response.isSuccessful() && response.body() != null) {
        String json = response.body().string();
        // 解析 JSON 响应
        JSONObject jsonObject = new JSONObject(json);
        JSONArray items = jsonObject.getJSONArray("items");

        for (int i = 0; i < items.length(); i++) {
            JSONObject card = items.getJSONObject(i);
            String name = card.getString("name");
            String nameZh = card.optString("zhs_name", "");
            // 处理卡牌数据...
        }
    }
} catch (IOException | JSONException e) {
    e.printStackTrace();
}
```

### 使用 Retrofit

```java
public interface MtgApiService {
    @GET("api/result")
    Call<SearchResponse> searchCards(
        @Query("q") String query,
        @Query("page") int page,
        @Query("page_size") int pageSize
    );

    @GET("api/random")
    Call<Card> getRandomCard();
}

// 使用
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("http://182.92.109.160/")
    .addConverterFactory(GsonConverterFactory.create())
    .build();

MtgApiService service = retrofit.create(MtgApiService.class);

// 搜索
Call<SearchResponse> call = service.searchCards("lightning", 1, 20);
call.enqueue(new Callback<SearchResponse>() {
    @Override
    public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            List<Card> cards = response.body().getItems();
            // 显示卡牌列表
        }
    }

    @Override
    public void onFailure(Call<SearchResponse> call, Throwable t) {
        // 处理错误
    }
});
```

### 数据模型示例（Java/Kotlin）

```java
public class Card {
    private String id;
    private String oracleId;
    private String scryfallId;
    private String name;
    @SerializedName("zhs_name")
    private String nameZh;
    private String manaCost;
    private double cmc;
    @SerializedName("type_line")
    private String typeLine;
    @SerializedName("oracle_text")
    private String oracleText;
    private String power;
    private String toughness;
    private List<String> colors;
    private String rarity;
    private String set;
    @SerializedName("set_name")
    private String setName;
    @SerializedName("image_uris")
    private ImageUris imageUris;
    @SerializedName("is_double_faced")
    private boolean isDoubleFaced;
    private List<CardFace> cardFaces;

    // Getters and Setters...
}

public class ImageUris {
    private String small;
    private String normal;
    private String large;
    private String png;

    // Getters and Setters...
}

public class CardFace {
    private String name;
    @SerializedName("nameZh")
    private String nameZh;
    private String manaCost;
    @SerializedName("type_line")
    private String typeLine;
    private String power;
    private String toughness;
    private ImageUris imageUris;

    // Getters and Setters...
}
```

---

## 9. 图片处理建议

### 图片 URL 选择

- **列表缩略图**: 使用 `image_uris.small` (68x96 px)
- **标准显示**: 使用 `image_uris.normal` (203x285 px) 或 `zhs_image`
- **高清显示**: 使用 `image_uris.large` (242x337 px)
- **原图**: 使用 `image_uris.png` (无损 PNG)

### 双面牌处理

双面牌的 `card_faces` 数组包含所有面的信息和图片：

```java
if (card.getCardFaces() != null && !card.getCardFaces().isEmpty()) {
    // 显示双面牌
    for (CardFace face : card.getCardFaces()) {
        String faceName = face.getName();
        String faceNameZh = face.getNameZh();
        String imageUrl = face.getImageUris().getNormal();
        // 显示每一面...
    }
}
```

### 图片缓存

建议使用图片加载库（如 Glide 或 Picasso）并启用缓存：

```java
// 使用 Glide 加载和缓存图片
Glide.with(context)
    .load(card.getImageUris().getNormal())
    .placeholder(R.drawable.card_back)
    .error(R.drawable.card_back)
    .into(cardImageView);
```

---

## 10. 最佳实践

### 10.1 搜索建议

- 使用防抖（debounce）避免频繁请求
- 显示加载指示器
- 缓存搜索结果
- 处理空结果情况

### 10.2 性能优化

- 使用分页避免一次加载过多数据
- 图片使用懒加载
- 启用 HTTP/2 连接复用
- 实现离线缓存

### 10.3 用户体验

- 优先显示中文图片（`zhs_image`）
- 支持中英文混合搜索
- 双面牌提供翻面查看功能
- 显示卡牌稀有度和系列信息

---

## 11. 版本更新日志

### v1.0.0 (2026-02-16)

- ✅ 支持中英文卡牌搜索
- ✅ 双面牌完整支持（含中文翻译）
- ✅ MTGCH 兼容接口
- ✅ 随机卡牌功能
- ✅ 系列过滤
- ✅ 高级搜索过滤（颜色、类型、稀有度等）

---

## 12. 支持

如有问题，请联系技术支持或查看项目文档。

**服务器地址**: http://182.92.109.160

**更新日期**: 2026-02-16
