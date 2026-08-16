# WhisperX 服务使用指南

## 🎯 架构设计

```
Spring Boot 启动
    ↓
WhisperXServerManager 自动启动
    ↓ 加载 Whisper 模型（10-20秒）
    ↓ 启动 Flask HTTP 服务（localhost:5000）
    ↓ 健康检查通过（/health）
    ✅ 服务就绪
    
用户请求（自动模式/手动模式）
    ↓
WhisperXServiceImpl.align()
    ↓ 检查服务是否可用
    ↓ 如果可用 → 直接使用（快速！）
    ↓ 如果不可用 → 尝试按需启动（备用方案）
    ✅ 返回字幕数据

Spring Boot 关闭
    ↓
WhisperXServerManager @PreDestroy
    ↓ 优雅关闭 WhisperX 进程
    ✅ 清理完成
```

## 📋 配置说明

### application.yaml 核心配置

```yaml
whisperx:
  use:
    server: true                          # 启用HTTP服务模式（推荐）
  
  server:
    url: http://localhost:5000            # 服务地址
    enabled: true                         # 启用自动管理
    auto-start: true                      # Spring Boot启动时自动启动
    startup-timeout: 180                  # 启动超时（秒，模型加载需要时间）
    
    python-path: py -3.13                 # Python路径
    script-path: D:/code/adminFlow/scripts/whisperx_server.py
    host: localhost
    port: 5000
```

## ✅ 功能特性

### 1. 自动启动（推荐）
- **Spring Boot 启动时自动启动 WhisperX 服务**
- 一次性加载模型，后续请求快速响应
- 适合生产环境和频繁使用场景

### 2. 自动模式支持
- 用户在前端点击"自动生成视频"
- 后端自动调用 WhisperX 进行字幕对齐
- **无需等待模型加载，直接使用已启动的服务**

### 3. 手动模式支持
- 用户手动上传音频和文本
- 后端调用 WhisperX 进行精确对齐
- **同样使用已启动的服务，速度快**

### 4. 容错机制
- 如果服务意外关闭，自动检测并尝试重启
- 如果重启失败，回退到 Python 脚本模式
- 确保服务稳定性

### 5. 优雅关闭
- Spring Boot 关闭时自动关闭 WhisperX 服务
- 清理资源，避免进程残留

## 🚀 启动验证

### 预期启动日志

```
========================================
[WhisperX Server] ✅ WhisperXServerManager Bean 已创建
========================================
[WhisperX Server] @PostConstruct 方法被调用
========================================
[WhisperX Server] 🚀 开始启动 HTTP 服务
========================================
[WhisperX Server] Python路径: py -3.13
[WhisperX Server] 脚本路径: D:/code/adminFlow/scripts/whisperx_server.py
[WhisperX Server] 服务地址: http://localhost:5000
========================================
[WhisperX Server] 执行命令: py -3.13 D:/code/adminFlow/scripts/whisperx_server.py
[WhisperX Server] 等待服务启动（最多180秒）...
[WhisperX Server] 健康检查URL: http://localhost:5000/health
[WhisperX Server] 使用设备：cpu
[WhisperX Server] 正在加载Whisper base模型...
[WhisperX Server] ✅ 健康检查通过: {"status":"ok"}
========================================
[WhisperX Server] ✅ HTTP 服务启动成功！
[WhisperX Server] 健康检查: http://localhost:5000/health
[WhisperX Server] 对齐接口: POST http://localhost:5000/align
[WhisperX Server] 批量接口: POST http://localhost:5000/align_batch
========================================
```

### 验证服务是否启动

```bash
# 方法1：检查端口占用
netstat -ano | findstr :5000

# 方法2：访问健康检查接口
curl http://localhost:5000/health
# 预期返回：{"status":"ok"}

# 方法3：查看进程
tasklist | findstr python
```

## 📊 性能对比

| 模式 | 首次请求耗时 | 后续请求耗时 | 说明 |
|------|-------------|-------------|------|
| **自动启动模式（推荐）** | 0.5-1秒 | 0.5-1秒 | Spring Boot启动时已加载模型 |
| 按需启动模式 | 20-30秒 | 0.5-1秒 | 首次需要加载模型 |
| Python脚本模式 | 20-30秒 | 20-30秒 | 每次都需要加载模型 |

