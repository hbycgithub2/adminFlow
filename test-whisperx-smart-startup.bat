@echo off
chcp 65001 > nul
echo ========================================
echo WhisperX智能启动测试脚本
echo ========================================
echo.

echo [测试1] 检查配置文件是否更新...
findstr "idle-timeout-minutes: 30" hm-service\src\main\resources\application.yaml >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ 配置文件已更新
) else (
    echo ❌ 配置文件未更新
    pause
    exit /b 1
)

echo.
echo [测试2] 检查管理Controller是否存在...
if exist "hm-service\src\main\java\com\hmall\tts\whisperx\controller\WhisperXManagementController.java" (
    echo ✅ 管理Controller已创建
) else (
    echo ❌ 管理Controller不存在
    pause
    exit /b 1
)

echo.
echo [测试3] 检查管理页面是否存在...
if exist "hm-service\src\main\resources\static\whisperx-manager.html" (
    echo ✅ 管理页面已创建
) else (
    echo ❌ 管理页面不存在
    pause
    exit /b 1
)

echo.
echo [测试4] 检查使用指南是否存在...
if exist "WhisperX-智能按需启动使用指南.md" (
    echo ✅ 使用指南已创建
) else (
    echo ❌ 使用指南不存在
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ 所有文件检查通过！
echo ========================================
echo.
echo 下一步操作：
echo 1. 编译项目：mvn clean compile -DskipTests
echo 2. 启动服务：cd hm-service ^& mvn spring-boot:run
echo 3. 访问管理页面：http://localhost:8080/whisperx-manager.html
echo 4. 查看使用指南：WhisperX-智能按需启动使用指南.md
echo.
pause
