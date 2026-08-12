@echo off
chcp 65001 >nul
echo =============================================
echo Edge TTS 项目启动脚本
echo =============================================
echo.

echo [1/5] 检查 Python 环境...
py --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Python 未安装！
    pause
    exit /b 1
)
echo [√] Python 已安装

echo.
echo [2/5] 检查 edge-tts...
py -m edge_tts --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] edge-tts 未安装！
    echo [提示] 请运行: install-edge-tts.bat
    pause
    exit /b 1
)
echo [√] edge-tts 已安装

echo.
echo [3/5] 检查 Java 环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Java 未配置！
    echo [提示] 请配置 JDK 11 环境变量
    echo.
    echo 配置方法：
    echo 1. 找到 JDK 11 安装路径（例如：C:\Program Files\Java\jdk-11.0.12）
    echo 2. 设置环境变量：
    echo    JAVA_HOME = C:\Program Files\Java\jdk-11.0.12
    echo    Path 添加: %%JAVA_HOME%%\bin
    echo 3. 重新打开命令行窗口
    echo.
    pause
    exit /b 1
)
echo [√] Java 已配置

echo.
echo [4/5] 编译 hm-common 模块...
cd hm-common
call mvn clean install -DskipTests
if %errorlevel% neq 0 (
    echo [错误] hm-common 编译失败！
    cd ..
    pause
    exit /b 1
)
cd ..
echo [√] hm-common 编译成功

echo.
echo [5/5] 启动 Spring Boot 项目...
cd hm-service
echo.
echo =============================================
echo 🚀 正在启动项目...
echo 📝 启动成功后访问：http://localhost:8080/edge-tts-test.html
echo 📝 按 Ctrl+C 停止项目
echo =============================================
echo.
call mvn spring-boot:run

pause
