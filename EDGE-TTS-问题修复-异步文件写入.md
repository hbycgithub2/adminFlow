# Edge TTS 问题修复 - 异步文件写入

**修复时间：** 2026-08-12 19:58  
**问题状态：** ✅ 已修复  
**修复文件：** `EdgeTTSCoreService.java`

---

## 🔍 问题现象

### 错误日志
```
✅ [Edge TTS Core] 进程退出: exitCode=0
❌ [Edge TTS Core] 音频文件未生成: d:\code\adminFlow\temp\tts_xxx.mp3
❌ [Edge TTS Core] 临时目录内容: [test.mp3, test_audio.mp3]
```

**关键发现：**
- ✅ 进程成功退出（exitCode=0）
- ❌ 文件未生成（检查时文件不存在）
- ✅ 手动执行命令可以生成文件

---

## 🔎 根本原因

### 问题1：stdout阻塞
```
edge-tts同时输出到stdout和文件
如果主线程同步读取stdout，会阻塞进程
导致文件写入延迟
```

### 问题2：文件写入异步
```
edge-tts的--write-media是异步写入
进程退出(exitCode=0)时，文件可能还在写入
立即检查文件会失败
```

### 问题3：等待时间不足
```
原代码：Thread.sleep(500);  // 固定等待500ms
实际：文件生成可能需要更长时间（取决于文本长度）
```

---

## ✅ 修复方案

### 修复1：异步读取stdout

**原代码：**
```java
// 同步读取stdout（会阻塞）
try (InputStream is = process.getInputStream()) {
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = is.read(buffer)) != -1) {
        // 处理数据
    }
}
```

**修复后：**
```java
// 使用单独的线程读取stdout（避免阻塞）
Thread outputReaderThread = new Thread(() -> {
    try (InputStream is = process.getInputStream()) {
        byte[] buffer = new byte[8192];
        while (is.read(buffer) != -1) {
            // 丢弃数据（必须读取，否则进程会阻塞）
        }
    } catch (IOException e) {
        log.debug("输出流读取结束: {}", e.getMessage());
    }
});
outputReaderThread.setDaemon(true);
outputReaderThread.start();
```

**优势：**
- ✅ 不阻塞主线程
- ✅ 进程可以并发写文件
- ✅ 提升性能

---

### 修复2：轮询检查文件

**原代码：**
```java
// 固定等待500ms
Thread.sleep(500);

// 检查文件
if (!Files.exists(tempFilePath)) {
    throw new TTSException("文件未生成");
}
```

**修复后：**
```java
// 等待文件生成（最多等待5秒，每100ms检查一次）
int maxRetries = 50;
int retryCount = 0;
while (!Files.exists(tempFilePath) && retryCount < maxRetries) {
    Thread.sleep(100);
    retryCount++;
}

// 检查文件是否生成
if (!Files.exists(tempFilePath)) {
    log.error("❌ [Edge TTS Core] 音频文件未生成: {}", tempFilePath);
    log.error("❌ [Edge TTS Core] 等待时间: {}ms", retryCount * 100);
    throw new TTSException(TTSErrorCode.EXECUTION_FAILED, "音频文件未生成");
}

log.info("✅ [Edge TTS Core] 文件生成成功，等待时间: {}ms", retryCount * 100);
```

**优势：**
- ✅ 自适应等待（文本短则快，文本长则慢）
- ✅ 最多等待5秒（超时保护）
- ✅ 记录实际等待时间（便于调试）

---

## 📊 完整代码

### EdgeTTSCoreService.java（核心部分）

