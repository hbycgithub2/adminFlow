@echo off
echo =============================================
echo Edge TTS 安装脚本
echo =============================================
echo.

echo [1/3] 检查 Python 是否安装...
python --version > nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Python 未安装！
    echo [提示] 请先安装 Python 3.7+
    echo [下载] https://www.python.org/downloads/
    pause
    exit /b 1
)
echo [√] Python 已安装

echo.
echo [2/3] 检查 pip 是否可用...
pip --version > nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] pip 未安装！
    pause
    exit /b 1
)
echo [√] pip 已安装

echo.
echo [3/3] 安装 edge-tts...
pip install edge-tts -i https://pypi.tuna.tsinghua.edu.cn/simple
if %errorlevel% neq 0 (
    echo [错误] 安装失败！
    pause
    exit /b 1
)

echo.
echo =============================================
echo ✅ Edge TTS 安装成功！
echo =============================================
echo.
echo 验证安装：
edge-tts --version

echo.
echo 测试语音合成：
edge-tts --text "Hello, this is Edge TTS!" --write-media test.mp3
if exist test.mp3 (
    echo [√] 测试文件已生成: test.mp3
    del test.mp3
)

echo.
pause
