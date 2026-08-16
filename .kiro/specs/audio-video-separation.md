# Spec: 音频视频分离生成方案B（三合一）

**创建时间**: 2026-08-16  
**状态**: 🚧 进行中  
**优先级**: 高

---

## 📋 需求概述

实现灵活的音频视频生成流程，支持三种模式：
1. **快速模式**：一键生成视频（保持当前功能）
2. **灵活模式**：先生成MP3（可下载、编辑） → 上传MP3生成视频
3. **自定义模式**：直接上传MP3生成视频

---

## 🎯 核心目标

- ✅ **向后兼容**：保留当前一键生成视频功能
- ✅ **灵活性**：支持单独生成MP3、下载MP3
- ✅ **可编辑**：支持上传编辑后的MP3重新生成视频
- ✅ **字幕同步**：自动处理字幕对齐（原始MP3或编辑后MP3）

---

## 📐 架构设计

### 接口设计

#### 接口1：一键生成视频（现有，增强返回）
```
POST /api/video/generate
Request: 
  - MultipartFile file (Word文档)
  - VideoGenerateRequest config
Response: 
  - videoUrl: 视频地址
  - audioUrl: 音频地址 ⭐（新增）
  - subtitles: 字幕数据
  - duration: 总时长
  - taskId: 任务ID
```

#### 接口2：仅生成MP3（新增）⭐
```
POST /api/audio/generate
Request:
  - MultipartFile file (Word文档)
  - VoiceConfig config
Response:
  - audioUrl: 音频地址
  - subtitles: 字幕数据（DialogSegments）
  - duration: 总时长
  - taskId: 任务ID
```

#### 接口3：上传MP3生成视频（新增）⭐
```
POST /api/video/generateFromAudio
Request:
  - MultipartFile audio (MP3文件)
  - String subtitlesJson (字幕数据JSON，可选)
  - String originalText (原始文本，用于重对齐，可选)
  - VideoConfig videoConfig
  - SubtitleConfig subtitleConfig
Response:
  - videoUrl: 视频地址
  - duration: 总时长
  - taskId: 任务ID
  - reAligned: 是否重新对齐
```

---

## 🔧 技术实现

### 模块1：AudioGeneratorService（新增）

**职责**：专门负责音频生成，与视频解耦

**核心方法**：
```java
public interface AudioGeneratorService {
    /**
     * 从Word文档生成音频
     * @return AudioGenerateResponse（包含音频URL、字幕数据）
     */
    AudioGenerateResponse generateAudioFromDocument(MultipartFile file, VoiceConfig config);
}
```

### 模块2：VideoGeneratorService（增强）

**增强功能**：
1. 现有方法：`generateVideoFromDocument()` → 增强返回值，包含audioUrl
2. 新增方法：`generateVideoFromAudio()` → 从MP3生成视频

**核心方法**：
```java
public interface VideoGeneratorService {
    // 现有方法（增强）
    VideoGenerateResponse generateVideoFromDocument(MultipartFile file, VideoGenerateRequest request);
    
    // 新增方法 ⭐
    VideoGenerateResponse generateVideoFromAudio(
        MultipartFile audio,
        List<DialogSegment> subtitles,  // 可选
        String originalText,            // 可选
        VideoConfig videoConfig,
        SubtitleConfig subtitleConfig
    );
}
```

### 模块3：SubtitleAlignmentService（新增）

**职责**：处理字幕对齐逻辑

**核心方法**：
```java
public interface SubtitleAlignmentService {
    /**
     * 智能对齐字幕
     * - 如果有原始字幕数据，尝试使用
     * - 如果MP3被编辑，重新对齐
     */
    List<DialogSegment> alignSubtitles(
        byte[] audioData,
        List<DialogSegment> originalSubtitles,  // 可选
        String originalText                      // 可选
    );
}
```

---

## 📋 实施任务

### Phase 1: 后端核心功能（优先）

#### Task 1.1: 创建AudioGeneratorService ⭐
- [x] 创建接口 `AudioGeneratorService`
- [x] 创建实现类 `AudioGeneratorServiceImpl`
- [x] 复用 `DocumentTTSService` 的音频生成逻辑
- [x] 返回 `AudioGenerateResponse`（包含audioUrl、subtitles、duration）

