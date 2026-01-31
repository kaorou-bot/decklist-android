# 🪟 Windows 测试环境搭建指南

> 在 Windows 上快速测试 Android APK 的完整指南

---

## 🎯 快速开始（3种方式）

### 方式 1：Windows Subsystem for Android (WSA) ⭐ 推荐

**最适合日常测试**

#### 第一步：安装 WSA

```powershell
# 在 Windows PowerShell (管理员) 中运行

# 方法 A：从 Microsoft Store 安装（推荐）
winget install "9P3395V91RFP"

# 方法 B：直接下载
# 访问：https://apps.microsoft.com/store/detail/windows-subsystem-for-android/9p3395v91rfp
```

#### 第二步：配置 WSA

1. **打开 WSA**
   - 开始菜单 → Windows Subsystem for Android

2. **启用开发者模式**
   - Settings → 开发者模式 → 开启

3. **启用 USB 调试**
   - Settings → 允许来自这台电脑的 USB 调试 → 开启

4. **记下 IP 地址**
   - 通常显示为：127.0.0.1:58526

#### 第三步：从 WSL2 连接

```bash
# 在 WSL2 中运行
cd /home/dministrator/decklist-android

# 运行 WSA 连接脚本
./connect_wsa.sh

# 如果自动连接失败，手动连接：
adb connect 127.0.0.1:58526

# 验证连接
adb devices
```

#### 第四步：部署应用

```bash
# 一键部署
./deploy_to_windows.sh
```

---

### 方式 2：Android Studio Emulator

**最适合开发调试**

#### 第一步：下载 Android Studio

```
https://developer.android.com/studio
```

#### 第二步：安装并创建虚拟设备

1. **安装 Android Studio**
   - 运行安装程序
   - 选择 "Standard" 安装

2. **创建虚拟设备**
   - Tools → Device Manager → Create Device
   - 选择设备：Pixel 6 或 Pixel 7
   - 选择系统镜像：API 34 (Android 14)
   - 完成

3. **启动模拟器**
   - 在 Device Manager 中点击启动按钮

#### 第三步：从 WSL2 连接

```bash
# Android Studio Emulator 通常会自动桥接到 ADB
# 在 WSL2 中：

cd /home/dministrator/decklist-android

# 验证连接
adb devices

# 部署应用
./deploy_to_windows.sh
```

---

### 方式 3：真实 Android 设备

**最准确的测试环境**

#### 第一步：启用开发者模式

1. **打开设置**
   - 设置 → 关于手机
   - 连续点击「版本号」7次

2. **启用开发者选项**
   - 设置 → 系统 → 开发者选项
   - USB 调试 → 开启

#### 第二步：连接到电脑

1. **USB 连接**
   - 使用 USB 数据线连接手机到电脑

2. **允许 USB 调试**
   - 手机弹出提示 → 允许 USB 调试

#### 第三步：部署

```bash
cd /home/dministrator/decklist-android

# 验证连接
adb devices

# 部署应用
./deploy_to_windows.sh
```

---

## 🚀 一键部署脚本使用

### deploy_to_windows.sh 功能

该脚本会自动执行以下步骤：

1. ✅ 构建 APK
2. ✅ 检查 ADB
3. ✅ 检测设备
4. ✅ 卸载旧版本
5. ✅ 安装新 APK
6. ✅ 启动应用
7. ✅ 显示实时日志

### 使用方法

```bash
cd /home/dministrator/decklist-android

# 方式 1：先连接 WSA，再部署
./connect_wsa.sh
./deploy_to_windows.sh

# 方式 2：直接部署（如果设备已连接）
./deploy_to_windows.sh
```

### 日志查看

脚本会自动显示应用日志，包括：
- 📱 应用生命周期日志
- 🐛 错误和警告
- 💡 调试信息

按 `Ctrl+C` 停止查看日志。

---

## 🛠️ 常见问题

### Q1：ADB 找不到设备

**原因：** 设备未正确连接或 USB 调试未启用

