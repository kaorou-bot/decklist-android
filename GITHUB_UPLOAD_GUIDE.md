# GitHub 上传指南

## 📋 步骤 1：在 GitHub 上创建仓库

1. 访问 https://github.com/new
2. 填写仓库信息：
   - **Repository name**: `decklist-android` (或你喜欢的名称)
   - **Description**: `MTGO Decklist Manager - Android app for browsing Magic: The Gathering Online decklists`
   - **Visibility**: ✅ Public 或 ❌ Private
   - **⚠️ 不要勾选**:
     - ❌ Add a README file (我们已经有了)
     - ❌ Add .gitignore (我们已经有了)
     - ❌ Choose a license (可以稍后添加)

3. 点击 **"Create repository"** 按钮

---

## 📋 步骤 2：获取仓库 URL

创建后，GitHub 会显示类似这样的 URL：

```
https://github.com/你的用户名/decklist-android.git
```

**复制这个 URL**（使用 HTTPS）

---

## 📋 步骤 3：连接并推送代码

在你的 WSL 终端执行以下命令：

```bash
cd /home/dministrator/decklist-android

# 添加远程仓库（替换下面的 URL 为你的实际 URL）
git remote add origin https://github.com/你的用户名/decklist-android.git

# 验证远程仓库
git remote -v

# 推送代码到 GitHub
git push -u origin main
```

**如果提示输入用户名和密码**：
- **用户名**: 你的 GitHub 用户名
- **密码**: 使用 **Personal Access Token** (不是你的 GitHub 密码)

---

## 🔑 获取 GitHub Personal Access Token

由于 GitHub 不再支持密码认证，你需要创建一个 Personal Access Token：

1. 访问：https://github.com/settings/tokens
2. 点击 **"Generate new token"** → **"Generate new token (classic)"**
3. 设置：
   - **Note**: `decklist-android`
   - **Expiration**: 选择过期时间（或无过期）
   - **勾选权限**:
     - ✅ `repo` (完整仓库访问权限)
4. 点击 **"Generate token"**
5. **复制 token**（只显示一次！保存好它）

推送时：
- 用户名：你的 GitHub 用户名
- 密码：粘贴这个 token

---

## 🚀 快速命令（准备好后）

```bash
# 1. 进入项目目录
cd /home/dministrator/decklist-android

# 2. 添加远程仓库（替换 URL）
git remote add origin https://github.com/你的用户名/decklist-android.git

# 3. 推送代码
git push -u origin main
```

---

## ✅ 验证上传成功

推送成功后，访问你的 GitHub 仓库页面，应该能看到：
- ✅ 所有源代码文件
- ✅ 文档文件（README.md, DEVELOPER_GUIDE.md 等）
- ✅ 提交历史

---

## 🔄 后续开发

日常开发流程：

```bash
# 修改代码后
git add .
git commit -m "描述你的更改"
git push

# 拉取最新代码
git pull
```

---

## 📝 提示

- **首次推送可能需要认证**：使用 Personal Access Token
- **推送失败**：检查网络连接和 URL 是否正确
- **冲突解决**：使用 `git pull --rebase` 后再推送

---

**准备好了吗？执行上面的命令就可以上传了！** 🎉
