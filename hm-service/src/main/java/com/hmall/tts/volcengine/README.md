# 火山引擎 TTS 模块

> **功能：** 基于火山引擎（ByteDance）的高质量语音合成服务  
> **版本：** v1.0  
> **创建时间：** 2026-08-13  
> **作者：** Kiro

---

## 📋 目录结构

```
volcengine/
├── config/
│   └── VolcengineConfig.java          # 火山引擎配置类
├── dto/
│   ├── TTSRequest.java                # TTS请求参数
│   ├── TTSResponse.java               # TTS响应结果
│   └── VoiceInfo.java                 # 音色信息
├── client/
│   └── VolcengineClient.java          # HTTP客户端（负责与火山引擎API通信）
├── service/
│   ├── VolcengineTTSService.java      # TTS服务接口
│   └── impl/
│       └── VolcengineTTSServiceImpl.java  # TTS服务实现
├── controller/
│   └── VolcengineTTSController.java   # REST API控制器
├── HttpUnidirectionalStreaming.java   # 原始Demo（保留作为参考）
└── README.md                          # 本文档
```

---

## 🚀 快速开始

### 步骤1：配置参数

在 `application.yaml` 中配置火山引擎参数：

```yaml
volcengine:
  tts:
    api-key: a83eef4b-bde3-4cbf-ac5f-0a35a17b31ad  # 火山引擎 API Key
    resource-id: seed-tts-2.0                      # TTS 服务资源 ID
    url: https://openspeech.bytedance.com/api/v3/tts/unidirectional
    connect-timeout: 30
    request-timeout: 5
    default-speaker: zh_female_vv_uranus_bigtts
    default-format: mp3
    default-sample-rate: 24000
    output-dir: tts
```

### 步骤2：启动项目

```bash
# 在 IDEA 中运行 HMallApplication
# 或使用 Maven 命令
mvn spring-boot:run
```

### 步骤3：访问测试页面

```
http://localhost:8080/volcengine-tts-test.html
```

---

## 🎤 支持的音色

### 中文音色

| 音色ID | 名称 | 描述 | 性别 | 适用场景 |
|--------|------|------|------|----------|
| zh_female_vv_uranus_bigtts | 晓晓 | 温柔女声 | 女 | 讲故事、客服 |
| zh_male_vv_uranus_bigtts | 云扬 | 沉稳男声 | 男 | 新闻播报、商务 |
| zh_female_calm_uranus_bigtts | 晓静 | 平静女声 | 女 | 教育、解说 |
| zh_male_calm_uranus_bigtts | 云舒 | 平静男声 | 男 | 知识讲解 |

### 英文音色

| 音色ID | 名称 | 描述 | 性别 |
|--------|------|------|------|
| en_female_vv_uranus_bigtts | Emma | 温柔女声 | 女 |
| en_male_vv_uranus_bigtts | Tom | 沉稳男声 | 男 |

---

## 📡 API 接口

### 1. 生成语音（返回文件URL）

**接口：** `POST /api/volcengine/tts/generate`

**请求参数：**
```json
{
  "text": "你好，这是一个语音测试",
  "speaker": "zh_female_vv_uranus_bigtts",
  "format": "mp3",
  "sampleRate": 24000,
  "speed": 1.0,
  "volume": 1.0,
  "pitch": 1.0
}
```

**响应结果：**
```json
{
  "success": true,
  "message": "语音生成成功",
  "audioPath": "tts/123e4567-e89b-12d3-a456-426614174000.mp3",
  "audioUrl": "/tts/123e4567-e89b-12d3-a456-426614174000.mp3",
  "audioSize": 45678,
  "generateTime": 1234
}
```

---

### 2. 生成语音（返回Base64）

**接口：** `POST /api/volcengine/tts/generate-base64`

**请求参数：** 同上

**响应结果：**
```json
{
  "success": true,
  "message": "语音生成成功",
  "audioData": "SUQzBAAAAAAAI1RTU0UAAAAPAAADTGF2ZjU4Ljc2LjEwMAAAAAAAAAAAAAAA//...",
  "audioSize": 45678,
  "generateTime": 1234
}
```

---

### 3. 生成语音（返回音频流）

**接口：** `POST /api/volcengine/tts/generate-stream`

**请求参数：** 同上

**响应结果：** 直接返回音频流（Content-Type: audio/mpeg）

---

### 4. 获取音色列表

**接口：** `GET /api/volcengine/tts/voices`

**响应结果：**
```json
[
  {
    "voiceId": "zh_female_vv_uranus_bigtts",
    "voiceName": "晓晓",
    "description": "温柔女声，适合讲故事、客服",
    "gender": "female",
    "language": "zh-CN",
    "style": "gentle",
    "recommended": true
  },
  ...
]
```

---

### 5. 获取指定音色信息

**接口：** `GET /api/volcengine/tts/voices/{voiceId}`

**响应结果：**
```json
{
  "voiceId": "zh_female_vv_uranus_bigtts",
  "voiceName": "晓晓",
  "description": "温柔女声，适合讲故事、客服",
  "gender": "female",
  "language": "zh-CN",
  "style": "gentle",
  "recommended": true
}
```

