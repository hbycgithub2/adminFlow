# Maven 依赖刷新指南

## 问题
编译错误：`程序包org.apache.poi.xwpf.usermodel不存在`

## 原因
缺少 Apache POI 依赖包（已添加到 pom.xml，但未下载）

## 解决方案

### 方法1：IDEA 刷新 Maven（推荐）⭐

1. **打开 Maven 面板**
   - 点击 IDEA 右侧的 "Maven" 按钮
   - 或使用快捷键：`Ctrl + Shift + A`，输入 "Maven"

2. **刷新依赖**
   - 在 Maven 面板中，找到 `hm-service` 模块
   - 点击刷新按钮（🔄循环箭头图标）
   - 或右键点击 `hm-service` → `Maven` → `Reimport`

3. **等待下载**
   - Maven 会自动下载 Apache POI 依赖（约 10MB）
   - 下载时间：1-3分钟（取决于网络）

4. **验证**
   - 下载完成后，打开 `WordDocumentParser.java`
   - 检查导入语句是否还有红色波浪线
   - 如果没有红色波浪线，说明依赖已成功添加

### 方法2：命令行刷新 Maven

```bash
# 进入项目目录
cd d:\code\adminFlow

# 刷新依赖
mvn clean install -DskipTests

# 或者只下载依赖
mvn dependency:resolve
```

### 方法3：手动下载（备用方案）

如果网络问题导致下载失败，可以手动下载 POI jar 包：

1. 下载地址：https://poi.apache.org/download.html
2. 下载 `poi-ooxml-5.2.3.jar` 及其依赖
3. 放到本地 Maven 仓库：`~\.m2\repository\org\apache\poi\poi-ooxml\5.2.3\`

## 添加的依赖

已在 `hm-service/pom.xml` 中添加：

```xml
<!-- Apache POI for Word document parsing -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>
```

## 依赖说明

**poi-ooxml** 包含：
- `poi-ooxml-5.2.3.jar`（主包）
- `poi-5.2.3.jar`（核心包）
- `poi-ooxml-lite-5.2.3.jar`（精简包）
- `xmlbeans-5.1.1.jar`（XML 解析）
- `commons-compress-1.21.jar`（压缩支持）

## 验证依赖是否成功

### 检查1：Maven 面板
在 IDEA 的 Maven 面板中：
- 展开 `hm-service` → `Dependencies`
- 查找 `poi-ooxml:5.2.3`
- 如果能看到，说明依赖已添加

### 检查2：编译项目
```bash
# 编译 hm-service 模块
mvn compile -f hm-service/pom.xml

# 如果编译成功，说明依赖已生效
```

### 检查3：代码提示
打开 `WordDocumentParser.java`：
```java
import org.apache.poi.xwpf.usermodel.XWPFDocument;
```
- 如果能看到代码提示，说明依赖已生效
- 如果还是红色波浪线，尝试重启 IDEA

## 常见问题

### Q1：刷新后还是报错？
**A：** 尝试以下步骤：
1. `File` → `Invalidate Caches / Restart`
2. 重启 IDEA
3. 重新刷新 Maven

### Q2：下载速度慢？
**A：** 配置 Maven 镜像（已配置阿里云镜像）：
```xml
<!-- settings.xml -->
<mirror>
  <id>aliyunmaven</id>
  <mirrorOf>*</mirrorOf>
  <name>阿里云公共仓库</name>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

### Q3：网络问题导致下载失败？
**A：** 等待网络恢复后，再次点击刷新按钮

## 下一步

依赖添加成功后，可以继续测试：
1. 参考 `WORD-DOCUMENT-TTS-测试指南.md`
2. 参考 `快速测试-Word文档TTS.md`
3. 运行集成测试：`WordDocumentTTSIntegrationTest`

---

**更新时间：** 2026-08-14 01:05  
**状态：** ✅ 依赖已添加到 pom.xml，等待 Maven 刷新
