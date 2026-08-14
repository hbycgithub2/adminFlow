# Design Document - Subtitle Editor

## High-Level Design

### System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  Frontend (Browser)                      │
│  ┌───────────────────────────────────────────────────┐  │
│  │          subtitle-editor.html                     │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌──────────┐ │  │
│  │  │ 字幕列表表格  │  │ 编辑对话框   │  │ 视频预览  │ │  │
│  │  └─────────────┘  └─────────────┘  └──────────┘ │  │
│  └───────────────────────────────────────────────────┘  │
│                         ↓ AJAX                           │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│              Backend (Spring Boot)                       │
│  ┌───────────────────────────────────────────────────┐  │
│  │      SubtitleEditorController (REST API)          │  │
│  │  /api/subtitle-editor/load                        │  │
│  │  /api/subtitle-editor/update                      │  │
│  │  /api/subtitle-editor/regenerate                  │  │
│  └───────────────────────────────────────────────────┘  │
│                         ↓                                │
│  ┌───────────────────────────────────────────────────┐  │
│  │        SubtitleEditorService                      │  │
│  │  - loadSubtitles()                                │  │
│  │  - updateSubtitles()                              │  │
│  │  - regenerateVideo()                              │  │
│  └───────────────────────────────────────────────────┘  │
│          ↓                    ↓                          │
│  ┌──────────────┐    ┌──────────────────┐               │
│  │  ASSParser   │    │ ASSFormatter     │               │
│  │ (Read ASS)   │    │ (Write ASS)      │               │
│  └──────────────┘    └──────────────────┘               │
│                         ↓                                │
│  ┌───────────────────────────────────────────────────┐  │
│  │        FFmpegUtil                                 │  │
│  │  generateVideoFromAudioAndASS()                   │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                File System                               │
│  /tts/temp/{taskId}.ass                                 │
│  /tts/temp/{taskId}.mp3    (original audio)             │
│  /tts/videos/{taskId}.mp4  (regenerated video)          │
└─────────────────────────────────────────────────────────┘
```

### Component Breakdown

#### Frontend Components
1. **Subtitle List Table** - 显示所有字幕列表（序号、文本、时间、操作）
2. **Edit Dialog** - 编辑单条字幕（文本、时间、样式）
3. **Style Panel** - 字幕样式配置（字体、颜色、动画）
4. **Video Preview** (可选) - HTML5 Video播放器
5. **Toolbar** - 批量操作按钮（全选、保存、重新生成）

#### Backend Components
1. **SubtitleEditorController** - REST API接口
2. **SubtitleEditorService** - 业务逻辑服务
3. **ASSParser** - ASS文件解析器
4. **ASSFormatter** - ASS文件格式化器（Pretty Printer）
5. **VideoRegenerationService** - 视频重新生成服务
6. **SubtitleSegment** - 字幕片段数据模型
7. **SubtitleStyle** - 字幕样式数据模型
8. **SubtitleEditData** - 编辑数据DTO

### Data Flow

```
用户编辑字幕
    ↓
前端表单提交 (AJAX POST)
    ↓
SubtitleEditorController.updateSubtitles()
    ↓
SubtitleEditorService.updateSubtitles()
    ↓
ASSFormatter.format(subtitles) → 生成新ASS文件
    ↓
保存ASS文件到 /tts/temp/{taskId}.ass
    ↓
(可选) FFmpegUtil.generateVideo(audio, ass, output)
    ↓
返回成功响应 + 新视频URL
    ↓
前端刷新字幕列表 + 更新视频预览
```

### Technology Stack

- **Frontend**: HTML5, JavaScript (ES6), CSS3, Fetch API
- **Backend**: Spring Boot 2.7, Spring MVC, Lombok
- **Parser**: 自定义ASS解析器（正则表达式 + 字符串处理）
- **Video**: FFmpeg 6.0+ (命令行调用)
- **Storage**: 本地文件系统
- **Logging**: SLF4J + Logback

---

## Low-Level Design

### Class Diagram

```java
// ========== DTO层 ==========
@Data
@Builder
public class SubtitleSegment {
    private Integer id;              // 序号
    private String text;             // 文本内容
    private Double startTime;        // 开始时间（秒）
    private Double duration;         // 持续时间（秒）
    private SubtitleStyle style;     // 样式
}

@Data
@Builder
public class SubtitleStyle {
    private String fontName;         // 字体名称
    private Integer fontSize;        // 字体大小
    private String fontColor;        // 字体颜色 (#FFFFFF)
    private String borderColor;      // 边框颜色 (#000000)
    private Integer borderWidth;     // 边框粗细
    private Integer position;        // 位置 (1-9)
    private String animationType;    // 动画类型 (fade, slide_up, etc.)
}

