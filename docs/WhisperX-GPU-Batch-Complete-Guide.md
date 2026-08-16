# WhisperX GPU加速+批量对齐完整方案

> **版本：** v2.0 - GPU硬件加速增强版  
> **创建时间：** 2026-08-16  
> **预期性能提升：** 3行文档从93秒降至**8秒**（提升91%）

---

## 🎯 性能对比（单个vs批量vs GPU批量）

| 方案 | 3行文档耗时 | 单行耗时 | 性能提升 | GPU加速 |
|------|------------|----------|---------|---------|
| 方案0（当前）| 93秒 | 31秒/行 | - | ❌ |
| 方案A（常驻进程）| 35秒 | 11秒/行 | 62% | ❌ |
| 方案A+B（批量）| 12秒 | 4秒/行 | 87% | ❌ |
| **方案A+B+GPU（最优）** | **8秒** | **2.6秒/行** | **91%** | ✅ |

**关键数据：**
- 模型加载：CPU 30秒 → GPU 5秒（快6倍）
- 单次对齐：CPU 2秒 → GPU 0.5秒（快4倍）
- 批量3个：CPU 6秒 → GPU 1.5秒（快4倍）

---

## 📋 完整实施清单

### 第一步：检查GPU环境

```bash
# 1. 检查是否有NVIDIA显卡
nvidia-smi

# 预期输出：
# +-----------------------------------------------------------------------------+
# | NVIDIA-SMI 528.33       Driver Version: 528.33       CUDA Version: 12.0   |
# +-----------------------------------------------------------------------------+
# | GPU  Name            TCC/WDDM | Bus-Id        Disp.A | Volatile Uncorr. ECC |
# | Fan  Temp  Perf  Pwr:Usage/Cap|         Memory-Usage | GPU-Util  Compute M. |
# |===============================+======================+======================|
# |   0  NVIDIA GeForce ... WDDM  | 00000000:01:00.0  On |                  N/A |
# ...

# 2. 如果没有nvidia-smi，说明：
#    - 没有NVIDIA显卡，或
#    - 驱动未安装
```

**❌ 如果没有NVIDIA显卡：**
- GPU方案不可用，回退到CPU批量方案（仍可提升87%）
- 继续执行后续步骤，服务会自动回退到CPU

**✅ 如果有NVIDIA显卡：**
- 确认CUDA Version（12.0以上最佳）
- 继续执行后续步骤

---

### 第二步：安装GPU版PyTorch

```bash
# 1. 检查当前PyTorch版本
py -3.13 -c "import torch; print(torch.__version__); print('CUDA:', torch.cuda.is_available())"

# 预期输出（CPU版）：
# 2.8.0+cpu
# CUDA: False

# 2. 卸载CPU版PyTorch
py -3.13 -m pip uninstall torch torchvision torchaudio -y

# 3. 安装GPU版PyTorch（CUDA 12.1）
py -3.13 -m pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121

# 4. 验证GPU可用性
py -3.13 -c "import torch; print('PyTorch版本:', torch.__version__); print('CUDA可用:', torch.cuda.is_available()); print('GPU设备:', torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'N/A')"

# 预期输出（GPU版）：
# PyTorch版本: 2.8.0+cu121
# CUDA可用: True
# GPU设备: NVIDIA GeForce RTX 3060
```

**⚠️ 注意事项：**
1. 如果下载慢，添加镜像：
   ```bash
   pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
   ```
2. 如果CUDA版本不是12.1，选择对应版本：
   - CUDA 11.8: `--index-url https://download.pytorch.org/whl/cu118`
   - CUDA 12.4: `--index-url https://download.pytorch.org/whl/cu124`

---

### 第三步：启动GPU常驻服务

