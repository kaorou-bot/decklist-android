# MTGO Decklist Manager - 构建指南

## 方法一：使用 Android Studio（推荐）

### 1. 安装 Android Studio

如果尚未安装，请从官网下载：
https://developer.android.com/studio

### 2. 打开项目

```bash
# 启动 Android Studio
# 打开菜单：File -> Open
# 选择目录：/home/dministrator/decklist-android
```

### 3. 等待 Gradle 同步

首次打开项目时，Android Studio 会：
- 自动下载 Gradle
- 下载依赖库
- 同步项目

这可能需要 5-15 分钟，取决于网络速度。

### 4. 构建并运行

- 连接 Android 设备（启用 USB 调试）
- 或启动 Android 模拟器
- 点击工具栏的 "Run" 按钮（绿色三角形）
- 或按快捷键：Shift + F10

### 5. 生成 APK

```bash
# 在 Android Studio 中
# 菜单：Build -> Build Bundle(s) / APK(s) -> Build APK(s)
```

APK 文件将生成在：
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 方法二：使用命令行

### 前置条件

1. 安装 JDK 17
2. 安装 Android SDK
3. 配置 ANDROID_HOME 环境变量
4. 安装 Gradle（可选，项目会自动下载）

### 初始化 Gradle Wrapper

```bash
cd /home/dministrator/decklist-android

# 如果系统有 gradle，可以手动初始化 wrapper
gradle wrapper
```

### 构建项目

```bash
# 1. 清理旧的构建
./gradlew clean

# 2. 构建 Debug APK
./gradlew assembleDebug

# 3. 构建 Release APK
./gradlew assembleRelease
```

### 安装到设备

```bash
# 连接设备后
./gradlew installDebug
```

---

## 方法三：使用系统 Gradle

如果系统已安装 Gradle：

```bash
cd /home/dministrator/decklist-android

# 使用系统的 gradle 构建
gradle clean assembleDebug
```

---

## 输出文件位置

### Debug APK
```
app/build/outputs/apk/debug/
├── app-debug.apk
└── output-metadata.json
```

### Release APK
```
app/build/outputs/apk/release/
├── app-release-unsigned.apk
└── output-metadata.json
```

### AAB (Google Play 格式)
```
app/build/outputs/bundle/release/
└── app-release.aab
```

---

## 常见问题

### 1. Gradle 下载失败

**解决方案**：
- 检查网络连接
- 使用 VPN 或代理
- 手动下载 Gradle 并放到 `~/.gradle/wrapper/dists/`

### 2. 依赖下载失败

**解决方案**：
```bash
# 清理并重新下载
./gradlew clean --refresh-dependencies
```

### 3. 编译错误

**解决方案**：
```bash
# 清理构建缓存
./gradlew clean cleanBuildCache

# 重新构建
./gradlew assembleDebug
```

### 4. SDK 版本问题

确保安装了：
- Android SDK Build-Tools 34.0.0
- Android SDK Platform 34
- Android SDK Platform-Tools

---

## 构建变体

### Debug 版本
```bash
./gradlew assembleDebug
```
- 包含调试信息
- 使用 debug 密钥签名
- 未优化代码

### Release 版本
```bash
./gradlew assembleRelease
```
- 不包含调试信息
- 需要配置签名
- 代码混淆和优化

---

## 性能优化

### 加速构建

在 `gradle.properties` 中添加：
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.daemon=true
org.gradle.caching=true
```

### 离线模式

如果依赖已下载：
```bash
./gradlew assembleDebug --offline
```

---

## 签名配置（Release）

### 1. 生成密钥库

```bash
keytool -genkey -v -keystore my-release-key.keystore \
    -alias my-key-alias \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000
```

### 2. 配置签名

在 `app/build.gradle` 中添加：
```gradle
android {
    signingConfigs {
        release {
            storeFile file("my-release-key.keystore")
            storePassword "your_password"
            keyAlias "my-key-alias"
            keyPassword "your_password"
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

---

## 下一步

构建成功后：
1. 在 Android 设备上测试应用
2. 检查所有功能是否正常
3. 修复发现的 Bug
4. 准备发布到应用商店

---

## 需要帮助？

如果遇到问题，请检查：
1. Android Studio 日志
2. Gradle 构建日志
3. Logcat 输出
4. 开发文档：DEVELOPER_GUIDE.md
