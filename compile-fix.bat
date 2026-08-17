@echo off
chcp 65001 > nul
echo ========================================
echo 方案H - 编译修复脚本
echo ========================================
echo.

echo [步骤1] 检查Java环境...
java -version 2>&1 | findstr "version" > nul
if errorlevel 1 (
    echo ❌ Java未安装或未配置到PATH
    echo.
    echo 🔧 解决方案：
    echo 1. 下载并安装Java 11：
    echo    https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html
    echo.
    echo 2. 配置环境变量：
    echo    JAVA_HOME = C:\Program Files\Java\jdk-11
    echo    PATH += %%JAVA_HOME%%\bin
    echo.
    echo 3. 重启命令行窗口
    echo.
    pause
    exit /b 1
)

echo ✅ Java已安装
java -version 2>&1 | findstr "11\."
if errorlevel 1 (
    echo.
    echo ⚠️ 警告：当前Java版本不是11
    echo 项目需要Java 11才能编译
    echo.
    java -version
    echo.
    echo 🔧 解决方案：
    echo 1. 安装Java 11
    echo 2. 或修改JAVA_HOME指向Java 11
    echo.
    pause
    exit /b 1
)

echo ✅ Java 11环境正确
echo.

echo [步骤2] 清理项目...
call mvn clean -q
if errorlevel 1 (
    echo ❌ Maven清理失败
    pause
    exit /b 1
)
echo ✅ 清理完成
echo.

echo [步骤3] 编译项目（跳过测试）...
echo 这可能需要几分钟...
echo.
call mvn compile -DskipTests
if errorlevel 1 (
    echo.
    echo ========================================
    echo ❌ 编译失败
    echo ========================================
    echo.
    echo 🔍 常见问题：
    echo 1. Java版本不是11
    echo 2. Maven依赖下载失败（网络问题）
    echo 3. 代码语法错误
    echo.
    echo 📝 详细错误信息见上方输出
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ 编译成功！
echo ========================================
echo.
echo 📁 编译输出：
echo    hm-common\target\classes\
echo    hm-service\target\classes\
echo.
echo 🚀 下一步：
echo    1. 运行服务：start-adminFlow.bat
echo    2. 测试局部编辑功能
echo.
pause