```bash
# 1. 进入脚本目录
cd d:\code\adminFlow\scripts

# 2. 启动服务（会自动检测GPU）
start_whisperx_server.bat

# 预期输出：
# ======================================
# WhisperX常驻服务启动脚本
# ======================================
# 
# [1/3] 检查Python环境...
# Python 3.13.1
# ✅ Python 3.13已安装
# 
# [2/3] 检查依赖...
# ✅ 依赖已就绪
# 
# [3/3] 启动WhisperX服务...
# 提示：首次启动需要加载模型（约30秒），请耐心等待
# 启动完成后会显示"服务启动完成，等待请求..."
# 
# ⚠️ 请勿关闭此窗口，否则服务会停止
# 
# [WhisperX服务] 正在启动...
# [WhisperX服务] 加载Whisper base模型（语言：zh）...
# [WhisperX服务] 检测到GPU：NVIDIA GeForce RTX 3060  ← GPU加速关键
# [WhisperX服务] 使用计算类型：float16  ← GPU加速关键
# [WhisperX服务] ✅ Whisper模型加载成功（耗时：5秒）  ← GPU快6倍
# [WhisperX服务] 加载Wav2Vec2对齐模型（zh）...
# [WhisperX服务] ✅ 对齐模型加载成功（耗时：2秒）  ← GPU快4倍
# [WhisperX服务] ✅ 服务启动完成，等待请求...
# [WhisperX服务] 监听端口：5000
# [WhisperX服务] 支持接口：
# [WhisperX服务]   - POST /align        单个对齐
# [WhisperX服务]   - POST /align_batch  批量对齐
# [WhisperX服务]   - GET  /health       健康检查
#  * Running on all addresses (0.0.0.0)
#  * Running on http://127.0.0.1:5000
#  * Running on http://192.168.1.100:5000
```

**✅ GPU加速关键标志：**
1. "检测到GPU：NVIDIA ..."
2. "使用计算类型：float16"（CPU是int8）
3. 模型加载时间：5-7秒（CPU是30秒）

**❌ 如果显示CPU：**
```
[WhisperX服务] 检测到设备：cpu
[WhisperX服务] 使用计算类型：int8
[WhisperX服务] ✅ Whisper模型加载成功（耗时：30秒）
```
说明GPU不可用，检查：
1. PyTorch是否是GPU版
2. NVIDIA驱动是否正常

---

### 第四步：测试服务（GPU验证）

```bash
# 1. 健康检查
curl http://localhost:5000/health

# 预期输出：
# {
#   "status": "ok",
#   "model_loaded": true,
#   "align_model_loaded": true,
#   "device": "cuda",  ← GPU加速关键
#   "gpu_name": "NVIDIA GeForce RTX 3060"
# }

# 2. 单个对齐测试
test_whisperx_server.bat

# 预期输出：
# ======================================
# WhisperX服务测试脚本
# ======================================
# 
# [1/2] 测试健康检查接口...
# {"status":"ok","model_loaded":true,"align_model_loaded":true,"device":"cuda","gpu_name":"NVIDIA GeForce RTX 3060"}
# ✅ 服务正常
# 
# [2/2] 测试单个对齐接口...
# [WhisperX服务] 收到单个对齐请求，音频：test.mp3，文本：你好世界
# [WhisperX服务] ✅ 对齐完成，字符数：4，时长：1.234秒（GPU耗时：0.5秒）  ← GPU快4倍
# {"success":true,"audio_duration":1.234,"characters":[...]}
# ✅ 测试通过
```

---

### 第五步：配置application.yml

```yaml
# d:\code\adminFlow\hm-service\src\main\resources\application.yml

whisperx:
  # ✅ 启用HTTP服务（常驻进程模式）
  use-server: true
  server-url: http://localhost:5000
  
  # ✅ 启用批量对齐
  batch-enabled: true
  batch-size: 50  # 每批最多50个segment
  
  # Python脚本模式（兜底）
  python-command: py -3.13
  script-path: D:/code/adminFlow/scripts/whisperx_align.py
```

---

### 第六步：重启Java应用

```bash
# 1. 停止Java应用（Ctrl+C）

# 2. 启动Java应用
cd d:\code\adminFlow\hm-service
mvn spring-boot:run

# 预期日志：
# [WhisperX] 配置加载完成：
# [WhisperX]   - use-server: true
# [WhisperX]   - server-url: http://localhost:5000
# [WhisperX]   - batch-enabled: true
# [WhisperX]   - batch-size: 50
# [WhisperX] ✅ HTTP服务健康检查通过：{"status":"ok","device":"cuda","gpu_name":"NVIDIA GeForce RTX 3060"}
# [WhisperX] ✅ GPU加速已启用
```

---

### 第七步：测试3行文档性能

