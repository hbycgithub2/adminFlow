@echo off
chcp 65001 >nul
echo ======================================
echo WhisperX 安装验证脚本
echo ======================================
echo.

echo [测试1] 检查 WhisperX 模块导入...
py -3.13 -c "import whisperx; print('✅ WhisperX 导入成功')"
if errorlevel 1 (
    echo ❌ WhisperX 导入失败
    pause
    exit /b 1
)
echo.

echo [测试2] 检查已安装的包...
py -3.13 -m pip show whisperx
echo.

echo [测试3] 检查关键依赖...
py -3.13 -c "import torch; print('✅ PyTorch:', torch.__version__)"
py -3.13 -c "import torchaudio; print('✅ TorchAudio:', torchaudio.__version__)"
py -3.13 -c "import faster_whisper; print('✅ Faster-Whisper 导入成功')"
py -3.13 -c "import transformers; print('✅ Transformers 导入成功')"
echo.

echo [测试4] 测试 whisperx_align.py 脚本...
if exist "whisperx_align.py" (
    echo 找到 whisperx_align.py，测试语法...
    py -3.13 -m py_compile whisperx_align.py
    if errorlevel 1 (
        echo ❌ 脚本语法错误
        pause
        exit /b 1
    ) else (
        echo ✅ 脚本语法正确
    )
) else (
    echo ⚠️ 未找到 whisperx_align.py，跳过测试
)
echo.

echo ======================================
echo ✅ WhisperX 安装验证完成！
echo ======================================
echo.
echo 安装的包列表:
py -3.13 -m pip list | findstr /i "whisperx torch faster-whisper transformers"
echo.
echo 下一步操作:
echo 1. 启动 Java 服务 (hm-service)
echo 2. 上传 Word 文档测试
echo 3. 观察日志输出
echo.

pause
