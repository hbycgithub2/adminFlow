# Word 文档 TTS 页面路径

## 📍 页面访问地址

### 主测试页面（推荐）⭐
```
http://localhost:8080/document-tts-test.html
```

**功能：**
- ✅ 上传 Word 文档（.docx 格式）
- ✅ 选择音色（加粗文本音色 + 普通文本音色）
- ✅ 在线播放生成的音频
- ✅ 下载生成的音频文件
- ✅ 查看文档解析结果
- ✅ 实时进度提示

**页面文件位置：**
```
d:\code\adminFlow\hm-service\src\main\resources\static\document-tts-test.html
```

---

## 🔌 后端接口路径

### 接口基础路径
```
http://localhost:8080/api/document-tts
```

### 接口1：生成文档对话语音（返回文件信息）⭐
```
POST http://localhost:8080/api/document-tts/generate
```

**参数：**
- `file`：Word 文档文件（必填，multipart/form-data）
- `boldVoice`：加粗文本音色（可选，默认：zh_male_m191_uranus_bigtts）
- `normalVoice`：普通文本音色（可选，默认：zh_female_vv_uranus_bigtts）
- `format`：音频格式（可选，默认：mp3）
- `sampleRate`：采样率（可选，默认：24000）

**返回：**
```json
{
  "success": true,
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "audioUrl": "/audio/document_1726253811234.mp3",
  "message": "文档TTS生成成功",
  "segments": [
    {
      "text": "客户",
      "isBold": true,
      "audioFile": "/audio/segment_1_1726253811234.mp3"
    },
    {
      "text": "您好！请问有什么可以帮助您的吗？",
      "isBold": false,
      "audioFile": "/audio/segment_2_1726253811234.mp3"
    }
  ],
  "totalDuration": 5.2
}
```

---

### 接口2：生成文档对话语音（返回音频流）
```
POST http://localhost:8080/api/document-tts/generate-stream
```

**参数：** 同接口1

**返回：**
- 音频文件流（audio/mpeg）
- 直接下载为 MP3 文件

---

### 接口3：快速测试接口（使用默认配置）
```
POST http://localhost:8080/api/document-tts/test
```

**参数：**
- `file`：Word 文档文件（必填）

**返回：** 同接口1（使用默认音色配置）

---

## 📂 文件结构

### 前端页面
```
d:\code\adminFlow\hm-service\src\main\resources\static\
└── document-tts-test.html  ← 测试页面
```

### 后端代码
```
d:\code\adminFlow\hm-service\src\main\java\com\hmall\tts\volcengine\
├── controller/
│   └── DocumentTTSController.java  ← 控制器（接口路径）
├── service/
│   ├── DocumentTTSService.java     ← 服务接口
│   └── impl/
│       └── DocumentTTSServiceImpl.java  ← 服务实现
├── docx/
│   ├── WordDocumentParser.java     ← Word 文档解析器
│   └── DialogSegment.java          ← 对话片段模型
└── dto/
    ├── DocumentTTSResult.java      ← 返回结果
    └── VoiceConfig.java            ← 音色配置
```

### 音频文件存储
```
d:\code\adminFlow\hm-service\src\main\resources\static\audio\
├── document_1726253811234.mp3  ← 完整音频
├── segment_1_1726253811234.mp3 ← 片段1（加粗文本）
├── segment_2_1726253811234.mp3 ← 片段2（普通文本）
└── ...
```

---

## 🎯 快速测试步骤

### 步骤1：启动服务
```bash
# 方法1：使用脚本
cd d:\code\adminFlow
start-adminFlow.bat

# 方法2：IDEA 启动
# 运行 HmServiceApplication 主类
```

### 步骤2：访问测试页面
在浏览器中打开：
```
http://localhost:8080/document-tts-test.html
```

### 步骤3：上传 Word 文档
1. 点击 "选择文件" 按钮
2. 选择 Word 文档（.docx 格式）
3. 选择音色（可选）
4. 点击 "生成语音" 按钮

### 步骤4：查看结果
- 等待生成（1-10秒）
- 点击 "播放音频" 按钮在线播放
- 点击 "下载音频" 按钮下载 MP3 文件

---

## 🎤 可用音色列表