#### Task 1.2: 增强VideoGeneratorService ⭐
- [x] 增强 `generateVideoFromDocument()` 返回值，包含audioUrl
- [x] 新增方法 `generateVideoFromAudio()`
- [x] 实现从MP3生成视频的逻辑

#### Task 1.3: 创建SubtitleAlignmentService ⭐
- [x] 创建接口 `SubtitleAlignmentService`
- [x] 创建实现类 `SubtitleAlignmentServiceImpl`
- [x] 实现智能对齐逻辑：
  - 如果有原始字幕且MP3未变化 → 直接使用
  - 如果MP3变化 → 调用WhisperX重新对齐
  - 如果无原始字幕 → 调用Whisper识别 + WhisperX对齐

#### Task 1.4: 创建Controller ⭐
- [x] 创建 `AudioGeneratorController`
- [x] 增强 `VideoGeneratorController`
- [x] 添加接口2：`POST /api/audio/generate`
- [x] 添加接口3：`POST /api/video/generateFromAudio`

#### Task 1.5: 创建DTO
- [x] `AudioGenerateRequest`
- [x] `AudioGenerateResponse`
- [x] `VideoFromAudioRequest`
- [x] 增强 `VideoGenerateResponse`

### Phase 2: 前端UI（次优先）

#### Task 2.1: 添加模式切换
- [ ] 快速模式（当前）
- [ ] 灵活模式（新增）

#### Task 2.2: 添加音频生成功能
- [ ] "生成音频"按钮
- [ ] 显示音频URL
- [ ] "下载MP3"按钮

#### Task 2.3: 添加上传MP3功能
- [ ] "上传MP3"按钮
- [ ] 文件选择器
- [ ] 显示字幕数据（可选编辑）

#### Task 2.4: 集成视频生成
- [ ] 支持从音频生成视频
- [ ] 显示生成进度
- [ ] 显示视频预览

### Phase 3: 优化与测试

#### Task 3.1: 性能优化
- [ ] 缓存机制（避免重复生成）
- [ ] 异步处理
- [ ] 进度通知

#### Task 3.2: 测试
- [ ] 单元测试
- [ ] 集成测试
- [ ] 端到端测试

---

## 🔄 工作流程

### 流程1：快速模式（一键生成）
```
用户上传Word → generateVideoFromDocument()
    ↓
生成音频（内部调用DocumentTTSService）
    ↓
生成字幕（内部使用WhisperX）
    ↓
生成视频（FFmpeg）
    ↓
返回：videoUrl + audioUrl + subtitles
```

### 流程2：灵活模式（两步生成）
```
步骤1：
用户上传Word → generateAudioFromDocument()
    ↓
生成音频
    ↓
生成字幕
    ↓
返回：audioUrl + subtitles + taskId

步骤2：
用户下载MP3 → 编辑（可选）→ 上传MP3
    ↓
generateVideoFromAudio(audio, subtitles)
    ↓
检测MP3是否变化
    ↓
如果变化 → 重新对齐字幕（WhisperX）
如果未变化 → 使用原字幕
    ↓
生成视频（FFmpeg）
    ↓
返回：videoUrl + duration
```

### 流程3：自定义模式
```
用户上传自己的MP3
    ↓
generateVideoFromAudio(audio, null, originalText)
    ↓
调用Whisper识别 + WhisperX对齐
    ↓
生成字幕
    ↓
生成视频（FFmpeg）
    ↓
返回：videoUrl + duration
```

---

## 🎯 成功标准

1. ✅ 保持当前功能100%可用
2. ✅ 支持单独生成MP3并下载
3. ✅ 支持上传MP3生成视频
4. ✅ 字幕同步准确率 ≥ 95%
5. ✅ 性能：音频生成 < 10秒，视频生成 < 20秒

---

## 📊 风险与挑战

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 字幕对齐失败 | 高 | 多层降级策略（WhisperX → Whisper → 均匀分配） |
| MP3格式不兼容 | 中 | 支持多种格式（MP3、WAV、M4A） |
| 性能问题 | 中 | 异步处理 + 缓存 |
| 前端复杂度 | 低 | 渐进式增强，保持简单UI |

---

## 📝 待决定

- [ ] 是否需要支持批量处理？
- [ ] 是否需要音频编辑器集成？
- [ ] 字幕格式是否支持SRT、VTT等？

---

**下一步**: 开始实施 Task 1.1 - 创建AudioGeneratorService

