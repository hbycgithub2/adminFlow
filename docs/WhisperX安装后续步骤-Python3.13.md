# WhisperX安装后续步骤 - Python 3.13

> **当前状态：** Python 3.13.15 已安装成功 ✅  
> **安装路径：** D:\Program Files\Python313\  
> **Python命令：** `py -3.13`

---

## ✅ 已完成的步骤

1. **Python 3.13.15 安装成功**
   - 路径：`D:\Program Files\Python313\`
   - 验证：`py -3.13 --version` 返回 `Python 3.13.15`

2. **pip 已升级**
   - 版本：26.2.1

3. **PyTorch安装成功** ✅
   - torch 2.13.0+cpu
   - torchaudio 2.11.0+cpu

4. **WhisperX正在安装中** (进行中)
   - 当前进度：下载 whisperx-3.8.6-py3-none-any.whl (16.5 MB) - 100%完成
   - 正在下载：torch-2.8.0-cp313-cp313-win_amd64.whl (241.3 MB) - 约10%完成
   - **问题：** 下载速度较慢（约400 KB/s），预计还需8-10分钟

---

## 🚀 下一步操作（让安装继续完成）

### 方案一：等待当前安装完成（推荐）

**当前命令还在后台运行：**
```bash
py -3.13 -m pip install whisperx
```

**预计完成时间：** 8-10分钟

**完成后的验证命令：**
```bash
py -3.13 -c "import whisperx; print('✅ WhisperX版本:', whisperx.__version__)"
py -3.13 -c "import torch; print('✅ PyTorch版本:', torch.__version__)"
```

**成功标志：**
```
✅ WhisperX版本: 3.8.6
✅ PyTorch版本: 2.8.0
```

---

### 方案二：如果安装失败或中断，手动重新安装

#### 2.1 检查是否已安装
```bash
py -3.13 -c "import whisperx; print(whisperx.__version__)"
```

如果显示错误，说明未安装成功，继续下面步骤。

#### 2.2 清理并重新安装
```bash
# 清理缓存
py -3.13 -m pip cache purge

# 重新安装 WhisperX（使用国内镜像加速）
py -3.13 -m pip install whisperx -i https://pypi.tuna.tsinghua.edu.cn/simple
```

#### 2.3 如果仍然失败，分步安装
```bash
# 1. 安装 PyTorch（已完成）
py -3.13 -m pip list | findstr torch

# 2. 手动安装 WhisperX 的依赖
py -3.13 -m pip install ctranslate2 faster-whisper nltk numpy omegaconf pandas

# 3. 最后安装 WhisperX
py -3.13 -m pip install whisperx==3.8.6
```

---

## 🔧 安装完成后的配置步骤

### 步骤1：验证WhisperX安装
```bash
py -3.13 D:\code\adminFlow\scripts\whisperx_align.py
```

**预期输出：**
```json
{"success": false, "error": "缺少参数：需要提供音频文件路径和原文"}
```

**说明：** 参数缺失错误是正常的，说明脚本可以正常调用。

---

### 步骤2：配置Java项目

#### 2.1 检查配置文件
打开：`D:\code\adminFlow\hm-service\src\main\resources\application.yml`

检查是否有以下配置：
```yaml
whisperx:
  python:
    command: py -3.13  # Python 3.13命令
  script:
    path: D:/code/adminFlow/scripts/whisperx_align.py
  temp:
    dir: D:/code/adminFlow/temp/whisperx
  timeout:
    seconds: 120
```

#### 2.2 如果没有配置，手动添加
在 `application.yml` 文件末尾追加：
```yaml

# WhisperX配置（Python 3.13）
whisperx:
  python:
    command: py -3.13
  script:
    path: D:/code/adminFlow/scripts/whisperx_align.py
  temp:
    dir: D:/code/adminFlow/temp/whisperx
  timeout:
    seconds: 120
```

---

### 步骤3：修改Java代码以支持 `py -3.13` 命令

当前Java代码会自动检测 `python313` 命令，但你的系统使用的是 `py -3.13`。

#### 3.1 方案A：修改Java代码（推荐）

打开：`D:\code\adminFlow\hm-service\src\main\java\com\hmall\tts\whisperx\service\impl\WhisperXServiceImpl.java`

在 `detectPython313()` 方法中，在第2步之前添加检测 `py -3.13` 的逻辑：

找到这一行：
```java
// 2. 尝试python313命令
```

在它之前添加：
```java
        // 1.5. 尝试 py -3.13（Windows Python Launcher）
        if (testPythonCommand("py", "-3.13", "--version")) {
            log.info("[WhisperX] ✅ 检测到py -3.13");
            // 需要特殊处理：返回"py"并修改调用方式
            // 暂时返回完整命令字符串，但需要修改 ProcessBuilder 调用
            return "py";
        }
