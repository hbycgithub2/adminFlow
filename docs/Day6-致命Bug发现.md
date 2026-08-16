# Day 6 - 发现致命Bug！

**发现时间：** 2026-08-16 10:45  
**严重程度：** 🔴 致命！  
**状态：** 已定位，待修复

---

## 🐛 致命Bug定位

### 问题：charTimings和currentTime不匹配！

**代码位置：** `DocumentTTSServiceImpl.java` - buildDialogSegments方法（第280-350行）

---

## 📊 问题分析

### 当前代码逻辑

```java
// 步骤1：计算lineDuration（基于FFprobe + 停顿）
while (audioIndex < audioSegments.size()) {
    double segmentDuration = audioSegment.getAccurateDuration();  // FFprobe时长
    lineDuration += segmentDuration;
    
    if (audioSegment.getNeedPause()) {
        double pauseSec = 0.8;
        lineDuration += pauseSec;
    }
}
// 结果：lineDuration = 1.65s(FFprobe) + 0.8s(停顿) = 2.45s

// 步骤2：生成charTimings（基于WhisperX实际时长）
for (AudioSegment segment : lineAudioSegments) {
    AlignmentResult result = buildCharTimingsWithWhisper(...);
    charTimings.addAll(result.charTimings);  // WhisperX时间戳：0-1.5s
    
    double segmentDuration = result.actualSpeechDuration;  // 1.5s（WhisperX）
    segmentStartTime += segmentDuration;  // 0 + 1.5 = 1.5s
    segmentStartTime += pauseSec;  // 1.5 + 0.8 = 2.3s
}
// 结果：charTimings范围 0-1.5s，segmentStartTime累积到2.3s

// 步骤3：创建DialogSegment
DialogSegment segment = DialogSegment.builder()
    .startTime(currentTime)  // 0s
    .duration(lineDuration)  // 2.45s ← 用的是FFprobe时长！
    .charTimings(charTimings)  // 0-1.5s ← 用的是WhisperX时长！
    .build();

// 步骤4：更新currentTime
currentTime += lineDuration;  // 0 + 2.45 = 2.45s
```

---

## ❌ 问题：两个不同的时长！

### 对比表

| 变量 | 基于什么计算 | 值 |
|------|------------|-----|
| `lineDuration` | FFprobe + 停顿 | 2.45s |
| `charTimings最后字符` | WhisperX实际时长 | 1.5s |
| `segmentStartTime累积` | WhisperX + 停顿 | 2.3s |
| `currentTime更新` | lineDuration | 2.45s |

**发现矛盾：**
```
DialogSegment.duration = 2.45s（FFprobe）
DialogSegment.charTimings最后字符 = 1.5s（WhisperX）
差异：2.45 - 1.5 = 0.95s  ← 巨大！
```

---

## 🎯 具体影响

### 场景：两句话

```
句子1："你好"
  FFprobe时长：1.65s
  WhisperX时长：1.5s
  停顿：0.8s
  lineDuration：1.65 + 0.8 = 2.45s
  charTimings：「你」0s, 「好」0.75s, 结束1.5s
  
句子1的DialogSegment：
  startTime: 0s
  duration: 2.45s  ← 错误！
  charTimings: 0-1.5s
  
currentTime更新：0 + 2.45 = 2.45s

句子2："再见"
  FFprobe时长：1.3s
  WhisperX时长：1.2s
  停顿：0.8s
  lineDuration：1.3 + 0.8 = 2.1s
  
句子2的DialogSegment：
  startTime: 2.45s  ← 基于FFprobe累积
  duration: 2.1s
  charTimings: 「再」2.45s, 「见」3.05s, 结束3.65s  ← 基于FFprobe累积
  
但实际音频中：
  句子1结束：1.5s(语音) + 0.8s(停顿) = 2.3s
  句子2开始：2.3s ← 而不是2.45s！
  
偏差：2.45 - 2.3 = 0.15s（第1句TTS静音）
```

**越到后面偏差越大：**
```
句子1偏差：0.15s
句子2偏差：0.15s + 0.1s = 0.25s
句子3偏差：0.25s + 0.08s = 0.33s
...
累积偏差！
```

