# v4.0.0 在线模式说明

## 分支信息
- 分支名称: `v4.0.0-online`
- 版本号: 4.0.0 (build 76)
- 基础版本: v3.12.1

## 主要变更

### 1. 移除本地数据库功能
- 删除数据库导入相关 UI
- 删除 WorkManager 后台导入任务
- 删除 `CardDatabaseDownloadWorker`

### 2. 在线使用 MTGCH API
- 所有卡牌查询直接调用 MTGCH API
- 无本地数据库存储
- 无需导入流程

### 3. 智能翻译回退机制
- 优先显示官方中文翻译
- 如果没有中文，调用 MTGCH API 获取机器翻译
- 使用 MTGCH 中文卡图

## 架构变更

### v3.x (离线模式)
```
用户查询卡牌
  → 检查本地数据库
  → 如果有数据，直接返回
  → 如果没有数据，返回 null
```

### v4.0.0 (在线模式)
```
用户查询卡牌
  → 调用 MTGCH API (/api/v1/result?q=卡牌名)
  → 解析响应数据
  → 使用机器翻译字段 (atomic_translated_*)
  → 缓存到内存（可选）
```

## 数据流

### MTGCH API 调用流程
1. 用户输入或选择卡牌
2. 调用 `MtgchApi.searchCard()`
3. 解析 `MtgchSearchResponse`
4. 提取卡牌信息：
   - `zhs_name` (官方中文) 或 `atomic_translated_name` (AI翻译)
   - `zhs_image_uris` (中文卡图)
   - 其他元数据

### 示例代码
```kotlin
// 在线查询卡牌
suspend fun getCardInfo(cardName: String): CardInfo? {
    val response = mtgchApi.searchCard(cardName)
    if (response.isSuccessful && response.body() != null) {
        val cards = response.body()!!.data
        if (!cards.isNullOrEmpty()) {
            val cardDto = cards[0]
            return mapToEntity(cardDto)
        }
    }
    return null
}
```

## 优势

1. **无需导入**: 首次启动即可使用，无需导入 150MB 数据库
2. **数据最新**: 直接从 MTGCH API 获取最新数据
3. **体积小**: APK 体积大幅减小（约 10MB）
4. **自动翻译**: 自动使用 MTGCH 的机器翻译功能

## 劣势

1. **需要网络**: 每次查询都需要网络连接
2. **速度较慢**: 相比本地数据库略慢
3. **API 限制**: 可能受到 MTGCH API 的调用频率限制

## 实施步骤

1. ✅ 创建新分支 `v4.0.0-online`
2. ✅ 更新版本号到 4.0.0
3. ⏳ 修改 MainActivity 移除数据库导入逻辑
4. ⏳ 修改 DecklistRepository 直接调用 MTGCH API
5. ⏳ 删除 CardDatabaseDownloadWorker
6. ⏳ 删除数据库导入 UI
7. ⏳ 测试在线查询功能
8. ⏳ 提交并推送到 GitHub

## 待办

- [ ] 移除数据库导入 UI
- [ ] 修改卡牌查询逻辑为在线模式
- [ ] 添加网络错误处理
- [ ] 添加加载状态提示
- [ ] 测试 API 调用
- [ ] 更新文档
