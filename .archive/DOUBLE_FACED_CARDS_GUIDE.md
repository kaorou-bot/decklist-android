# 双面牌 API 使用指南

## 概述

双面牌（Double-Faced Cards）是一类特殊的 Magic: The Gathering 卡牌，具有两个或多个面。本指南详细说明如何在安卓端正确处理这类卡牌。

---

## 1. 识别双面牌

### 方法 1：检查 `is_double_faced` 字段

```json
{
  "is_double_faced": true,
  "layout": "transform"
}
```

### 方法 2：检查 `layout` 字段

双面牌的 `layout` 可能是以下值之一：
- `transform` - 正面/反面转换（如：双面牌、中野尊牌）
- `modal_dfc` - 模式双面牌
- `reversible_card` - 双面卡牌
- `adventure` - 历险（瞬间/法术 + 生物面）
- `split` - 分割卡
- `flip` - 翻转牌
- `double_faced_token` - 双面衍生物

---

## 2. API 响应结构

### 2.1 主卡牌对象

```json
{
  "id": "536",
  "oracle_id": "ba57df46-11c9-4f28-8818-175fb52e67bc",
  "name": "Reckless Waif // Merciless Predator",
  "zhs_name": null,
  "name_zh": null,
  "lang": "en",
  "mana_cost": "{R}",
  "cmc": 1,
  "type_line": "Creature — Human Rogue Werewolf // Creature — Werewolf",
  "zhs_type_line": "生物 — 人类 浪客 狼人 // 生物 — 狼人",
  "oracle_text": null,
  "zhs_text": null,
  "colors": ["R"],
  "color_identity": ["R"],
  "rarity": "uncommon",
  "set": "ISD",
  "set_name": "Innistrad",
  "collector_number": "159",
  "layout": "transform",
  "is_double_faced": true,
  "face_index": 0,
  "card_faces": [...],
  "other_faces": [...]
}
```

**关键字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `is_double_faced` | boolean | 是否为双面牌 |
| `layout` | string | 卡牌布局类型 |
| `face_index` | number | 当前显示的面（0 = 正面，1 = 背面） |
| `card_faces` | array | 所有的面（完整数据） |
| `other_faces` | array | 其他面的信息（简化数据） |

### 2.2 `card_faces` 数组（推荐使用）

`card_faces` 数组包含所有面的完整信息，每个面的字段格式与主卡牌对象相同：

```json
{
  "card_faces": [
    {
      "name": "Reckless Waif",
      "name_zh": "鲁莽流浪儿",
      "mana_cost": "{R}",
      "type_line": "Creature — Human Rogue Werewolf",
      "type_line_zh": "生物 — 人类 浪客 狼人",
      "oracle_text": "At the beginning of each upkeep, if no spells were cast last turn, ..."
    },
    {
      "name": "Merciless Predator",
      "name_zh": "残酷掠食者",
      "mana_cost": null,
      "type_line": "Creature — Werewolf",
      "type_line_zh": "生物 — 狼人",
      "oracle_text": "Whenever Merciless Predator deals combat damage to a player, ..."
    }
  ]
}
```

**`card_faces` 中每个面的字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | ✅ | 面的英文名称 |
| `name_zh` | string\|null | ❌ | 面的中文名称 |
| `mana_cost` | string\|null | ❌ | 法术力费用（背面可能为 null） |
| `type_line` | string | ❌ | 英文类型行 |
| `type_line_zh` | string\|null | ❌ | 中文类型行 |
| `oracle_text` | string\|null | ❌ | 英文规则文本 |
| `oracle_text_zh` | string\|null | ❌ | 中文规则文本 |
| `power` | string\|null | ❌ | 攻击力 |
| `toughness` | string\|null | ❌ | 防御力 |
| `loyalty` | string\|null | ❌ | 忠诚度（鹏洛客） |
| `colors` | array | ❌ | 颜色数组 |
| `image_uris` | object\|null | ❌ | 图片 URL 对象 |

### 2.3 `other_faces` 数组

`other_faces` 包含除当前面外的其他面（简化数据）：

```json
{
  "other_faces": [
    {
      "name": "Merciless Predator",
      "name_zh": "残酷掠食者",
      "mana_cost": null,
      "type_line": "Creature — Werewolf",
      "oracle_text": "..."
    }
  ]
}
```

---

## 3. 安卓端处理指南

### 3.1 数据模型定义

