@echo off
chcp 65001 >nul
echo ========================================
echo WhisperX测试脚本
echo ========================================
echo.

echo [测试1] 检查Python环境...
py --version
if errorlevel 1 (
    echo ❌ Python未安装
    pause
    exit /b 1
)
echo ✅ Python已安装
echo.

echo [测试2] 检查WhisperX是否安装...
py -c "import whisperx; print('✅ WhisperX已安装')"
if errorlevel 1 (
    echo ❌ WhisperX未安装，请先运行 install_whisperx.bat
    pause
    exit /b 1
)
echo.

echo [测试3] 检查GPU是否可用...
py -c "import torch; print('✅ GPU可用：' + torch.cuda.get_device_name(0) if torch.cuda.is_available() else '⚠️ GPU不可用（将使用CPU）')"
echo.

echo [测试4] 检查FFmpeg是否可用...
D:\ai\codex\ffmpeg-9.0.1-essentials_build\bin\ffmpeg.exe -version >nul 2>&1
if errorlevel 1 (
    echo ❌ FFmpeg不可用
    pause
    exit /b 1
)
echo ✅ FFmpeg已安装
echo.

echo [测试5] 检查WhisperX脚本是否存在...
if not exist "D:\code\adminFlow\scripts\whisperx_align.py" (
    echo ❌ whisperx_align.py不存在
    pause
    exit /b 1
)
echo ✅ whisperx_align.py已存在
echo.

echo ========================================
echo 所有测试通过！✅
echo ========================================
echo.
echo 系统已准备就绪，可以使用WhisperX功能。
echo.
echo 如需测试实际对齐效果，请：
echo 1. 准备一个MP3音频文件
echo 2. 运行：py D:\code\adminFlow\scripts\whisperx_align.py audio.mp3 "原文"
echo.
pause
