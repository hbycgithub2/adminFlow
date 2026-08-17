@echo off
chcp 65001 > nul
echo ========================================
echo Java环境检查工具
echo ========================================
echo.

echo [检查1] Java是否安装...
where java > nul 2>&1
if errorlevel 1 (
    echo ❌ Java未安装或未配置到PATH
    echo.
    echo 当前PATH环境变量：
    echo %PATH%
    echo.
    goto :install_guide
) else (
    echo ✅ Java已安装
    echo.
)

echo [检查2] Java版本...
java -version 2>&1
echo.

echo [检查3] JAVA_HOME环境变量...
if "%JAVA_HOME%"=="" (
    echo ⚠️ JAVA_HOME未设置
    echo.
) else (
    echo ✅ JAVA_HOME = %JAVA_HOME%
    echo.
)

echo [检查4] Maven是否安装...
where mvn > nul 2>&1
if errorlevel 1 (
    echo ❌ Maven未安装或未配置到PATH
    echo.
    echo 🔧 解决方案：
    echo 1. 下载Maven：https://maven.apache.org/download.cgi
    echo 2. 解压到 C:\Program Files\Maven
    echo 3. 配置环境变量：
    echo    MAVEN_HOME = C:\Program Files\Maven
    echo    PATH += %%MAVEN_HOME%%\bin
    echo.
) else (
    echo ✅ Maven已安装
    mvn -version
    echo.
)

echo [检查5] 项目Java配置...
echo 项目要求：Java 11
echo.
findstr /C:"maven.compiler.source" pom.xml
findstr /C:"maven.compiler.target" pom.xml
echo.

echo ========================================
echo 检查完成
echo ========================================
echo.

java -version 2>&1 | findstr "11\." > nul
if errorlevel 1 (
    echo ⚠️ 当前Java版本不是11，编译可能失败
    echo.
    goto :install_guide
) else (
    echo ✅ 环境配置正确，可以编译
    echo.
    echo 运行以下命令编译：
    echo    compile-fix.bat
    echo.
)

pause
exit /b 0

:install_guide
echo.
echo ========================================
echo 📚 Java 11安装指南
echo ========================================
echo.
echo 1️⃣ 下载Java 11：
echo    https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html
echo    或使用开源版本：
echo    https://adoptium.net/temurin/releases/?version=11
echo.
echo 2️⃣ 安装到默认位置：
echo    C:\Program Files\Java\jdk-11
echo.
echo 3️⃣ 配置环境变量（系统变量）：
echo    新建变量：
echo      变量名：JAVA_HOME
echo      变量值：C:\Program Files\Java\jdk-11
echo.
echo    编辑PATH变量，添加：
echo      %%JAVA_HOME%%\bin
echo.
echo 4️⃣ 验证安装：
echo    打开新的命令行窗口
echo    运行：java -version
echo    应该显示：java version "11.x.x"
echo.
echo 5️⃣ 重新运行本脚本验证
echo.
pause
exit /b 1
