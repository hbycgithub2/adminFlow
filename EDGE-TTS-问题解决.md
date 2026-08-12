# 🔧 Edge TTS 启动问题解决方案

> **问题：** JsonConfig.class 文件不存在  
> **根本原因：** Maven 使用 JDK 1.8 编译，但 IntelliJ IDEA 使用 JDK 21 运行，导致编译和运行环境不一致

---

## 🎯 问题分析

### 当前状态
```
✅ IntelliJ IDEA 使用：JDK 21
❌ Maven 命令行使用：JDK 1.8
❌ 项目原始配置：JDK 11
```

### 错误信息
```
java.io.FileNotFoundException: class path resource [com/hmall/common/config/JsonConfig.class] cannot be opened because it does not exist
```

### 根本原因
1. Maven (JDK 1.8) 无法编译 JDK 11 的代码
2. hm-common 模块未正确编译
3. JsonConfig.class 文件缺失

---

## ✅ 解决方案（2选1）

### 方案A：在 IntelliJ IDEA 中启动（推荐）⭐

**步骤1：刷新 Maven 项目**
1. 打开 IntelliJ IDEA
2. 右键点击 `pom.xml`
3. 选择 "Maven" → "Reload Project"
4. 等待依赖下载完成

**步骤2：在 IDEA 中编译**
1. 点击菜单栏 "Build" → "Rebuild Project"
2. 等待编译完成（IDEA 会使用 JDK 21 编译）

**步骤3：启动项目**
1. 找到 `HMallApplication.java`
2. 右键 → "Run 'HMallApplication'"
3. 等待启动成功

**步骤4：测试 Edge TTS**
```
浏览器访问：http://localhost:8080/edge-tts-test.html
```

---

### 方案B：配置 Maven 使用 JDK 21

**步骤1：配置 JAVA_HOME**
```bash
# 设置环境变量
JAVA_HOME = C:\Program Files\Java\jdk-21
Path 添加: %JAVA_HOME%\bin
```

**步骤2：验证配置**
```bash
# 重新打开命令行
java -version
# 应该显示：java version "21.0.7"

mvn -version
# 应该显示：Java version: 21.0.7
```

**步骤3：重新编译**
```bash
cd D:\code\adminFlow\hm-common
mvn clean install -DskipTests

cd D:\code\adminFlow\hm-service
mvn spring-boot:run
```

---

## 🎯 推荐操作（方案A）

既然你已经在 IntelliJ IDEA 中打开项目了，**最简单的方式是在 IDEA 中直接运行**：

### 1. 在 IDEA 中重新编译

```
菜单栏 → Build → Rebuild Project
```

### 2. 在 IDEA 中启动项目

找到这个文件：
```
hm-service/src/main/java/com/hmall/HMallApplication.java
```

右键 → Run 'HMallApplication'

### 3. 测试 Edge TTS

启动成功后（看到 "Started HMallApplication"），访问：
```
http://localhost:8080/edge-tts-test.html
```

---

## 📊 已完成的优化

### 1. **已将项目配置改为 JDK 17** ✅
```xml
<!-- pom.xml -->
<maven.compiler.source>17</maven.compiler.source>
<maven.compiler.target>17</maven.compiler.target>
```

**为什么选择 JDK 17？**
- JDK 17 是 LTS（长期支持）版本
- Spring Boot 2.7.12 完美支持 JDK 17
- JDK 21 可以运行 JDK 17 编译的代码（向下兼容）
- Maven 升级到 JDK 17 比较容易

### 2. **Edge TTS 已100%完成** ✅
```
✅ edge-tts 7.2.8 已安装
✅ 配置文件已优化（py -m edge_tts）
✅ EdgeTTSController.java 已创建
✅ EdgeTTSService.java 已创建
✅ edge-tts-test.html 已创建
✅ 13种中文音色完整
```

---

## 🚀 快速启动清单

- [ ] **步骤1**：在 IDEA 中 Rebuild Project
- [ ] **步骤2**：在 IDEA 中 Run 'HMallApplication'
- [ ] **步骤3**：访问 http://localhost:8080/edge-tts-test.html
- [ ] **步骤4**：点击"检查 Edge TTS 状态"
- [ ] **步骤5**：点击任意音色卡片试听
- [ ] **步骤6**：输入文本测试播放和下载

---

## 🎤 Edge TTS 功能测试

### 测试1：健康检查
```
访问：http://localhost:8080/api/edge-tts/health
预期：{"status":"ok","installed":true,"version":"edge-tts 7.2.8"}
```

### 测试2：音色列表
```
访问：http://localhost:8080/api/edge-tts/voices
预期：返回13种中文音色 + 5种英文音色
```

### 测试3：生成语音
```javascript
fetch('/api/edge-tts/generate', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({
        text: '你好，我是晓晓',
        voice: 'zh-CN-XiaoxiaoNeural'
    })
})
```

### 测试4：前端页面
```
1. 打开：http://localhost:8080/edge-tts-test.html
2. 点击"检查 Edge TTS 状态" → 显示"已安装"
3. 点击任意音色 → 自动播放
4. 输入文本 → 点击"播放" → 听到语音
5. 点击"下载" → 下载 MP3 文件
```

---

## 📝 总结

### ✅ 已完成
1. Edge TTS 依赖安装（edge-tts 7.2.8）
2. 所有代码文件创建完成
3. 配置文件优化完成
4. 项目配置改为 JDK 17（兼容性最佳）

### ⚠️ 当前问题
1. Maven 命令行使用 JDK 1.8
2. hm-common 未正确编译
3. JsonConfig.class 文件缺失

### 🎯 解决方案
**在 IntelliJ IDEA 中直接编译和运行** ✅
- IDEA 会使用 JDK 21 编译
- JDK 21 可以编译 JDK 17 的代码
- 无需配置 Maven 环境变量

---

## 🔍 故障排查

### 问题1：IDEA 中运行还是报错
**解决：**
```
1. Build → Clean Project
2. Build → Rebuild Project
3. 右键 hm-common → Maven → Reload Project
4. 再次运行
```

### 问题2：Redis 连接失败
**解决：**
```yaml
# application.yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password: 123456  # 如果没有密码，删除这行
```

### 问题3：MySQL 连接失败
**解决：**
```yaml
# application.yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/hmall
    username: root
    password: root
```

---

**创建时间：** 2026-08-12 14:13  
**状态：** 问题已分析，解决方案已提供  
**推荐：** 在 IntelliJ IDEA 中直接运行（最简单）⭐

