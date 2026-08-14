# Requirements Document - Subtitle Editor

## Introduction

本文档定义字幕编辑功能的需求规格，该功能允许用户编辑已生成视频的字幕内容、时间和样式，无需重新生成整个视频。本功能是现有"Word文档生成带字幕视频"功能的重要补充，解决了字幕无法编辑导致需要重新生成整个视频的痛点。

**背景：** 当前系统已实现从Word文档生成带字幕视频的功能，支持8种字幕动画效果、自动生成ASS字幕文件、FFmpeg视频合成。但生成的字幕无法编辑，任何错别字、时间不准确或样式不满意都需要重新生成整个视频，效率低下。

**目标：** 实现类似剪映的字幕编辑功能，用户可以随意编辑字幕文本、调整时间、修改样式，编辑后快速重新生成视频。

## Glossary

- **Subtitle_Editor**: 字幕编辑器，提供可视化界面编辑字幕的Web应用
- **Subtitle_Segment**: 字幕片段，表示一句字幕，包含文本、开始时间、持续时间和样式
- **ASS_File**: Advanced SubStation Alpha字幕文件，支持丰富特效的字幕格式
- **Time_Axis**: 时间轴，可视化显示所有字幕片段时间分布的UI组件
- **Video_Generator**: 视频生成器，使用FFmpeg合成视频和字幕的后端服务
- **Animation_Type**: 动画类型，字幕显示时的动画效果（渐入渐出、飞入等）
- **Subtitle_Style**: 字幕样式，包括字体、大小、颜色、边框等视觉属性
- **Task_ID**: 任务标识符，唯一标识一个视频生成任务
- **Original_Audio**: 原始音频，保持不变的音频文件
- **FFmpeg**: 开源视频处理工具，用于视频和字幕合成

## Requirements

### Requirement 1: 字幕列表展示

**User Story:** 作为视频内容创作者，我希望看到已生成视频的所有字幕列表，以便了解字幕整体结构和内容。

#### Acceptance Criteria

1. WHEN a user opens THE Subtitle_Editor with a valid Task_ID, THE Subtitle_Editor SHALL retrieve all Subtitle_Segments for that task
2. THE Subtitle_Editor SHALL display Subtitle_Segments in a table format with columns for sequence number, text content, start time, duration, and style
3. WHEN a Subtitle_Segment text exceeds 30 characters, THE Subtitle_Editor SHALL truncate the display text and show ellipsis
4. THE Subtitle_Editor SHALL display start time in seconds with 1 decimal precision
5. THE Subtitle_Editor SHALL display duration in seconds with 1 decimal precision
6. WHEN the video has more than 20 Subtitle_Segments, THE Subtitle_Editor SHALL implement pagination with 20 segments per page

### Requirement 2: 字幕文本编辑

**User Story:** 作为视频内容创作者，我希望修改字幕文本内容，以便纠正错别字或调整表达。

#### Acceptance Criteria

1. WHEN a user clicks the edit button for a Subtitle_Segment, THE Subtitle_Editor SHALL display a text input field with the current text
2. THE Subtitle_Editor SHALL accept UTF-8 encoded text input including Chinese, English, numbers, and punctuation
3. WHEN a user enters text exceeding 100 characters, THE Subtitle_Editor SHALL display a warning message
4. WHEN a user saves the edited text, THE Subtitle_Editor SHALL update the Subtitle_Segment text immediately
5. WHEN a user cancels editing, THE Subtitle_Editor SHALL restore the original text
6. THE Subtitle_Editor SHALL validate that text is not empty before saving

### Requirement 3: 字幕时间调整

**User Story:** 作为视频内容创作者，我希望调整字幕的开始时间和持续时间，以便字幕与音频同步。

#### Acceptance Criteria

1. WHEN a user clicks the edit button for a Subtitle_Segment, THE Subtitle_Editor SHALL display input fields for start time and duration
2. THE Subtitle_Editor SHALL accept start time values from 0.0 to video duration in seconds with 0.1 second precision
3. THE Subtitle_Editor SHALL accept duration values from 0.1 to 30.0 seconds with 0.1 second precision
4. WHEN a user enters a start time that causes overlap with the next segment, THE Subtitle_Editor SHALL display a warning message
5. WHEN a user enters a duration that causes the segment to extend beyond video duration, THE Subtitle_Editor SHALL display a warning message
6. WHEN a user saves time changes, THE Subtitle_Editor SHALL update the Subtitle_Segment timing immediately
7. THE Subtitle_Editor SHALL validate that start time plus duration does not exceed video duration

