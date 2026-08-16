@echo off
chcp 65001 >nul
echo ========================================
echo    Whisper功能快速诊断
echo ========================================
echo.

echo [1/5] 检查Python环境...
py --version 2>nul
if %errorlevel% neq 0 (
    echo ❌ Python未安装或不可用
    echo    请从 https://www.python.org/downloads/ 安装Python 3.8+
    goto :end
) else (
    echo ✅ Python可用
)
echo.

echo [2/5] 检查openai-whisper模块...
py -c "import whisper; print('✅ openai-whisper已安装，版本:', whisper.__version__)" 2>nul
if %errorlevel% neq 0 (
    echo ❌ openai-whisper未安装
    echo.
    echo 是否立即安装？(Y/N)
    set /p install_whisper=
    if /i "%install_whisper%"=="Y" (
        echo 正在安装openai-whisper...
        py -m pip install -U openai-whisper
    ) else (
        echo 跳过安装
    )
) else (
    echo （已在上一行显示）
)
echo.

echo [3/5] 检查Whisper脚本...
if exist "D:\code\adminFlow\scripts\whisper_transcribe.py" (
    echo ✅ Whisper脚本存在
) else (
    echo ❌ Whisper脚本不存在
    echo    路径: D:\code\adminFlow\scripts\whisper_transcribe.py
)
echo.

echo [4/5] 检查代码编译时间...
if exist "D:\code\adminFlow\hm-service\target\classes\com\hmall\tts\volcengine\service\impl\DocumentTTSServiceImpl.class" (
    echo ✅ 编译文件存在
    dir "D:\code\adminFlow\hm-service\target\classes\com\hmall\tts\volcengine\service\impl\DocumentTTSServiceImpl.class" | findstr /C:"DocumentTTSServiceImpl"
    echo.
    echo    提示: 如果时间不是今天，需要重新编译！
) else (
    echo ❌ 编译文件不存在，需要编译
)
echo.

echo [5/5] 测试Whisper脚本...
echo    创建测试音频...
py -c "import numpy as np; import wave; data = (np.sin(2*np.pi*440*np.arange(44100)/44100)*32767).astype(np.int16); w=wave.open('test_audio.wav','w'); w.setnchannels(1); w.setsampwidth(2); w.setframerate(44100); w.writeframes(data.tobytes()); w.close(); print('✅ 测试音频创建成功')" 2>nul
if exist "test_audio.wav" (
    echo    测试Whisper识别...
    py D:\code\adminFlow\scripts\whisper_transcribe.py test_audio.wav
    if %errorlevel% equ 0 (
        echo ✅ Whisper脚本工作正常
    ) else (
        echo ❌ Whisper脚本执行失败
    )
    del test_audio.wav
) else (
    echo ⚠️  跳过脚本测试（需要numpy和wave模块）
)
echo.

echo ========================================
echo    诊断完成
echo ========================================
echo.
echo 下一步操作：
echo 1. 如果Python或whisper未安装，先安装
echo 2. 在IDEA中重新编译项目 (Build ^> Rebuild Project)
echo 3. 重启服务
echo 4. 重新生成视频
echo 5. 在IDEA控制台搜索"Whisper"查看日志
echo.

:end
pause
