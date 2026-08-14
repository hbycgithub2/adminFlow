/**
 * 字幕编辑器专业版 - 实时预览功能
 * 
 * 核心功能：
 * 1. HTML5 Video播放器
 * 2. Canvas字幕实时渲染
 * 3. 样式编辑实时同步
 * 4. 所见即所得编辑体验
 * 
 * @author Kiro
 * @since 2026-08-14
 */

class SubtitleEditorPro {
    constructor() {
        // 数据
        this.taskId = '';
        this.subtitles = [];
        this.currentSubtitleIndex = -1;
        this.videoUrl = '';
        this.totalDuration = 0;

        // DOM元素
        this.video = document.getElementById('videoPlayer');
        this.canvas = document.getElementById('subtitleCanvas');
        this.ctx = this.canvas.getContext('2d');
        this.playBtn = document.getElementById('playBtn');
        this.progressBar = document.getElementById('progressBar');
        this.progressFill = document.getElementById('progressFill');
        this.progressHandle = document.getElementById('progressHandle');

        // 初始化
        this.init();
    }

    init() {
        // 监听视频事件
        this.video.addEventListener('loadedmetadata', () => this.onVideoLoaded());
        this.video.addEventListener('timeupdate', () => this.onTimeUpdate());
        this.video.addEventListener('play', () => this.onPlay());
        this.video.addEventListener('pause', () => this.onPause());
        this.video.addEventListener('ended', () => this.onEnded());

        // 监听进度条拖动
        this.progressBar.addEventListener('click', (e) => this.onProgressClick(e));
        this.progressHandle.addEventListener('mousedown', (e) => this.onHandleMouseDown(e));

        // 页面加载时自动读取URL参数
        const urlParams = new URLSearchParams(window.location.search);
        const taskId = urlParams.get('taskId');
        if (taskId) {
            document.getElementById('taskIdInput').value = taskId;
            setTimeout(() => this.loadSubtitles(), 500);
        }
    }

    // ==================== 加载数据 ====================

    async loadSubtitles() {
        const taskId = document.getElementById('taskIdInput').value.trim();
        if (!taskId) {
            this.showToast('请输入任务ID', 'error');
            return;
        }

        this.showLoading('加载字幕中...');

        try {
            const response = await fetch(`/api/subtitle-editor/load?taskId=${taskId}`);
            if (!response.ok) {
                throw new Error('加载失败');
            }

            const data = await response.json();
            this.taskId = taskId;
            this.subtitles = data.subtitles;
            this.videoUrl = data.videoUrl;
            this.totalDuration = data.totalDuration;

            // 加载视频
            this.video.src = this.videoUrl;
            this.video.style.display = 'block';
            document.getElementById('emptyVideo').style.display = 'none';

            // 渲染字幕列表
            this.renderSubtitleList();

            // 启用按钮
            document.getElementById('saveBtn').disabled = false;
            document.getElementById('regenerateBtn').disabled = false;
            this.playBtn.disabled = false;
            document.querySelectorAll('.control-btn').forEach(btn => btn.disabled = false);

            this.showToast(`成功加载 ${this.subtitles.length} 条字幕`, 'success');
        } catch (error) {
            this.showToast('加载字幕失败: ' + error.message, 'error');
        } finally {
            this.hideLoading();
        }
    }

    onVideoLoaded() {
        // 设置Canvas尺寸与视频一致
        const videoRect = this.video.getBoundingClientRect();
        this.canvas.width = this.video.videoWidth || 1280;
        this.canvas.height = this.video.videoHeight || 720;
        this.canvas.style.width = videoRect.width + 'px';
        this.canvas.style.height = videoRect.height + 'px';

        console.log('视频加载完成:', {
            videoWidth: this.video.videoWidth,
            videoHeight: this.video.videoHeight,
            duration: this.video.duration
        });

        // 初始渲染第一帧字幕
        this.renderSubtitle();
    }

    // ==================== 视频播放控制 ====================

    togglePlay() {
        if (this.video.paused) {
            this.video.play();
        } else {
            this.video.pause();
        }
    }

