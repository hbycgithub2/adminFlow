@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ========================================
echo 一键安装 7-Zip + FFmpeg 6.1.1
echo ========================================
echo.

:: 检查7-Zip是否已安装
where 7z >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ 7-Zip已安装，跳过安装步骤
    goto :INSTALL_FFMPEG
)

echo 📦 步骤1：安装7-Zip（自动）
echo.

:: 下载7-Zip安装包（使用x64版本）
set "SEVENZIP_URL=https://www.7-zip.org/a/7z2408-x64.exe"
set "SEVENZIP_INSTALLER=%TEMP%\7z-installer.exe"

echo ⏳ 正在下载7-Zip（约2MB）...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; try { Invoke-WebRequest -Uri '%SEVENZIP_URL%' -OutFile '%SEVENZIP_INSTALLER%' -UseBasicParsing; Write-Host '✅ 7-Zip下载完成' -ForegroundColor Green; } catch { Write-Host '❌ 下载失败：' $_.Exception.Message -ForegroundColor Red; exit 1; }}"

if %errorlevel% neq 0 (
    echo.
    echo ❌ 7-Zip下载失败
    echo.
    echo 💡 手动安装方法：
    echo 1. 访问：https://www.7-zip.org/download.html
    echo 2. 下载：7-Zip 64-bit x64
    echo 3. 安装后重新运行此脚本
    echo.
    start https://www.7-zip.org/download.html
    goto :END
)

:: 静默安装7-Zip
echo 🔧 正在安装7-Zip...
"%SEVENZIP_INSTALLER%" /S

:: 等待安装完成（最多30秒）
set /a counter=0
:WAIT_INSTALL
timeout /t 2 /nobreak >nul
set /a counter+=2

:: 检查安装是否完成（查找7z.exe）
if exist "C:\Program Files\7-Zip\7z.exe" (
    echo ✅ 7-Zip安装成功
    
    :: 添加到PATH环境变量（当前会话）
    set "PATH=%PATH%;C:\Program Files\7-Zip"
    
    :: 清理安装包
    del /f /q "%SEVENZIP_INSTALLER%" 2>nul
    
    goto :INSTALL_FFMPEG
)

if %counter% lss 30 goto :WAIT_INSTALL

echo ⚠️ 7-Zip安装超时，请手动检查
echo.
pause
goto :END

:INSTALL_FFMPEG
echo.
echo ========================================
echo 📥 步骤2：安装FFmpeg 6.1.1
echo ========================================
echo.

:: 调用下载脚本
call "%~dp0download_ffmpeg_6.1.1.bat"

if %errorlevel% neq 0 (
    echo.
    echo ❌ FFmpeg安装失败
    goto :END
)

echo.
echo ========================================
echo 🎉 安装全部完成！
echo ========================================
echo.
echo ✅ 7-Zip：已安装
echo ✅ FFmpeg 6.1.1：已安装
echo.
echo 📋 下一步操作：
echo.
echo 1️⃣ 重启Spring Boot服务：
echo    cd D:\code\adminFlow\hm-service
echo    mvn spring-boot:run
echo.
echo 2️⃣ 测试视频生成：
echo    curl -X POST http://localhost:8080/api/video/generate -H "Content-Type: application/json" -d "{\"documentId\":\"test\",\"text\":\"测试文本\"}"
echo.
echo 3️⃣ 查看日志验证GPU加速：
echo    type D:\code\adminFlow\hm-service\logs\spring.log ^| findstr "GPU"
echo.

:END
pause
