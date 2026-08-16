# WhisperX批量对齐深度检查报告

> **检查时间：** 2026-08-16 18:50  
> **检查范围：** 完整的批量对齐流程  
> **状态：** 发现1个潜在问题

---

## ✅ 已确认正确的部分

### 1. 批量收集逻辑（第230-283行）

**代码：**
```java
// ✅ Day 10批量优化：先收集所有需要对齐的音频和文本
log.info("[WhisperX] === 开始批量收集对齐任务 ===");

List<byte[]> allSegmentAudios = new ArrayList<>();
List<String> allSegmentTexts = new ArrayList<>();
Map<String, Integer> segmentToBatchIndexMap = new HashMap<>();

int audioIndex = 0;
int batchIndex = 0;

for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
    LineInfo line = lines.get(lineIndex);
    int segmentIndexInLine = 0;
    
    while (audioIndex < audioSegments.size()) {
        AudioSegment audioSegment = audioSegments.get(audioIndex);
        
        // 收集有效音频segment
        if (audioSegment.getAudioData() != null && audioSegment.getAudioData().length > 0) {
            allSegmentAudios.add(audioSegment.getAudioData());
            allSegmentTexts.add(audioSegment.getMergedSegment().getText());
            
            String key = lineIndex + "-" + segmentIndexInLine;
            segmentToBatchIndexMap.put(key, batchIndex);
            batchIndex++;
        }
        
        segmentIndexInLine++;  // ✅ 无论有无音频，都递增
        audioIndex++;
        
        // 检查是否同一行
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
}
```

**检查结果：✅ 正确**
- 遍历所有行和segment
- 只收集有效音频（非空）
- 正确记录映射关系（lineIndex-segmentIndexInLine → batchIndex）
- segmentIndexInLine无论有无音频都递增（与第二遍遍历保持一致）

---

### 2. 批量对齐调用（第285-300行）

**代码：**
```java
List<List<com.hmall.tts.whisperx.dto.CharTimestamp>> batchResults = null;
if (!allSegmentAudios.isEmpty()) {  // ✅ 修复后：无条件执行
    try {
        log.info("[WhisperX] === 开始批量对齐 ===");
        long batchStartTime = System.currentTimeMillis();
        
        batchResults = whisperXService.alignBatch(allSegmentAudios, allSegmentTexts);
        
        long batchElapsedTime = System.currentTimeMillis() - batchStartTime;
        log.info("[WhisperX] ✅ 批量对齐完成，总耗时：{} ms，平均每个：{} ms", 
                 batchElapsedTime, batchElapsedTime / allSegmentAudios.size());
        
    } catch (Exception e) {
        log.warn("[WhisperX] 批量对齐失败，将回退到智能算法：{}", e.getMessage());
        batchResults = null;
    }
}
```

**检查结果：✅ 正确**
- 只要有segment就会执行
- 调用`whisperXService.alignBatch()`
- 异常处理：失败后回退到智能算法

---

### 3. 批量结果应用（第370-435行）

**代码：**
```java
for (int segIdx = 0; segIdx < lineAudioSegments.size(); segIdx++) {
    AudioSegment audioSegment = lineAudioSegments.get(segIdx);
    
    // ✅ 使用映射关系获取批量结果的索引
    String key = lineIndex + "-" + segIdx;  // ✅ 直接使用segIdx
    Integer batchResultIdx = segmentToBatchIndexMap.get(key);
    
    // 跳过空音频
    if (audioSegment.getAudioData() == null || audioSegment.getAudioData().length == 0) {
        log.debug("[WhisperX] Segment {} 音频为空，跳过", segIdx + 1);
        continue;  // ✅ 继续循环，segIdx会自动递增
    }
    
    List<com.hmall.tts.whisperx.dto.CharTimestamp> whisperXChars = null;
    if (batchResults != null && batchResultIdx != null && batchResultIdx < batchResults.size()) {
        whisperXChars = batchResults.get(batchResultIdx);
    }
    
    if (whisperXChars != null && !whisperXChars.isEmpty()) {
        // ✅ 使用WhisperX结果
        // ...
    } else {
        // ✅ 回退到智能算法
        // ...
    }
}
```

**检查结果：✅ 正确**
- 使用segIdx直接构建key（lineIndex-segIdx）
- 从映射表中查找batchIndex
- 从batchResults中获取对齐结果
- 空音频自动跳过（continue）
- 批量结果缺失时回退到智能算法

---

## ⚠️ 发现的潜在问题

### 问题1：空音频导致索引不对齐（已修复）

**场景：**
```
行0：segment0（有音频）、segment1（空音频）、segment2（有音频）
```

