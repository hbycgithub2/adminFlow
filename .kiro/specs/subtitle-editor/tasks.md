# Implementation Tasks - Subtitle Editor

## Phase 1: Core Editing Functionality (1-2 Days)

### Backend Tasks (Day 1)

#### Task 1: Create DTO Classes
**Status**: completed
**Priority**: high
**Dependencies**: none

**Files to create**:
- `SubtitleSegment.java` - 字幕片段数据模型
- `SubtitleStyle.java` - 字幕样式数据模型
- `SubtitleEditData.java` - 编辑数据DTO
- `SubtitleUpdateRequest.java` - 更新请求DTO
- `SubtitleUpdateResponse.java` - 更新响应DTO

**Acceptance**:
- All DTOs use Lombok annotations (@Data, @Builder)
- All fields have JavaDoc comments
- All DTOs are serializable

---

#### Task 2: Implement ASS Parser
**Status**: completed
**Priority**: high
**Dependencies**: Task 1

**Files to create**:
- `ASSParser.java` - ASS文件解析器

**Methods**:
- `parse(String assFilePath)` - 解析ASS文件
- `parseDialogueLine(String line)` - 解析单行Dialogue
- `parseTime(String timeStr)` - 解析时间字符串
- `extractAnimationType(String text)` - 提取动画类型

**Acceptance**:
- 能正确解析ASS文件的[Events]段
- 能正确解析Dialogue行的所有字段
- 时间解析精度0.01秒
- 能正确提取动画标签

---

#### Task 3: Implement ASS Formatter (Pretty Printer)
**Status**: completed
**Priority**: high
**Dependencies**: Task 1

**Files to create**:
- `ASSFormatter.java` - ASS文件格式化器

**Methods**:
- `format(List<SubtitleSegment>, SubtitleConfig)` - 格式化为ASS内容
- `generateHeader(SubtitleConfig)` - 生成头部
- `generateStyles(SubtitleConfig)` - 生成样式定义
- `generateDialogueLine(SubtitleSegment, String)` - 生成Dialogue行
- `formatTime(Double)` - 格式化时间

**Acceptance**:
- 生成的ASS文件格式正确
- 动画标签正确包裹在{}中
- 时间格式符合ASS规范 (0:00:01.23)
- 支持所有8种动画类型

---

#### Task 4: Write Round-Trip Tests
**Status**: pending
**Priority**: high
**Dependencies**: Task 2, Task 3

**Files to create**:
- `RoundTripTest.java` - 往返一致性测试

**Test cases**:
- `testRoundTripProperty()` - parse → format → parse 数据一致
- `testTextPreservation()` - 文本内容完全保留
- `testTimingPrecision()` - 时间精度0.01秒
- `testStylePreservation()` - 样式属性完全保留
- `testAnimationPreservation()` - 动画标签完全保留

**Acceptance**:
- 所有测试通过
- Round-trip property验证成功

---

#### Task 5: Implement SubtitleEditorService
**Status**: completed
**Priority**: high
**Dependencies**: Task 2, Task 3

**Files to create**:
- `SubtitleEditorService.java` - 服务接口
- `SubtitleEditorServiceImpl.java` - 服务实现

**Methods**:
- `loadSubtitles(String taskId)` - 加载字幕数据
- `updateSubtitles(SubtitleUpdateRequest)` - 更新字幕数据
- `regenerateVideo(String taskId)` - 重新生成视频
- `validateTimeOverlap(List<SubtitleSegment>)` - 验证时间重叠

**Acceptance**:
- 能正确加载ASS文件
- 能正确保存修改后的ASS文件
- 能调用FFmpegUtil重新生成视频
- 能检测时间重叠冲突

---

#### Task 6: Implement SubtitleEditorController
**Status**: completed
**Priority**: high
**Dependencies**: Task 5

**Files to create**:
- `SubtitleEditorController.java` - REST API控制器

**Endpoints**:
- `GET /api/subtitle-editor/load?taskId={taskId}` - 加载字幕
- `POST /api/subtitle-editor/update` - 更新字幕
- `POST /api/subtitle-editor/regenerate?taskId={taskId}` - 重新生成

**Acceptance**:
- 所有端点返回正确的JSON格式
- 错误处理完善（返回用户友好的中文错误消息）
- 日志记录完善

---

#### Task 7: Test Backend APIs
**Status**: pending
**Priority**: high
**Dependencies**: Task 6

**Test methods**:
- Postman/curl测试所有API
- 验证加载字幕功能
- 验证更新字幕功能
- 验证重新生成视频功能
- 验证错误处理

