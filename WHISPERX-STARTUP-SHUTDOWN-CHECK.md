# WhisperX 启动/关闭验证清单

## ✅ 当前配置状态

### 1. **启动时自动启动** ✅
- **位置：** `HMallApplication.main()` 方法
- **时机：** Spring Boot 启动之前
- **代码：**
  ```java
  public static void main(String[] args) {
      // 1. 先启动 WhisperX 服务
      startWhisperXService();
      
      // 2. 再启动 Spring Boot
      SpringApplication.run(HMallApplication.class, args);
  }
  ```

### 2. **关闭时自动关闭** ✅
- **位置：** `HMallApplication.main()` 中的 ShutdownHook
- **时机：** JVM 关闭时自动触发
- **代码：**
  ```java
  Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("[WhisperX] 正在关闭服务...");
      stopWhisperXService();
  }));
  ```

### 3. **按需启动已禁用** ✅
- **WhisperXServiceImpl 配置：** `auto-start: false`（默认值）
- **ensureServerRunning 方法：** 只检查，不启动，服务不可用时抛出异常
- **删除的方法：**
  - ❌ `startWhisperXServer()` - 已删除
  - ❌ `stopWhisperXServer()` - 已删除

---

## 🔍 启动流程验证

### 预期启动日志（按顺序）

```
========================================
[WhisperX] 🚀 正在启动 HTTP 服务...
========================================
[WhisperX] 启动命令: py -3.13 D:/code/adminFlow/scripts/whisperx_server.py
[WhisperX] ⏳ 等待服务启动（最多180秒）...
[WhisperX Server] 使用设备：cpu
[WhisperX Server] 正在加载Whisper base模型...
[WhisperX Server] 模型加载完成
[WhisperX Server] Flask服务启动完成
========================================
[WhisperX] ✅ HTTP 服务启动成功！
[WhisperX] 服务地址: http://localhost:5000
========================================

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v2.7.12)

[WhisperX] WhisperX 服务由 HMallApplication 主类管理
[WhisperX] 按需启动已禁用，只使用已启动的服务
```

### 验证步骤

**步骤1：检查启动日志**
```bash
# 启动服务后，查看控制台输出
# 应该能看到上面的日志
```

**步骤2：验证服务可用**
```bash
# 方法1：访问健康检查接口
curl http://localhost:5000/health
# 预期返回：{"status":"ok"}

# 方法2：检查端口
netstat -ano | findstr :5000
# 应该能看到端口被占用
```

**步骤3：测试视频生成**
```bash
# 点击"自动生成视频"按钮
# 首次请求应该很快（0.5-1秒/段）
# 不应该看到 "[WhisperX] 服务未运行，准备按需启动..."
```

---

## 🔍 关闭流程验证

### 预期关闭日志

```
========================================
[WhisperX] 正在关闭服务...
========================================
[WhisperX] ✅ 服务已停止
```

### 验证步骤

**步骤1：正常关闭服务**
```bash
# 在 IDEA 中点击 "Stop" 按钮
# 或按 Ctrl+F2
```

**步骤2：检查日志**
```bash
# 应该能在控制台看到关闭日志
```

**步骤3：验证进程已清理**
```bash
# 检查 python 进程是否还在
tasklist | findstr python
# 应该看不到 whisperx_server.py 相关的进程
```

---

## ⚠️ 常见问题

### 问题1：启动时看不到 WhisperX 相关日志

**可能原因：**
- Maven 没有重新编译
- IDE 缓存问题

**解决方案：**
```bash
# 方法1：Maven 清理重新编译
mvn clean compile

# 方法2：IDEA 重新构建
Build -> Rebuild Project

# 方法3：重启 IDEA
```

---

### 问题2：服务启动超时

**症状：**
```
[WhisperX] ⏳ 等待服务启动（最多180秒）...
[WhisperX] ❌ 启动超时，将回退到按需启动模式
```

**可能原因：**
- 模型下载慢
- 网络问题
- Python 环境问题

**解决方案：**
```bash
# 手动测试 Python 脚本
cd D:\code\adminFlow\scripts
py -3.13 whisperx_server.py

# 查看是否有错误信息
```