    onPlay() {
        this.playBtn.innerHTML = '⏸';
    }

    onPause() {
        this.playBtn.innerHTML = '▶';
    }

    onEnded() {
        this.playBtn.innerHTML = '▶';
    }

    skipBackward() {
        this.video.currentTime = Math.max(0, this.video.currentTime - 5);
    }

    skipForward() {
        this.video.currentTime = Math.min(this.video.duration, this.video.currentTime + 5);
    }

    onTimeUpdate() {
        // 更新进度条
        const progress = (this.video.currentTime / this.video.duration) * 100;
        this.progressFill.style.width = progress + '%';
        this.progressHandle.style.left = progress + '%';

        // 更新时间显示
        const currentTime = this.formatTime(this.video.currentTime);
        const duration = this.formatTime(this.video.duration);
        document.getElementById('timeDisplay').textContent = `${currentTime} / ${duration}`;

        // 渲染当前字幕
        this.renderSubtitle();

        // 高亮当前字幕
        this.highlightCurrentSubtitle();
    }

    onProgressClick(e) {
        const rect = this.progressBar.getBoundingClientRect();
        const percent = (e.clientX - rect.left) / rect.width;
        this.video.currentTime = percent * this.video.duration;
    }

    onHandleMouseDown(e) {
        e.preventDefault();
        const onMouseMove = (moveEvent) => {
            const rect = this.progressBar.getBoundingClientRect();
            let percent = (moveEvent.clientX - rect.left) / rect.width;
            percent = Math.max(0, Math.min(1, percent));
            this.video.currentTime = percent * this.video.duration;
        };

        const onMouseUp = () => {
            document.removeEventListener('mousemove', onMouseMove);
            document.removeEventListener('mouseup', onMouseUp);
        };

        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
    }

    formatTime(seconds) {
        if (isNaN(seconds)) return '0:00';
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    }

    // ==================== 字幕渲染（核心） ====================

    renderSubtitle() {
        // 清空画布
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        // 查找当前时间对应的字幕
        const currentTime = this.video.currentTime;
        const subtitle = this.findCurrentSubtitle(currentTime);

        if (!subtitle) return;

        // 应用样式并绘制
        this.drawSubtitle(subtitle, currentTime);
    }

    findCurrentSubtitle(time) {
        return this.subtitles.find(sub => {
            const start = sub.startTime;
            const end = sub.startTime + sub.duration;
            return time >= start && time < end;
        });
    }

    drawSubtitle(subtitle, currentTime) {
        const style = subtitle.style;
        const canvas = this.canvas;
        const ctx = this.ctx;

        // 计算动画进度
        const elapsed = currentTime - subtitle.startTime;
        const progress = Math.min(elapsed / 0.3, 1); // 0.3秒动画时间

        // 设置字体
        ctx.font = `${style.fontSize}px ${style.fontName}`;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';

        // 计算文本位置
        const pos = this.calculatePosition(style.position, canvas.width, canvas.height);
        let x = pos.x;
        let y = pos.y;

        // 应用动画效果
        const animatedPos = this.applyAnimation(style.animationType, x, y, progress, canvas);
        x = animatedPos.x;
        y = animatedPos.y;

        // 绘制边框（描边）
        ctx.strokeStyle = style.borderColor;
        ctx.lineWidth = style.borderWidth;
        ctx.strokeText(subtitle.text, x, y);

        // 绘制字体
        ctx.fillStyle = style.fontColor;
        ctx.fillText(subtitle.text, x, y);
    }

    calculatePosition(position, width, height) {
        const padding = 50;
        const positions = {
            1: { x: padding, y: height - padding },                    // 左下
            2: { x: width / 2, y: height - padding },                  // 底部居中
            3: { x: width - padding, y: height - padding },            // 右下
            4: { x: padding, y: height / 2 },                          // 左中
            5: { x: width / 2, y: height / 2 },                        // 正中
            6: { x: width - padding, y: height / 2 },                  // 右中
            7: { x: padding, y: padding },                             // 左上
            8: { x: width / 2, y: padding },                           // 顶部居中
            9: { x: width - padding, y: padding }                      // 右上
        };
        return positions[position] || positions[2]; // 默认底部居中
    }

