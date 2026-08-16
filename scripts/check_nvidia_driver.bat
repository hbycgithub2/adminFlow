@echo off
chcp 65001 > nul
echo ========================================
echo NVIDIA驱动版本检查工具
echo ========================================
echo.

echo 1. 当前驱动版本：
nvidia-smi --query-gpu=driver_version --format=csv,noheader
echo.

echo 2. GPU型号：
nvidia-smi --query-gpu=name --format=csv,noheader
echo.

echo 3. CUDA版本：
nvidia-smi --query-gpu=cuda_version --format=csv,noheader
echo.

echo ========================================
echo 升级步骤：
echo 1. 访问 https://www.nvidia.com/drivers
echo 2. 下载最新Studio驱动（推荐）或Game Ready驱动
echo 3. 安装时选择"全新安装"（Clean Install）
echo 4. 重启电脑后重新测试
echo ========================================
pause
