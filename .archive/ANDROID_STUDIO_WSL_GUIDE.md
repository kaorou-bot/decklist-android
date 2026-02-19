# 🤖 Android Studio (WSL2) 测试指南

> 在 WSL2 环境中使用 Android Studio 测试 APK

---

## 📋 前提条件检查

### 1. 确认 Android Studio 已安装

```bash
# 检查 Android Studio 是否安装
ls -lh ~/android-studio/bin/studio.sh

# 或检查安装位置
which android-studio
```

### 2. 检查 JAVA 环境

```bash
# 检查 Java 版本
java -version

# 应该显示 Java 17 或更高版本
```

---

## 🚀 方式一：使用 Android Studio 运行（推荐）⭐

### 步骤 1：启动 Android Studio

```bash
cd /home/dministrator/decklist-android

# 启动 Android Studio
~/android-studio/bin/studio.sh &

# 或者
android-studio &
```

### 步骤 2：等待 Android Studio 完全加载

**首次启动可能需要：**
- 索引代码（1-3分钟）
- Gradle 同步（2-5分钟）

### 步骤 3：创建虚拟设备（如果还没有）

#### 3.1 打开 Device Manager
- **Tools → Device Manager**
- 或点击右上角的设备图标

#### 3.2 创建新设备
1. 点击 **Create Device**
2. 选择设备型号：
   - 推荐：**Pixel 6** 或 **Pixel 7**
   - 屏幕：1080p 或更高
3. 选择系统镜像：
   - 推荐：**API 34** (Android 14.0)
   - 或 **API 33** (Android 13)
4. 点击 **Finish** 下载并创建

#### 3.3 启动模拟器
- 在 Device Manager 中点击设备的 **启动按钮** ▶️
- 等待模拟器启动（首次可能较慢）

### 步骤 4：运行应用

#### 方式 A：从 Android Studio 运行

1. **选择设备**
   - 顶部工具栏选择虚拟设备
   - 例如：`Pixel 6 API 34`

2. **点击运行按钮**
   - 点击绿色的 ▶️ 按钮
   - 或按快捷键：`Shift + F10`

3. **等待构建和安装**
   - Android Studio 会自动：
     - 构建 APK
     - 安装到模拟器
     - 启动应用

#### 方式 B：从命令行运行（模拟器已启动时）

```bash
cd /home/dministrator/decklist-android

# 确认模拟器正在运行
adb devices

# 一键构建并安装
./gradlew installDebug

# 启动应用
adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity
```

---

## 🎯 方式二：仅使用命令行（快速迭代）

### 步骤 1：启动模拟器

#### 从 Android Studio 启动
```
Tools → Device Manager → 选择设备 → 点击启动
```

#### 或从命令行启动

```bash
# 查看可用的模拟器
emulator -list-avds

# 启动指定模拟器
emulator -avd Pixel_6_API_34 &

# 或指定更多选项
emulator -avd Pixel_6_API_34 -gpu host &
```

### 步骤 2：验证连接

```bash
# 检查 ADB 设备
adb devices

# 应该看到：
# emulator-5554   device
```

### 步骤 3：构建并安装

```bash
cd /home/dministrator/decklist-android

# 清理旧的构建（可选）
./gradlew clean

# 构建 Debug APK
./gradlew assembleDebug

# 安装到模拟器
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity
```

### 步骤 4：查看日志

```bash
# 清除旧日志
adb logcat -c

# 实时查看应用日志
adb logcat | grep -E "Decklist|MTGO"

# 或保存到文件
adb logcat -v time > app_log.txt &
```

---

## 🔧 配置优化

### 1. 为 WSL2 优化 Android Studio

#### 启用硬件加速（重要！）

```bash
# 检查 GPU 是否可用
glxinfo | grep "OpenGL renderer"

# 如果显示 virgl，说明 GPU 加速可用
```

**在 Android Studio 中设置：**
```
File → Settings → Appearance & Behavior → System Settings →
→ Graphics Backend
选择：ANGLE (Desktop OpenGL)
或：Software Rendering
```

#### 增加 Android Studio 内存

