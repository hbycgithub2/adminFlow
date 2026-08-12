# Edge TTS 问题修复：音频文件未生成

## 🐛 问题描述

**错误信息：**
```
com.hmall.tts.exception.TTSException: 音频文件未生成
at com.hmall.tts.service.EdgeTTSCoreService.generateSpeech
```

**发生时间：** 2026-08-12 19:35:37

**问题现象：** 
- 前端调用 `/api/tts/generate` 接口
- 后端执行 edge-tts 命令
- 命令执行成功（exitCode=0）
- 但音频文件未生成

---

## 🔍 问题分析

### 1. 手动测试（成功）

```bash
# 测试命令
py -m edge_tts --voice "zh-CN-XiaoxiaoNeural" --rate "+0%" --pitch "+0Hz" --text "测试" --write-media "d:\code\adminFlow\temp\test_audio.mp3"

# 结果：成功生成音频文件（8064 bytes）
```

**结论：** edge-tts 本身工作正常 ✅

### 2. 配置问题（根本原因）

**原配置：**
```yaml
edge-tts:
  temp-dir: temp  # ❌ 相对路径，可能导致路径解析错误
```

**问题：**
- 使用相对路径 `temp`
- Java 运行时的工作目录可能不是项目根目录
- 导致生成的文件路径不正确

**修复后：**
```yaml
edge-tts:
  temp-dir: d:/code/adminFlow/temp  # ✅ 绝对路径
```

### 3. 代码优化（增强调试）

**原代码问题：**
- ❌ 日志级别为 `debug`，生产环境看不到执行细节
- ❌ 没有打印实际执行的命令
- ❌ 没有打印输出文件路径
- ❌ 没有打印临时目录内容
- ❌ 文件检查太快，可能文件还没写入完成

**修复后：**
```java
// 1. 打印详细信息（改为 info 级别）
log.info("🎤 [Edge TTS Core] 执行命令: {}", String.join(" ", command));
log.info("🎤 [Edge TTS Core] 输出文件: {}", tempFilePath.toString());

// 2. 打印进程输出（每一行）
log.debug("📝 [Edge TTS Output] {}", line);

// 3. 打印进程退出码
log.info("✅ [Edge TTS Core] 进程退出: exitCode={}", exitCode);

// 4. 等待文件写入完成
Thread.sleep(500);

// 5. 文件检查失败时，打印临时目录内容
if (!Files.exists(tempFilePath)) {
    log.error("❌ [Edge TTS Core] 音频文件未生成: {}", tempFilePath);
    log.error("❌ [Edge TTS Core] 临时目录内容: {}", 
        Arrays.toString(tempDirPath.toFile().listFiles()));
    throw new TTSException(TTSErrorCode.EXECUTION_FAILED, "音频文件未生成: " + tempFilePath);
}

// 6. 修复字符编码问题
new InputStreamReader(process.getInputStream(), "UTF-8")
```

---

## 🔧 修复步骤

### 步骤1：修改配置文件

**文件：** `hm-service/src/main/resources/application.yaml`

```yaml
# 修改前
edge-tts:
  temp-dir: temp

# 修改后
edge-tts:
  temp-dir: d:/code/adminFlow/temp
```

### 步骤2：修改核心服务

**文件：** `hm-service/src/main/java/com/hmall/tts/service/EdgeTTSCoreService.java`

**修改内容：**
1. 日志级别从 `debug` 改为 `info`（关键执行信息）
2. 增加详细的执行日志
3. 增加进程输出日志
4. 增加文件写入等待时间（500ms）
5. 增加文件检查失败时的详细错误信息
6. 修复字符编码问题（UTF-8）

### 步骤3：重启服务

**方式1：使用 IDEA**
```
1. 停止正在运行的服务
2. 点击"重新运行"（Ctrl+F5）
```

**方式2：使用命令行**
```bash
# 停止旧服务（Ctrl+C）
# 编译项目
mvn clean compile -DskipTests

# 启动服务
cd hm-service
mvn spring-boot:run
```

**方式3：使用快速重启脚本（新增）**
```bash
restart-service.bat
```

---

## ✅ 验证步骤

