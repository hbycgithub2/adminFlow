@echo off
echo ===== 测试编译DocumentTTSServiceImpl =====
cd /d D:\code\adminFlow\hm-service

echo.
echo 设置JAVA_HOME...
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_271
set PATH=%JAVA_HOME%\bin;%PATH%

echo.
echo 检查Java版本...
java -version

echo.
echo 开始Maven编译...
D:\code\wap\apache-maven-3.5.0\bin\mvn.cmd compile -DskipTests -e

echo.
echo ===== 编译完成 =====
pause
