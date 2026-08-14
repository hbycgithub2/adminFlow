# BUG修复 - 空文本处理

**修复时间：** 2026-08-14  
**问题编号：** BUG-001  
**严重级别：** P1（阻塞测试）  
**修复状态：** ✅ 已修复

---

## 🐛 问题描述

### 错误信息

```
Caused by: java.lang.Exception: 火山引擎API错误: {"reqid":"","code":45002001,"message":"No readable text!"}
```

### 错误原因

**火山引擎TTS API返回错误：** "No readable text!"（没有可读取的文本）

**根本原因：** 文档中存在空行或只包含空格的文本片段，这些片段被传递给TTS API后，API拒绝处理并返回错误。

---

## 🔍 问题分析

### 问题场景

**文档内容：**
```
你好

我是云舟

很高兴认识你

我还喜欢游泳，滑冰
```

**Word文档特点：**
- 每行文本之间有**空行**（用于视觉分隔）
- Word解析器会解析出这些空行（`text = ""` 或 `text = " "`）

### 问题流程

```
1. Word解析器解析文档
   → 输出：["你好", "", "我是云舟", "", "很高兴认识你", "", "我还喜欢游泳，滑冰"]
   → 问题：包含空字符串片段

2. TextSegmentMerger.mergeNoMerge()
   → 独立模式：每个片段独立包装
   → 空字符串片段也被包装为 MergedSegment
   → 问题：空片段没有被过滤

3. TTS API调用
   → 调用TTS生成音频，text = ""
   → TTS API返回错误："No readable text!"
   → 整个流程失败 ❌
```

---

## 🔧 修复方案

### 修复位置

**文件：** `TextSegmentMerger.java`

**修改方法：**
1. `mergeNoMerge()` - 独立模式
2. `merge()` - 合并模式

### 修复内容

#### 修复1：mergeNoMerge() 方法

**修复前：**
```java
public List<MergedSegment> mergeNoMerge(List<TextSegment> segments) {
    List<MergedSegment> result = new ArrayList<>();
    
    for (TextSegment segment : segments) {
        MergedSegment mergedSegment = new MergedSegment(segment.getSpeaker());
        mergedSegment.addText(segment.getText());  // ❌ 可能是空文本
        mergedSegment.addOriginalSegment(segment);
        result.add(mergedSegment);
    }
    
    return result;
}
```

**修复后：**
```java
public List<MergedSegment> mergeNoMerge(List<TextSegment> segments) {
    List<MergedSegment> result = new ArrayList<>();
    
    for (TextSegment segment : segments) {
        // ✅ 添加空文本检查
        String text = segment.getText();
        if (text == null || text.trim().isEmpty()) {
            log.debug("跳过空文本片段: order={}", segment.getOrder());
            continue;  // 跳过空文本
        }
        
        MergedSegment mergedSegment = new MergedSegment(segment.getSpeaker());
        mergedSegment.addText(text);
        mergedSegment.addOriginalSegment(segment);
        result.add(mergedSegment);
    }
    
    return result;
}
```

**关键改进：**
- ✅ 检查 `text` 是否为 `null` 或空字符串
- ✅ 使用 `trim()` 检查是否只包含空格
- ✅ 跳过无效片段，不添加到结果列表
- ✅ 添加调试日志，记录跳过的片段

---

#### 修复2：merge() 方法

**修复前：**
```java
public List<MergedSegment> merge(List<TextSegment> segments) {
    List<MergedSegment> merged = new ArrayList<>();
    
    // 直接使用原始片段（可能包含空文本）
    MergedSegment current = new MergedSegment(segments.get(0).getSpeaker());
    current.addText(segments.get(0).getText());  // ❌ 可能是空文本
    ...
}
```

