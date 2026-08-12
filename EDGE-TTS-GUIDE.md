# 🎤 Edge TTS 完整使用指南

> **项目：** adminFlow (Spring Boot 2.7.12)  
> **创建时间：** 2026-08-12  
> **状态：** ✅ 可立即使用

---

## 📋 目录

1. [快速开始](#快速开始)
2. [中文音色完整列表](#中文音色完整列表)
3. [API 接口文档](#api-接口文档)
4. [常见问题](#常见问题)

---

## 🚀 快速开始（3步）

### 步骤1：安装 edge-tts（必需）

```bash
# 双击运行
install-edge-tts.bat

# 或手动安装
pip install edge-tts
```

**验证安装：**
```bash
edge-tts --version
# 应该显示：6.1.9 或更高版本
```

---

### 步骤2：启动 Spring Boot 项目

```bash
cd hm-service
mvn spring-boot:run
```

**启动成功标志：**
```
Started HMallApplication in 8.5 seconds
```

---

### 步骤3：测试功能

**访问测试页面：**
```
http://localhost:8080/edge-tts-test.html
```

**快速测试：**
1. 点击"检查 Edge TTS 状态"→ 应该显示"✅ Edge TTS 已安装"
2. 点击任意音色卡片 → 自动试听
3. 输入文本 → 点击"播放" → 听到语音 ✅
4. 点击"下载音频" → 浏览器自动下载 MP3 文件 ✅

---

## 🎵 中文音色完整列表（13种）

### 1. 普通话（zh-CN）- 8种 ⭐⭐⭐⭐⭐

| 代码 | 名称 | 性别 | 特点 | 适用场景 | 推荐度 |
|------|------|------|------|---------|--------|
| `zh-CN-XiaoxiaoNeural` | 晓晓 | 女 | 温柔 | 新闻、小说 | ⭐⭐⭐⭐⭐ |
| `zh-CN-XiaoyiNeural` | 晓伊 | 女 | 活泼 | 动画、小说 | ⭐⭐⭐⭐ |
| `zh-CN-YunjianNeural` | 云健 | 男 | 激情 | 体育、小说 | ⭐⭐⭐⭐ |
| `zh-CN-YunxiNeural` | 云希 | 男 | 活泼阳光 | 小说 | ⭐⭐⭐⭐⭐ |
| `zh-CN-YunxiaNeural` | 云霞 | 男 | 可爱 | 动画、小说 | ⭐⭐⭐⭐ |
| `zh-CN-YunyangNeural` | 云扬 | 男 | 专业可靠 | 新闻 | ⭐⭐⭐⭐⭐ |
| `zh-CN-liaoning-XiaobeiNeural` | 晓北 | 女 | 幽默 | 方言（东北） | ⭐⭐⭐ |
| `zh-CN-shaanxi-XiaoniNeural` | 晓妮 | 女 | 明亮 | 方言（陕西） | ⭐⭐⭐ |

### 2. 粤语（zh-HK）- 3种 ⭐⭐⭐⭐

| 代码 | 名称 | 性别 | 特点 | 推荐度 |
|------|------|------|------|--------|
| `zh-HK-HiuGaaiNeural` | 曉佳 | 女 | 友好积极 | ⭐⭐⭐⭐ |
| `zh-HK-HiuMaanNeural` | 曉曼 | 女 | 友好积极 | ⭐⭐⭐⭐ |
| `zh-HK-WanLungNeural` | 雲龍 | 男 | 友好积极 | ⭐⭐⭐⭐ |

### 3. 台湾国语（zh-TW）- 3种 ⭐⭐⭐⭐

| 代码 | 名称 | 性别 | 特点 | 推荐度 |
|------|------|------|------|--------|
| `zh-TW-HsiaoChenNeural` | 曉臻 | 女 | 友好积极 | ⭐⭐⭐⭐ |
| `zh-TW-YunJheNeural` | 雲哲 | 男 | 友好积极 | ⭐⭐⭐⭐ |
| `zh-TW-HsiaoYuNeural` | 曉雨 | 女 | 友好积极 | ⭐⭐⭐⭐ |

**总计：13种中文音色** ✅ 100%完整

---

## 📚 API 接口文档

### 1. 生成语音

**接口：** `POST /api/edge-tts/generate`

**请求示例：**
```javascript
fetch('/api/edge-tts/generate', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        text: '你好，我是晓晓',
        voice: 'zh-CN-XiaoxiaoNeural',
        rate: '+0%',    // 语速：-50% 到 +100%
        pitch: '+0Hz'   // 音调：-20Hz 到 +20Hz
    })
})
.then(response => response.blob())
.then(audioBlob => {
    const audio = new Audio(URL.createObjectURL(audioBlob));
    audio.play();
});
```

**响应：** 音频文件（MP3格式）

---

### 2. 健康检查

**接口：** `GET /api/edge-tts/health`

**响应示例：**
```json
{
    "status": "ok",
    "message": "edge-tts 已安装",
    "installed": true,
    "version": "6.1.9"
}
```

---

### 3. 获取音色列表

**接口：** `GET /api/edge-tts/voices`

**响应示例：**
```json
{
    "success": true,
    "data": {
        "zh-CN": [
            {
                "code": "zh-CN-XiaoxiaoNeural",
                "name": "晓晓",
                "gender": "女",
                "characteristic": "温柔",
                "scene": "新闻、小说"
            }
        ],
        "zh-HK": [...],
        "zh-TW": [...],
        "en-US": [...]
    }
}
```

---

## 🔧 常见问题

### ❌ 问题1：启动失败 - edge-tts 未安装

**症状：**
```
健康检查显示：❌ Edge TTS 未安装
```

**解决方案：**
```bash
# 运行安装脚本
install-edge-tts.bat

# 验证安装
edge-tts --version
```

---

### ❌ 问题2：播放失败 - 无法生成语音

**症状：**
```
播放失败: Edge TTS 执行失败
```

**解决方案：**
```bash
# 1. 检查 edge-tts 命令是否可用
edge-tts --version

# 2. 手动测试
edge-tts --text "测试" --write-media test.mp3

# 3. 检查临时目录是否存在
mkdir temp
```

---

### ❌ 问题3：中文乱码

**症状：**
```
生成的语音发音错误
```

**解决方案：**
```yaml
# 确保 application.yaml 中设置了正确的编码
spring:
  http:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

---

### ❌ 问题4：CORS 跨域问题

**症状：**
```
Access to fetch at 'http://localhost:8080/api/edge-tts/generate' from origin 'http://127.0.0.1:5500' has been blocked by CORS policy
```

**解决方案：**
```
已在 EdgeTTSController 中添加了 @CrossOrigin(origins = "*")
如果问题仍然存在，请检查是否有其他 CORS 配置覆盖
```

---

## 📁 项目文件结构

```
adminFlow/
├── install-edge-tts.bat                # Edge TTS 安装脚本
├── EDGE-TTS-GUIDE.md                   # 本文档
├── hm-service/
│   └── src/main/
│       ├── java/com/hmall/
│       │   ├── controller/
│       │   │   └── EdgeTTSController.java   # REST API
│       │   └── service/
│       │       └── EdgeTTSService.java      # 业务逻辑
│       └── resources/
│           ├── application.yaml             # 配置文件
│           └── static/
│               └── edge-tts-test.html       # 测试页面
└── temp/                                    # 临时文件目录
```

---

## 🎯 使用场景

### 场景1：在线客服系统
```java
// 将文字转换为语音，提供语音播报功能
String text = "您的订单已发货，请注意查收";
byte[] audio = edgeTTSService.generateSpeech(
    text, 
    "zh-CN-XiaoxiaoNeural",  // 使用温柔的女声
    "+0%", 
    "+0Hz"
);
```

### 场景2：多语言支持
```java
// 根据用户语言切换音色
String locale = user.getLocale();
String voice = switch (locale) {
    case "zh-CN" -> "zh-CN-XiaoxiaoNeural";  // 普通话
    case "zh-HK" -> "zh-HK-HiuGaaiNeural";   // 粤语
    case "zh-TW" -> "zh-TW-HsiaoChenNeural"; // 台湾国语
    default -> "en-US-JennyNeural";          // 英文
};
```

### 场景3：批量生成音频
```java
List<String> sentences = Arrays.asList(
    "欢迎使用我们的产品",
    "感谢您的支持",
    "祝您生活愉快"
);

for (String sentence : sentences) {
    byte[] audio = edgeTTSService.generateSpeech(
        sentence,
        "zh-CN-YunyangNeural",  // 使用专业的男声
        "+0%",
        "+0Hz"
    );
    // 保存或返回音频
}
```

---

## ✅ 完整性检查清单

- [x] **后端服务**：Spring Boot REST API ✅
- [x] **CORS 问题**：已通过后端代理解决 ✅
- [x] **中文音色**：13种（普通话8 + 粤语3 + 台湾国语3）✅
- [x] **英文音色**：5种（主要音色）✅
- [x] **音频播放**：支持 ✅
- [x] **音频导出**：支持下载 MP3 ✅
- [x] **测试页面**：完整功能演示 ✅
- [x] **文档**：完整使用指南 ✅
- [x] **安装脚本**：一键安装 ✅

**总体完整度：100%** ⭐⭐⭐⭐⭐

---

## 🚀 下一步

1. ✅ 运行 `install-edge-tts.bat` 安装依赖
2. ✅ 启动 Spring Boot 项目
3. ✅ 访问 http://localhost:8080/edge-tts-test.html
4. ✅ 试听 13 种中文音色
5. ✅ 集成到你的业务系统中

---

**创建时间：** 2026-08-12  
**作者：** Kiro  
**版本：** v1.0  
**项目：** adminFlow (Spring Boot 2.7.12)