编辑 `~/android-studio/bin/studio.vmoptions`:
```bash
-Xms2048m
-Xmx8192m
-XX:ReservedCodeCacheSize=1024m
```

### 2. 优化模拟器性能

#### 创建高性能设备

**推荐配置：**
- 设备：Pixel 6 或 Pixel 7 Pro
- RAM：2048 MB 或更高
- VM heap：512 MB
- Internal Storage：2048 MB
- SD Card：512 MB
- GPU：Host (硬件加速)
- Boot option：Cold Boot

#### 启动命令优化

```bash
# 使用硬件加速启动
emulator -avd Pixel_6_API_34 \
  -gpu host \
  -memory 2048 \
  -cores 4 \
  -no-snapshot-load &

# 后台运行，不阻塞终端
```

---

## 📱 常用操作

### 应用操作

```bash
# 启动应用
adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity

# 停止应用
adb shell am force-stop com.mtgo.decklistmanager

# 清除应用数据
adb shell pm clear com.mtgo.decklistmanager

# 卸载应用
adb uninstall com.mtgo.decklistmanager

# 重新安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 文件操作

```bash
# 从模拟器拉取文件
adb pull /sdcard/Pictures/test.png ./

# 推送文件到模拟器
adb push ./test.txt /sdcard/

# 进入模拟器 Shell
adb shell

# 在 Shell 中操作
ls /sdcard/
cd /data/data/com.mtgo.decklistmanager/
```

### 网络测试

```bash
# 检查模拟器网络连接
adb shell ping -c 3 google.com

# 检查应用网络权限
adb shell dumpsys package com.mtgo.decklistmanager | grep permission

# 修复网络（如果需要）
adb shell svc wifi disable
adb shell svc wifi enable
```

---

## 🐛 调试技巧

### 1. 查看 Logcat

```bash
# 实时查看所有日志
adb logcat

# 只查看应用日志
adb logcat | grep "com.mtgo.decklistmanager"

# 只查看错误和警告
adb logcat *:W

# 清除日志
adb logcat -c
```

### 2. 使用 Android Studio Debugger

1. **设置断点**
   - 在代码行号左侧点击，设置断点

2. **Debug 模式运行**
   - 点击 🐛 图标
   - 或按 `Shift + F9`

3. **调试操作**
   - F8：单步跳过
   - F7：单步进入
   - F9：继续执行
   - Ctrl+F8：切换断点

### 3. 查看应用详细信息

```bash
# 查看应用 Activity
adb shell dumpsys activity top | grep decklist

# 查看内存使用
adb shell dumpsys meminfo com.mtgo.decklistmanager

# 查看数据库
adb shell "run-as com.mtgo.decklistmanager cat databases/app_database.db" > database.db
sqlite3 database.db
```

---

## ⚡ 快速工作流

### 方案 A：完全使用 Android Studio

```
1. 打开 Android Studio
2. 等待索引完成
3. 点击运行按钮 ▶️
4. 在模拟器中测试
5. 修改代码
6. 重新运行 ▶️
```

### 方案 B：命令行快速迭代（推荐）

```bash
# 终端 1：保持模拟器运行
emulator -avd Pixel_6_API_34 -gpu host &

# 终端 2：持续部署循环
cd /home/dministrator/decklist-android

while true; do
  echo "等待代码修改..."
  read

  echo "构建中..."
  ./gradlew assembleDebug

  echo "安装中..."
  adb install -r app/build/outputs/apk/debug/app-debug.apk

  echo "启动应用..."
  adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity

  echo "完成！按 Enter 继续下一次部署..."
done
```

### 方案 C：最快的一键部署

```bash
# 创建快速部署脚本
cat > /home/dministrator/decklist-android/quick_deploy.sh << 'EOF'
#!/bin/bash
./gradlew assembleDebug && \
adb install -r app/build/outputs/apk/debug/app-debug.apk && \
adb shell am start -n com.mtgo.decklistmanager/.ui.decklist.MainActivity && \
adb logcat -c && \
adb logcat | grep -E "Decklist|MTGO"
EOF