@Data
public class SubtitleEditData {
    private String taskId;           // 任务ID
    private List<SubtitleSegment> subtitles;  // 字幕列表
    private Double totalDuration;    // 总时长
}

@Data
public class SubtitleUpdateRequest {
    private String taskId;           // 任务ID
    private List<SubtitleSegment> subtitles;  // 更新后的字幕
    private Boolean regenerateVideo; // 是否重新生成视频
}

@Data
public class SubtitleUpdateResponse {
    private Boolean success;
    private String message;
    private String videoUrl;         // 新视频URL（如果重新生成）
}

// ========== Parser层 ==========
public class ASSParser {
    /**
     * 解析ASS文件为字幕片段列表
     * @param assFilePath ASS文件路径
     * @return 字幕片段列表
     */
    public List<SubtitleSegment> parse(String assFilePath);
    
    /**
     * 解析Dialogue行
     * @param line Dialogue行文本
     * @return 字幕片段
     */
    private SubtitleSegment parseDialogueLine(String line);
    
    /**
     * 解析时间字符串 (0:00:01.23)
     * @param timeStr 时间字符串
     * @return 秒数
     */
    private Double parseTime(String timeStr);
    
    /**
     * 提取动画标签
     * @param text 文本内容
     * @return 动画类型
     */
    private String extractAnimationType(String text);
}

// ========== Formatter层 ==========
public class ASSFormatter {
    /**
     * 格式化字幕片段列表为ASS文件内容
     * @param subtitles 字幕片段列表
     * @param config 字幕配置
     * @return ASS文件内容
     */
    public String format(List<SubtitleSegment> subtitles, SubtitleConfig config);
    
    /**
     * 生成ASS文件头部
     * @param config 字幕配置
     * @return 头部内容
     */
    private String generateHeader(SubtitleConfig config);
    
    /**
     * 生成样式定义
     * @param config 字幕配置
     * @return 样式内容
     */
    private String generateStyles(SubtitleConfig config);
    
    /**
     * 生成Dialogue行
     * @param segment 字幕片段
     * @param styleName 样式名称
     * @return Dialogue行
     */
    private String generateDialogueLine(SubtitleSegment segment, String styleName);
    
    /**
     * 格式化时间为ASS格式 (0:00:01.23)
     * @param seconds 秒数
     * @return 时间字符串
     */
    private String formatTime(Double seconds);
}

// ========== Service层 ==========
@Service
@Slf4j
public class SubtitleEditorService {
    @Autowired
    private ASSParser assParser;
    
    @Autowired
    private ASSFormatter assFormatter;
    
    @Autowired
    private FFmpegUtil ffmpegUtil;
    
    /**
     * 加载字幕数据
     * @param taskId 任务ID
     * @return 字幕编辑数据
     */
    public SubtitleEditData loadSubtitles(String taskId);
    
    /**
     * 更新字幕数据
     * @param request 更新请求
     * @return 更新响应
     */
    public SubtitleUpdateResponse updateSubtitles(SubtitleUpdateRequest request);
    
    /**
     * 重新生成视频
     * @param taskId 任务ID
     * @return 视频URL
     */
    public String regenerateVideo(String taskId);
    
    /**
     * 验证字幕时间重叠
     * @param subtitles 字幕列表
     * @return 重叠的字幕ID列表
     */
    private List<Integer> validateTimeOverlap(List<SubtitleSegment> subtitles);
}

// ========== Controller层 ==========
@RestController
@RequestMapping("/api/subtitle-editor")
@Slf4j
public class SubtitleEditorController {
    @Autowired
    private SubtitleEditorService subtitleEditorService;
    
    /**
     * 加载字幕数据
     * @param taskId 任务ID
     * @return 字幕编辑数据
     */
    @GetMapping("/load")
    public SubtitleEditData loadSubtitles(@RequestParam String taskId);
    
    /**
     * 更新字幕数据
     * @param request 更新请求
     * @return 更新响应
     */
    @PostMapping("/update")
    public SubtitleUpdateResponse updateSubtitles(@RequestBody SubtitleUpdateRequest request);
    
    /**
     * 重新生成视频
     * @param taskId 任务ID
     * @return 重新生成响应
     */
    @PostMapping("/regenerate")
    public SubtitleUpdateResponse regenerateVideo(@RequestParam String taskId);
}
```

### API Specifications

#### 1. 加载字幕数据

```
GET /api/subtitle-editor/load?taskId={taskId}

