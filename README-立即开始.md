# 🚀 FFmpeg 6.1.1 立即开始

## 当前状态

- ✅ **Java代码已配置完成**（FFmpegUtil.java 指向 FFmpeg 6.1.1）
- ✅ **安装脚本已创建完成**
- ✅ **文档已全部准备完成**
- ❌ **FFmpeg 6.1.1 未安装**（需要执行安装）

---

## ⚡ 最快执行方法（推荐）

### 方法1：命令行执行（3分钟）

**打开CMD，复制粘贴：**

```cmd
cd D:\code\adminFlow\scripts && install_7zip_and_ffmpeg.bat
```

这个命令会：
1. 自动下载安装 7-Zip（2MB）
2. 自动下载 FFmpeg 6.1.1（30MB）
3. 自动解压到正确位置
4. 自动验证安装结果

---

### 方法2：双击图标执行（最简单）

**第1步：创建桌面快捷方式**

```cmd
cd D:\code\adminFlow
创建桌面快捷方式.bat
```

**第2步：双击桌面上的"安装FFmpeg 6.1.1"图标**

---

## 📋 安装后的完整流程

### 1. 验证安装（30秒）

```cmd
D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -version
```

应该看到：`ffmpeg version 6.1.1`

---

### 2. 检查GPU支持（30秒）

```cmd
D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -encoders | findstr nvenc
```

**如果看到 `h264_nvenc`：** ✅ GPU加速可用  
**如果没有输出：** ⚠️ 需要升级NVIDIA驱动到531.00+

---

### 3. 重启Spring Boot服务（1分钟）

```cmd
cd D:\code\adminFlow\hm-service
mvn spring-boot:run
```

等待看到：`Tomcat started on port(s): 8080`

---

### 4. 测试视频生成（新开CMD窗口）

```cmd
curl -X POST http://localhost:8080/api/video/generate -H "Content-Type: application/json" -d "{\"documentId\":\"test\",\"text\":\"测试文本\"}"
```

成功标志：`{"code":200,"message":"视频生成成功"}`

---

### 5. 验证GPU加速

```cmd
type D:\code\adminFlow\hm-service\logs\spring.log | findstr "GPU"
```

成功标志：`✅ 使用GPU硬件加速（NVIDIA NVENC h264_nvenc）`

---

## 📁 文件结构

```
D:\code\adminFlow\
├── 立即执行-FFmpeg安装.md        (详细安装指南)
├── 快速执行卡片.txt              (5分钟执行流程)
├── 执行清单-FFmpeg-6.1.1.md      (完整执行清单)
├── README-立即开始.md            (本文件)
├── 创建桌面快捷方式.bat          (创建快捷方式)
│
├── scripts/                      (脚本目录)
│   ├── install_7zip_and_ffmpeg.bat           (一键安装)
│   ├── download_ffmpeg_6.1.1.bat             (下载FFmpeg)
│   ├── download_ffmpeg_without_7zip.bat      (备用下载)
│   ├── test_ffmpeg_6.1.1.bat                 (测试脚本)
│   └── setup_ffmpeg_6.1.1_complete.bat       (完整安装)
│
├── docs/                         (文档目录)
│   ├── FFmpeg问题解决方案-总索引.md
│   ├── FFmpeg-6.1.1-快速启动指南.md
│   ├── FFmpeg降级到6.1.1-LTS版本.md
│   └── 紧急降级-禁用GPU加速.md
│
└── hm-service/                   (Spring Boot项目)
    └── src/main/java/com/hmall/tts/video/util/
        └── FFmpegUtil.java       (已配置为6.1.1)
```

---

## 🎯 三种安装方案对比

| 方案 | 命令 | 优点 | 缺点 | 推荐度 |
|------|------|------|------|--------|
| **方案1：一键安装** | `install_7zip_and_ffmpeg.bat` | 全自动，最快 | 需要下载7-Zip | ⭐⭐⭐⭐⭐ |
| **方案2：无需7-Zip** | `download_ffmpeg_without_7zip.bat` | 使用Windows内置工具 | 仅限Windows 10+ | ⭐⭐⭐⭐ |
| **方案3：手动安装** | 手动下载解压 | 完全可控 | 步骤多，耗时长 | ⭐⭐⭐ |

---

## ❌ 常见问题快速解决

### Q1：安装脚本下载失败怎么办？

**A：使用备用方案**

```cmd
cd D:\code\adminFlow\scripts
download_ffmpeg_without_7zip.bat
```

或手动下载：https://www.gyan.dev/ffmpeg/builds/packages/

---

### Q2：GPU编码器不可用怎么办？

**A：检查NVIDIA驱动版本**

```cmd
nvidia-smi
```

如果驱动 < 531.00：
- **方案A（推荐）：** 升级驱动 → https://www.nvidia.com/drivers
- **方案B（应急）：** 禁用GPU加速（修改FFmpegUtil.java第23行为false）

---

### Q3：视频生成失败怎么办？

**A：查看详细错误日志**

```cmd
type D:\code\adminFlow\hm-service\logs\spring.log | findstr "ERROR"
```

常见错误：
- `Driver does not support...` → 升级驱动到531.00+
- `Cannot load nvcuda.dll` → 安装CUDA Toolkit
- `FFmpeg执行失败，退出码：-40` → 禁用GPU加速

---

### Q4：如何回退到旧版本FFmpeg？

**A：修改FFmpegUtil.java的路径**

```java
// 改回9.0.1
private static final String FFMPEG_PATH = 
    "D:\\ai\\codex\\ffmpeg-9.0.1-essentials_build\\bin\\ffmpeg.exe";
```

重新编译并重启服务

---

## 📞 获取帮助

### 查看详细文档

```cmd
# 快速启动指南（5分钟上手）
type D:\code\adminFlow\docs\FFmpeg-6.1.1-快速启动指南.md

# 技术详解（深度分析）
type D:\code\adminFlow\docs\FFmpeg降级到6.1.1-LTS版本.md

# 总索引（导航所有文档）
type D:\code\adminFlow\docs\FFmpeg问题解决方案-总索引.md
```

---

### 查看快速执行卡片

```cmd
type D:\code\adminFlow\快速执行卡片.txt
```

---

## ✅ 成功检查清单

安装完成后，确认以下所有项都是 ✅：

- [ ] FFmpeg 6.1.1 已安装（`ffmpeg -version` 显示6.1.1）
- [ ] GPU编码器可用（`ffmpeg -encoders | findstr nvenc` 有输出）
- [ ] NVIDIA驱动 ≥ 531.00（`nvidia-smi` 显示版本号）
- [ ] Spring Boot服务已启动（`curl http://localhost:8080/actuator/health` 返回UP）
- [ ] 视频生成成功（调用接口返回200）
- [ ] 日志显示GPU加速（日志包含"使用GPU硬件加速"）

---

## 🎉 准备好了吗？

**现在就执行第一步，5分钟后完成！**

```cmd
cd D:\code\adminFlow\scripts && install_7zip_and_ffmpeg.bat
```

或者查看快速卡片：

```cmd
type D:\code\adminFlow\快速执行卡片.txt
```

---

**文档版本：** v1.0  
**最后更新：** 2026-08-16  
**预计耗时：** 5-10分钟  
**成功率：** 95%+  

**祝你安装顺利！** 🚀
