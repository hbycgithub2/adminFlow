@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ========================================
echo FFmpeg 6.1.1 下载脚本（使用tar.exe）
echo ========================================
echo.
echo ⚠️  此脚本需要Windows 10或更高版本
echo    （使用内置的tar.exe解压，无需安装7-Zip）
echo.

:: 检查Windows版本（需要Windows 10+）
ver | findstr /i "10\." >nul
if %errorlevel% neq 0 (
    ver | findstr /i "11\." >nul
    if %errorlevel% neq 0 (
        echo ❌ 此方法需要Windows 10或Windows 11
        echo 💡 请使用方案A安装7-Zip
        goto :END
    )
)

echo ✅ Windows版本检查通过
echo.

:: 设置目标目录
set "TARGET_DIR=D:\ai\codex\ffmpeg-6.1.1-essentials_build"
set "DOWNLOAD_URL=https://github.com/GyanD/codexffmpeg/releases/download/6.1.1/ffmpeg-6.1.1-essentials_build.zip"
set "DOWNLOAD_FILE=%TEMP%\ffmpeg-6.1.1-essentials_build.zip"

echo 📥 步骤1：下载FFmpeg 6.1.1（ZIP版本）...
echo 下载地址：%DOWNLOAD_URL%
echo.
echo ⏳ 正在下载（约40MB，预计2-5分钟）...
echo.

:: 使用PowerShell下载ZIP版本
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Write-Host '开始下载...' -ForegroundColor Green; try { Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%DOWNLOAD_FILE%' -UseBasicParsing; Write-Host '✅ 下载完成' -ForegroundColor Green; } catch { Write-Host '❌ 下载失败：' $_.Exception.Message -ForegroundColor Red; exit 1; }}"

if %errorlevel% neq 0 (
    echo.
    echo ❌ 下载失败
    echo.
    echo 💡 备用方案：
    echo 1. 手动访问：https://github.com/GyanD/codexffmpeg/releases/tag/6.1.1
    echo 2. 下载：ffmpeg-6.1.1-essentials_build.zip
    echo 3. 解压到：D:\ai\codex\
    goto :END
)

echo.
echo 📦 步骤2：解压FFmpeg（使用tar.exe）...
echo.

:: 创建目标目录
if not exist "D:\ai\codex" mkdir "D:\ai\codex"

:: 删除旧版本
if exist "%TARGET_DIR%" (
    echo 🗑️ 删除旧版本...
    rmdir /s /q "%TARGET_DIR%"
)

:: 使用Windows内置的tar命令解压（Windows 10+）
tar -xf "%DOWNLOAD_FILE%" -C "D:\ai\codex\"

if %errorlevel% neq 0 (
    echo ❌ 解压失败
    echo.
    echo 💡 手动解压方法：
    echo 1. 右键点击：%DOWNLOAD_FILE%
    echo 2. 选择"全部解压缩"
    echo 3. 解压到：D:\ai\codex\
    goto :END
)

echo ✅ 解压完成
echo.

:: 验证安装
echo 🧪 步骤3：验证安装...
echo.

if not exist "%TARGET_DIR%\bin\ffmpeg.exe" (
    echo ❌ FFmpeg未找到：%TARGET_DIR%\bin\ffmpeg.exe
    goto :END
)

"%TARGET_DIR%\bin\ffmpeg.exe" -version | findstr "6.1.1"
if %errorlevel% neq 0 (
    echo ❌ FFmpeg版本验证失败
    goto :END
)

echo ✅ FFmpeg 6.1.1 验证成功
echo.

:: 检查GPU编码器
echo 🎮 步骤4：检查GPU编码器支持...
"%TARGET_DIR%\bin\ffmpeg.exe" -encoders 2>nul | findstr "h264_nvenc" >nul
if %errorlevel% equ 0 (
    echo ✅ h264_nvenc 编码器可用（支持NVIDIA GPU加速）
) else (
    echo ⚠️  h264_nvenc 编码器不可用（将使用CPU编码）
    echo     需要NVIDIA驱动 ≥ 531.00
)
echo.

:: 清理下载文件
echo 🗑️ 清理临时文件...
del /f /q "%DOWNLOAD_FILE%" 2>nul
echo.

echo ========================================
echo 🎉 FFmpeg 6.1.1 安装成功！
echo ========================================
echo.
echo 📍 安装路径：%TARGET_DIR%
echo.
echo 📋 下一步操作：
echo 1. 重启Spring Boot服务
echo 2. 测试视频生成功能
echo.

:END
pause
