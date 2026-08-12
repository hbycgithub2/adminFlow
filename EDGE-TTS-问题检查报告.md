# Edge TTS 模块化重构 - 问题检查报告

> **检查时间：** 2026-08-12  
> **检查结果：** ⚠️ 发现 3 个问题  
> **影响程度：** 中等（可编译，但运行时可能出错）

---

## 🔍 检查结果汇总

| 序号 | 问题类型 | 严重程度 | 影响范围 | 状态 |
|------|---------|---------|---------|------|
| 1 | 缺少 validation 依赖 | ⚠️ 中等 | DTO 参数验证失效 | 待修复 |
| 2 | LongTextRequest 参数可能为 null | ⚠️ 低 | 长文本接口可能空指针 | 待修复 |
| 3 | 旧控制器未删除导致路径冲突 | ℹ️ 提示 | 可能导致接口冲突 | 需确认 |

---

## 📋 问题详情

### ❌ 问题1：缺少 validation 依赖

**问题描述：**
- 代码中使用了 `@Validated`、`@NotBlank`、`@Size`、`@Min`、`@Max` 等注解
- 但 `pom.xml` 中缺少 `spring-boot-starter-validation` 依赖
- **影响：** 参数验证不会生效，可能导致非法参数传入

**受影响的文件：**
- `TTSRequest.java` - 使用了 `@NotBlank`、`@Size`
- `LongTextRequest.java` - 使用了 `@NotBlank`、`@Min`、`@Max`
- `TTSController.java` - 使用了 `@Validated`

**错误示例：**
```java
// TTSRequest.java
@NotBlank(message = "文本内容不能为空")  // ← 不会生效
@Size(max = 5000, message = "文本内容不能超过5000字符")  // ← 不会生效
private String text;

// TTSController.java
public ResponseEntity<byte[]> generateSpeech(@Validated @RequestBody TTSRequest request) {
    // @Validated 不会生效，空文本也能传入
}
```

**后果：**
```
POST /api/tts/generate
{
  "text": ""  // ← 空文本，应该被拦截，但没有拦截
}

结果：传入 EdgeTTSCoreService，抛出 TTSException(EMPTY_TEXT)
```

**修复方案：**
在 `hm-service/pom.xml` 中添加依赖：
```xml
<!--validation-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

### ⚠️ 问题2：LongTextRequest 参数可能为 null

**问题描述：**
- `LongTextRequest.maxSegmentLength` 默认值是 `500`
- 但在 `TTSController.generateLongTextSpeech()` 中直接使用 `request.getMaxSegmentLength()`
- 如果用户传入 `null`，会导致 `NullPointerException`

**受影响的代码：**
```java
// TTSController.java
byte[] audioData = longTextService.generateLongTextSpeech(
    request.getText(),
    request.getVoice(),
    request.getRate(),
    request.getPitch(),
    request.getMaxSegmentLength()  // ← 可能为 null
);

// LongTextTTSService.java
public byte[] generateLongTextSpeech(..., int maxSegmentLength) {
    // 如果 maxSegmentLength 是 null，这里会自动拆箱失败
    List<String> segments = textSplitService.smartSplit(text, maxSegmentLength);
}
```

**错误场景：**
```json
POST /api/tts/long-text
{
  "text": "测试文本",
  "maxSegmentLength": null  // ← 用户传入 null
}

结果：NullPointerException（int 不能接收 null）
```

**修复方案：**
在 `LongTextRequest.java` 中确保默认值：
```java
// 修改前
private Integer maxSegmentLength = 500;

// 修改后（更安全）
private Integer maxSegmentLength = 500;