**解决：**
```bash
# 1. 检查 ADB 服务
adb start-server

# 2. 查看设备列表
adb devices -l

# 3. 如果使用 WSA，手动连接
adb connect 127.0.0.1:58526

# 4. 如果使用真实设备，检查：
#    - USB 线是否正常
#    - USB 调试是否启用
#    - 是否授权了调试权限
```

---

### Q2：WSA 无法连接

**原因：** WSA 未正确配置或端口错误

**解决：**
```powershell
# Windows PowerShell 中：

# 1. 重启 WSA
wsl --shutdown
# 然后重新打开 WSA

# 2. 检查 WSA 设置
# 确保：
#   - 开发者模式已开启
#   - USB 调试已启用

# 3. 查看 WSA IP
# 在 WSA 中查看显示的 IP 地址和端口
```

```bash
# WSL2 中尝试不同端口
adb connect 127.0.0.1:58526
adb connect 127.0.0.1:58525
adb connect 127.0.0.1:58524
```

---

### Q3：APK 安装失败

**原因：** 存储空间不足或签名问题

**解决：**
```bash
# 1. 检查设备存储空间
adb shell df -h

# 2. 卸载旧版本
adb uninstall com.mtgo.decklistmanager

# 3. 重新安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. 如果还是失败，查看详细错误
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### Q4：应用启动后崩溃

**原因：** 代码错误或配置问题

**解决：**
```bash
# 查看崩溃日志
adb logcat -b crash

# 查看应用日志
adb logcat | grep -E "Decklist|MTGO|FATAL"

# 清除应用数据后重试
adb shell pm clear com.mtgo.decklistmanager
adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity
```

---

## 📊 设备选择建议

| 设备类型 | 优点 | 缺点 | 推荐场景 |
|---------|------|------|---------|
| **WSA** | 快速、方便、性能好 | 需要 Windows 11 | 日常测试 ⭐⭐⭐⭐⭐ |
| **Android Studio** | 功能完整、可调试 | 占资源大 | 深度调试 ⭐⭐⭐⭐ |
| **真实设备** | 最准确 | 需要硬件 | 最终测试 ⭐⭐⭐⭐⭐ |

---

## 💡 推荐工作流

### 日常开发
```bash
# 1. 启动 WSA
# (Windows 中打开 WSA)

# 2. 连接 WSA
./connect_wsa.sh

# 3. 部署并测试
./deploy_to_windows.sh
```

### 调试问题
```bash
# 1. 清除日志
adb logcat -c

# 2. 重启应用
adb shell am force-stop com.mtgo.decklistmanager
adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity

# 3. 查看日志
adb logcat | grep -E "Decklist|MTGO"
```

### 快速迭代
```bash
# 修改代码后一键部署
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity
```

---

## 🎯 针对 MTGO Decklist Manager 的测试建议

### 必测功能

1. **在线模式测试**
   - 确保 WSA/设备有网络连接
   - 测试卡牌查询功能
   - 测试赛事下载功能

2. **中文显示**
   - 检查卡牌中文名是否正确显示
   - 检查 UI 中文字体

3. **图片加载**
   - 检查卡牌图片是否正常显示
   - 检查双面牌图片

### 性能测试

```bash
# 查看应用内存使用
adb shell dumpsys meminfo com.mtgo.decklistmanager

# 查看 CPU 使用
adb shell top -n 1 | grep decklist

# 查看应用启动时间
adb shell am start -W -n com.mtgo.decklistmanager/.ui.decklist.MainActivity
```

---

## 📞 获取帮助

如果遇到问题：

1. **查看详细日志**
   ```bash
   adb logcat -v time > app_log.txt
   ```

2. **检查设备信息**
   ```bash
   adb devices -l
   adb shell getprop ro.build.version.release
   ```

3. **重启 ADB**
   ```bash
   adb kill-server
   adb start-server
   ```

---

**创建时间：** 2026-01-31
**适用版本：** v4.0.0+
**测试环境：** WSL2 + Windows 11

---

**祝你测试顺利！** 🚀
