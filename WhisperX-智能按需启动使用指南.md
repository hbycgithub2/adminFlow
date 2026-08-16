# WhisperX智能按需启动使用指南

> **版本：** v1.0  
> **更新时间：** 2026-08-17  
> **功能：** 按需启动 + 30分钟自动关闭

---

## 🎯 核心特性

### 1. 按需启动（Lazy Loading）
- ✅ **Java启动快速**：不等待WhisperX加载，立即启动完成
- ✅ **用时才启动**：首次生成视频时自动启动WhisperX服务
- ✅ **透明无感**：用户无需手动操作，自动管理

### 2. 自动关闭（Auto Shutdown）
- ✅ **节省内存**：空闲30分钟后自动关闭，释放1-2GB内存
- ✅ **智能监控**：后台线程每60秒检查一次空闲状态
- ✅ **灵活配置**：可调整超时时间（5-60分钟）

### 3. 手动控制
- ✅ **管理页面**：访问 http://localhost:8080/whisperx-manager.html
- ✅ **API接口**：提供启动/停止/状态查询接口
- ✅ **实时监控**：查看服务状态、空闲时间、最后使用时间

---

## 📊 性能对比

| 场景 | 旧方案（脚本模式） | 新方案（智能启动） | 提升 |
|------|------------------|-------------------|------|
| **Java启动时间** | 立即启动 | 立即启动 | 0秒 |
| **首次生成视频** | 25秒 | 15秒（启动）+ 2秒（对齐）= 17秒 | 快32% |
| **后续生成视频** | 25秒 | 2秒 | **快92%** ⬆️ |
| **10分钟不用** | - | 服务运行中 | - |
| **30分钟不用** | - | 自动关闭，释放内存 ✅ | - |
| **再次使用** | 25秒 | 15秒（重启）+ 2秒（对齐）= 17秒 | 快32% |
| **内存占用** | 0MB（每次临时） | 0-2GB（用时才占用） | 按需占用 |

---

## 🚀 快速开始

### 方式1：自动启动（推荐）

```bash
# 1. 启动Java服务
cd d:\code\adminFlow
start-adminFlow.bat

# 2. 访问前端页面，生成视频
# 首次点击"生成视频"时，自动启动WhisperX（等待15秒）
# 后续生成视频，立即完成（2秒）
```

**体验流程：**
```
用户打开页面
  ↓
点击"生成视频"
  ↓ 提示：首次启动需要15秒...
  ↓ 自动启动WhisperX服务
  ↓ 加载Whisper模型（9秒）
  ↓ 加载Wav2Vec2模型（6秒）
  ↓ 执行对齐（2秒）
  ↓
✅ 视频生成完成（总耗时17秒）
  ↓
再次生成视频
  ↓ 直接使用已启动的服务
  ↓
✅ 视频生成完成（总耗时2秒）
  ↓
30分钟无操作
  ↓ 后台自动检测空闲
  ↓ 自动关闭WhisperX
  ↓
✅ 内存释放（节省1-2GB）
```

---

### 方式2：手动管理

**访问管理页面：**
```
http://localhost:8080/whisperx-manager.html
```

**功能：**
- 📊 **查看状态**：服务运行状态、空闲时间、最后使用时间
- ▶️ **手动启动**：立即启动WhisperX服务
- ⏹️ **手动停止**：立即停止服务，释放内存
- 🔄 **刷新状态**：实时更新服务状态

**使用场景：**
- 提前启动服务（避免首次等待15秒）
- 立即释放内存（不等30分钟自动关闭）
- 查看服务运行状态

---

## ⚙️ 配置说明

**配置文件：** `hm-service/src/main/resources/application.yaml`

```yaml
whisperx:
  server:
    # 按需启动配置
    auto-start: true                    # 启用自动启动（用时才启动）
    startup-timeout: 60                 # 启动超时（秒）
    
    # 自动关闭配置
    auto-shutdown: true                 # 启用自动关闭
    idle-timeout-minutes: 30            # 空闲30分钟后关闭
    check-interval-seconds: 60          # 每60秒检查一次
```

**调整超时时间：**

| 场景 | 推荐配置 | 说明 |
|------|---------|------|
| **开发环境（经常用）** | `idle-timeout-minutes: 60` | 1小时不用才关闭 |
| **生产环境（偶尔用）** | `idle-timeout-minutes: 30` | 30分钟不用就关闭 |
| **高频使用（始终开）** | `auto-shutdown: false` | 禁用自动关闭 |
| **节省内存（快速关）** | `idle-timeout-minutes: 5` | 5分钟不用就关闭 |

---

## 📡 API接口

### 1. 查看服务状态

```bash
GET http://localhost:8080/api/whisperx/status
```