// Controller 中添加检查
if (request.getMaxSegmentLength() == null) {
    request.setMaxSegmentLength(500);
}
```

---

### ℹ️ 问题3：旧控制器未删除可能导致路径冲突

**问题描述：**
- 新的 `TTSController` 使用路径 `/api/tts/**`
- 旧的 `EdgeTTSController` 使用路径 `/api/edge-tts/**`
- 两者不冲突，但可能造成混淆

**当前状态：**
```
com/hmall/controller/
  └── EdgeTTSController.java   ← 旧控制器（保留）
      - POST /api/edge-tts/generate
      - GET /api/edge-tts/health
      - GET /api/edge-tts/voices

com/hmall/tts/controller/
  └── TTSController.java        ← 新控制器
      - POST /api/tts/generate
      - POST /api/tts/long-text  ← 新增
      - GET /api/tts/health
      - GET /api/tts/voices
```

**潜在问题：**
1. 两个控制器同时注入相同的 Service（可能导致循环依赖）
2. 配置文件中两个路径都放行了，可能造成安全隐患
3. 用户不清楚应该使用哪个接口

**建议方案：**

**方案A：保留旧控制器（兼容性优先）**
- ✅ 保留 `EdgeTTSController`（兼容旧代码）
- ✅ 新功能使用 `TTSController`
- ⚠️ 需要清晰的文档说明

**方案B：删除旧控制器（简洁性优先）**
- ❌ 删除 `EdgeTTSController`
- ✅ 只保留 `TTSController`
- ⚠️ 旧代码需要修改路径

**推荐：** 方案A（保留旧控制器，向后兼容）

---

## 🔧 修复方案

### 修复1：添加 validation 依赖

**文件：** `hm-service/pom.xml`

**添加依赖：**
```xml
<!--validation-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**位置：** 在 `</dependencies>` 之前添加

---

### 修复2：防止 maxSegmentLength 为 null

**方案A：在 Controller 中添加默认值检查**

**文件：** `TTSController.java`

**修改方法：** `generateLongTextSpeech()`

```java
@PostMapping("/long-text")
public ResponseEntity<byte[]> generateLongTextSpeech(@Validated @RequestBody LongTextRequest request) {
    long startTime = System.currentTimeMillis();
    
    // 添加默认值检查
    Integer maxSegmentLength = request.getMaxSegmentLength();
    if (maxSegmentLength == null) {
        maxSegmentLength = 500;  // 默认值
        log.warn("⚠️ [长文本 TTS] maxSegmentLength 为 null，使用默认值 500");
    }
    
    log.info("🎤 [长文本 TTS] 收到请求: 文本长度={} 字符, voice={}, rate={}, pitch={}, maxSegmentLength={}", 
            request.getText().length(), request.getVoice(), request.getRate(), 
            request.getPitch(), maxSegmentLength);

    try {
        // 使用局部变量，而不是直接使用 request.getMaxSegmentLength()
        byte[] audioData = longTextService.generateLongTextSpeech(
                request.getText(),
                request.getVoice(),
                request.getRate(),
                request.getPitch(),
                maxSegmentLength  // ← 使用检查后的值
        );
        // ... 其余代码不变
    }
}
```

**方案B：在 DTO 中使用 @NotNull 注解**

**文件：** `LongTextRequest.java`

```java
import javax.validation.constraints.NotNull;

/**
 * 每段最大字符数（可选，默认：500，范围：100-1000）
 */
@NotNull(message = "每段最大字符数不能为空")
@Min(value = 100, message = "每段最大字符数不能小于100")
@Max(value = 1000, message = "每段最大字符数不能大于1000")
private Integer maxSegmentLength = 500;
```

**推荐：** 方案A + 方案B（双重保险）

---

### 修复3：明确新旧接口的使用说明

**文件：** 创建一个 API 对比文档

**内容：** 参见下方"API 对比"部分

---

## 📊 API 对比（新旧接口）

### 旧接口（EdgeTTSController）

**保留原因：** 向后兼容

| 接口 | 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|------|
| 生成语音 | POST | `/api/edge-tts/generate` | 短文本生成 | ✅ 保留 |
| 健康检查 | GET | `/api/edge-tts/health` | 检查 TTS 状态 | ✅ 保留 |
| 音色列表 | GET | `/api/edge-tts/voices` | 获取音色列表 | ✅ 保留 |

---

### 新接口（TTSController）

**推荐使用：** 新功能更丰富

| 接口 | 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|------|
| 生成语音 | POST | `/api/tts/generate` | 短文本生成 | ✅ 新增 |
| 长文本生成 | POST | `/api/tts/long-text` | 长文本智能断句 | ✨ 新增 |
| 健康检查 | GET | `/api/tts/health` | 检查 TTS 状态 | ✅ 新增 |
| 音色列表 | GET | `/api/tts/voices` | 获取音色列表 | ✅ 新增 |

---

## 🎯 修复优先级

| 优先级 | 问题 | 修复时间 | 影响 |
|--------|------|---------|------|
| P0 | 添加 validation 依赖 | 1分钟 | 高（参数验证失效） |
| P1 | 防止 maxSegmentLength 为 null | 2分钟 | 中（可能空指针） |
| P2 | 明确新旧接口说明 | 5分钟 | 低（文档问题） |

---

## ✅ 修复后的完整性检查

### 检查清单：

- [ ] 1. `pom.xml` 中添加 `spring-boot-starter-validation` 依赖
- [ ] 2. `TTSController.generateLongTextSpeech()` 中添加 null 检查
- [ ] 3. `LongTextRequest.maxSegmentLength` 添加 `@NotNull` 注解
- [ ] 4. 编译项目：`mvn clean compile`
- [ ] 5. 启动项目：运行 `HmServiceApplication`
- [ ] 6. 测试短文本接口：`POST /api/tts/generate`
- [ ] 7. 测试长文本接口：`POST /api/tts/long-text`
- [ ] 8. 测试参数验证：传入空文本、超长文本、null 参数
- [ ] 9. 测试健康检查：`GET /api/tts/health`
- [ ] 10. 测试音色列表：`GET /api/tts/voices`

---

## 📝 总结

### 核心问题：
1. ❌ **缺少 validation 依赖**（必须修复）
2. ⚠️ **maxSegmentLength 可能为 null**（建议修复）
3. ℹ️ **新旧接口共存**（需要文档说明）

### 修复后的优势：
- ✅ 参数验证生效，防止非法参数
- ✅ 防止空指针异常
- ✅ 清晰的 API 文档
- ✅ 向后兼容

### 预计修复时间：
- 代码修改：5分钟
- 编译测试：3分钟
- **总计：8分钟**

---

**版本：** v2.0  
**作者：** Kiro  
**最后更新：** 2026-08-12  
**状态：** ⚠️ 发现问题，等待修复

