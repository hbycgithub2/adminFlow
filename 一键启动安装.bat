@echo off
color 0A
chcp 65001 > nul
setlocal enabledelayedexpansion

:: 设置窗口标题
title FFmpeg 6.1.1 一键安装向导

cls
echo.
echo    ╔═══════════════════════════════════════════════════════════════╗
echo    ║                                                               ║
echo    ║          FFmpeg 6.1.1 一键安装向导                            ║
echo    ║                                                               ║
echo    ║          解决问题: Driver does not support NVENC API          ║
echo    ║                                                               ║
echo    ╚═══════════════════════════════════════════════════════════════╝
echo.
echo.
echo    当前状态:
echo    ────────────────────────────────────────────────────────
echo    ✅ Java代码已配置 (FFmpegUtil.java)
echo    ✅ 安装脚本已准备
echo    ❌ FFmpeg 6.1.1 未安装 ^<-- 即将安装
echo.
echo.
echo    安装内容:
echo    ────────────────────────────────────────────────────────
echo    📦 7-Zip (约2MB, 解压工具)
echo    📦 FFmpeg 6.1.1 LTS (约30MB, 视频处理工具)
echo.
echo.
echo    预计耗时:
echo    ────────────────────────────────────────────────────────
echo    ⏱️  下载时间: 2-5分钟 (取决于网络速度)
echo    ⏱️  安装时间: 30秒
echo    ⏱️  验证时间: 30秒
echo    ⏱️  总计: 3-6分钟
echo.
echo.
echo    ═══════════════════════════════════════════════════════════════
echo.
echo    准备开始安装吗？
echo.
choice /C YN /M "    [Y] 立即开始安装    [N] 稍后再说" /T 30 /D Y

if errorlevel 2 (
    echo.
    echo    ❌ 用户取消安装
    echo.
    echo    💡 提示: 运行此脚本即可重新开始安装
    echo.
    pause
    exit /b 1
)

:: 开始安装
cls
echo.
echo    ╔═══════════════════════════════════════════════════════════════╗
echo    ║                    开始安装流程                               ║
echo    ╚═══════════════════════════════════════════════════════════════╝
echo.
echo.

:: 调用安装脚本
call "%~dp0scripts\install_7zip_and_ffmpeg.bat"

if %errorlevel% neq 0 (
    color 0C
    echo.
    echo    ╔═══════════════════════════════════════════════════════════════╗
    echo    ║                    安装失败                                   ║
    ╚═══════════════════════════════════════════════════════════════╝
    echo.
    echo    ❌ 安装过程中出现错误
    echo.
    echo    💡 故障排查建议:
    echo       1. 检查网络连接
    echo       2. 查看错误信息
    echo       3. 使用备用方案 (见文档)
    echo.
    echo    📄 详细文档:
    echo       D:\code\adminFlow\README-立即开始.md
    echo.
    pause
    exit /b 1
)

:: 安装成功
color 0A
cls
echo.
echo    ╔═══════════════════════════════════════════════════════════════╗
echo    ║                                                               ║
echo    ║                 🎉 安装完成！                                 ║
echo    ║                                                               ║
echo    ╚═══════════════════════════════════════════════════════════════╝
echo.
echo.
echo    ✅ 安装结果:
echo    ────────────────────────────────────────────────────────
echo       ✓ 7-Zip 已安装
echo       ✓ FFmpeg 6.1.1 已安装
echo       ✓ GPU编码器检查完成
echo.
echo.
echo    📍 安装路径:
echo    ────────────────────────────────────────────────────────
echo       D:\ai\codex\ffmpeg-6.1.1-essentials_build\
echo.
echo.
echo    🔄 下一步操作:
echo    ────────────────────────────────────────────────────────
echo.
echo       第1步: 重启Spring Boot服务
echo              cd D:\code\adminFlow\hm-service
echo              mvn spring-boot:run
echo.
echo       第2步: 测试视频生成 (新开CMD窗口)
echo              curl -X POST http://localhost:8080/api/video/generate ^
echo                   -H "Content-Type: application/json" ^
echo                   -d "{\"documentId\":\"test\",\"text\":\"测试\"}"
echo.
echo       第3步: 验证GPU加速
echo              type logs\spring.log ^| findstr "GPU"
echo.
echo.
echo    📄 详细文档:
echo    ────────────────────────────────────────────────────────
echo       快速执行卡片: D:\code\adminFlow\快速执行卡片.txt
echo       完整指南:     D:\code\adminFlow\README-立即开始.md
echo.
echo.
echo    ═══════════════════════════════════════════════════════════════
echo.
echo.

:: 询问是否打开文档
choice /C YN /M "    是否打开快速执行卡片？[Y/N]" /T 10 /D N

if errorlevel 1 (
    if errorlevel 2 (
        echo.
        echo    按任意键退出...
        pause >nul
        exit /b 0
    )
    
    :: 打开快速执行卡片
    notepad D:\code\adminFlow\快速执行卡片.txt
)

echo.
echo    按任意键退出...
pause >nul
exit /b 0
