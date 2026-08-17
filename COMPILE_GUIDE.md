# 编译指南 - 方案H修复完成

> **状态：** ✅ 代码修复完成，等待编译验证  
> **要求：** Java 11 + Maven 3.6+

---

## 🎯 快速开始

### 方式1：自动检查并编译（推荐）

```bash
# 步骤1：检查Java环境
check-java-env.bat

# 步骤2：如果环境正确，编译项目
compile-fix.bat
```

---

### 方式2：手动编译

```bash
# 清理并编译
mvn clean compile -DskipTests
```

---

## ⚠️ 常见问题

### 问题1：Java未安装或版本错误

**错误信息：**
```
找不到符号: 程序包java.net.http不存在
或
无效的目标发行版: 11
```

**解决方案：**

1. **下载Java 11：**
   - Oracle JDK 11：https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html
   - 或开源版本（推荐）：https://adoptium.net/temurin/releases/?version=11

2. **安装到默认位置：**
   ```
   C:\Program Files\Java\jdk-11
   ```

3. **配置环境变量：**
   - 打开"系统属性" → "环境变量"
   - 新建系统变量：
     ```
     变量名：JAVA_HOME
     变量值：C:\Program Files\Java\jdk-11
     ```
   - 编辑系统变量 `Path`，添加：
     ```
     %JAVA_HOME%\bin
     ```

4. **验证安装：**
   ```bash
   # 重新打开命令行窗口
   java -version
   # 应该显示：java version "11.x.x"
   ```

---

### 问题2：Maven依赖下载失败

**错误信息：**
```
Failed to read artifact descriptor
或
Could not resolve dependencies
```

**解决方案：**

1. **配置Maven镜像（加速下载）：**
   
   编辑文件：`C:\Users\你的用户名\.m2\settings.xml`
   
   添加阿里云镜像：
   ```xml
   <mirrors>
     <mirror>
       <id>aliyun</id>
       <mirrorOf>central</mirrorOf>
       <name>Aliyun Maven Mirror</name>
       <url>https://maven.aliyun.com/repository/public</url>
     </mirror>
   </mirrors>
   ```

2. **重新编译：**
   ```bash
   mvn clean compile -DskipTests -U
   ```

---

### 问题3：编译报错 "找不到符号"

**错误示例：**
```
找不到符号: 类 VolcengineTTSService
找不到符号: 类 TTSRequest
```

**解决方案：**

这是导入语句或类路径问题，已在修复中解决。如果还有此错误：

1. **清理项目：**
   ```bash
   mvn clean
   ```

2. **删除本地仓库缓存：**
   ```bash
   rm -rf ~/.m2/repository/com/heima
   ```

3. **重新编译：**
   ```bash
   mvn clean install -DskipTests
   ```

---

## 📋 已修复的代码问题

### 修复1：TTS方法调用
**位置：** `SegmentEditorServiceImpl.java`

**原代码（错误）：**
```java
byte[] audioData = ttsService.synthesize(segment.getText(), segment.getVoiceId());
```

**新代码（正确）：**
```java
TTSRequest request = TTSRequest.builder()
        .text(segment.getText())
        .voiceId(segment.getVoiceId())
        .format(metadata.getVoiceConfig().getFormat())
        .sampleRate(metadata.getVoiceConfig().getSampleRate())
        .build();

byte[] audioData = ttsService.generateSpeechBytes(request);
```

---

### 修复2：Java版本统一
**位置：** `pom.xml`（3个文件）

**修改：**
- 父pom.xml：Java 11
- hm-common/pom.xml：Java 11
- hm-service/pom.xml：Java 11

**原因：** 项目使用了 `java.net.http` 包（Java 11特性），必须使用Java 11编译

---

## 🔍 编译验证

### 成功标志：

```
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for hmall 1.0.0:
[INFO] 
[INFO] hmall .............................................. SUCCESS [  0.180 s]
[INFO] hm-common .......................................... SUCCESS [  2.345 s]
[INFO] hm-service ......................................... SUCCESS [ 15.678 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 编译产物：

```
d:\code\adminFlow\
├── hm-common\target\classes\           # 公共类
└── hm-service\target\classes\          # 服务类
    └── com\hmall\tts\segment\
        └── service\impl\
            └── SegmentEditorServiceImpl.class  # ✅ 已编译
```

---

## 🚀 编译成功后的步骤

### 1. 启动服务

```bash
# 方式1：使用启动脚本
start-adminFlow.bat

# 方式2：手动启动
cd hm-service
mvn spring-boot:run
```

### 2. 测试局部编辑功能

```bash
# 生成初始视频
curl -X POST http://localhost:8080/api/video/generate \
  -F "file=@test.docx" \
  -F "boldVoice=zh_female_shuangkuaisisi_moon_bigtts" \
  -F "normalVoice=zh_male_wennuanahu_moon_bigtts"

# 响应：
{
  "success": true,
  "taskId": "abc123",
  "videoUrl": "/tts/videos/abc123.mp4"
}

# 编辑段落
curl -X PUT http://localhost:8080/api/tts/segment/edit \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "abc123",
    "segmentIndex": 0,
    "newText": "这是修改后的文本",
    "regenerateVideo": true
  }'

# 响应：
{
  "success": true,
  "jobId": "xyz789"
}

# 查询进度
curl http://localhost:8080/api/tts/segment/job-status/xyz789

# 响应（处理中）：
{
  "jobId": "xyz789",
  "status": "processing",
  "progress": 60,
  "currentStep": "对齐字幕..."
}

# 响应（完成）：
{
  "jobId": "xyz789",
  "status": "completed",
  "progress": 100,
  "videoUrl": "/tts/videos/abc123.mp4"
}
```

---

## 📝 相关文档

- **修复完成报告：** `PHASE_H_FIX_COMPLETE.md`
- **问题审查报告：** `PHASE_H_REVIEW.md`
- **实施总结：** `PHASE2_IMPLEMENTATION_SUMMARY.md`

---

## 🆘 获取帮助

如果遇到问题：

1. **运行诊断脚本：**
   ```bash
   check-java-env.bat
   ```

2. **查看详细错误：**
   ```bash
   mvn compile -DskipTests -X
   ```

3. **检查日志：**
   ```
   hm-service\target\maven-status\maven-compiler-plugin\compile\default-compile\
   ```

---

**最后更新：** 2026-08-17  
**修复状态：** ✅ 代码完成，等待编译验证
