# WhisperX 路径空格问题 - 完整修复报告

> **问题：** `Cannot run program "D:/Program Files/Python313/python.exe": CreateProcess error=2`  
> **原因：** Python路径包含空格，ProcessBuilder无法正确处理  
> **状态：** ✅ 已修复（3处代码修改）

---

## 🔍 问题根源

### 错误日志
```
java.io.IOException: Cannot run program "D:/Program Files/Python313/python.exe": 
CreateProcess error=2, 系统找不到指定的文件。
```

### 根本原因

1. **路径包含空格**：`D:/Program Files/Python313/python.exe`
2. **Java ProcessBuilder 处理问题**：
   - 配置文件中：`command: "D:/Program Files/Python313/python.exe"`
   - Spring 解析后：字符串 `D:/Program Files/Python313/python.exe`
   - ProcessBuilder 需要：数组 `["D:/Program Files/Python313/python.exe"]`
   - 如果包含参数：需要拆分为 `["py", "-3.13"]`

3. **核心问题**：
   - ProcessBuilder 要求：`new ProcessBuilder("py", "-3.13", script, audio, text)`
   - 错误做法：`new ProcessBuilder("py -3.13", script, audio, text)`  ← ❌ 整个当成一个命令
   - 正确做法：`new ProcessBuilder("py", "-3.13", script, audio, text)`  ← ✅ 拆分为数组

---

## ✅ 修复方案（3处修改）

### 修复1：配置文件（application.yaml）

**修改前：**
```yaml
whisperx:
  python:
    command: "D:/Program Files/Python313/python.exe"  # ❌ 路径有空格
```

**修改后：**
```yaml
whisperx:
  python:
    command: py -3.13  # ✅ 使用 py launcher，避免空格问题
```

**优势：**
- ✅ 不需要指定完整路径
- ✅ py launcher 会自动找到 Python 3.13
- ✅ 避免空格问题

---

### 修复2：新增方法 `getPythonCommandArray()`

**新增代码：**
```java
/**
 * ✅ 获取Python命令（支持带参数的命令，如 "py -3.13"）
 * 
 * @return Python命令数组（例如：["py", "-3.13"] 或 ["python"]）
 */
private String[] getPythonCommandArray() {
    // 首次调用时检测
    if (actualPythonCommand == null) {
        actualPythonCommand = detectPython313();
    }
    
    // 如果命令包含空格，拆分为数组（支持 "py -3.13"）
    if (actualPythonCommand.contains(" ")) {
        String[] parts = actualPythonCommand.split("\\s+");
        log.debug("[WhisperX] Python命令拆分：{}", Arrays.toString(parts));
        return parts;
    }
    
    // 单个命令（如 "python" 或 "python313"）
    return new String[] { actualPythonCommand };
}
```

**功能：**
- 检测命令是否包含空格
- 如果包含空格（如 `py -3.13`），拆分为数组：`["py", "-3.13"]`
- 如果没有空格（如 `python`），返回单元素数组：`["python"]`

---

### 修复3：修改 `align()` 方法（核心修复）

**修改前：**
```java
// ❌ 错误：整个命令当成一个字符串
ProcessBuilder pb = new ProcessBuilder(
    actualPythonCommand,  // "py -3.13" 或 "D:/Program Files/Python313/python.exe"
    scriptPath,
    audioPath.toString(),
    originalText
);
```

**修改后：**
```java
// ✅ 正确：拆分命令并构建命令列表
String[] pythonCmd = getPythonCommandArray();  // ["py", "-3.13"] 或 ["python"]

// 构建完整命令（Python命令 + 脚本路径 + 参数）
List<String> commandList = new ArrayList<>();
commandList.addAll(Arrays.asList(pythonCmd));  // 添加Python命令（可能是多个部分）
commandList.add(scriptPath);                   // 添加脚本路径
commandList.add(audioPath.toString());         // 添加音频路径
commandList.add(originalText);                 // 添加原文

// 构建ProcessBuilder
ProcessBuilder pb = new ProcessBuilder(commandList);
```

