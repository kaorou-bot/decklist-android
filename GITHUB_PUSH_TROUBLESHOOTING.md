# GitHub 推送问题解决方案

## ❌ 当前问题
```
fatal: unable to access 'https://github.com/kaorou-bot/decklist-android.git/':
Failed to connect to github.com port 443 after 123878 ms: Couldn't connect to server
```

## 🔍 问题分析
- ✅ DNS 解析正常（github.com 可以 ping 通）
- ❌ HTTPS 443 端口连接失败
- **可能原因**：
  - 防火墙阻止 443 端口
  - 需要代理服务器
  - SSL/TLS 证书问题
  - 网络环境限制

---

## 🛠️ 解决方案

### 方案 1：配置代理（如果你有代理）

如果你有 HTTP/HTTPS 代理，配置 Git 使用它：

```bash
# 设置代理（替换为你的代理地址和端口）
git config --global http.proxy http://127.0.0.1:7890
git config --global https.proxy http://127.0.0.1:7890

# 然后重新推送
git push -u origin main

# 如果不需要代理了，取消代理
git config --global --unset http.proxy
git config --global --unset https.proxy
```

---

### 方案 2：使用 SSH 方式（推荐）

SSH 通常比 HTTPS 更稳定，且不受 443 端口限制。

#### 步骤 1：生成 SSH 密钥

```bash
# 检查是否已有 SSH 密钥
ls -la ~/.ssh/id_*.pub

# 如果没有，生成新的
ssh-keygen -t ed25519 -C "496291727@qq.com"

# 一路按回车（使用默认路径和无密码）
```

#### 步骤 2：查看并复制公钥

```bash
# 查看公钥内容
cat ~/.ssh/id_ed25519.pub
```

复制输出的内容（从 `ssh-ed25519` 开始的整行）

#### 步骤 3：添加到 GitHub

1. 访问：https://github.com/settings/keys
2. 点击 **"New SSH key"**
3. **Title**: `WSL2 Decklist`
4. **Key**: 粘贴刚才复制的公钥
5. 点击 **"Add SSH key"**

#### 步骤 4：测试 SSH 连接

```bash
# 测试 SSH 连接
ssh -T git@github.com

# 首次会提示 "Are you sure..."，输入 yes
# 成功会显示：Hi kaorou-bot! You've successfully authenticated...
```

#### 步骤 5：更改远程仓库 URL

```bash
cd /home/dministrator/decklist-android

# 删除旧的 HTTPS 远程地址
git remote remove origin

# 添加 SSH 地址
git remote add origin git@github.com:kaorou-bot/decklist-android.git

# 验证
git remote -v

# 推送
git push -u origin main
```

---

### 方案 3：在其他环境推送

如果 WSL2 网络有问题，可以：

#### 选项 A：在 Windows 命令行推送

```cmd
# 在 Windows PowerShell 或 CMD 中
cd C:\Users\你的用户名\decklist-android

# 如果项目不在 Windows 文件系统，先复制过去
# 然后执行同样的 git 命令
git push -u origin main
```

#### 选项 B：使用 GitHub Desktop

1. 下载安装：https://desktop.github.com/
2. 登录你的 GitHub 账号
3. File → Add Local Repository → 选择项目文件夹
4. 点击 "Publish repository"

#### 选项 C：上传压缩包

1. 打包项目：
```bash
cd /home/dministrator
tar czf decklist-android.tar.gz decklist-android/
```

2. 将 `decklist-android.tar.gz` 传输到能访问 GitHub 的电脑

3. 在那里解压并推送

---

### 方案 4：手动创建 GitHub 仓库文件

如果无法推送，你可以：

1. 访问：https://github.com/new
2. 创建仓库：`decklist-android`
3. 在 GitHub 网页上手动上传文件

---

## 🎯 推荐方案

**最简单：使用方案 3B（GitHub Desktop）**
- 图形界面，操作简单
- 自动处理认证问题
- Windows 上网络通常更稳定

**最稳定：使用方案 2（SSH）**
- 一次配置，永久使用
- 不受 HTTPS 限制
- 适合长期开发

---

## 📝 当前状态

你的项目已经：
- ✅ Git 仓库初始化完成
- ✅ 所有文件已提交（2 个提交）
- ✅ 本地开发环境就绪

只需要推送到 GitHub 即可！

---

## 🚀 快速开始（SSH 方式）

```bash
# 1. 生成 SSH 密钥
ssh-keygen -t ed25519 -C "496291727@qq.com"

# 2. 查看公钥
cat ~/.ssh/id_ed25519.pub

# 3. 复制公钥到 GitHub Settings → SSH Keys

# 4. 测试连接
ssh -T git@github.com

# 5. 更改远程地址
cd /home/dministrator/decklist-android
git remote remove origin
git remote add origin git@github.com:kaorou-bot/decklist-android.git

# 6. 推送
git push -u origin main
```

---

需要我帮你执行哪个方案？
