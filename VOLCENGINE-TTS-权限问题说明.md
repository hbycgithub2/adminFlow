# 火山引擎 TTS API Key 权限问题说明

> **问题：** 女声和男声都报错 403  
> **错误码：** 45000030  
> **错误信息：** `[resource_id=volcano_tts] requested resource not granted`  
> **时间：** 2026-08-13 18:45  
> **状态：** ⚠️ API Key权限不足

---

## 🔍 问题分析

### 错误信息
```json
{
  "code": 45000030,
  "message": "[resource_id=volcano_tts] requested resource not granted"
}
```

### 问题原因

**当前API Key只有 `seed-tts-2.0` 的权限，没有 `volcano_tts` 的权限！**

从日志可以看出：
```
INFO : 音色: zh_female_vv_uranus_bigtts
INFO : 选择的 Resource ID: volcano_tts  ← 女声也被选成了volcano_tts（错误！）
ERROR: requested resource not granted  ← 没有权限
```

**这说明：**
1. 判断逻辑有问题（女声被误判为需要volcano_tts）
2. API Key没有volcano_tts权限

---

## 🐛 发现的Bug

### Bug1：女声也被选成了volcano_tts

**问题代码：**
```java
if (speaker != null && (
    speaker.contains("male") ||  // 男声
    speaker.contains("calm")     // 平静系列
)) {
    return resourceIdBigModel;  // volcano_tts
}
```

**问题分析：**
- `zh_female_vv_uranus_bigtts` 应该用 `seed-tts-2.0`
- 但它不包含"male"或"calm"
- 按理说应该返回 `resourceId` (seed-tts-2.0)
- 为什么日志显示选择了 `volcano_tts`？

**可能原因：** 配置加载有问题，或者有其他地方修改了逻辑

---

## ✅ 临时解决方案

### 方案：禁用自动选择，统一使用 seed-tts-2.0

**修改配置：**
```yaml
volcengine:
  tts:
    auto-select-resource-id: false  # ⭐ 关闭自动选择
    resource-id: seed-tts-2.0       # 所有音色都用这个
```

**效果：**
- 所有音色（女声+男声）都使用 `seed-tts-2.0`
- 可以使用晓晓（女声）
- 男声可能会失败（如果需要不同的Resource ID）

---

## 🔧 永久解决方案

### 方案1：获取volcano_tts权限（推荐）

**步骤：**
1. 登录火山引擎控制台：https://console.volcengine.com/
2. 进入「语音合成」服务
3. 申请开通 `volcano_tts` 资源权限
4. 等待审核通过
5. 修改配置：`auto-select-resource-id: true`

---

### 方案2：只使用女声（临时）

**修改测试页面，隐藏男声选项：**
```html
<!-- 只显示女声 -->
<div class="voice-card" data-voice="zh_female_vv_uranus_bigtts">
    <div class="voice-name">晓晓</div>
    <div class="voice-desc">温柔女声</div>
</div>
```

---

### 方案3：使用其他TTS服务

**考虑使用：**
- **Edge TTS**（已实现）：免费，音色多
- **阿里云TTS**：价格便宜
- **腾讯云TTS**：性价比高

---

## 📋 验证API Key权限

### 步骤1：查看API Key信息

登录火山引擎控制台，查看当前API Key的权限：
```
https://console.volcengine.com/speech/
```

### 步骤2：查看已开通的Resource ID

**检查清单：**
- [ ] seed-tts-2.0（基础版）
- [ ] volcano_tts（大模型版）
- [ ] 其他Resource ID

### 步骤3：测试可用的Resource ID