**Acceptance**:
- 所有API测试通过
- 错误场景测试通过

---

### Frontend Tasks (Day 2)

#### Task 8: Create HTML Structure
**Status**: pending
**Priority**: high
**Dependencies**: none

**Files to create**:
- `subtitle-editor.html` - 字幕编辑器页面

**Sections**:
- 页面头部（标题、视频信息）
- 字幕列表表格
- 编辑对话框
- 工具栏（保存、重新生成按钮）

**Acceptance**:
- HTML结构清晰
- 语义化标签使用正确
- 响应式布局

---

#### Task 9: Implement Subtitle List Table
**Status**: pending
**Priority**: high
**Dependencies**: Task 8

**Features**:
- 显示所有字幕（序号、文本、开始时间、持续时间、操作）
- 文本超过30字符显示省略号
- 时间显示1位小数
- 每行有"编辑"按钮

**Acceptance**:
- 表格显示正确
- 分页功能正常（20条/页）
- 数据绑定正确

---

#### Task 10: Implement Edit Dialog
**Status**: pending
**Priority**: high
**Dependencies**: Task 9

**Features**:
- 点击"编辑"弹出对话框
- 文本输入框（支持100字符）
- 开始时间输入框（数字，0.1精度）
- 持续时间输入框（数字，0.1精度）
- 样式配置面板
- 保存/取消按钮

**Acceptance**:
- 对话框显示正确
- 表单验证正常
- 保存/取消功能正常

---

#### Task 11: Implement Style Configuration Panel
**Status**: pending
**Priority**: high
**Dependencies**: Task 10

**Features**:
- 字体名称下拉框
- 字体大小滑块（24-96px）
- 字体颜色选择器
- 边框颜色选择器
- 边框粗细滑块（1-5px）
- 字幕位置下拉框（1-9）
- 动画类型下拉框（8种）

**Acceptance**:
- 所有配置项显示正确
- 颜色选择器支持#RRGGBB格式
- 实时预览效果

---

#### Task 12: Implement AJAX Communication
**Status**: pending
**Priority**: high
**Dependencies**: Task 6, Task 11

**Functions**:
- `loadSubtitles(taskId)` - 加载字幕数据
- `updateSubtitles(data)` - 更新字幕数据
- `regenerateVideo(taskId)` - 重新生成视频

**Acceptance**:
- Fetch API调用正确
- 错误处理完善
- Loading状态显示

---

#### Task 13: Implement Batch Operations
**Status**: pending
**Priority**: medium
**Dependencies**: Task 9

**Features**:
- 全选复选框
- 批量应用样式按钮
- 批量删除按钮

**Acceptance**:
- 全选功能正常
- 批量操作正常

---

#### Task 14: Add Time Overlap Validation
**Status**: pending
**Priority**: medium
**Dependencies**: Task 10

**Features**:
- 保存时检测时间重叠
- 显示重叠警告
- 高亮重叠的字幕

**Acceptance**:
- 重叠检测准确
- 警告提示友好

---

#### Task 15: Integration Testing
**Status**: pending
**Priority**: high
**Dependencies**: All frontend tasks

**Test scenarios**:
- 加载字幕列表
- 编辑单条字幕
- 修改字幕样式
- 批量应用样式
- 保存修改
- 重新生成视频
- 错误处理

**Acceptance**:
- 所有功能测试通过
- 用户体验流畅

---

#### Task 16: Write Documentation
**Status**: pending
**Priority**: medium
**Dependencies**: Task 15

**Files to create**:
- `SUBTITLE-EDITOR-README.md` - 使用指南
- `SUBTITLE-EDITOR-API.md` - API文档

**Content**:
- 功能介绍
- 使用步骤
- API文档
- 常见问题

**Acceptance**:
- 文档完整清晰
- 示例代码正确

---

## Summary

**Total Tasks**: 16
**Estimated Time**: 1-2 days
**Priority Breakdown**:
- High: 13 tasks
- Medium: 3 tasks

**Implementation Order**:
1. Day 1 Morning: Tasks 1-3 (DTO + Parser + Formatter)
2. Day 1 Afternoon: Tasks 4-7 (Tests + Service + Controller + API Testing)
3. Day 2 Morning: Tasks 8-12 (Frontend Structure + UI + AJAX)
4. Day 2 Afternoon: Tasks 13-16 (Batch Ops + Validation + Testing + Docs)

---

**Ready to start implementation!** 🚀
