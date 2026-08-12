@echo off
echo =============================================
echo adminFlow 项目启动脚本
echo =============================================
echo.

echo [1/4] 检查 MySQL 是否运行...
mysql -uroot -proot -e "SELECT 1" > nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] MySQL 未启动，请先启动 MySQL！
    pause
    exit /b 1
)
echo [√] MySQL 已启动

echo.
echo [2/4] 检查 Redis 是否运行...
redis-cli -h 127.0.0.1 -p 6379 -a 123456 ping > nul 2>&1
if %errorlevel% neq 0 (
    echo [警告] Redis 未启动，项目可能无法正常运行
    echo [提示] 请先启动 Redis: redis-server
    pause
) else (
    echo [√] Redis 已启动
)

echo.
echo [3/4] 初始化数据库（如果已初始化请按 Ctrl+C 跳过）...
pause
mysql -uroot -proot < hmall.sql
if %errorlevel% neq 0 (
    echo [错误] 数据库初始化失败！
    pause
    exit /b 1
)
echo [√] 数据库初始化成功

echo.
echo [4/4] 启动 Spring Boot 项目...
cd hm-service
mvn spring-boot:run

pause
