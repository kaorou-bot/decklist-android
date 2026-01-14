# 更新日志 (Changelog)

所有值得注意的项目更改都将记录在此文件中。

## [2.4.8] - 2026-01-14

### 新增 (Added)
- **三级架构实现**: Event → Decklist → Card 完整层级结构
  - 新增 EventEntity 和 EventDao 数据库层
  - 新增 EventListActivity 和 EventDetailActivity UI层
  - 新增 fetchEventList() 和 fetchEventDecklists() 爬虫方法
- 数据库迁移 (v1 → v2):
  - 创建 events 表
  - 为 decklists 表添加 event_id 外键
  - 自动聚合现有数据创建赛事记录

### 改进 (Improved)
- DecklistRepository 现在支持通过 eventId 查询卡组
- 重构 MtgTop8Scraper 以支持事件枚举
- 优化数据库外键关系，确保数据完整性

### 技术细节 (Technical)
- 修复数据库迁移 SQL 语法错误
- 添加 EventDao 到 Hilt 依赖注入模块
- 实现事件-卡组关联的完整数据流

### 已知问题 (Known Issues)
- MTGTop8 的 d 参数枚举需要进一步优化（当前遇到 HTTP 500 错误）
- d 参数不是简单数字序列，而是具体的卡组ID

---

## [2.4.7] - 2026-01-13

### 新增
- 初始版本的 MTGTop8 爬虫功能
- 基础的卡组数据导入和展示

### 改进
- 数据库基础架构搭建
- Scryfall API 集成获取卡牌详情

---

## 格式说明

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

### 变更类型
- `新增` - 新功能
- `改进` - 现有功能的改进
- `修复` - bug 修复
- `移除` - 功能移除
- `已知问题` - 尚未解决的问题
