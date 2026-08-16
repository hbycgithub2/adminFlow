@echo off
chcp 65001 >nul
echo ========================================
echo WhisperX安装脚本
echo ========================================
echo.

echo [1/5] 检查Python环境...
py --version
if errorlevel 1 (
    echo ❌ Python未安装，请先安装Python 3.8+
    pause
    exit /b 1
)
echo ✅ Python已安装
echo.

echo [2/5] 升级pip...
py -m pip install --upgrade pip
echo ✅ pip已升级
echo.

echo [3/5] 安装WhisperX...
echo 正在安装，这可能需要3-5分钟...
py -m pip install whisperx -i https://pypi.tuna.tsinghua.edu.cn/simple
if errorlevel 1 (
    echo ❌ WhisperX安装失败，尝试使用默认源...
    py -m pip install whisperx
    if errorlevel 1 (
        echo ❌ WhisperX安装失败
        pause
        exit /b 1
    )
)
echo ✅ WhisperX已安装
echo.

echo [4/5] 验证安装...
py -c "import whisperx; print('WhisperX导入成功')"
if errorlevel 1 (
    echo ❌ WhisperX验证失败
    pause
    exit /b 1
)
echo ✅ WhisperX验证成功
echo.

echo [5/5] 检查GPU支持...
py -c "import torch; print('✅ GPU可用' if torch.cuda.is_available() else '⚠️ GPU不可用（将使用CPU，速度较慢）')"
echo.

echo ========================================
echo 安装完成！
echo ========================================
echo.
echo 下一步：
echo 1. 在application.yml中配置WhisperX参数
echo 2. 重启Spring Boot服务
echo 3. 测试TTS字幕同步功能
echo.
echo 详细配置说明请查看：docs/WhisperX配置说明.md
echo.
pause
