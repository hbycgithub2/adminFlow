#!/bin/bash

echo "🚀 Redis快速启动脚本"
echo

# 检查Docker是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker未安装"
    echo "💡 请先安装Docker:"
    echo "   Ubuntu/Debian: sudo apt install docker.io"
    echo "   CentOS/RHEL: sudo yum install docker"
    echo "   Mac: brew install docker"
    echo
    echo "🔧 或者手动安装Redis:"
    echo "   Ubuntu/Debian: sudo apt install redis-server"
    echo "   CentOS/RHEL: sudo yum install redis"
    echo "   Mac: brew install redis"
    exit 1
fi

echo "✅ Docker已安装，正在启动Redis..."
echo

# 停止可能存在的Redis容器
echo "🛑 停止现有Redis容器..."
docker stop redis-server 2>/dev/null
docker rm redis-server 2>/dev/null

# 启动新的Redis容器
echo "🚀 启动Redis容器..."
if docker run -d --name redis-server -p 6379:6379 redis:latest; then
    echo
    echo "✅ Redis启动成功！"
    echo "📍 连接地址: localhost:6379"
    echo
    
    # 等待Redis完全启动
    echo "⏳ 等待Redis完全启动..."
    sleep 3
    
    # 测试Redis连接
    echo "🔍 测试Redis连接..."
    if docker exec redis-server redis-cli ping | grep -q "PONG"; then
        echo
        echo "🎉 Redis启动完成，可以运行测试了！"
        echo
        echo "📋 常用命令:"
        echo "   运行测试: mvn test -Dredis.test.enabled=true"
        echo "   停止Redis: docker stop redis-server"
        echo "   查看日志: docker logs redis-server"
        echo "   连接Redis: docker exec -it redis-server redis-cli"
        echo
    else
        echo "❌ Redis连接测试失败"
    fi
else
    echo "❌ Redis启动失败"
    echo "💡 请检查Docker是否正常运行"
fi