### 男声音色（用于加粗文本）
- `zh_male_m191_uranus_bigtts`（云舟，默认）⭐
- `zh_male_aojiaobozai_moon_bigtts`（傲娇男）
- `zh_male_wennuanahu_moon_bigtts`（温暖大叔）

### 女声音色（用于普通文本）
- `zh_female_vv_uranus_bigtts`（薇薇，默认）⭐
- `zh_female_shuangkuaisisi_moon_bigtts`（爽快思思）
- `zh_female_wanwanxiaohe_moon_bigtts`（湾湾小河）

---

## 📋 测试文档示例

### 示例1：简单对话
```
客户：您好！请问有什么可以帮助您的吗？
客服：我想咨询一下贵公司的产品。
客户：好的，请问您对哪个产品感兴趣？
```

### 示例2：使用加粗文本
在 Word 中：
- 加粗文本（如 "客户"）会使用男声音色
- 普通文本（如 "您好！请问..."）会使用女声音色

### 测试文档位置
```
d:\code\adminFlow\测试文档内容模板.txt
```

---

## 🔧 Postman 测试

### 请求配置
```
POST http://localhost:8080/api/document-tts/generate
Content-Type: multipart/form-data

Body:
- file: [选择 Word 文档]
- boldVoice: zh_male_m191_uranus_bigtts
- normalVoice: zh_female_vv_uranus_bigtts
- format: mp3
- sampleRate: 24000
```

### cURL 命令
```bash
curl -X POST "http://localhost:8080/api/document-tts/generate" \
  -F "file=@测试文档.docx" \
  -F "boldVoice=zh_male_m191_uranus_bigtts" \
  -F "normalVoice=zh_female_vv_uranus_bigtts" \
  -F "format=mp3" \
  -F "sampleRate=24000"
```

---

## 📊 Swagger 文档

### 访问地址
```
http://localhost:8080/swagger-ui.html
```

### 接口位置
```
文档TTS接口 → /api/document-tts
```

在 Swagger 中可以：
- ✅ 查看接口文档
- ✅ 在线测试接口
- ✅ 查看参数说明
- ✅ 查看返回值示例

---

## 🎯 完整访问流程

```
浏览器
  ↓
http://localhost:8080/document-tts-test.html
  ↓
选择 Word 文档 → 选择音色 → 点击生成
  ↓
POST http://localhost:8080/api/document-tts/generate
  ↓
DocumentTTSController.generateDocumentSpeech()
  ↓
DocumentTTSService.generateDocumentSpeech()
  ↓
1. WordDocumentParser 解析文档
2. VolcEngineTTSService 生成音频
3. 合并音频片段
  ↓
返回结果（JSON）
  ↓
页面显示音频播放器 + 下载按钮
```

---

## ⚠️ 注意事项

1. **端口号：** 默认是 8080，如果修改过，需要相应调整访问地址

2. **服务状态：** 访问前确保服务已启动
   ```bash
   # 检查服务是否启动
   curl http://localhost:8080/actuator/health
   ```

3. **文档格式：** 只支持 `.docx` 格式（不支持 `.doc`）

4. **文档大小：** 建议不超过 5MB

5. **音频格式：** 支持 mp3、wav、ogg

6. **浏览器兼容性：** 推荐使用 Chrome/Edge/Firefox

---

## 🔍 故障排查

### 问题1：页面打不开（404）
**原因：** 服务未启动或端口错误  
**解决：** 
```bash
# 检查服务状态
curl http://localhost:8080/actuator/health

# 重启服务
start-adminFlow.bat
```

### 问题2：上传失败
**原因：** 文档格式错误或文件过大  
**解决：** 
- 确保是 .docx 格式
- 文件大小不超过 5MB
- 检查控制台错误日志

### 问题3：音频无法播放
**原因：** 音频文件未生成或路径错误  
**解决：** 
- 检查 `static/audio/` 目录
- 查看返回的 `audioUrl` 是否正确
- 尝试直接访问音频 URL

---

**更新时间：** 2026-08-14 01:15  
**服务端口：** 8080  
**测试页面：** http://localhost:8080/document-tts-test.html  
**接口路径：** /api/document-tts/generate
