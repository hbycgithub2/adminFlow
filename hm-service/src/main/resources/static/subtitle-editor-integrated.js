/**
 * 字幕编辑器专业版 - 集成版
 * 集成到 video-generator-test.html 页面中
 */

// 编辑器状态
let proSubtitles = [];
let proCurrentSubtitleIndex = -1;
let proVideo = null;
let proCanvas = null;
let proCtx = null;
let proRenderInterval = null;

// 初始化专业版编辑器
function initProEditor() {
    proVideo = document.getElementById('editorVideo');
    proCanvas = document.getElementById('subtitleCanvas');
    proCtx = proCanvas.getContext('2d');
    
    // 监听视频事件
    proVideo.addEventListener('loadedmetadata', onProVideoLoaded);
    proVideo.addEventListener('timeupdate', onProTimeUpdate);
    proVideo.addEventListener('play', () => startProRenderLoop());
    proVideo.addEventListener('pause', () => stopProRenderLoop());
    proVideo.addEventListener('seeked', () => renderProSubtitle());
    
    // 监听进度条
    const progressBar = document.getElementById('editorProgressBar');
    progressBar.addEventListener('click', onProProgressClick);
    
    const handle = document.getElementById('editorProgressHandle');
    handle.addEventListener('mousedown', onProHandleMouseDown);
}

function onProVideoLoaded() {
    // 设置Canvas尺寸与视频一致
    proCanvas.width = proVideo.videoWidth || 1280;
    proCanvas.height = proVideo.videoHeight || 720;
    
    const videoRect = proVideo.getBoundingClientRect();
    proCanvas.style.width = videoRect.width + 'px';
    proCanvas.style.height = videoRect.height + 'px';
    proCanvas.style.position = 'absolute';
    proCanvas.style.top = '0';
    proCanvas.style.left = '0';
    
    console.log('专业版视频加载完成:', {
        videoWidth: proVideo.videoWidth,
        videoHeight: proVideo.videoHeight,
        duration: proVideo.duration
    });
    
    renderProSubtitle();
}

// ==================== 字幕渲染（核心） ====================

function startProRenderLoop() {
    if (proRenderInterval) return;
    proRenderInterval = setInterval(() => {
        if (proVideo && !proVideo.paused) {
            renderProSubtitle();
        }
    }, 50); // 每50ms渲染一次
}

function stopProRenderLoop() {
    if (proRenderInterval) {
        clearInterval(proRenderInterval);
        proRenderInterval = null;
    }
}

function renderProSubtitle() {
    if (!proCtx || !proCanvas) return;
    
    // 清空画布
    proCtx.clearRect(0, 0, proCanvas.width, proCanvas.height);
    
    // 查找当前时间对应的字幕
    const currentTime = proVideo.currentTime;
    const subtitle = findProCurrentSubtitle(currentTime);
    
    if (!subtitle) return;
    
    // 应用样式并绘制
    drawProSubtitle(subtitle, currentTime);
}

function findProCurrentSubtitle(time) {
    return proSubtitles.find(sub => {
        const start = sub.startTime;
        const end = sub.startTime + sub.duration;
        return time >= start && time < end;
    });
}

function drawProSubtitle(subtitle, currentTime) {
    const style = subtitle.style;
    const canvas = proCanvas;
    const ctx = proCtx;
    
    // 计算动画进度
    const elapsed = currentTime - subtitle.startTime;
    const progress = Math.min(elapsed / 0.3, 1); // 0.3秒动画时间
    
    // 设置字体
    ctx.font = `${style.fontSize}px ${style.fontName}`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    
    // 计算文本位置
    const pos = calculateProPosition(style.position, canvas.width, canvas.height);
    let x = pos.x;
    let y = pos.y;
    
    // 应用动画效果
    const animatedPos = applyProAnimation(style.animationType, x, y, progress, canvas, ctx);
    x = animatedPos.x;
    y = animatedPos.y;
    
    // 绘制边框（描边）
    ctx.strokeStyle = style.borderColor;
    ctx.lineWidth = style.borderWidth;
    ctx.strokeText(subtitle.text, x, y);
    
    // 绘制字体
    ctx.fillStyle = style.fontColor;
    ctx.fillText(subtitle.text, x, y);
    
    // 重置透明度和变换
    ctx.globalAlpha = 1.0;
    ctx.restore();
}

function calculateProPosition(position, width, height) {
    const padding = 50;
    const positions = {
        1: { x: padding, y: height - padding },
        2: { x: width / 2, y: height - padding },
        3: { x: width - padding, y: height - padding },
        4: { x: padding, y: height / 2 },
        5: { x: width / 2, y: height / 2 },
        6: { x: width - padding, y: height / 2 },
        7: { x: padding, y: padding },
        8: { x: width / 2, y: padding },
        9: { x: width - padding, y: padding }
    };
    return positions[position] || positions[2];
}

