@echo off
chcp 65001 >nul
echo ======================================
echo WhisperX 模型手动下载脚本
echo （终极解决方案 - 绕过SSL问题）
echo ======================================
echo.

REM 设置模型下载目录（HuggingFace缓存目录）
set "HF_HOME=%USERPROFILE%\.cache\huggingface"
set "MODEL_DIR=%HF_HOME%\hub"

echo [信息] 模型将下载到：%MODEL_DIR%
echo.

REM 创建目录
if not exist "%MODEL_DIR%" (
    echo [步骤1] 创建模型目录...
    mkdir "%MODEL_DIR%"
)

echo ======================================
echo 需要下载的模型清单
echo ======================================
echo.
echo 1. Whisper Base 模型（faster-whisper）
echo    - 大小：约 150MB
echo    - 用途：语音识别和分段
echo    - 下载地址：https://huggingface.co/Systran/faster-whisper-base
echo.
echo 2. Wav2Vec2 中文对齐模型
echo    - 大小：约 300MB
echo    - 用途：字符级时间对齐（核心！）
echo    - 下载地址：https://huggingface.co/jonatasgrosman/wav2vec2-large-xlsr-53-chinese-zh-cn
echo.
echo ======================================
echo 下载方式（3种方式任选其一）
echo ======================================
echo.
echo 方式1：使用 huggingface-cli 下载（推荐，自动处理）
echo 方式2：使用浏览器下载（手动，适合网络问题）
echo 方式3：使用 Git LFS 克隆（完整，适合有git的用户）
echo.

:MENU
echo 请选择下载方式：
echo [1] 使用 huggingface-cli 下载（推荐）
echo [2] 显示手动下载链接（浏览器下载）
echo [3] 使用 Git LFS 克隆
echo [4] 跳过下载，显示已下载的模型
echo [0] 退出
echo.
set /p choice="请输入选项 [1-4, 0]: "

if "%choice%"=="1" goto METHOD1
if "%choice%"=="2" goto METHOD2
if "%choice%"=="3" goto METHOD3
if "%choice%"=="4" goto CHECK_MODELS
if "%choice%"=="0" goto END
goto MENU

REM ============================================
REM 方式1：使用 huggingface-cli 下载
REM ============================================
:METHOD1
echo.
echo ======================================
echo 方式1：使用 huggingface-cli 下载
echo ======================================
echo.

echo [步骤1] 检查 huggingface_hub 是否安装...
py -3.13 -c "import huggingface_hub; print('[成功] huggingface_hub 已安装，版本:', huggingface_hub.__version__)" 2>nul
if errorlevel 1 (
    echo [安装] huggingface_hub 未安装，正在安装...
    py -3.13 -m pip install huggingface_hub -i https://pypi.tuna.tsinghua.edu.cn/simple
)
echo.

echo [步骤2] 设置环境变量（禁用SSL验证）...
set HF_HUB_DISABLE_SSL_VERIFY=1
set NO_PROXY=*
set HTTP_PROXY=
set HTTPS_PROXY=
echo [成功] 环境变量已设置
echo.

echo [步骤3] 下载 Whisper Base 模型...
echo 提示：约150MB，可能需要5-10分钟
echo.
py -3.13 -c "import os; os.environ['HF_HUB_DISABLE_SSL_VERIFY']='1'; os.environ['NO_PROXY']='*'; from huggingface_hub import snapshot_download; snapshot_download('Systran/faster-whisper-base', local_dir='%MODEL_DIR%\\models--Systran--faster-whisper-base', local_dir_use_symlinks=False); print('[成功] Whisper Base 模型下载完成')"

if errorlevel 1 (
    echo [错误] Whisper Base 模型下载失败
    echo 建议：使用方式2（浏览器手动下载）
    pause
    goto MENU
)
echo.

echo [步骤4] 下载 Wav2Vec2 中文对齐模型...
echo 提示：约300MB，可能需要10-15分钟
echo.
py -3.13 -c "import os; os.environ['HF_HUB_DISABLE_SSL_VERIFY']='1'; os.environ['NO_PROXY']='*'; from huggingface_hub import snapshot_download; snapshot_download('jonatasgrosman/wav2vec2-large-xlsr-53-chinese-zh-cn', local_dir='%MODEL_DIR%\\models--jonatasgrosman--wav2vec2-large-xlsr-53-chinese-zh-cn', local_dir_use_symlinks=False); print('[成功] Wav2Vec2 中文模型下载完成')"

if errorlevel 1 (
    echo [错误] Wav2Vec2 模型下载失败
    echo 建议：使用方式2（浏览器手动下载）
    pause
    goto MENU
)
echo.

echo ======================================
echo ✅ 所有模型下载完成！
echo ======================================
goto CHECK_MODELS

