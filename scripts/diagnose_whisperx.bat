@echo off
chcp 65001 > nul
color 0A

title WhisperX诊断工具

cls
echo.
echo ╔═══════════════════════════════════════════════════════════╗
echo ║                                                           ║
echo ║         WhisperX 完整诊断工具                             ║
echo ║                                                           ║
echo ╚═══════════════════════════════════════════════════════════╝
echo.
echo.

set PASS_COUNT=0
set FAIL_COUNT=0

echo ═══════════════════════════════════════════════════════════
echo 第1项：检查Python 3.13
echo ═══════════════════════════════════════════════════════════
echo.

py -3.13 --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ PASS: py -3.13 可用
    py -3.13 --version
    set /a PASS_COUNT+=1
) else (
    echo ❌ FAIL: py -3.13 不可用
    set /a FAIL_COUNT+=1
)
echo.

echo ═══════════════════════════════════════════════════════════
echo 第2项：检查WhisperX是否安装
echo ═══════════════════════════════════════════════════════════
echo.

py -3.13 -c "import whisperx; print(f'WhisperX版本：{whisperx.__version__}')" 2>nul
if %errorlevel% equ 0 (
    echo ✅ PASS: WhisperX已安装
    set /a PASS_COUNT+=1
) else (
    echo ❌ FAIL: WhisperX未安装
    echo    运行: install_whisperx_fast.bat
    set /a FAIL_COUNT+=1
)
echo.

echo ═══════════════════════════════════════════════════════════
echo 第3项：检查Flask是否安装
echo ═══════════════════════════════════════════════════════════
echo.

py -3.13 -c "import flask; print(f'Flask版本：{flask.__version__}')" 2>nul
if %errorlevel% equ 0 (
    echo ✅ PASS: Flask已安装
    set /a PASS_COUNT+=1
) else (
    echo ❌ FAIL: Flask未安装
    echo    运行: py -3.13 -m pip install flask
    set /a FAIL_COUNT+=1
)
echo.

echo ═══════════════════════════════════════════════════════════
echo 第4项：检查whisperx_align.py是否存在
echo ═══════════════════════════════════════════════════════════
echo.

if exist "%~dp0whisperx_align.py" (
    echo ✅ PASS: whisperx_align.py 存在
    echo    路径：%~dp0whisperx_align.py
    set /a PASS_COUNT+=1
) else (
    echo ❌ FAIL: whisperx_align.py 不存在
    set /a FAIL_COUNT+=1
)
echo.

echo ═══════════════════════════════════════════════════════════
echo 第5项：检查whisperx_server.py是否存在
echo ═══════════════════════════════════════════════════════════
echo.

if exist "%~dp0whisperx_server.py" (
    echo ✅ PASS: whisperx_server.py 存在
    echo    路径：%~dp0whisperx_server.py
    set /a PASS_COUNT+=1
) else (
    echo ❌ FAIL: whisperx_server.py 不存在
    set /a FAIL_COUNT+=1
)
echo.

echo ═══════════════════════════════════════════════════════════
echo 第6项：检查WhisperX HTTP服务是否运行
echo ═══════════════════════════════════════════════════════════
echo.

curl -s http://localhost:5000/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ PASS: WhisperX HTTP服务正在运行
    curl -s http://localhost:5000/health
    set /a PASS_COUNT+=1
) else (
    echo ⚠️  WARN: WhisperX HTTP服务未运行
    echo    运行: start_whisperx_server.bat
    echo    （这是可选项，未运行会自动回退到Python脚本模式）
    set /a FAIL_COUNT+=1
)
echo.

echo ═══════════════════════════════════════════════════════════
echo 第7项：检查临时目录是否存在
echo ═══════════════════════════════════════════════════════════
echo.

