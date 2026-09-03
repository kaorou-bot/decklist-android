# 当前任务 (CURRENT_TASK)

> 记录当前正在进行的任务，便于快速恢复工作

---

## 📊 任务状态

**状态**: 🟡 **开发中 - v5.1.0**
**最后更新**: 2026-09-03
**当前版本**: v5.0.1 (已发布)，v5.1.0 (开发中)
**当前分支**: dev/v5.1.0

---

## 🚀 当前任务 (2026-09-03)

### v5.1.0 - 恢复开发：工程治理 + 新卡牌服务器接入

#### 背景
- 项目停更约 7 个月（最后提交 2026-02-20，v5.0.1）
- 旧自建卡牌服务器 `http://182.92.109.160/` 已离线，在线搜索功能不可用
- 用户已有新的卡牌服务器（数据格式待提供），将接入替换

#### 已完成 ✅
- [x] 本机环境打通：克隆仓库、SSL 证书（Windows 根证书 PEM）、JDK 17 构建、debug.keystore
- [x] 构建验证：`decklist-manager-v5.0.1-debug.apk` 构建成功（零错误）
- [x] 模拟器验证：Medium_Phone_API_36.1 上安装运行正常，无崩溃
- [x] 创建开发分支 `dev/v5.1.0`
- [x] 文档治理：80+ 过程文档归档到 `docs/archive/`（git mv，保留历史）
- [x] 新增 GitHub Actions CI（android-ci.yml：JDK 17 + 生成调试证书 + 构建 + 上传 APK）
- [x] **接入 Forge 中文卡查 API**（2026-09-03）
  - 新服务器：`https://play.mtg-forge-kaorou.vip:8443/api/v1/`（34444 卡 / 672 系列，在线）
  - 新增 `forge` 包：ForgeCardApi + ForgeCardDto + ForgeMapper + ForgeCardApiAdapter
  - 适配器实现旧 MtgchApi 接口，上层零改动；新 24 位 id → 旧 oracleId 字段
  - 旧查询语法 → 新结构化参数解析器
  - 模拟器实测：英文关键词命中中文卡牌，中文卡图 CDN 加载正常

- [x] **卡图加载失败回退**（2026-09-03）
  - 新增 `util/CardImageFallbackLoader.kt`：主图 → printings 候选（带缓存）→ Scryfall 最终回退
  - Scryfall 回退：`api.scryfall.com/cards/{set}/{collector}?format=image`（双面牌背面加 `&face=back`）
  - 三个调用点（SearchResultAdapter / CardInfoFragment / CardDetailActivity）传入 setCode/collectorNumber
  - 修复 Glide 崩溃：RequestListener 回调内禁止发起新加载，改用主线程 Handler post
  - 模拟器实测：幸运佩戴者比尔博主图 404 → 自动回退其他版本图正常显示，无崩溃

#### 待完成 📋
- [ ] 补齐代码内 8 处 TODO（ArenaFormatExporter、ScrapingOptionsDialog、FoldersActivity）
- [ ] 补单元测试（当前为零）
- [ ] 发布 v5.1.0 并同步 GitHub Release（目前 Release 停在 4.2.6）

#### 技术要点
- 构建必须用 JDK 17（Gradle 8.1.1 不兼容 JDK 21）：`JAVA_HOME="C:\Program Files\Java\jdk-17\jdk-17.0.18+8"`
- git 访问 GitHub 需 `http.sslBackend=openssl` + `http.sslCAInfo` 指向 Windows 根证书 PEM（已固化在仓库本地配置）
- API 层入口：`MtgchApi.kt`（Retrofit 接口）+ `MtgchMapper.kt`（DTO→Entity），Base URL 在 `AppModule.provideMtgchRetrofit`

---

## 📜 历史任务（v4.3.0，2026-02-10，已被 v5.0.1 取代）

**状态**: ✅ 已完成
**旧版本**: v4.3.0 (开发中)
**整体进度**: 95%

---

## 🚀 当前任务 (2026-02-10)

### v4.3.0 新功能开发

#### 已完成的模块 ✅

**1. 数据库架构设计**
- 创建了 5 个新实体：FolderEntity, TagEntity, DecklistNoteEntity, DecklistFolderRelationEntity, DecklistTagRelationEntity
- 数据库版本从 v10 升级到 v11
- 完成了数据库迁移脚本

**2. 数据访问层 (DAO)**
- FolderDao - 文件夹数据访问
- TagDao - 标签数据访问
- DecklistNoteDao - 备注数据访问
- DecklistFolderRelationDao - 文件夹关联
- DecklistTagRelationDao - 标签关联

**3. 仓库层 (Repository)**
- FolderRepository - 文件夹业务逻辑
- TagRepository - 标签业务逻辑
- DecklistNoteRepository - 备注业务逻辑

**4. 视图模型 (ViewModel)**
- FolderViewModel
- TagViewModel
- DecklistNoteViewModel

