@echo off
REM WhisperX常驻服务启动脚本
REM 作者：Kiro AI Assistant
REM 日期：2026-08-16

echo ============================================================
echo WhisperX常驻服务启动脚本
echo ============================================================
echo.

REM 检查Python 3.13是否存在
where python313 >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] 检测到python313命令
    set PYTHON_CMD=python313
    goto :install_flask
)

REM 尝试py -3.13
py -3.13 --version >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] 检测到py -3.13命令
    set PYTHON_CMD=py -3.13
    goto :install_flask
)

REM 尝试python
python --version >nul 2>&1
if %errorlevel% equ 0 (
    echo [警告] 使用python命令（建议安装Python 3.13）
    set PYTHON_CMD=python
    goto :install_flask
) else (
    echo [错误] 未找到Python！
    echo 请运行 setup_python311_whisperx.bat 安装Python 3.13
    pause
    exit /b 1
)

:install_flask
echo.
echo [步骤1] 检查Flask依赖...
%PYTHON_CMD% -c "import flask" >nul 2>&1
if %errorlevel% neq 0 (
    echo [安装] Flask未安装，正在安装...
    %PYTHON_CMD% -m pip install flask flask-cors -i https://pypi.tuna.tsinghua.edu.cn/simple
    if %errorlevel% neq 0 (
        echo [错误] Flask安装失败！
        pause
        exit /b 1
    )
) else (
    echo [OK] Flask已安装
)

echo.
echo [步骤2] 启动WhisperX服务...
echo 服务地址: http://localhost:5000
echo 健康检查: http://localhost:5000/health
echo 对齐接口: http://localhost:5000/align (POST)
echo.
echo 按Ctrl+C停止服务
echo ============================================================
echo.

REM 启动服务
cd /d "%~dp0"
%PYTHON_CMD% whisperx_server.py

pause