**使用curl测试：**
```bash
# 测试 seed-tts-2.0
curl -X POST https://openspeech.bytedance.com/api/v3/tts/unidirectional \
  -H "X-Api-Key: YOUR_API_KEY" \
  -H "X-Api-Resource-Id: seed-tts-2.0" \
  -H "Content-Type: application/json" \
  -d '{"req_params":{"text":"测试","speaker":"zh_female_vv_uranus_bigtts","audio_params":{"format":"mp3","sample_rate":24000}}}'

# 测试 volcano_tts
curl -X POST https://openspeech.bytedance.com/api/v3/tts/unidirectional \
  -H "X-Api-Key: YOUR_API_KEY" \
  -H "X-Api-Resource-Id: volcano_tts" \
  -H "Content-Type: application/json" \
  -d '{"req_params":{"text":"测试","speaker":"zh_male_vv_uranus_bigtts","audio_params":{"format":"mp3","sample_rate":24000}}}'
```

---

## 🎯 当前可行方案

### 立即可用：只使用 seed-tts-2.0

**配置：**
```yaml
auto-select-resource-id: false
resource-id: seed-tts-2.0
```

**可用音色：**
- ✅ 晓晓（女声-温柔）`zh_female_vv_uranus_bigtts`
- ⚠️ 云扬（男声-沉稳）`zh_male_vv_uranus_bigtts` - 可能失败
- ⚠️ 晓静（女声-平静）`zh_female_calm_uranus_bigtts` - 可能失败
- ⚠️ 云舒（男声-平静）`zh_male_calm_uranus_bigtts` - 可能失败

**测试步骤：**
1. 重启服务
2. 测试晓晓（女声）- 应该成功 ✅
3. 测试云扬（男声）- 可能失败 ⚠️

---

## 📊 Resource ID与音色对应关系（推测）

根据错误信息和原始demo，推测：

| Resource ID | 支持的音色 | API Key权限 |
|-------------|-----------|------------|
| seed-tts-2.0 | zh_female_vv_uranus_bigtts（晓晓） | ✅ 有权限 |
| volcano_tts | zh_male_vv_uranus_bigtts（云扬）<br>zh_female_calm_uranus_bigtts（晓静）<br>zh_male_calm_uranus_bigtts（云舒） | ❌ 无权限 |

**注意：** 这只是推测，具体需要查看火山引擎官方文档。

---

## 🔍 排查判断逻辑Bug

### 为什么女声也选择了volcano_tts？

**检查点1：配置是否正确加载**
```bash
# 查看启动日志
grep "volcengine" logs/*.log
grep "auto-select" logs/*.log
```

**检查点2：判断逻辑是否有问题**
```java
// 当前逻辑
if (speaker.contains("male") || speaker.contains("calm")) {
    return resourceIdBigModel;  // volcano_tts
}
return resourceId;  // seed-tts-2.0
```

**验证：**
- `"zh_female_vv_uranus_bigtts"`.contains("male") → **false**
- `"zh_female_vv_uranus_bigtts"`.contains("calm") → **false**
- 应该返回 `resourceId` (seed-tts-2.0)

**结论：** 逻辑没问题，可能是配置加载有问题。

---

## 🎯 下一步操作

### 立即执行（已完成）

1. ✅ 修改配置：`auto-select-resource-id: false`
2. ⏳ 重启服务
3. ⏳ 测试晓晓（女声）

### 后续任务

1. 联系火山引擎申请 `volcano_tts` 权限
2. 或者只使用女声音色
3. 或者切换到 Edge TTS

---

## 📞 火山引擎技术支持

**官方文档：** https://www.volcengine.com/docs/6561/79816  
**控制台：** https://console.volcengine.com/speech/  
**工单系统：** https://console.volcengine.com/workorder

**需要咨询的问题：**
1. 当前API Key支持哪些Resource ID？
2. 如何开通 volcano_tts 权限？
3. 不同音色需要使用哪个Resource ID？
4. 免费试用有哪些限制？

---

**问题总结：**
- ❌ API Key没有 `volcano_tts` 权限（403错误）
- ⚠️ 可能女声判断逻辑也有bug（需要进一步排查）
- ✅ 临时方案：关闭自动选择，统一用 `seed-tts-2.0`

**立即操作：** 重启服务，测试女声是否能正常工作

---

**时间：** 2026-08-13  
**状态：** ⚠️ 待重启测试
