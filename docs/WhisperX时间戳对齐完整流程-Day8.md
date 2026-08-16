# WhisperX时间戳对齐完整流程 - Day 8 重构版

> **创建时间：** 2026-08-16  
> **状态：** 已完成重构，待测试验证

---

## 📋 核心原理

### 1. 时间戳的三种形式

```
相对时间（WhisperX返回）
  ↓ 字符「你」→ 0.0s
  ↓ 字符「好」→ 0.5s
  ↓（相对于当前segment音频的时间）

累积时间（Segment起始）
  ↓ Segment 1 → 0.0s
  ↓ Segment 2 → 1.5s（1.0s音频 + 0.5s停顿）
  ↓ Segment 3 → 3.0s
  ↓（相对于整个文档的累积时间）

绝对时间（最终输出）
  ↓ Segment 2 的「你」→ 1.5s + 0.0s = 1.5s
  ↓ Segment 2 的「好」→ 1.5s + 0.5s = 2.0s
  ↓（在整个文档中的最终时间）
```

### 2. 核心公式（Day 8简化版）

```java
绝对时间 = 累积时间(segmentStartTime) + 相对时间(WhisperX)
```

**去除的复杂逻辑：**
- ❌ TTS静音补偿（ttsHeadSilence）
- ❌ FFprobe与WhisperX时长对比
- ❌ 任何时间偏移的猜测

---

## 🔄 完整处理流程

### 阶段1：文本分段（TTS生成音频）

```
输入：「你好世界！」

分段结果：
  Segment 1: 「你好」+ 停顿0.5秒
  Segment 2: 「世界」+ 停顿0.5秒
  Segment 3: 「！」+ 无停顿

音频生成：
  Audio 1: [你好.mp3] 1.0秒（纯语音）
  Audio 2: [世界.mp3] 1.0秒（纯语音）
  Audio 3: [！.mp3] 0.5秒（纯语音）
```

### 阶段2：逐个Segment处理

#### Segment 1 处理

```java
// 输入
segmentText = "你好"
audioData = [你好.mp3]  // 1.0秒纯语音
segmentStartTime = 0.0  // 第一个segment，从0开始

// WhisperX处理
WhisperX输入：[你好.mp3]（1.0秒纯语音，无停顿）
WhisperX输出（相对时间）：
  "你" → start: 0.0s, end: 0.5s
  "好" → start: 0.5s, end: 1.0s

// 时间戳转换
convertWhisperXToCharTimings(whisperXChars, 0.0):
  "你" → 0.0 + 0.0 = 0.0s
  "好" → 0.0 + 0.5 = 0.5s

// 更新累积时间
segmentDuration = 1.0s（WhisperX实际时长）
segmentStartTime += 1.0  // 累积时间 = 1.0s
segmentStartTime += 0.5  // 加停顿 = 1.5s
```

#### Segment 2 处理

```java
// 输入
segmentText = "世界"
audioData = [世界.mp3]  // 1.0秒纯语音
segmentStartTime = 1.5  // 继承上一个segment的累积时间

// WhisperX处理
WhisperX输入：[世界.mp3]（1.0秒纯语音，无停顿）
WhisperX输出（相对时间）：
  "世" → start: 0.0s, end: 0.5s  ← 相对于当前segment
  "界" → start: 0.5s, end: 1.0s  ← 相对于当前segment

// 时间戳转换
convertWhisperXToCharTimings(whisperXChars, 1.5):
  "世" → 1.5 + 0.0 = 1.5s  ✅ 正确！
  "界" → 1.5 + 0.5 = 2.0s  ✅ 正确！

// 更新累积时间
segmentDuration = 1.0s
segmentStartTime += 1.0  // 累积时间 = 2.5s
segmentStartTime += 0.5  // 加停顿 = 3.0s
```

#### Segment 3 处理