**5. UI 组件**
- FoldersActivity - 文件夹管理页面
- FolderAdapter - 文件夹列表适配器
- DecklistTagsBottomSheet - 标签管理弹窗

#### 待完成的模块 📋

**1. 完善 UI 功能**
- 标签选择对话框
- 备注编辑对话框
- 集成到 DeckDetailActivity

**2. 套牌对比功能**
- 创建 DeckComparisonActivity
- 实现卡牌差异对比
- 实现统计信息对比

**3. 卡图组合图片功能**
- 创建图片组合工具类
- 实现卡图拼接逻辑
- 支持导出为图片

**4. 导出增强**
- PDF 导出格式
- HTML 导出格式
- 美化导出样式

**5. 测试与发布**
- 测试所有新功能
- 修复 Bug
- 构建 Release 版本
- 更新文档

---

## 📁 新增文件清单

### 数据层
- `data/local/entity/FolderEntity.kt`
- `data/local/entity/TagEntity.kt`
- `data/local/entity/DecklistNoteEntity.kt`
- `data/local/entity/DecklistFolderRelationEntity.kt`
- `data/local/entity/DecklistTagRelationEntity.kt`
- `data/local/dao/FolderDao.kt`
- `data/local/dao/TagDao.kt`
- `data/local/dao/DecklistNoteDao.kt`
- `data/local/dao/DecklistFolderRelationDao.kt`
- `data/local/dao/DecklistTagRelationDao.kt`
- `data/repository/FolderRepository.kt`
- `data/repository/TagRepository.kt`
- `data/repository/DecklistNoteRepository.kt`

### 领域层
- `domain/model/Folder.kt`
- `domain/model/Tag.kt`
- `domain/model/DecklistNote.kt`

### UI 层
- `ui/folder/FoldersActivity.kt`
- `ui/folder/FolderViewModel.kt`
- `ui/folder/FolderAdapter.kt`
- `ui/tag/TagViewModel.kt`
- `ui/note/DecklistNoteViewModel.kt`
- `ui/decklist/DecklistTagsBottomSheet.kt`

### 资源文件
- `layout/activity_folders.xml`
- `layout/item_folder.xml`
- `layout/dialog_create_folder.xml`
- `layout/bottom_sheet_decklist_tags.xml`
- `drawable/ic_folder.xml`
- `drawable/ic_delete.xml`
- `drawable/ic_close.xml`
- `drawable/ic_add.xml`
- `menu/menu_folders.xml`

---

## 🔧 下一步工作

1. **完善标签功能** - 实现标签选择对话框
2. **实现备注功能** - 创建备注编辑对话框
3. **集成到 DeckDetailActivity** - 添加标签和备注入口
4. **套牌对比功能** - 实现两个套牌的对比
5. **卡图组合功能** - 将套牌卡图拼接成一张图片
6. **导出增强** - 添加 PDF 和 HTML 导出
7. **测试发布** - 完整测试并发布 v4.3.0

---

## 📝 技术要点

- 数据库使用 Room 迁移从 v10 到 v11
- 使用 Hilt 进行依赖注入
- 使用 ViewModel + Repository 模式
- 使用 Material Design 3 组件
- 支持深色模式

---

**最后更新：2026-02-10**

---

## 🎉 刚完成的任务 (2026-02-09)

### v4.2.2 - v4.2.6 深色模式完全优化系列

在2小时内连续发布4个版本，完成深色模式的完全优化：

