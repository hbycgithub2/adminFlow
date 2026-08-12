@echo off
echo =============================================
echo adminFlow 服务快速重启
echo =============================================

cd hm-service
echo [1/2] 编译项目...
call mvn clean compile -DskipTests -Dmaven.compiler.source=17 -Dmaven.compiler.target=17

if %errorlevel% neq 0 (
    echo [错误] 编译失败！
    pause
    exit /b 1
)

echo.
echo [2/2] 启动服务...
call mvn spring-boot:run

pause