chmod +x /home/dministrator/decklist-android/quick_deploy.sh

# 使用
./quick_deploy.sh
```

---

## 🔥 性能优化建议

### 1. 首次启动优化

```bash
# 预热 Gradle
./gradlew tasks

# 预下载依赖
./gradlew assembleDebug --dry-run
```

### 2. 增量构建

Android Studio 默认启用增量构建，确保 `gradle.properties` 中有：

```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
android.enableJetifier=true
android.useAndroidX=true
```

### 3. 关闭不必要的功能

**在模拟器中：**
- Settings → Display → Sleep → 30 minutes (防止休眠)
- Settings → Security → Screen lock → None (快速测试)

---

## 🎯 针对 v4.0.0 在线模式的测试

### 网络配置

```bash
# 1. 确认模拟器有网络
adb shell ping -c 3 mtgch.com

# 2. 查看应用网络权限
adb shell dumpsys package com.mtgo.decklistmanager | grep -A 20 "granted= true"

# 3. 测试 API 连接
# 在应用中尝试搜索卡牌，查看日志
adb logcat | grep -E "MTGCH|API|HTTP"
```

### 模拟器中配置代理（如果需要）

```bash
# 设置 HTTP 代理
adb shell settings put global http_proxy <proxy_ip>:<port>

# 清除代理
adb shell settings put global http_proxy :0

# 查看当前代理设置
adb shell settings get global http_proxy
```

---

## 📊 模拟器推荐配置

### 配置 A：日常开发（平衡）
```
设备：Pixel 6
RAM：1536 MB
VM Heap：384 MB
内部存储：2048 MB
SD 卡：512 MB
GPU：Host
引导：Cold Boot
```

### 配置 B：高性能测试
```
设备：Pixel 7 Pro
RAM：4096 MB
VM Heap：512 MB
内部存储：4096 MB
SD 卡：1024 MB
GPU：Host
引导：Quick Boot
```

### 配置 C：低性能机器
```
设备：Pixel 5
RAM：1024 MB
VM Heap：256 MB
内部存储：1536 MB
SD 卡：无
GPU：SwiftShader (软件)
引导：Cold Boot
```

---

## 🆘 常见问题

### Q1：模拟器启动很慢

**解决：**
```bash
# 使用 Quick Boot
emulator -avd Pixel_6_API_34 -gpu host -no-snapshot &

# 或保持模拟器一直运行，不要关闭
```

### Q2：Gradle 构建很慢

**解决：**
```bash
# 启用 Gradle 守护进程
./gradlew --stop

# 清理并重新构建
./gradlew clean
./gradlew assembleDebug --info
```

### Q3：ADB 连接失败

**解决：**
```bash
# 重启 ADB
adb kill-server
adb start-server

# 验证连接
adb devices
```

### Q4：应用无法访问网络

**解决：**
```bash
# 检查网络权限
adb shell dumpsys package com.mtgo.decklistmanager | grep INTERNET

# 检查网络状态
adb shell settings get global airplane_mode_on

# 关闭飞行模式
adb shell settings put global airplane_mode_on 0
```

### Q5：应用崩溃

**解决：**
```bash
# 查看崩溃日志
adb logcat -b crash

# 查看完整日志
adb logcat -v time > crash_log.txt

# 清除应用数据重试
adb shell pm clear com.mtgo.decklistmanager
```

---

## 🎓 总结

### 推荐工作流程

```
日常开发：
1. 启动 Android Studio
2. 启动模拟器（Device Manager）
3. 修改代码
4. 点击运行按钮 ▶️
5. 查看日志和结果

快速测试：
1. 模拟器保持运行
2. 修改代码
3. ./quick_deploy.sh
4. 查看结果
```

### 关键命令

```bash
# 启动模拟器
emulator -avd Pixel_6_API_34 -gpu host &

# 一键部署
./quick_deploy.sh

# 查看日志
adb logcat | grep -E "Decklist|MTGO"
```

---

**创建时间：** 2026-01-31
**环境：** WSL2 + Android Studio
**适用版本：** v4.0.0+

**祝你测试顺利！** 🚀
