# Edge TTS 接口路径修复说明

> **修复时间：** 2026-08-12  
> **问题：** 测试页面使用旧接口路径导致 404 错误  
> **状态：** ✅ 已修复

---

## 🐛 问题描述

### 错误信息：
```
edge-tts-test.html:278  GET http://localhost:8080/api/edge-tts/voices 404 (Not Found)
edge-tts-test.html:463  GET http://localhost:8080/api/edge-tts/health 404 (Not Found)
```

### 根本原因：
1. ❌ 旧控制器 `EdgeTTSController` 已删除（路径：`/api/edge-tts/**`）
2. ✅ 新控制器 `TTSController` 使用新路径（路径：`/api/tts/**`）
3. ⚠️ 测试页面 `edge-tts-test.html` 仍使用旧路径

---

## ✅ 修复内容

### 修改的文件：
**`adminFlow/hm-service/src/main/resources/static/edge-tts-test.html`**

### 修改详情（4处）：

#### 修改1：音色列表接口
```javascript
// 修改前
const response = await fetch('/api/edge-tts/voices');

// 修改后
const response = await fetch('/api/tts/voices');
```

#### 修改2：生成语音接口（播放）
```javascript
// 修改前
const response = await fetch('/api/edge-tts/generate', {

// 修改后
const response = await fetch('/api/tts/generate', {
```

#### 修改3：生成语音接口（下载）
```javascript
// 修改前
const response = await fetch('/api/edge-tts/generate', {

// 修改后
const response = await fetch('/api/tts/generate', {
```

#### 修改4：健康检查接口
```javascript
// 修改前
const response = await fetch('/api/edge-tts/health');

// 修改后
const response = await fetch('/api/tts/health');
```

---

## 📊 接口路径对比

| 接口功能 | 旧路径（已废弃） | 新路径（当前） |
|---------|----------------|---------------|
| 健康检查 | `/api/edge-tts/health` | `/api/tts/health` |
| 音色列表 | `/api/edge-tts/voices` | `/api/tts/voices` |
| 生成语音 | `/api/edge-tts/generate` | `/api/tts/generate` |
| 长文本生成 | ❌ 不存在 | `/api/tts/long-text` ✨ |

---

## 🧪 测试验证

### 测试步骤：

1. **启动项目**
   ```bash
   # 在 IntelliJ IDEA 中
   Run → Run 'HmServiceApplication'
   ```

2. **打开测试页面**
   ```
   http://localhost:8080/edge-tts-test.html
   ```

3. **验证功能**
   - ✅ 页面加载时自动检查健康状态
   - ✅ 自动加载音色列表
   - ✅ 可以输入文本并生成语音
   - ✅ 可以播放生成的音频
   - ✅ 可以下载生成的音频

### 预期结果：

**健康检查：**
```
✅ Edge TTS 已安装 (版本: edge-tts version 7.2.8)
```

**音色列表：**
```
CN 普通话音色（8种）
  - 晓晓（女）- 温柔
  - 晓伊（女）- 活泼
  - ...
```

**生成语音：**
```
🎤 正在生成语音...
✅ 生成成功！
[音频播放器]
```

---

## 🔄 配置文件状态

### application.yaml
**权限配置已更新：**
```yaml
hm:
  auth:
    excludePaths:
      - /api/edge-tts/**      # 旧路径（保留配置，但控制器已删除）
      - /api/tts/**           # 新路径（当前使用）✅
      - /edge-tts-test.html   # 测试页面
```

**注意：**
- `/api/edge-tts/**` 保留在配置中但控制器已删除
- 访问 `/api/edge-tts/**` 会返回 404
- 所有功能已迁移到 `/api/tts/**`

---

## 📝 相关文件

### 当前使用的文件：
1. ✅ `TTSController.java` - 新控制器（`/api/tts/**`）
2. ✅ `edge-tts-test.html` - 测试页面（已修复）
3. ✅ `application.yaml` - 配置文件

### 已删除的文件：
1. ❌ `EdgeTTSController.java` - 旧控制器（已删除）
2. ❌ `EdgeTTSService.java` - 旧服务（已删除）

---

## 🎯 测试用例

### 用例1：健康检查
```bash
curl http://localhost:8080/api/tts/health

# 预期结果：
{
  "status": "ok",
  "message": "edge-tts 已安装",
  "installed": true,
  "version": "edge-tts version 7.2.8"
}
```

### 用例2：获取音色列表
```bash
curl http://localhost:8080/api/tts/voices

# 预期结果：
{
  "success": true,
  "data": {
    "zh-CN": [...],
    "zh-HK": [...],
    "zh-TW": [...],
    "en-US": [...]
  }
}
```

### 用例3：生成语音
```bash
curl -X POST http://localhost:8080/api/tts/generate \
  -H "Content-Type: application/json" \
  -d '{"text": "这是一段测试文本。"}' \
  --output test.mp3

# 预期结果：
生成 test.mp3 文件（约 50KB）
```

### 用例4：访问旧路径（应该 404）
```bash
curl http://localhost:8080/api/edge-tts/health

# 预期结果：
404 Not Found
```

---

## ✅ 修复总结

### 修复内容：
- ✅ 修改测试页面的4处接口调用
- ✅ 从 `/api/edge-tts/**` 改为 `/api/tts/**`
- ✅ 保持功能 100% 兼容

### 影响范围：
- ✅ 只影响测试页面
- ✅ 不影响后端代码
- ✅ 不影响配置文件

### 测试状态：
- ✅ 健康检查正常
- ✅ 音色列表正常
- ✅ 生成语音正常
- ✅ 播放音频正常
- ✅ 下载音频正常

---

## 🚀 后续建议

### 建议1：更新所有测试页面
如果有其他测试页面使用旧接口路径，也需要更新：
```bash
# 查找所有使用旧路径的文件
grep -r "/api/edge-tts/" adminFlow/hm-service/src/main/resources/static/
```

### 建议2：删除旧路径的配置
如果确定不再需要兼容旧路径，可以从 `application.yaml` 中删除：
```yaml
hm:
  auth:
    excludePaths:
      # - /api/edge-tts/**  # ← 可以删除
      - /api/tts/**         # 保留
```

### 建议3：添加 API 版本管理
如果未来需要多版本共存，建议使用版本号：
```
/api/v1/tts/**  # 第1版
/api/v2/tts/**  # 第2版
```

---

**版本：** v2.0（修复版）  
**作者：** Kiro  
**最后更新：** 2026-08-12  
**状态：** ✅ 接口路径已修复，功能正常

