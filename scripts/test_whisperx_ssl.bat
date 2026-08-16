@echo off
chcp 65001 >nul
echo ======================================
echo WhisperX SSL修复验证脚本
echo ======================================
echo.

cd /d "%~dp0"

echo [步骤1] 验证 Python 3.13...
py -3.13 --version
if errorlevel 1 (
    echo [错误] Python 3.13 未找到
    pause
    exit /b 1
)
echo.

echo [步骤2] 验证 WhisperX 安装...
py -3.13 -c "import whisperx; print('[成功] WhisperX 已安装')"
if errorlevel 1 (
    echo [错误] WhisperX 未安装
    pause
    exit /b 1
)
echo.

echo [步骤3] 测试 SSL 配置（尝试导入模型）...
echo 提示：这会触发模型下载，请观察是否有 SSL 错误
echo.

py -3.13 -c "import os; import ssl; import warnings; warnings.filterwarnings('ignore'); ssl._create_default_https_context = ssl._create_unverified_context; os.environ['HTTPX_VERIFY'] = 'false'; os.environ['HF_HUB_DISABLE_SSL_VERIFY'] = '1'; os.environ['NO_PROXY'] = '*'; os.environ['HTTP_PROXY'] = ''; os.environ['HTTPS_PROXY'] = ''; import httpx; _original_client_init = httpx.Client.__init__; def patched_init(self, *args, **kwargs): kwargs['verify'] = False; return _original_client_init(self, *args, **kwargs); httpx.Client.__init__ = patched_init; import whisperx; print('[SSL测试] 开始加载模型...'); model = whisperx.load_model('base', device='cpu', language='zh'); print('[SSL测试] ✅ 模型加载成功！SSL配置有效！')"

if errorlevel 1 (
    echo.
    echo [错误] SSL配置测试失败！
    echo 请检查以上错误信息。
    echo.
    echo 常见问题：
    echo 1. 如果看到 "SSL: CERTIFICATE_VERIFY_FAILED"，说明SSL配置未生效
    echo 2. 如果看到 "Connection timeout"，说明网络问题
    echo 3. 如果看到 "No module named"，说明依赖未安装
    echo.
) else (
    echo.
    echo ======================================
    echo ✅ SSL配置测试通过！
    echo ======================================
    echo.
    echo WhisperX 可以正常下载模型，SSL证书验证已禁用。
    echo.
    echo 下一步：
    echo 1. 启动 Java 服务
    echo 2. 上传 Word 文档
    echo 3. 观察 WhisperX 对齐结果
    echo.
)

pause