set TEMP_DIR=D:\code\adminFlow\temp\whisperx
if exist "%TEMP_DIR%" (
    echo ✅ PASS: 临时目录存在
    echo    路径：%TEMP_DIR%
    set /a PASS_COUNT+=1
) else (
    echo ⚠️  WARN: 临时目录不存在，将自动创建
    mkdir "%TEMP_DIR%"
    if %errorlevel% equ 0 (
        echo ✅ 临时目录创建成功
        set /a PASS_COUNT+=1
    ) else (
        echo ❌ 临时目录创建失败
        set /a FAIL_COUNT+=1
    )
)
echo.

echo ═══════════════════════════════════════════════════════════
echo 第8项：检查GPU是否可用
echo ═══════════════════════════════════════════════════════════
echo.

py -3.13 -c "import torch; print(f'CUDA可用：{torch.cuda.is_available()}'); print(f'GPU名称：{torch.cuda.get_device_name(0) if torch.cuda.is_available() else \"无\"}') if torch.cuda.is_available() else print(f'GPU数量：{0}')" 2>nul
if %errorlevel% equ 0 (
    echo ✅ PASS: PyTorch已安装
    set /a PASS_COUNT+=1
) else (
    echo ❌ FAIL: PyTorch未安装或GPU不可用
    set /a FAIL_COUNT+=1
)
echo.

echo ═══════════════════════════════════════════════════════════
echo 第9项：检查FFmpeg是否可用
echo ═══════════════════════════════════════════════════════════
echo.

where ffmpeg >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ PASS: FFmpeg在PATH中
    ffmpeg -version | findstr "ffmpeg version"
    set /a PASS_COUNT+=1
) else (
    echo ⚠️  WARN: FFmpeg不在PATH中
    echo    WhisperX会使用配置的FFmpeg路径
    set /a FAIL_COUNT+=1
)
echo.

echo ═══════════════════════════════════════════════════════════
echo 第10项：检查Spring Boot配置
echo ═══════════════════════════════════════════════════════════
echo.

set CONFIG_FILE=D:\code\adminFlow\hm-service\src\main\resources\application.yml
if exist "%CONFIG_FILE%" (
    echo ✅ PASS: application.yml 存在
    echo.
    echo 关键配置：
    findstr /C:"whisperx:" "%CONFIG_FILE%"
    findstr /C:"use:" "%CONFIG_FILE%"
    findstr /C:"server:" "%CONFIG_FILE%"
    findstr /C:"url:" "%CONFIG_FILE%"
    set /a PASS_COUNT+=1
) else (
    echo ❌ FAIL: application.yml 不存在
    set /a FAIL_COUNT+=1
)
echo.

echo ═══════════════════════════════════════════════════════════
echo 诊断结果汇总
echo ═══════════════════════════════════════════════════════════
echo.
echo ✅ 通过：%PASS_COUNT%/10
echo ❌ 失败：%FAIL_COUNT%/10
echo.

if %FAIL_COUNT% equ 0 (
    color 0A
    echo ╔═══════════════════════════════════════════════════════════╗
    echo ║                                                           ║
    echo ║          🎉 所有检查通过！                                 ║
    echo ║                                                           ║
    echo ║          系统已准备就绪，可以开始使用                       ║
    echo ║                                                           ║
    echo ╚═══════════════════════════════════════════════════════════╝
) else (
    color 0C
    echo ╔═══════════════════════════════════════════════════════════╗
    echo ║                                                           ║
    echo ║          ⚠️  发现问题，请修复后重新运行诊断                 ║
    echo ║                                                           ║
    echo ╚═══════════════════════════════════════════════════════════╝
)

echo.
echo ═══════════════════════════════════════════════════════════
echo 快速修复指南
echo ═══════════════════════════════════════════════════════════
echo.
echo 如果第1-2项失败：
echo   → 运行 setup_python311_whisperx.bat 安装Python 3.13 + WhisperX
echo.
echo 如果第3项失败：
echo   → 运行 py -3.13 -m pip install flask
echo.
echo 如果第6项失败（可选）：
echo   → 运行 start_whisperx_server.bat 启动HTTP服务
echo   → 注意：不启动也能工作，只是会慢一些
echo.
echo 如果第10项失败：
echo   → 检查 application.yml 配置是否正确
echo.

pause
