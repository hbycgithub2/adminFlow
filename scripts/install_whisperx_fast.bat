@echo off
chcp 65001 >nul
echo ======================================
echo WhisperX 快速安装脚本 (使用国内镜像)
echo ======================================
echo.

REM 切换到脚本所在目录
cd /d "%~dp0"

echo [步骤1] 检查 Python 3.13...
py -3.13 --version
if errorlevel 1 (
    echo [错误] 未找到 Python 3.13！
    echo 请先安装 Python 3.13.15
    pause
    exit /b 1
)
echo [成功] Python 3.13 已安装
echo.

echo [步骤2] 升级 pip (使用清华镜像)...
py -3.13 -m pip install --upgrade pip -i https://pypi.tuna.tsinghua.edu.cn/simple
echo.

echo [步骤3] 安装 PyTorch (使用清华镜像)...
echo 提示: PyTorch 约 200MB，请耐心等待...
py -3.13 -m pip install torch torchaudio --index-url https://download.pytorch.org/whl/cpu -i https://pypi.tuna.tsinghua.edu.cn/simple
if errorlevel 1 (
    echo [警告] 清华镜像失败，尝试官方源...
    py -3.13 -m pip install torch torchaudio --index-url https://download.pytorch.org/whl/cpu
)
echo.

echo [步骤4] 验证 PyTorch 安装...
py -3.13 -c "import torch; print('PyTorch 版本:', torch.__version__)"
if errorlevel 1 (
    echo [错误] PyTorch 安装失败！
    pause
    exit /b 1
)
echo [成功] PyTorch 已安装
echo.

echo [步骤5] 安装 WhisperX (使用清华镜像)...
echo 提示: 这可能需要 5-10 分钟...
py -3.13 -m pip install whisperx -i https://pypi.tuna.tsinghua.edu.cn/simple
if errorlevel 1 (
    echo [警告] 清华镜像失败，尝试阿里云镜像...
    py -3.13 -m pip install whisperx -i https://mirrors.aliyun.com/pypi/simple/
    if errorlevel 1 (
        echo [警告] 阿里云镜像失败，使用官方源...
        py -3.13 -m pip install whisperx
    )
)
echo.

echo [步骤6] 验证 WhisperX 安装...
py -3.13 -c "import whisperx; print('WhisperX 版本:', whisperx.__version__)"
if errorlevel 1 (
    echo [错误] WhisperX 安装失败！
    pause
    exit /b 1
)
echo.

echo [步骤7] 安装其他依赖...
py -3.13 -m pip install ffmpeg-python -i https://pypi.tuna.tsinghua.edu.cn/simple
echo.

echo ======================================
echo 安装完成！
echo ======================================
echo.
echo 已安装的包:
py -3.13 -m pip list | findstr /i "torch whisperx ffmpeg"
echo.
echo 下一步: 配置 application.yml
echo.

pause