## 🔧 故障排查

### 问题1：启动日志中看不到 WhisperX Server 相关信息

**可能原因：**
- `WhisperXServerManager` 这个 Bean 没有被 Spring 加载

**解决方案：**
1. 检查 `WhisperXServerManager.java` 是否在正确的包下
2. 检查类上是否有 `@Component` 注解
3. 重启 IDE 并清理缓存

### 问题2：服务启动超时

**可能原因：**
- 模型下载/加载太慢
- `startup-timeout` 设置太短

**解决方案：**
```yaml
whisperx:
  server:
    startup-timeout: 300  # 增加到5分钟
```

### 问题3：服务启动后自动模式仍然很慢

**可能原因：**
- 服务虽然启动了，但每次请求都重新启动进程

**解决方案：**
1. 检查日志，确认是否有 `[WhisperX] 服务未运行，准备按需启动...`
2. 如果有，说明健康检查失败，需要排查网络问题

### 问题4：Spring Boot 关闭后 WhisperX 进程没有关闭

**可能原因：**
- `@PreDestroy` 方法没有执行

**解决方案：**
1. 优雅关闭 Spring Boot（不要强制 kill）
2. 检查是否有异常导致关闭失败
3. 手动清理：`taskkill /F /IM python.exe`

## 🎬 使用示例

### 自动模式生成视频

```
用户点击"自动生成视频"
    ↓
前端调用：POST /api/video/generate
    ↓
后端调用：DocumentTTSServiceImpl.generateDocumentSpeech()
    ↓
调用：WhisperXServiceImpl.alignBatch()
    ↓
检测到服务已启动（WhisperXServerManager）
    ↓
直接使用 HTTP 服务（快速！0.5-1秒/段）
    ✅ 返回精确字幕数据
```

### 手动模式生成视频

```
用户上传音频 + 文本
    ↓
前端调用：POST /api/video/generate-manual
    ↓
后端调用：WhisperXServiceImpl.align()
    ↓
检测到服务已启动
    ↓
直接使用 HTTP 服务（快速！）
    ✅ 返回精确字幕数据
```

## 📝 开发建议

### 生产环境
```yaml
whisperx:
  server:
    enabled: true
    auto-start: true           # 启用自动启动
    startup-timeout: 180       # 足够的启动时间
    auto-shutdown: false       # 关闭自动关闭（保持常驻）
```

### 开发环境
```yaml
whisperx:
  server:
    enabled: true
    auto-start: true           # 启用自动启动，开发时也很快
    startup-timeout: 180
    auto-shutdown: true        # 启用自动关闭，节省资源
    idle-timeout-minutes: 10   # 10分钟不用就关闭
```

### 测试环境
```yaml
whisperx:
  server:
    enabled: false             # 禁用自动启动
    auto-start: true           # 按需启动
```

## 🌟 最佳实践

1. **生产环境务必启用自动启动**
   - 用户体验更好（首次请求也很快）
   - 避免多次加载模型浪费资源

2. **合理设置超时时间**
   - 首次启动：180秒（模型下载+加载）
   - 后续启动：60秒（模型已缓存）

3. **监控服务状态**
   - 定期访问 `/health` 接口检查服务状态
   - 如果服务异常，及时重启

4. **日志级别设置**
   ```yaml
   logging:
     level:
       com.hmall.tts.whisperx: DEBUG  # 开发时用 DEBUG
       # com.hmall.tts.whisperx: INFO   # 生产时用 INFO
   ```

## ✅ 验证清单

- [ ] Spring Boot 启动时能看到 `[WhisperX Server] ✅ WhisperXServerManager Bean 已创建`
- [ ] 能看到 `[WhisperX Server] 🚀 开始启动 HTTP 服务`
- [ ] 能看到 `[WhisperX Server] ✅ HTTP 服务启动成功！`
- [ ] 访问 `http://localhost:5000/health` 返回 `{"status":"ok"}`
- [ ] 自动模式生成视频速度快（不再等待60秒超时）
- [ ] 手动模式生成视频速度快
- [ ] Spring Boot 关闭时能看到 `[WhisperX Server] ✅ HTTP 服务已停止`
- [ ] 关闭后检查进程，确认 python 进程已清理

---

**最后更新：** 2026-08-17  
**版本：** v2.0（统一服务管理版本）
