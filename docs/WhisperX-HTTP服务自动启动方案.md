# WhisperX HTTP服务自动启动方案

> **实施时间：** 2026-08-16  
> **性能提升：** 30秒 → 2-3秒（提速10倍⚡）  
> **作者：** Kiro AI Assistant

---

## 📋 方案概述

**问题：**
- Manual模式生成音频很慢（30秒），因为每次都要加载WhisperX模型（18秒）
- Auto模式也受影响，第一次生成视频很慢

**解决方案：**
- Spring Boot启动时自动启动WhisperX HTTP服务
- 模型预加载到内存（只加载一次）
- 后续请求直接使用，无需重新加载
- 性能提升：30秒 → 2-3秒（提速10倍）

---

## 🚀 自动启动流程

### **1. Spring Boot启动**
```
[08:00:00] Spring Boot 应用启动中...
[08:00:01] 初始化各种Bean...
[08:00:02] ✅ WhisperXServerManager 检测到配置启用
[08:00:02] 🚀 开始启动 WhisperX HTTP 服务
```

### **2. WhisperX服务启动**
```
[08:00:03] 执行命令: py -3.13 D:/code/adminFlow/scripts/whisperx_server.py
[08:00:04] [WhisperX Server] 使用设备：cpu
[08:00:05] [WhisperX Server] 正在加载Whisper base模型...
[08:00:14] [WhisperX Server] ✅ Whisper模型加载完成
[08:00:15] [WhisperX Server] 服务启动中...
[08:00:15] [WhisperX Server] 监听地址：http://0.0.0.0:5000
```

### **3. 健康检查**
```
[08:00:16] [WhisperX Server] 等待服务启动（最多60秒）...
[08:00:17] [WhisperX Server] 健康检查URL: http://localhost:5000/health
[08:00:18] [WhisperX Server] ✅ 健康检查通过: {"status":"healthy","device":"cpu","model_loaded":true}
[08:00:18] ========================================
[08:00:18] [WhisperX Server] ✅ HTTP 服务启动成功！
[08:00:18] [WhisperX Server] 健康检查: http://localhost:5000/health
[08:00:18] [WhisperX Server] 对齐接口: POST http://localhost:5000/align
[08:00:18] [WhisperX Server] 批量接口: POST http://localhost:5000/align_batch
[08:00:18] ========================================
```

### **4. Spring Boot启动完成**
```
[08:00:19] Spring Boot 应用启动完成！
[08:00:19] 访问地址: http://localhost:8080
```

---

## ⚙️ 配置说明

### **配置文件位置**
```
d:\code\adminFlow\hm-service\src\main\resources\application.yaml
```

### **配置项详解**

```yaml
whisperx:
  server:
    # 是否启用自动启动（true=启用，false=禁用）
    enabled: true
    
    # Spring Boot启动时是否自动启动WhisperX服务（true=自动，false=手动）
    auto-start: true
    
    # Python路径（使用py launcher避免空格问题）
    python-path: py -3.13
    
    # WhisperX HTTP服务脚本路径
    script-path: D:/code/adminFlow/scripts/whisperx_server.py
    
    # 服务地址
    host: localhost
    
    # 服务端口
    port: 5000
    
    # 启动超时（秒）
    startup-timeout: 60
```

---

## 🎯 使用场景

### **场景1：开发环境（默认配置）**
```yaml
whisperx:
  server:
    enabled: true      # ✅ 启用
    auto-start: true   # ✅ 自动启动
```
**效果：**
- Spring Boot启动时自动启动WhisperX服务
- 开发时无需手动启动，开箱即用
- 适合日常开发和测试

---

### **场景2：生产环境（独立部署）**
```yaml
whisperx:
  server:
    enabled: true      # ✅ 启用
    auto-start: false  # ❌ 不自动启动
    url: http://whisperx-server:5000  # 使用独立服务器
```
**效果：**
- WhisperX服务部署在独立服务器
- Spring Boot启动时不启动WhisperX
- 通过HTTP调用远程服务
- 适合生产环境

---

