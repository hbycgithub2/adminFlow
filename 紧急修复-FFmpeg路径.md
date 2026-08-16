# 🚨 紧急问题：Whisper找不到FFmpeg

## 问题分析

### 日志显示
```
[Whisper] JSON结果：{"success": false, "error": "[WinError 2] 系统找不到指定的文件。"}
```

### 根本原因
Whisper库依赖FFmpeg来处理音频文件，但Python进程找不到FFmpeg可执行文件。

### 为什么会这样？
1. Java服务启动时，Python继承了Java的环境变量
2. Java的PATH中可能没有FFmpeg
3. Whisper调用FFmpeg时失败：`[WinError 2] 系统找不到指定的文件`

---

## ✅ 解决方案

### 方案1：设置系统环境变量PATH（推荐）

1. **打开环境变量设置**：
   - 右键"此电脑" → 属性 → 高级系统设置 → 环境变量

2. **编辑PATH变量**：
   - 在"系统变量"中找到`Path`
   - 点击"编辑"
   - 点击"新建"
   - 添加：`D:\ai\codex\ffmpeg-9.0.1-essentials_build\bin`
   - 点击"确定"

3. **重启IDEA和服务**：
   - 完全关闭IDEA
   - 重新打开IDEA
   - 启动服务
   - 重新测试

---

### 方案2：在application.yaml中配置FFmpeg路径（快速）

修改配置文件，让Whisper知道FFmpeg的位置：

**文件**：`d:\code\adminFlow\hm-service\src\main\resources\application.yaml`

**添加配置**：
```yaml
whisper:
  python:
    command: py
  script:
    path: D:/code/adminFlow/scripts/whisper_transcribe.py
  temp:
    dir: D:/code/adminFlow/temp/whisper
  timeout:
    seconds: 60
  ffmpeg:
    path: D:/ai/codex/ffmpeg-9.0.1-essentials_build/bin/ffmpeg.exe  # 新增
```

然后修改Python脚本，在调用Whisper前设置环境变量。

---

### 方案3：修改Python脚本，显式指定FFmpeg路径（最快）

修改`whisper_transcribe.py`，在import whisper之前设置FFmpeg路径：

```python
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import whisper
import json
import sys
import os

# ✅ 新增：设置FFmpeg路径
os.environ['PATH'] = r'D:\ai\codex\ffmpeg-9.0.1-essentials_build\bin' + os.pathsep + os.environ.get('PATH', '')

def transcribe_audio(audio_path):
    # ... 后续代码不变
```

---

## 🎯 推荐方案：方案3（最快，立即生效）

修改Python脚本第8行之后添加FFmpeg路径设置。

---

## 📝 验证步骤

修复后，手动测试：

```bash
# 1. 手动测试Whisper脚本
py D:\code\adminFlow\scripts\whisper_transcribe.py D:\code\adminFlow\temp\whisper\[某个mp3文件]

# 2. 应该看到成功输出
{"success": true, "text": "...", "words": [...]}

# 3. 如果仍然失败，检查FFmpeg
D:\ai\codex\ffmpeg-9.0.1-essentials_build\bin\ffmpeg.exe -version
```

---

## 🔍 为什么之前手动测试成功？

你之前手动测试Whisper脚本是成功的，因为：
1. 你的PowerShell会话可能有正确的PATH
2. 或者Whisper首次运行下载了模型，但没有报错

但在Java服务中：
1. Java进程的环境变量与你的PowerShell不同
2. Python继承了Java的环境变量
3. 没有FFmpeg在PATH中

---

## ⚡ 立即修复

我现在帮你修改Python脚本（方案3）：
