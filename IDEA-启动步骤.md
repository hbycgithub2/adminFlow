# IDEA 启动步骤（推荐）

## 🎯 问题原因

Maven命令行编译失败，原因：
- ❌ JDK版本配置问题（项目要求Java 17，但系统未配置）
- ❌ 命令行没有正确的Java环境变量

**解决方案：** 使用IDEA启动（IDEA自带JDK）✅

---

## 📝 IDEA启动步骤

### 步骤1：打开项目

1. 打开IDEA
2. 文件 → 打开 → 选择 `D:\code\adminFlow`
3. 等待Maven自动加载依赖

### 步骤2：配置JDK（如果需要）

1. 文件 → 项目结构（Ctrl+Alt+Shift+S）
2. 项目设置 → 项目
3. SDK：选择JDK 11或JDK 17
   - 如果没有，点击"添加SDK" → "下载JDK"
4. 点击"应用"

### 步骤3：Maven重新导入（如果需要）

1. 右键点击 `pom.xml`
2. Maven → 重新加载项目
3. 等待依赖下载完成

### 步骤4：编译项目

**方式1：IDEA自动编译（推荐）**
```
构建 → 重新构建项目
```

**方式2：Maven命令**
```
右键 pom.xml → Maven → 编译
```

### 步骤5：启动服务

1. 找到主类：`com.hmall.HMallApplication`
2. 右键 → 运行 'HMallApplication'
3. 或点击类旁边的绿色▶️按钮

**第一次启动可能需要1-2分钟（Maven下载依赖）**

### 步骤6：验证启动

看到以下日志表示成功：
```
Started HMallApplication in xxx seconds
```

---

## ✅ 验证服务

### 1. 访问测试页面

```
http://localhost:8080/edge-tts-test.html
```

### 2. 测试TTS功能

1. 输入文本："你好，这是测试"
2. 选择语音：晓晓
3. 点击"生成语音"
4. 应该成功播放

### 3. 查看日志

IDEA控制台应该看到：
```
🎤 [Edge TTS Core] 执行命令: py -m edge_tts --voice zh-CN-XiaoxiaoNeural ...
🎤 [Edge TTS Core] 输出文件: d:/code/adminFlow/temp/tts_xxx.mp3
✅ [Edge TTS Core] 进程退出: exitCode=0
✅ [Edge TTS Core] 生成成功: 8064 bytes
```

---

## 🔧 常见问题

### 问题1：Maven依赖下载失败

**解决方案：**
1. 检查网络连接
2. 配置Maven镜像（阿里云）
3. 文件 → 设置 → 构建、执行、部署 → 构建工具 → Maven
4. 用户设置文件：编辑 `settings.xml`

```xml
<mirrors>
    <mirror>
        <id>aliyun</id>
        <mirrorOf>central</mirrorOf>
        <name>Aliyun Maven</name>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
</mirrors>
```

### 问题2：端口8080被占用

**解决方案1：修改端口**
```yaml
# application.yaml
server:
  port: 8081  # 改成其他端口
```

**解决方案2：关闭占用8080的进程**
```bash
# Windows
netstat -ano | findstr :8080
taskkill /F /PID <进程ID>
```

### 问题3：Redis连接失败

**解决方案：**
```bash
# 启动Redis
redis-server

# 或在Windows服务中启动Redis
services.msc → Redis → 启动
```

### 问题4：MySQL连接失败

**解决方案：**
```bash
# 检查MySQL是否启动
mysql -uroot -proot

# 如果没启动，启动MySQL服务
net start mysql
```

---

## 📊 项目结构

```
adminFlow/
├── hm-common/              # 公共模块
│   └── src/main/java/
│       └── com/hmall/common/
├── hm-service/             # 核心服务（主模块）⭐
│   ├── src/main/java/
│   │   └── com/hmall/
│   │       ├── HMallApplication.java  # 主类（启动入口）⭐
│   │       ├── tts/                   # TTS模块
│   │       ├── controller/
│   │       ├── service/
│   │       └── ...
│   └── src/main/resources/
│       ├── application.yaml           # 配置文件
│       └── static/
│           └── edge-tts-test.html     # 测试页面
└── pom.xml                 # Maven主配置
```

---

## 🎯 启动前检查清单

- [ ] IDEA已安装
- [ ] JDK 11或17已配置
- [ ] Maven依赖已下载（第一次需要时间）
- [ ] MySQL已启动（端口3306）
- [ ] Redis已启动（端口6379）
- [ ] Python已安装（edge-tts需要）
- [ ] edge-tts已安装（`py -m pip install edge-tts`）
- [ ] 端口8080未被占用

---

## 🚀 快速启动（一键）

如果所有依赖都准备好了：

1. **打开IDEA**
2. **找到 `HMallApplication` 类**
3. **点击绿色▶️按钮**
4. **等待启动完成**
5. **访问测试页面**

完成！

---

## 📝 修改记录

**2026-08-12 19:45**
- 将JDK版本从17改为11（避免命令行编译问题）
- 临时目录从相对路径改为绝对路径
- 增强日志输出（详细的TTS执行日志）

---

**推荐使用IDEA启动！** IDEA会自动处理编译、依赖、JDK等问题。

命令行启动容易遇到环境变量问题。