**第一遍遍历（收集）：**
```java
segmentIndexInLine = 0;
处理segment0：有音频 → 收集 → map["0-0"] = 0 → segmentIndexInLine++
处理segment1：空音频 → 跳过 → segmentIndexInLine++
处理segment2：有音频 → 收集 → map["0-2"] = 1 → segmentIndexInLine++
```

**第二遍遍历（应用）：**
```java
for (int segIdx = 0; segIdx < 3; segIdx++) {
    String key = "0-" + segIdx;
    
    segIdx=0: key="0-0" → map["0-0"] = 0 ✅ 找到
    segIdx=1: key="0-1" → map["0-1"] = null ❌ 找不到（因为segment1是空音频）
    segIdx=2: key="0-2" → map["0-2"] = 1 ✅ 找到
}
```

**检查结果：✅ 正确处理**
- segIdx=1时，key="0-1"找不到（返回null）
- 代码检查`batchResultIdx != null`，所以不会出错
- segment1是空音频，会被`if (audioSegment.getAudioData() == null)`跳过
- 逻辑正确！

---

### 问题2：批量对齐失败后的处理（需要确认）

**当前代码：**
```java
if (whisperXChars != null && !whisperXChars.isEmpty()) {
    // 使用WhisperX结果
} else {
    // 回退到智能算法
    log.warn("[WhisperX] Segment {} 使用智能算法（批量结果缺失）", segIdx + 1);
}
```

**可能的原因：**
1. batchResults为null（批量对齐失败）
2. batchResultIdx为null（找不到映射）
3. batchResultIdx >= batchResults.size()（索引越界）
4. whisperXChars为空列表（对齐成功但无结果）

**建议：增加详细日志**
```java
if (batchResults == null) {
    log.warn("[WhisperX] Segment {} 使用智能算法（批量对齐失败，batchResults为null）", segIdx + 1);
} else if (batchResultIdx == null) {
    log.warn("[WhisperX] Segment {} 使用智能算法（找不到映射，key={}）", segIdx + 1, key);
} else if (batchResultIdx >= batchResults.size()) {
    log.warn("[WhisperX] Segment {} 使用智能算法（索引越界，batchResultIdx={}, size={}）", 
             segIdx + 1, batchResultIdx, batchResults.size());
} else if (whisperXChars == null || whisperXChars.isEmpty()) {
    log.warn("[WhisperX] Segment {} 使用智能算法（对齐结果为空）", segIdx + 1);
} else {
    log.warn("[WhisperX] Segment {} 使用智能算法（未知原因）", segIdx + 1);
}
```

---

## 🔧 建议的优化

### 优化1：增加详细的调试日志

**位置：** 第390行附近

**修改：**
```java
// ✅ 使用映射关系获取批量结果的索引
String key = lineIndex + "-" + segIdx;
Integer batchResultIdx = segmentToBatchIndexMap.get(key);

// ✅ 增加调试日志
if (log.isDebugEnabled()) {
    log.debug("[WhisperX] 查找映射：key={}, batchResultIdx={}, batchResults.size()={}", 
              key, batchResultIdx, (batchResults != null ? batchResults.size() : "null"));
}

// 跳过空音频
if (audioSegment.getAudioData() == null || audioSegment.getAudioData().length == 0) {
    log.debug("[WhisperX] Segment {} 音频为空，跳过", segIdx + 1);
    continue;
}

List<com.hmall.tts.whisperx.dto.CharTimestamp> whisperXChars = null;
if (batchResults != null && batchResultIdx != null && batchResultIdx < batchResults.size()) {
    whisperXChars = batchResults.get(batchResultIdx);
    
    // ✅ 增加调试日志
    if (log.isDebugEnabled()) {
        log.debug("[WhisperX] 获取批量结果：batchResultIdx={}, 字符数={}", 
                  batchResultIdx, (whisperXChars != null ? whisperXChars.size() : "null"));
    }
}

if (whisperXChars != null && !whisperXChars.isEmpty()) {
    // 使用WhisperX结果
    // ...
} else {
    // ✅ 详细的回退原因
    if (batchResults == null) {
        log.warn("[WhisperX] Segment {} 使用智能算法（批量对齐失败）", segIdx + 1);
    } else if (batchResultIdx == null) {
        log.warn("[WhisperX] Segment {} 使用智能算法（找不到映射，key={}）", segIdx + 1, key);
    } else if (batchResultIdx >= batchResults.size()) {
        log.warn("[WhisperX] Segment {} 使用智能算法（索引越界，idx={}, size={}）", 
                 segIdx + 1, batchResultIdx, batchResults.size());
    } else {
        log.warn("[WhisperX] Segment {} 使用智能算法（对齐结果为空）", segIdx + 1);
    }
    
    // 回退到智能算法
    // ...
}
```