REM ============================================
REM 方式2：显示手动下载链接
REM ============================================
:METHOD2
echo.
echo ======================================
echo 方式2：浏览器手动下载
echo ======================================
echo.
echo 请按照以下步骤操作：
echo.
echo [模型1] Whisper Base 模型
echo -----------------------------------------
echo 1. 打开浏览器，访问：
echo    https://huggingface.co/Systran/faster-whisper-base/tree/main
echo.
echo 2. 下载以下文件（点击文件名 → 点击 Download 按钮）：
echo    - config.json
echo    - model.bin
echo    - tokenizer.json
echo    - vocabulary.txt （如果有）
echo.
echo 3. 将下载的文件放到此目录：
echo    %MODEL_DIR%\models--Systran--faster-whisper-base\snapshots\main\
echo.
echo    （请手动创建这些文件夹）
echo.
echo -----------------------------------------
echo.
echo [模型2] Wav2Vec2 中文对齐模型
echo -----------------------------------------
echo 1. 打开浏览器，访问：
echo    https://huggingface.co/jonatasgrosman/wav2vec2-large-xlsr-53-chinese-zh-cn/tree/main
echo.
echo 2. 下载以下文件：
echo    - config.json
echo    - preprocessor_config.json
echo    - pytorch_model.bin （约1.2GB，核心文件！）
echo    - special_tokens_map.json
echo    - tokenizer_config.json
echo    - vocab.json
echo.
echo 3. 将下载的文件放到此目录：
echo    %MODEL_DIR%\models--jonatasgrosman--wav2vec2-large-xlsr-53-chinese-zh-cn\snapshots\main\
echo.
echo -----------------------------------------
echo.
echo 下载完成后，按任意键继续验证...
pause
goto CHECK_MODELS

REM ============================================
REM 方式3：使用 Git LFS 克隆
REM ============================================
:METHOD3
echo.
echo ======================================
echo 方式3：使用 Git LFS 克隆
echo ======================================
echo.

echo [检查] 验证 Git LFS 是否安装...
git lfs version >nul 2>&1
if errorlevel 1 (
    echo [错误] Git LFS 未安装
    echo.
    echo 请先安装 Git LFS：
    echo 1. 访问：https://git-lfs.github.com/
    echo 2. 下载并安装 Git LFS
    echo 3. 运行：git lfs install
    echo.
    pause
    goto MENU
)
echo [成功] Git LFS 已安装
echo.

echo [步骤1] 克隆 Whisper Base 模型...
cd /d "%MODEL_DIR%"
git clone https://huggingface.co/Systran/faster-whisper-base models--Systran--faster-whisper-base
if errorlevel 1 (
    echo [错误] 克隆失败
    pause
    goto MENU
)
echo.

echo [步骤2] 克隆 Wav2Vec2 中文模型...
git clone https://huggingface.co/jonatasgrosman/wav2vec2-large-xlsr-53-chinese-zh-cn models--jonatasgrosman--wav2vec2-large-xlsr-53-chinese-zh-cn
if errorlevel 1 (
    echo [错误] 克隆失败
    pause
    goto MENU
)
echo.

echo ======================================
echo ✅ 模型克隆完成！
echo ======================================
goto CHECK_MODELS

REM ============================================
REM 检查已下载的模型
REM ============================================
:CHECK_MODELS
echo.
echo ======================================
echo 检查已下载的模型
echo ======================================
echo.

echo [模型1] Whisper Base 模型
if exist "%MODEL_DIR%\models--Systran--faster-whisper-base" (
    echo ✅ 已找到：%MODEL_DIR%\models--Systran--faster-whisper-base
    dir /s /b "%MODEL_DIR%\models--Systran--faster-whisper-base\*.bin" 2>nul | findstr /i "model.bin" >nul
    if errorlevel 1 (
        echo ⚠️ 警告：未找到 model.bin 文件，模型可能不完整
    ) else (
        echo ✅ model.bin 文件已存在
    )
) else (
    echo ❌ 未找到模型文件
)
echo.

echo [模型2] Wav2Vec2 中文对齐模型
if exist "%MODEL_DIR%\models--jonatasgrosman--wav2vec2-large-xlsr-53-chinese-zh-cn" (
    echo ✅ 已找到：%MODEL_DIR%\models--jonatasgrosman--wav2vec2-large-xlsr-53-chinese-zh-cn
    dir /s /b "%MODEL_DIR%\models--jonatasgrosman--wav2vec2-large-xlsr-53-chinese-zh-cn\*.bin" 2>nul | findstr /i "pytorch_model.bin" >nul
    if errorlevel 1 (
        echo ⚠️ 警告：未找到 pytorch_model.bin 文件，模型可能不完整
    ) else (
        echo ✅ pytorch_model.bin 文件已存在
    )
) else (
    echo ❌ 未找到模型文件
)
echo.

echo ======================================
echo 下一步操作
echo ======================================
echo.
echo 1. 如果模型都已下载，可以直接运行 whisperx_align.py
echo 2. 启动 Java 服务，上传 Word 文档测试
echo 3. 如果遇到问题，查看日志中的错误信息
echo.

pause
goto END

:END
echo.
echo 脚本执行完成。
echo.