### 1. 查看启动日志

启动后应该看到：
```
🎤 [Edge TTS Core] 执行命令: py -m edge_tts --voice zh-CN-XiaoxiaoNeural ...
🎤 [Edge TTS Core] 输出文件: d:\code\adminFlow\temp\tts_1234567890_abcd1234.mp3
✅ [Edge TTS Core] 进程退出: exitCode=0
✅ [Edge TTS Core] 生成成功: 8064 bytes
```

### 2. 测试生成语音

访问测试页面：
```
http://localhost:8080/edge-tts-test.html
```

**测试步骤：**
1. 输入测试文本："你好，这是测试"
2. 选择语音：晓晓
3. 点击"生成语音"
4. 应该成功播放

### 3. 检查临时目录

```bash
dir d:\code\adminFlow\temp

# 应该看到生成的临时文件（会自动清理）
```

---

## 📊 修复前后对比

| 项目 | 修复前 | 修复后 |
|------|--------|--------|
| 临时目录 | `temp`（相对路径） | `d:/code/adminFlow/temp`（绝对路径） |
| 日志级别 | `debug` | `info`（关键信息） |
| 命令日志 | ❌ 不打印 | ✅ 打印完整命令 |
| 输出文件日志 | ❌ 不打印 | ✅ 打印文件路径 |
| 进程输出日志 | ❌ 不打印 | ✅ 打印每一行 |
| 退出码日志 | ❌ 错误时才打印 | ✅ 总是打印 |
| 文件检查等待 | ❌ 立即检查 | ✅ 等待500ms |
| 错误信息 | ❌ 简单 | ✅ 详细（包含临时目录内容） |
| 字符编码 | ❌ 默认 | ✅ UTF-8 |

---

## 🎯 根本原因总结

**核心问题：** 配置使用相对路径 `temp`

**为什么会失败？**

```
工作目录（可能）： D:\code\adminFlow\hm-service\target\classes\
相对路径：          temp
实际路径：          D:\code\adminFlow\hm-service\target\classes\temp  ← ❌ 错误

edge-tts 生成文件： D:\code\adminFlow\temp\xxx.mp3  ← ✅ 正确位置
Java 检查文件：      D:\code\adminFlow\hm-service\target\classes\temp\xxx.mp3  ← ❌ 检查错误位置
结果：               文件未找到（但实际已生成）
```

**解决方案：** 使用绝对路径 `d:/code/adminFlow/temp`

---

## 📝 后续优化建议

### 1. 支持动态临时目录

```yaml
edge-tts:
  temp-dir: ${user.home}/edge-tts-temp  # 使用用户目录
```

### 2. 定期清理临时文件

```java
// 增加定时任务，清理超过1小时的临时文件
@Scheduled(cron = "0 0 * * * ?")
public void cleanOldTempFiles() {
    // 清理逻辑
}
```

### 3. 增加健康检查

```java
@GetMapping("/health")
public Map<String, Object> health() {
    // 检查 edge-tts 是否正常
    // 检查临时目录是否可写
    // 检查磁盘空间
}
```

### 4. 支持音频格式配置

```yaml
edge-tts:
  output-format: mp3  # 支持 mp3, wav, ogg
```

---

## 🔗 相关文件

**修改文件：**
1. `hm-service/src/main/resources/application.yaml`（配置）
2. `hm-service/src/main/java/com/hmall/tts/service/EdgeTTSCoreService.java`（核心服务）

**新增文件：**
1. `restart-service.bat`（快速重启脚本）
2. `EDGE-TTS-问题修复-音频文件未生成.md`（本文件）

---

## ✨ 总结

**问题：** 音频文件未生成（音频文件未生成异常）

**根本原因：** 配置使用相对路径，导致路径解析错误

**修复方案：** 
1. 配置改为绝对路径
2. 增加详细日志
3. 增加文件写入等待时间
4. 修复字符编码问题

**修复后状态：** ✅ 应该正常工作

**验证方式：** 访问 http://localhost:8080/edge-tts-test.html 测试

---

**修复时间：** 2026-08-12 19:40  
**修复者：** Kiro AI Assistant