```java
// 输入
segmentText = "！"
audioData = [！.mp3]  // 0.5秒纯语音
segmentStartTime = 3.0  // 继承上一个segment的累积时间

// WhisperX处理
WhisperX输入：[！.mp3]（0.5秒纯语音，无停顿）
WhisperX输出（相对时间）：
  "！" → start: 0.0s, end: 0.5s

// 时间戳转换
convertWhisperXToCharTimings(whisperXChars, 3.0):
  "！" → 3.0 + 0.0 = 3.0s  ✅ 正确！

// 更新累积时间
segmentDuration = 0.5s
segmentStartTime += 0.5  // 累积时间 = 3.5s
// 无停顿
```

### 阶段3：最终结果

```
完整时间轴：
  "你" → 0.0s
  "好" → 0.5s
  [停顿0.5s]
  "世" → 1.5s
  "界" → 2.0s
  [停顿0.5s]
  "！" → 3.0s
  
总时长：3.5秒
```

---

## 🔍 关键代码片段

### 1. Segment处理循环

```java
double segmentStartTime = currentTime;  // 累积时间

for (AudioSegment audioSegment : lineAudioSegments) {
    String segmentText = audioSegment.getMergedSegment().getText();
    
    // 调用WhisperX对齐
    AlignmentResult segmentResult = buildCharTimingsWithWhisper(
        segmentText,
        List.of(audioSegment),
        segmentStartTime,  // ✅ 传入累积时间
        ...,
        voiceConfig
    );
    
    // 收集字符时间戳
    charTimings.addAll(segmentResult.charTimings);
    
    // 更新累积时间
    double segmentDuration = segmentResult.actualSpeechDuration;
    segmentStartTime += segmentDuration;
    
    // 加上停顿
    if (audioSegment.getNeedPause()) {
        double pauseSec = audioSegment.getPauseDuration() / 1000.0;
        segmentStartTime += pauseSec;
    }
}
```

### 2. 时间戳转换函数

```java
private List<CharTiming> convertWhisperXToCharTimings(
        List<CharTimestamp> whisperXChars,
        double startTime) {  // startTime = 累积时间
    
    List<CharTiming> charTimings = new ArrayList<>();
    
    for (CharTimestamp whisperXChar : whisperXChars) {
        // ✅ 核心公式：相对时间 + 累积时间 = 绝对时间
        double absoluteTime = startTime + whisperXChar.getStartTime();
        
        CharTiming charTiming = CharTiming.builder()
                .character(whisperXChar.getCharacter())
                .startTime(absoluteTime)  // 绝对时间
                .duration(whisperXChar.getDuration())
                .build();
        
        charTimings.add(charTiming);
    }
    
    return charTimings;
}
```

### 3. WhisperX Python脚本返回格式

```python
# Python返回的JSON格式
{
    "success": true,
    "text": "你好",
    "chars": [
        {
            "char": "你",
            "start": 0.0,    # 相对时间（从0开始）
            "end": 0.5
        },
        {
            "char": "好",
            "start": 0.5,    # 相对时间
            "end": 1.0
        }
    ],
    "char_count": 2,
    "duration": 1.0
}
```

---

## 🧪 测试验证步骤

### 步骤1：启用详细日志

确保application.yml中的日志级别设置为DEBUG：

```yaml
logging:
  level:
    com.hmall.tts: DEBUG
```

### 步骤2：测试用例

使用以下简单文本测试：

```
第一行：你好
第二行：世界
```

### 步骤3：观察日志输出

关键日志标记：

```
[WhisperX] === 开始处理行 0 ===
[WhisperX] 行文本：「你好」
[WhisperX] 行起始时间：0.000秒（文档累积时间）
[WhisperX] 共1个segment

[WhisperX] --- Segment 1 ---
[WhisperX] Segment文本：「你好」
[WhisperX] Segment起始时间：0.000秒

[WhisperX转换] 开始转换2个字符，Segment起始=0.000秒
[WhisperX转换] 字符[1]「你」: WhisperX相对=0.000s, Segment起始=0.000s, 最终绝对=0.000s, 时长=0.XXXs
[WhisperX转换] 字符[2]「好」: WhisperX相对=0.XXXs, Segment起始=0.000s, 最终绝对=0.XXXs, 时长=0.XXXs

[WhisperX] Segment音频时长：X.XXX秒（WhisperX实际）
[WhisperX] Segment停顿时长：0.800秒
[WhisperX] Segment结束后累积时间：X.XXX秒

[WhisperX] === 行 0 处理完成 ===

[WhisperX] === 开始处理行 1 ===
[WhisperX] 行文本：「世界」
[WhisperX] 行起始时间：X.XXX秒（文档累积时间）  ← 应该接近上一行的结束时间
...
```

