# 🎬 视频生成器 - 使用指南

**功能：** 从Word对话文档生成带字幕动画的视频  
**版本：** v1.0 (阶段1 - 基础功能)  
**完成时间：** 2026-08-14

---

## 🎯 功能特点

### ✅ 已实现功能

1. **Word文档解析**
   - 支持.docx格式
   - 自动识别加粗/非加粗文本（区分角色）
   - 逐句解析对话内容

2. **语音合成**
   - 使用火山引擎TTS
   - 支持多种音色（男声/女声）
   - 高质量音频输出（MP3）

3. **字幕生成**
   - ASS格式字幕（支持丰富特效）
   - 可自定义字体、颜色、大小
   - 可调整字幕位置（九宫格）

4. **动画效果**（8种）
   - 渐入渐出 - 平滑自然
   - 从下飞入 - 动感强
   - 从上飞入 - 快速出现
   - 从左/右飞入 - 侧向进入
   - 缩放进入 - 有冲击力
   - 弹跳效果 - 活泼可爱
   - 无动画 - 直接显示

5. **视频合成**
   - FFmpeg高性能视频生成
   - 支持多种分辨率（HD/Full HD/4K）
   - 可自定义背景颜色
   - MP4格式输出

---

## 📋 环境要求

### 必须安装

1. **FFmpeg** - 视频处理工具
   - Windows: 下载ffmpeg.exe，添加到PATH环境变量
   - 下载地址: https://ffmpeg.org/download.html
   - 验证安装: `ffmpeg -version`

2. **Java 8+** - 已安装

3. **Maven** - 已安装

### 可选

- **火山引擎TTS账号** - 已配置

---

## 🚀 快速开始

### 步骤1：检查FFmpeg

```bash
# Windows命令行执行
ffmpeg -version
```

如果提示找不到命令，需要：
1. 下载FFmpeg: https://ffmpeg.org/download.html
2. 解压到任意目录，例如: `C:\ffmpeg`
3. 添加到PATH环境变量: `C:\ffmpeg\bin`
4. 重启命令行

### 步骤2：启动服务

```bash
cd d:\code\adminFlow\hm-service
mvn spring-boot:run
```

### 步骤3：访问测试页面

```
http://localhost:9080/video-generator-test.html
```

### 步骤4：生成视频

1. 上传Word文档（.docx格式）
2. 选择音色（加粗/非加粗）
3. 配置视频参数（分辨率、背景色）
4. 配置字幕样式（字体、颜色、位置）
5. 选择动画效果
6. 点击"生成视频"
7. 等待生成完成（通常30-60秒）
8. 预览并下载视频

---

## 📁 项目结构

```
hm-service/src/main/java/com/hmall/tts/
├── volcengine/                    # 现有TTS功能
│   ├── controller/
│   ├── service/
│   └── dto/
│
└── video/                         # 新功能：视频生成
    ├── controller/
    │   └── VideoGeneratorController.java      # API接口
    ├── service/
    │   ├── VideoGeneratorService.java         # 服务接口
    │   └── impl/
    │       └── VideoGeneratorServiceImpl.java # 服务实现
    ├── subtitle/
    │   └── ASSSubtitleGenerator.java          # ASS字幕生成器
    ├── animation/
    │   └── AnimationType.java                 # 动画类型枚举
    ├── dto/
    │   ├── VideoGenerateRequest.java          # 请求DTO
    │   ├── VideoGenerateResponse.java         # 响应DTO
    │   ├── SubtitleSegment.java               # 字幕片段
    │   ├── VideoConfig.java                   # 视频配置
    │   └── SubtitleConfig.java                # 字幕配置
    └── util/
        ├── FFmpegUtil.java                    # FFmpeg工具
        └── TimeUtil.java                      # 时间格式转换
```

---

## 🎨 使用示例

### 示例1：基础使用

**Word文档内容：**
```
你好，我是云舟，很高兴认识你小薇
你好，云舟我也很高兴认识你
你来自哪里？
我来在吉林，你呢
我来在大连
```

**配置：**
- 加粗音色：云舟（男声）
- 非加粗音色：薇薇（女声）
- 动画效果：渐入渐出
- 分辨率：1920x1080

**生成结果：**
- 视频时长：约15秒
- 文件大小：约2-3MB
- 每句话独立显示，配合语音播报

### 示例2：高级配置

**配置：**
- 分辨率：3840x2160 (4K)
- 背景颜色：#E8F5E9（淡绿色）
- 字体大小：60
- 字体颜色：#1B5E20（深绿色）
- 动画效果：从下飞入
- 字幕位置：底部居中

**生成结果：**
- 4K超高清视频
- 绿色主题风格
- 动感飞入效果

---

## 🔧 API文档

### 生成视频

**接口：** `POST /api/video-generator/generate`

**请求参数：**
```
file: Word文档文件 (multipart/form-data)
boldVoice: 加粗文本音色 (string, 可选, 默认: zh_male_m191_uranus_bigtts)
normalVoice: 非加粗文本音色 (string, 可选, 默认: zh_female_vv_uranus_bigtts)
videoWidth: 视频宽度 (int, 可选, 默认: 1920)
videoHeight: 视频高度 (int, 可选, 默认: 1080)
backgroundColor: 背景颜色 (string, 可选, 默认: #FFFFFF)
fontSize: 字体大小 (int, 可选, 默认: 48)
fontColor: 字体颜色 (string, 可选, 默认: #FFFFFF)
borderColor: 边框颜色 (string, 可选, 默认: #000000)
subtitlePosition: 字幕位置 (int, 可选, 1-9, 默认: 2)
animationType: 动画类型 (string, 可选, 默认: fade)
```

