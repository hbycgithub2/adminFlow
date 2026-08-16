@echo off
chcp 65001 >nul
echo ============================================
echo WhisperX兼容性修复脚本
echo ============================================
echo.
echo 问题：Python 3.14太新，faster-whisper不兼容
echo 解决：降级faster-whisper到兼容版本
echo.
echo 按任意键开始修复...
pause >nul
echo.

echo [步骤1] 卸载当前版本...
pip uninstall faster-whisper -y

echo.
echo [步骤2] 安装兼容版本（1.0.3）...
pip install faster-whisper==1.0.3

if %errorlevel% neq 0 (
    echo.
    echo ❌ 安装失败！尝试备选版本...
    pip install faster-whisper==1.0.0
)

echo.
echo [步骤3] 验证安装...
py -c "import faster_whisper; print('✅ faster-whisper版本:', faster_whisper.__version__)"

if %errorlevel% neq 0 (
    echo.
    echo ❌ 验证失败！
    pause
    exit /b 1
)

echo.
echo ============================================
echo ✅ 修复完成！
echo ============================================
echo.
echo 下一步：
echo 1. 重启Spring Boot服务
echo 2. 重新测试
echo.
pause