**响应示例：**
```json
{
  "processAlive": true,
  "serverAvailable": true,
  "lastUsedTime": 1723891234567,
  "autoStart": true,
  "autoShutdown": true,
  "idleTimeoutMinutes": 30,
  "idleMinutes": 5
}
```

---

### 2. 手动启动服务

```bash
POST http://localhost:8080/api/whisperx/start
```

**响应示例：**
```json
{
  "success": true,
  "message": "服务启动中，请等待15秒后刷新状态"
}
```

---

### 3. 手动停止服务

```bash
POST http://localhost:8080/api/whisperx/stop
```

**响应示例：**
```json
{
  "success": true,
  "message": "服务已停止"
}
```

---

## 🔍 监控日志

**查看启动日志：**
```
[WhisperX] 服务初始化完成（懒加载模式，用时才启动）
[WhisperX] 配置：自动启动=true, 自动关闭=true, 空闲超时=30分钟
[WhisperX] 空闲监控已启动，每60秒检查一次
```

**首次使用时：**
```
[WhisperX] 服务未运行，准备按需启动...
[WhisperX] 正在启动HTTP服务...
[WhisperX] 启动命令：py -3.13 D:/code/adminFlow/scripts/whisperx_server.py
[WhisperX] 等待服务启动（最多60秒）...
[WhisperX Server] 使用设备：cpu
[WhisperX Server] 正在加载Whisper base模型...
[WhisperX Server] ✅ Whisper模型加载完成
[WhisperX] ✅ 服务启动成功：http://localhost:5000/health
```

**空闲检查日志：**
```
[WhisperX] 服务空闲5分钟（阈值：30分钟），继续运行
[WhisperX] 服务空闲10分钟（阈值：30分钟），继续运行
...
[WhisperX] 服务空闲30分钟，准备自动关闭...
[WhisperX] 正在停止服务...
[WhisperX] ✅ 服务已停止
```

---

## ❓ 常见问题

### Q1: 首次生成视频为什么需要等15秒？
**A:** 首次启动需要加载两个AI模型：
- Whisper模型（9秒）：语音识别
- Wav2Vec2模型（6秒）：音频对齐

模型加载到内存后，后续对齐只需2秒。

---

### Q2: 如何提前启动服务，避免首次等待？
**A:** 有3种方式：
1. **访问管理页面**：http://localhost:8080/whisperx-manager.html，点击"启动服务"
2. **调用API**：`curl -X POST http://localhost:8080/api/whisperx/start`
3. **手动启动**：运行 `scripts/start_whisperx_server.bat`

---

### Q3: 如何禁用自动关闭，让服务一直运行？
**A:** 修改 `application.yaml`：
```yaml
whisperx:
  server:
    auto-shutdown: false  # 禁用自动关闭
```

---

### Q4: 如何调整空闲超时时间？
**A:** 修改 `application.yaml`：
```yaml
whisperx:
  server:
    idle-timeout-minutes: 60  # 改为60分钟
```

---

### Q5: 如何立即释放内存？
**A:** 有3种方式：
1. **访问管理页面**：点击"停止服务"
2. **调用API**：`curl -X POST http://localhost:8080/api/whisperx/stop`
3. **重启Java**：重启后服务不会自动启动

---

### Q6: 服务自动关闭后，再次使用需要等多久？
**A:** 需要重新启动，首次对齐耗时17秒（15秒启动 + 2秒对齐）。

---

### Q7: 如何查看服务运行状态？
**A:** 有2种方式：
1. **访问管理页面**：http://localhost:8080/whisperx-manager.html
2. **调用API**：`curl http://localhost:8080/api/whisperx/status`

---

## 📝 升级说明

**从旧版本升级：**
1. 备份 `WhisperXServiceImpl.java`
2. 替换为新版本代码
3. 更新 `application.yaml` 配置
4. 添加 `WhisperXManagementController.java`
5. 添加 `whisperx-manager.html`
6. 重启Java服务

**兼容性：**
- ✅ 向下兼容：如果禁用自动启动，回退到脚本模式
- ✅ 配置兼容：旧配置仍然有效，新增配置有默认值

---

## 🎉 总结

**核心优势：**
1. ✅ **快速启动**：Java启动不等待WhisperX
2. ✅ **按需使用**：用时才启动，透明无感
3. ✅ **节省内存**：空闲30分钟自动关闭
4. ✅ **灵活控制**：提供管理页面和API接口
5. ✅ **性能提升**：后续对齐速度提升92%

**适用场景：**
- ✅ 开发环境：经常生成视频
- ✅ 生产环境：偶尔生成视频
- ✅ 测试环境：节省服务器资源

---

**文档版本：** v1.0  
**最后更新：** 2026-08-17  
**作者：** Kiro AI Assistant
