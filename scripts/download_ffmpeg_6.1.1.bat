@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ========================================
echo FFmpeg 6.1.1 LTS 自动下载脚本
echo ========================================
echo.

:: 设置目标目录
set "TARGET_DIR=D:\ai\codex\ffmpeg-6.1.1-essentials_build"
set "DOWNLOAD_URL=https://www.gyan.dev/ffmpeg/builds/packages/ffmpeg-6.1.1-essentials_build.7z"
set "DOWNLOAD_FILE=%TEMP%\ffmpeg-6.1.1-essentials_build.7z"

echo 📥 步骤1：检查目标目录...
if exist "%TARGET_DIR%" (
    echo ⚠️  目标目录已存在：%TARGET_DIR%
    echo.
    choice /C YN /M "是否覆盖现有版本？(Y=是, N=否)"
    if errorlevel 2 (
        echo ❌ 用户取消操作
        goto :END
    )
    echo 🗑️  删除旧版本...
    rmdir /s /q "%TARGET_DIR%"
)

echo ✅ 目标目录准备完成
echo.

:: 检查7-Zip是否安装
echo 📦 步骤2：检查7-Zip...
where 7z >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 未找到7-Zip，请先安装7-Zip
    echo.
    echo 📥 自动打开7-Zip下载页面...
    start https://www.7-zip.org/download.html
    echo.
    echo 安装完成后，请重新运行此脚本
    goto :END
)
echo ✅ 7-Zip已安装
echo.

:: 下载FFmpeg
echo 📥 步骤3：下载FFmpeg 6.1.1...
echo 下载地址：%DOWNLOAD_URL%
echo 保存位置：%DOWNLOAD_FILE%
echo.
echo ⏳ 正在下载（约30MB，预计1-3分钟）...
echo.

:: 使用PowerShell下载（支持进度条）
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Write-Host '开始下载...' -ForegroundColor Green; try { Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%DOWNLOAD_FILE%' -UseBasicParsing; Write-Host '✅ 下载完成' -ForegroundColor Green; } catch { Write-Host '❌ 下载失败：' $_.Exception.Message -ForegroundColor Red; exit 1; }}"

if %errorlevel% neq 0 (
    echo.
    echo ❌ 下载失败，请检查网络连接
    echo.
    echo 💡 手动下载方法：
    echo 1. 访问：https://www.gyan.dev/ffmpeg/builds/
    echo 2. 下载：ffmpeg-6.1.1-essentials_build.7z
    echo 3. 保存到：%DOWNLOAD_FILE%
    echo 4. 重新运行此脚本
    goto :END
)

:: 检查下载文件
if not exist "%DOWNLOAD_FILE%" (
    echo ❌ 下载文件不存在：%DOWNLOAD_FILE%
    goto :END
)

echo.
echo 📦 步骤4：解压FFmpeg...
echo 目标目录：D:\ai\codex\
echo.

:: 创建临时解压目录
set "TEMP_EXTRACT=%TEMP%\ffmpeg_extract"
if exist "%TEMP_EXTRACT%" rmdir /s /q "%TEMP_EXTRACT%"
mkdir "%TEMP_EXTRACT%"

:: 解压到临时目录
7z x "%DOWNLOAD_FILE%" -o"%TEMP_EXTRACT%" -y >nul
if %errorlevel% neq 0 (
    echo ❌ 解压失败
    goto :CLEANUP
)

:: 查找解压后的目录（7z会创建一个带版本号的子目录）
for /d %%i in ("%TEMP_EXTRACT%\ffmpeg-*") do (
    set "EXTRACTED_DIR=%%i"
    goto :FOUND_DIR
)

:FOUND_DIR
if not defined EXTRACTED_DIR (
    echo ❌ 未找到解压后的FFmpeg目录
    goto :CLEANUP
)

echo ✅ 解压完成：!EXTRACTED_DIR!
echo.

:: 移动到目标目录
echo 📁 步骤5：安装到目标目录...
mkdir "D:\ai\codex" 2>nul
move "!EXTRACTED_DIR!" "%TARGET_DIR%" >nul
if %errorlevel% neq 0 (
    echo ❌ 移动文件失败
    goto :CLEANUP
)

echo ✅ 安装完成：%TARGET_DIR%
echo.

:: 验证安装
echo 🧪 步骤6：验证安装...
echo.

"%TARGET_DIR%\bin\ffmpeg.exe" -version | findstr "version 6.1.1"
if %errorlevel% neq 0 (
    echo ❌ FFmpeg版本验证失败
    goto :CLEANUP
)

echo ✅ FFmpeg 6.1.1 验证成功
echo.

:: 检查NVENC编码器
echo 🎮 步骤7：检查GPU编码器支持...
"%TARGET_DIR%\bin\ffmpeg.exe" -encoders 2>nul | findstr "h264_nvenc" >nul
if %errorlevel% equ 0 (
    echo ✅ h264_nvenc 编码器可用（支持NVIDIA GPU加速）
) else (
    echo ⚠️  h264_nvenc 编码器不可用（将使用CPU编码）
)

"%TARGET_DIR%\bin\ffmpeg.exe" -encoders 2>nul | findstr "hevc_nvenc" >nul
if %errorlevel% equ 0 (
    echo ✅ hevc_nvenc 编码器可用（支持NVIDIA H.265编码）
) else (
    echo ⚠️  hevc_nvenc 编码器不可用
)
echo.

:: 清理下载文件
:CLEANUP
echo 🗑️  步骤8：清理临时文件...
if exist "%DOWNLOAD_FILE%" del /f /q "%DOWNLOAD_FILE%"
if exist "%TEMP_EXTRACT%" rmdir /s /q "%TEMP_EXTRACT%"
echo ✅ 清理完成
echo.

echo ========================================
echo 🎉 FFmpeg 6.1.1 安装成功！
echo ========================================
echo.
echo 📍 安装路径：
echo    %TARGET_DIR%
echo.
echo 🔧 可执行文件：
echo    ffmpeg.exe:  %TARGET_DIR%\bin\ffmpeg.exe
echo    ffprobe.exe: %TARGET_DIR%\bin\ffprobe.exe
echo.
echo 📝 下一步操作：
echo    1. ✅ Java代码已自动更新（无需手动修改）
echo    2. 🔄 重启Spring Boot服务
echo    3. 🧪 测试视频生成功能
echo.
echo 🚀 快速测试命令：
echo    "%TARGET_DIR%\bin\ffmpeg.exe" -hwaccel cuda -encoders ^| findstr nvenc
echo.

:END
echo 按任意键退出...
pause >nul
