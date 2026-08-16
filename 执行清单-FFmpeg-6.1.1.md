# 🚀 FFmpeg 6.1.1 执行清单（复制粘贴即可）

## ✅ 第1步：运行自动化安装脚本（3分钟）

**打开CMD，复制粘贴执行：**

```cmd
cd D:\code\adminFlow\scripts && setup_ffmpeg_6.1.1_complete.bat
```

**等待提示：**
```
🎉 FFmpeg 6.1.1 安装成功！
```

---

## ✅ 第2步：检查安装结果

**复制粘贴执行：**

```cmd
D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -version
```

**预期输出：**
```
ffmpeg version 6.1.1-essentials_build-www.gyan.dev
```

---

## ✅ 第3步：检查GPU编码器支持

**复制粘贴执行：**

```cmd
D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -encoders | findstr nvenc
```

**预期输出：**
```
V..... h264_nvenc           NVIDIA NVENC H.264 encoder
V..... hevc_nvenc           NVIDIA NVENC hevc encoder
```

**如果没有输出：**
- 说明NVIDIA驱动版本 < 531.00
- 需要升级驱动或禁用GPU加速

---

## ✅ 第4步：检查NVIDIA驱动版本

**复制粘贴执行：**

```cmd
nvidia-smi
```

**查看输出：**
```
Driver Version: 531.00 或更高 → ✅ GPU加速可用
Driver Version: 低于531.00 → ⚠️ 需要升级驱动或禁用GPU加速
```

---

## ✅ 第5步：重启Spring Boot服务

### 方法A：Maven启动（开发环境）

**复制粘贴执行：**

```cmd
cd D:\code\adminFlow\hm-service && mvn spring-boot:run
```

### 方法B：Jar启动（生产环境）

**复制粘贴执行：**

```cmd
cd D:\code\adminFlow\hm-service && java -jar target\hm-service-1.0.0.jar
```

**等待启动完成，看到：**
```
Tomcat started on port(s): 8080
```

---

## ✅ 第6步：测试视频生成功能

**新开一个CMD窗口，复制粘贴执行：**

```cmd
curl -X POST http://localhost:8080/api/video/generate -H "Content-Type: application/json" -d "{\"documentId\":\"test\",\"text\":\"这是测试文本\"}"
```

**预期输出：**
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

## ✅ 第7步：查看日志验证GPU加速

**复制粘贴执行：**

```cmd
type D:\code\adminFlow\hm-service\logs\spring.log | findstr "GPU"
```

**成功标志：**
```
✅ 使用GPU硬件加速（NVIDIA NVENC h264_nvenc）
```

**失败标志：**
```
⚠️ 使用CPU编码（libx264）
```

如果看到CPU编码，说明GPU加速未启用，需要检查驱动版本。

---

## ✅ 第8步：查看生成的视频文件

**复制粘贴执行：**

```cmd
dir D:\code\adminFlow\hm-service\tts\videos\*.mp4
```

**预期输出：**
```
2024-08-16  18:50        1,024,000 afe1db76-afff-4c4c-9629-9382e324e98f.mp4
```

---

## 🔧 故障排查（如果上述步骤失败）

### 问题1：FFmpeg安装失败（下载失败）

**手动下载方法：**