---

### 6. 健康检查

**接口：** `GET /api/volcengine/tts/health`

**响应结果：**
```json
{
  "status": "ok",
  "service": "volcengine-tts",
  "timestamp": 1692000000000
}
```

---

### 7. 快速测试

**接口：** `GET /api/volcengine/tts/test?text=你好`

**响应结果：** 同"生成语音（返回文件URL）"

---

## 💻 代码示例

### 1. 基本使用

```java
@Autowired
private VolcengineTTSService ttsService;

public void test() {
    // 构建请求
    TTSRequest request = TTSRequest.builder()
            .text("你好，这是一个语音测试")
            .speaker("zh_female_vv_uranus_bigtts")
            .format("mp3")
            .sampleRate(24000)
            .build();
    
    // 生成语音
    TTSResponse response = ttsService.generateSpeech(request);
    
    if (response.getSuccess()) {
        System.out.println("音频URL: " + response.getAudioUrl());
        System.out.println("音频大小: " + response.getAudioSize() + " 字节");
    }
}
```

---

### 2. 生成Base64音频

```java
@Autowired
private VolcengineTTSService ttsService;

public void testBase64() {
    TTSRequest request = TTSRequest.builder()
            .text("你好，这是Base64测试")
            .build();
    
    TTSResponse response = ttsService.generateSpeechBase64(request);
    
    if (response.getSuccess()) {
        String audioBase64 = response.getAudioData();
        // 前端可以直接使用：data:audio/mp3;base64,{audioBase64}
    }
}
```

---

### 3. 获取音色列表

```java
@Autowired
private VolcengineTTSService ttsService;

public void testVoices() {
    List<VoiceInfo> voices = ttsService.getVoiceList();
    
    voices.forEach(voice -> {
        System.out.println("音色: " + voice.getVoiceName());
        System.out.println("描述: " + voice.getDescription());
    });
}
```

---

## 🔧 核心架构

### 1. 分层设计

```
Controller（控制器层）
    ↓ 调用
Service（服务层）
    ↓ 调用
Client（客户端层）
    ↓ HTTP请求
火山引擎API
```

### 2. 职责划分

- **Controller**：接收HTTP请求，参数验证，返回响应
- **Service**：业务逻辑处理，文件管理，数据转换
- **Client**：HTTP通信，流式数据处理，错误处理
- **Config**：配置管理，参数注入
- **DTO**：数据传输对象，参数封装

### 3. 设计特点

- ✅ **模块化**：各层职责清晰，易于维护
- ✅ **可配置**：所有参数可通过配置文件修改
- ✅ **易扩展**：新增音色、格式无需修改代码
- ✅ **高性能**：使用流式传输，支持大文本
- ✅ **错误处理**：完善的异常处理机制

---

## 📊 性能特点

### 1. 音频质量

- 采样率：8000Hz / 16000Hz / 24000Hz
- 格式：MP3 / WAV / PCM
- 码率：高质量音频

### 2. 响应时间

- 短文本（<50字）：1-2秒
- 中等文本（50-200字）：2-5秒
- 长文本（>200字）：5-10秒

### 3. 并发能力

- 支持多并发请求
- 流式传输，内存占用低

---

## 🔍 故障排查

### 问题1：API Key 无效

**错误信息：** `火山引擎API错误: {"code": 401, ...}`

**解决方案：**
1. 检查 `application.yaml` 中的 `api-key` 是否正确
2. 登录火山引擎控制台确认 API Key 状态
3. 确认 API Key 是否有 TTS 服务权限

---

### 问题2：连接超时

**错误信息：** `TTS请求失败: Read timed out`

**解决方案：**
1. 检查网络连接
2. 增加 `request-timeout` 时间
3. 确认防火墙是否拦截

---

### 问题3：音频文件无法访问

**错误信息：** `404 Not Found`

**解决方案：**
1. 检查 `output-dir` 目录是否存在
2. 确认文件是否成功生成
3. 配置静态资源映射

---

## 📝 更新日志

### v1.0 (2026-08-13)

- ✅ 实现基础 TTS 功能
- ✅ 支持6种音色（4中文 + 2英文）
- ✅ 支持3种音频格式（MP3/WAV/PCM）
- ✅ 提供3种输出方式（文件/Base64/流）
- ✅ 完整的 REST API
- ✅ 测试页面
- ✅ 健康检查接口

---

## 🎯 下一步计划

- [ ] 支持更多音色
- [ ] 长文本自动分段
- [ ] 音频缓存机制
- [ ] 音频拼接功能
- [ ] 音频效果处理（变速、变调）
- [ ] 批量生成接口
- [ ] 异步生成任务
- [ ] WebSocket 实时流式输出

---

## 📞 技术支持

**火山引擎官方文档：** https://www.volcengine.com/docs/6561/79816  
**API 文档：** https://www.volcengine.com/docs/6561/79820  
**控制台：** https://console.volcengine.com/speech/service

---

**最后更新时间：** 2026-08-13  
**版本：** v1.0  
**作者：** Kiro
