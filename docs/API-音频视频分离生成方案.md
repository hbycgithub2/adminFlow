# 音频视频分离生成方案 - API文档

**版本**: v1.0  
**创建时间**: 2026-08-16  
**方案**: 方案B（三合一混合方案）

---

## 📋 概述

本方案支持三种视频生成模式：
1. **快速模式**：一键从Word生成视频（保留原功能）
2. **灵活模式**：先生成MP3（可下载编辑）→ 上传MP3生成视频
3. **自定义模式**：直接上传MP3生成视频

---

## 🎯 核心接口

### 接口1：一键生成视频（增强版）⭐

**用途**: 从Word文档直接生成视频，同时返回音频URL供下载

**接口地址**: `POST /api/video-generator/generate`

**请求参数**:
```
Content-Type: multipart/form-data

file: Word文档文件 (必填)
boldVoice: 粗体文本音色 (可选, 默认: zh_male_m191_uranus_bigtts)
normalVoice: 普通文本音色 (可选, 默认: zh_female_vv_uranus_bigtts)
audioFormat: 音频格式 (可选, 默认: mp3)
sampleRate: 采样率 (可选, 默认: 24000)
videoWidth: 视频宽度 (可选, 默认: 1920)
videoHeight: 视频高度 (可选, 默认: 1080)
videoFps: 视频帧率 (可选, 默认: 30)
backgroundColor: 背景颜色 (可选, 默认: #FFFFFF)
fontName: 字体名称 (可选, 默认: Arial)
fontSize: 字体大小 (可选, 默认: 48)
fontColor: 字体颜色 (可选, 默认: #FFFFFF)
animationType: 动画类型 (可选, 默认: fade)
```

**响应示例**:
```json
{
  "success": true,
  "message": "视频生成成功",
  "taskId": "abc123",
  "videoUrl": "/tts/videos/abc123.mp4",
  "audioUrl": "/tts/documents/abc123.mp3",  // ⭐ 新增
  "duration": 120.5,
  "videoSize": 5242880,
  "subtitles": [
    {
      "text": "这是第一句话",
      "startTime": 0.0,
      "duration": 2.5,
      "isBold": false,
      "speaker": "zh_female_vv_uranus_bigtts"
    }
  ]
}
```

**使用场景**:
- 快速生成视频，适合简单场景
- 同时获取MP3文件，可以下载备用

---

### 接口2：仅生成音频（新增）⭐

**用途**: 从Word文档生成音频和字幕数据，不生成视频

**接口地址**: `POST /api/audio/generate`

**请求参数**:
```
Content-Type: multipart/form-data

file: Word文档文件 (必填)
boldVoice: 粗体文本音色 (可选, 默认: zh_male_m191_uranus_bigtts)
normalVoice: 普通文本音色 (可选, 默认: zh_female_vv_uranus_bigtts)
audioFormat: 音频格式 (可选, 默认: mp3)
sampleRate: 采样率 (可选, 默认: 24000)
```

**响应示例**:
```json
{
  "success": true,
  "message": "音频生成成功",
  "taskId": "def456",
  "audioUrl": "/tts/documents/def456.mp3",
  "audioSize": 2097152,
  "duration": 120.5,
  "subtitles": [
    {
      "text": "这是第一句话",
      "startTime": 0.0,
      "duration": 2.5,
      "voiceId": "zh_female_vv_uranus_bigtts",
      "isBold": false,
      "charTimings": [
        {
          "character": "这",
          "startTime": 0.0,
          "duration": 0.25
        }
      ]
    }
  ],
  "generateTime": 8500
}
```

**使用场景**:
- 只需要音频，不需要视频
- 需要下载MP3进行编辑（调整语速、添加背景音乐等）
- 灵活模式第一步：生成音频

---

### 接口3：上传MP3生成视频（新增）⭐

**用途**: 从音频文件生成视频，支持自动字幕对齐

**接口地址**: `POST /api/video-generator/generate-from-audio`

**请求参数**:
```
Content-Type: multipart/form-data

audio: 音频文件 (必填, 支持MP3/WAV等)
subtitles: 字幕数据JSON (可选)
originalText: 原始文本 (可选)
forceReAlign: 是否强制重新对齐 (可选, 默认: false)
videoWidth: 视频宽度 (可选, 默认: 1920)
videoHeight: 视频高度 (可选, 默认: 1080)
videoFps: 视频帧率 (可选, 默认: 30)
backgroundColor: 背景颜色 (可选, 默认: #FFFFFF)
fontName: 字体名称 (可选, 默认: Arial)
fontSize: 字体大小 (可选, 默认: 48)
fontColor: 字体颜色 (可选, 默认: #FFFFFF)
animationType: 动画类型 (可选, 默认: fade)
```

