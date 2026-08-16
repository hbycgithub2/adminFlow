@echo off
chcp 65001 >nul
echo ======================================
echo WhisperX 模型一键下载
echo （终极解决方案 - 绕过SSL）
echo ======================================
echo.

cd /d "%~dp0"

echo [提示] 此脚本将下载 WhisperX 所需的模型文件
echo [提示] 总大小约 1.4GB，请确保网络连接稳定
echo [提示] 支持断点续传，中断后可重新运行
echo.

pause

echo.
echo [执行] 开始下载...
echo.

py -3.13 download_models_simple.py

if errorlevel 1 (
    echo.
    echo ======================================
    echo ❌ 下载失败
    echo ======================================
    echo.
    echo 可能的原因：
    echo 1. 网络连接问题
    echo 2. Python 环境问题
    echo 3. huggingface_hub 未安装
    echo.
    echo 解决方案：
    echo 1. 检查网络连接
    echo 2. 重新运行此脚本（支持断点续传）
    echo 3. 或使用手动下载：运行 download_whisperx_models.bat
    echo.
) else (
    echo.
    echo ======================================
    echo ✅ 下载完成
    echo ======================================
    echo.
    echo 模型已缓存到：%USERPROFILE%\.cache\huggingface\hub
    echo.
    echo 下一步操作：
    echo 1. 启动 Java 服务（hm-service）
    echo 2. 上传 Word 文档测试 WhisperX
    echo 3. 查看日志输出
    echo.
)

pause
