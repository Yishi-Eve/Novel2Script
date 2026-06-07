# Novel2Script - AI小说转剧本工具

基于大语言模型的小说自动转剧本工具，支持将 TXT、Markdown、Word 格式的小说文件转换为结构化的 YAML 剧本。

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
| MySQL | 数据库 |
| Thymeleaf | 模板引擎 |
| Bootstrap 5 | 前端UI框架 |
| LangChain4j | AI模型集成 |
| 通义千问 | 大语言模型 |
| Jackson YAML | YAML生成 |
| Apache POI | Word文档解析 |
| CommonMark | Markdown解析 |

## 快速开始

### 环境要求

- JDK 21+
- MySQL 8.0+
- Maven 3.6+

### 安装步骤

1. **克隆项目**
```bash
git clone https://github.com/Yishi-Eve/Novel2Script.git
cd Novel2Script
```

2. **创建数据库**
```sql
CREATE DATABASE novel2script DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **执行初始化脚本**
```bash
mysql -u root -p novel2script < sql/schema.sql
```

4. **配置AI服务**

编辑 `src/main/resources/application.yml`，配置通义千问 API Key：
```yaml
langchain4j:
  community:
    dashscope:
      chat-model:
        api-key: your-api-key-here
        model-name: qwen-plus
```

5. **编译运行**
```bash
mvn clean compile
mvn spring-boot:run
```

6. **访问应用**

打开浏览器访问 http://localhost:8080

## 项目结构

```
Novel2Script/
├── src/main/java/com/qiniu/novel2script/
│   ├── config/                    # 配置类
│   │   ├── StorageProperties.java # 文件存储配置
│   │   └── AsyncConfig.java       # 异步任务配置
│   ├── controller/                # 控制器
│   │   ├── PageController.java    # 页面路由
│   │   ├── NovelController.java   # 小说API
│   │   └── ScriptController.java  # 剧本API
│   ├── dto/                       # 数据传输对象
│   │   ├── Chapter.java           # 章节
│   │   ├── ChapterScript.java     # 章节剧本
│   │   ├── Character.java         # 角色
│   │   ├── Scene.java             # 场景
│   │   └── ConvertStatus.java     # 转换状态
│   ├── entity/                    # 实体类
│   │   ├── NovelUpload.java       # 小说上传记录
│   │   └── ScriptOutput.java      # 剧本输出记录
│   ├── enums/                     # 枚举类
│   │   ├── FileType.java          # 文件类型
│   │   ├── NovelStatus.java       # 小说状态
│   │   └── ScriptStatus.java      # 剧本状态
│   ├── mapper/                    # MyBatis Mapper
│   ├── service/                   # 服务层
│   │   ├── impl/                  # 服务实现
│   │   └── splitter/              # 章节分割器
│   └── Novel2ScriptApplication.java
├── src/main/resources/
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
├── sql/                           # SQL脚本
├── uploads/                       # 文件上传目录
│   ├── 2026/06/07/                # 按日期存储的小说文件
│   └── scripts/                   # 生成的剧本文件
└── pom.xml                        # Maven配置
```

## API文档

### 小说管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/novel/upload` | 上传小说文件 |
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

| 路径 | 页面 |
|------|------|
| `/` | 首页 |
| `/upload` | 上传页 |
| `/script?novelId={id}` | 剧本预览页 |
| `/history` | 历史记录页 |

## YAML输出格式

```yaml
script:
  metadata:
    title: 小说标题
    total_episodes: 5
    total_scenes: 20
    created_at: '2026-06-07T20:00:00'
    version: '1.0'
  characters:
    - name: 角色名
      description: 角色描述
  episodes:
    - episode_number: 1
      title: 幕标题
      scenes:
        - scene_number: 1
          scene_header: 场景标题
          content: |-
            场景内容...
```

## 使用说明

1. **上传小说**：在上传页选择或拖拽小说文件（支持 TXT/MD/DOCX）
2. **开始转换**：上传成功后点击"开始AI转换"按钮
3. **查看进度**：在剧本预览页实时查看转换进度
4. **预览剧本**：转换完成后可查看 YAML 源码或结构预览
5. **下载剧本**：点击"下载YAML"按钮保存剧本文件

## 测试文件

项目提供了多个测试文件用于功能验证：

| 文件名 | 测试场景 |
|--------|----------|
| `test-novel.txt` | 标准3章武侠小说 |
| `test-恰好3章.txt` | 最低章节数要求 |
| `test-不足3章.txt` | 章节数不足验证 |
| `test-不支持格式.xml` | 格式限制验证 |
| `test-章节内容很短.txt` | 短内容处理 |
| `test-超长标题.txt` | 长标题处理 |

## 许可证

本项目仅供学习和研究使用。

## 致谢

- [Spring Boot](https://spring.io/projects/spring-boot)
- [LangChain4j](https://github.com/langchain4j/langchain4j)
- [通义千问](https://dashscope.aliyun.com/)
- [Bootstrap](https://getbootstrap.com/)