### Requirement 4: 字幕样式编辑

**User Story:** 作为视频内容创作者，我希望修改字幕样式（字体、大小、颜色、边框、动画），以便调整视觉效果。

#### Acceptance Criteria

1. WHEN a user clicks the style edit button for a Subtitle_Segment, THE Subtitle_Editor SHALL display a style configuration panel
2. THE Subtitle_Editor SHALL provide a font name dropdown with at least Microsoft YaHei, SimHei, and Arial options
3. THE Subtitle_Editor SHALL provide a font size slider from 32 to 96 pixels with 4 pixel increments
4. THE Subtitle_Editor SHALL provide a color picker for font color supporting hexadecimal color codes
5. THE Subtitle_Editor SHALL provide a color picker for border color supporting hexadecimal color codes
6. THE Subtitle_Editor SHALL provide a border width slider from 0 to 8 pixels with 1 pixel increments
7. THE Subtitle_Editor SHALL provide a position dropdown with 9 options (top-left, top-center, top-right, middle-left, center, middle-right, bottom-left, bottom-center, bottom-right)
8. THE Subtitle_Editor SHALL provide an Animation_Type dropdown with 8 options (fade, slide_up, slide_down, slide_left, slide_right, zoom_in, bounce, none)
9. WHEN a user saves style changes, THE Subtitle_Editor SHALL update the Subtitle_Style immediately

### Requirement 5: 批量样式应用

**User Story:** 作为视频内容创作者，我希望将某个字幕的样式应用到所有字幕，以便保持整体风格一致。

#### Acceptance Criteria

1. WHEN a user clicks the "Apply to All" button for a Subtitle_Segment style, THE Subtitle_Editor SHALL display a confirmation dialog
2. WHEN a user confirms the batch style application, THE Subtitle_Editor SHALL update all Subtitle_Segments to use the same Subtitle_Style
3. THE Subtitle_Editor SHALL preserve the text and timing of each Subtitle_Segment during batch style update
4. WHEN batch style application completes, THE Subtitle_Editor SHALL display a success message with the count of updated segments

### Requirement 6: 字幕添加

**User Story:** 作为视频内容创作者，我希望在指定时间位置添加新字幕，以便补充遗漏的内容。

#### Acceptance Criteria

1. WHEN a user clicks the "Add Subtitle" button, THE Subtitle_Editor SHALL display a dialog for new Subtitle_Segment creation
2. THE Subtitle_Editor SHALL require text, start time, and duration for the new Subtitle_Segment
3. THE Subtitle_Editor SHALL use the default Subtitle_Style for the new Subtitle_Segment
4. WHEN a user saves the new Subtitle_Segment, THE Subtitle_Editor SHALL insert it into the correct position based on start time
5. THE Subtitle_Editor SHALL update sequence numbers of all Subtitle_Segments after insertion
6. WHEN the new Subtitle_Segment overlaps with existing segments, THE Subtitle_Editor SHALL display a warning and require confirmation

### Requirement 7: 字幕删除

**User Story:** 作为视频内容创作者，我希望删除不需要的字幕，以便优化视频内容。

#### Acceptance Criteria

1. WHEN a user clicks the delete button for a Subtitle_Segment, THE Subtitle_Editor SHALL display a confirmation dialog
2. WHEN a user confirms deletion, THE Subtitle_Editor SHALL remove the Subtitle_Segment from the list
3. THE Subtitle_Editor SHALL update sequence numbers of all remaining Subtitle_Segments
4. WHEN deletion completes, THE Subtitle_Editor SHALL display a success message
5. THE Subtitle_Editor SHALL prevent deletion when only one Subtitle_Segment remains

### Requirement 8: 字幕顺序调整

**User Story:** 作为视频内容创作者，我希望调整字幕顺序，以便重新组织内容结构。

#### Acceptance Criteria

1. WHEN a user clicks the up arrow for a Subtitle_Segment, THE Subtitle_Editor SHALL swap the segment with the previous segment
2. WHEN a user clicks the down arrow for a Subtitle_Segment, THE Subtitle_Editor SHALL swap the segment with the next segment
3. THE Subtitle_Editor SHALL disable the up arrow for the first Subtitle_Segment
4. THE Subtitle_Editor SHALL disable the down arrow for the last Subtitle_Segment
5. WHEN segments are swapped, THE Subtitle_Editor SHALL also swap their start times to maintain timeline continuity
6. THE Subtitle_Editor SHALL update sequence numbers after each swap operation

