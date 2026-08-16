# Whisper Base实施 - 5天完整计划

**开始时间：** 2026-08-14  
**预计完成：** 2026-08-19  
**目标：** 实现100%免费的字幕-语音精确同步（88-92%准确率）

---

## 📅 5天计划总览

| 天数 | 任务 | 预计时间 | 核心交付物 | 状态 |
|------|------|---------|-----------|------|
| **第1天** | 环境准备 | 45分钟 | Python + Whisper安装完成 | 🔄 进行中 |
| **第2天** | Java集成 | 4小时 | WhisperService + Python脚本 | ⏳ 待开始 |
| **第3天** | TTS集成 | 4小时 | 集成到DocumentTTSServiceImpl | ⏳ 待开始 |
| **第4天** | 测试优化 | 4小时 | 单元测试 + 性能优化 | ⏳ 待开始 |
| **第5天** | 上线验证 | 2小时 | 生成测试视频，验证同步 | ⏳ 待开始 |

**总投入：** 约15小时实际开发时间  
**总成本：** 0元（完全免费）  
**预期效果：** 88-92%准确率，100%同步

---

## 📋 详细任务分解

### 第1天：环境准备（45分钟）✅

**任务清单：**
- [ ] 安装Python 3.11（15分钟）
- [ ] 安装openai-whisper（10分钟）
- [ ] 配置FFmpeg环境变量（5分钟）
- [ ] 下载base模型（10分钟，150MB）
- [ ] 测试Whisper识别（5分钟）

**交付物：**
- Python环境可用
- Whisper base模型下载完成
- 测试识别成功

**验收标准：**
```python
import whisper
model = whisper.load_model("base")
result = model.transcribe("test.mp3", language="zh", word_timestamps=True)
print(result["text"])  # 应该输出识别文本
```

---

### 第2天：Java集成（4小时）

**任务2.1：创建Python识别脚本（30分钟）**
```
文件：whisper_transcribe.py
功能：
- 接收音频文件路径
- 调用Whisper识别
- 返回JSON格式的逐字时间戳
```

**任务2.2：创建WhisperService（1.5小时）**
```
文件：WhisperService.java
功能：
- 保存音频到临时文件
- 调用Python脚本
- 解析JSON结果
- 返回WordTimestamp列表
```

**任务2.3：配置文件（30分钟）**
```
文件：application.yml
添加：
- whisper.python.path
- whisper.script.path
```

**任务2.4：单元测试（1.5小时）**
```
文件：WhisperServiceTest.java
测试：
- 识别准确性
- 时间戳连续性
- 异常处理
```

**交付物：**
- whisper_transcribe.py
- WhisperService.java
- WhisperServiceTest.java
- application.yml更新

**验收标准：**
```java
WhisperService whisperService = new WhisperService();
List<WordTimestamp> words = whisperService.transcribe(audioData);
System.out.println(words.size());  // 应该输出字数
```

---

### 第3天：TTS集成（4小时）

**任务3.1：修改DocumentTTSServiceImpl（2小时）**
```
修改：buildDialogSegments()方法
添加：
- 调用WhisperService
- 使用Whisper返回的时间戳
- 回退机制（Whisper失败 → FFprobe）
```

**任务3.2：创建WordTimestamp DTO（30分钟）**
```
文件：WordTimestamp.java
字段：
- text: String
- startTime: Double
- endTime: Double
```

**任务3.3：异常处理（1小时）**
```
添加：
- WhisperException
- 超时控制
- 回退机制
```

**任务3.4：日志完善（30分钟）**
```
添加：
- 识别开始/结束日志
- 耗时统计
- 准确率统计
```

**交付物：**
- DocumentTTSServiceImpl修改完成
- WordTimestamp.java
- WhisperException.java
- 日志完善

**验收标准：**
```java
// 上传Word文档
MultipartFile file = ...;
DocumentTTSResult result = documentTTSService.generateDocumentSpeech(file, voiceConfig);

// 检查日志
// 应该看到："使用Whisper识别音频..."
// 应该看到："Whisper识别完成，字数：XX"
```

---

### 第4天：测试优化（4小时）

**任务4.1：完整流程测试（1.5小时）**
```
测试：
1. 上传Word文档
2. 生成视频
3. 播放视频，检查字幕同步
4. 验证准确率
```

**任务4.2：性能优化（1.5小时）**
```
优化：
- 异步处理Whisper识别
- 结果缓存（相同文本不重复识别）
- 批量处理
```

**任务4.3：异常场景测试（1小时）**
```
测试：
- Whisper识别失败 → 回退到FFprobe
- Python脚本错误 → 捕获异常
- 超时 → 自动终止
```

**交付物：**
- 完整测试报告
- 性能优化代码
- 异常处理完善

**验收标准：**
- 测试视频字幕100%同步
- Whisper识别耗时<10秒/分钟音频
- 异常场景正常回退

