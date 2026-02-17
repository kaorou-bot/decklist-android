# 自有服务端 API 搜索功能诊断报告

**诊断时间**: 2026-02-16
**诊断结果**: ⚠️ **搜索功能异常 - 返回空结果**

---

## 🔍 问题现象

### 客户端日志
```
GET http://182.92.109.160/api/result?q=Lightning&page=1&page_size=20
Response: {"success":true,"count":0,"page":1,"page_size":20,"total_pages":0,"items":[]}
SearchViewModel: Found 0 results
```

### 测试结果

| 测试项 | URL | 结果 |
|--------|-----|------|
| 健康检查 | `/api/health` | ✅ 正常 |
| 随机卡牌 | `/api/random` | ✅ 返回数据 |
| 搜索 "Lightning" | `/api/result?q=Lightning` | ❌ 空结果 |
| 搜索 "delver" | `/api/result?q=delver` | ❌ 空结果 |
| 搜索 "Planar" | `/api/result?q=Planar` | ❌ 空结果 |

---

## 🐛 根本原因

**服务端搜索功能存在问题**：
1. ✅ 数据库连接正常（random 接口工作）
2. ✅ 数据存在（random 接口返回卡牌数据）
3. ❌ **搜索查询失败**（所有搜索都返回 0 结果）

可能的原因：
- 数据库缺少搜索索引
- 搜索逻辑错误
- 数据库字段未正确映射
- MongoDB 文本搜索未配置

---

## 🛠️ 解决方案

### 方案 1：修复服务端搜索功能（推荐）

需要检查服务端代码，确保：

1. **数据库索引已建立**
```javascript
// MongoDB 索引示例
db.cards.createIndex({ name: "text", zhs_name: "text" })
db.cards.createIndex({ oracle_id: 1 })
```

2. **搜索逻辑正确**
```javascript
// 确保搜索查询正确处理
const searchQuery = {
  $or: [
    { name: { $regex: query, $options: 'i' } },
    { zhs_name: { $regex: query, $options: 'i' } }
  ]
}
```

3. **API 路由正确**
```javascript
app.get('/api/result', async (req, res) => {
  const { q } = req.query
  // 确保搜索逻辑正确执行
})
```

### 方案 2：客户端临时使用 Scryfall API（备选）

在服务端搜索功能修复前，可以临时切换回 Scryfall 或 MTGCH API：

```kotlin
// AppModule.kt
fun provideMtgchRetrofit(okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder()
        .baseUrl("https://api.mtgch.com/")  // 或其他可用 API
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
```

### 方案 3：使用本地数据库缓存（临时方案）

如果服务端无法快速修复，可以考虑：
1. 下载离线卡牌数据库
2. 实现本地搜索
3. 定期同步更新

---

## 📊 当前状态

### 客户端状态
- ✅ API 迁移代码完成
- ✅ 编译成功
- ✅ 安装到模拟器
- ✅ 网络请求正常发送
- ✅ 响应解析正常
- ❌ **搜索结果为空（服务端问题）**

### 服务端状态
- ✅ 服务运行正常
- ✅ 数据库已连接
- ✅ 随机卡牌功能正常
- ❌ **搜索功能返回空结果**

---

## 🎯 下一步行动

### 立即行动（高优先级）

1. **联系服务端开发者**
   - 报告搜索功能问题
   - 提供测试日志
   - 请求修复搜索接口

2. **验证服务端配置**
   - 检查数据库索引
   - 检查搜索逻辑
   - 查看服务器日志

3. **临时解决方案**
   - 考虑切换回其他 API
   - 或实现本地搜索

### 后续改进（中优先级）

1. **添加错误处理**
   ```kotlin
   if (response.body()?.success == true) {
       val items = response.body()?.items
       if (items.isNullOrEmpty()) {
           // 显示友好提示："未找到匹配的卡牌"
       }
   }
   ```

2. **添加搜索建议**
   - 提供搜索提示
   - 自动纠错
   - 相关推荐

3. **实现离线搜索**
   - 下载卡牌数据库
   - 本地索引
   - 离线搜索

---

## 📝 技术细节

### 测试命令

```bash
# 测试健康检查
curl "http://182.92.109.160/api/health"

# 测试随机卡牌
curl "http://182.92.109.160/api/random"

# 测试搜索
curl "http://182.92.109.160/api/result?q=Lightning"

# 测试分页
curl "http://182.92.109.160/api/result?q=a&page=1&page_size=100"
```

### 期望的搜索响应

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
      "name": "Lightning Bolt",
      "zhs_name": "闪电",
      ...
    }
  ]
}
```

### 实际的搜索响应

```json
{
  "success": true,
  "count": 0,
  "page": 1,
  "page_size": 20,
  "total_pages": 0,
  "items": []
}
```

---

## 📞 联系信息

**客户端问题**: 请查看 `SelfHosted_API_Migration_Report.md`
**服务端问题**: 需要服务端开发者修复搜索功能
**API 文档**: `API_DOCUMENTATION.md`

---

**诊断结论**: ⚠️ **客户端代码正确，问题出在服务端搜索功能**

**建议**: 立即联系服务端开发者修复搜索接口，或临时切换回其他可用的卡牌 API。