---

## ✅ 正确的逻辑

### 应该怎么做

```java
// ❌ 错误：使用FFprobe时长
currentTime += lineDuration;  // lineDuration基于FFprobe

// ✅ 正确：使用WhisperX累积时长
currentTime = segmentStartTime;  // segmentStartTime基于WhisperX
```

**完整修复：**

```java
// 步骤2（修改后）：生成charTimings
double actualLineDuration = 0.0;  // ← 新增：记录实际时长
double segmentStartTime = currentTime;

for (AudioSegment segment : lineAudioSegments) {
    AlignmentResult result = buildCharTimingsWithWhisper(...);
    charTimings.addAll(result.charTimings);
    
    double segmentDuration = result.actualSpeechDuration > 0 ? 
        result.actualSpeechDuration : 
        audioSegment.getAccurateDuration();
    
    segmentStartTime += segmentDuration;
    actualLineDuration += segmentDuration;  // ← 累加实际时长
    
    if (segment.getNeedPause()) {
        double pauseSec = 0.8;
        segmentStartTime += pauseSec;
        actualLineDuration += pauseSec;  // ← 累加停顿
    }
}

// 步骤3（修改后）：创建DialogSegment
DialogSegment segment = DialogSegment.builder()
    .startTime(currentTime)
    .duration(actualLineDuration)  // ← 使用WhisperX实际时长
    .charTimings(charTimings)
    .build();

// 步骤4（修改后）：更新currentTime
currentTime += actualLineDuration;  // ← 使用WhisperX实际时长
// 或者直接：currentTime = segmentStartTime;
```

---

## 🎯 修复效果

### 修复后的时间轴

```
句子1："你好"
  WhisperX时长：1.5s
  停顿：0.8s
  actualLineDuration：1.5 + 0.8 = 2.3s
  charTimings：「你」0s, 「好」0.75s, 结束1.5s
  
句子1的DialogSegment：
  startTime: 0s
  duration: 2.3s  ← 正确！
  charTimings: 0-1.5s
  
currentTime更新：0 + 2.3 = 2.3s

句子2："再见"
  WhisperX时长：1.2s
  停顿：0.8s
  actualLineDuration：1.2 + 0.8 = 2.0s
  
句子2的DialogSegment：
  startTime: 2.3s  ← 正确！基于WhisperX累积
  duration: 2.0s
  charTimings: 「再」2.3s, 「见」2.9s, 结束3.5s
  
实际音频中：
  句子1结束：1.5s + 0.8s = 2.3s ✅
  句子2开始：2.3s ✅
  
偏差：0s  ← 完美！
```

---

## 📋 修复清单

### 需要修改的代码

1. **新增变量：actualLineDuration**
   ```java
   double actualLineDuration = 0.0;
   ```

2. **在循环中累加实际时长**
   ```java
   actualLineDuration += segmentDuration;
   actualLineDuration += pauseSec;
   ```

3. **创建DialogSegment时使用actualLineDuration**
   ```java
   .duration(actualLineDuration)
   ```

4. **更新currentTime使用actualLineDuration**
   ```java
   currentTime += actualLineDuration;
   ```

5. **删除lineDuration的计算和使用**
   ```java
   // ❌ 删除这段代码（步骤2的lineDuration计算）
   // 或者保留但不使用
   ```

---

## 🔧 完整修复代码

