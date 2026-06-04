# 小米球管理 (XiaoMiQiuManager)
# 小米球官网 (https://manager.xiaomiqiu.com)
一款用于管理小米球（XiaoMiQiu）网络代理/隧道服务的 Android 管理工具，采用 Material You 现代设计语言，界面美观、操作便捷。

---

## 功能特性

| 功能 | 说明 |
|------|------|
| 服务状态监控 | 实时监控 `xiaomiqiu` 进程的运行状态 |
| 一键启停 | 通过主界面按钮或快捷设置磁贴（Quick Settings Tile）启动/停止服务 |
| 多配置管理 | 支持保存多个服务器配置，可切换默认配置 |
| 二进制文件管理 | 从应用 assets 中释放 `xiaomiqiu` 可执行文件到设备指定目录，支持 MD5 校验 |
| 配置文件写入 | 自动将服务器地址和认证 Token 写入配置文件 |
| 开机自启 | 设备重启后自动启动监控服务 |
| 前台通知 | 通过前台服务通知持续显示服务运行状态 |

---

## 界面预览

应用采用 Material You 设计风格：

- **顶部渐变标题栏**：紫色渐变背景，现代感十足
- **状态卡片**：彩色状态指示器 + 大字体状态显示 + 图标按钮
- **配置卡片**：带图标的信息列表 + 圆角选择器
- **配置列表项**：左侧图标 + 名称/地址 + "默认"标签 + 右侧箭头
- **空状态提示**：无配置时显示友好的插图和引导

---

## 技术栈

| 层级 | 技术/库 |
|------|---------|
| 开发语言 | Java 21 |
| 构建工具 | Gradle（Android Gradle Plugin 8.7.0）|
| 编译 SDK | 34（Android 14）|
| 最低 SDK | 24（Android 7.0）|
| UI 框架 | Android XML Layout + Material Design Components |
| 依赖库 | AndroidX AppCompat、Material Components、ConstraintLayout、Preference、Gson |
| 数据存储 | SharedPreferences（JSON 序列化）|
| Root 操作 | `su` 命令执行（ProcessBuilder）|

---

## 项目结构

```
app/src/main/
├── assets/
│   └── xiaomiqiu              # 小米球二进制可执行文件
├── java/com/xiaomiqiu/manager/
│   ├── model/
│   │   └── Config.java        # 配置数据模型
│   ├── service/
│   │   ├── BootReceiver.java  # 开机广播接收器
│   │   └── MonitorService.java# 前台监控服务
│   ├── tile/
│   │   └── QuickTileService.java # 快捷设置磁贴服务
│   ├── ui/
│   │   ├── ConfigEditActivity.java # 配置编辑界面
│   │   └── MainActivity.java  # 主界面
│   ├── utils/
│   │   ├── ConfigManager.java # 配置管理器
│   │   ├── NotificationHelper.java # 通知辅助类
│   │   └── RootUtils.java     # Root 命令执行工具
│   └── XiaoMiQiuApp.java      # 自定义 Application
├── res/
│   ├── drawable/              # 矢量图标和背景
│   ├── layout/                # 界面布局文件
│   ├── menu/                  # 菜单资源
│   ├── values/
│   │   ├── colors.xml         # 颜色方案
│   │   ├── themes.xml         # 主题配置
│   │   └── strings.xml        # 字符串资源
│   └── xml/                   # 快捷设置磁贴配置
└── AndroidManifest.xml        # 应用清单
```

---

## 构建与运行

### 环境要求

- Android Studio Hedgehog 或更高版本
- JDK 21
- Android SDK 34

### 构建步骤

```bash
# 1. 克隆项目
git clone <repository-url>
cd XiaoMiQiuManager

# 2. 使用 Gradle 构建
./gradlew assembleDebug

# 3. 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 使用说明

### 首次使用

1. **确保设备已 Root** — 应用所有核心功能均依赖 Root 权限
2. **添加配置** — 点击右下角 "+" 按钮，填写服务器地址和 Token
3. **启动服务** — 选择配置后，点击"启动服务"按钮

### 配置管理

- **新建配置**：点击右下角浮动按钮
- **编辑配置**：点击配置列表项
- **设为默认**：长按配置项，选择"设为默认"
- **删除配置**：长按配置项，选择"删除"

### 快捷操作

- **快捷设置磁贴**：下拉通知栏，点击磁贴一键启停服务
- **开机自启**：重启设备后自动启动监控服务

---

## 权限说明

| 权限 | 用途 |
|------|------|
| `POST_NOTIFICATIONS` | 发送前台服务通知 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | 前台服务保活 |
| `RECEIVE_BOOT_COMPLETED` | 开机自启动 |
| `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE` | 外部存储读写 |

---

## 注意事项

1. **必须 Root**：应用功能高度依赖设备 Root 权限，未 Root 设备无法正常使用
2. **二进制文件**：首次启动时会从 APK assets 释放 `xiaomiqiu` 到 `/data/local/tmp/xmq/`
3. **SELinux**：如遇权限问题，确保目标路径符合 SELinux 策略

---

## 开源协议

本项目仅供学习交流使用。

---

## 致谢

- [Material Design Components](https://github.com/material-components/material-components-android)
- [AndroidX](https://developer.android.com/jetpack/androidx)
