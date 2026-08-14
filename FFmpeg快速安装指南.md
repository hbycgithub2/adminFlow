# FFmpeg 快速安装指南

> **错误信息**：`Cannot run program "ffmpeg": CreateProcess error=2, 系统找不到指定的文件。`  
> **原因**：FFmpeg未安装或未添加到系统PATH

---

## 🚀 方案1：Chocolatey安装（最简单）

### 步骤1：检查Chocolatey是否已安装

```powershell
choco --version
```

如果未安装，先安装Chocolatey：

```powershell
# 以管理员身份运行PowerShell
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

### 步骤2：安装FFmpeg

```powershell
# 以管理员身份运行
choco install ffmpeg -y
```

### 步骤3：验证安装

```bash
# 重新打开命令行窗口
ffmpeg -version
```

**预期输出**：
```
ffmpeg version 6.1-essentials_build-www.gyan.dev
Copyright (c) 2000-2023 the FFmpeg developers
...
```

---

## 🔧 方案2：手动安装

### 步骤1：下载FFmpeg

**官方下载页面**：
- https://ffmpeg.org/download.html

**Windows推荐下载**：
- https://www.gyan.dev/ffmpeg/builds/
- 下载：`ffmpeg-release-essentials.zip`（约100MB）

### 步骤2：解压到固定目录

```
推荐位置：C:\ffmpeg\
```

解压后目录结构：
```
C:\ffmpeg\
├─ bin\
│  ├─ ffmpeg.exe
│  ├─ ffplay.exe
│  └─ ffprobe.exe
├─ doc\
└─ presets\
```

### 步骤3：添加到系统PATH

#### 方法A：图形界面

1. **打开环境变量设置**：
   ```
   此电脑 → 右键"属性" → 高级系统设置 → 环境变量
   ```

2. **编辑Path变量**：
   ```
   系统变量 → 找到"Path" → 编辑 → 新建
   ```

3. **添加FFmpeg路径**：
   ```
   C:\ffmpeg\bin
   ```

4. **确定保存**

#### 方法B：命令行（管理员）

```powershell
# 以管理员身份运行PowerShell
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\ffmpeg\bin", "Machine")
```

### 步骤4：验证安装

```bash
# ⚠️ 重新打开命令行窗口
ffmpeg -version
```

---

## ⚡ 方案3：临时解决（快速测试）

如果不想修改系统PATH，可以将ffmpeg.exe直接放到项目目录：

### 步骤1：下载便携版

从上面的链接下载并解压

### 步骤2：复制到项目

```bash
# 复制ffmpeg.exe到项目bin目录
copy C:\ffmpeg\bin\ffmpeg.exe d:\code\adminFlow\hm-service\bin\
```

### 步骤3：修改代码（临时）

修改 `FFmpegUtil.java`，使用相对路径：

```java
// 修改前
ProcessBuilder pb = new ProcessBuilder("ffmpeg", ...);

// 修改后
ProcessBuilder pb = new ProcessBuilder("./bin/ffmpeg.exe", ...);
```

**注意**：这只是临时方案，不推荐用于生产环境。

---

## ✅ 验证FFmpeg安装

### 检查版本

```bash
ffmpeg -version
```

### 测试基本功能

```bash
# 测试生成5秒空白视频
ffmpeg -f lavfi -i color=c=blue:s=1280x720:d=5 -y test.mp4
```

如果生成test.mp4文件，说明FFmpeg工作正常。

---

## 🔍 常见问题

### 问题1：choco命令不存在

**错误**：
```
choco : 无法将"choco"项识别为 cmdlet、函数、脚本文件或可运行程序的名称
```

**解决**：
1. 安装Chocolatey（见方案1）
2. 或使用手动安装（见方案2）

### 问题2：ffmpeg命令不存在（安装后）

**错误**：
```
ffmpeg : 无法将"ffmpeg"项识别为 cmdlet、函数、脚本文件或可运行程序的名称
```

**原因**：
- 未添加到PATH
- 或添加后未重启命令行

**解决**：
1. 检查PATH是否包含ffmpeg路径
   ```powershell
   $env:Path -split ';' | Select-String ffmpeg
   ```

2. 重新打开命令行窗口（必须）

3. 如果还不行，重启电脑

### 问题3：权限不足

**错误**：
```
Set-ExecutionPolicy : 对注册表项"HKEY_LOCAL_MACHINE\SOFTWARE\..."的访问被拒绝
```

**解决**：
1. 以管理员身份运行PowerShell
2. 右键PowerShell图标 → "以管理员身份运行"

### 问题4：下载速度慢

**解决**：
1. 使用国内镜像：
   - https://github.com/BtbN/FFmpeg-Builds/releases
   - 选择 `ffmpeg-master-latest-win64-gpl.zip`

2. 或使用下载工具（迅雷、IDM）下载

---

## 📝 安装完成后

### 1. 验证安装

```bash
ffmpeg -version
```

### 2. 重启Spring Boot服务

```bash
# 停止当前服务（Ctrl+C）
# 重新启动
mvn spring-boot:run
```

### 3. 重新测试视频生成

访问：http://localhost:8080/video-generator-test.html

---

## 🎯 推荐安装方式

| 方式 | 难度 | 时间 | 推荐度 |
|------|------|------|--------|
| Chocolatey | ⭐ 简单 | 5分钟 | ⭐⭐⭐⭐⭐ |
| 手动安装 | ⭐⭐ 中等 | 10分钟 | ⭐⭐⭐⭐ |
| 临时方案 | ⭐ 简单 | 3分钟 | ⭐⭐（仅测试） |

**建议**：优先使用Chocolatey，简单快速。

---

## 🔗 相关链接

- **FFmpeg官网**：https://ffmpeg.org/
- **Windows下载**：https://www.gyan.dev/ffmpeg/builds/
- **GitHub镜像**：https://github.com/BtbN/FFmpeg-Builds/releases
- **Chocolatey**：https://chocolatey.org/install

---

**安装完成后，重启服务即可正常使用视频生成功能！** 🎉