```java
public class Card implements Serializable {
    private String id;
    private String oracleId;
    private String name;
    private String nameZh;         // zhs_name
    private String typeLine;       // type_line
    private String typeLineZh;     // zhs_type_line (新增)
    private String oracleText;     // oracle_text
    private String oracleTextZh;   // zhs_text (新增)
    private String manaCost;
    private String layout;
    private boolean isDoubleFaced; // is_double_faced
    private int faceIndex;         // face_index
    private List<CardFace> cardFaces;    // card_faces
    private List<OtherFace> otherFaces;  // other_faces

    // Getters and Setters...
}

public class CardFace implements Serializable {
    private String name;
    private String nameZh;         // name_zh
    private String manaCost;
    private String typeLine;       // type_line
    private String typeLineZh;     // type_line_zh (新增)
    private String oracleText;     // oracle_text
    private String oracleTextZh;   // oracle_text_zh (新增)
    private String power;
    private String toughness;
    private ImageUris imageUris;

    // Getters and Setters...
}

public class OtherFace implements Serializable {
    private String name;
    private String nameZh;         // name_zh
    private String manaCost;
    private String typeLine;       // type_line
    private String oracleText;     // oracle_text
    private String power;
    private String toughness;
    private List<String> colors;
    private ImageUris imageUris;
}
```

### 3.2 使用 Gson 解析

```java
Gson gson = new GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create();

CardResponse response = gson.fromJson(jsonString, CardResponse.class);
Card card = response.getItems().get(0);
```

### 3.3 判断是否为双面牌

```java
if (card.isDoubleFaced() || "transform".equals(card.getLayout())) {
    // 这是双面牌
    if (card.getCardFaces() != null && !card.getCardFaces().isEmpty()) {
        // 显示所有面
        for (CardFace face : card.getCardFaces()) {
            String name = face.getName();
            String nameZh = face.getNameZh();
            String typeLine = face.getTypeLine();
            String typeLineZh = face.getTypeLineZh();
            // 显示这个面...
        }
    }
}
```

### 3.4 显示双面牌

```java
public void displayDoubleFacedCard(Card card) {
    if (card.getCardFaces() != null && !card.getCardFaces().isEmpty()) {
        // 方法 1: 使用 ViewPager 显示所有面
        ViewPager viewPager = findViewById(R.id.viewPager);
        CardFaceAdapter adapter = new CardFaceAdapter(card.getCardFaces());
        viewPager.setAdapter(adapter);

        // 方法 2: 使用 Tab 切换
        TabLayout tabLayout = findViewById(R.id.tabs);
        for (int i = 0; i < card.getCardFaces().size(); i++) {
            CardFace face = card.getCardFaces().get(i);
            String tabName = face.getNameZh() != null ? face.getNameZh() : face.getName();
            tabLayout.addTab(tabLayout.newTab().setText(tabName));
        }
    }
}
```

### 3.5 加载双面牌图片

```java
public void loadCardFaceImages(Card card) {
    if (card.getCardFaces() != null) {
        for (CardFace face : card.getCardFaces()) {
            if (face.getImageUris() != null
                && face.getImageUris().getNormal() != null) {

                String imageUrl = face.getImageUris().getNormal();

                // 使用 Glide 加载
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.card_back)
                    .into(imageView);

                // 或使用 Picasso
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.card_back)
                    .into(imageView);
            }
        }
    }
}
```

### 3.6 显示中文名称和类型

```java
public void displayCardInfo(Card card) {
    // 主卡牌名称
    String name = card.getName();
    String nameZh = card.getNameZh();

    // 显示名称（优先中文）
    textViewName.setText(nameZh != null ? nameZh : name);

    // 显示类型（优先中文）
    String typeLine = card.getTypeLine();
    String typeLineZh = card.getTypeLineZh();
    textViewType.setText(typeLineZh != null ? typeLineZh : typeLine);

    // 如果是双面牌，显示所有面的信息
    if (card.isDoubleFaced() && card.getCardFaces() != null) {
        StringBuilder sb = new StringBuilder();
        for (CardFace face : card.getCardFaces()) {
            sb.append(face.getName())
              .append(" / ")
              .append(face.getNameZh() != null ? face.getNameZh() : "")
              .append("\n");
        }
        textViewAllNames.setText(sb.toString());
    }
}
```

---

## 4. 常见问题解决

### 4.1 字段为 null 的问题

**问题**：`name_zh` 或 `type_line_zh` 返回 null

**原因**：该卡牌可能没有翻译，或使用的是旧版本数据

**解决方案**：

```java
// 总是提供回退值
String displayName = card.getNameZh() != null
    ? card.getNameZh()
    : card.getName();

String displayType = card.getTypeLineZh() != null
    ? card.getTypeLineZh()
    : card.getTypeLine();
```

### 4.2 解析错误

**问题**：Gson 解析失败，字段不匹配

**解决方案**：确保使用正确的命名策略

```java
Gson gson = new GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create();
```

### 4.3 图片加载失败

**问题**：双面牌的图片 URL 在 `image_uris` 中为 null

**解决方案**：使用 `card_faces` 中每张面的 `image_uris`

```java
public String getCardFaceImageUrl(Card card, int faceIndex) {
    if (card.getCardFaces() != null && card.getCardFaces().size() > faceIndex) {
        CardFace face = card.getCardFaces().get(faceIndex);
        if (face.getImageUris() != null && face.getImageUris().getNormal() != null) {
            return face.getImageUris().getNormal();
        }
    }
    // 回退到主卡牌的图片
    return card.getImageUris() != null ? card.getImageUris().getNormal() : null;
}
```

---

## 5. 完整示例代码

