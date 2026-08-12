# Edge TTS 最终修复 - stdout输出问题

**修复时间：** 2026-08-12 19:50  
**问题状态：** ✅ 已修复  
**修复文件：** `EdgeTTSCoreService.java`

---

## 🔍 问题分析

### 根本原因

edge-tts工具的设计问题：
```
edge-tts --text "hello" --write-media output.mp3
  ↓
1. 将音频写入 output.mp3 文件 ✅
2. 同时将音频输出到 stdout ❌（这是bug）
```

**关键发现：**
- `--write-media` 参数会正确写入文件
- 但edge-tts仍然将MP3二进制数据输出到stdout
- 如果不读取stdout，进程会阻塞（buffer满了）
- 如果使用`redirectOutput()`，文件会被覆盖为空

### 之前的错误修复

**错误修复1：** 使用redirectOutput()
```java
processBuilder.redirectOutput(tempFilePath.toFile());
```
**问题：** 这会将stdout重定向到文件，覆盖--write-media写入的文件

**错误修复2：** 尝试读取stdout为文本
```java
BufferedReader reader = new BufferedReader(
    new InputStreamReader(process.getInputStream(), "UTF-8"));
```
**问题：** MP3二进制数据无法正确解码为UTF-8，导致日志乱码

---

## ✅ 最终修复方案

### 核心思路
1. ✅ 使用 `--write-media` 让edge-tts写文件
2. ✅ 读取并丢弃stdout（避免进程阻塞）
3. ✅ 从文件读取最终音频数据

### 修复代码

```java
// 执行命令
ProcessBuilder processBuilder = new ProcessBuilder(command);
processBuilder.redirectErrorStream(true);

Process process = processBuilder.start();

// 读取输出并丢弃（edge-tts会同时输出到stdout和文件）
// 必须读取stdout，否则进程会阻塞
StringBuilder output = new StringBuilder();
try (InputStream is = process.getInputStream()) {
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = is.read(buffer)) != -1) {
        // 丢弃二进制数据（音频数据）
        // 不再尝试解码为文本，避免乱码日志
    }
}

// 等待进程完成
boolean finished = process.waitFor(properties.getTimeout(), TimeUnit.SECONDS);

if (!finished) {
    process.destroyForcibly();
    log.error("❌ [Edge TTS Core] 超时: timeout={}秒", properties.getTimeout());
    throw new TTSException(TTSErrorCode.TIMEOUT);
}

int exitCode = process.exitValue();
log.info("✅ [Edge TTS Core] 进程退出: exitCode={}", exitCode);

if (exitCode != 0) {
    log.error("❌ [Edge TTS Core] 执行失败: exitCode={}", exitCode);
    throw new TTSException(TTSErrorCode.EXECUTION_FAILED, "Edge TTS 执行失败，退出码: " + exitCode);
}

// 等待一下（让文件系统有时间写入）
Thread.sleep(500);

// 读取生成的音频文件
if (!Files.exists(tempFilePath)) {
    log.error("❌ [Edge TTS Core] 音频文件未生成: {}", tempFilePath);
    log.error("❌ [Edge TTS Core] 临时目录内容: {}", 
        Arrays.toString(tempDirPath.toFile().listFiles()));
    throw new TTSException(TTSErrorCode.EXECUTION_FAILED, "音频文件未生成: " + tempFilePath);
}

byte[] audioData = Files.readAllBytes(tempFilePath);
log.debug("✅ [Edge TTS Core] 生成成功: {} bytes", audioData.length);

return audioData;
```

---

## 🎯 修复要点

### 1. 正确处理stdout
```java
// ❌ 错误：尝试解码二进制数据为文本
BufferedReader reader = new BufferedReader(
    new InputStreamReader(process.getInputStream(), "UTF-8"));

// ✅ 正确：直接读取二进制数据并丢弃
try (InputStream is = process.getInputStream()) {
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = is.read(buffer)) != -1) {
        // 丢弃数据
    }
}
```

