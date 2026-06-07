# Novel2Script - AI小说转剧本工具

基于大语言模型的小说自动转剧本工具，支持将 TXT、Markdown、Word 格式的小说文件转换为结构化的 YAML 剧本。

演示视频：[小说转剧本AI工具演示_哔哩哔哩_bilibili](https://www.bilibili.com/video/BV1ULEt6zErZ/?vd_source=36a5da1f2b3017a011dde33926bfff07)

## 功能特性

- **多格式支持**：支持 TXT、Markdown (.md)、Word (.docx) 三种常见小说格式
- **智能章节识别**：自动识别多种章节标题格式（第X章、Chapter X、数字序号等）
- **AI剧本转换**：基于通义千问大模型，自动生成符合行业标准的结构化剧本
- **YAML标准输出**：输出标准 YAML 格式剧本，可直接阅读、编辑和二次加工
- **实时进度跟踪**：转换过程中实时显示进度百分比和当前处理章节
- **断点续传**：支持取消、重试失败的转换任务
- **Web界面**：提供友好的 Web 操作界面

## 技术栈

| 技术 | 说明 |
|------|------|
| Java 21 | 开发语言 |
| Spring Boot 3.2.5 | 应用框架 |
| MyBatis Plus | ORM框架 |
| MySQL 8.0+ | 数据库 |
| Thymeleaf | 模板引擎 |
| Bootstrap 5 | 前端UI框架 |
| LangChain4j | AI模型集成 |
| 通义千问 qwen3.6-plus | 大语言模型 |
| Jackson YAML | YAML生成 |
| Apache POI | Word文档解析 |
| CommonMark | Markdown解析 |

---

## 快速开始

### 1. 环境要求

| 环境 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 21+ | 推荐使用 OpenJDK 或 Oracle JDK |
| MySQL | 8.0+ | 需要提前安装并启动服务 |
| Maven | 3.6+ | 用于编译和运行项目 |

### 2. 克隆项目

```bash
git clone https://github.com/Yishi-Eve/Novel2Script.git
cd Novel2Script
```

### 3. 数据库配置

#### 3.1 创建数据库

登录 MySQL 并创建数据库：

```bash
mysql -u root -p
```

```sql
-- 创建数据库
CREATE DATABASE novel2script 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE novel2script;
```

#### 3.2 导入数据表

SQL 文件位于 `src/main/resources/sql/schema.sql`，执行以下命令导入：

```bash
# 方式一：在项目根目录执行
mysql -u root -p novel2script < src/main/resources/sql/schema.sql

# 方式二：登录MySQL后执行
mysql -u root -p
```
```sql
USE novel2script;
SOURCE src/main/resources/sql/schema.sql;
```

#### 3.3 修改数据库连接（可选）

如果你的 MySQL 用户名/密码不是 `root/root`，需要修改 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/novel2script?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true
    username: your_username    # 修改为你的MySQL用户名
    password: your_password    # 修改为你的MySQL密码
```

### 4. 配置阿里云百炼平台 API

本项目使用阿里云百炼平台的通义千问大模型，需要申请 API Key。

#### 4.1 申请 API Key

1. 访问 [阿里云百炼平台](https://bailian.console.aliyun.com/)
2. 注册或登录阿里云账号
3. 首次使用需要**开通百炼服务**（免费开通）
4. 进入控制台，点击左侧菜单 **「API-KEY 管理」**
5. 点击 **「创建新的 API-KEY」**
6. 复制生成的 API Key（格式类似：`sk-xxxxxxxxxxxxxxxxxxxxxxxx`）

> ⚠️ **注意**：API Key 只在创建时显示一次，请务必妥善保存！

#### 4.2 配置 API Key

**方式一：设置环境变量（推荐，安全）**

```bash
# Windows PowerShell
$env:BaiLian_QiNiu_API_KEY="sk-你的API-Key"

# Windows CMD
set BaiLian_QiNiu_API_KEY=sk-你的API-Key

# Linux / Mac
export BaiLian_QiNiu_API_KEY=sk-你的API-Key
```

**方式二：直接修改配置文件**

编辑 `src/main/resources/application.yml`，将 API Key 直接写入：

```yaml
langchain4j:
  community:
    dashscope:
      chat-model:
        api-key: sk-你的API-Key    # 替换为你的API Key
        model-name: qwen3.6-plus   # 通义千问模型名称
        max-tokens: 32768
        temperature: 0.7
```

#### 4.3 模型说明

| 模型 | 说明 | 推荐场景 |
|------|------|----------|
| qwen3.6-plus | 通义千问旗舰模型 | 推荐，效果最好 |
| qwen-plus | 通义千问增强版 | 性价比高 |
| qwen-turbo | 通义千问快速版 | 速度快，效果一般 |

### 5. 编译运行

```bash
# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

### 6. 访问应用

打开浏览器访问：**http://localhost:8080**

---

## 使用指南

### 上传小说

1. 点击首页 **「开始转换」** 按钮
2. 在上传页面，拖拽文件或点击 **「选择文件」**
3. 支持的文件格式：
   - `.txt` - 纯文本文件
   - `.md` - Markdown 文件
   - `.docx` - Word 文档
4. 文件大小限制：**10MB**

### 开始转换

1. 上传成功后，点击 **「开始AI转换」** 按钮
2. 系统会自动跳转到剧本预览页
3. 转换过程中可以看到实时进度

### 查看剧本

转换完成后，可以：
- **YAML源码**：查看原始 YAML 格式剧本
- **结构预览**：查看可视化展示的剧本结构
- **下载YAML**：将剧本保存为 YAML 文件

### 管理历史

访问 **「历史记录」** 页面可以：
- 查看所有上传的小说
- 查看转换状态
- 重新转换失败的任务
- 删除不需要的记录

---

## 项目结构

```
Novel2Script/
├── src/main/java/com/qiniu/novel2script/
│   ├── ai/                        # AI服务接口
│   │   ├── ScriptConverter.java   # 剧本转换器
│   │   ├── SummaryGenerator.java  # 摘要生成器
│   │   └── OverviewGenerator.java # 概览生成器
│   ├── config/                    # 配置类
│   │   ├── StorageProperties.java # 文件存储配置
│   │   ├── ScriptConvertProperties.java # 转换配置
│   │   └── AsyncConfig.java       # 异步任务配置
│   ├── controller/                # 控制器
│   │   ├── PageController.java    # 页面路由
│   │   ├── NovelController.java   # 小说API
│   │   └── ScriptController.java  # 剧本API
│   ├── dto/                       # 数据传输对象
│   │   ├── Chapter.java           # 章节
│   │   ├── ChapterScript.java     # 章节剧本
│   │   ├── Character.java         # 角色
│   │   └── Scene.java             # 场景
│   ├── entity/                    # 实体类
│   │   ├── NovelUpload.java       # 小说上传记录
│   │   └── ScriptOutput.java      # 剧本输出记录
│   ├── enums/                     # 枚举类
│   ├── mapper/                    # MyBatis Mapper
│   ├── service/                   # 服务层
│   │   ├── impl/                  # 服务实现
│   │   └── splitter/              # 章节分割器
│   └── Novel2ScriptApplication.java
├── src/main/resources/
│   ├── prompts/                   # AI提示词模板
│   │   ├── convert-system.txt     # 转换系统提示词
│   │   ├── convert-user.txt       # 转换用户提示词
│   │   ├── summary-system.txt     # 摘要系统提示词
│   │   └── overview-system.txt    # 概览系统提示词
│   ├── sql/
│   │   └── schema.sql             # 数据库初始化脚本
│   ├── templates/                 # Thymeleaf模板
│   │   ├── fragments/layout.html  # 公共布局
│   │   ├── index.html             # 首页
│   │   ├── upload.html            # 上传页
│   │   ├── script.html            # 剧本预览页
│   │   └── history.html           # 历史记录页
│   ├── static/                    # 静态资源
│   │   ├── css/style.css          # 自定义样式
│   │   └── js/app.js              # 前端逻辑
│   └── application.yml            # 应用配置
├── uploads/                       # 文件上传目录
│   └── scripts/                   # 生成的剧本文件
└── pom.xml                        # Maven配置
```

---

## API 文档

### 小说管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/novel/upload` | 上传小说文件（form-data） |
| `GET` | `/api/novel/{id}` | 获取小说信息 |
| `GET` | `/api/novels?page=0&size=10` | 获取小说列表（分页） |
| `DELETE` | `/api/novel/{id}` | 删除小说及其剧本 |

### 剧本转换

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/convert/{novelId}` | 触发AI转换 |
| `GET` | `/api/convert/{id}/status` | 获取转换状态/进度 |
| `POST` | `/api/convert/{id}/cancel` | 取消转换 |
| `POST` | `/api/convert/{id}/retry` | 重试失败的转换 |

### 剧本查询

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/script/novel/{novelId}` | 获取剧本（含YAML内容） |
| `GET` | `/api/script/novel/{novelId}/download` | 下载YAML文件 |

### 页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | 首页 | 项目介绍、最近记录 |
| `/upload` | 上传页 | 文件上传 |
| `/script?novelId={id}` | 剧本预览页 | 查看转换结果 |
| `/history` | 历史记录页 | 管理所有记录 |

---

## YAML 输出格式

```yaml
script:
  metadata:
    title: 小说标题
    total_episodes: 5          # 总幕数
    total_scenes: 20           # 总场景数
    created_at: '2026-06-07T20:00:00'
    version: '1.0'
  characters:                  # 角色表
    - name: 角色名
      description: 角色描述
  episodes:                    # 剧本内容
    - episode_number: 1
      title: 幕标题
      scenes:
        - scene_number: 1
          scene_header: 地点 时间 内外景  # 如：出租屋 夜 内
          content: |-
            场景内容...
```

---

## 测试文件

项目提供了多个测试文件，位于 `uploads/` 目录：

| 文件名 | 测试场景 |
|--------|----------|
| `test-novel.txt` | 标准3章武侠小说（推荐） |
| `test-不足3章.txt` | 章节数不足验证 |
| `test-章节内容很短.txt` | 短内容处理 |
| `test-不支持格式.xml` | 格式限制验证 |
| `test-不支持格式.json` | 格式限制验证 |

---

## 常见问题

### 1. API Key 相关问题

**问题**：`API request failed` 或 `Invalid API key`

**解决方案**：
- 检查 API Key 是否正确配置
- 确认已在百炼平台开通服务
- 检查 API Key 是否有调用额度

### 2. 数据库连接失败

**问题**：`Communications link failure` 或 `Access denied`

**解决方案**：
- 确认 MySQL 服务已启动
- 检查 `application.yml` 中的用户名和密码
- 确认数据库 `novel2script` 已创建
- 确认已执行 `schema.sql` 导入数据表

### 3. 端口被占用

**问题**：`Port 8080 was already in use`

**解决方案**：
```bash
# 方式一：查找并关闭占用端口的进程
netstat -ano | findstr :8080
taskkill /F /PID <进程ID>

# 方式二：修改端口号
# 编辑 application.yml，修改 server.port
server:
  port: 8081
```

### 4. 章节数量不足

**问题**：`章节数量不足，至少需要3个章节`

**解决方案**：
- 确保小说至少有3个章节
- 章节标题格式建议使用 `第X章` 或 `Chapter X`
- 避免章节标题过长（建议50字符以内）

### 5. 文件上传失败

**问题**：`文件格式不支持` 或 `文件过大`

**解决方案**：
- 仅支持 `.txt`、`.md`、`.docx` 格式
- 文件大小限制 10MB
- 检查文件编码是否为 UTF-8

### 6. YAML 解析失败

**问题**：`YAML 解析失败: unknown tag !`

**解决方案**：
- 确保使用最新版本的代码
- 删除旧的剧本记录，重新转换

---

## 开发说明

### 修改 AI 模型

编辑 `src/main/resources/application.yml`：

```yaml
langchain4j:
  community:
    dashscope:
      chat-model:
        model-name: qwen-plus  # 可改为其他模型
```

### 修改提示词

提示词文件位于 `src/main/resources/prompts/`：
- `convert-system.txt` - 转换任务的系统提示词
- `convert-user.txt` - 转换任务的用户提示词
- `summary-system.txt` - 摘要生成的系统提示词
- `overview-system.txt` - 概览生成的系统提示词

### 修改章节识别规则

编辑 `src/main/java/com/qiniu/novel2script/service/splitter/ChapterPattern.java` 添加新的正则表达式。

---

## 许可证

本项目仅供学习和研究使用。

## 致谢

- [Spring Boot](https://spring.io/projects/spring-boot)
- [LangChain4j](https://github.com/langchain4j/langchain4j)
- [阿里云百炼平台](https://bailian.console.aliyun.com/)
- [通义千问](https://qwen.aliyun.com/)
- [Bootstrap](https://getbootstrap.com/)
- [MyBatis Plus](https://baomidou.com/)