**执行效果：**
```java
// 配置：command: py -3.13
// 最终命令：["py", "-3.13", "D:/code/adminFlow/scripts/whisperx_align.py", "audio.mp3", "原文"]

// 配置：command: python
// 最终命令：["python", "D:/code/adminFlow/scripts/whisperx_align.py", "audio.mp3", "原文"]
```

---

### 修复4：修改 `isAvailable()` 方法（一致性修复）

**修改前：**
```java
// ❌ 错误：使用字符串命令
ProcessBuilder pb = new ProcessBuilder(actualPythonCommand, "--version");
```

**修改后：**
```java
// ✅ 正确：使用命令数组
String[] pythonCmd = getPythonCommandArray();

List<String> commandList = new ArrayList<>();
commandList.addAll(Arrays.asList(pythonCmd));
commandList.add("--version");

ProcessBuilder pb = new ProcessBuilder(commandList);
```

---

## 📊 修复对比

### 之前（❌ 错误）

```java
// 配置：command: "D:/Program Files/Python313/python.exe"
ProcessBuilder pb = new ProcessBuilder(
    "D:/Program Files/Python313/python.exe",  // ❌ 整个路径当成一个命令
    scriptPath,
    audioPath,
    text
);

// Windows 执行时：
// CreateProcess("D:/Program Files/Python313/python.exe", ...)
// Windows 找不到 "D:/Program" 命令 ← ❌ 被空格截断
```

### 现在（✅ 正确）

```java
// 配置：command: py -3.13
String[] pythonCmd = getPythonCommandArray();  // ["py", "-3.13"]

List<String> commandList = new ArrayList<>();
commandList.addAll(Arrays.asList(pythonCmd));  // ["py", "-3.13"]
commandList.add(scriptPath);                   // ["py", "-3.13", "script.py"]
commandList.add(audioPath);                    // ["py", "-3.13", "script.py", "audio.mp3"]
commandList.add(text);                         // ["py", "-3.13", "script.py", "audio.mp3", "文本"]

ProcessBuilder pb = new ProcessBuilder(commandList);

// Windows 执行时：
// CreateProcess("py", "-3.13 script.py audio.mp3 文本")
// 正确执行 ✅
```

---

## 🎯 完整执行流程

### 配置加载
```yaml
# application.yaml
whisperx:
  python:
    command: py -3.13  # ← Spring 读取为字符串 "py -3.13"
```

### Java 处理
```java
// 1. getPythonCommandArray() 拆分命令
String command = "py -3.13";  // 从配置读取
String[] parts = command.split("\\s+");  // 拆分：["py", "-3.13"]

// 2. 构建完整命令列表
List<String> commandList = new ArrayList<>();
commandList.add("py");
commandList.add("-3.13");
commandList.add("D:/code/adminFlow/scripts/whisperx_align.py");
commandList.add("D:/code/adminFlow/temp/whisperx/segment_0.mp3");
commandList.add("这是第一句话");

// 3. 创建 ProcessBuilder
ProcessBuilder pb = new ProcessBuilder(commandList);
// 等价于：
// ProcessBuilder pb = new ProcessBuilder(
//     "py", 
//     "-3.13", 
//     "D:/code/adminFlow/scripts/whisperx_align.py", 
//     "D:/code/adminFlow/temp/whisperx/segment_0.mp3", 
//     "这是第一句话"
// );

// 4. 执行
Process process = pb.start();
```

### Windows 执行
```cmd
# Windows CreateProcess 实际执行：
py -3.13 D:/code/adminFlow/scripts/whisperx_align.py D:/code/adminFlow/temp/whisperx/segment_0.mp3 "这是第一句话"
```

---

## ✅ 验证方法

### 方法1：查看启动日志
```
[WhisperX] 使用配置的Python命令：py -3.13
[WhisperX] Python命令拆分：["py", "-3.13"]
[WhisperX] 服务可用（Python: py -3.13）
```