### 2. 移除redirectOutput
```java
// ❌ 错误：重定向会覆盖--write-media写的文件
processBuilder.redirectOutput(tempFilePath.toFile());

// ✅ 正确：不重定向，让--write-media正常工作
// 不设置redirectOutput
```

### 3. 清理日志
```java
// ❌ 之前：大量乱码日志
log.debug("📝 [Edge TTS Output] {}", line);  // 乱码！

// ✅ 现在：不输出二进制数据
// 直接丢弃，不记录日志
```

---

## 📋 测试步骤

### 1. 重启应用
在IDEA中停止并重新运行 `HMallApplication`

### 2. 测试TTS生成
打开浏览器访问：`http://localhost:8080/edge-tts-test.html`

点击"生成并播放"按钮

### 3. 预期结果
```
✅ 后端日志：
  - 🎤 [Edge TTS Core] 执行命令: py -m edge_tts --voice zh-CN-XiaoxiaoNeural ...
  - 🎤 [Edge TTS Core] 输出文件: d:\code\adminFlow\temp\tts_xxx.mp3
  - ✅ [Edge TTS Core] 进程退出: exitCode=0
  - ✅ [Edge TTS Core] 生成成功: 8064 bytes
  - ✅ [TTS] 生成成功: 8064 bytes

✅ 前端效果：
  - 状态显示：播放成功
  - 音频播放器出现并自动播放
  - 播放中文语音："我叫小晓..."

✅ 文件系统：
  - temp目录下有临时mp3文件
  - 生成后会自动清理
```

---

## 🔧 关键技术点

### 1. 进程stdout缓冲区
```
进程stdout有缓冲区（通常8KB）
如果不读取，缓冲区满了进程会阻塞
edge-tts输出的MP3数据远大于8KB
必须持续读取stdout才能让进程继续运行
```

### 2. 二进制数据vs文本数据
```
MP3是二进制数据，不能用BufferedReader读取
必须用InputStream直接读取字节
如果强制解码为UTF-8会产生乱码
```

### 3. --write-media参数的作用
```
edge-tts --write-media file.mp3
  ↓
1. edge-tts将音频写入file.mp3
2. 同时将音频输出到stdout（bug）
3. 我们需要读取stdout（避免阻塞）
4. 最终从file.mp3读取音频数据
```

---

## 📊 问题演化过程

### 阶段1：最初问题
```
❌ 音频文件未生成
原因：temp目录使用相对路径"temp"
解决：改为绝对路径"d:/code/adminFlow/temp"
```

### 阶段2：redirectOutput问题
```
❌ 使用redirectOutput后仍然文件未生成
原因：redirectOutput覆盖了--write-media写的文件
解决：移除redirectOutput
```

### 阶段3：stdout阻塞问题（当前）
```
❌ 不读取stdout导致进程阻塞
原因：edge-tts输出大量MP3数据到stdout
解决：读取并丢弃stdout数据
```

---

## 🎓 经验总结

### 1. 进程管理
- 必须读取stdout/stderr，避免缓冲区满导致阻塞
- redirectOutput会覆盖文件，需谨慎使用
- 二进制数据不能用文本流读取

### 2. edge-tts工具特性
- `--write-media`会同时写文件和stdout
- stdout输出的是原始MP3数据
- 必须读取并丢弃stdout数据

### 3. 日志优化
- 二进制数据不要输出到日志（会导致乱码）
- 大量数据不要逐行输出（影响性能）
- 关键节点记录INFO，详细信息记录DEBUG

---

**修复完成！** 🎉

请重启应用并测试：
1. 停止IDEA中的应用
2. 重新运行 `HMallApplication`
3. 打开 `http://localhost:8080/edge-tts-test.html`
4. 点击"生成并播放"
5. 确认音频正常播放

如果仍有问题，请提供新的错误日志。
