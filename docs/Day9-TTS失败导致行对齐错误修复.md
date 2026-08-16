# Day 9 - TTS失败导致行对齐错误修复

> **创建时间：** 2026-08-16  
> **问题：** TTS失败时audioSegments和lines数量不匹配，导致最后的行无法处理  
> **解决方案：** 保持audioSegments和lines一一对应，TTS失败时返回空音频占位

---

## 🔍 问题诊断

### 现象1：最后一句话没有被处理

**日志表现：**
```
15:49:36:513  WARN [WhisperX] 当前行没有音频片段，跳过WhisperX对齐，使用智能算法
```

**根本原因：**
- TTS调用失败时返回null
- null被过滤掉：`if (segment != null) { results.add(segment); }`
- 导致audioSegments.size() < lines.size()
- 最后一行遍历时，audioIndex已经超出范围，lineAudioSegments为空

### 现象2：时间戳对齐已经正确

从日志看到时间戳归零化成功：
```
[WhisperX转换] 字符[1]「我」: WhisperX相对=0.000s ← ✅ 已经从0开始
[WhisperX转换] 字符[1]「你」: WhisperX相对=0.000s ← ✅ 已经从0开始
```

Day 8的归零化修复已经生效！

---

## ✅ 解决方案

### 核心思想

**保持audioSegments和lines的一一对应关系**

```
Word文档解析：
  行1：你好，我是云舟
  行2：你好，云舟我也很高兴认识你
  行3：你来自哪里？
  行4：我来在吉林，你呢
  行5：我来在大连
  行6：我喜欢打羽毛球、打棒球...

TTS生成（修复前）：
  segment1 ✅
  segment2 ✅
  segment3 ✅
  segment4 ✅
  segment5 ❌ TTS失败 → 返回null → 被过滤
  segment6 ✅

结果：audioSegments.size() = 5, lines.size() = 6 → ❌ 不匹配！

TTS生成（修复后）：
  segment1 ✅
  segment2 ✅
  segment3 ✅
  segment4 ✅
  segment5 ❌ TTS失败 → 返回空音频占位
  segment6 ✅

结果：audioSegments.size() = 6, lines.size() = 6 → ✅ 一一对应！
```

---

## 📝 代码修改

### 修改1：synthesizeParallel - TTS失败时返回空音频

**文件：** `DocumentTTSServiceImpl.java`

**修改前：**
```java
} catch (Exception e) {
    log.error("TTS合成失败，文本: [{}], 错误: {}", segment.getText(), e.getMessage());
    // ✅ 关键修改：返回null而不是抛异常，让其他片段继续处理
    return null;
}
```

**修改后：**
```java
} catch (Exception e) {
    log.error("❌ TTS合成失败，文本: [{}], 错误: {}", segment.getText(), e.getMessage());
    // ✅ Day 9修复：不返回null，返回空音频占位，保持与原始segment的一一对应
    AudioSegment emptySegment = new AudioSegment(new byte[0], segment);
    emptySegment.setAccurateDuration(0.0);
    return emptySegment;
}
```

**原理：**
- 返回空字节数组而不是null
- 保持audioSegments和lines的数量一致
- 后续处理中会跳过空音频

---

### 修改2：synthesizeParallel - 收集结果时不过滤

**修改前：**
```java
// 收集结果（保持原始顺序，过滤null）
List<AudioSegment> results = new ArrayList<>();
for (CompletableFuture<AudioSegment> future : futures) {
    try {
        AudioSegment segment = future.get();
        if (segment != null) {  // ← 过滤null
            results.add(segment);
        } else {
            log.warn("跳过失败的TTS片段");
        }
    } catch (Exception e) {
        log.warn("某个TTS请求失败: {}", e.getMessage());
    }
}
```

**修改后：**
```java
// 收集结果（保持原始顺序，不过滤）
List<AudioSegment> results = new ArrayList<>();
for (CompletableFuture<AudioSegment> future : futures) {
    try {
        AudioSegment segment = future.get();
        // ✅ Day 9修复：保留所有segment，包括失败的（空音频）
        results.add(segment);
        if (segment.getAudioData().length == 0) {
            log.warn("⚠️ TTS片段失败，文本: [{}]，将跳过此行的WhisperX对齐", 
                     segment.getMergedSegment().getText());
        }
    } catch (Exception e) {
        log.error("❌ TTS请求异常: {}", e.getMessage());
    }
}
```

**原理：**
- 不再过滤，所有segment都加入results
- 但会记录哪些segment失败了

---

### 修改3：buildDialogSegments - 检测空音频

**修改前：**
```java
// 如果当前行没有音频片段，直接使用智能算法
if (lineAudioSegments.isEmpty()) {
    log.warn("[WhisperX] 当前行没有音频片段，跳过WhisperX对齐，使用智能算法");
    actualLineDuration = lineDuration;
    charTimings = buildCharTimings(line.text, currentTime, actualLineDuration);
} else {
```