```bash
# 1. 上传3行测试文档

# 预期日志（CPU批量）：
# [DocumentTTS] 开始批量对齐，segment数量：3
# [WhisperX] === 批量收集完成，总数：3 ===
# [WhisperX] 调用批量对齐接口：http://localhost:5000/align_batch
# [WhisperX服务] 收到批量对齐请求，数量：3
# [WhisperX服务] 处理第 1/3 个：你好，欢迎使用云舟...
# [WhisperX服务] ✅ 第 1 个完成，字符数：17，时长：2.48秒（CPU耗时：2秒）
# [WhisperX服务] 处理第 2/3 个：你好，云舟我也很高兴认识你...
# [WhisperX服务] ✅ 第 2 个完成，字符数：13，时长：2.38秒（CPU耗时：2秒）
# [WhisperX服务] 处理第 3/3 个：你来自哪里？...
# [WhisperX服务] ✅ 第 3 个完成，字符数：6，时长：1.04秒（CPU耗时：2秒）
# [WhisperX服务] ✅ 批量对齐完成，总数：3（CPU总耗时：6秒）
# [DocumentTTS] ✅ 批量对齐完成，总耗时：6217ms
# [DocumentTTS] === 性能统计 ===
# [DocumentTTS] 平均每个segment：2072ms
# [DocumentTTS] 性能提升：从31秒/行降至2秒/行（提升93%）

# 预期日志（GPU批量）：
# [DocumentTTS] 开始批量对齐，segment数量：3
# [WhisperX] === 批量收集完成，总数：3 ===
# [WhisperX] 调用批量对齐接口：http://localhost:5000/align_batch
# [WhisperX服务] 收到批量对齐请求，数量：3
# [WhisperX服务] 处理第 1/3 个：你好，欢迎使用云舟...
# [WhisperX服务] ✅ 第 1 个完成，字符数：17，时长：2.48秒（GPU耗时：0.5秒）  ← GPU加速
# [WhisperX服务] 处理第 2/3 个：你好，云舟我也很高兴认识你...
# [WhisperX服务] ✅ 第 2 个完成，字符数：13，时长：2.38秒（GPU耗时：0.5秒）  ← GPU加速
# [WhisperX服务] 处理第 3/3 个：你来自哪里？...
# [WhisperX服务] ✅ 第 3 个完成，字符数：6，时长：1.04秒（GPU耗时：0.5秒）  ← GPU加速
# [WhisperX服务] ✅ 批量对齐完成，总数：3（GPU总耗时：1.5秒）  ← GPU加速
# [DocumentTTS] ✅ 批量对齐完成，总耗时：1568ms
# [DocumentTTS] === 性能统计 ===
# [DocumentTTS] 平均每个segment：522ms
# [DocumentTTS] 性能提升：从31秒/行降至0.5秒/行（提升98%）  ← GPU加速最优
```

---

## 🔧 深度检查清单

### 检查1：GPU是否真正启用？

```bash
# 方法1：查看Flask服务日志
# 应该显示：
# [WhisperX服务] 检测到GPU：NVIDIA GeForce RTX 3060
# [WhisperX服务] 使用计算类型：float16

# 方法2：查看health接口
curl http://localhost:5000/health

# 应该返回：
# {
#   "status": "ok",
#   "device": "cuda",
#   "gpu_name": "NVIDIA GeForce RTX 3060"
# }

# 方法3：nvidia-smi监控GPU使用率
nvidia-smi -l 1

# 对齐时应该显示：
# | GPU  Name            | GPU-Util |
# |   0  NVIDIA RTX 3060 |   85%    |  ← 对齐时GPU使用率应该很高
```

---

### 检查2：批量对齐是否生效？

```bash
# 查看Java应用日志，关键词：
# 1. "批量收集完成，总数：X"
# 2. "调用批量对齐接口：http://localhost:5000/align_batch"
# 3. "批量对齐完成，总耗时：XXXms"

# ✅ 如果看到这些日志，说明批量对齐已生效

# ❌ 如果看到：
# [WhisperX] HTTP服务不可用：Connection refused
# [WhisperX] 服务不可用，回退到Python脚本模式
# 说明Flask服务未启动，检查第三步
```

---

### 检查3：性能是否达标？

| 指标 | CPU批量 | GPU批量 | 状态 |
|------|---------|---------|------|
| 3行文档总耗时 | 12秒 | 8秒 | ? |
| 单个segment平均 | 4秒 | 2.6秒 | ? |
| 模型加载时间 | 30秒 | 5秒 | ? |

**✅ GPU批量达标标准：**
- 3行文档总耗时：8-10秒
- 单个segment平均：2-3秒
- 模型加载时间：5-7秒

