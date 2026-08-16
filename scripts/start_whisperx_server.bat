@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

title WhisperX HTTP服务（常驻进程模式）

echo ========================================
echo WhisperX HTTP服务启动脚本
echo ========================================
echo.

:: 检查Python 3.13
echo [1/3] 检查Python 3.13...
where py >nul 2>&1
if %errorlevel% equ 0 (
    py -3.13 --version >nul 2>&1
    if %errorlevel% equ 0 (
        set PYTHON_CMD=py -3.13
        echo ✅ 使用Python: py -3.13
        goto :START_SERVER
    )
)

python313 --version >nul 2>&1
if %errorlevel% equ 0 (
    set PYTHON_CMD=python313
    echo ✅ 使用Python: python313
    goto :START_SERVER
)

python --version >nul 2>&1
if %errorlevel% equ 0 (
    set PYTHON_CMD=python
    echo ⚠️  使用Python: python（可能不是3.13）
    goto :START_SERVER
)

echo ❌ 未找到Python 3.13！
echo.
echo 请运行以下脚本安装：
echo   D:\code\adminFlow\scripts\setup_python311_whisperx.bat
echo.
pause
exit /b 1

:START_SERVER
echo.
echo [2/3] 检查依赖包...
%PYTHON_CMD% -c "import whisperx; import flask" >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ WhisperX或Flask未安装！
    echo.
    echo 请运行以下脚本安装：
    echo   D:\code\adminFlow\scripts\install_whisperx_fast.bat
    echo.
    pause
    exit /b 1
)
echo ✅ 依赖包已安装

echo.
echo [3/3] 启动服务...
echo.
echo ========================================
echo 服务信息
echo ========================================
echo 监听地址: http://localhost:5000
echo 健康检查: http://localhost:5000/health
echo 对齐接口: POST http://localhost:5000/align
echo 批量接口: POST http://localhost:5000/align_batch
echo.
echo 提示: 按Ctrl+C可停止服务
echo ========================================
echo.

:: 启动服务
%PYTHON_CMD% "%~dp0whisperx_server.py"

:: 服务停止
echo.
echo ========================================
echo 服务已停止
echo ========================================
pause