### Requirement 9: 视频预览同步

**User Story:** 作为视频内容创作者，我希望在编辑字幕时预览视频效果，以便验证字幕与视频的匹配度。

#### Acceptance Criteria

1. THE Subtitle_Editor SHALL display an HTML5 video player with the current video file
2. WHEN the video player time updates, THE Subtitle_Editor SHALL highlight the currently active Subtitle_Segment in the list
3. WHEN a user clicks on a Subtitle_Segment, THE Subtitle_Editor SHALL seek the video player to that segment's start time
4. THE Subtitle_Editor SHALL overlay the current Subtitle_Segment text on the video player using Canvas
5. THE Subtitle_Editor SHALL render the overlay text using the Subtitle_Style of the current segment
6. WHEN playback reaches the end of a Subtitle_Segment, THE Subtitle_Editor SHALL clear the overlay and prepare for the next segment

### Requirement 10: ASS字幕重新生成

**User Story:** 作为视频内容创作者，我希望将编辑后的字幕保存为ASS文件，以便重新生成视频。

#### Acceptance Criteria

1. WHEN a user clicks the save button, THE Subtitle_Editor SHALL send all Subtitle_Segments to the backend API
2. THE Video_Generator SHALL regenerate the ASS_File using the updated Subtitle_Segments
3. THE Video_Generator SHALL preserve ASS_File header format and style definitions
4. THE Video_Generator SHALL write each Subtitle_Segment as a Dialogue line in the ASS_File
5. WHEN ASS_File generation succeeds, THE Video_Generator SHALL return the ASS_File path to the client
6. IF ASS_File generation fails, THE Video_Generator SHALL return an error message with failure reason

### Requirement 11: 视频重新合成

**User Story:** 作为视频内容创作者，我希望使用编辑后的字幕重新生成视频，以便获得最终成果。

#### Acceptance Criteria

1. WHEN a user clicks the regenerate video button, THE Video_Generator SHALL verify that the ASS_File exists
2. THE Video_Generator SHALL use the Original_Audio file without modification
3. THE Video_Generator SHALL call FFmpeg with the Original_Audio, new ASS_File, and original video configuration
4. THE Video_Generator SHALL generate the output video file with the same resolution and format as the original
5. WHEN video generation completes within 60 seconds, THE Video_Generator SHALL return the video URL to the client
6. IF video generation exceeds 60 seconds, THE Video_Generator SHALL continue processing and allow status polling
7. WHEN video generation succeeds, THE Subtitle_Editor SHALL display a success message with download link
8. IF video generation fails, THE Subtitle_Editor SHALL display an error message with failure reason

### Requirement 12: 编辑历史撤销

**User Story:** 作为视频内容创作者，我希望撤销最近的编辑操作，以便恢复误操作。

#### Acceptance Criteria

1. THE Subtitle_Editor SHALL maintain an edit history stack with a maximum of 20 operations
2. THE Subtitle_Editor SHALL record each edit operation (text change, time change, style change, add, delete, reorder) in the history stack
3. WHEN a user clicks the undo button, THE Subtitle_Editor SHALL revert the most recent operation from the history stack
4. THE Subtitle_Editor SHALL disable the undo button when the history stack is empty
5. WHEN undo is performed, THE Subtitle_Editor SHALL update the display immediately to reflect the previous state
6. THE Subtitle_Editor SHALL clear the history stack when a new Task_ID is loaded

### Requirement 13: 自动保存

**User Story:** 作为视频内容创作者，我希望编辑器自动保存我的修改，以便防止意外丢失数据。

#### Acceptance Criteria

1. WHEN a user makes any edit operation, THE Subtitle_Editor SHALL mark the data as unsaved
2. WHEN unsaved data exists for 30 seconds without new edits, THE Subtitle_Editor SHALL automatically save to browser local storage
3. THE Subtitle_Editor SHALL store the complete Subtitle_Segments data in local storage with the Task_ID as key
4. WHEN a user reopens the same Task_ID within 24 hours, THE Subtitle_Editor SHALL detect local storage data
5. WHEN local storage data is detected, THE Subtitle_Editor SHALL prompt the user to restore unsaved changes
6. WHEN local storage data is older than 24 hours, THE Subtitle_Editor SHALL automatically discard it

### Requirement 14: 字幕导出导入

