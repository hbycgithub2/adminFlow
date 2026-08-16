@echo off
chcp 65001 > nul

echo ========================================
echo 创建桌面快捷方式
echo ========================================
echo.

:: 获取当前用户桌面路径
set "DESKTOP=%USERPROFILE%\Desktop"

:: 创建快捷方式（使用PowerShell）
powershell -Command "$WS = New-Object -ComObject WScript.Shell; $SC = $WS.CreateShortcut('%DESKTOP%\安装FFmpeg 6.1.1.lnk'); $SC.TargetPath = '%~dp0scripts\install_7zip_and_ffmpeg.bat'; $SC.WorkingDirectory = '%~dp0scripts'; $SC.Description = '一键安装FFmpeg 6.1.1和7-Zip'; $SC.Save()"

if %errorlevel% equ 0 (
    echo ✅ 快捷方式创建成功！
    echo.
    echo 📍 位置：%DESKTOP%\安装FFmpeg 6.1.1.lnk
    echo.
    echo 💡 使用方法：双击桌面上的"安装FFmpeg 6.1.1"图标即可开始安装
) else (
    echo ❌ 创建失败
)

echo.
pause