Response:
{
  "taskId": "uuid",
  "subtitles": [
    {
      "id": 1,
      "text": "你好，我是云舟，很高兴认识你",
      "startTime": 0.0,
      "duration": 4.5,
      "style": {
        "fontName": "Microsoft YaHei",
        "fontSize": 64,
        "fontColor": "#FFFFFF",
        "borderColor": "#000000",
        "borderWidth": 3,
        "position": 2,
        "animationType": "fade"
      }
    },
    ...
  ],
  "totalDuration": 28.6
}
```

#### 2. 更新字幕数据

```
POST /api/subtitle-editor/update

Request:
{
  "taskId": "uuid",
  "subtitles": [...],  // 修改后的字幕列表
  "regenerateVideo": false  // 是否立即重新生成视频
}

Response:
{
  "success": true,
  "message": "字幕更新成功",
  "videoUrl": null  // 如果regenerateVideo=true，返回新视频URL
}
```

#### 3. 重新生成视频

```
POST /api/subtitle-editor/regenerate?taskId={taskId}

Response:
{
  "success": true,
  "message": "视频重新生成成功",
  "videoUrl": "/tts/videos/{taskId}.mp4"
}
```

### ASS Parser Algorithm

```java
public List<SubtitleSegment> parse(String assFilePath) {
    List<SubtitleSegment> segments = new ArrayList<>();
    List<String> lines = Files.readAllLines(Paths.get(assFilePath));
    
    int id = 1;
    boolean inEventsSection = false;
    
    for (String line : lines) {
        // 检测[Events]段
        if (line.trim().equals("[Events]")) {
            inEventsSection = true;
            continue;
        }
        
        // 解析Dialogue行
        if (inEventsSection && line.startsWith("Dialogue:")) {
            SubtitleSegment segment = parseDialogueLine(line);
            segment.setId(id++);
            segments.add(segment);
        }
    }
    
    return segments;
}

private SubtitleSegment parseDialogueLine(String line) {
    // Dialogue格式: Dialogue: 0,0:00:00.00,0:00:04.50,Default,,0,0,0,,{动画标签}文本内容
    String[] parts = line.substring("Dialogue: ".length()).split(",", 10);
    
    Double startTime = parseTime(parts[1]);
    Double endTime = parseTime(parts[2]);
    Double duration = endTime - startTime;
    String text = parts[9];  // 最后一个字段是文本
    
    // 提取动画标签 (如果有)
    String animationType = extractAnimationType(text);
    text = text.replaceAll("\\{[^}]+\\}", "").trim();  // 移除动画标签
    
    SubtitleStyle style = SubtitleStyle.builder()
        .fontName("Microsoft YaHei")
        .fontSize(64)
        .fontColor("#FFFFFF")
        .borderColor("#000000")
        .borderWidth(3)
        .position(2)
        .animationType(animationType)
        .build();
    
    return SubtitleSegment.builder()
        .text(text)
        .startTime(startTime)
        .duration(duration)
        .style(style)
        .build();
}

private Double parseTime(String timeStr) {
    // 格式: 0:00:01.23 → 1.23秒
    String[] parts = timeStr.split(":");
    double hours = Double.parseDouble(parts[0]);
    double minutes = Double.parseDouble(parts[1]);
    double seconds = Double.parseDouble(parts[2]);
    return hours * 3600 + minutes * 60 + seconds;
}
```

### ASS Formatter Algorithm (Pretty Printer)

```java
public String format(List<SubtitleSegment> subtitles, SubtitleConfig config) {
    StringBuilder sb = new StringBuilder();
    
    // 1. 生成头部
    sb.append(generateHeader(config));
    sb.append("\n");
    
    // 2. 生成样式定义
    sb.append(generateStyles(config));
    sb.append("\n");
    
    // 3. 生成Events段
    sb.append("[Events]\n");
    sb.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
    
    // 4. 生成每条Dialogue
    for (SubtitleSegment segment : subtitles) {
        sb.append(generateDialogueLine(segment, "Default"));
        sb.append("\n");
    }
    
    return sb.toString();
}

private String generateDialogueLine(SubtitleSegment segment, String styleName) {
    String startTime = formatTime(segment.getStartTime());
    String endTime = formatTime(segment.getStartTime() + segment.getDuration());
    
    // 添加动画标签
    String animationTag = getAnimationTag(segment.getStyle().getAnimationType());
    String text = animationTag.isEmpty() ? segment.getText() : "{" + animationTag + "}" + segment.getText();
    
    return String.format("Dialogue: 0,%s,%s,%s,,0,0,0,,%s",
        startTime, endTime, styleName, text);
}