```java
// 步骤2：根据AudioSegment的实际音频时长构建DialogSegment
int audioIndex = 0;
for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
    LineInfo line = lines.get(lineIndex);
    
    // 找到对应的AudioSegment（相同音色）
    List<AudioSegment> lineAudioSegments = new ArrayList<>();
    
    while (audioIndex < audioSegments.size()) {
        AudioSegment audioSegment = audioSegments.get(audioIndex);
        lineAudioSegments.add(audioSegment);
        audioIndex++;
        
        // 检查下一个AudioSegment是否属于同一行
        if (audioIndex < audioSegments.size()) {
            AudioSegment nextSegment = audioSegments.get(audioIndex);
            String nextSpeaker = nextSegment.getMergedSegment().getSpeaker();
            if (!nextSpeaker.equals(line.speaker)) {
                break;
            }
        } else {
            break;
        }
    }
    
    // ✅ Day 6修复：记录实际时长
    List<CharTiming> charTimings = new ArrayList<>();
    double actualLineDuration = 0.0;  // ← 新增
    
    if (lineAudioSegments.isEmpty()) {
        log.warn("[WhisperX] 当前行没有音频片段，跳过");
        // 使用估算值
        actualLineDuration = 2.0;
        charTimings = buildCharTimings(line.text, currentTime, actualLineDuration);
    } else {
        double segmentStartTime = currentTime;
        
        for (AudioSegment audioSegment : lineAudioSegments) {
            String segmentText = audioSegment.getMergedSegment().getText();
            
            AlignmentResult segmentResult = buildCharTimingsWithWhisper(
                segmentText,
                List.of(audioSegment),
                segmentStartTime,
                audioSegment.getAccurateDuration() != null ? 
                    audioSegment.getAccurateDuration() : 
                    calculateAudioDuration(audioSegment.getAudioData(), voiceConfig.getFormat(), voiceConfig.getSampleRate()),
                voiceConfig
            );
            
            charTimings.addAll(segmentResult.charTimings);
            
            // ✅ 使用WhisperX实际时长
            double segmentDuration = segmentResult.actualSpeechDuration > 0 ? 
                segmentResult.actualSpeechDuration : 
                (audioSegment.getAccurateDuration() != null ? 
                    audioSegment.getAccurateDuration() : 
                    calculateAudioDuration(audioSegment.getAudioData(), voiceConfig.getFormat(), voiceConfig.getSampleRate()));
            
            segmentStartTime += segmentDuration;
            actualLineDuration += segmentDuration;  // ← 累加
            
            // 加上停顿时间
            if (audioSegment.getNeedPause() != null && audioSegment.getNeedPause()) {
                double pauseSec = (audioSegment.getPauseDuration() != null ? 
                                  audioSegment.getPauseDuration() : 800) / 1000.0;
                segmentStartTime += pauseSec;
                actualLineDuration += pauseSec;  // ← 累加停顿
                
                log.debug("[WhisperX] Segment「{}」音频{}秒 + 停顿{}秒", 
                         segmentText, 
                         String.format("%.3f", segmentDuration),
                         String.format("%.3f", pauseSec));
            }
        }
        
        log.info("[WhisperX] ✅ 行对齐完成，共{}个字符，实际时长: {}秒", 
                 charTimings.size(),
                 String.format("%.3f", actualLineDuration));  // ← 改为actualLineDuration
    }
    
    // 创建DialogSegment
    DialogSegment dialogSegment = DialogSegment.builder()
            .index(lineIndex)
            .text(line.text)
            .isBold(line.isBold)
            .startTime(currentTime)
            .duration(actualLineDuration)  // ← 使用WhisperX实际时长
            .voiceId(line.speaker)
            .charTimings(charTimings)
            .build();
    
    dialogSegments.add(dialogSegment);
    currentTime += actualLineDuration;  // ← 使用WhisperX实际时长
}
```

---

## 🎉 修复总结

### Bug原因

**混用了两个不同的时长计算：**
- `lineDuration`：基于FFprobe时长（包含TTS静音）
- `charTimings`：基于WhisperX时长（不含TTS静音）

**导致：**
- DialogSegment.duration过长（包含TTS静音）
- currentTime累积过快（每句话都多累积0.1-0.15秒）
- 字幕时间戳和currentTime不匹配

### 修复方法

**完全使用WhisperX实际时长：**
- 新增 `actualLineDuration` 变量
- 在循环中累加 `segmentResult.actualSpeechDuration` 和停顿
- DialogSegment.duration使用 `actualLineDuration`
- currentTime更新使用 `actualLineDuration`

### 预期效果

**完美同步：**
- ✅ charTimings和duration一致（都基于WhisperX）
- ✅ currentTime累积准确（基于WhisperX）
- ✅ 没有累积误差
- ✅ 偏差 < 10ms

---

**发现时间：** 2026-08-16 10:45  
**严重程度：** 🔴 致命  
**状态：** 已定位，立即修复

**这就是字幕对应不上的真正原因！**