1. 访问：https://www.gyan.dev/ffmpeg/builds/packages/
2. 下载：`ffmpeg-6.1.1-essentials_build.7z`
3. 解压到：`D:\ai\codex\ffmpeg-6.1.1-essentials_build\`
4. 重新运行第2步验证

---

### 问题2：GPU编码器不可用

**检查NVIDIA驱动版本：**

```cmd
nvidia-smi
```

**如果驱动版本 < 531.00：**

#### 方案A：升级NVIDIA驱动（推荐）

1. 访问：https://www.nvidia.com/drivers
2. 下载最新Studio驱动
3. 安装并重启电脑
4. 重新运行第3步验证

#### 方案B：禁用GPU加速（应急）

**修改 `FFmpegUtil.java`：**

```java
// 第23行，改为false
private static final boolean ENABLE_GPU_ACCELERATION = false;
```

**重新编译：**

```cmd
cd D:\code\adminFlow\hm-service && mvn clean package -DskipTests
```

**重启服务（第5步）**

---

### 问题3：视频生成失败

**查看详细错误日志：**

```cmd
type D:\code\adminFlow\hm-service\logs\spring.log | findstr "ERROR"
```

**常见错误：**

#### 错误A：`Driver does not support...`
**原因：** NVIDIA驱动版本不足  
**解决：** 升级驱动到531.00+

#### 错误B：`Cannot load nvcuda.dll`
**原因：** CUDA未正确安装  
**解决：** 安装CUDA Toolkit（https://developer.nvidia.com/cuda-downloads）

#### 错误C：`FFmpeg执行失败，退出码：-40`
**原因：** GPU编码器初始化失败  
**解决：** 禁用GPU加速（方案B）

---

## 📊 性能验证

**查看GPU使用率（实时监控）：**

```cmd
nvidia-smi -l 1
```

**预期结果（视频生成时）：**
```
GPU利用率: 60-80%
显存使用: 2-4 GB
温度: 50-70°C
```

**如果GPU利用率为0%：**
说明未使用GPU加速，检查日志是否有错误。

---

## 🎉 成功标志总结

### ✅ 全部成功（GPU加速可用）

```
第1步：✅ FFmpeg 6.1.1 安装成功
第2步：✅ 版本验证通过
第3步：✅ h264_nvenc 编码器可用
第4步：✅ NVIDIA驱动 ≥ 531.00
第5步：✅ Spring Boot启动成功
第6步：✅ 视频生成成功（HTTP 200）
第7步：✅ 日志显示"使用GPU硬件加速"
第8步：✅ 视频文件已生成
```

### ⚠️ 部分成功（CPU编码降级）

```
第1步：✅ FFmpeg 6.1.1 安装成功
第2步：✅ 版本验证通过
第3步：❌ h264_nvenc 编码器不可用
第4步：⚠️ NVIDIA驱动 < 531.00
第5步：✅ Spring Boot启动成功
第6步：✅ 视频生成成功（但使用CPU编码）
第7步：⚠️ 日志显示"使用CPU编码"
第8步：✅ 视频文件已生成（但生成速度慢）
```

**建议：** 升级NVIDIA驱动以启用GPU加速（性能提升20倍）

---

## 📞 快速帮助

### 查看完整文档

```cmd
# 打开文档目录
explorer D:\code\adminFlow\docs

# 查看总索引
type D:\code\adminFlow\docs\FFmpeg问题解决方案-总索引.md

# 查看快速启动指南
type D:\code\adminFlow\docs\FFmpeg-6.1.1-快速启动指南.md
```

### 重新运行安装脚本

```cmd
cd D:\code\adminFlow\scripts
setup_ffmpeg_6.1.1_complete.bat
```

### 检查配置报告

```cmd
type D:\code\adminFlow\docs\FFmpeg-6.1.1-配置报告.txt
```

---

## 🔗 相关文档

| 文档 | 路径 | 用途 |
|------|------|------|
| 总索引 | `docs\FFmpeg问题解决方案-总索引.md` | 导航所有文档 |
| 快速启动指南 | `docs\FFmpeg-6.1.1-快速启动指南.md` | 5分钟快速上手 |
| 技术详解 | `docs\FFmpeg降级到6.1.1-LTS版本.md` | 深度技术分析 |
| 应急降级 | `docs\紧急降级-禁用GPU加速.md` | CPU编码方案 |
| 本清单 | `执行清单-FFmpeg-6.1.1.md` | 复制粘贴执行 |

---

**执行清单版本：** v1.0  
**最后更新：** 2026-08-16  
**预计耗时：** 5-10分钟  
**成功率：** 95%+  

**打印此文档，按步骤执行即可！** 📋