    applyAnimation(type, x, y, progress, canvas) {
        switch (type) {
            case 'slide_up':
                return { x, y: y + (1 - progress) * 100 };
            case 'slide_down':
                return { x, y: y - (1 - progress) * 100 };
            case 'slide_left':
                return { x: x + (1 - progress) * 200, y };
            case 'slide_right':
                return { x: x - (1 - progress) * 200, y };
            case 'zoom_in':
                this.ctx.globalAlpha = progress;
                this.ctx.save();
                this.ctx.translate(x, y);
                this.ctx.scale(progress, progress);
                this.ctx.translate(-x, -y);
                return { x, y };
            case 'fade':
                this.ctx.globalAlpha = progress;
                return { x, y };
            case 'bounce':
                const bounce = progress < 0.5 ? 
                    Math.sin(progress * Math.PI * 4) * 20 : 0;
                return { x, y: y + bounce };
            case 'none':
            default:
                return { x, y };
        }
    }

    // ==================== 字幕列表 ====================

    renderSubtitleList() {
        const listEl = document.getElementById('subtitleList');
        listEl.innerHTML = '';

        this.subtitles.forEach((subtitle, index) => {
            const item = document.createElement('div');
            item.className = 'subtitle-item';
            item.onclick = () => this.selectSubtitle(index);

            const header = document.createElement('div');
            header.className = 'subtitle-item-header';

            const id = document.createElement('span');
            id.className = 'subtitle-id';
            id.textContent = `#${subtitle.id}`;

            const time = document.createElement('span');
            time.className = 'subtitle-time';
            time.textContent = `${subtitle.startTime.toFixed(1)}s - ${subtitle.duration.toFixed(1)}s`;

            header.appendChild(id);
            header.appendChild(time);

            const text = document.createElement('div');
            text.className = 'subtitle-text';
            text.textContent = subtitle.text;

            item.appendChild(header);
            item.appendChild(text);
            listEl.appendChild(item);
        });
    }

    selectSubtitle(index) {
        this.currentSubtitleIndex = index;
        const subtitle = this.subtitles[index];

        // 更新列表高亮
        document.querySelectorAll('.subtitle-item').forEach((item, i) => {
            item.classList.toggle('active', i === index);
        });

        // 显示样式编辑器
        document.getElementById('styleEditor').style.display = 'block';

        // 填充表单
        document.getElementById('editSubtitleId').textContent = subtitle.id;
        document.getElementById('editText').value = subtitle.text;
        document.getElementById('editStartTime').value = subtitle.startTime;
        document.getElementById('editDuration').value = subtitle.duration;
        document.getElementById('editFontName').value = subtitle.style.fontName;
        document.getElementById('editFontSize').value = subtitle.style.fontSize;
        document.getElementById('fontSizeValue').textContent = subtitle.style.fontSize;
        document.getElementById('editFontColor').value = subtitle.style.fontColor;
        document.getElementById('editBorderColor').value = subtitle.style.borderColor;
        document.getElementById('editBorderWidth').value = subtitle.style.borderWidth;
        document.getElementById('editAnimationType').value = subtitle.style.animationType;

        // 更新位置按钮
        this.updatePositionButtons(subtitle.style.position);

        // 跳转到该字幕的时间
        this.video.currentTime = subtitle.startTime;
    }