function applyProAnimation(type, x, y, progress, canvas, ctx) {
    ctx.save();
    
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
            ctx.globalAlpha = progress;
            ctx.translate(x, y);
            ctx.scale(progress, progress);
            ctx.translate(-x, -y);
            return { x, y };
        case 'fade':
            ctx.globalAlpha = progress;
            return { x, y };
        case 'bounce':
            const bounce = progress < 0.5 ? Math.sin(progress * Math.PI * 4) * 20 : 0;
            return { x, y: y + bounce };
        case 'none':
        default:
            return { x, y };
    }
}

// ==================== 字幕列表 ====================

function renderProSubtitleList() {
    const listEl = document.getElementById('proSubtitleList');
    listEl.innerHTML = '';
    
    if (proSubtitles.length === 0) {
        listEl.innerHTML = '<div style="color: #666; text-align: center; padding: 40px 20px;"><p>暂无字幕</p></div>';
        return;
    }
    
    proSubtitles.forEach((subtitle, index) => {
        const item = document.createElement('div');
        item.className = 'pro-subtitle-item';
        item.onclick = () => selectProSubtitle(index);
        
        const header = document.createElement('div');
        header.className = 'pro-subtitle-item-header';
        
        const id = document.createElement('span');
        id.className = 'pro-subtitle-id';
        id.textContent = `#${subtitle.id || (index + 1)}`;
        
        const time = document.createElement('span');
        time.className = 'pro-subtitle-time';
        time.textContent = `${subtitle.startTime.toFixed(1)}s ~ ${(subtitle.startTime + subtitle.duration).toFixed(1)}s`;
        
        header.appendChild(id);
        header.appendChild(time);
        
        const text = document.createElement('div');
        text.className = 'pro-subtitle-text';
        text.textContent = subtitle.text;
        
        item.appendChild(header);
        item.appendChild(text);
        listEl.appendChild(item);
    });
}

function selectProSubtitle(index) {
    proCurrentSubtitleIndex = index;
    const subtitle = proSubtitles[index];
    
    // 更新列表高亮
    document.querySelectorAll('.pro-subtitle-item').forEach((item, i) => {
        item.classList.toggle('active', i === index);
    });
    
    // 显示样式编辑器
    document.getElementById('proStyleEditor').style.display = 'block';
    
    // 填充表单
    document.getElementById('proEditSubtitleId').textContent = subtitle.id || (index + 1);
    document.getElementById('proEditText').value = subtitle.text;
    document.getElementById('proEditStartTime').value = subtitle.startTime;
    document.getElementById('proEditDuration').value = subtitle.duration;
    document.getElementById('proEditFontName').value = subtitle.style.fontName;
    document.getElementById('proEditFontSize').value = subtitle.style.fontSize;
    document.getElementById('proFontSizeValue').textContent = subtitle.style.fontSize;
    document.getElementById('proEditFontColor').value = subtitle.style.fontColor;
    document.getElementById('proEditBorderColor').value = subtitle.style.borderColor;
    document.getElementById('proEditBorderWidth').value = subtitle.style.borderWidth;
    document.getElementById('proEditAnimationType').value = subtitle.style.animationType;
    
    // 更新位置按钮
    updateProPositionButtons(subtitle.style.position);
    
    // 跳转到该字幕的时间
    proVideo.currentTime = subtitle.startTime;
}