---

### 第5天：上线验证（2小时）

**任务5.1：生成测试视频（30分钟）**
```
测试样本：
1. 短视频（1分钟）
2. 中等视频（5分钟）
3. 长视频（10分钟）
```

**任务5.2：人工验证（1小时）**
```
验证内容：
1. 字幕与语音同步程度
2. 识别准确率
3. 特殊字符处理
4. 标点符号准确性
```

**任务5.3：性能测试（30分钟）**
```
测试：
- 并发10个请求
- 检查CPU/内存使用
- 检查识别速度
```

**交付物：**
- 测试视频3个
- 验证报告
- 性能测试报告

**验收标准：**
- 字幕同步准确率>90%
- 识别准确率>85%
- 无明显卡顿和错误

---

## 📁 文件结构

```
hm-service/
├── scripts/
│   └── whisper_transcribe.py          ← 第2天创建
│
├── src/main/java/com/hmall/tts/
│   ├── whisper/
│   │   ├── service/
│   │   │   ├── WhisperService.java          ← 第2天创建
│   │   │   └── WhisperServiceImpl.java
│   │   ├── dto/
│   │   │   └── WordTimestamp.java           ← 第3天创建
│   │   └── exception/
│   │       └── WhisperException.java        ← 第3天创建
│   │
│   └── volcengine/service/impl/
│       └── DocumentTTSServiceImpl.java      ← 第3天修改
│
└── src/main/resources/
    └── application.yml                      ← 第2天修改
```

---

## 🎯 关键里程碑

### 里程碑1：环境就绪（第1天结束）
```
✅ Python + Whisper安装完成
✅ 测试识别成功
✅ 可以开始Java集成
```

### 里程碑2：Java集成完成（第2天结束）
```
✅ WhisperService开发完成
✅ 可以从Java调用Whisper
✅ 返回逐字时间戳
```

### 里程碑3：TTS集成完成（第3天结束）
```
✅ 集成到DocumentTTSServiceImpl
✅ 可以生成100%同步的字幕
✅ 有完整的回退机制
```

### 里程碑4：测试完成（第4天结束）
```
✅ 完整流程测试通过
✅ 性能优化完成
✅ 异常处理完善
```

### 里程碑5：上线验证（第5天结束）
```
✅ 测试视频验证通过
✅ 准确率达标（>85%）
✅ 性能达标
✅ 正式上线
```

---

## 📊 预期效果

### 同步准确度
```
Whisper base：
- 逐字准确率：88-92%
- 时间戳精度：±50ms
- 100%同步（字幕和语音完全对齐）

vs 当前方案：
- 句子准确率：95%
- 逐字估算：90-95%
- 时间戳精度：±100ms
```

### 成本节省
```
Whisper base（完全免费）：
- 软件成本：0元
- 运营成本：0元/年

vs 火山ASR：
- 节省：1080元/年

vs Whisper API：
- 节省：7200元/年
```

### 性能表现
```
Whisper base：
- 1分钟音频：约8秒识别
- 5分钟音频：约40秒识别
- CPU占用：中等
- 内存占用：约500MB
```

---

## 🔧 风险和应对

### 风险1：Whisper识别速度慢

**应对措施：**
```
1. 异步处理（不阻塞主线程）
2. 结果缓存（相同文本不重复识别）
3. 批量处理（多个音频并行识别）
4. GPU加速（可选，如果有GPU）
```

### 风险2：Python环境问题

**应对措施：**
```
1. 详细的安装文档
2. 环境检查脚本
3. Docker镜像（备选方案）
4. 完整的故障排查指南
```

### 风险3：识别准确率不够

**应对措施：**
```
1. 回退到当前FFprobe方案
2. 或升级到Whisper small模型（500MB，准确率92-95%）
3. 或最终升级到MFA（2GB，准确率95-97%）
```

---

## ✅ 成功标准

### 必须达成：
- ✅ Whisper识别成功运行
- ✅ 字幕与语音100%同步
- ✅ 识别准确率>85%
- ✅ 无严重bug
- ✅ 有回退机制

### 期望达成：
- ✅ 识别准确率>88%
- ✅ 识别速度<10秒/分钟音频
- ✅ 异步处理不阻塞
- ✅ 完善的日志和监控

---

## 🚀 下一步

**立即开始第1天任务：**
1. 下载Python 3.11安装包
2. 安装Python（勾选Add to PATH）
3. 安装openai-whisper
4. 下载base模型
5. 测试识别

**详细步骤请查看：**
`Whisper实施指南-第1天.md`

---

**计划制定时间：** 2026-08-14 19:15  
**预计完成时间：** 2026-08-19  
**总投入：** 5天（约15小时）  
**总成本：** 0元（完全免费）  
**作者：** Kiro AI Assistant
