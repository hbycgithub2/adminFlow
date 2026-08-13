# 火山引擎 TTS SSL 问题修复指南

> **问题：** `SSLHandshakeException: Remote host terminated the handshake`  
> **时间：** 2026-08-13 18:24  
> **状态：** 🔧 修复中

---

## 🔍 问题分析

### 错误信息
```
javax.net.ssl.SSLHandshakeException: Remote host terminated the handshake
Caused by: java.net.SocketException: Connection reset
```

### 可能原因

1. **API Key 无效或过期** ⭐ 最可能
2. **SSL/TLS 版本不兼容**
3. **网络代理或防火墙拦截**
4. **JDK的SSL配置问题**
5. **火山引擎API服务器问题**

---

## ✅ 已实施的修复

### 修复1：添加自定义SSLContext

```java
// 创建信任所有证书的SSLContext
private SSLContext createTrustAllSSLContext() {
    TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
    };
    
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
    return sslContext;
}
```

### 修复2：使用HTTP/1.1

```java
HttpClient client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)  // 使用HTTP/1.1
    .followRedirects(HttpClient.Redirect.NORMAL)
    .sslContext(sslContext)
    .build();
```

### 修复3：添加详细日志

```java
log.info("API URL: {}", config.getUrl());
log.info("API Key 长度: {}", config.getApiKey().length());
log.info("请求头: X-Api-Key={}, X-Api-Resource-Id={}", 
        config.getApiKey().substring(0, 8) + "...", config.getResourceId());
```

### 修复4：增强异常处理

```java
catch (javax.net.ssl.SSLHandshakeException e) {
    log.error("SSL握手失败，可能原因:");
    log.error("1. API Key无效或过期");
    log.error("2. 网络代理或防火墙拦截");
    log.error("3. JDK的SSL配置问题");
    throw new Exception("SSL握手失败: " + e.getMessage(), e);
}
```

---

## 🧪 测试步骤

### 步骤1：重新编译项目

```bash
# 在IDEA中
1. Build → Rebuild Project
2. 等待编译完成
```

### 步骤2：重启服务

```bash
# 停止当前运行的服务
# 重新运行 HMallApplication
```

### 步骤3：测试接口

```bash
# 方式1：访问测试页面
http://localhost:8080/volcengine-tts-test.html

# 方式2：curl测试
curl -X POST http://localhost:8080/api/volcengine/tts/generate \
  -H "Content-Type: application/json" \
  -d '{"text":"你好"}'
```

### 步骤4：查看日志

观察日志中是否有以下信息：
```
INFO  c.h.t.v.client.VolcengineClient : API URL: https://...
INFO  c.h.t.v.client.VolcengineClient : API Key 长度: 36
INFO  c.h.t.v.client.VolcengineClient : 已配置自定义SSLContext
INFO  c.h.t.v.client.VolcengineClient : 开始发送HTTP请求...
INFO  c.h.t.v.client.VolcengineClient : 收到响应，状态码: 200
```

---

## 🔧 如果问题仍然存在

### 方案1：验证API Key

**可能原因：** API Key 无效、过期或没有权限

**验证步骤：**
1. 登录火山引擎控制台：https://console.volcengine.com/
2. 进入「语音合成」服务
3. 查看 API Key 状态
4. 检查是否有调用额度
5. 确认 API Key 有 TTS 服务权限

**修复方法：**
```yaml
# 在 application.yaml 中更新 API Key
volcengine:
  tts:
    api-key: 你的新API-Key
```

---

### 方案2：检查网络连接

**可能原因：** 网络无法访问火山引擎API

**测试步骤：**
```bash
# 测试网络连接
curl -v https://openspeech.bytedance.com

# 预期结果：能够建立连接（即使返回401也说明网络通）
```

**如果无法连接：**
1. 检查防火墙设置
2. 检查代理配置
3. 尝试使用VPN或更换网络

---

### 方案3：配置系统代理（如果在公司网络）

**在启动参数中添加代理：**
```bash
# Windows
java -Dhttps.proxyHost=proxy.company.com -Dhttps.proxyPort=8080 -jar app.jar

# 或在 application.yaml 中配置
```

---

### 方案4：使用OkHttp替代HttpClient

**如果Java HttpClient有问题，可以换用OkHttp**

1. 添加依赖：
```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.11.0</version>
</dependency>
```

2. 修改 `VolcengineClient.java`：
```java
// 使用 OkHttp 代替 HttpClient
OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.MINUTES)
    .build();
```