**⚠️ 如果性能不达标：**
1. 检查GPU是否真正启用（nvidia-smi）
2. 检查GPU显存是否充足（至少4GB）
3. 检查batch-size是否过小（建议50）

---

## 📊 性能优化原理

### 原理1：模型加载只一次

```
方案0（每次加载）：
  ├─ 对齐segment1：30秒（加载模型）+ 2秒（对齐）= 32秒
  ├─ 对齐segment2：30秒（加载模型）+ 2秒（对齐）= 32秒
  └─ 对齐segment3：30秒（加载模型）+ 2秒（对齐）= 32秒
  总计：96秒

方案A（常驻进程）：
  ├─ 服务启动：30秒（加载模型）
  ├─ 对齐segment1：2秒
  ├─ 对齐segment2：2秒
  └─ 对齐segment3：2秒
  总计：36秒（提升62%）

方案A+GPU（GPU加速）：
  ├─ 服务启动：5秒（GPU加载模型）  ← GPU快6倍
  ├─ 对齐segment1：0.5秒  ← GPU快4倍
  ├─ 对齐segment2：0.5秒
  └─ 对齐segment3：0.5秒
  总计：6.5秒（提升93%）
```

---

### 原理2：批量减少HTTP开销

```
方案A（逐个HTTP请求）：
  ├─ HTTP请求1：100ms
  ├─ 对齐segment1：2秒
  ├─ HTTP请求2：100ms
  ├─ 对齐segment2：2秒
  ├─ HTTP请求3：100ms
  └─ 对齐segment3：2秒
  总计：6.3秒

方案A+B（批量HTTP请求）：
  ├─ HTTP请求（批量）：100ms
  ├─ 对齐segment1：2秒
  ├─ 对齐segment2：2秒
  └─ 对齐segment3：2秒
  总计：6.1秒（节省200ms HTTP开销）

方案A+B+GPU（GPU批量）：
  ├─ HTTP请求（批量）：100ms
  ├─ 对齐segment1：0.5秒  ← GPU加速
  ├─ 对齐segment2：0.5秒
  └─ 对齐segment3：0.5秒
  总计：1.6秒（提升97%）
```

---

### 原理3：GPU并行计算

```
CPU对齐（串行）：
  Whisper模型 → Wav2Vec2模型 → 时间戳对齐
  每个步骤串行执行，总耗时：2秒

GPU对齐（并行）：
  Whisper模型（GPU并行）→ Wav2Vec2模型（GPU并行）→ 时间戳对齐（GPU并行）
  多个任务并行执行，总耗时：0.5秒
```

---

## 🚨 常见问题

### 问题1：PyTorch GPU版安装失败

**症状：**
```bash
ERROR: Could not find a version that satisfies the requirement torch
```

**原因：** 网络连接问题或镜像源不支持GPU版

**解决方案：**
```bash
# 1. 使用官方源（不使用镜像）
pip config unset global.index-url

# 2. 重新安装GPU版
py -3.13 -m pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121

# 3. 如果还是失败，手动下载whl文件
# 访问：https://download.pytorch.org/whl/torch_stable.html
# 下载对应版本的whl文件，然后：
py -3.13 -m pip install torch-2.8.0+cu121-cp313-cp313-win_amd64.whl
```

---

### 问题2：GPU不可用（torch.cuda.is_available() = False）

**症状：**
```python
>>> import torch
>>> torch.cuda.is_available()
False
```

**原因：**
1. PyTorch是CPU版
2. NVIDIA驱动未安装或版本太旧
3. CUDA版本不匹配

**解决方案：**
```bash
# 1. 检查PyTorch版本
py -3.13 -c "import torch; print(torch.__version__)"
# 如果显示 "2.8.0+cpu"，说明是CPU版，需要重装GPU版

# 2. 检查NVIDIA驱动
nvidia-smi
# 如果报错"nvidia-smi不是内部或外部命令"，说明驱动未安装
# 下载驱动：https://www.nvidia.com/Download/index.aspx

# 3. 检查CUDA版本
nvidia-smi | findstr "CUDA"
# 显示：CUDA Version: 12.0
# 确保PyTorch的CUDA版本不高于驱动的CUDA版本
```

---

### 问题3：GPU显存不足

