# adminFlow 项目快速启动指南

> **项目类型：** Spring Boot 2.7.12 + MyBatis-Plus + Redis + MySQL  
> **原项目名：** hmall（黑马商城）  
> **当前项目名：** adminFlow  
> **默认端口：** 8080

---

## 📋 环境要求

### 必需环境
- **JDK 11**
- **MySQL 8.0+**（用户名：root，密码：root）
- **Redis 5.0+**（密码：123456）
- **Maven 3.5+**

### 可选环境
- **Sentinel 控制台**（端口：8088，用于流量控制）

---

## 🚀 快速启动（自动脚本）

### Windows 系统
```bash
# 1. 进入项目目录
cd D:\code\adminFlow

# 2. 执行启动脚本（会自动检查环境并初始化数据库）
start-adminFlow.bat
```

### 手动启动步骤
如果自动脚本失败，可以手动执行以下步骤：

---

## 🔧 手动启动步骤

### 步骤1：启动 MySQL
```bash
# 确保 MySQL 运行在 3306 端口
# 用户名：root，密码：root

# 测试连接
mysql -uroot -proot -e "SELECT 1"
```

### 步骤2：初始化数据库
```bash
# 在项目根目录执行
mysql -uroot -proot < hmall.sql
```

**数据库说明：**
- 数据库名：`hmall`
- 包含 8 张表：user、item、order、order_detail、cart、address、order_logistics、pay_order
- 包含测试数据：3 个用户、5 个商品

**测试账号：**
- 用户名：`admin`，密码：`123456`
- 用户名：`user1`，密码：`123456`
- 用户名：`user2`，密码：`123456`

### 步骤3：启动 Redis
```bash
# 方法1：直接启动（默认配置）
redis-server

# 方法2：带密码启动
redis-server --requirepass 123456

# 测试连接
redis-cli -h 127.0.0.1 -p 6379 -a 123456 ping
# 应该返回：PONG
```

### 步骤4：启动 Spring Boot 项目
```bash
# 方法1：Maven 启动（推荐）
cd hm-service
mvn clean spring-boot:run

# 方法2：打包后启动
mvn clean package
java -jar target/hm-service.jar

# 方法3：IDEA 启动
# 打开 HMallApplication.java，点击绿色三角形运行
```

---

## ✅ 验证启动是否成功

### 验证1：查看启动日志
```
启动成功标志：
  ____          _            __ _
 / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v2.7.12)

...
2026-08-12 17:00:00.000  INFO 12345 --- [main] com.hmall.HMallApplication : Started HMallApplication in 8.5 seconds
```

### 验证2：访问接口文档（Knife4j）
```
浏览器访问：http://localhost:8080/doc.html
```

### 验证3：测试接口
```bash
# 测试健康检查
curl http://localhost:8080/hi
# 应该返回：Hi

# 测试商品列表
curl http://localhost:8080/items
# 应该返回：JSON 格式的商品列表
```

---

## 📊 核心功能模块

### 1. 用户模块
- 用户注册
- 用户登录（JWT Token）
- 用户信息查询

### 2. 商品模块
- 商品列表查询
- 商品详情查询
- 商品分页查询

### 3. 购物车模块
- 添加购物车
- 查询购物车
- 修改购物车数量
- 删除购物车

### 4. 订单模块
- 创建订单
- 查询订单
- 取消订单
- 订单支付

### 5. 地址模块
- 添加地址
- 查询地址
- 修改地址
- 删除地址

---

## 🔍 常见问题排查

### 问题1：启动失败 - 端口被占用
**错误信息：**
```
Web server failed to start. Port 8080 was already in use.
```

**解决方案：**
```bash
# 方法1：修改端口（application.yaml）
server:
  port: 8081

# 方法2：找到占用端口的进程并关闭
netstat -ano | findstr :8080
taskkill /F /PID [进程ID]
```

### 问题2：数据库连接失败
**错误信息：**
```
Communications link failure
```

**解决方案：**
```bash
# 1. 检查 MySQL 是否启动
mysql -uroot -proot -e "SELECT 1"

# 2. 检查配置文件（application.yaml）
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/hmall?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
    username: root
    password: root
```

### 问题3：Redis 连接失败
**错误信息：**
```
Unable to connect to Redis
```

**解决方案：**
```bash
# 1. 检查 Redis 是否启动
redis-cli -h 127.0.0.1 -p 6379 -a 123456 ping

# 2. 检查配置文件（application.yaml）
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password: 123456
```

### 问题4：Sentinel 连接失败（可忽略）
**错误信息：**
```
Failed to fetch metric from <http://localhost:8088/...>
```

**解决方案：**
```
这是正常的，Sentinel 是可选的
如果不需要流量控制功能，可以忽略此错误
项目仍然可以正常运行
```

---

## 📁 项目结构

```
adminFlow/
├── hm-common/                # 公共模块
├── hm-service/              # 服务模块（主模块）
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/hmall/
│   │   │   │       ├── HMallApplication.java  # 启动类 ⭐
│   │   │   │       ├── controller/            # 控制器层
│   │   │   │       ├── service/               # 服务层
│   │   │   │       ├── mapper/                # 数据访问层
│   │   │   │       ├── domain/                # 实体类
│   │   │   │       ├── config/                # 配置类
│   │   │   │       └── utils/                 # 工具类
│   │   │   └── resources/
│   │   │       ├── application.yaml           # 主配置文件 ⭐
│   │   │       ├── application-dev.yaml       # 开发环境配置
│   │   │       └── mapper/                    # MyBatis XML
│   │   └── test/                             # 测试代码
│   └── pom.xml
├── hmall.sql                # 数据库初始化脚本 ⭐
├── start-adminFlow.bat      # 启动脚本 ⭐
├── README-启动指南.md        # 本文档 ⭐
└── pom.xml                  # Maven 父 POM
```

---

## 🎯 下一步计划

### 开发任务
- [ ] 了解项目业务逻辑
- [ ] 修改项目名称（代码中还有 hmall 相关命名）
- [ ] 添加新的业务功能
- [ ] 优化性能（Redis 缓存、SQL 优化）

### 部署任务
- [ ] 配置生产环境
- [ ] Docker 容器化部署
- [ ] 配置 Nginx 反向代理
- [ ] 配置 HTTPS

---

## 📞 技术支持

**项目作者：** 虎哥（黑马程序员）  
**项目修改：** adminFlow（基于 hmall 修改）

**相关文档：**
- Spring Boot 官方文档：https://spring.io/projects/spring-boot
- MyBatis-Plus 官方文档：https://baomidou.com/
- Redis 官方文档：https://redis.io/docs/

---

**最后更新时间：** 2026-08-12  
**版本：** v1.0  
**创建者：** Kiro