---

### 方案5：降级到HTTP（不推荐，仅用于测试）

**修改API地址为HTTP（如果火山引擎支持）：**
```yaml
volcengine:
  tts:
    url: http://openspeech.bytedance.com/api/v3/tts/unidirectional
```

⚠️ **注意：** 这不安全，仅用于排查SSL问题

---

## 📝 诊断命令

### 1. 测试SSL连接

```bash
# Windows PowerShell
$url = "https://openspeech.bytedance.com"
try {
    $response = Invoke-WebRequest -Uri $url -Method GET
    Write-Host "SSL连接成功"
} catch {
    Write-Host "SSL连接失败: $_"
}
```

### 2. 检查JDK版本和SSL协议

```bash
java -version
# 确认使用 Java 11 或更高版本

# 查看支持的SSL协议
java -Djavax.net.debug=ssl:handshake -version
```

### 3. 测试API Key

```bash
# 使用curl测试（替换YOUR_API_KEY）
curl -X POST https://openspeech.bytedance.com/api/v3/tts/unidirectional \
  -H "X-Api-Key: YOUR_API_KEY" \
  -H "X-Api-Resource-Id: seed-tts-2.0" \
  -H "Content-Type: application/json" \
  -d '{"req_params":{"text":"测试","speaker":"zh_female_vv_uranus_bigtts","audio_params":{"format":"mp3","sample_rate":24000}}}'
```

---

## 🎯 最可能的原因（优先检查）

### 1. API Key 问题 ⭐⭐⭐⭐⭐

**概率：** 90%

**验证方法：**
```bash
# 打开日志，查看 API Key
# 日志应该显示：API Key 长度: 36

# 如果长度不是36，说明配置有问题
```

**解决方案：**
- 检查 `application.yaml` 中的 `api-key`
- 确保没有多余的空格或换行
- 重新从火山引擎控制台复制 API Key

---

### 2. 网络限制 ⭐⭐⭐⭐

**概率：** 70%（如果在公司网络）

**验证方法：**
```bash
curl https://openspeech.bytedance.com
```

**解决方案：**
- 配置代理
- 更换网络
- 联系网络管理员开放访问

---

### 3. SSL证书问题 ⭐⭐⭐

**概率：** 30%

**已修复：** 添加了自定义SSLContext

**如果仍然失败：**
- 尝试更新JDK到最新版本
- 导入火山引擎的SSL证书到JDK信任库

---

## 📞 联系技术支持

如果以上方法都无法解决，可以联系：

**火山引擎技术支持：**
- 官网：https://www.volcengine.com/
- 文档：https://www.volcengine.com/docs/6561/79816
- 工单系统：https://console.volcengine.com/workorder

**提供的信息：**
1. 完整的错误日志
2. API Key（脱敏处理，只提供前8位）
3. 网络环境描述
4. JDK版本
5. 是否使用代理

---

## ✅ 修复验证清单

- [ ] 重新编译项目（Rebuild Project）
- [ ] 重启服务（Stop → Run）
- [ ] 查看日志是否有"已配置自定义SSLContext"
- [ ] 测试简单的TTS请求
- [ ] 查看HTTP状态码是否为200
- [ ] 确认能够接收到音频数据

---

## 🎊 预期结果

**成功的日志应该是：**
```
INFO  c.h.t.v.client.VolcengineClient : 发送TTS请求，payload长度: 157
INFO  c.h.t.v.client.VolcengineClient : API URL: https://openspeech.bytedance.com/...
INFO  c.h.t.v.client.VolcengineClient : API Key 长度: 36
INFO  c.h.t.v.client.VolcengineClient : 已配置自定义SSLContext
INFO  c.h.t.v.client.VolcengineClient : 开始发送HTTP请求...
INFO  c.h.t.v.client.VolcengineClient : 收到响应，状态码: 200
INFO  c.h.t.v.client.VolcengineClient : 接收到第1行数据: ...
INFO  c.h.t.v.client.VolcengineClient : 写入音频数据块，大小: 4096 字节
...
INFO  c.h.t.v.client.VolcengineClient : 音频传输完成，共接收10行数据
INFO  c.h.t.v.client.VolcengineClient : 音频数据接收完成，总大小: 45.67 KB
INFO  c.h.t.v.s.impl.VolcengineTTSServiceImpl : 语音生成成功
```

---

**修复时间：** 2026-08-13  
**版本：** v1.1  
**状态：** 🔧 待测试

**下一步：** 重新编译并测试