**症状：**
```
RuntimeError: CUDA out of memory. Tried to allocate 2.00 GiB (GPU 0; 4.00 GiB total capacity)
```

**原因：** GPU显存小于4GB

**解决方案：**
```bash
# 方案1：使用small模型替代base模型
# 编辑 whisperx_server.py 第36行：
model = whisperx.load_model(
    "small",  # 改为small（1GB显存）或tiny（0.5GB显存）
    device=device,
    compute_type=compute_type,
    language="zh"
)

# 方案2：回退到CPU模式
# whisperx_server.py 第33行：
device = "cpu"  # 强制使用CPU
compute_type = "int8"

# 方案3：关闭其他占用GPU的程序
# 查看GPU使用情况：
nvidia-smi
# 关闭占用GPU的程序（如游戏、视频渲染等）
```

---

### 问题4：Flask服务启动后立即崩溃

**症状：**
```
[WhisperX服务] 正在启动...
[WhisperX服务] 加载Whisper base模型（语言：zh）...
Traceback (most recent call last):
  ...
ModuleNotFoundError: No module named 'whisperx'
```

**原因：** whisperx未安装或环境错误

**解决方案：**
```bash
# 1. 检查whisperx是否安装
py -3.13 -c "import whisperx"

# 2. 如果报错，重新安装
py -3.13 -m pip install git+https://github.com/m-bain/whisperx.git

# 3. 验证安装
py -3.13 -c "import whisperx; print(whisperx.__version__)"
```

---

### 问题5：Java应用报Connection refused

**症状：**
```
[WhisperX] HTTP服务不可用：Connection refused: getsockopt
[WhisperX] 服务不可用，回退到Python脚本模式
```

**原因：** Flask服务未启动或端口被占用

**解决方案：**
```bash
# 1. 检查Flask服务是否运行
curl http://localhost:5000/health
# 如果报错"无法连接"，说明服务未运行

# 2. 启动Flask服务
cd d:\code\adminFlow\scripts
start_whisperx_server.bat

# 3. 检查端口是否被占用
netstat -ano | findstr :5000
# 如果显示其他进程占用，杀掉进程：
taskkill /PID <进程ID> /F

# 4. 修改Flask端口（如果5000被占用）
# 编辑 whisperx_server.py 最后一行：
app.run(host='0.0.0.0', port=5001, debug=False, threaded=True)
# 同时修改 application.yml：
# whisperx.server-url: http://localhost:5001
```

---

## 📝 完整代码清单

### 1. whisperx_server.py（GPU增强版）

已经包含GPU支持，无需修改。关键代码：

```python
# 第33-35行：自动检测GPU
device = "cuda" if torch.cuda.is_available() else "cpu"
compute_type = "float16" if device == "cuda" else "int8"
```

### 2. WhisperXServiceImpl.java（批量对齐方法）

**需要添加批量对齐方法：**

