# WhisperX 快速安装指南（解决安装慢问题）

## 🚀 问题原因

WhisperX 安装慢的原因：
1. **PyTorch 太大**：torch + torchaudio 约 200-300MB
2. **依赖多**：需要下载 30+ 个依赖包
3. **国外服务器慢**：PyPI 官方服务器在国外，下载速度慢

---

## ✅ 解决方案（3种方法）

### 方案1：使用国内镜像（推荐⭐）

**直接运行脚本（最简单）：**
```bash
# 在 CMD 中执行
cd D:\code\adminFlow\scripts
install_whisperx_fast.bat
```

**或手动执行命令：**
```bash
# 1. 升级 pip (使用清华镜像)
py -3.13 -m pip install --upgrade pip -i https://pypi.tuna.tsinghua.edu.cn/simple

# 2. 安装 PyTorch (CPU 版本，使用官方 CPU 源)
py -3.13 -m pip install torch torchaudio --index-url https://download.pytorch.org/whl/cpu

# 3. 安装 WhisperX (使用清华镜像)
py -3.13 -m pip install whisperx -i https://pypi.tuna.tsinghua.edu.cn/simple

# 4. 安装其他依赖
py -3.13 -m pip install ffmpeg-python -i https://pypi.tuna.tsinghua.edu.cn/simple
```

**预计时间：** 5-10 分钟（比原来快 3-5 倍）

---

### 方案2：使用阿里云镜像（备选）

如果清华镜像慢，换阿里云：
```bash
# 安装 PyTorch (官方源)
py -3.13 -m pip install torch torchaudio --index-url https://download.pytorch.org/whl/cpu

# 安装 WhisperX (阿里云镜像)
py -3.13 -m pip install whisperx -i https://mirrors.aliyun.com/pypi/simple/

# 安装其他依赖
py -3.13 -m pip install ffmpeg-python -i https://mirrors.aliyun.com/pypi/simple/
```

---

### 方案3：离线安装（最快⚡，需要提前下载）

**步骤1：在网络好的环境下载包**
```bash
# 在另一台机器或网络好的时候下载
py -3.13 -m pip download whisperx torch torchaudio -d D:\whisperx_packages
```

**步骤2：在目标机器离线安装**
```bash
# 从本地文件安装
py -3.13 -m pip install --no-index --find-links=D:\whisperx_packages whisperx torch torchaudio
```

---

## 🔍 验证安装

安装完成后，验证：
```bash
# 1. 检查 PyTorch
py -3.13 -c "import torch; print('PyTorch 版本:', torch.__version__)"

# 2. 检查 WhisperX
py -3.13 -c "import whisperx; print('WhisperX 版本:', whisperx.__version__)"

# 3. 检查所有包
py -3.13 -m pip list | findstr /i "torch whisperx ffmpeg"
```

**预期输出：**
```
PyTorch 版本: 2.5.0+cpu
WhisperX 版本: 3.1.1
torch               2.5.0+cpu
torchaudio          2.5.0+cpu
whisperx            3.1.1
ffmpeg-python       0.2.0
```

---

## ⚠️ 常见问题

### 问题1：清华镜像连接超时
**解决：** 换阿里云镜像或使用官方源
```bash
py -3.13 -m pip install whisperx -i https://mirrors.aliyun.com/pypi/simple/
```

### 问题2：PyTorch 下载慢
**原因：** PyTorch 约 200MB，需要从官方源下载  
**解决：** 使用 CPU 版本（比 CUDA 版小很多）
```bash
py -3.13 -m pip install torch torchaudio --index-url https://download.pytorch.org/whl/cpu
```

### 问题3：安装过程中断
**解决：** 清理缓存后重新安装
```bash
# 清理缓存
py -3.13 -m pip cache purge

# 重新安装
py -3.13 -m pip install whisperx -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 问题4：依赖冲突
**解决：** 创建虚拟环境（推荐）
```bash
# 创建虚拟环境
py -3.13 -m venv D:\code\adminFlow\venv

# 激活虚拟环境
D:\code\adminFlow\venv\Scripts\activate

# 在虚拟环境中安装
pip install torch torchaudio --index-url https://download.pytorch.org/whl/cpu
pip install whisperx -i https://pypi.tuna.tsinghua.edu.cn/simple
```

---

## 📊 速度对比

| 方案 | 下载速度 | 预计时间 | 推荐度 |
|------|---------|---------|--------|
| 官方源（PyPI） | 100-500 KB/s | 20-30分钟 | ⭐ |
| 清华镜像 | 1-5 MB/s | 5-10分钟 | ⭐⭐⭐⭐⭐ |
| 阿里云镜像 | 1-3 MB/s | 8-15分钟 | ⭐⭐⭐⭐ |
| 离线安装 | 本地速度 | 1-2分钟 | ⭐⭐⭐⭐⭐ |

---

## 🎯 推荐操作步骤

**最快方式（5分钟完成）：**

1. **打开 CMD，运行脚本：**
   ```bash
   cd D:\code\adminFlow\scripts
   install_whisperx_fast.bat
   ```

2. **等待安装完成**（约 5-10 分钟）

3. **验证安装：**
   ```bash
   py -3.13 -c "import whisperx; print('安装成功！')"
   ```

4. **如果验证成功，继续配置 application.yml**

---

## 📝 国内镜像源列表

```bash
# 清华大学
https://pypi.tuna.tsinghua.edu.cn/simple

# 阿里云
https://mirrors.aliyun.com/pypi/simple/

# 中科大
https://pypi.mirrors.ustc.edu.cn/simple/

# 豆瓣
https://pypi.douban.com/simple/

# 华为云
https://mirrors.huaweicloud.com/repository/pypi/simple/
```

**使用方法：**
```bash
# 临时使用
py -3.13 -m pip install whisperx -i https://pypi.tuna.tsinghua.edu.cn/simple

# 永久配置（不推荐，可能影响其他项目）
py -3.13 -m pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
```

---

## ✅ 下一步（安装完成后）

1. ✅ 验证 WhisperX 安装成功
2. 📝 配置 `application.yml`（添加 WhisperX 配置）
3. 🔧 修改 Java 代码支持 `py -3.13` 命令
4. 🚀 重启 Spring Boot 服务
5. 🧪 上传 Word 文档测试

---

**创建时间：** 2025-01-XX  
**适用版本：** Python 3.13.15 + WhisperX 3.1.1  
**系统要求：** Windows 10/11, 网络连接

