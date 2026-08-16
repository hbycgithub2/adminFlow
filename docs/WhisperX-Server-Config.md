# WhisperX服务配置指南

## 📋 配置说明

在 `application.yml` 中添加以下配置：

```yaml
# WhisperX配置
whisperx:
  # 是否使用HTTP服务（常驻进程模式，推荐）
  use:
    server: true
  
  # HTTP服务地址
  server:
    url: http://localhost:5000
  
  # Python命令（自动检测，无需修改）
  python:
    command: auto
  
  # 脚本路径（兼容模式，回退使用）
  script:
    path: D:/code/adminFlow/scripts/whisperx_align.py
  
  # 临时文件目录
  temp:
    dir: D:/code/adminFlow/temp/whisperx
  
  # 超时时间（秒）
  timeout:
    seconds: 120
```

## 🚀 启动服务

### 方式1：Windows批处理（推荐）

双击运行：
```
D:\code\adminFlow\scripts\start_whisperx_server.bat
```

### 方式2：命令行启动

```bash
cd D:\code\adminFlow\scripts
py -3.13 whisperx_server.py
```

## 📊 性能对比

| 模式 | 3行文档耗时 | 10行文档耗时 | 说明 |
|------|------------|-------------|------|
| 原模式（逐行） | 93秒 | 300秒 | 每次重新加载模型 |
| 服务模式（批量） | 35秒 | 35秒 | 模型只加载一次⭐ |

## 🔧 故障排查

### 问题1：服务无法启动

**症状：** 运行 `start_whisperx_server.bat` 报错

**原因：** 缺少Flask依赖

**解决：**
```bash
py -3.13 -m pip install flask -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 问题2：Java端报错"HTTP服务不可用"

**症状：** 日志显示"服务不可用，回退到Python脚本模式"

**原因：** WhisperX服务未启动

**解决：**
1. 检查服务是否运行：访问 http://localhost:5000/health
2. 如果无法访问，启动服务：`start_whisperx_server.bat`

### 问题3：端口5000被占用

**症状：** 服务启动时报错"Address already in use"

**解决：**
1. 修改 `whisperx_server.py` 最后一行：
   ```python
   app.run(host='0.0.0.0', port=5001, debug=False, threaded=True)
   ```

2. 修改 `application.yml`：
   ```yaml
   whisperx:
     server:
       url: http://localhost:5001
   ```

## ✅ 验证服务

### 健康检查

访问：http://localhost:5000/health

预期返回：
```json
{
  "status": "ok",
  "model_loaded": true,
  "align_model_loaded": true
}
```

### 测试单个对齐

```bash
curl -X POST http://localhost:5000/align \
  -H "Content-Type: application/json" \
  -d "{\"audio\":\"test.mp3\",\"text\":\"你好\"}"
```

### 测试批量对齐

```bash
curl -X POST http://localhost:5000/align_batch \
  -H "Content-Type: application/json" \
  -d "{\"requests\":[{\"audio\":\"test1.mp3\",\"text\":\"你好\"},{\"audio\":\"test2.mp3\",\"text\":\"世界\"}]}"
```

## 📝 使用建议

1. **开发环境：** 服务模式（速度快，方便调试）
2. **生产环境：** 服务模式 + 自动重启（systemd/supervisor）
3. **一次性任务：** Python脚本模式（无需启动服务）

## 🔄 自动启动（可选）

### Windows任务计划程序

1. 打开"任务计划程序"
2. 创建基本任务
3. 触发器：登录时
4. 操作：启动程序
   - 程序：`D:\code\adminFlow\scripts\start_whisperx_server.bat`
5. 完成

### Linux Systemd（生产环境）

创建 `/etc/systemd/system/whisperx.service`：

```ini
[Unit]
Description=WhisperX Alignment Service
After=network.target

[Service]
Type=simple
User=your-user
WorkingDirectory=/opt/adminFlow/scripts
ExecStart=/usr/bin/python3.13 whisperx_server.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
sudo systemctl daemon-reload
sudo systemctl enable whisperx
sudo systemctl start whisperx
```

## 📊 监控建议

1. **内存监控：** 服务占用约2GB内存
2. **日志监控：** 查看 `whisperx_server.py` 输出
3. **健康检查：** 定期访问 `/health` 接口

---

**最后更新：** 2026-08-16  
**版本：** v1.0（A+B优化方案）
