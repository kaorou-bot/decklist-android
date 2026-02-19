# ⚡ 快速测试卡片

> 最常用的测试命令，随时查阅

---

## 🚀 一键部署

```bash
cd /home/dministrator/decklist-android

# 如果使用 WSA，先连接
./connect_wsa.sh

# 一键部署
./deploy_to_windows.sh
```

---

## 📱 常用命令

### 查看设备
```bash
adb devices
```

### 安装 APK
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 卸载应用
```bash
adb uninstall com.mtgo.decklistmanager
```

### 启动应用
```bash
adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity
```

### 停止应用
```bash
adb shell am force-stop com.mtgo.decklistmanager
```

### 清除数据
```bash
adb shell pm clear com.mtgo.decklistmanager
```

---

## 📋 日志命令

### 查看实时日志
```bash
adb logcat | grep -E "Decklist|MTGO"
```

### 查看崩溃日志
```bash
adb logcat -b crash
```

### 清除日志
```bash
adb logcat -c
```

### 保存日志到文件
```bash
adb logcat -v time > app_log.txt
```

---

## 🔧 WSA 连接

### 自动连接
```bash
./connect_wsa.sh
```

### 手动连接
```bash
adb connect 127.0.0.1:58526
```

### 查看连接状态
```bash
adb devices
```

---

## 🐛 调试命令

### 查看 Activity
```bash
adb shell dumpsys activity top
```

### 查看内存
```bash
adb shell dumpsys meminfo com.mtgo.decklistmanager
```

### 查看存储
```bash
adb shell df -h
```

### 查看网络
```bash
adb shell shell netstat
```

---

## 🔄 快速迭代

```bash
# 构建 + 安装 + 启动（一行命令）
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity
```

---

## 💡 遇到问题？

### ADB 无响应
```bash
adb kill-server
adb start-server
```

### 找不到设备
```bash
# 如果是 WSA
./connect_wsa.sh

# 查看所有设备
adb devices -l
```

### 应用崩溃
```bash
# 查看崩溃日志
adb logcat -b crash | grep -A 20 "FATAL"
```

---

## 📞 完整指南

查看详细的测试指南：
```bash
cat WINDOWS_TESTING_GUIDE.md
```

---

**最后更新：** 2026-01-31