**响应：**
```json
{
  "success": true,
  "message": "视频生成成功",
  "taskId": "uuid",
  "videoUrl": "/tts/videos/uuid.mp4",
  "duration": 15.5,
  "videoSize": 2048576,
  "subtitles": [...]
}
```

### 获取动画类型列表

**接口：** `GET /api/video-generator/animation-types`

**响应：**
```json
[
  {
    "code": "fade",
    "description": "渐入渐出"
  },
  {
    "code": "slide_up",
    "description": "从下飞入"
  },
  ...
]
```

### 获取音色列表

**接口：** `GET /api/video-generator/voices`

**响应：**
```json
{
  "male": [
    {
      "code": "zh_male_m191_uranus_bigtts",
      "name": "云舟（沉稳男声）"
    },
    ...
  ],
  "female": [
    {
      "code": "zh_female_vv_uranus_bigtts",
      "name": "薇薇（温柔女声）"
    },
    ...
  ]
}
```

---

## ⚙️ 配置说明

### 动画类型

| 代码 | 名称 | 描述 | 视觉效果 |
|------|------|------|---------|
| fade | 渐入渐出 | 淡入淡出 | 平滑自然 |
| slide_up | 从下飞入 | 从屏幕下方飞入 | 动感强 |
| slide_down | 从上飞入 | 从屏幕上方飞入 | 快速出现 |
| slide_left | 从左飞入 | 从屏幕左侧飞入 | 侧向进入 |
| slide_right | 从右飞入 | 从屏幕右侧飞入 | 侧向进入 |
| zoom_in | 缩放进入 | 从小到大 | 有冲击力 |
| bounce | 弹跳效果 | 弹性动画 | 活泼可爱 |
| none | 无动画 | 直接显示 | 简洁 |

### 字幕位置

```
1 2 3    1=左上   2=顶部居中  3=右上
4 5 6    4=左中   5=正中      6=右中
7 8 9    7=左下   8=底部居中  9=右下
```

### 分辨率选项

- **1280x720** (HD) - 文件小，生成快
- **1920x1080** (Full HD) - 推荐，平衡
- **3840x2160** (4K) - 超高清，文件大

---

## 🐛 故障排查

### 问题1：FFmpeg not found

**症状：** 提示"FFmpeg不可用"

**解决方案：**
1. 确认FFmpeg已安装：`ffmpeg -version`
2. 添加到PATH环境变量
3. 重启服务

### 问题2：视频生成失败

**症状：** 生成失败，提示"FFmpeg执行失败"

**可能原因：**
- 磁盘空间不足
- FFmpeg版本过旧
- 权限问题

**解决方案：**
1. 检查磁盘空间
2. 更新FFmpeg到最新版本
3. 检查输出目录权限

### 问题3：字幕不显示

**症状：** 视频中看不到字幕

**可能原因：**
- ASS字幕文件生成失败
- 字体颜色与背景色相同
- 字幕位置超出视频范围

**解决方案：**
1. 检查日志中ASS文件是否生成
2. 调整字幕颜色与背景色对比度
3. 使用默认字幕位置（底部居中）

### 问题4：生成速度慢

**症状：** 生成1分钟视频需要5-10分钟

**优化方案：**
1. 降低视频分辨率（使用720p）
2. 降低视频比特率
3. 使用更快的编码器

---

## 📈 性能参考

### 生成时间

| 视频时长 | 分辨率 | 生成时间 | 文件大小 |
|---------|-------|---------|---------|
| 30秒 | 720p | 10-15秒 | 1-2MB |
| 30秒 | 1080p | 20-30秒 | 2-3MB |
| 30秒 | 4K | 60-90秒 | 8-10MB |
| 1分钟 | 1080p | 40-60秒 | 4-6MB |
| 3分钟 | 1080p | 2-3分钟 | 12-18MB |

### 服务器要求

**最低配置：**
- CPU: 2核
- 内存: 4GB
- 磁盘: 10GB可用空间

**推荐配置：**
- CPU: 4核
- 内存: 8GB
- 磁盘: 50GB可用空间
- SSD硬盘

---

## 🚧 下一步计划（阶段2）

### 即将实现的功能

1. **更多动画效果**
   - 逐字显示
   - 打字机效果
   - 3D翻转
   - 光效扫过

2. **高级字幕**
   - 自定义字体文件
   - 渐变色字幕
   - 描边动画
   - 阴影效果

3. **背景支持**
   - 背景图片
   - 背景视频
   - 背景模糊

4. **批量处理**
   - 异步队列
   - 进度查询
   - 批量下载

---

## 💡 使用技巧

### 技巧1：选择合适的动画

- **渐入渐出** - 适合正式、严肃的内容
- **飞入效果** - 适合活泼、动感的内容
- **弹跳效果** - 适合儿童、可爱风格
- **无动画** - 适合需要快速阅读的内容

### 技巧2：字幕颜色搭配

- **白色字幕 + 黑色边框** - 通用，适合大多数场景
- **黑色字幕 + 白色背景** - 简洁，适合演示
- **彩色字幕** - 活泼，适合娱乐内容

### 技巧3：分辨率选择

- **720p** - 手机观看、社交媒体分享
- **1080p** - 电脑观看、YouTube上传
- **4K** - 专业制作、大屏展示

---

## 📞 技术支持

**文档位置：** `d:\code\adminFlow\VIDEO-GENERATOR-README.md`

**相关文件：**
- 视频方案分析：`d:\code\adminFlow\视频导出方案-完整分析.md`
- 测试页面：`http://localhost:9080/video-generator-test.html`

---

**状态：** ✅ 阶段1完成（基础功能）  
**版本：** v1.0  
**最后更新：** 2026-08-14