**修改后：**
```java
// 如果当前行没有音频片段，或者所有音频片段都是失败的（空音频），直接使用智能算法
boolean hasValidAudio = false;
for (AudioSegment seg : lineAudioSegments) {
    if (seg.getAudioData() != null && seg.getAudioData().length > 0) {
        hasValidAudio = true;
        break;
    }
}

if (lineAudioSegments.isEmpty() || !hasValidAudio) {
    log.warn("[WhisperX] 当前行「{}」没有有效音频片段，跳过WhisperX对齐，使用智能算法", 
             line.text.length() > 20 ? line.text.substring(0, 20) + "..." : line.text);
    actualLineDuration = lineDuration;
    charTimings = buildCharTimings(line.text, currentTime, actualLineDuration);
} else {
```

**原理：**
- 不仅检查lineAudioSegments是否为空
- 还检查是否所有音频都是空的（TTS失败）
- 如果没有有效音频，降级到智能算法

---

### 修改4：逐个处理AudioSegment时跳过空音频

**修改前：**
```java
int segmentIndex = 0;
for (AudioSegment audioSegment : lineAudioSegments) {
    segmentIndex++;
    
    // 获取segment对应的文本
    String segmentText = audioSegment.getMergedSegment().getText();
```

**修改后：**
```java
int segmentIndex = 0;
for (AudioSegment audioSegment : lineAudioSegments) {
    segmentIndex++;
    
    // ✅ Day 9新增：跳过空音频（TTS失败的段落）
    if (audioSegment.getAudioData() == null || audioSegment.getAudioData().length == 0) {
        log.warn("[WhisperX] Segment {} 音频为空（TTS失败），跳过", segmentIndex);
        continue;
    }
    
    // 获取segment对应的文本
    String segmentText = audioSegment.getMergedSegment().getText();
```

**原理：**
- 在处理WhisperX对齐时，跳过空音频
- 避免传递空数据给WhisperX

---

### 修改5：mergeLineAudioSegments - 只合并有效音频

**修改前：**
```java
private byte[] mergeLineAudioSegments(List<AudioSegment> audioSegments, VoiceConfig voiceConfig) {
    try {
        // ✅ 只合并纯语音，不添加停顿（WhisperX需要纯语音）
        List<byte[]> pureAudioList = new ArrayList<>();
        for (AudioSegment segment : audioSegments) {
            pureAudioList.add(segment.getAudioData());
        }
        
        // 使用简单合并（无停顿）
        byte[] mergedAudio = audioMerger.mergeSimple(pureAudioList);
        
        log.debug("[WhisperX] 合并了{}个纯语音片段（无停顿），总大小：{} KB", 
                 audioSegments.size(), mergedAudio.length / 1024.0);
        
        return mergedAudio;
    } catch (Exception e) {
        log.error("[WhisperX] 音频合并失败", e);
        return null;
    }
}
```

**修改后：**
```java
private byte[] mergeLineAudioSegments(List<AudioSegment> audioSegments, VoiceConfig voiceConfig) {
    try {
        // ✅ Day 9修复：只合并有效的纯语音，过滤空音频（TTS失败的）
        List<byte[]> pureAudioList = new ArrayList<>();
        for (AudioSegment segment : audioSegments) {
            if (segment.getAudioData() != null && segment.getAudioData().length > 0) {
                pureAudioList.add(segment.getAudioData());
            } else {
                log.debug("[WhisperX] 跳过空音频segment: {}", 
                         segment.getMergedSegment().getText());
            }
        }
        
        if (pureAudioList.isEmpty()) {
            log.warn("[WhisperX] 所有音频segment都是空的，无法合并");
            return null;
        }
        
        // 使用简单合并（无停顿）
        byte[] mergedAudio = audioMerger.mergeSimple(pureAudioList);
        
        log.debug("[WhisperX] 合并了{}个纯语音片段（无停顿），总大小：{} KB", 
                 pureAudioList.size(), mergedAudio.length / 1024.0);
        
        return mergedAudio;
    } catch (Exception e) {
        log.error("[WhisperX] 音频合并失败", e);
        return null;
    }
}
```

**原理：**
- 过滤空音频，只合并有效音频
- 如果所有音频都是空的，返回null
- 避免传递空数据给AudioMerger

---

## 🎯 修复效果

### 修复前：

```
Word文档：6行
TTS生成：5个有效 + 1个失败（被过滤）
audioSegments.size() = 5
lines.size() = 6

结果：
  行0-4 ✅ 正常处理
  行5 ❌ 没有对应的audioSegment → 跳过WhisperX对齐 → 使用智能算法

问题：最后一行无法使用WhisperX高精度对齐
```

### 修复后：