#### v4.2.3 - 深色模式基础优化 ✅
- 使用纯黑背景 (#121212)
- 优化文本颜色（纯白主文本、浅灰次要文本）
- 改进卡片背景（深灰 #1E1E1E）
- 完整的 Material Design 3 颜色系统

#### v4.2.4 - 界面改进 ✅
- 批量替换所有硬编码白色背景
- 设置改为齿轮图标（显示在 Action Bar）
- 语言选项从主菜单移至设置页面
- 菜单简化（刷新、搜索、设置）

#### v4.2.5 - 进一步优化 ✅
- 修复 Toolbar 弹出菜单主题问题
- 批量替换浅灰色背景（#F5F5F5, #F0F0F0, #E0E0E0）
- 弹出菜单自动适配深色模式

#### v4.2.6 - 修复图表文字 ✅
- 修复法术力曲线图坐标轴文字颜色
- 修复颜色分布饼图标签颜色
- 修复类型分布柱状图坐标轴文字颜色
- 所有图表在深色模式下清晰可见

### 📦 发布信息

| 版本 | 版本代码 | 大小 | 主要功能 |
|------|---------|------|---------|
| v4.2.2 | 80 | 4.6MB | 性能优化 |
| v4.2.3 | 81 | 4.6MB | 深色模式基础优化 |
| v4.2.4 | 82 | 4.6MB | 界面改进与完全优化 |
| v4.2.5 | 83 | 4.6MB | 深色模式进一步优化 |
| v4.2.6 | 84 | 4.6MB | 修复图表文字显示 |

### 📝 最近提交
```
0e16f3c - release: 发布 v4.2.6 - 修复深色模式图表文字显示问题
fe68a23 - release: 发布 v4.2.5 - 深色模式进一步优化
9c56920 - release: 发布 v4.2.4 - 深色模式完全优化与界面改进
ae48f3f - release: 发布 v4.2.3 - 深色模式优化
```

---

## 🌙 深色模式优化总结

### 修复的问题
1. ✅ 大量硬编码白色背景（#FFFFFF, @android:color/white）
2. ✅ 硬编码文本颜色（#000000, #333333, #666666）
3. ✅ 硬编码浅灰色背景（#F5F5F5, #F0F0F0, #E0E0E0）
4. ✅ Toolbar 弹出菜单强制浅色主题
5. ✅ 图表坐标轴文字不可见

### 优化效果
- 🎨 完美的深色模式体验
- 📱 所有 UI 组件自动适配主题
- 📊 所有图表文字清晰可见
- 🌙 舒适的夜间阅读体验
- ✨ 完全符合 Material Design 3 规范

### 技术要点
- 所有颜色使用语义化资源（@color/*）
- 图表使用 ContextCompat.getColor() 获取主题颜色
- 移除强制主题覆盖（如 popupTheme）
- 完整的颜色映射表（深色/浅色自动切换）

---

## 📋 当前无待办任务

所有计划的功能都已完成并测试通过。

### 可选的后续功能 (v4.3.0)
- 卡牌收藏夹分类
- 套牌对比功能
- 云同步功能
- 导出格式增强（PDF、HTML）
- 自定义标签和备注

---

## 🔧 快速继续开发

当需要继续开发时，请按以下步骤操作：

1. **阅读文档**
   ```
   告诉 Claude：
   "请阅读 PROJECT_STATUS.md、SESSION_LOG.md 和 CURRENT_TASK.md，然后继续完成当前任务。"
   ```

2. **查看当前状态**
   - 当前版本: v4.2.6
   - 当前分支: dev/v4.2.0
   - 整体进度: 93%

3. **开始新功能开发**
   - 确定要实现的功能
   - 更新 CURRENT_TASK.md
   - 开始开发
   - 测试并发布

---

## 🚀 快速开始

### 如果要开始新任务

1. **阅读项目状态**
   ```
   在对话开始时说：
   "请阅读 PROJECT_STATUS.md、SESSION_LOG.md 和 CURRENT_TASK.md，然后继续完成当前任务。"
   ```

2. **查看当前代码**
   ```bash
   # 查看最近的提交
   git log --oneline -10

   # 查看当前分支
   git branch

   # 查看未提交的更改
   git status
   ```

3. **开始开发**
   - 从 PROJECT_STATUS.md 中找到要开发的版本
   - 按照 PROJECT_STATUS.md 中的"开发指南"进行
   - 定期更新 CURRENT_TASK.md

---

## 📝 任务模板

当开始新任务时，使用以下模板：

```markdown
## 📅 新任务 (YYYY-MM-DD)

### 任务名称
- **优先级**: 高/中/低
- **预计时间**: X小时
- **状态**: 进行中/待开始/已完成

### 任务描述
简要描述任务目标

### 子任务
- [ ] 子任务1
- [ ] 子任务2
- [ ] 子任务3

### 相关文件
- 文件1
- 文件2

### 技术要点
- 要点1
- 要点2

### 遇到的问题
（记录开发过程中遇到的问题和解决方案）
```

---

## 🔍 问题诊断

### 如果遇到构建问题
```bash
# 清理构建
./gradlew clean

# 重新构建
./gradlew assembleDebug

# 查看详细错误
./gradlew assembleDebug --stacktrace
```

### 如果遇到应用崩溃
```bash
# 查看日志
adb logcat | grep -E "AndroidRuntime|FATAL"

# 查看特定标签
adb logcat | grep "MTGO"
```

### 如果遇到深色模式问题
1. 检查颜色是否使用语义化资源（@color/*）
2. 检查图表是否设置了文字颜色
3. 检查是否有强制主题覆盖（如 popupTheme）
4. 查看 SESSION_LOG.md 的"深色模式优化要点"

---

## 📞 获取帮助

如果遇到问题：
1. 查看 `PROJECT_STATUS.md` 的"已知问题"部分
2. 查看 `SESSION_LOG.md` 的"经验总结"部分
3. 查看 `README.md` 的项目说明
4. 搜索相关代码文件

---

## 📊 深色模式颜色速查表

| 元素 | 颜色资源 | 深色模式值 | 浅色模式值 |
|------|---------|-----------|-----------|
| 背景 | background | #121212 | #FFFBFE |
| 卡片背景 | card_background | #1E1E1E | #FFFFFF |
| 提升背景 | background_elevated | #2C2C2C | #F7F2FA |
| 主文本 | text_primary | #FFFFFF | #1C1B1F |
| 次文本 | text_secondary | #B3B3B3 | #49454F |
| 分隔线 | divider | #2C2C2C | #E7E0EC |

---

**最后更新：2026-02-09 22:00**