---

### 问题3：视频生成还是很慢

**症状：**
```
[WhisperX] HTTP服务不可用：Connection refused
[WhisperX] 服务未运行，准备按需启动...
```

**诊断：**
```bash
# 检查服务是否真的启动了
curl http://localhost:5000/health

# 如果返回 Connection refused，说明服务没启动
# 检查启动日志中是否有 "[WhisperX] ✅ HTTP 服务启动成功！"
```

**解决方案：**
- 检查启动日志，找到启动失败的原因
- 可能需要手动启动服务测试

---

### 问题4：关闭后 Python 进程还在

**症状：**
```bash
tasklist | findstr python
# 还能看到 python.exe 进程
```

**解决方案：**
```bash
# 手动清理进程
taskkill /F /IM python.exe

# 或者更精确地只杀 whisperx 相关的进程
# 先找到 PID
netstat -ano | findstr :5000
# 然后 kill
taskkill /F /PID <PID>
```

---

## 📊 性能对比

### 之前（按需启动）
```
首次请求：60秒超时 ❌
后续请求：60秒超时 ❌
用户体验：非常差 ❌
```

### 现在（主类启动）
```
首次请求：0.5-1秒 ✅
后续请求：0.5-1秒 ✅
用户体验：非常好 ✅
```

---

## ✅ 验证清单

启动验证：
- [ ] 启动日志中看到 `[WhisperX] 🚀 正在启动 HTTP 服务...`
- [ ] 启动日志中看到 `[WhisperX] ✅ HTTP 服务启动成功！`
- [ ] 访问 `http://localhost:5000/health` 返回 `{"status":"ok"}`
- [ ] 日志中看到 `[WhisperX] WhisperX 服务由 HMallApplication 主类管理`
- [ ] 日志中看到 `[WhisperX] 按需启动已禁用，只使用已启动的服务`

功能验证：
- [ ] 自动模式生成视频速度快（0.5-1秒/段）
- [ ] 手动模式生成视频速度快
- [ ] 日志中**没有**出现 `[WhisperX] 服务未运行，准备按需启动...`
- [ ] 日志中**没有**出现 `[WhisperX] 正在启动HTTP服务...`（运行时不应该启动）

关闭验证：
- [ ] 关闭服务时日志中看到 `[WhisperX] 正在关闭服务...`
- [ ] 关闭服务时日志中看到 `[WhisperX] ✅ 服务已停止`
- [ ] 关闭后执行 `tasklist | findstr python` 看不到 whisperx 相关进程
- [ ] 关闭后执行 `netstat -ano | findstr :5000` 看不到端口占用

---

## 🎯 总结

**启动流程：**
```
启动 Spring Boot
  ↓
HMallApplication.main() 执行
  ↓
调用 startWhisperXService()
  ↓ 启动 Python 进程
  ↓ 加载 Whisper 模型（10-20秒）
  ↓ 启动 Flask HTTP 服务
  ✅ 服务就绪（http://localhost:5000）
  ↓
启动 Spring Boot 容器
  ↓
WhisperXServiceImpl 初始化
  ↓ 日志：按需启动已禁用
  ✅ 系统启动完成
```

**关闭流程：**
```
关闭 Spring Boot（Ctrl+F2 或 Stop 按钮）
  ↓
JVM 开始关闭
  ↓
触发 ShutdownHook
  ↓
调用 stopWhisperXService()
  ↓ 发送 destroy 信号
  ↓ 等待进程退出（最多5秒）
  ↓ 如果没退出，强制 kill
  ✅ 服务已停止
  ↓
JVM 退出
```

**使用流程：**
```
用户请求（自动/手动模式）
  ↓
WhisperXServiceImpl.align()
  ↓
调用 ensureServerRunning()
  ↓ 检查服务是否可用
  ↓ 如果可用 → 直接使用 ✅
  ↓ 如果不可用 → 抛出异常 ❌（不再尝试启动）
  ✅ 返回结果（0.5-1秒）
```

---

**最后更新：** 2026-08-17  
**版本：** v3.0（主类启动版本）