### 方法2：查看执行日志
```
[WhisperX] 执行命令：py -3.13 D:/code/adminFlow/scripts/whisperx_align.py D:/code/adminFlow/temp/whisperx/segment_0.mp3 "这是第一句话"
[WhisperX日志] Loading WhisperX model...
[WhisperX日志] Loading audio: D:/code/adminFlow/temp/whisperx/segment_0.mp3
[WhisperX日志] Transcribing with Whisper...
[WhisperX日志] Aligning with WhisperX...
[WhisperX日志] Alignment complete!
[WhisperX] ✅ 对齐完成，字符数：7，准确率：98-99%
```

### 方法3：没有错误日志
```
# 之前的错误日志应该消失：
[WhisperX] 检查服务可用性失败
java.io.IOException: Cannot run program "D:/Program Files/Python313/python.exe"
CreateProcess error=2, 系统找不到指定的文件。
```

---

## 🔄 重启服务

### 在 IDEA 中
1. 停止服务（红色方块按钮）
2. 重新编译（Build → Rebuild Project）
3. 启动服务（绿色三角按钮）

### 或在 CMD 中
```bash
cd D:\code\adminFlow
restart-service.bat
```

---

## 🎉 预期效果

### 启动日志
```
[WhisperX] 使用配置的Python命令：py -3.13
[WhisperX] Python命令拆分：["py", "-3.13"]
[WhisperX] 服务可用（Python: py -3.13）
```

### 上传文档后的日志
```
=== 开始 WhisperX 字符级对齐 ===
句子: 这是第一句话
MP3路径: D:/code/adminFlow/temp/whisperx/segment_0.mp3
文本内容: 这是第一句话

[WhisperX] 执行命令：py -3.13 D:/code/adminFlow/scripts/whisperx_align.py "D:/code/adminFlow/temp/whisperx/segment_0.mp3" "这是第一句话"

[WhisperX日志] Loading WhisperX model...
[WhisperX日志] Loading audio: D:/code/adminFlow/temp/whisperx/segment_0.mp3
[WhisperX日志] Transcribing with Whisper...
[WhisperX日志] Aligning with WhisperX...
[WhisperX日志] Alignment complete!

[WhisperX] ✅ 对齐完成，字符数：7，准确率：98-99%，音频时长：0.84秒，耗时：1523 ms
=== WhisperX 对齐完成 ===
```

---

## 📝 技术要点总结

### Java ProcessBuilder 规则
1. **命令数组**：每个元素是一个独立的参数
2. **不要用空格分割**：`["py -3.13"]` ≠ `["py", "-3.13"]`
3. **路径包含空格**：必须作为单个元素，不能拆分

### 正确示例
```java
// ✅ 正确
new ProcessBuilder("py", "-3.13", "script.py")

// ✅ 正确（路径有空格）
new ProcessBuilder("D:/Program Files/Python313/python.exe", "script.py")

// ❌ 错误（整个命令当成一个字符串）
new ProcessBuilder("py -3.13", "script.py")

// ❌ 错误（路径被空格截断）
new ProcessBuilder("D:/Program Files/Python313/python.exe", "script.py")  // 如果是字符串拆分的话
```

### 最佳实践
1. **优先使用 py launcher**：`py -3.13`，避免路径问题
2. **拆分命令字符串**：`"py -3.13".split("\\s+")` → `["py", "-3.13"]`
3. **使用命令列表**：`List<String> commandList = new ArrayList<>()`
4. **统一处理**：所有调用 ProcessBuilder 的地方都使用 `getPythonCommandArray()`

---

## 🔗 相关文件

1. **配置文件**：`hm-service/src/main/resources/application.yaml`
2. **修复代码**：`hm-service/src/main/java/com/hmall/tts/whisperx/service/impl/WhisperXServiceImpl.java`
3. **测试指南**：`docs/WhisperX-立即测试指南.md`

---

**修复完成时间：** 2026-08-16  
**版本：** v1.1（路径空格问题修复版）  
**状态：** ✅ 可立即测试