**User Story:** 作为视频内容创作者，我希望导出字幕为JSON文件并能重新导入，以便备份和共享字幕数据。

#### Acceptance Criteria

1. WHEN a user clicks the export button, THE Subtitle_Editor SHALL generate a JSON file containing all Subtitle_Segments
2. THE Subtitle_Editor SHALL include video metadata (Task_ID, duration, resolution) in the exported JSON file
3. THE Subtitle_Editor SHALL trigger a file download with filename pattern "subtitles_[Task_ID]_[timestamp].json"
4. WHEN a user clicks the import button, THE Subtitle_Editor SHALL display a file picker accepting JSON files
5. WHEN a JSON file is selected, THE Subtitle_Editor SHALL validate the file structure and content
6. WHEN validation succeeds, THE Subtitle_Editor SHALL replace current Subtitle_Segments with imported data
7. IF validation fails, THE Subtitle_Editor SHALL display an error message specifying the validation error

### Requirement 15: 键盘快捷键

**User Story:** 作为视频内容创作者，我希望使用键盘快捷键快速操作，以便提高编辑效率。

#### Acceptance Criteria

1. WHEN a user presses Ctrl+S (or Cmd+S on macOS), THE Subtitle_Editor SHALL save all changes immediately
2. WHEN a user presses Ctrl+Z (or Cmd+Z on macOS), THE Subtitle_Editor SHALL perform undo operation
3. WHEN a user presses Space bar, THE Subtitle_Editor SHALL toggle video playback (play/pause)
4. WHEN a user presses Left Arrow, THE Subtitle_Editor SHALL seek video backward 2 seconds
5. WHEN a user presses Right Arrow, THE Subtitle_Editor SHALL seek video forward 2 seconds
6. WHEN a user presses Up Arrow in the subtitle list, THE Subtitle_Editor SHALL move selection to the previous segment
7. WHEN a user presses Down Arrow in the subtitle list, THE Subtitle_Editor SHALL move selection to the next segment
8. WHEN a user presses Enter while a segment is selected, THE Subtitle_Editor SHALL open the edit dialog for that segment

### Requirement 16: 性能要求

**User Story:** 作为视频内容创作者，我希望编辑器响应快速流畅，以便获得良好的用户体验。

#### Acceptance Criteria

1. WHEN loading a video with 100 Subtitle_Segments, THE Subtitle_Editor SHALL render the complete list within 1 second
2. WHEN a user edits a Subtitle_Segment, THE Subtitle_Editor SHALL update the display within 100 milliseconds
3. WHEN regenerating the ASS_File, THE Video_Generator SHALL complete processing within 2 seconds for 100 Subtitle_Segments
4. WHEN regenerating a 1-minute video, THE Video_Generator SHALL complete FFmpeg processing within 30 seconds
5. THE Subtitle_Editor SHALL maintain 60 frames per second animation performance during video playback with subtitle overlay
6. WHEN auto-saving to local storage, THE Subtitle_Editor SHALL complete the save operation within 200 milliseconds

### Requirement 17: 错误处理

**User Story:** 作为视频内容创作者，我希望系统能够优雅处理错误情况，以便我了解问题并采取措施。

#### Acceptance Criteria

1. WHEN the backend API returns an error status code, THE Subtitle_Editor SHALL display a user-friendly error message
2. WHEN video file fails to load, THE Subtitle_Editor SHALL display an error message and provide retry option
3. WHEN FFmpeg processing fails, THE Video_Generator SHALL log the error details and return a descriptive error message
4. WHEN ASS_File generation fails due to invalid characters, THE Video_Generator SHALL identify the problematic Subtitle_Segment
5. WHEN network connection is lost during save operation, THE Subtitle_Editor SHALL cache the changes and retry when connection is restored
6. THE Subtitle_Editor SHALL display all error messages in Chinese with clear description of the problem and suggested actions

### Requirement 18: 浏览器兼容性

**User Story:** 作为视频内容创作者，我希望编辑器在主流浏览器上都能正常工作，以便我使用习惯的浏览器。

#### Acceptance Criteria

1. THE Subtitle_Editor SHALL function correctly on Chrome version 90 or higher
2. THE Subtitle_Editor SHALL function correctly on Edge version 90 or higher
3. THE Subtitle_Editor SHALL function correctly on Firefox version 88 or higher
4. THE Subtitle_Editor SHALL degrade gracefully on Safari version 14 or higher (video overlay may have reduced functionality)
5. WHEN accessing from an unsupported browser, THE Subtitle_Editor SHALL display a warning message recommending supported browsers
6. THE Subtitle_Editor SHALL use HTML5 Canvas for subtitle overlay, falling back to DOM overlay if Canvas is not supported

