# v5.2.0 提交信息

## 提交标题
```
feat: v5.2.0 - 添加卡牌分类排序功能，移除下载按钮
```

## 提交内容

### 新增功能
- **卡牌分类排序**: 套牌详情页按类型分类显示（地/生物/法术/瞬间/神器/结界/鹏洛克/其他）
  - 支持中英文类型识别
  - 添加分类标题布局 `item_card_type_header.xml`

### 移除功能
- 移除赛事下载按钮（MainActivity）
- 移除套牌下载按钮（EventDetailActivity）
- 简化用户界面，减少手动操作

### 改进
- 隐藏 deck_count 为 0 的赛事（MainViewModel）
- 优化用户体验，只显示有效赛事

### 修改文件列表
```
app/build.gradle
  - 更新版本号到 5.2.0 (versionCode: 88)

app/src/main/java/com/mtgo/decklistmanager/ui/decklist/DeckDetailActivity.kt
  - 恢复 populateCardList() 卡牌分类排序功能
  - 支持中英文类型混合识别

app/src/main/java/com/mtgo/decklistmanager/ui/decklist/MainActivity.kt
  - 移除 btnDownloadEvent 按钮
  - 移除 showDownloadEventDialog() 方法
  - 移除 showDateSelectionDialog() 方法
  - 移除 showNumberOfEventsDialog() 方法

app/src/main/java/com/mtgo/decklistmanager/ui/decklist/EventDetailActivity.kt
  - 移除 btnDownloadDecklists 按钮
  - 移除 showDownloadDialogIfNeeded() 方法

app/src/main/java/com/mtgo/decklistmanager/ui/decklist/MainViewModel.kt
  - 添加过滤: deckCount > 0

app/src/main/res/layout/activity_main.xml
  - 移除下载按钮布局

app/src/main/res/layout/activity_event_detail.xml
  - 移除下载按钮布局

app/src/main/res/layout/item_card_type_header.xml (新建)
  - 卡牌分类标题布局

CHANGELOG.md
  - 添加 v5.2.0 更新日志

RELEASE_NOTES_v5.2.0.md (新建)
  - v5.2.0 发布说明
```

## 测试清单
- ✅ 卡牌分类显示正常
- ✅ 中英文类型都能正确识别
- ✅ 空赛事自动隐藏
- ✅ 下载按钮已移除
- ✅ 套牌详情页正常工作

## 已知问题
- 部分新系列卡牌缺少中文规则文本（服务器端数据问题）

## 相关文档
- CHANGELOG.md
- RELEASE_NOTES_v5.2.0.md