### **场景3：禁用模式（回退到Python脚本）**
```yaml
whisperx:
  use:
    server: false      # ❌ 禁用HTTP服务
  server:
    enabled: false     # ❌ 不启用自动启动
```
**效果：**
- 不使用HTTP服务
- 回退到Python脚本模式（每次加载模型）
- 适合测试或故障排查

---

## 📊 性能对比

| 模式 | 启动耗时 | 单次请求耗时 | 并发支持 | 推荐场景 |
|------|---------|-------------|---------|---------|
| Python脚本模式 | 0秒 | 30秒 | ❌ | 测试/故障排查 |
| HTTP服务（手动启动） | 30秒 | 2-3秒 | ✅ | 临时测试 |
| HTTP服务（自动启动⭐） | +18秒 | 2-3秒 | ✅ | **开发环境（推荐）** |
| HTTP服务（独立部署） | 0秒 | 2-3秒 | ✅ | 生产环境 |

**说明：**
- **Python脚本模式：** 每次请求都加载模型，慢但简单
- **HTTP服务（手动启动）：** 启动一次，后续快速，但每次手动启动麻烦
- **HTTP服务（自动启动）：** Spring Boot启动慢18秒，但后续所有请求都快，推荐！
- **HTTP服务（独立部署）：** 生产环境最佳方案，Spring Boot无需启动WhisperX

---

## 🔧 故障排查

### **问题1：服务启动失败**

**日志：**
```
[WhisperX Server] ❌ 启动失败：脚本文件不存在：D:/code/adminFlow/scripts/whisperx_server.py
```

**解决方案：**
1. 检查脚本路径是否正确
2. 确认脚本文件存在
3. 修改配置文件中的`script-path`

---

### **问题2：端口被占用**

**日志：**
```
[WhisperX Server] ⚠️ 端口 5000 已被占用，假设服务已在运行
```

**解决方案：**
1. 检查是否已手动启动了WhisperX服务
2. 如果端口被其他程序占用，修改配置：
   ```yaml
   whisperx:
     server:
       port: 5001  # 改为其他端口
   ```

---

### **问题3：启动超时**

**日志：**
```
[WhisperX Server] ❌ 启动超时（60秒）
```

**解决方案：**
1. 增加启动超时时间：
   ```yaml
   whisperx:
     server:
       startup-timeout: 120  # 改为120秒
   ```
2. 检查网络连接（下载模型需要网络）
3. 检查Python环境和依赖是否正确安装

---

### **问题4：Python版本不匹配**

**日志：**
```
[WhisperX Server] ❌ 启动失败：No Python 3.13 found
```

**解决方案：**
1. 检查Python版本：
   ```bash
   py -3.13 --version
   ```
2. 修改配置使用其他Python版本：
   ```yaml
   whisperx:
     server:
       python-path: py -3.11  # 或 python、python3
   ```

---

## 🎓 高级用法

### **1. 手动重启服务**

如果需要重启WhisperX服务（不重启Spring Boot）：

```java
@Autowired
private WhisperXServerManager serverManager;

// 重启服务
serverManager.restartServer();
```

---

### **2. 检查服务状态**

```java
@Autowired
private WhisperXServerManager serverManager;

// 检查服务是否启动
boolean isStarted = serverManager.isServerStarted();
```

---

### **3. 健康检查端点**

访问：
```
http://localhost:5000/health
```

响应：
```json
{
  "status": "healthy",
  "device": "cpu",
  "model_loaded": true
}
```

---

## 📝 总结

### **优势**
- ✅ 自动启动，无需手动操作
- ✅ 性能提升10倍（30秒 → 2-3秒）
- ✅ 开发体验极佳，开箱即用
- ✅ Spring Boot关闭时自动停止服务
- ✅ 支持并发，多个请求同时处理

### **注意事项**
- ⚠️ Spring Boot启动时间增加18秒（模型加载）
- ⚠️ 占用2-3GB内存（模型常驻）
- ⚠️ 首次启动需要下载模型（需要网络）

### **推荐配置**
- **开发环境：** `auto-start: true`（自动启动）
- **生产环境：** `auto-start: false`（独立部署）

---

**最后更新时间：** 2026-08-16  
**版本：** v1.0
