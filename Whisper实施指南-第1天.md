# Whisper Base实施指南 - 第1天（环境准备）

**开始时间：** 2026-08-14 19:15  
**预计完成：** 2026-08-14 20:00（45分钟）  
**目标：** 安装Python、Whisper、测试识别

---

## ✅ 第1步：安装Python（15分钟）

### 检查Python状态
```powershell
# 检查结果：系统未安装Python
python --version
# 输出：未找到命令
```

### 下载和安装Python

**1. 下载Python 3.11**
```
官方下载地址：
https://www.python.org/ftp/python/3.11.9/python-3.11.9-amd64.exe

或使用国内镜像（更快）：
https://npm.taobao.org/mirrors/python/3.11.9/python-3.11.9-amd64.exe

文件大小：约25MB
```

**2. 安装Python**
```
运行下载的安装包：python-3.11.9-amd64.exe

重要选项：
✅ 勾选 "Add Python 3.11 to PATH" （重要！）
✅ 选择 "Install Now"

安装位置（默认）：
C:\Users\你的用户名\AppData\Local\Programs\Python\Python311\
```

**3. 验证安装**
```powershell
# 关闭并重新打开PowerShell
python --version
# 应该输出：Python 3.11.9

pip --version
# 应该输出：pip 24.0 from ...
```

---

## ✅ 第2步：安装Whisper（10分钟）

### 安装openai-whisper

**使用国内镜像（推荐，速度快）：**
```powershell
# 使用清华源安装Whisper
pip install openai-whisper -i https://pypi.tuna.tsinghua.edu.cn/simple

# 安装FFmpeg的Python绑定
pip install ffmpeg-python -i https://pypi.tuna.tsinghua.edu.cn/simple
```

**如果国内镜像失败，使用官方源：**
```powershell
pip install openai-whisper
pip install ffmpeg-python
```

**预计下载大小：**
- openai-whisper：约5MB
- 依赖包（torch、numpy等）：约50MB
- 总计：约55MB

---

## ✅ 第3步：配置FFmpeg路径（5分钟）

Whisper需要FFmpeg来处理音频。你已经有FFmpeg了！

**添加环境变量：**
```
1. 右键"此电脑" → 属性 → 高级系统设置 → 环境变量
2. 在"系统变量"中找到"Path"，点击"编辑"
3. 点击"新建"，添加：
   D:\ai\codex\ffmpeg-9.0.1-essentials_build\bin
4. 点击"确定"保存
5. 重启PowerShell
```

**验证FFmpeg：**
```powershell
ffmpeg -version
# 应该输出：ffmpeg version 9.0.1 ...
```

---

## ✅ 第4步：下载Whisper base模型（10分钟）

### 自动下载（推荐）

**创建测试脚本：**
```powershell
# 创建测试目录
mkdir D:\code\adminFlow\whisper-test
cd D:\code\adminFlow\whisper-test

# 创建测试脚本
notepad test_whisper.py
```

**test_whisper.py内容：**
```python
import whisper

print("正在加载Whisper base模型...")
model = whisper.load_model("base")
print("模型加载成功！")

# 显示模型信息
print(f"模型位置：{model}")
print("Whisper base安装完成！")
```

**运行测试：**
```powershell
python test_whisper.py

# 首次运行会自动下载base模型（150MB）
# 输出类似：
# 正在加载Whisper base模型...
# Downloading: 100%|████████████████| 150MB/150MB [00:30<00:00, 5.00MB/s]
# 模型加载成功！
```

**模型下载位置：**
```
Windows默认位置：
C:\Users\你的用户名\.cache\whisper\

模型文件：
base.pt（150MB）
```

---

## ✅ 第5步：测试Whisper识别（5分钟）

### 准备测试音频

**生成测试音频：**
```powershell
# 使用火山引擎TTS生成一个测试音频
# 或使用现有的音频文件
# 放到：D:\code\adminFlow\whisper-test\test.mp3
```

**创建识别测试脚本：**
```python
# test_recognize.py
import whisper
import json

print("加载Whisper base模型...")
model = whisper.load_model("base")

print("识别音频...")
result = model.transcribe(
    "test.mp3", 
    language="zh",
    word_timestamps=True
)

print("\n识别结果：")
print(f"完整文本：{result['text']}")

print("\n逐字时间戳：")
for segment in result["segments"]:
    if "words" in segment:
        for word in segment["words"]:
            print(f"  {word['word']}: {word['start']:.3f}s - {word['end']:.3f}s")

print("\nWhisper识别测试成功！✅")
```

**运行测试：**
```powershell
python test_recognize.py

# 预期输出：
# 加载Whisper base模型...
# 识别音频...
# 识别结果：
# 完整文本：你来自哪里
#
# 逐字时间戳：
#   你: 0.000s - 0.240s
#   来: 0.240s - 0.480s
#   自: 0.480s - 0.720s
#   哪: 0.720s - 0.960s
#   里: 0.960s - 1.200s
#
# Whisper识别测试成功！✅
```

---

## 📋 第1天完成检查清单

- [ ] Python 3.11已安装
- [ ] pip可用
- [ ] FFmpeg路径已配置
- [ ] openai-whisper已安装
- [ ] Whisper base模型已下载（150MB）
- [ ] 测试识别成功

**如果全部完成，继续第2天的工作！**

---

## 🔧 故障排查

### 问题1：Python安装后找不到命令

**解决方案：**
```
1. 卸载Python
2. 重新安装，务必勾选"Add Python to PATH"
3. 重启电脑
```

### 问题2：pip下载很慢

**解决方案：**
```powershell
# 使用国内镜像
pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
```

### 问题3：Whisper模型下载失败

**解决方案：**
```
手动下载：
https://openaipublic.azureedge.net/main/whisper/models/base.pt

保存到：
C:\Users\你的用户名\.cache\whisper\base.pt
```

### 问题4：FFmpeg找不到

**解决方案：**
```
检查环境变量是否正确：
D:\ai\codex\ffmpeg-9.0.1-essentials_build\bin

重启PowerShell
```

---

## 📊 第1天总结

**完成内容：**
- ✅ Python 3.11环境
- ✅ Whisper库安装
- ✅ FFmpeg配置
- ✅ base模型下载（150MB）
- ✅ 测试识别成功

**空间使用：**
- Python：约100MB
- Whisper库：约50MB
- base模型：150MB
- 总计：约300MB

**下一步：**
明天开始第2天工作：Java集成开发

---

**第1天指南完成时间：** 2026-08-14 19:15  
**预计完成时间：** 45分钟  
**作者：** Kiro AI Assistant
