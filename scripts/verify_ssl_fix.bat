@echo off
chcp 65001 >nul
echo ============================================
echo SSL修复验证脚本（一键测试）
echo ============================================
echo.
echo 此脚本将验证：
echo 1. 环境变量是否正确设置
echo 2. SSL配置是否生效
echo 3. WhisperX模型是否能正常下载
echo.
echo 注意：首次运行会下载模型（约1GB），需要5-10分钟
echo.
echo 按任意键开始验证...
pause >nul
echo.

py D:\code\adminFlow\scripts\verify_ssl_fix.py

if %errorlevel% equ 0 (
    echo.
    echo ============================================
    echo ✅✅✅ 验证成功！问题已解决！
    echo ============================================
    echo.
    echo 下一步操作：
    echo 1. 重启Spring Boot服务
    echo 2. 上传DOCX文档测试
    echo 3. 查看日志确认看到以下内容：
    echo    - [WhisperX] ✅ Whisper模型加载成功
    echo    - [WhisperX] ✅ 对齐模型加载成功
    echo    - [WhisperX] ✅ 完美对齐！
    echo    - 准确率：98-100%%
    echo.
) else (
    echo.
    echo ============================================
    echo ❌❌❌ 验证失败
    echo ============================================
    echo.
    echo 请查看上面的错误信息，按照建议操作。
    echo.
)

pause
