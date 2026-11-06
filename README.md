# TengWear NFC（com.tengwear.nfc）

完全自研的「手机读卡端 + 手表卡模拟端」组合，局域网直连传输，**无三星账号、无 Galaxy Wearable 配对、不修改任何三星应用**。

## 架构

```
┌─────────────────┐  UDP 广播 47812   ┌─────────────────────┐
│  手表端 (wear)   │ ◄─────────────── │  手机读卡端 (phone)   │
│  · TCP 服务 47813│  ── TCP 传输 ──► │  · ReaderMode 读卡    │
│  · HCE 模拟服务  │  (配置 JSON)      │  · UDP 发现手表       │
│  · 动态注册 AID  │                  │  · 发送卡片数据        │
└─────────────────┘                  └─────────────────────┘
```

- **发现**：手表端每 3 秒广播 `TENGWEAR_NFC|<设备名>|<ip>|<tcp端口>` 到 UDP 47812；
- **传输**：手机 TCP 连接手表 47813，协议为 `[4字节大端长度][UTF-8 JSON]`，手表回 1 字节 `0x01` 确认；
- **配置格式**：

```json
{
  "aid": "F001020304",
  "selectResponse": "6F2180012B...9000",
  "defaultResponse": "6D00",
  "responses": [
    {"req": "80CA9F7F00", "res": "9F7F2A...9000"}
  ]
}
```

手表端按规则应答：`00A40400xx`（SELECT）用 `selectResponse`，其余按 `responses` 精确匹配，兜底 `defaultResponse`。

## 构建与安装

APK 命名规则：**`p_` 开头 = 手机版（读卡端），`w_` 开头 = 手表版（模拟端）**，位于 `apk/` 目录。

```bash
# 构建（需 Android Studio 或命令行 Android SDK 34 / AGP 8.5.2）
./gradlew :phone:assembleDebug :wear:assembleDebug

# 安装手机版 p_手机读卡端_v1.0.apk
adb install apk/p_手机读卡端_v1.0.apk

# 安装手表版 w_手表模拟端_v1.0.apk（手表需开启开发者选项 → ADB 调试，且与电脑同网段）
adb connect <手表IP>:5555
adb -s <手表IP>:5555 install apk/w_手表模拟端_v1.0.apk
```

也可以用 WearOS 工具箱等侧载工具安装手表端。

## 使用步骤

1. 手表和手机连**同一个 Wi-Fi**（手表：设置 → 连接 → Wi-Fi）；
2. 打开手表端 App → 显示「运行中，等待手机发送配置…」；
3. 打开手机端 App → 列表出现手表设备，**点击选中（▶）**；
4. 点「开始读卡」，碰 NFC 卡/标签（读取 UID、技术类型、NDEF 文本）；
5. 数据自动经局域网发送到手表，手表端显示「模拟运行中」；
6. 手表靠近支持该 AID 的读头，HCE 服务按配置应答 APDU。

## 模块说明

| 路径 | 内容 |
|---|---|
| `phone/…/MainActivity.java` | 读卡 UI、ReaderMode 回调、NDEF 解析、发送 |
| `phone/…/WatchDiscovery.java` | UDP 47812 发现手表 |
| `phone/…/TcpSender.java` | TCP 长度前缀协议发送 |
| `wear/…/MainActivity.java` | TCP 服务 + 动态 AID 注册（CardEmulation API） |
| `wear/…/EmulatorApduService.java` | HostApduService，APDU 交给规则引擎应答 |
| `wear/…/ConfigStore.java` | 配置持久化 + APDU 匹配 |
| `wear/…/LanServer.java` / `Announcer.java` | 局域网接收 / 广播 |

## 已知限制（务必阅读）

1. **HCE 是 ISO-DEP（ISO 14443-4）协议**：模拟的是 APDU 层。MIFARE Classic 门禁卡的私有扇区认证（Crypto1）无法用 HCE 模拟——这不是本 App 的缺陷，是 Android HCE 的固有边界；ISO 14443-4 / 自定义 AID 场景（如 CPU 卡式交互、演示协议）可用。
2. **手表不能当读卡器**：Galaxy Watch 的 NFC 只开放卡模拟，Reader 模式被系统限制（Google issue #213599626），所以读卡必须由手机端完成——这就是「手机读 + 手表模拟」分工的原因。
3. 默认 AID `F001020304` 为演示值，实际使用时由手机端配置下发并动态注册。

## 合规声明

本工具仅用于学习 NFC 协议、自建设备间传输与自有卡片的实验研究：
- 只读取**你自己拥有**的卡/标签的公开数据（UID、NDEF）；
- 模拟端只应答**自定义 AID 与自行构造的数据**，不冒用真实支付卡、不复制他人门禁卡；
- 不修改、不绕过任何第三方应用（包括三星智能卡）的安全机制；由此产生的滥用责任由使用者自行承担。