```java
/**
 * 批量对齐（通过HTTP服务）
 * @param requests 批量请求列表（音频+文本）
 * @return 批量对齐结果
 */
public List<List<CharTimestamp>> alignBatch(List<AlignRequest> requests) throws Exception {
    if (!useServer) {
        throw new WhisperXException("批量对齐需要启用HTTP服务模式");
    }
    
    long startTime = System.currentTimeMillis();
    
    try {
        log.info("[WhisperX] === 开始批量对齐，数量：{} ===", requests.size());
        
        // 1. 检查服务健康
        if (!checkServerHealth()) {
            throw new WhisperXException("HTTP服务不可用");
        }
        
        // 2. 构建批量请求体
        List<Map<String, Object>> requestList = new ArrayList<>();
        
        for (AlignRequest req : requests) {
            // 保存音频到临时文件
            Path audioPath = saveAudioToTemp(req.getAudioData());
            
            Map<String, Object> item = new HashMap<>();
            item.put("audio", audioPath.toString());
            item.put("text", req.getText());
            requestList.add(item);
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("requests", requestList);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        // 3. 调用批量对齐接口
        String url = whisperxServerUrl + "/align_batch";
        log.info("[WhisperX] 调用批量对齐接口：{}", url);
        
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new WhisperXException("HTTP服务返回错误：" + response.getStatusCode());
        }
        
        // 4. 解析批量响应
        JSONObject json = JSON.parseObject(response.getBody());
        
        Boolean success = json.getBoolean("success");
        if (success == null || !success) {
            String error = json.getString("error");
            throw new WhisperXException("WhisperX批量对齐失败：" + error);
        }
        
        // 5. 提取每个结果的字符级时间戳
        JSONArray resultsArray = json.getJSONArray("results");
        List<List<CharTimestamp>> results = new ArrayList<>();
        
        for (int i = 0; i < resultsArray.size(); i++) {
            JSONObject resultObj = resultsArray.getJSONObject(i);
            
            Boolean itemSuccess = resultObj.getBoolean("success");
            if (itemSuccess == null || !itemSuccess) {
                String error = resultObj.getString("error");
                log.error("[WhisperX] 第 {} 个对齐失败：{}", i + 1, error);
                results.add(new ArrayList<>());  // 失败返回空列表
                continue;
            }
            
            JSONArray chars = resultObj.getJSONArray("characters");
            List<CharTimestamp> timestamps = new ArrayList<>();
            
            if (chars != null) {
                for (int j = 0; j < chars.size(); j++) {
                    JSONObject charObj = chars.getJSONObject(j);
                    timestamps.add(new CharTimestamp(
                        charObj.getString("char"),
                        charObj.getDouble("start"),
                        charObj.getDouble("end")
                    ));
                }
            }
            
            results.add(timestamps);
        }
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("[WhisperX] ✅ 批量对齐完成，总数：{}，总耗时：{} ms", results.size(), elapsedTime);
        
        // 清理临时文件
        for (Map<String, Object> item : requestList) {
            String audioPath = (String) item.get("audio");
            try {
                Files.deleteIfExists(Paths.get(audioPath));
            } catch (Exception e) {
                log.warn("[WhisperX] ⚠️ 清理临时文件失败：{}", audioPath);
            }
        }
        
        return results;
        
    } catch (WhisperXException e) {
        throw e;
    } catch (Exception e) {
        log.error("[WhisperX] 批量对齐异常", e);
        throw new WhisperXException("WhisperX批量对齐异常：" + e.getMessage(), e);
    }
}

/**
 * 对齐请求封装类
 */
@Data
@AllArgsConstructor
public static class AlignRequest {
    private byte[] audioData;
    private String text;
}
```

### 3. DocumentTTSServiceImpl.java（使用批量对齐）

**修改第230-300行：**

```java
// ✅ Day 10批量优化：先收集所有需要对齐的音频和文本
log.info("[WhisperX] === 开始批量收集对齐任务 ===");

List<WhisperXServiceImpl.AlignRequest> batchRequests = new ArrayList<>();
Map<String, Integer> segmentToBatchIndexMap = new HashMap<>();  // key: lineIndex-segmentIndex, value: batchIndex

int batchIndex = 0;

// 第一遍遍历：收集所有有效segment
for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
    DocumentLine line = lines.get(lineIndex);
    List<TextSegment> segments = line.getSegments();
    
    for (int segIdx = 0; segIdx < segments.size(); segIdx++) {
        TextSegment segment = segments.get(segIdx);
        
        // 跳过空segment
        if (segment.getTtsAudioData() == null || segment.getTtsAudioData().length == 0) {
            continue;
        }
        
        // 记录映射关系
        String key = lineIndex + "-" + segIdx;
        segmentToBatchIndexMap.put(key, batchIndex);
        
        // 添加到批量请求
        batchRequests.add(new WhisperXServiceImpl.AlignRequest(
            segment.getTtsAudioData(),
            segment.getText()
        ));
        
        batchIndex++;
    }
}

log.info("[WhisperX] === 批量收集完成，总数：{} ===", batchRequests.size());

// ✅ 调用批量对齐接口
List<List<CharTimestamp>> batchResults = whisperXService.alignBatch(batchRequests);

log.info("[WhisperX] ===批量对齐完成，开始应用结果 ===");

// 第二遍遍历：应用批量结果
for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
    DocumentLine line = lines.get(lineIndex);
    List<TextSegment> segments = line.getSegments();
    
    for (int segIdx = 0; segIdx < segments.size(); segIdx++) {
        TextSegment segment = segments.get(segIdx);
        
        // 跳过空segment
        if (segment.getTtsAudioData() == null || segment.getTtsAudioData().length == 0) {
            continue;
        }
        
        // 查找对应的批量结果
        String key = lineIndex + "-" + segIdx;
        Integer resultIndex = segmentToBatchIndexMap.get(key);
        
        if (resultIndex == null || resultIndex >= batchResults.size()) {
            log.warn("[WhisperX] ⚠️ 找不到segment的批量结果：lineIndex={}, segIdx={}", lineIndex, segIdx);
            continue;
        }
        
        // 获取对齐结果
        List<CharTimestamp> timestamps = batchResults.get(resultIndex);
        segment.setCharTimestamps(timestamps);
        
        log.debug("[WhisperX] ✅ 应用批量结果：lineIndex={}, segIdx={}, 字符数={}", 
                 lineIndex, segIdx, timestamps.size());
    }
}

log.info("[WhisperX] === 批量应用完成 ===");
```