**修复后：**
```java
public List<MergedSegment> merge(List<TextSegment> segments) {
    List<MergedSegment> merged = new ArrayList<>();
    
    // ✅ 先过滤掉空文本片段
    List<TextSegment> validSegments = new ArrayList<>();
    for (TextSegment segment : segments) {
        if (segment.getText() != null && !segment.getText().trim().isEmpty()) {
            validSegments.add(segment);
        } else {
            log.debug("过滤空文本片段: order={}", segment.getOrder());
        }
    }
    
    if (validSegments.isEmpty()) {
        log.warn("过滤后没有有效的文本片段");
        return merged;
    }
    
    // 使用过滤后的片段进行合并
    MergedSegment current = new MergedSegment(validSegments.get(0).getSpeaker());
    current.addText(validSegments.get(0).getText());
    ...
}
```

**关键改进：**
- ✅ 在合并前先过滤掉所有空文本片段
- ✅ 确保第一个片段是有效的（不是空文本）
- ✅ 如果过滤后没有有效片段，返回空列表
- ✅ 添加调试日志，记录过滤的片段数量

---

## ✅ 修复验证

### 验证1：编译检查

```bash
# 编译通过
✅ TextSegmentMerger.java - No diagnostics found
```

### 验证2：日志验证

**期望日志：**
```
使用独立模式，不合并相同音色的片段，原始片段数: 7
跳过空文本片段: order=1
跳过空文本片段: order=3
跳过空文本片段: order=5
独立片段: 音色=zh_male_chunhoushumoyinnan_moon_bigtts, 文本=你好
独立片段: 音色=zh_female_qingxin, 文本=我是云舟
独立片段: 音色=zh_male_chunhoushumoyinnan_moon_bigtts, 文本=很高兴认识你
独立片段: 音色=zh_male_chunhoushumoyinnan_moon_bigtts, 文本=我还喜欢游泳，滑冰
独立模式完成，输出片段数: 4（过滤后）
```

**关键验证点：**
- ✅ 跳过3个空文本片段（空行）
- ✅ 输出4个有效片段（对应4行文本）
- ✅ 每个片段都包含有效文本

### 验证3：TTS API调用

**期望行为：**
- ✅ 只调用4次TTS API（4个有效片段）
- ✅ 每次调用的 `text` 参数都不为空
- ✅ 不会出现 "No readable text!" 错误

---

## 🎯 修复效果

### 修复前（❌ 失败）

```
文档内容：["你好", "", "我是云舟", "", "很高兴认识你", "", "我还喜欢游泳，滑冰"]

独立模式处理：
  片段1："你好" ✅
  片段2："" ❌ 空文本
  片段3："我是云舟" ✅
  片段4："" ❌ 空文本
  片段5："很高兴认识你" ✅
  片段6："" ❌ 空文本
  片段7："我还喜欢游泳，滑冰" ✅

TTS调用：
  调用1："你好" → 成功 ✅
  调用2："" → 失败 ❌ "No readable text!"
  → 整个流程失败 ❌
```

### 修复后（✅ 成功）

```
文档内容：["你好", "", "我是云舟", "", "很高兴认识你", "", "我还喜欢游泳，滑冰"]

独立模式处理（添加空文本过滤）：
  片段1："你好" ✅
  片段2："" → 跳过 ✅
  片段3："我是云舟" ✅
  片段4："" → 跳过 ✅
  片段5："很高兴认识你" ✅
  片段6："" → 跳过 ✅
  片段7："我还喜欢游泳，滑冰" ✅

TTS调用：
  调用1："你好" → 成功 ✅
  调用2："我是云舟" → 成功 ✅
  调用3："很高兴认识你" → 成功 ✅
  调用4："我还喜欢游泳，滑冰" → 成功 ✅
  → 整个流程成功 ✅
```

---

## 📊 影响范围

### 影响的功能

1. **生成视频功能** - 修复后可以正常生成视频
2. **加载字幕功能** - 修复后可以正常加载字幕
3. **字幕编辑功能** - 不受影响（已生成的字幕）

### 影响的代码路径

1. **DocumentTTSServiceImpl.generateDocumentSpeechWithTiming()** 
   - 调用 `mergeNoMerge()` - ✅ 已修复
   