**字幕JSON格式**:
```json
[
  {
    "text": "这是第一句话",
    "startTime": 0.0,
    "duration": 2.5,
    "voiceId": "zh_female_vv_uranus_bigtts",
    "isBold": false
  }
]
```

**响应示例**:
```json
{
  "success": true,
  "message": "视频生成成功",
  "taskId": "ghi789",
  "videoUrl": "/tts/videos/ghi789.mp4",
  "duration": 120.5,
  "videoSize": 5242880,
  "reAligned": true,  // ⭐ 标识是否重新对齐
  "subtitles": [...]
}
```

**使用场景**:
- 使用编辑后的MP3生成视频
- 使用自己录制的音频生成视频
- 使用其他TTS工具生成的音频
- 灵活模式第二步：上传编辑后的音频

---

## 🔄 三种工作流程

### 流程1：快速模式（一键生成）

```
1. 用户上传Word文档
   ↓
2. 调用接口1：POST /api/video-generator/generate
   ↓
3. 系统返回：
   - videoUrl: 视频地址
   - audioUrl: 音频地址 ⭐（可下载）
   - subtitles: 字幕数据
```

**cURL示例**:
```bash
curl -X POST http://localhost:8080/api/video-generator/generate \
  -F "file=@document.docx" \
  -F "boldVoice=zh_male_m191_uranus_bigtts" \
  -F "normalVoice=zh_female_vv_uranus_bigtts"
```

---

### 流程2：灵活模式（两步生成）

```
步骤1：生成音频
1. 用户上传Word文档
   ↓
2. 调用接口2：POST /api/audio/generate
   ↓
3. 系统返回：
   - audioUrl: 音频地址
   - subtitles: 字幕数据（保存到变量）
   ↓
4. 用户下载MP3，编辑（可选）
   - 调整语速
   - 添加背景音乐
   - 裁剪片段

步骤2：生成视频
5. 用户上传编辑后的MP3
   ↓
6. 调用接口3：POST /api/video-generator/generate-from-audio
   参数：
   - audio: 编辑后的MP3
   - subtitles: 步骤1保存的字幕数据（JSON）
   - forceReAlign: true（如果编辑了音频）
   ↓
7. 系统返回：
   - videoUrl: 视频地址
   - reAligned: true（表示重新对齐了字幕）
```

**cURL示例（步骤1）**:
```bash
# 步骤1：生成音频
curl -X POST http://localhost:8080/api/audio/generate \
  -F "file=@document.docx" \
  -F "boldVoice=zh_male_m191_uranus_bigtts" \
  -F "normalVoice=zh_female_vv_uranus_bigtts" \
  > audio_response.json

# 提取audioUrl和subtitles
audioUrl=$(jq -r '.audioUrl' audio_response.json)
subtitles=$(jq -r '.subtitles' audio_response.json)

# 下载MP3
curl -O http://localhost:8080${audioUrl}

# 用户编辑MP3（略）

# 步骤2：上传编辑后的MP3生成视频
curl -X POST http://localhost:8080/api/video-generator/generate-from-audio \
  -F "audio=@edited_audio.mp3" \
  -F "subtitles=${subtitles}" \
  -F "forceReAlign=true"
```

---

### 流程3：自定义模式（直接上传MP3）

```
1. 用户准备自己的MP3（录音、其他TTS工具等）
   ↓
2. 用户准备原始文本（用于生成字幕）
   ↓
3. 调用接口3：POST /api/video-generator/generate-from-audio
   参数：
   - audio: 自定义MP3
   - originalText: 原始文本
   - forceReAlign: true
   ↓
4. 系统自动识别并对齐字幕
   ↓
5. 系统返回视频
```

**cURL示例**:
```bash
curl -X POST http://localhost:8080/api/video-generator/generate-from-audio \
  -F "audio=@my_recording.mp3" \
  -F "originalText=这是我的录音内容，需要生成字幕。" \
  -F "forceReAlign=true"
```

---

## 🎨 字幕对齐策略

系统会根据输入智能选择对齐策略：

| 场景 | 输入 | 对齐策略 | 准确率 |
|------|------|---------|--------|
| **原始MP3** | 有原始字幕 + 原始MP3 | 直接使用原始字幕 | ✅ 100% |
| **编辑后MP3** | 有原始字幕 + 编辑后MP3 | WhisperX重新对齐 | ✅ 98% |
| **自定义MP3** | 有原始文本 | WhisperX对齐 | ✅ 98% |
| **无字幕无文本** | 只有MP3 | Whisper识别 | ⚠️ 90% |

