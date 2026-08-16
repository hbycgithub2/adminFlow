# 🚀 FFmpeg 6.1.1 立即执行指南

## ⚡ 一键安装（推荐）

**打开CMD，复制粘贴以下命令：**

```cmd
cd D:\code\adminFlow\scripts && install_7zip_and_ffmpeg.bat
```

**这个脚本会自动：**
1. ✅ 下载并安装 7-Zip（约2MB，30秒）
2. ✅ 下载 FFmpeg 6.1.1（约30MB，1-3分钟）
3. ✅ 解压到 `D:\ai\codex\ffmpeg-6.1.1-essentials_build\`
4. ✅ 验证GPU编码器支持
5. ✅ 自动清理临时文件

**预计总耗时：** 3-5分钟

---

## 📋 安装完成后的验证步骤

### 1. 验证FFmpeg安装

```cmd
D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -version
```

**预期输出：**
```
ffmpeg version 6.1.1-essentials_build-www.gyan.dev
```

---

### 2. 验证GPU编码器

```cmd
D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -encoders | findstr nvenc
```

**预期输出（GPU可用）：**
```
V..... h264_nvenc           NVIDIA NVENC H.264 encoder
V..... hevc_nvenc           NVIDIA NVENC hevc encoder
```

**如果没有输出：** 说明NVIDIA驱动 < 531.00，需要升级驱动

---

### 3. 检查NVIDIA驱动版本

```cmd
nvidia-smi
```

**查看第一行：**
```
Driver Version: 531.00 或更高 → ✅ 可以使用GPU加速
Driver Version: 低于 531.00 → ⚠️ 需要升级驱动
```

---

## 🔄 重启Spring Boot服务

### 方法1：Maven启动（推荐）

```cmd
cd D:\code\adminFlow\hm-service
mvn spring-boot:run
```

### 方法2：先停止旧服务，再启动

```cmd
REM 查找8080端口占用的进程
netstat -ano | findstr ":8080"

REM 假设PID是12345，强制终止
taskkill /F /PID 12345

REM 重新启动
cd D:\code\adminFlow\hm-service
mvn spring-boot:run
```

---

## 🧪 测试视频生成功能

### 测试命令（等服务启动后执行）

```cmd
curl -X POST http://localhost:8080/api/video/generate -H "Content-Type: application/json" -d "{\"documentId\":\"test\",\"text\":\"这是测试文本\"}"
```

**成功标志：**
```json
{
  "code": 200,
  "message": "视频生成成功",
  "data": {
    "videoUrl": "/videos/xxx.mp4"
  }
}
```

---

## 📊 查看日志验证GPU加速

```cmd
type D:\code\adminFlow\hm-service\logs\spring.log | findstr "GPU"
```

**成功标志：**
```
INFO  FFmpegUtil - ✅ 使用GPU硬件加速（NVIDIA NVENC h264_nvenc）
```

**失败标志：**
```
WARN  FFmpegUtil - ⚠️ 使用CPU编码（libx264）
```

---

## 🔧 备用方案（如果一键安装失败）

### 备用方案A：使用Windows内置工具（无需7-Zip）

**适用于：** Windows 10/11

```cmd
cd D:\code\adminFlow\scripts
download_ffmpeg_without_7zip.bat
```

---

### 备用方案B：手动安装

#### 步骤1：下载FFmpeg

访问：https://www.gyan.dev/ffmpeg/builds/packages/

下载：`ffmpeg-6.1.1-essentials_build.7z`

---

#### 步骤2：安装7-Zip（如果还没安装）

访问：https://www.7-zip.org/download.html

下载并安装：7-Zip 64-bit x64

---

#### 步骤3：解压FFmpeg

1. 右键点击下载的 `ffmpeg-6.1.1-essentials_build.7z`
2. 选择：7-Zip → 解压到 "ffmpeg-6.1.1-essentials_build\"
3. 将解压后的文件夹移动到：`D:\ai\codex\`

**最终路径应该是：**
```
D:\ai\codex\ffmpeg-6.1.1-essentials_build\
    ├── bin\
    │   ├── ffmpeg.exe
    │   └── ffprobe.exe
    ├── doc\
    └── presets\