2. **DocumentTTSServiceImpl.getDialogSegments()** 
   - 调用 `mergeNoMerge()` - ✅ 已修复
   
3. **其他使用 `merge()` 的地方** 
   - ✅ 已修复（添加空文本过滤）

---

## 🛡️ 防御性编程改进

### 改进1：多层防护

```
第1层：Word解析器（WordDocumentParser）
  → 跳过空段落：if (paragraph.getText().trim().isEmpty())
  → 跳过空Run：if (text == null || text.trim().isEmpty())

第2层：文本合并器（TextSegmentMerger）
  → mergeNoMerge()：跳过空文本片段
  → merge()：过滤空文本片段

第3层：TTS服务（VolcengineTTSService）
  → 如果前两层都失败，TTS API会返回错误
  → 通过前两层防护，不会到达第3层
```

### 改进2：日志增强

**添加调试日志：**
```java
log.debug("跳过空文本片段: order={}", segment.getOrder());
log.debug("过滤空文本片段: order={}", segment.getOrder());
```

**作用：**
- ✅ 方便调试，知道哪些片段被跳过
- ✅ 方便排查文档解析问题
- ✅ 方便统计有效片段数量

---

## 🎓 经验总结

### 经验1：外部API调用需要输入验证

**教训：**
- 外部API（TTS）对输入有严格要求
- 不能假设输入总是有效的
- 需要在调用前进行验证

**解决：**
- 添加空文本检查
- 过滤无效输入
- 记录调试日志

### 经验2：Word文档解析的特殊性

**问题：**
- Word文档中的空行会被解析为空文本片段
- 用户为了视觉美观会添加空行
- 需要容错处理

**解决：**
- 在多个层次过滤空文本
- 不能只依赖Word解析器
- 需要在业务逻辑层再次过滤

### 经验3：防御性编程的重要性

**原则：**
- 不信任输入数据
- 在关键路径添加验证
- 添加详细的日志

**实践：**
- ✅ 空值检查：`if (text == null)`
- ✅ 空字符串检查：`if (text.trim().isEmpty())`
- ✅ 边界情况处理：`if (validSegments.isEmpty())`
- ✅ 调试日志：`log.debug("跳过...")`

---

## 🚀 测试步骤

### 步骤1：重启服务

```bash
# 停止当前服务
Ctrl + C

# 重新启动
cd d:\code\adminFlow\hm-service
mvn spring-boot:run
```

### 步骤2：准备测试文档

**创建 `测试-空行处理.docx`，输入：**
```
你好

我是云舟

很高兴认识你

我还喜欢游泳，滑冰
```

**注意：** 每行之间有空行（这是正常的文档格式）

### 步骤3：使用Postman测试

- URL: `POST http://localhost:8080/tts/video/generate`
- 上传测试文档

### 步骤4：验证结果

**期望结果：**
- ✅ HTTP 200 OK（不是500错误）
- ✅ 返回 `videoUrl`（视频生成成功）
- ✅ 返回4条 `dialogSegments`（对应4行有效文本）
- ✅ 日志显示"跳过空文本片段"（过滤了空行）

**验证点：**
```json
{
  "success": true,
  "videoUrl": "/tts/videos/xxx.mp4",
  "dialogSegments": [
    {"text": "你好", ...},
    {"text": "我是云舟", ...},
    {"text": "很高兴认识你", ...},
    {"text": "我还喜欢游泳，滑冰", ...}
  ]
}
```

---

## ✅ 修复清单

- [x] 修改 `TextSegmentMerger.mergeNoMerge()` - 添加空文本检查
- [x] 修改 `TextSegmentMerger.merge()` - 添加空文本过滤
- [x] 编译验证 - 无错误
- [x] 创建修复文档 - 本文件

**待测试：**
- [ ] 重启服务
- [ ] 上传包含空行的文档
- [ ] 验证不会出现 "No readable text!" 错误
- [ ] 验证生成的视频正常

---

**修复完成时间：** 2026-08-14  
**修复人员：** Kiro  
**状态：** ✅ 已修复，等待测试验证

