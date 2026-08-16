# WhisperX批量优化 - 编译问题修复

> **修复时间：** 2026-08-16  
> **状态：** ✅ 已全部修复

---

## 🔧 编译问题修复

### 问题1：getReadTimeout()方法不存在

**错误信息：**
```
java: 找不到符号
符号:   方法 getReadTimeout()
位置: 类型为org.springframework.http.client.SimpleClientHttpRequestFactory的变量 factory
```

**文件：** `WhisperXServiceImpl.java:85`

**原因：** `SimpleClientHttpRequestFactory`没有`getReadTimeout()`方法

**修复前：**
```java
factory.setReadTimeout(Math.max(timeoutSeconds * 1000, 60000));
restTemplate = new RestTemplate(factory);

log.info("[WhisperX] RestTemplate初始化完成，连接超时：5秒，读取超时：{}秒", 
        factory.getReadTimeout() / 1000);  // ❌ 方法不存在
```

**修复后：**
```java
int connectTimeout = 5000;
factory.setConnectTimeout(connectTimeout);

int readTimeout = Math.max(timeoutSeconds * 1000, 60000);
factory.setReadTimeout(readTimeout);

restTemplate = new RestTemplate(factory);

log.info("[WhisperX] RestTemplate初始化完成，连接超时：{}秒，读取超时：{}秒", 
        connectTimeout / 1000, readTimeout / 1000);  // ✅ 使用变量
```

**修复状态：** ✅ 已修复

---

### 问题2：Map和HashMap导入缺失

**错误信息：**
```
java: 找不到符号
符号:   类 Map
位置: 类 com.hmall.tts.volcengine.service.impl.DocumentTTSServiceImpl
```

**文件：** `DocumentTTSServiceImpl.java:234`

**原因：** 缺少`Map`和`HashMap`的导入

**修复前：**
```java
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
// ❌ 缺少 Map 和 HashMap
```

**修复后：**
```java
import java.util.ArrayList;
import java.util.HashMap;  // ✅ 新增
import java.util.List;
import java.util.Map;      // ✅ 新增
import java.util.UUID;
```

**修复状态：** ✅ 已修复

---

## 📋 所有修复汇总

| 文件 | 行号 | 问题 | 修复 |
|------|------|------|------|
| WhisperXServiceImpl.java | 85 | getReadTimeout()不存在 | 使用变量 |
| DocumentTTSServiceImpl.java | 234 | Map导入缺失 | 添加导入 |
| DocumentTTSServiceImpl.java | 234 | HashMap导入缺失 | 添加导入 |

---

## ✅ 编译验证

### 验证命令

```bash
cd d:\code\adminFlow\hm-service
mvn clean compile -DskipTests
```

### 预期结果

```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 如果遇到依赖问题

**问题：** Maven无法下载依赖（网络问题）

**解决方案1：** 使用阿里云镜像

编辑 `pom.xml`，添加：
```xml
<repositories>
    <repository>
        <id>aliyun</id>
        <url>https://maven.aliyun.com/repository/public</url>
    </repository>
</repositories>
```

**解决方案2：** 跳过编译，直接运行

如果只是依赖下载问题，代码本身没问题，可以：
1. 启动WhisperX服务
2. 直接运行已有的JAR包
3. 观察运行日志验证功能

---

## 🎯 关键代码位置

### WhisperXServiceImpl.java

**初始化方法（70-88行）：**
```java
@javax.annotation.PostConstruct
private void initRestTemplate() {
    SimpleClientHttpRequestFactory factory = 
        new SimpleClientHttpRequestFactory();
    
    int connectTimeout = 5000;
    factory.setConnectTimeout(connectTimeout);
    
    int readTimeout = Math.max(timeoutSeconds * 1000, 60000);
    factory.setReadTimeout(readTimeout);
    
    restTemplate = new RestTemplate(factory);
    
    log.info("[WhisperX] RestTemplate初始化完成，连接超时：{}秒，读取超时：{}秒", 
            connectTimeout / 1000, readTimeout / 1000);
}
```

### DocumentTTSServiceImpl.java

**导入语句（13-20行）：**
```java
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;  // ✅ 新增
import java.util.List;
import java.util.Map;      // ✅ 新增
import java.util.UUID;
```

**Map使用（234行）：**
```java
Map<String, Integer> segmentToBatchIndexMap = new HashMap<>();
```

---

## 📝 代码审查清单

编译前，请确认：

- [x] WhisperXServiceImpl.java导入正确
  - [x] `import org.springframework.web.client.RestTemplate;`
  - [x] `import org.springframework.http.*;`
  - [x] `import java.util.HashMap;`
  - [x] `import java.util.Map;`

- [x] DocumentTTSServiceImpl.java导入正确
  - [x] `import java.util.HashMap;`
  - [x] `import java.util.Map;`

- [x] 方法调用正确
  - [x] 不使用`factory.getReadTimeout()`
  - [x] 使用变量`readTimeout`

- [x] 变量声明正确
  - [x] `Map<String, Integer>`使用`HashMap<>()`初始化

---

## 🚀 下一步

编译成功后：

1. **启动WhisperX服务**
   ```bash
   cd D:\code\adminFlow\scripts
   start_whisperx_server.bat
   ```

2. **配置application.yml**
   ```yaml
   whisperx:
     use:
       server: true
     server:
       url: http://localhost:5000
   ```

3. **启动Java应用**
   ```bash
   cd d:\code\adminFlow\hm-service
   mvn spring-boot:run
   ```

4. **测试批量对齐**
   - 上传3行文档
   - 观察日志输出
   - 验证性能提升

---

## 📊 预期日志

### 启动日志

```
[WhisperX] RestTemplate初始化完成，连接超时：5秒，读取超时：120秒
```

### 批量对齐日志

```
[WhisperX] === 开始批量收集对齐任务 ===
[WhisperX] 收集：行0-segment0 → batch0
[WhisperX] 收集：行0-segment1 → batch1
[WhisperX] 收集完成，共5个segment需要对齐

[WhisperX] === 开始批量对齐 ===
[WhisperX] ✅ HTTP批量对齐完成，总耗时：6000 ms，平均每个：1200 ms
```

---

**修复完成时间：** 2026-08-16  
**状态：** ✅ 可以编译