### 步骤4：检查点

✅ **检查1：第一个字符的时间接近0**
```
第一行第一个字符「你」→ 应该接近0.000s
```

✅ **检查2：同一segment内时间递增**
```
字符[1]「你」: 最终绝对=0.000s
字符[2]「好」: 最终绝对=0.XXXs  ← 应该大于0.000s
```

✅ **检查3：跨segment时间连续**
```
行0结束累积时间：X.XXXs
行1起始累积时间：X.XXXs  ← 应该相等
```

✅ **检查4：停顿时间体现**
```
Segment结束前累积时间：1.000s
Segment停顿时长：0.800s
Segment结束后累积时间：1.800s  ← 应该是 1.000 + 0.800
```

---

## ❌ 常见问题诊断

### 问题1：字符时间不递增

**症状：**
```
字符[1]「你」: 最终绝对=5.000s
字符[2]「好」: 最终绝对=4.500s  ← 时间倒退！
```

**原因：** WhisperX返回的时间戳有问题

**诊断：**
1. 检查WhisperX Python脚本的输出
2. 确认JSON中的`start`字段递增
3. 检查音频文件是否损坏

### 问题2：第二行时间从0开始

**症状：**
```
行0：「你好」→ 0.0s - 1.0s
行1：「世界」→ 0.0s - 1.0s  ← 应该从1.8s开始！
```

**原因：** `segmentStartTime`没有正确累积

**诊断：**
1. 检查`currentTime`是否正确更新
2. 检查`actualLineDuration`是否包含停顿
3. 检查是否误用了相对时间

### 问题3：停顿时间消失

**症状：**
```
行0结束时间：1.000s
行1开始时间：1.000s  ← 应该是1.800s（包含0.8s停顿）
```

**原因：** 停顿没有加到`segmentStartTime`

**诊断：**
1. 检查`audioSegment.getNeedPause()`是否为true
2. 检查停顿时长是否正确（毫秒→秒转换）
3. 检查`segmentStartTime += pauseSec`是否执行

### 问题4：时间偏移

**症状：**
```
实际音频：「你」在0.5s
字幕显示：「你」在0.8s  ← 偏移0.3s
```

**原因：** 可能是TTS开头有静音

**Day 8解决方案：** 
- WhisperX会自动处理音频的真实时间轴
- 不需要任何静音补偿
- 如果还有偏移，检查音频文件本身

---

## 📊 Day 8 重构总结

### 删除的逻辑

1. ❌ `ttsHeadSilence`计算（TTS静音补偿）
2. ❌ FFprobe与WhisperX时长对比诊断
3. ❌ 任何基于时长差异的猜测逻辑

### 保留的核心

1. ✅ Segment逐个处理
2. ✅ 累积时间(`segmentStartTime`)管理
3. ✅ 停顿时间的显式累加
4. ✅ WhisperX相对时间→绝对时间的简单转换

### 核心优势

```
Before (Day 7):
  绝对时间 = WhisperX时间 + TTS静音补偿 + 累积时间
  （复杂，容易出错）

After (Day 8):
  绝对时间 = 累积时间 + WhisperX时间
  （简单，易理解）
```

---

## 🎯 下一步

1. **重启服务**，启用DEBUG日志
2. **运行测试用例**，观察日志输出
3. **对比预期结果**，检查时间戳是否正确
4. **如果还有问题**，提供完整的日志输出进行进一步分析

---

**文档版本：** v1.0  
**最后更新：** 2026-08-16  
**作者：** Kiro AI Assistant
