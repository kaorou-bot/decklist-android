# 🚀 MTGO Decklist Manager v4.1.0 发布创建指南

## 自动创建（需要 GitHub Token）

如果您有 GitHub Personal Access Token，可以运行以下命令自动创建 Release：

```bash
# 设置您的 GitHub Token
export GITHUB_TOKEN="your_github_token_here"

# 创建 Release 并上传 APK
gh release create v4.1.0 \
  --title "v4.1.0 - 套牌导出、搜索与双面牌完整支持" \
  --notes-file RELEASE_NOTES_v4.1.0.md \
  app/build/outputs/apk/release/decklist-manager-v4.1.0-release.apk
```

如果没有安装 `gh` CLI，请使用手动创建方法。

---

## 手动创建步骤

### 步骤 1：访问 GitHub Release 页面

打开浏览器，访问：
```
https://github.com/kaorou-bot/decklist-android/releases/new
```

### 步骤 2：填写 Release 信息

#### 基本信息
- **Choose a tag**: 选择 `v4.1.0`
- **Release title**: 填写 `v4.1.0 - 套牌导出、搜索与双面牌完整支持`
- **Description**: 复制下方的内容

#### Release Description（复制以下内容）

```markdown
# MTGO Decklist Manager v4.1.0

## 🎉 主要更新

### 📦 套牌导出功能
- 支持 4 种格式：MTGO、Arena、文本、Moxfield
- 一键导出和分享套牌

### 🔍 在线卡牌搜索
- 完整复制 MTGCH 高级搜索（13个字段）
- 支持中英文搜索
- 搜索历史记录

### 🃏 双面牌完整支持
- 背面名称、类型、规则文本
- 背面力量/防御力、忠诚度
- 中文翻译优化
- 性能优化（< 50ms 加载）

### 🛠️ 重要修复
- ✅ 修复中文名称和法术力值自动修复
- ✅ 修复连体牌匹配（Wear//Tear）
- ✅ 优化法术力符号颜色
- ✅ 优化按钮文案

---

## 📥 下载

**APK 文件**: [decklist-manager-v4.1.0-release.apk](https://github.com/kaorou-bot/decklist-android/releases/download/v4.1.0/decklist-manager-v4.1.0-release.apk) (7.0 MB)

**系统要求**: Android 5.0 (API 21) 或更高版本

---

## 📝 完整更新日志

详见 [RELEASE_NOTES_v4.1.0.md](https://github.com/kaorou-bot/decklist-android/blob/dev/v4.1.0/RELEASE_NOTES_v4.1.0.md)
```

### 步骤 3：上传 APK 文件

1. 在 Release 页面底部，找到 **"Attach binaries"** 区域
2. 点击 **"Choose files"** 或拖放文件
3. 选择文件：`app/build/outputs/apk/release/decklist-manager-v4.1.0-release.apk`
4. 等待上传完成（文件大小：7.0 MB）

### 步骤 4：发布 Release

1. 确认所有信息正确
2. 点击底部的绿色按钮 **"Publish release"**
3. 等待发布完成

---

## ✅ 发布后检查清单

发布完成后，请检查：

- [ ] Release 页面显示正确
- [ ] APK 文件可以下载
- [ ] 下载链接格式正确：
  ```
  https://github.com/kaorou-bot/decklist-android/releases/download/v4.1.0/decklist-manager-v4.1.0-release.apk
  ```

---

## 🎯 快速链接

- Release 页面：https://github.com/kaorou-bot/decklist-android/releases
- 标签列表：https://github.com/kaorou-bot/decklist-android/tags
- 提交历史：https://github.com/kaorou-bot/decklist-android/commits/dev/v4.1.0

---

**祝您使用愉快！🎴**