private String formatTime(Double seconds) {
    int hours = (int) (seconds / 3600);
    int minutes = (int) ((seconds % 3600) / 60);
    double secs = seconds % 60;
    return String.format("%d:%02d:%05.2f", hours, minutes, secs);
}
```

### Round-Trip Property Test

```java
@Test
public void testRoundTripProperty() {
    // 1. 解析原始ASS文件
    List<SubtitleSegment> original = assParser.parse("test.ass");
    
    // 2. 格式化为新ASS文件
    String newAssContent = assFormatter.format(original, config);
    Files.write(Paths.get("test_new.ass"), newAssContent.getBytes());
    
    // 3. 再次解析新文件
    List<SubtitleSegment> reparsed = assParser.parse("test_new.ass");
    
    // 4. 验证一致性
    assertEquals(original.size(), reparsed.size());
    for (int i = 0; i < original.size(); i++) {
        SubtitleSegment orig = original.get(i);
        SubtitleSegment repr = reparsed.get(i);
        
        assertEquals(orig.getText(), repr.getText());
        assertEquals(orig.getStartTime(), repr.getStartTime(), 0.01);  // 0.01秒精度
        assertEquals(orig.getDuration(), repr.getDuration(), 0.01);
        assertEquals(orig.getStyle().getAnimationType(), repr.getStyle().getAnimationType());
    }
}
```

---

## Key Design Decisions

### 1. 为什么使用ASS格式？
- **优点**: 支持丰富的字幕特效（动画、样式、位置）
- **优点**: FFmpeg原生支持ASS字幕合成
- **优点**: 文本格式，易于解析和修改
- **缺点**: 格式复杂，需要自定义解析器

### 2. 为什么先实现表格UI？
- **优点**: 开发速度快（1-2天）
- **优点**: 满足80%的编辑场景（文本修正、时间调整、样式修改）
- **优点**: 易于测试和调试
- **可扩展**: 后续可升级为时间轴可视化编辑

### 3. 为什么保持原音频不变？
- **优点**: 重新生成视频速度快（只需重新合成字幕，不需要重新生成语音）
- **优点**: 音频质量不会损失
- **优点**: 实现简单（FFmpeg直接合成音频+ASS）

### 4. 为什么使用Parser + Pretty Printer模式？
- **优点**: 职责分离（读和写分开）
- **优点**: 易于测试（Round-trip property）
- **优点**: 易于扩展（新增动画类型只需修改格式化逻辑）
- **优点**: 数据安全（解析错误不会影响写入）

---

## Implementation Plan

### Phase 1 Scope (Requirements 1-8, 10-11, 19-21)

**Day 1: Backend Implementation**
1. 创建DTO类 (SubtitleSegment, SubtitleStyle, SubtitleEditData等)
2. 实现ASSParser (解析ASS文件)
3. 实现ASSFormatter (生成ASS文件)
4. 编写Round-trip测试
5. 实现SubtitleEditorService
6. 实现SubtitleEditorController
7. 测试API接口

**Day 2: Frontend Implementation**
1. 创建subtitle-editor.html页面
2. 实现字幕列表表格
3. 实现编辑对话框
4. 实现样式配置面板
5. 实现AJAX通信
6. 集成测试
7. 文档编写

### File Structure

```
hm-service/src/main/java/com/hmall/tts/
└── subtitle/
    ├── controller/
    │   └── SubtitleEditorController.java
    ├── service/
    │   ├── SubtitleEditorService.java
    │   └── impl/
    │       └── SubtitleEditorServiceImpl.java
    ├── parser/
    │   ├── ASSParser.java
    │   └── ASSFormatter.java
    └── dto/
        ├── SubtitleSegment.java
        ├── SubtitleStyle.java
        ├── SubtitleEditData.java
        ├── SubtitleUpdateRequest.java
        └── SubtitleUpdateResponse.java

hm-service/src/main/resources/static/
└── subtitle-editor.html

hm-service/src/test/java/com/hmall/tts/subtitle/
├── ASSParserTest.java
├── ASSFormatterTest.java
└── RoundTripTest.java
```

### Testing Strategy

1. **Unit Tests**: ASSParser, ASSFormatter (独立测试)
2. **Round-Trip Tests**: parse → format → parse (数据一致性)
3. **Integration Tests**: Controller + Service (API测试)
4. **Manual Tests**: 前端页面功能测试

---

**Design Complete!** Ready for implementation. 🚀
