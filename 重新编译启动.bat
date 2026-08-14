@echo off
echo ========================================
echo 强制重新编译并启动服务
echo ========================================

cd /d d:\code\adminFlow\hm-service

echo.
echo [1/3] 清理旧的编译文件...
call mvn clean

echo.
echo [2/3] 重新编译项目（跳过测试）...
call mvn compile -DskipTests

echo.
echo [3/3] 启动服务...
call mvn spring-boot:run

pause