function highlightProCurrentSubtitle() {
    const currentTime = proVideo.currentTime;
    const index = proSubtitles.findIndex(sub => {
        return currentTime >= sub.startTime && currentTime < (sub.startTime + sub.duration);
    });
    
    if (index !== -1 && index !== proCurrentSubtitleIndex) {
        const items = document.querySelectorAll('.pro-subtitle-item');
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

function onProStyleChange() {
    if (proCurrentSubtitleIndex === -1) return;
    
    const subtitle = proSubtitles[proCurrentSubtitleIndex];
    
    // 更新数据
    subtitle.text = document.getElementById('proEditText').value;
    subtitle.startTime = parseFloat(document.getElementById('proEditStartTime').value);
    subtitle.duration = parseFloat(document.getElementById('proEditDuration').value);
    subtitle.style.fontName = document.getElementById('proEditFontName').value;
    subtitle.style.fontSize = parseInt(document.getElementById('proEditFontSize').value);
    subtitle.style.fontColor = document.getElementById('proEditFontColor').value;
    subtitle.style.borderColor = document.getElementById('proEditBorderColor').value;
    subtitle.style.borderWidth = parseInt(document.getElementById('proEditBorderWidth').value);
    subtitle.style.animationType = document.getElementById('proEditAnimationType').value;
    
    // 立即刷新预览
    renderProSubtitle();
    
    // 更新列表
    renderProSubtitleList();
    document.querySelectorAll('.pro-subtitle-item')[proCurrentSubtitleIndex].classList.add('active');
}

function onProFontSizeChange() {
    const value = document.getElementById('proEditFontSize').value;
    document.getElementById('proFontSizeValue').textContent = value;
    onProStyleChange();
}

function setProPosition(position) {
    if (proCurrentSubtitleIndex === -1) return;
    
    proSubtitles[proCurrentSubtitleIndex].style.position = position;
    updateProPositionButtons(position);
    renderProSubtitle();
}

function updateProPositionButtons(activePosition) {
    document.querySelectorAll('.pro-position-btn').forEach(btn => {
        const pos = parseInt(btn.dataset.position);
        btn.classList.toggle('active', pos === activePosition);
    });
}

// ==================== 视频播放控制 ====================

function editorTogglePlay() {
    if (!proVideo) return;
    
    if (proVideo.paused) {
        proVideo.play();
    } else {
        proVideo.pause();
    }
}

function editorSkipBackward() {
    if (!proVideo) return;
    proVideo.currentTime = Math.max(0, proVideo.currentTime - 5);
}

function editorSkipForward() {
    if (!proVideo) return;
    proVideo.currentTime = Math.min(proVideo.duration, proVideo.currentTime + 5);
}

function onProTimeUpdate() {
    if (!proVideo) return;
    
    // 更新进度条
    const progress = (proVideo.currentTime / proVideo.duration) * 100;
    document.getElementById('editorProgressFill').style.width = progress + '%';
    document.getElementById('editorProgressHandle').style.left = progress + '%';
    
    // 更新时间显示
    const currentTime = formatProTime(proVideo.currentTime);
    const duration = formatProTime(proVideo.duration);
    document.getElementById('editorTimeDisplay').textContent = `${currentTime} / ${duration}`;
    
    // 更新播放按钮
    document.getElementById('editorPlayBtn').innerHTML = proVideo.paused ? '▶' : '⏸';
    
    // 渲染当前字幕
    renderProSubtitle();
    
    // 高亮当前字幕
    highlightProCurrentSubtitle();
}

function onProProgressClick(e) {
    if (!proVideo) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const percent = (e.clientX - rect.left) / rect.width;
    proVideo.currentTime = percent * proVideo.duration;
}

function onProHandleMouseDown(e) {
    if (!proVideo) return;
    e.preventDefault();
    
    const progressBar = document.getElementById('editorProgressBar');
    
    const onMouseMove = (moveEvent) => {
        const rect = progressBar.getBoundingClientRect();
        let percent = (moveEvent.clientX - rect.left) / rect.width;
        percent = Math.max(0, Math.min(1, percent));
        proVideo.currentTime = percent * proVideo.duration;
    };
    
    const onMouseUp = () => {
        document.removeEventListener('mousemove', onMouseMove);
        document.removeEventListener('mouseup', onMouseUp);
    };
    
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
}

function formatProTime(seconds) {
    if (isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

// ==================== 保存和重新生成 ====================

async function saveAllSubtitles() {
    if (!window.currentTaskId) {
        showStatus('无效的任务ID', 'error');
        return;
    }
    
    if (proSubtitles.length === 0) {
        showStatus('没有字幕可保存', 'error');
        return;
    }
    
    showStatus('正在保存字幕修改...', 'info');
    
    try {
        const response = await fetch('/api/subtitle-editor/update', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                taskId: window.currentTaskId,
                subtitles: proSubtitles,
                regenerateVideo: false
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            showStatus('字幕修改已保存！', 'success');
        } else {
            showStatus('保存失败：' + result.message, 'error');
        }
    } catch (error) {
        showStatus('保存失败：' + error.message, 'error');
        console.error('保存失败:', error);
    }
}

async function regenerateVideo() {
    if (!window.currentTaskId) {
        showStatus('无效的任务ID', 'error');
        return;
    }
    
    if (!confirm('确定要重新生成视频吗？这可能需要30-60秒')) {
        return;
    }
    
    showStatus('正在重新生成视频，请稍候...', 'info');
    
    try {
        const response = await fetch('/api/subtitle-editor/update', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                taskId: window.currentTaskId,
                subtitles: proSubtitles,
                regenerateVideo: true
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            // 更新视频URL
            const newVideoUrl = result.videoUrl || result.data?.newVideoUrl;
            if (newVideoUrl) {
                currentVideoUrl = newVideoUrl;
                document.getElementById('video').src = currentVideoUrl;
                proVideo.src = currentVideoUrl + '?t=' + Date.now();
                proVideo.load();
            }
            
            showStatus(`视频重新生成成功！耗时${result.duration?.toFixed(2) || 0}秒`, 'success');
        } else {
            showStatus('重新生成失败：' + result.message, 'error');
        }
    } catch (error) {
        showStatus('重新生成失败：' + error.message, 'error');
        console.error('重新生成失败:', error);
    }
}