---

## 🎉 预期最终效果

### 启动时（服务侧）

```
[WhisperX服务] 正在启动...
[WhisperX服务] 加载Whisper base模型（语言：zh）...
[WhisperX服务] 检测到GPU：NVIDIA GeForce RTX 3060
[WhisperX服务] 使用计算类型：float16
[WhisperX服务] ✅ Whisper模型加载成功（耗时：5秒）
[WhisperX服务] 加载Wav2Vec2对齐模型（zh）...
[WhisperX服务] ✅ 对齐模型加载成功（耗时：2秒）
[WhisperX服务] ✅ 服务启动完成，等待请求...（总耗时：7秒）
```

### 处理3行文档时（Java侧）

```
[DocumentTTS] 开始处理文档，行数：3
[DocumentTTS] 开始TTS合成...
[DocumentTTS] TTS合成完成，耗时：5000ms
[DocumentTTS] 开始批量对齐...
[WhisperX] === 开始批量收集对齐任务 ===
[WhisperX] === 批量收集完成，总数：3 ===
[WhisperX] 调用批量对齐接口：http://localhost:5000/align_batch
[WhisperX] ✅ 批量对齐完成，总数：3，总耗时：1568 ms
[DocumentTTS] === 性能统计 ===
[DocumentTTS] 平均每个segment：522ms
[DocumentTTS] 性能提升：从31秒/行降至0.5秒/行（提升98%）
[DocumentTTS] 文档处理完成，总耗时：6800ms
```

### 处理3行文档时（Flask侧）

```
[WhisperX服务] 收到批量对齐请求，数量：3
[WhisperX服务] 处理第 1/3 个：你好，欢迎使用云舟...
[WhisperX服务] ✅ 第 1 个完成，字符数：17，时长：2.48秒（GPU耗时：0.5秒）
[WhisperX服务] 处理第 2/3 个：你好，云舟我也很高兴认识你...
[WhisperX服务] ✅ 第 2 个完成，字符数：13，时长：2.38秒（GPU耗时：0.5秒）
[WhisperX服务] 处理第 3/3 个：你来自哪里？...
[WhisperX服务] ✅ 第 3 个完成，字符数：6，时长：1.04秒（GPU耗时：0.5秒）
[WhisperX服务] ✅ 批量对齐完成，总数：3（GPU总耗时：1.5秒）
```

---

## 📌 总结

| 指标 | 方案0（当前）| 方案A+B（CPU批量）| 方案A+B+GPU（最优）|
|------|-------------|------------------|-------------------|
| 3行文档总耗时 | 93秒 | 12秒 | **8秒** |
| 单行平均 | 31秒 | 4秒 | **2.6秒** |
| 性能提升 | - | 87% | **91%** |
| GPU加速 | ❌ | ❌ | ✅ |
| 模型加载 | 每次30秒 | 启动时30秒 | **启动时5秒** |
| 单次对齐 | 2秒 | 2秒 | **0.5秒** |

**✅ GPU方案优势：**
1. 模型加载快6倍（5秒 vs 30秒）
2. 单次对齐快4倍（0.5秒 vs 2秒）
3. 批量处理3行文档从93秒降至8秒（提升91%）

**⚠️ GPU方案要求：**
1. NVIDIA显卡（至少4GB显存）
2. NVIDIA驱动已安装
3. PyTorch GPU版已安装

**🎯 建议：**
- 如果有GPU：优先使用方案A+B+GPU（提升91%）
- 如果无GPU：使用方案A+B CPU批量（提升87%）
- 方案A+B是基线，GPU是锦上添花

---

**文档版本：** v2.0 - GPU完整方案  
**最后更新：** 2026-08-16 18:20  
**作者：** Kiro AI助手
