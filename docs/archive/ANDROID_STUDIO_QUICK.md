# ⚡ Android Studio 快速参考

> 在 WSL2 中使用 Android Studio 测试 APK 的快速指南

---

## 🚀 最快方式（推荐）

### 第一次使用

```bash
# 1. 启动 Android Studio
android-studio &

# 2. 等待完全加载（首次较慢）

# 3. 创建虚拟设备
# Tools → Device Manager → Create Device
# 选择：Pixel 6 + API 34

# 4. 启动模拟器
# 在 Device Manager 中点击启动按钮 ▶️

# 5. 运行应用
# 点击绿色的运行按钮 ▶️ (或 Shift+F10)
```

### 日常开发（快速迭代）

```bash
# 方式 1：从 Android Studio
修改代码 → 点击运行按钮 ▶️

# 方式 2：从命令行（更快）
cd /home/dministrator/decklist-android
./quick_deploy.sh
```

---

## 📱 模拟器操作

### 启动模拟器

**从 Android Studio：**
```
Tools → Device Manager → 选择设备 → 点击启动 ▶️
```

**从命令行：**
```bash
# 查看可用设备
emulator -list-avds

# 启动设备
emulator -avd Pixel_6_API_34 -gpu host &

# 后台运行，不阻塞终端
```

### 保持模拟器运行

**重要！** 第一次启动后，**保持模拟器一直运行**，不要关闭。
这样可以：
- ✅ 避免重复启动等待
- ✅ 快速部署测试
- ✅ 提高开发效率

---

## 🔧 一键部署脚本

### 使用方法

```bash
cd /home/dministrator/decklist-android

# 运行快速部署
./quick_deploy.sh
```

**脚本会自动：**
1. ✅ 检查模拟器是否运行
2. ✅ 构建 APK
3. ✅ 安装到模拟器
4. ✅ 启动应用
5. ✅ 显示实时日志

按 `Ctrl+C` 退出日志查看（应用继续运行）

---

## 📋 常用命令

### 设备检查

```bash
# 查看连接的设备
adb devices

# 查看设备详情
adb devices -l
```

### 应用操作

```bash
# 启动应用
adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity

# 停止应用
adb shell am force-stop com.mtgo.decklistmanager

# 清除数据
adb shell pm clear com.mtgo.decklistmanager

# 卸载
adb uninstall com.mtgo.decklistmanager
```

### 日志查看

```bash
# 实时日志
adb logcat | grep -E "Decklist|MTGO"

# 清除日志
adb logcat -c

# 保存日志
adb logcat -v time > log.txt &
```

---

## ⚡ 日常开发流程

### 流程 A：完全使用 Android Studio

```
1. 启动 Android Studio
2. 启动模拟器（Device Manager）
3. 修改代码
4. 点击运行按钮 ▶️
5. 查看结果
6. 重复步骤 3-5
```

### 流程 B：混合方式（推荐）

```
1. 启动 Android Studio（用于代码编辑）
2. 启动模拟器（保持运行）
3. 修改代码
4. 在终端运行：./quick_deploy.sh
5. 查看日志和结果
6. 重复步骤 3-5
```

### 流程 C：纯命令行（最快）

```bash
# 终端 1：保持模拟器运行
emulator -avd Pixel_6_API_34 -gpu host &

# 终端 2：代码编辑和部署
cd /home/dministrator/decklist-android

# 编辑代码
vim app/src/main/java/...

# 快速部署
./quick_deploy.sh
```

---

## 🎯 性能优化

### 模拟器启动选项

```bash
# 使用硬件加速（推荐）
emulator -avd Pixel_6_API_34 -gpu host &

# 分配更多内存
emulator -avd Pixel_6_API_34 -memory 2048 -cores 4 &

# 快速启动
emulator -avd Pixel_6_API_34 -no-snapshot-load &
```

### Android Studio 优化

**增加内存：**
```
File → Settings → Appearance & Behavior → Memory Settings
将 Memory Limit 调整到：4096 MB 或更高
```

**启用增量编译：**
```
File → Settings → Build, Execution, Deployment → Compiler
勾选：Compile independent modules in parallel
勾选：Rebuild on dependency changes
```

---

## 🐛 调试技巧

### 1. 使用断点调试

1. 在代码行号左侧点击设置断点
2. 点击 🐛 Debug 按钮（或 Shift+F9）
3. 应用会在断点处暂停
4. F8：下一步，F7：进入，F9：继续

### 2. 查看日志

```bash
# 只看应用日志
adb logcat | grep "com.mtgo.decklistmanager"

# 只看错误
adb logcat *:E

# 过滤关键词
adb logcat | grep -E "MTGCH|API|HTTP"
```

### 3. 查看崩溃信息

```bash
# 查看崩溃缓冲区
adb logcat -b crash

# 保存完整日志
adb logcat -v time > crash_log.txt
```

---

## 🔥 针对在线模式测试

### 网络测试

```bash
# 测试模拟器网络
adb shell ping -c 3 mtgch.com

# 测试应用网络权限
adb shell dumpsys package com.mtgo.decklistmanager | grep INTERNET

# 查看网络日志
adb logcat | grep -E "MTGCH|API|HTTP|Network"
```

### 清除数据重新测试

```bash
# 停止应用
adb shell am force-stop com.mtgo.decklistmanager

# 清除数据（删除缓存等）
adb shell pm clear com.mtgo.decklistmanager

# 重新启动
adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity
```

---

## 🆘 常见问题

### Q：模拟器启动慢

**A：** 保持模拟器一直运行，不要关闭

### Q：Gradle 构建慢

**A：** 首次构建后，Gradle 会缓存，后续会快很多

### Q：找不到设备

**A：**
```bash
# 重启 ADB
adb kill-server
adb start-server

# 确认模拟器正在运行
adb devices
```

### Q：应用崩溃

**A：**
```bash
# 查看崩溃日志
adb logcat -b crash | grep -A 20 "FATAL"

# 清除数据重试
adb shell pm clear com.mtgo.decklistmanager
```

---

## 📚 完整指南

查看详细指南：
```bash
cat ANDROID_STUDIO_WSL_GUIDE.md
```

---

## ✅ 快速检查清单

每次测试前确认：

- [ ] Android Studio 已启动
- [ ] 模拟器正在运行
- [ ] 已选择正确的设备
- [ ] 模拟器有网络连接（在线模式需要）

---

**最后更新：** 2026-01-31
**环境：** WSL2 + Android Studio

**祝你测试顺利！** 🚀