```

**注意：** 这需要进一步修改 ProcessBuilder 的调用方式，因为 `py -3.13` 是两个参数。

#### 3.2 方案B：直接在application.yml配置（更简单）

不修改Java代码，直接在 `application.yml` 中指定完整路径：
```yaml
whisperx:
  python:
    command: D:/Program Files/Python313/python.exe
```

或者创建一个批处理文件：

创建文件：`D:\code\adminFlow\scripts\python313.bat`
```bat
@echo off
"D:\Program Files\Python313\python.exe" %*
```

然后配置：
```yaml
whisperx:
  python:
    command: D:/code/adminFlow/scripts/python313.bat
```

---

### 步骤4：重启Spring Boot服务

1. 停止当前服务（如果正在运行）
2. 重新启动服务
3. 查看日志，确认WhisperX可用

**成功日志：**
```
[WhisperX] 自动检测Python命令：D:/Program Files/Python313/python.exe
[WhisperX] 服务可用（Python: D:/Program Files/Python313/python.exe）
```

---

### 步骤5：测试WhisperX

上传Word文档到TTS接口，查看日志：

**成功标志：**
```
[WhisperX] 开始强制对齐，音频大小：XXX KB，文本长度：XXX
[WhisperX] 音频已保存到：D:\code\adminFlow\temp\whisperx\xxx.mp3
[WhisperX日志] [SSL配置] ssl._create_default_https_context已设置
[WhisperX日志] [WhisperX] 使用设备：cpu
[WhisperX日志] [WhisperX] ✅ Whisper模型加载成功
[WhisperX日志] [WhisperX] ✅ 对齐模型加载成功
[WhisperX] ✅ 对齐完成，字符数：XXX，准确率：98-99%，音频时长：X.XX秒，耗时：XXXX ms
```

---

## 📋 验证清单

完成所有步骤后，验证以下项目：

- [ ] Python 3.13可用：`py -3.13 --version`
- [ ] WhisperX已安装：`py -3.13 -c "import whisperx; print(whisperx.__version__)"`
- [ ] PyTorch已安装：`py -3.13 -c "import torch; print(torch.__version__)"`
- [ ] Python脚本可调用：`py -3.13 D:\code\adminFlow\scripts\whisperx_align.py`
- [ ] application.yml已配置
- [ ] Java服务启动成功
- [ ] 日志显示"WhisperX服务可用"
- [ ] 上传Word文档测试成功
- [ ] 日志显示"✅ 对齐完成，准确率：98-99%"
- [ ] 返回JSON包含charTimings字段

---

## ❌ 常见问题

### Q1：`py -3.13` 命令不工作
**A：** 使用完整路径：
```bash
"D:\Program Files\Python313\python.exe" --version
```

### Q2：WhisperX导入失败
**A：** 检查安装状态：
```bash
py -3.13 -m pip list | findstr whisperx
```

如果没有输出，重新安装：
```bash
py -3.13 -m pip install whisperx
```

### Q3：Java服务找不到Python
**A：** 在 `application.yml` 中配置完整路径：
```yaml
whisperx:
  python:
    command: D:/Program Files/Python313/python.exe
```

### Q4：模型下载失败（SSL错误）
**A：** Python脚本已内置SSL修复，但如果仍有问题：
```bash
# 设置环境变量
set HTTPX_VERIFY=false
set HF_HUB_DISABLE_SSL_VERIFY=1

# 重新运行
py -3.13 D:\code\adminFlow\scripts\whisperx_align.py
```

---

## 📞 下一步

**当前任务：**
1. 等待WhisperX安装完成（约8-10分钟）
2. 验证安装：`py -3.13 -c "import whisperx; print(whisperx.__version__)"`
3. 配置application.yml
4. 重启Java服务
5. 测试上传Word文档

**如果遇到问题：**
- 查看上面的"常见问题"部分
- 或提供具体的错误信息

---

**文档版本：** v1.0  
**更新时间：** 2026-08-16  
**当前状态：** WhisperX安装进行中（约10%完成）
