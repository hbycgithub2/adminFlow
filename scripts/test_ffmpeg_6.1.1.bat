@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ========================================
echo FFmpeg 6.1.1 功能验证测试
echo ========================================
echo.

set "FFMPEG_PATH=D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe"
set "TEST_DIR=%TEMP%\ffmpeg_test"

:: 检查FFmpeg是否存在
echo 📦 步骤1：检查FFmpeg安装...
if not exist "%FFMPEG_PATH%" (
    echo ❌ FFmpeg未安装：%FFMPEG_PATH%
    echo.
    echo 💡 请先运行：download_ffmpeg_6.1.1.bat
    goto :END
)
echo ✅ FFmpeg已安装
echo.

:: 显示版本信息
echo 📋 步骤2：查看版本信息...
"%FFMPEG_PATH%" -version | findstr /C:"ffmpeg version" /C:"configuration"
echo.

:: 检查NVENC支持
echo 🎮 步骤3：检查GPU编码器...
echo.
echo h264_nvenc（H.264 GPU编码器）：
"%FFMPEG_PATH%" -encoders 2>nul | findstr "h264_nvenc"
if %errorlevel% equ 0 (
    echo ✅ 可用
) else (
    echo ❌ 不可用
)
echo.

echo hevc_nvenc（H.265 GPU编码器）：
"%FFMPEG_PATH%" -encoders 2>nul | findstr "hevc_nvenc"
if %errorlevel% equ 0 (
    echo ✅ 可用
) else (
    echo ❌ 不可用
)
echo.

:: 检查NVIDIA驱动
echo 🖥️  步骤4：检查NVIDIA驱动...
where nvidia-smi >nul 2>&1
if %errorlevel% equ 0 (
    echo.
    nvidia-smi --query-gpu=driver_version,name --format=csv,noheader
    echo.
) else (
    echo ⚠️  nvidia-smi不可用（可能没有NVIDIA显卡）
    echo.
)

:: 创建测试目录
echo 🧪 步骤5：生成测试视频...
if not exist "%TEST_DIR%" mkdir "%TEST_DIR%"

:: 生成测试音频（1秒静音）
echo 生成测试音频...
"%FFMPEG_PATH%" -f lavfi -i anullsrc=r=44100:cl=mono -t 1 -q:a 9 -acodec libmp3lame "%TEST_DIR%\test_audio.mp3" -y >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 测试音频生成失败
    goto :END
)
echo ✅ 测试音频生成成功
echo.

:: 测试CPU编码（libx264）
echo 测试1：CPU编码（libx264）...
"%FFMPEG_PATH%" -i "%TEST_DIR%\test_audio.mp3" -f lavfi -i color=c=white:s=1280x720 -c:v libx264 -preset ultrafast -t 1 -c:a aac -shortest "%TEST_DIR%\test_cpu.mp4" -y >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ CPU编码成功：test_cpu.mp4
    for %%F in ("%TEST_DIR%\test_cpu.mp4") do echo    文件大小：%%~zF bytes
) else (
    echo ❌ CPU编码失败
)
echo.

:: 测试GPU编码（h264_nvenc）
echo 测试2：GPU编码（h264_nvenc）...
"%FFMPEG_PATH%" -hwaccel cuda -i "%TEST_DIR%\test_audio.mp3" -f lavfi -i color=c=white:s=1280x720 -c:v h264_nvenc -preset p1 -t 1 -c:a aac -shortest "%TEST_DIR%\test_gpu.mp4" -y >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ GPU编码成功：test_gpu.mp4
    for %%F in ("%TEST_DIR%\test_gpu.mp4") do echo    文件大小：%%~zF bytes
    echo.
    echo 🎉 GPU硬件加速可用！
) else (
    echo ❌ GPU编码失败（可能驱动版本不足或无NVIDIA显卡）
    echo.
    echo 💡 解决方案：
    echo    - 升级NVIDIA驱动到 ≥ 531.00
    echo    - 或使用CPU编码（禁用GPU加速）
)
echo.

:: 清理测试文件
echo 🗑️  清理测试文件...
if exist "%TEST_DIR%" (
    rmdir /s /q "%TEST_DIR%"
    echo ✅ 清理完成
)
echo.

echo ========================================
echo 测试完成
echo ========================================
echo.
echo 📊 测试结果总结：
echo    1. FFmpeg版本：6.1.1 ✅
echo    2. CPU编码（libx264）：[查看上方结果]
echo    3. GPU编码（h264_nvenc）：[查看上方结果]
echo.
echo 📝 下一步操作：
echo    1. 如果GPU编码成功 → 直接重启Spring Boot服务
echo    2. 如果GPU编码失败 → 升级NVIDIA驱动或禁用GPU加速
echo.

:END
pause