```
Word文档：6行
TTS生成：5个有效 + 1个失败（保留空音频占位）
audioSegments.size() = 6
lines.size() = 6

结果：
  行0-4 ✅ 正常处理（WhisperX对齐）
  行5 ⚠️ 音频为空 → 跳过WhisperX对齐 → 使用智能算法

优势：
  1. audioSegments和lines一一对应
  2. 所有行都能找到对应的audioSegment
  3. 失败的行会明确标记并降级处理
  4. 不影响其他行的正常处理
```

---

## 🧪 测试验证

### 测试用例1：正常情况（所有TTS成功）

**输入：**
```
行1：你好，我是云舟
行2：你好，云舟我也很高兴认识你
行3：你来自哪里？
```

**预期结果：**
```
✅ 3个audioSegment都有效
✅ 所有行都使用WhisperX对齐
✅ 字幕与音频完美同步
```

### 测试用例2：部分TTS失败

**输入：**
```
行1：你好，我是云舟
行2：你好，云舟我也很高兴认识你  ← TTS失败
行3：你来自哪里？
```

**预期结果：**
```
✅ audioSegment1: 有效
⚠️ audioSegment2: 空音频（TTS失败）
✅ audioSegment3: 有效

✅ 行1：WhisperX对齐
⚠️ 行2：降级到智能算法
✅ 行3：WhisperX对齐
```

### 测试用例3：最后一行TTS失败

**输入：**
```
行1：你好，我是云舟
行2：你好，云舟我也很高兴认识你
行3：你来自哪里？  ← TTS失败
```

**预期结果：**
```
✅ audioSegment1: 有效
✅ audioSegment2: 有效
⚠️ audioSegment3: 空音频（TTS失败）

✅ 行1：WhisperX对齐
✅ 行2：WhisperX对齐
⚠️ 行3：降级到智能算法

关键：行3不会被完全跳过，而是使用智能算法
```

---

## 📊 预期日志

### 修复后的日志（正常情况）：

```
[INFO] 步骤3: 并发生成语音（3个API调用）...
[DEBUG] 开始生成音频，音色: BV001, 文本长度: 8, 文本内容: [你好，我是云舟]
[DEBUG] 音频生成完成，大小: 12.5 KB
[DEBUG] FFprobe精确时长: 1.632秒

[WhisperX] === 开始处理行 0 ===
[WhisperX] 行文本：「你好，我是云舟」
[WhisperX] 共1个segment
[WhisperX] ✅ 对齐成功，字符数：8，准确率：98-99%
```

### 修复后的日志（TTS失败情况）：

```
[INFO] 步骤3: 并发生成语音（3个API调用）...
[DEBUG] 开始生成音频，音色: BV001, 文本长度: 8, 文本内容: [你好，我是云舟]
[ERROR] ❌ TTS合成失败，文本: [你好，我是云舟], 错误: Connection timeout
[WARN] ⚠️ TTS片段失败，文本: [你好，我是云舟]，将跳过此行的WhisperX对齐

[WhisperX] === 开始处理行 0 ===
[WhisperX] 行文本：「你好，我是云舟」
[WhisperX] 共1个segment
[WARN] [WhisperX] Segment 1 音频为空（TTS失败），跳过
[WARN] [WhisperX] 当前行「你好，我是云舟」没有有效音频片段，跳过WhisperX对齐，使用智能算法
```

---

## 🔧 故障排查

### 问题1：所有行都使用智能算法

**症状：**
```
[WARN] [WhisperX] 当前行「...」没有有效音频片段，跳过WhisperX对齐，使用智能算法
```

**可能原因：**
1. 所有TTS调用都失败了
2. AudioSegment.getAudioData()返回null或空数组

**检查：**
```
# 查看TTS生成日志
[ERROR] ❌ TTS合成失败，文本: [...]

# 查看音频数据大小
[DEBUG] 音频生成完成，大小: 0 KB  ← 异常！应该大于0
```

### 问题2：最后一行仍然被跳过

**症状：**
```
只处理了前5行，第6行完全没有日志
```

**可能原因：**
- 修改没有生效，仍然在过滤null
- lines的数量计算有误

**检查：**
```java
// 确认修改已生效
if (segment != null) {  // ← 应该已删除
    results.add(segment);
}

// 应该改为
results.add(segment);  // ← 不过滤
```

---

## ✅ 修改总结

**核心思想：**
- 保持audioSegments和lines的一一对应
- TTS失败时返回空音频占位，而不是null
- 处理时跳过空音频，降级到智能算法

**影响范围：**
- 5个方法修改
- 0个新方法
- 0个文件删除

**兼容性：**
- ✅ 向后兼容（不影响现有功能）
- ✅ 不影响Day 8的归零化修复
- ✅ 不影响正常TTS流程

---

**文档版本：** v1.0  
**最后更新：** 2026-08-16  
**作者：** Kiro AI Assistant
