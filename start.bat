@echo off
chcp 65001 >nul
echo ========================================
echo 启动 HMall TTS 服务
echo ========================================
echo.

REM 设置Java 21环境
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%

echo Java版本：
java -version
echo.

echo ========================================
echo 正在启动服务...
echo ========================================
cd /d "%~dp0hm-service"
java -jar target\hm-service-1.0.0.jar

pause