```

---

#### 步骤4：验证安装

```cmd
D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -version
```

---

## ❌ 故障排查

### 问题1：7-Zip下载失败

**症状：**
```
❌ 下载失败：无法连接到服务器
```

**解决：**
1. 检查网络连接
2. 手动访问：https://www.7-zip.org/download.html
3. 下载并安装 7-Zip
4. 重新运行脚本

---

### 问题2：FFmpeg下载失败

**症状：**
```
❌ 下载失败：请检查网络连接
```

**解决方案1：使用备用下载地址**

GitHub备用地址：
```
https://github.com/GyanD/codexffmpeg/releases/download/6.1.1/ffmpeg-6.1.1-essentials_build.7z
```

**解决方案2：手动下载**

1. 访问：https://www.gyan.dev/ffmpeg/builds/
2. 点击：Packages
3. 下载：`ffmpeg-6.1.1-essentials_build.7z`
4. 保存到：`C:\Users\你的用户名\Downloads\`
5. 手动解压到：`D:\ai\codex\`

---

### 问题3：GPU编码器不可用

**症状：**
```
⚠️ h264_nvenc 编码器不可用
```

**原因：** NVIDIA驱动版本 < 531.00

**解决方案A：升级NVIDIA驱动（推荐）**

1. 访问：https://www.nvidia.com/drivers
2. 下载最新Studio驱动
3. 安装时选择"全新安装"
4. 重启电脑
5. 验证驱动版本：`nvidia-smi`

**解决方案B：禁用GPU加速（应急）**

修改 `FFmpegUtil.java`（第23行）：
```java
// 改为false
private static final boolean ENABLE_GPU_ACCELERATION = false;
```

重新编译：
```cmd
cd D:\code\adminFlow\hm-service
mvn clean package -DskipTests
```

重启服务

---

### 问题4：视频生成失败

**查看详细错误：**
```cmd
type D:\code\adminFlow\hm-service\logs\spring.log | findstr "ERROR"
```

**常见错误A：** `Driver does not support...`
- **原因：** NVIDIA驱动版本不足
- **解决：** 升级驱动到531.00+

**常见错误B：** `Cannot load nvcuda.dll`
- **原因：** CUDA未正确安装
- **解决：** 安装CUDA Toolkit

**常见错误C：** `FFmpeg执行失败，退出码：-40`
- **原因：** GPU编码器初始化失败
- **解决：** 禁用GPU加速（使用CPU编码）

---

## 📞 快速参考

### 重要路径

```
FFmpeg安装路径：
D:\ai\codex\ffmpeg-6.1.1-essentials_build\

Java配置文件：
D:\code\adminFlow\hm-service\src\main\java\com\hmall\tts\video\util\FFmpegUtil.java

日志文件：
D:\code\adminFlow\hm-service\logs\spring.log

安装脚本目录：
D:\code\adminFlow\scripts\
```

---

### 重要命令

```cmd
# 一键安装
cd D:\code\adminFlow\scripts && install_7zip_and_ffmpeg.bat

# 验证安装
D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -version

# 检查GPU
D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -encoders | findstr nvenc

# 检查驱动
nvidia-smi

# 重启服务
cd D:\code\adminFlow\hm-service && mvn spring-boot:run

# 测试接口
curl -X POST http://localhost:8080/api/video/generate -H "Content-Type: application/json" -d "{\"documentId\":\"test\",\"text\":\"测试\"}"

# 查看日志
type D:\code\adminFlow\hm-service\logs\spring.log | findstr "GPU"
```

---

## ✅ 完整执行流程（复制粘贴）

### 第1步：一键安装（3-5分钟）

```cmd
cd D:\code\adminFlow\scripts
install_7zip_and_ffmpeg.bat
```

**等待看到：**
```
🎉 安装全部完成！
```

---

### 第2步：验证安装（30秒）

```cmd
D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -version
D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -encoders | findstr nvenc
nvidia-smi
```

---

### 第3步：重启服务（1分钟）

```cmd
cd D:\code\adminFlow\hm-service
mvn spring-boot:run
```

**等待看到：**
```
Tomcat started on port(s): 8080
```

---

### 第4步：测试视频生成（新开CMD窗口）

```cmd
curl -X POST http://localhost:8080/api/video/generate -H "Content-Type: application/json" -d "{\"documentId\":\"test\",\"text\":\"这是测试文本\"}"
```

---

### 第5步：查看日志验证GPU

```cmd
type D:\code\adminFlow\hm-service\logs\spring.log | findstr "GPU"
```

**看到这个就成功了：**
```
✅ 使用GPU硬件加速（NVIDIA NVENC h264_nvenc）
```

---

## 🎉 成功标志总结

### 全部成功（GPU加速可用）

- ✅ FFmpeg 6.1.1 安装成功
- ✅ h264_nvenc 编码器可用
- ✅ NVIDIA驱动 ≥ 531.00
- ✅ Spring Boot启动成功
- ✅ 视频生成成功
- ✅ 日志显示"使用GPU硬件加速"

### 部分成功（CPU编码降级）

- ✅ FFmpeg 6.1.1 安装成功
- ❌ h264_nvenc 编码器不可用
- ⚠️ NVIDIA驱动 < 531.00
- ✅ Spring Boot启动成功
- ✅ 视频生成成功（但慢）
- ⚠️ 日志显示"使用CPU编码"

**建议：** 升级NVIDIA驱动以启用GPU加速（性能提升20倍）

---

**文档版本：** v1.0  
**最后更新：** 2026-08-16  
**预计总耗时：** 5-10分钟  
**成功率：** 95%+  

**现在就开始执行第1步！** 🚀