**检测逻辑**:
- 系统会自动比较音频时长判断是否编辑
- 如果时长差异 > 1秒，判定为已编辑，自动重新对齐
- 可以通过 `forceReAlign=true` 强制重新对齐

---

## 📊 性能指标

| 操作 | 耗时 | 说明 |
|------|------|------|
| **生成音频** | 6-10秒 | 依赖TTS API速度 |
| **生成视频** | 10-20秒 | 依赖FFmpeg性能 |
| **字幕对齐** | 2-5秒 | WhisperX对齐 |
| **一键生成** | 16-30秒 | 音频+视频 |

---

## ⚙️ 配置说明

### 音色列表

可以通过接口查询：`GET /api/video-generator/voices`

**男声**:
- `zh_male_m191_uranus_bigtts`: 云舟（沉稳男声）
- `zh_male_taocheng_uranus_bigtts`: 小天（阳光男声）

**女声**:
- `zh_female_vv_uranus_bigtts`: 薇薇（温柔女声）
- `zh_female_xiaohe_uranus_bigtts`: 小何（甜美女声）

### 动画类型

可以通过接口查询：`GET /api/video-generator/animation-types`

支持的动画类型：
- `fade`: 淡入淡出
- `slide`: 滑动
- `zoom`: 缩放
- `none`: 无动画

---

## 🔧 错误处理

### 常见错误

| 错误码 | 说明 | 解决方案 |
|--------|------|---------|
| **400** | 参数错误 | 检查必填参数 |
| **500** | TTS生成失败 | 检查Word文档格式、网络连接 |
| **500** | FFmpeg生成失败 | 检查FFmpeg安装、磁盘空间 |
| **500** | 字幕对齐失败 | 提供原始文本或字幕数据 |

### 错误响应示例

```json
{
  "success": false,
  "message": "字幕对齐失败：无法生成有效的字幕数据",
  "taskId": "xxx"
}
```

---

## 📝 前端集成示例

### JavaScript/Fetch示例

```javascript
// 流程1：一键生成视频
async function generateVideoOneStep(wordFile) {
  const formData = new FormData();
  formData.append('file', wordFile);
  formData.append('boldVoice', 'zh_male_m191_uranus_bigtts');
  formData.append('normalVoice', 'zh_female_vv_uranus_bigtts');
  
  const response = await fetch('/api/video-generator/generate', {
    method: 'POST',
    body: formData
  });
  
  const result = await response.json();
  
  if (result.success) {
    console.log('视频URL:', result.videoUrl);
    console.log('音频URL:', result.audioUrl); // ⭐ 可以下载
  }
}

// 流程2：灵活模式 - 步骤1：生成音频
async function generateAudio(wordFile) {
  const formData = new FormData();
  formData.append('file', wordFile);
  
  const response = await fetch('/api/audio/generate', {
    method: 'POST',
    body: formData
  });
  
  const result = await response.json();
  
  if (result.success) {
    // 保存字幕数据到localStorage或state
    localStorage.setItem('subtitles', JSON.stringify(result.subtitles));
    
    // 下载MP3
    window.open(result.audioUrl, '_blank');
  }
}

// 流程2：灵活模式 - 步骤2：上传MP3生成视频
async function generateVideoFromAudio(audioFile) {
  const formData = new FormData();
  formData.append('audio', audioFile);
  
  // 获取之前保存的字幕数据
  const subtitles = localStorage.getItem('subtitles');
  formData.append('subtitles', subtitles);
  formData.append('forceReAlign', 'true');
  
  const response = await fetch('/api/video-generator/generate-from-audio', {
    method: 'POST',
    body: formData
  });
  
  const result = await response.json();
  
  if (result.success) {
    console.log('视频URL:', result.videoUrl);
    console.log('是否重新对齐:', result.reAligned);
  }
}
```

---

## 🚀 后续优化方向

1. **批量处理**: 支持批量上传Word文档
2. **进度通知**: WebSocket实时推送生成进度
3. **音频编辑器**: 集成在线音频编辑器
4. **更多格式**: 支持PDF、TXT等格式
5. **更多动画**: 添加更多字幕动画效果

---

## 📞 支持

如有问题，请联系技术支持或查看详细文档。

**文档版本**: v1.0  
**最后更新**: 2026-08-16
