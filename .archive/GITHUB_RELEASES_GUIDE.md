# GitHub Releases 配置指南

## 📦 当前版本信息
- **版本**: v3.11.1
- **版本代码**: 71
- **APK 大小**: 36 MB
- **数据库**: 66,504 张卡牌 (内置在 APK 中)

---

## 🚀 如何配置 GitHub Releases

### 方法一：通过 GitHub 网页界面（推荐）

#### 步骤 1: 准备 APK 文件
APK 文件位置：
```
app/build/outputs/apk/debug/decklist-manager-v3.11.1-debug.apk
```

#### 步骤 2: 访问 GitHub Releases 页面
1. 打开你的 GitHub 仓库
2. 点击右侧的 **Releases** 链接
3. 点击 **Draft a new release** 按钮

#### 步骤 3: 填写 Release 信息

**Tag version（版本标签）**:
```
v3.11.1
```

**Release title（发布标题）**:
```
MTG 套牌管理器 v3.11.1 - 完整数据库版本
```

**Describe this release（发布说明）**:
```markdown
## 🎉 新功能
- ✅ 完整卡牌数据库：66,504 张卡牌内置
- ✅ 日期分组赛事列表：按日期清晰分组显示
- ✅ 数据库导入进度条：实时显示导入进度
- ✅ 筛选状态栏：清晰显示当前赛制和日期筛选
- ✅ 日期格式统一：统一为 yyyy-MM-dd 格式

## 📦 下载
- **APK (36 MB)**: 完整数据库内置，离线可用
- 支持 Android 5.0+ (API 21+)

## 🔧 技术细节
- **版本**: v3.11.1 (build 71)
- **最小 SDK**: 21
- **目标 SDK**: 34
- **数据库**: 66,504 张卡牌 (压缩后约 36 MB)

## 🐛 修复问题
- 修复数据库导入无进度显示问题
- 修复赛制按钮默认不显示问题
- 修复日期格式不一致问题
- 修复部分卡牌详情无法打开问题
```

#### 步骤 4: 附加 APK 文件

在 **Binary attachment** 区域：
1. 点击 **Attach binaries** 或 **Choose files**
2. 选择 APK 文件：
   ```
   app/build/outputs/apk/debug/decklist-manager-v3.11.1-debug.apk
   ```
3. 等待上传完成

#### 步骤 5: 设置 Release 选项

- ✅ **Set as the latest release**（设为最新版本）
- ⚠️ **Set as a pre-release**（不勾选，除非是测试版）

#### 步骤 6: 发布 Release

点击底部绿色按钮 **Publish release**

---

### 方法二：通过 GitHub CLI (gh)

如果你安装了 [GitHub CLI](https://cli.github.com/)：

```bash
# 创建 release
gh release create v3.11.1 \
  --title "MTG 套牌管理器 v3.11.1 - 完整数据库版本" \
  --notes "## 🎉 新功能
- ✅ 完整卡牌数据库：66,504 张卡牌内置
- ✅ 日期分组赛事列表
- ✅ 数据库导入进度条
- ✅ 筛选状态栏
- ✅ 日期格式统一

## 📦 下载
- **APK (36 MB)**: 完整数据库内置，离线可用
- 支持 Android 5.0+ (API 21+)" \
  app/build/outputs/apk/debug/decklist-manager-v3.11.1-debug.apk
```

---

## 📋 版本命名规范

建议遵循 [语义化版本](https://semver.org/):

- **主版本号** (Major): 不兼容的 API 修改
  - 示例: 3.11.1 → 4.0.0

- **次版本号** (Minor): 向下兼容的功能性新增
  - 示例: 3.11.1 → 3.12.0

- **修订号** (Patch): 向下兼容的问题修正
  - 示例: 3.11.1 → 3.11.2

**当前版本说明**:
- 3.11.0 → 3.11.1: 问题修复版本
- 保留数据库但修复所有用户报告的问题

---

## 🔄 更新现有 Release

如果需要更新已发布的 Release：

### 更新 APK 文件
1. 进入 Release 页面
2. 点击 **Edit release**
3. 在 **Binaries** 区域删除旧的 APK
4. 上传新的 APK
5. 更新 Release notes（如果有变更）
6. 点击 **Update release**

### 删除旧版本
1. 进入 Release 页面
2. 点击右上角的 **Delete release**
3. 确认删除

⚠️ **注意**: 删除 Release 不会删除 Git tag，需要手动删除：
```bash
git tag -d v3.11.0
git push origin :refs/tags/v3.11.0
```

---

## 📊 Release 检查清单

发布前确认：
- [ ] APK 文件已测试可安装
- [ ] 版本号已更新 (versionCode & versionName)
- [ ] CHANGELOG.md 已更新
- [ ] Release notes 已填写
- [ ] APK 文件已附加
- [ ] 标签版本号正确 (v3.11.1)

---

## 🔗 快速链接

### 你的仓库链接
```
https://github.com/kaorou-bot/decklist-android
```

### Releases 页面
```
https://github.com/kaorou-bot/decklist-android/releases
```

### 最新版本下载
```
https://github.com/kaorou-bot/decklist-android/releases/latest
```

---

## 📱 用户安装方式

### 方式 1: 从 GitHub Releases 下载（推荐）
1. 访问 Releases 页面
2. 下载最新的 APK 文件
3. 在手机上打开并安装

### 方式 2: 扫描二维码
1. 在 GitHub Release 页面找到 APK 下载链接
2. 使用二维码生成器生成二维码
3. 用户扫描二维码下载

### 方式 3: 直接分享 APK
```bash
# 生成下载链接
echo "https://github.com/kaorou-bot/decklist-android/releases/download/v3.11.1/decklist-manager-v3.11.1-debug.apk"
```

---

## 💡 提示

### APK 签名（可选）
如果要发布到 Google Play，需要对 APK 进行签名：

```bash
# 创建密钥库（仅需一次）
keytool -genkey -v -keystore mtg-decklist.keystore -alias mtgdecklist -keyalg RSA -keysize 2048 -validity 10000

# 签名 APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore mtg-decklist.keystore \
  app/build/outputs/apk/debug/decklist-manager-v3.11.1-debug.apk mtgdecklist

# 验证签名
jarsigner -verify -verbose -certs mtg-decklist.keystore \
  app/build/outputs/apk/debug/decklist-manager-v3.11.1-debug.apk
```

### 自动化脚本
创建 `release.sh` 脚本自动化发布：

```bash
#!/bin/bash
VERSION="v3.11.1"
APK_PATH="app/build/outputs/apk/debug/decklist-manager-${VERSION}-debug.apk"

# 构建
./gradlew assembleDebug

# 创建 Release
gh release create $VERSION \
  --title "MTG 套牌管理器 ${VERSION}" \
  --notes-file CHANGELOG.md \
  $APK_PATH

echo "✅ Release $VERSION 发布成功！"
```

使用方式：
```bash
chmod +x release.sh
./release.sh
```

---

**准备好了吗？** 按照上面的步骤，你就可以轻松创建 GitHub Release 并让用户下载你的应用！