### Requirement 19: ASS字幕解析器

**User Story:** 作为系统开发者，我希望实现ASS字幕文件解析器，以便从已生成的ASS文件中读取字幕数据供编辑器使用。

#### Acceptance Criteria

1. THE Video_Generator SHALL provide a parser that reads ASS_File content
2. THE parser SHALL extract all Dialogue lines from the ASS_File Events section
3. THE parser SHALL parse each Dialogue line into Subtitle_Segment components (start time, end time, style name, text)
4. THE parser SHALL convert ASS time format (H:MM:SS.ms) to seconds with decimal precision
5. THE parser SHALL extract style definitions from the ASS_File Styles section
6. THE parser SHALL map style names to Subtitle_Style properties (font name, font size, colors, border width)
7. THE parser SHALL handle ASS special codes (animation tags, positioning codes) by stripping them from display text
8. WHEN parsing encounters malformed ASS syntax, THE parser SHALL skip the invalid line and continue processing
9. THE parser SHALL return a JSON array of Subtitle_Segments with all properties populated

### Requirement 20: ASS字幕格式化器（Pretty Printer）

**User Story:** 作为系统开发者，我希望实现ASS字幕文件格式化器，以便将编辑后的字幕数据写回标准格式的ASS文件。

#### Acceptance Criteria

1. THE Video_Generator SHALL provide a formatter that generates ASS_File content from Subtitle_Segments
2. THE formatter SHALL generate the ASS_File header with standard Script Info section
3. THE formatter SHALL generate the Styles section with all Subtitle_Style definitions
4. THE formatter SHALL generate the Events section with all Subtitle_Segments as Dialogue lines
5. THE formatter SHALL convert start time and duration from seconds to ASS time format (H:MM:SS.ms)
6. THE formatter SHALL wrap animation tags with curly braces according to ASS specification
7. THE formatter SHALL escape special characters in subtitle text (backslash, curly braces)
8. THE formatter SHALL maintain consistent field ordering in Dialogue lines (Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text)
9. THE formatter SHALL output UTF-8 encoded text with BOM header

### Requirement 21: 字幕数据往返一致性

**User Story:** 作为质量保证人员，我希望验证ASS解析和格式化的往返一致性，以便确保数据不会丢失或损坏。

#### Acceptance Criteria

1. FOR ALL valid ASS_Files, WHEN the parser reads an ASS_File and the formatter writes the result back to a new ASS_File, THEN parsing the new file SHALL produce equivalent Subtitle_Segments
2. THE system SHALL maintain text content exactly during round-trip (parse → format → parse)
3. THE system SHALL maintain timing values within 0.01 second precision during round-trip
4. THE system SHALL maintain style properties exactly during round-trip for supported attributes
5. THE system SHALL preserve the sequence order of all Subtitle_Segments during round-trip
6. WHEN animation tags are present, THE system SHALL preserve the tag type and parameters during round-trip

## Non-Functional Requirements

### Usability

1. THE Subtitle_Editor SHALL provide inline help tooltips for all configuration options
2. THE Subtitle_Editor SHALL use intuitive icons and labels in Chinese for all UI elements
3. THE Subtitle_Editor SHALL provide visual feedback (loading spinner, progress bar) for long-running operations
4. THE Subtitle_Editor SHALL maintain consistent color scheme and typography throughout the interface

### Reliability

1. THE Video_Generator SHALL handle concurrent requests from multiple users without data corruption
2. THE system SHALL maintain data integrity when unexpected failures occur during video generation
3. THE system SHALL provide transaction rollback capability if video regeneration fails after ASS_File update

### Security

1. THE system SHALL validate all user input to prevent XSS attacks in subtitle text
2. THE system SHALL restrict file access to only the Task_ID owned by the current user
3. THE system SHALL sanitize file paths to prevent directory traversal attacks
4. THE system SHALL limit uploaded JSON file size to 5 MB to prevent denial of service

### Maintainability

1. THE Subtitle_Editor frontend code SHALL be organized into modular components (list view, editor dialog, style panel, video player)
2. THE backend API SHALL follow RESTful conventions with clear endpoint naming
3. THE system SHALL log all API operations with timestamp, user ID, and operation type for debugging
4. THE system SHALL provide comprehensive error messages in logs for troubleshooting