---

### 优化2：验证映射表的完整性

**位置：** 第283行之后

**添加：**
```java
log.info("[WhisperX] 收集完成，共{}个segment需要对齐", allSegmentAudios.size());

// ✅ 验证映射表
if (log.isDebugEnabled()) {
    log.debug("[WhisperX] 映射表大小：{}", segmentToBatchIndexMap.size());
    log.debug("[WhisperX] 映射表内容：{}", segmentToBatchIndexMap);
}

// ✅ 验证：映射表大小应该等于收集的音频数量
if (segmentToBatchIndexMap.size() != allSegmentAudios.size()) {
    log.error("[WhisperX] ⚠️ 映射表大小不一致：map.size()={}, audios.size()={}", 
              segmentToBatchIndexMap.size(), allSegmentAudios.size());
}
```

---

## 📊 预期执行流程

### 正常情况（批量对齐成功）

```
[WhisperX] === 开始批量收集对齐任务 ===
[WhisperX] 收集：行0-segment0 → batch0
[WhisperX] 收集：行0-segment1 → batch1
[WhisperX] 收集：行1-segment0 → batch2
...
[WhisperX] 收集完成，共7个segment需要对齐
[WhisperX] 映射表大小：7
[WhisperX] === 开始批量对齐 ===
[WhisperX] 使用HTTP批量接口，音频数量：7
[WhisperX] ✅ HTTP批量对齐完成，音频数量：7，成功：7，失败：0，耗时：2100 ms
[WhisperX] ✅ 批量对齐完成，总耗时：2100 ms，平均每个：300 ms
[WhisperX] === 开始应用对齐结果 ===
[WhisperX] === 处理行 0 ===
[WhisperX] 查找映射：key=0-0, batchResultIdx=0, batchResults.size()=7
[WhisperX] 获取批量结果：batchResultIdx=0, 字符数=17
[WhisperX] Segment 1 对齐成功，字符数：17，时长：2.480秒
[WhisperX] 查找映射：key=0-1, batchResultIdx=1, batchResults.size()=7
[WhisperX] 获取批量结果：batchResultIdx=1, 字符数=13
[WhisperX] Segment 2 对齐成功，字符数：13，时长：2.380秒
...
[WhisperX] 行 0 完成，字符数：30，实际时长：4.860秒
```

---

### 异常情况（批量对齐失败）

```
[WhisperX] === 开始批量收集对齐任务 ===
[WhisperX] 收集完成，共7个segment需要对齐
[WhisperX] === 开始批量对齐 ===
[WhisperX] HTTP服务不可用：Connection refused
[WhisperX] 服务不可用，回退到逐个处理模式
[WhisperX] 批量对齐失败，将回退到智能算法：Connection refused
[WhisperX] === 开始应用对齐结果 ===
[WhisperX] === 处理行 0 ===
[WhisperX] Segment 1 使用智能算法（批量对齐失败）
[WhisperX] Segment 2 使用智能算法（批量对齐失败）
...
```

---

## 🎯 检查结论

### ✅ 已确认正确的部分
1. 批量收集逻辑：正确遍历所有segment，记录映射关系
2. 批量对齐调用：无条件执行（只要有segment）
3. 批量结果应用：正确使用映射表查找结果
4. 空音频处理：正确跳过，不会导致索引错位
5. 异常处理：批量失败后回退到智能算法

### ⚠️ 需要优化的部分
1. 日志不够详细：无法判断回退到智能算法的具体原因
2. 缺少验证：映射表大小与音频数量不一致时无警告

### 🚀 建议
1. 增加详细的调试日志（优化1）
2. 验证映射表完整性（优化2）
3. 重启Java应用后，上传文档测试
4. 观察日志，确认批量对齐是否生效

---

## 📝 测试检查清单

启动后测试文档TTS，检查以下日志：

- [ ] `[WhisperX] === 开始批量收集对齐任务 ===`
- [ ] `[WhisperX] 收集完成，共X个segment需要对齐`
- [ ] `[WhisperX] === 开始批量对齐 ===`
- [ ] `[WhisperX] 使用HTTP批量接口，音频数量：X`
- [ ] `[WhisperX] ✅ HTTP批量对齐完成`
- [ ] `[WhisperX] Segment X 对齐成功`（不是"使用智能算法"）

**如果看到：**
- `[WhisperX] Segment X 使用智能算法（批量结果缺失）`
  - 说明批量对齐没有生效，需要查看详细日志

**如果看到：**
- `[WhisperX] HTTP服务不可用：Connection refused`
  - 说明WhisperX服务未启动或端口不对

---

**检查报告版本：** v1.0  
**最后更新：** 2026-08-16 18:55  
**作者：** Kiro AI助手
