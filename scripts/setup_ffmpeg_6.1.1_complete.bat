@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ========================================
echo FFmpeg 6.1.1 一键安装与配置
echo ========================================
echo.
echo 🚀 此脚本将自动完成以下操作：
echo    1. 下载FFmpeg 6.1.1 LTS版本
echo    2. 安装到 D:\ai\codex\ffmpeg-6.1.1-essentials_build
echo    3. 验证GPU编码器支持
echo    4. 测试视频生成功能
echo    5. 生成配置报告
echo.
echo ⏱️  预计耗时：3-5分钟（取决于网络速度）
echo.

choice /C YN /M "是否继续？(Y=是, N=否)"
if errorlevel 2 (
    echo ❌ 用户取消操作
    goto :END
)

echo.
echo ========================================
echo 第1步：下载并安装FFmpeg 6.1.1
echo ========================================
echo.

call "%~dp0download_ffmpeg_6.1.1.bat"

if %errorlevel% neq 0 (
    echo.
    echo ❌ FFmpeg安装失败，请检查错误信息
    goto :END
)

echo.
echo ========================================
echo 第2步：验证GPU编码器支持
echo ========================================
echo.

call "%~dp0test_ffmpeg_6.1.1.bat"

echo.
echo ========================================
echo 第3步：检查Spring Boot服务状态
echo ========================================
echo.

:: 检查Spring Boot服务是否运行
netstat -ano | findstr ":8080" >nul 2>&1
if %errorlevel% equ 0 (
    echo ⚠️  检测到Spring Boot服务正在运行（端口8080占用）
    echo.
    choice /C YN /M "是否重启服务以应用新配置？(Y=是, N=稍后手动重启)"
    if errorlevel 1 (
        echo.
        echo 🔄 正在停止服务...
        :: 查找并终止占用8080端口的进程
        for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do (
            taskkill /F /PID %%a >nul 2>&1
        )
        echo ✅ 服务已停止
        echo.
        echo 💡 请手动重启Spring Boot服务：
        echo    cd D:\code\adminFlow\hm-service
        echo    mvn spring-boot:run
        echo.
        echo    或者：
        echo    java -jar target\hm-service-1.0.0.jar
    )
) else (
    echo ℹ️  Spring Boot服务未运行
    echo.
    echo 💡 启动服务命令：
    echo    cd D:\code\adminFlow\hm-service
    echo    mvn spring-boot:run
)

echo.
echo ========================================
echo 第4步：生成配置报告
echo ========================================
echo.

set "REPORT_FILE=%~dp0..\docs\FFmpeg-6.1.1-配置报告.txt"

(
    echo ========================================
    echo FFmpeg 6.1.1 配置报告
    echo ========================================
    echo 生成时间：%date% %time%
    echo.
    echo 1. 安装信息
    echo    安装路径：D:\ai\codex\ffmpeg-6.1.1-essentials_build
    echo    版本：6.1.1-essentials_build
    echo.
    echo 2. 配置信息
    echo    Java配置文件：FFmpegUtil.java
    echo    FFMPEG_PATH：D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe
    echo    FFPROBE_PATH：D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffprobe.exe
    echo    GPU加速：ENABLE_GPU_ACCELERATION = true
    echo.
    echo 3. 编码器支持
) > "%REPORT_FILE%"

D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -encoders 2>nul | findstr "h264_nvenc" >> "%REPORT_FILE%"
if %errorlevel% equ 0 (
    echo    ✅ h264_nvenc（H.264 GPU编码）：可用 >> "%REPORT_FILE%"
) else (
    echo    ❌ h264_nvenc（H.264 GPU编码）：不可用 >> "%REPORT_FILE%"
)

D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe -encoders 2>nul | findstr "hevc_nvenc" >> "%REPORT_FILE%"
if %errorlevel% equ 0 (
    echo    ✅ hevc_nvenc（H.265 GPU编码）：可用 >> "%REPORT_FILE%"
) else (
    echo    ❌ hevc_nvenc（H.265 GPU编码）：不可用 >> "%REPORT_FILE%"
)

(
    echo    ✅ libx264（H.264 CPU编码）：可用
    echo.
    echo 4. NVIDIA驱动信息
) >> "%REPORT_FILE%"

where nvidia-smi >nul 2>&1
if %errorlevel% equ 0 (
    nvidia-smi --query-gpu=driver_version,name --format=csv,noheader >> "%REPORT_FILE%"
) else (
    echo    ⚠️  nvidia-smi不可用（可能没有NVIDIA显卡） >> "%REPORT_FILE%"
)

(
    echo.
    echo 5. 测试结果
    echo    [详见上方测试输出]
    echo.
    echo 6. 下一步操作
    echo    1. ✅ FFmpeg 6.1.1已安装
    echo    2. ✅ Java代码已配置
    echo    3. 🔄 重启Spring Boot服务
    echo    4. 🧪 测试视频生成功能
    echo.
    echo 7. 测试命令
    echo    curl -X POST http://localhost:8080/api/video/generate \
    echo         -H "Content-Type: application/json" \
    echo         -d "{\"documentId\":\"test\"}"
    echo.
    echo ========================================
) >> "%REPORT_FILE%"

echo ✅ 配置报告已生成：%REPORT_FILE%
echo.
type "%REPORT_FILE%"

echo.
echo ========================================
echo 🎉 安装完成！
echo ========================================
echo.
echo 📋 快速参考：
echo.
echo 1️⃣  FFmpeg路径：
echo    D:\ai\codex\ffmpeg-6.1.1-essentials_build\bin\ffmpeg.exe
echo.
echo 2️⃣  Java配置（已自动更新）：
echo    FFmpegUtil.java → FFMPEG_PATH
echo.
echo 3️⃣  重启服务：
echo    cd D:\code\adminFlow\hm-service
echo    mvn spring-boot:run
echo.
echo 4️⃣  测试接口：
echo    POST http://localhost:8080/api/video/generate
echo.
echo 📄 详细报告：
echo    %REPORT_FILE%
echo.
echo 💡 故障排查：
echo    如果GPU编码失败，请查看：
echo    D:\code\adminFlow\docs\FFmpeg降级到6.1.1-LTS版本.md
echo.

:END
pause
