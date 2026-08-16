@echo off
chcp 65001 > nul

echo ========================================
echo WhisperX HTTP服务测试脚本
echo ========================================
echo.

echo [1/2] 测试健康检查接口...
curl -s http://localhost:5000/health
echo.
echo.

if %errorlevel% neq 0 (
    echo ❌ 服务未启动！
    echo.
    echo 请先运行: start_whisperx_server.bat
    pause
    exit /b 1
)

echo ✅ 服务正常运行

echo.
echo [2/2] 测试对齐接口（需要有测试音频文件）...
echo.
echo 💡 提示: 手动测试命令：
echo.
echo curl -X POST http://localhost:5000/align ^
echo      -H "Content-Type: application/json" ^
echo      -d "{\"audio\":\"/path/to/test.mp3\",\"text\":\"测试文本\"}"
echo.

pause