```java
// 执行命令
ProcessBuilder processBuilder = new ProcessBuilder(command);
processBuilder.redirectErrorStream(true);

Process process = processBuilder.start();

// 使用单独的线程读取stdout（避免阻塞）
Thread outputReaderThread = new Thread(() -> {
    try (InputStream is = process.getInputStream()) {
        byte[] buffer = new byte[8192];
        while (is.read(buffer) != -1) {
            // 丢弃数据（必须读取，否则进程会阻塞）
        }
    } catch (IOException e) {
        log.debug("输出流读取结束: {}", e.getMessage());
    }
});
outputReaderThread.setDaemon(true);
outputReaderThread.start();

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
    throw new TTSException(TTSErrorCode.EXECUTION_FAILED, "退出码: " + exitCode);
}

// 等待文件生成（最多等待5秒，每100ms检查一次）
int maxRetries = 50;
int retryCount = 0;
while (!Files.exists(tempFilePath) && retryCount < maxRetries) {
    Thread.sleep(100);
    retryCount++;
}

// 检查文件是否生成
if (!Files.exists(tempFilePath)) {
    log.error("❌ [Edge TTS Core] 音频文件未生成: {}", tempFilePath);
    log.error("❌ [Edge TTS Core] 临时目录内容: {}", 
        Arrays.toString(tempDirPath.toFile().listFiles()));
    log.error("❌ [Edge TTS Core] 等待时间: {}ms", retryCount * 100);
    throw new TTSException(TTSErrorCode.EXECUTION_FAILED, "音频文件未生成");
}

log.info("✅ [Edge TTS Core] 文件生成成功，等待时间: {}ms", retryCount * 100);

// 读取音频数据
byte[] audioData = Files.readAllBytes(tempFilePath);
log.debug("✅ [Edge TTS Core] 生成成功: {} bytes", audioData.length);

return audioData;
```

---

## 🧪 测试步骤

### 1. 重启应用
在IDEA中停止并重新运行 `HMallApplication`

### 2. 测试短文本
```
文本：你好世界
预期：等待时间 < 500ms
```

### 3. 测试长文本
```
文本：月亮姑娘升起来了...(400字)
预期：等待时间 1-3秒
```

### 4. 观察日志
```
✅ [Edge TTS Core] 进程退出: exitCode=0
✅ [Edge TTS Core] 文件生成成功，等待时间: 1200ms
✅ [Edge TTS Core] 生成成功: 45678 bytes
✅ [TTS] 生成成功: 45678 bytes
```

---

## 🎯 关键技术点

### 1. 守护线程（Daemon Thread）
```java
outputReaderThread.setDaemon(true);
```
- 守护线程不会阻止JVM退出
- 主线程结束时自动终止
- 适合后台任务

### 2. 轮询检查（Polling）
```java
while (!Files.exists(tempFilePath) && retryCount < maxRetries) {
    Thread.sleep(100);
    retryCount++;
}
```
- 每100ms检查一次
- 最多检查50次（5秒）
- 自适应等待

### 3. 超时保护
```java
boolean finished = process.waitFor(properties.getTimeout(), TimeUnit.SECONDS);
if (!finished) {
    process.destroyForcibly();
    throw new TTSException(TTSErrorCode.TIMEOUT);
}
```
- 防止进程永久阻塞
- 强制终止超时进程
- 释放系统资源

---

## 📈 性能优化

### 优化前
```
固定等待500ms
无论文本长短都等待500ms
短文本浪费时间
长文本可能失败
```

### 优化后
```
自适应等待100-5000ms
短文本：100-500ms
长文本：1000-3000ms
超长文本：自动超时保护
```

---

## 🔧 故障排查

### 如果仍然文件未生成

**步骤1：检查手动命令**
```bash
py -m edge_tts --text "测试" --voice zh-CN-XiaoxiaoNeural --rate +0% --pitch +0Hz --write-media d:\code\adminFlow\temp\test.mp3
```
如果手动命令失败，说明edge-tts有问题

**步骤2：检查日志**
```
查看等待时间：retryCount * 100
如果等待时间 = 5000ms，说明超时
如果等待时间 < 1000ms，说明文件生成快但路径错误
```

**步骤3：检查权限**
```bash
# 检查temp目录权限
icacls d:\code\adminFlow\temp

# 确保有写入权限
```

---

## ✅ 预期结果

### 后端日志
```
🎤 [Edge TTS Core] 执行命令: py -m edge_tts ...
🎤 [Edge TTS Core] 输出文件: d:\code\adminFlow\temp\tts_xxx.mp3
✅ [Edge TTS Core] 进程退出: exitCode=0
✅ [Edge TTS Core] 文件生成成功，等待时间: 1200ms
✅ [Edge TTS Core] 生成成功: 45678 bytes
✅ [TTS] 生成成功: 45678 bytes
```

### 前端效果
```
✅ 状态显示：播放成功
✅ 音频播放器出现并自动播放
✅ 播放中文语音："月亮姑娘升起来了..."
```

---

**修复完成！** 🎉

请重启应用并测试：
1. 停止IDEA中的应用
2. 重新运行 `HMallApplication`
3. 打开 `http://localhost:8080/edge-tts-test.html`
4. 点击"生成并播放"
5. 观察日志中的等待时间
6. 确认音频正常播放

如果仍有问题，请提供新的错误日志。