    highlightCurrentSubtitle() {
        const currentTime = this.video.currentTime;
        const index = this.subtitles.findIndex(sub => {
            return currentTime >= sub.startTime && currentTime < (sub.startTime + sub.duration);
        });

        if (index !== -1 && index !== this.currentSubtitleIndex) {
            // 自动选中当前字幕（但不跳转视频时间）
            const items = document.querySelectorAll('.subtitle-item');
            items.forEach((item, i) => {
                if (i === index) {
                    item.style.background = '#2a2a2a';
                } else {
                    item.style.background = '#1a1a1a';
                }
            });

            // 滚动到当前字幕
            if (items[index]) {
                items[index].scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
        }
    }

    // ==================== 样式编辑（实时同步） ====================

    onStyleChange() {
        if (this.currentSubtitleIndex === -1) return;

        const subtitle = this.subtitles[this.currentSubtitleIndex];

        // 更新数据
        subtitle.text = document.getElementById('editText').value;
        subtitle.startTime = parseFloat(document.getElementById('editStartTime').value);
        subtitle.duration = parseFloat(document.getElementById('editDuration').value);
        subtitle.style.fontName = document.getElementById('editFontName').value;
        subtitle.style.fontSize = parseInt(document.getElementById('editFontSize').value);
        subtitle.style.fontColor = document.getElementById('editFontColor').value;
        subtitle.style.borderColor = document.getElementById('editBorderColor').value;
        subtitle.style.borderWidth = parseInt(document.getElementById('editBorderWidth').value);
        subtitle.style.animationType = document.getElementById('editAnimationType').value;

        // 立即刷新预览
        this.renderSubtitle();

        // 更新列表
        this.renderSubtitleList();
        document.querySelectorAll('.subtitle-item')[this.currentSubtitleIndex].classList.add('active');
    }

    onFontSizeChange() {
        const value = document.getElementById('editFontSize').value;
        document.getElementById('fontSizeValue').textContent = value;
        this.onStyleChange();
    }

    setPosition(position) {
        if (this.currentSubtitleIndex === -1) return;

        this.subtitles[this.currentSubtitleIndex].style.position = position;
        this.updatePositionButtons(position);
        this.renderSubtitle();
    }

    updatePositionButtons(activePosition) {
        document.querySelectorAll('.position-btn').forEach(btn => {
            const pos = parseInt(btn.dataset.position);
            btn.classList.toggle('active', pos === activePosition);
        });
    }

    // ==================== 保存和重新生成 ====================

    async saveSubtitles() {
        if (!this.taskId) {
            this.showToast('请先加载字幕', 'error');
            return;
        }

        this.showLoading('保存中...');

        try {
            const response = await fetch('/api/subtitle-editor/update', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    taskId: this.taskId,
                    subtitles: this.subtitles,
                    regenerateVideo: false
                })
            });

            const result = await response.json();

            if (result.success) {
                this.showToast(result.message, 'success');
            } else {
                this.showToast(result.message, 'error');
            }
        } catch (error) {
            this.showToast('保存失败: ' + error.message, 'error');
        } finally {
            this.hideLoading();
        }
    }

    async regenerateVideo() {
        if (!this.taskId) {
            this.showToast('请先加载字幕', 'error');
            return;
        }

        if (!confirm('确定要重新生成视频吗？这可能需要30-60秒')) {
            return;
        }

        this.showLoading('重新生成视频中，请稍候...');

        try {
            const response = await fetch('/api/subtitle-editor/update', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    taskId: this.taskId,
                    subtitles: this.subtitles,
                    regenerateVideo: true
                })
            });

            const result = await response.json();

            if (result.success) {
                this.showToast(`${result.message}，耗时${result.duration.toFixed(2)}秒`, 'success');
                
                // 重新加载视频
                this.video.src = result.videoUrl + '?t=' + Date.now();
                this.video.load();
            } else {
                this.showToast(result.message, 'error');
            }
        } catch (error) {
            this.showToast('重新生成视频失败: ' + error.message, 'error');
        } finally {
            this.hideLoading();
        }
    }

    // ==================== UI辅助函数 ====================

    showLoading(text = '处理中...') {
        document.getElementById('loadingText').textContent = text;
        document.getElementById('loadingOverlay').style.display = 'flex';
    }

    hideLoading() {
        document.getElementById('loadingOverlay').style.display = 'none';
    }

    showToast(message, type = 'success') {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.className = `toast ${type}`;
        toast.style.display = 'block';

        setTimeout(() => {
            toast.style.display = 'none';
        }, 3000);
    }
}

// 初始化应用
const app = new SubtitleEditorPro();