### 5.1 Activity 示例

```java
public class CardDetailActivity extends AppCompatActivity {
    private Card card;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_detail);

        // 获取卡牌数据
        String cardJson = getIntent().getStringExtra("card_json");
        card = new Gson().fromJson(cardJson, Card.class);

        displayCard();
    }

    private void displayCard() {
        TextView nameText = findViewById(R.id.card_name);
        TextView typeText = findViewById(R.id.card_type);
        ImageView imageView = findViewById(R.id.card_image);
        ViewPager viewPager = findViewById(R.id.face_viewpager);

        // 显示名称（优先中文）
        nameText.setText(card.getNameZh() != null
            ? card.getNameZh()
            : card.getName());

        // 显示类型（优先中文）
        typeText.setText(card.getTypeLineZh() != null
            ? card.getTypeLineZh()
            : card.getTypeLine());

        // 处理双面牌
        if (card.isDoubleFaced() && card.getCardFaces() != null) {
            // 显示所有面
            CardFacePagerAdapter adapter = new CardFacePagerAdapter(
                getSupportFragmentManager(),
                card.getCardFaces()
            );
            viewPager.setAdapter(adapter);
            viewPager.setVisibility(View.VISIBLE);
        } else {
            // 单面卡，直接显示图片
            if (card.getImageUris() != null) {
                Glide.with(this)
                    .load(card.getImageUris().getNormal())
                    .into(imageView);
            }
        }
    }
}
```

### 5.2 RecyclerView Adapter

```java
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {
    private List<Card> cards;

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Card card = cards.get(position);

        // 名称
        String displayName = card.getNameZh() != null
            ? card.getNameZh()
            : card.getName();
        holder.nameText.setText(displayName);

        // 类型
        String displayType = card.getTypeLineZh() != null
            ? card.getTypeLineZh()
            : card.getTypeLine();
        holder.typeText.setText(displayType);

        // 双面牌标记
        if (card.isDoubleFaced()) {
            holder.doubleFacedBadge.setVisibility(View.VISIBLE);
        } else {
            holder.doubleFacedBadge.setVisibility(View.GONE);
        }

        // 加载图片
        String imageUrl = null;
        if (card.getCardFaces() != null && !card.getCardFaces().isEmpty()) {
            // 双面牌，使用第一面的图片
            CardFace firstFace = card.getCardFaces().get(0);
            if (firstFace.getImageUris() != null) {
                imageUrl = firstFace.getImageUris().getNormal();
            }
        } else if (card.getImageUris() != null) {
            // 单面卡
            imageUrl = card.getImageUris().getNormal();
        }

        if (imageUrl != null) {
            Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.card_back)
                .into(holder.cardImage);
        }
    }
}
```

---

## 6. API 测试

### 6.1 搜索双面牌

```bash
# 搜索双面牌
curl "http://182.92.109.160/api/result?q=Reckless+Waif"

# 搜索中野尊包双面牌
curl "http://182.92.109.160/api/result?q=Invasion+of+Tolvada"

# 搜索冒险双面牌
curl "http://182.92.109.160/api/result?q=Bramble+Trapper"
```

### 6.2 验证响应

```bash
# 检查 card_faces 数组
curl "http://182.92.109.160/api/result?q=Reckless+Waif" | jq '.items[0].card_faces'
```

---

## 7. 字段对照表

### 英文 -> 中文字段映射

| API 返回字段 | Java 字段 | 说明 |
|------------|----------|------|
| `name` | `name` | 英文名称 |
| `zhs_name` | `nameZh` | 中文名称 |
| `name_zh` | `nameZh` | 中文名称（card_faces 内） |
| `type_line` | `typeLine` | 英文类型行 |
| `zhs_type_line` | `typeLineZh` | 中文类型行 |
| `type_line_zh` | `typeLineZh` | 中文类型行（card_faces 内） |
| `oracle_text` | `oracleText` | 英文规则文本 |
| `zhs_text` | `oracleTextZh` | 中文规则文本 |
| `oracle_text_zh` | `oracleTextZh` | 中文规则文本（card_faces 内） |
| `is_double_faced` | `isDoubleFaced` | 是否为双面牌 |
| `face_index` | `faceIndex` | 当前面索引 |

---

## 8. 更新日志

### v1.1 (2026-02-16)

- ✅ 统一 `card_faces` 数组中的字段命名（使用蛇形命名）
- ✅ 添加 `type_line_zh` 字段到 `card_faces`
- ✅ 添加 `oracle_text_zh` 字段到 `card_faces`
- ✅ 确保安卓端可以正确解析所有字段

---

## 9. 技术支持

如有问题，请检查：

1. **API 响应格式**：使用 `jq` 或 JSON 验证工具检查响应
2. **字段命名**：确保使用 LOWER_CASE_WITH_UNDERSCORES 策略
3. **null 检查**：总是处理可能为 null 的字段
4. **双面牌判断**：使用 `is_double_faced` 或 `layout` 字段判断

**服务器地址**: http://182.92.109.160

**更新日期**: 2026-02-16
