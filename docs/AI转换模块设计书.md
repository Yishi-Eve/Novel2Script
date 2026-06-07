# AI转换模块设计书

## 一、模块概述

### 1.1 模块定位

AI转换模块（ScriptConvertService）是Novel2Script系统的核心业务模块，负责将章节分割模块输出的结构化章节数据通过阿里云百炼API转换为符合行业规范的剧本格式。该模块采用LangChain4j AI Services架构，实现声明式AI调用，简化代码并提升可维护性。

**核心创新**：采用混合策略充分利用qwen3.6-plus的1M上下文窗口能力，通过全书概览生成和滑动窗口转换，确保角色一致性和情节连贯性。

### 1.2 设计目标

| 目标 | 说明 |
|------|------|
| 转换质量 | 生成符合中国影视剧本行业规范的剧本内容 |
| 上下文连贯 | 利用1M上下文窗口，保持角色一致性和情节连贯性 |
| 异步处理 | 支持后台异步转换，避免长时间阻塞用户请求 |
| 进度反馈 | 实时反馈转换进度，提升用户体验 |
| 容错机制 | 实现自动重试、超时控制、错误恢复等容错策略 |
| 声明式API | 使用LangChain4j AI Services，简化AI调用代码 |

### 1.3 技术选型

| 技术 | 版本 | 说明 |
|------|------|------|
| LangChain4j | 1.15.0-beta25 | AI应用开发框架 |
| langchain4j-community-dashscope | 1.15.0-beta25 | 阿里云百炼集成 |
| langchain4j-spring-boot-starter | 1.15.0-beta25 | Spring Boot自动配置 |
| qwen3.6-plus | - | 阿里云百炼大模型，支持1M上下文窗口 |

### 1.4 依赖关系

```
┌─────────────────────┐
│    章节分割模块      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│    AI转换模块        │◄──── 本模块
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│    YAML生成模块      │
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│    剧本输出模块      │
└─────────────────────┘
```

### 1.5 模型能力与Token策略

**qwen3.6-plus模型规格：**
- 最大上下文窗口：**1,000,000 tokens (1M)**
- 单章输入限制：**128,000 tokens (128K)**
- 输出token限制：**32,768 tokens (32K)**

**Token分配策略（128K限制）：**

| 组件 | Token数 | 说明 |
|------|---------|------|
| 系统提示词 | 2K | 角色定义、格式要求 |
| 全书概览 | 5-10K | 角色表、情节线、地点表 |
| 前文上下文 | 30-50K | 前面2-4章内容 |
| 当前章节 | 50-70K | 待转换章节 |
| 输出缓冲 | 20-30K | 剧本输出 |
| **总计** | **107-162K** | 平均128K |

**小说规模与Token关系：**

| 章节类型 | 字数 | Token数 | 是否需要分片 |
|----------|------|---------|-------------|
| 短章（3000字） | 3000 | 5000-6000 | ❌ 不需要 |
| 中等章节（5000字） | 5000 | 7500-10000 | ❌ 不需要 |
| 长章节（10000字） | 10000 | 15000-20000 | ❌ 不需要 |
| 超长章节（20000字） | 20000 | 30000-40000 | ❌ 不需要 |

**结论**：即使是最长的章节，也远低于128K的限制，无需分片处理。

---

## 二、处理流程

### 2.1 整体流程（混合策略）

```
章节分割模块输出 chapters（章节列表）
           │
           ▼
┌─────────────────────────────────────────────────────────────┐
│  AI转换模块（ScriptConvertService）- 混合策略                │
│                                                              │
│  ① 创建转换任务，记录到数据库                                │
│  ② 异步启动转换流程                                          │
│  ③ 生成全书概览（利用1M上下文窗口）                          │
│     └─→ 一次性传入所有章节 → AI生成概览                      │
│         - 角色表（名称、类型、描述、关系）                    │
│         - 情节线（主线、支线、伏笔）                          │
│         - 地点表                                              │
│         - 写作风格分析                                        │
│  ④ 滑动窗口逐章转换（保持上下文连贯）                        │
│     └─→ 窗口大小：3-5章（根据小说规模自适应）                │
│         - 第1章：[概览 + 第1章]                               │
│         - 第2章：[概览 + 第1-2章]                             │
│         - 第3章：[概览 + 第1-3章]                             │
│         - 第4章：[概览 + 第2-4章]（滑动）                     │
│         - 第5章：[概览 + 第3-5章]（滑动）                     │
│  ⑤ 合并转换结果为完整剧本                                    │
│  ⑥ 调用YAML生成模块输出YAML                                  │
│  ⑦ 更新任务状态为完成                                        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
           │
           │ 传递：YAML文件路径
           ▼
┌─────────────────────────────────────┐
│  剧本输出模块                        │
│  - 提供预览和下载功能                │
└─────────────────────────────────────┘
```

### 2.2 混合策略详解

#### 2.2.1 小说规模自适应策略

| 小说规模 | 章节数 | 窗口大小 | 策略 | 说明 |
|----------|--------|----------|------|------|
| 短篇 | <10章 | 全部 | 批量转换 | 一次搞定，上下文最完整 |
| 中篇 | 10-30章 | 5章 | 滑动窗口 | 平衡成本和质量 |
| 长篇 | 30-100章 | 3章 | 全书概览 + 滑动窗口 | 先理解全局，再分段转换 |
| 超长篇 | 100+章 | 3章 | 分卷概览 + 滑动窗口 | 分卷处理，保持卷内连贯 |

#### 2.2.2 为什么采用混合策略？

**传统逐章独立转换的问题：**
```
章节1 ──→ AI转换 ──→ 剧本1
章节2 ──→ AI转换 ──→ 剧本2  (不知道章节1发生了什么)
章节3 ──→ AI转换 ──→ 剧本3  (不知道章节1、2发生了什么)
```

- ❌ 角色性格前后不一致
- ❌ 情节发展不连贯
- ❌ 伏笔、铺垫丢失
- ❌ 场景描写重复

**混合策略的优势：**
```
全书概览 ──→ 角色表、情节线、地点表
              ↓
[概览+章节1] ──→ AI转换 ──→ 剧本1
[概览+章节1+2] ──→ AI转换 ──→ 剧本2
[概览+章节2+3] ──→ AI转换 ──→ 剧本3
```

- ✅ 角色性格保持一致
- ✅ 情节发展连贯
- ✅ 伏笔、铺垫得以保留
- ✅ 场景描写不重复

### 2.3 转换流程详解

**步骤1：创建转换任务**
- 输入：novelId（小说ID）
- 逻辑：
  - 查询小说信息和章节数据
  - 创建script_output记录，状态为CONVERTING
  - 返回转换任务ID
- 输出：convertId（转换任务ID）

**步骤2：异步启动转换流程**
- 输入：convertId
- 逻辑：
  - 使用Spring @Async异步执行转换
  - 更新转换进度
- 输出：无（异步执行）

**步骤3：生成全书概览（新增）**
- 输入：所有章节列表
- 逻辑：
  - 调用OverviewGenerator AI Service
  - 一次性传入所有章节内容（利用1M上下文窗口）
  - AI生成全书概览：角色表、情节线、地点表、写作风格
  - 保存概览结果，供后续转换使用
- 输出：NovelOverview对象

**步骤4：滑动窗口逐章转换（优化）**
- 输入：章节列表、全书概览、窗口大小
- 逻辑：
  - 根据小说规模确定窗口大小
  - 遍历章节列表，使用滑动窗口：
    - 计算窗口范围：`start = max(0, i - windowSize + 1)`, `end = i + 1`
    - 构建上下文：`[概览 + 窗口内章节]`
    - 调用ScriptConverter AI Service转换当前章节
    - 直接返回ChapterScript对象
  - 更新转换进度
- 输出：各章节的ChapterScript对象

**步骤5：合并转换结果**
- 输入：各章节的ChapterScript对象
- 逻辑：
  - 提取元数据（标题、作者、题材等）
  - 合并角色表（去重、合并信息）
  - 合并幕和场景
  - 重新编号场景
- 输出：完整的剧本结构

**步骤6：生成YAML文件**
- 输入：完整的剧本结构
- 逻辑：
  - 调用YamlGeneratorService生成YAML
  - 保存到文件系统
  - 更新数据库中的yaml_file_path
- 输出：YAML文件路径

**步骤7：更新任务状态**
- 输入：转换结果
- 逻辑：
  - 更新script_output表状态为COMPLETED
  - 记录章节数、场景数等统计信息
- 输出：无

### 2.4 进度更新流程（更新）

```
┌─────────────────────────────────────────────────────────┐
│                    进度更新机制                          │
├─────────────────────────────────────────────────────────┤
│  0%   │ 任务创建                                        │
│  10%  │ 开始转换，加载章节数据                          │
│  20%  │ 全书概览生成完成                                │
│  30%  │ 第1章转换完成                                    │
│  40%  │ 第2章转换完成                                    │
│  ...  │ ...                                              │
│  80%  │ 最后一章转换完成                                 │
│  90%  │ 合并结果，生成YAML                               │
│  100% │ 转换完成                                         │
└─────────────────────────────────────────────────────────┘
```

**进度计算算法：**
```
输入：当前阶段 stage，当前章节索引 currentIndex，总章节数 totalChapters
输出：进度百分比 progress

1. 任务创建阶段（0-10%）
   progress = 0 + (taskCreated ? 10 : 0)

2. 全书概览阶段（10-20%）
   progress = 10 + (overviewProgress * 10)

3. 逐章转换阶段（20-90%）
   chapterProgress = (currentIndex / totalChapters) * 70
   progress = 20 + chapterProgress

4. 收尾阶段（90-100%）
   progress = 90 + (finalizeProgress * 10)

5. 限制在0-100范围内
   progress = Math.max(0, Math.min(100, progress))

返回 progress
```

### 2.5 滑动窗口算法

```
输入：章节列表 chapters，全书概览 overview，窗口大小 windowSize
输出：各章节的ChapterScript对象列表 chapterScripts

1. 初始化结果列表 chapterScripts = []

2. 确定窗口大小（根据小说规模自适应）
   if chapters.size < 10:
       windowSize = chapters.size  // 短篇：批量转换
   else if chapters.size <= 30:
       windowSize = 5              // 中篇：5章窗口
   else:
       windowSize = 3              // 长篇：3章窗口

3. 遍历章节列表
   for i = 0 to chapters.size - 1:
       // 计算窗口范围
       start = Math.max(0, i - windowSize + 1)
       end = i + 1
       
       // 构建上下文
       contextChapters = chapters.subList(start, end)
       currentChapter = chapters.get(i)
       
       // 调用AI转换
       chapterScript = scriptConverter.convertChapter(
           overview,
           contextChapters,
           currentChapter
       )
       
       chapterScripts.add(chapterScript)

4. 返回 chapterScripts
```

### 2.6 全书概览生成算法

```
输入：章节列表 chapters
输出：NovelOverview overview

1. 调用OverviewGenerator AI Service
   overview = overviewGenerator.generateOverview(chapters)

2. 概览内容包括：
   - characters: 角色表（名称、类型、描述、关系）
   - plotLines: 情节线（主线、支线、伏笔）
   - locations: 地点表（主要场景地点）
   - writingStyle: 写作风格（语言特点、叙事风格）

3. 返回 overview
```

---

## 三、功能需求

### 3.1 功能列表

| 编号 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| F-001 | 创建转换任务 | P0 | 创建异步转换任务，返回任务ID |
| F-002 | 异步转换执行 | P0 | 后台异步执行转换流程 |
| F-003 | 全书概览生成 | P0 | 利用1M上下文窗口生成全书概览（角色、情节、地点） |
| F-004 | 滑动窗口转换 | P0 | 使用滑动窗口保持上下文连贯的逐章转换 |
| F-005 | 进度查询 | P0 | 查询转换任务的当前进度 |
| F-006 | 结果合并 | P0 | 合并各章转换结果为完整剧本 |
| F-007 | YAML生成 | P0 | 调用YAML生成模块输出YAML文件 |
| F-008 | 任务取消 | P1 | 支持取消正在进行的转换任务 |
| F-009 | 失败重试 | P1 | API调用失败时自动重试（最多3次） |
| F-010 | 错误恢复 | P1 | 转换失败时记录错误信息，支持手动重试 |
| F-011 | 规模自适应 | P1 | 根据小说规模自动选择最优转换策略 |

### 3.2 输入输出

**输入：**
- novelId：小说ID（Long）
- 章节数据：来自章节分割模块的JSON文件

**输出：**
- ConvertResult对象，包含：
  - convertId：转换任务ID
  - status：任务状态
  - message：状态消息

### 3.3 业务规则

| 规则编号 | 规则描述 |
|----------|----------|
| BR-001 | 单章输入限制为128K tokens，输出限制为32K tokens |
| BR-002 | API调用失败时最多重试3次 |
| BR-003 | 单次API调用超时时间为60秒 |
| BR-004 | 转换过程中需更新进度到数据库 |
| BR-005 | 转换失败时需记录错误信息 |
| BR-006 | 同一小说同时只能有一个转换任务 |
| BR-007 | 转换结果必须符合YAML Schema规范 |
| BR-008 | 全书概览生成失败时，回退到简单逐章转换模式 |
| BR-009 | 滑动窗口大小根据小说规模自适应：短篇全量、中篇5章、长篇3章 |
| BR-010 | 前文上下文超过50K tokens时，自动使用摘要替代 |

---

## 四、接口设计

### 4.1 RESTful API

| 方法 | 路径 | 说明 | 请求参数 | 响应 |
|------|------|------|----------|------|
| POST | /api/convert/{novelId} | 触发转换 | - | ConvertResult |
| GET | /api/convert/{id}/status | 获取转换状态 | - | ConvertStatus |
| POST | /api/convert/{id}/cancel | 取消转换 | - | Result |
| POST | /api/convert/{id}/retry | 重试转换 | - | ConvertResult |

### 4.2 服务接口

ScriptConvertService 提供五个核心方法：

- **startConvert(novelId)**：启动转换任务
  - 查询小说和章节信息
  - 创建script_output记录
  - 异步启动转换流程
  - 返回ConvertResult

- **getConvertStatus(convertId)**：获取转换状态
  - 查询script_output表
  - 返回转换进度和状态

- **cancelConvert(convertId)**：取消转换任务
  - 更新任务状态为FAILED
  - 设置错误信息为"用户取消"

- **retryConvert(convertId)**：重试转换任务
  - 查询失败的任务
  - 重新启动转换流程

- **doConvert(convertId)**：执行转换（内部方法）
  - 逐章调用AI Services转换
  - 合并结果
  - 生成YAML
  - 更新状态

### 4.3 AI Service接口

#### ScriptConverter（AI转换服务）

```java
@AiService
public interface ScriptConverter {
    
    @SystemMessage(fromResource = "prompts/convert-system.txt")
    @UserMessage(fromResource = "prompts/convert-user.txt")
    ChapterScript convertChapter(
        @V("overview") String overview,
        @V("contextChapters") String contextChapters,
        @V("title") String title, 
        @V("chapterNumber") int chapterNumber, 
        @V("content") String content
    );
}
```

**说明：**
- 使用`@AiService`注解，Spring Boot自动配置
- 使用`@SystemMessage`加载系统提示词
- 使用`@UserMessage`加载用户提示词模板
- 使用`@V`注解绑定模板变量
- 返回类型为`ChapterScript`，LangChain4j自动解析JSON
- **新增参数**：`overview`（全书概览）、`contextChapters`（前文上下文）

#### OverviewGenerator（全书概览生成服务）

```java
@AiService
public interface OverviewGenerator {
    
    @SystemMessage(fromResource = "prompts/overview-system.txt")
    @UserMessage(fromResource = "prompts/overview-user.txt")
    NovelOverview generateOverview(@V("chapters") String chapters);
}
```

**说明：**
- 利用1M上下文窗口，一次性传入所有章节
- 生成全书概览：角色表、情节线、地点表、写作风格
- 为后续滑动窗口转换提供全局上下文

#### SummaryGenerator（章节摘要生成服务）

```java
@AiService
public interface SummaryGenerator {
    
    @SystemMessage(fromResource = "prompts/summary-system.txt")
    @UserMessage(fromResource = "prompts/summary-user.txt")
    ChapterSummary generateSummary(@V("chapter") String chapter);
}
```

**说明：**
- 当前文上下文超过50K tokens时，用于生成章节摘要
- 摘要替代原文，减少token消耗
- 保留关键信息：角色、情节、场景

### 4.4 数据传输对象

#### ConvertResult（转换结果）

| 字段 | 类型 | 说明 |
|------|------|------|
| convertId | Long | 转换任务ID |
| status | String | 任务状态 |
| message | String | 状态消息 |

#### ConvertStatus（转换状态）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 转换任务ID |
| status | String | 任务状态 |
| progress | Integer | 进度百分比（0-100） |
| currentChapter | Integer | 当前处理的章节 |
| totalChapters | Integer | 总章节数 |
| message | String | 状态消息 |
| errorMessage | String | 错误信息（如果有） |

#### ChapterScript（章节剧本）

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterScript {
    @Description("幕标题，简短概括本章内容")
    private String episodeTitle;
    
    @Description("角色列表")
    private List<Character> characters;
    
    @Description("场景列表")
    private List<Scene> scenes;
}
```

#### Scene（场景）

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scene {
    @Description("场景序号")
    private Integer sceneNumber;
    
    @Description("场景标题行，格式：地点 时间 内外景")
    private String sceneHeader;
    
    @Description("场景内容，包含描写和对话")
    private String content;
}
```

#### Character（角色）

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Character {
    @Description("角色名称")
    private String name;
    
    @Description("角色简短描述")
    private String description;
}
```

#### NovelOverview（全书概览）- 新增

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NovelOverview {
    @Description("角色表")
    private List<CharacterInfo> characters;
    
    @Description("情节线")
    private List<PlotLine> plotLines;
    
    @Description("地点表")
    private List<Location> locations;
    
    @Description("写作风格")
    private WritingStyle writingStyle;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterInfo {
    @Description("角色名称")
    private String name;
    
    @Description("角色描述")
    private String description;
    
    @Description("性格特点")
    private String personality;
    
    @Description("与其他角色关系")
    private String relationships;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlotLine {
    @Description("情节线类型：主线/支线/伏笔")
    private String type;
    
    @Description("情节描述")
    private String description;
    
    @Description("涉及章节")
    private List<Integer> chapters;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    @Description("地点名称")
    private String name;
    
    @Description("地点描述")
    private String description;
    
    @Description("出现章节")
    private List<Integer> chapters;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WritingStyle {
    @Description("语言特点")
    private String languageStyle;
    
    @Description("叙事风格")
    private String narrativeStyle;
    
    @Description("对话特点")
    private String dialogueStyle;
}
```

#### ChapterSummary（章节摘要）- 新增

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterSummary {
    @Description("章节序号")
    private Integer chapterNumber;
    
    @Description("章节标题")
    private String title;
    
    @Description("主要角色")
    private List<String> characters;
    
    @Description("主要情节")
    private String plotSummary;
    
    @Description("关键场景")
    private List<String> keyScenes;
    
    @Description("重要对话")
    private List<String> keyDialogues;
}
```

### 4.5 枚举定义

#### ScriptStatus（剧本状态）

| 枚举值 | 描述 |
|--------|------|
| CONVERTING | 转换中 |
| COMPLETED | 转换完成 |
| FAILED | 转换失败 |
| CANCELLED | 已取消 |

---

## 五、数据库设计

### 5.1 设计原则

1. **只存储文件路径**：YAML内容保存为文件，数据库只存路径
2. **记录转换状态**：实时更新转换进度和状态
3. **错误信息记录**：转换失败时记录详细的错误信息

### 5.2 表结构定义

#### script_output 表（剧本输出记录）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| novel_id | BIGINT | FK, NOT NULL | 关联小说ID |
| title | VARCHAR(255) | NOT NULL | 剧本标题 |
| original_author | VARCHAR(255) | - | 原作者 |
| genre | VARCHAR(50) | - | 题材类型 |
| yaml_file_path | VARCHAR(500) | - | YAML文件路径 |
| status | VARCHAR(20) | NOT NULL | 状态 |
| progress | INT | DEFAULT 0 | 转换进度（0-100） |
| current_chapter | INT | DEFAULT 0 | 当前处理章节 |
| total_chapters | INT | DEFAULT 0 | 总章节数 |
| total_scenes | INT | DEFAULT 0 | 总场景数 |
| error_message | VARCHAR(1000) | - | 错误信息 |
| created_time | DATETIME | NOT NULL | 创建时间 |
| update_time | DATETIME | NOT NULL | 更新时间 |

**索引：**
- idx_novel_id：小说ID索引
- idx_status：状态索引
- idx_created_time：创建时间索引

### 5.3 实体类

ScriptOutput 实体字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| novelId | Long | 关联小说ID |
| title | String | 剧本标题 |
| originalAuthor | String | 原作者 |
| genre | String | 题材类型 |
| yamlFilePath | String | YAML文件路径 |
| status | ScriptStatus | 状态 |
| progress | Integer | 转换进度 |
| currentChapter | Integer | 当前处理章节 |
| totalChapters | Integer | 总章节数 |
| totalScenes | Integer | 总场景数 |
| errorMessage | String | 错误信息 |
| createdTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

### 5.4 Mapper接口

ScriptOutputMapper 继承 BaseMapper，提供以下自定义查询：

- **selectByNovelId(novelId)**：根据小说ID查询剧本
- **selectByStatus(status)**：根据状态查询剧本
- **updateProgress(id, progress, currentChapter)**：更新转换进度
- **updateStatus(id, status)**：更新任务状态
- **updateStatusWithError(id, status, errorMessage)**：更新状态和错误信息

### 5.5 文件存储路径

```
uploads/
├── novels/                    # 小说文件
│   └── 2024/01/15/
│       └── xxx.txt
├── chapters/                  # 章节JSON文件
│   └── novel_1_chapters.json
├── scripts/                   # 剧本YAML文件
│   └── script_1.yaml
└── overviews/                 # 全书概览文件（新增）
    └── novel_1_overview.json
```

---

## 六、类设计

### 6.1 类结构

```
com.qiniu.novel2script
├── ai/
│   ├── ScriptConverter                # AI Service接口（LangChain4j）
│   ├── OverviewGenerator              # 全书概览生成AI服务（新增）
│   └── SummaryGenerator               # 章节摘要生成AI服务（新增）
├── service/
│   ├── ScriptConvertService           # 剧本转换服务接口
│   ├── YamlGeneratorService           # YAML生成服务接口
│   ├── NovelOverviewService           # 全书概览服务接口（新增）
│   └── impl/
│       ├── ScriptConvertServiceImpl   # 剧本转换服务实现
│       ├── YamlGeneratorServiceImpl   # YAML生成服务实现
│       └── NovelOverviewServiceImpl   # 全书概览服务实现（新增）
├── config/
│   ├── AsyncConfig                    # 异步配置
│   └── ScriptConvertProperties        # 转换配置属性（新增）
├── dto/
│   ├── ConvertResult                  # 转换结果DTO
│   ├── ConvertStatus                  # 转换状态DTO
│   ├── ChapterScript                  # 章节剧本DTO
│   ├── Scene                          # 场景DTO
│   ├── Character                      # 角色DTO
│   ├── NovelOverview                  # 全书概览DTO（新增）
│   ├── ChapterSummary                 # 章节摘要DTO（新增）
│   └── overview/                      # 概览相关DTO（新增）
│       ├── CharacterInfo              # 角色信息DTO
│       ├── PlotLine                   # 情节线DTO
│       ├── Location                   # 地点DTO
│       └── WritingStyle               # 写作风格DTO
├── entity/
│   ├── NovelUpload                    # 小说实体（已有）
│   └── ScriptOutput                   # 剧本实体（新增）
├── mapper/
│   ├── NovelUploadMapper              # 小说Mapper（已有）
│   └── ScriptOutputMapper             # 剧本Mapper（新增）
├── enums/
│   └── ScriptStatus                   # 剧本状态枚举（新增）
└── exception/
    └── ScriptConvertException         # 转换异常（新增）
```

### 6.2 核心类设计

#### ScriptConverter（AI Service接口）

```java
package com.qiniu.novel2script.ai;

import com.qiniu.novel2script.dto.ChapterScript;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ScriptConverter {
    
    @SystemMessage(fromResource = "prompts/convert-system.txt")
    @UserMessage(fromResource = "prompts/convert-user.txt")
    ChapterScript convertChapter(
        @V("overview") String overview,
        @V("contextChapters") String contextChapters,
        @V("title") String title, 
        @V("chapterNumber") int chapterNumber, 
        @V("content") String content
    );
}
```

**设计说明：**
- 使用`@AiService`注解，Spring Boot自动创建代理实现
- 使用`@SystemMessage(fromResource = "...")`从资源文件加载系统提示词
- 使用`@UserMessage(fromResource = "...")`从资源文件加载用户提示词模板
- 使用`@V("variable")`注解绑定模板变量
- 返回类型为`ChapterScript`，LangChain4j自动解析JSON为POJO
- 无需手动构建提示词和解析响应
- **新增参数**：`overview`（全书概览）、`contextChapters`（前文上下文）

#### OverviewGenerator（全书概览生成服务）

```java
package com.qiniu.novel2script.ai;

import com.qiniu.novel2script.dto.overview.NovelOverview;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface OverviewGenerator {
    
    @SystemMessage(fromResource = "prompts/overview-system.txt")
    @UserMessage(fromResource = "prompts/overview-user.txt")
    NovelOverview generateOverview(@V("chapters") String chapters);
}
```

**设计说明：**
- 利用1M上下文窗口，一次性传入所有章节内容
- 生成全书概览：角色表、情节线、地点表、写作风格
- 为后续滑动窗口转换提供全局上下文
- 如果生成失败，回退到简单逐章转换模式

#### SummaryGenerator（章节摘要生成服务）

```java
package com.qiniu.novel2script.ai;

import com.qiniu.novel2script.dto.ChapterSummary;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface SummaryGenerator {
    
    @SystemMessage(fromResource = "prompts/summary-system.txt")
    @UserMessage(fromResource = "prompts/summary-user.txt")
    ChapterSummary generateSummary(@V("chapter") String chapter);
}
```

**设计说明：**
- 当前文上下文超过50K tokens时，用于生成章节摘要
- 摘要替代原文，减少token消耗
- 保留关键信息：角色、情节、场景

#### ScriptConvertService（服务接口）

```java
public interface ScriptConvertService {
    /**
     * 启动转换任务
     * @param novelId 小说ID
     * @return 转换结果
     */
    ConvertResult startConvert(Long novelId);
    
    /**
     * 获取转换状态
     * @param convertId 转换任务ID
     * @return 转换状态
     */
    ConvertStatus getConvertStatus(Long convertId);
    
    /**
     * 取消转换任务
     * @param convertId 转换任务ID
     * @return 操作结果
     */
    Result cancelConvert(Long convertId);
    
    /**
     * 重试转换任务
     * @param convertId 转换任务ID
     * @return 转换结果
     */
    ConvertResult retryConvert(Long convertId);
}
```

#### ScriptConvertServiceImpl（服务实现）

依赖：
- ScriptConverter：AI Service（LangChain4j自动注入）
- OverviewGenerator：全书概览生成AI服务（新增）
- SummaryGenerator：章节摘要生成AI服务（新增）
- YamlGeneratorService：YAML生成
- ChapterSplitterService：加载章节
- ScriptOutputMapper：数据库操作
- NovelUploadMapper：小说信息查询
- StorageProperties：存储路径配置
- ScriptConvertProperties：转换配置属性（新增）

**startConvert 方法调用流程：**
1. 查询小说信息，验证状态
2. 检查是否已有进行中的转换任务
3. 加载章节数据
4. 创建ScriptOutput记录，状态为CONVERTING
5. 异步调用doConvert方法
6. 返回ConvertResult

**doConvert 方法调用流程（混合策略）：**
1. **生成全书概览**（新增）
   - 调用overviewGenerator.generateOverview
   - 传入所有章节内容
   - 获取NovelOverview对象
2. **确定窗口大小**（新增）
   - 根据章节数确定窗口大小
   - 短篇（<10章）：全量
   - 中篇（10-30章）：5章
   - 长篇（30+章）：3章
3. **滑动窗口逐章转换**（优化）
   - 遍历章节列表
   - 计算窗口范围
   - 构建上下文：[概览 + 窗口内章节]
   - 调用scriptConverter.convertChapter转换
   - 直接获取ChapterScript对象（无需手动解析JSON）
   - 更新进度
4. 合并所有章节结果
5. 调用YamlGeneratorService生成YAML
6. 保存YAML文件路径到数据库
7. 更新状态为COMPLETED

**异常处理：**
- 捕获所有异常
- 更新状态为FAILED
- 记录错误信息
- 支持手动重试

#### NovelOverviewService（全书概览服务接口）

```java
public interface NovelOverviewService {
    /**
     * 生成全书概览
     * @param novelId 小说ID
     * @param chapters 章节列表
     * @return 全书概览
     */
    NovelOverview generateOverview(Long novelId, List<Chapter> chapters);
    
    /**
     * 从文件加载全书概览
     * @param novelId 小说ID
     * @return 全书概览
     */
    NovelOverview loadOverview(Long novelId);
}
```

#### NovelOverviewServiceImpl（全书概览服务实现）

依赖：
- OverviewGenerator：AI Service（LangChain4j自动注入）
- StorageProperties：存储路径配置

**generateOverview 方法调用流程：**
1. 将章节列表序列化为字符串
2. 调用overviewGenerator.generateOverview
3. 获取NovelOverview对象
4. 保存到JSON文件
5. 返回NovelOverview

#### YamlGeneratorService（YAML生成服务接口）

```java
public interface YamlGeneratorService {
    /**
     * 生成YAML文件
     * @param scriptData 剧本数据
     * @param filePath 文件路径
     * @return 文件路径
     */
    String generateYaml(ScriptData scriptData, String filePath);
}
```

#### YamlGeneratorServiceImpl（YAML生成服务实现）

依赖：
- SnakeYAML：YAML生成库
- StorageProperties：存储路径配置

**generateYaml 方法调用流程：**
1. 构建YAML数据结构
2. 使用SnakeYAML序列化
3. 写入文件
4. 返回文件路径

#### AsyncConfig（异步配置）

配置内容：
- 异步任务执行器
- 线程池配置
- 异步异常处理

#### ScriptConvertProperties（转换配置属性）

```java
@ConfigurationProperties(prefix = "script-convert")
@Data
public class ScriptConvertProperties {
    private int windowSize = 3;           // 滑动窗口大小
    private int overviewMaxTokens = 10000; // 概览最大token
    private int summaryMaxTokens = 2000;   // 摘要最大token
    private int maxRetries = 3;            // 最大重试次数
    private int timeoutSeconds = 60;       // 超时时间
}
```

#### ScriptOutput（剧本实体）

字段：
- id：主键
- novelId：小说ID
- title：剧本标题
- originalAuthor：原作者
- genre：题材类型
- yamlFilePath：YAML文件路径
- status：状态（ScriptStatus枚举）
- progress：转换进度
- currentChapter：当前处理章节
- totalChapters：总章节数
- totalScenes：总场景数
- errorMessage：错误信息
- createdTime：创建时间
- updateTime：更新时间

#### ScriptOutputMapper（剧本Mapper）

继承BaseMapper，提供自定义查询方法。

#### ScriptStatus（剧本状态枚举）

枚举值：
- CONVERTING：转换中
- COMPLETED：转换完成
- FAILED：转换失败
- CANCELLED：已取消

#### ScriptConvertException（转换异常）

继承RuntimeException，提供两个构造方法。

### 6.3 核心类调用关系

```
ScriptConvertServiceImpl
    │
    ├── ScriptConverter（AI Service，LangChain4j自动注入）
    │   └── QwenChatModel（DashScope模型，自动配置）
    │
    ├── OverviewGenerator（全书概览生成，LangChain4j自动注入）
    │   └── QwenChatModel（DashScope模型，自动配置）
    │
    ├── SummaryGenerator（章节摘要生成，LangChain4j自动注入）
    │   └── QwenChatModel（DashScope模型，自动配置）
    │
    ├── YamlGeneratorService
    │   └── StorageProperties（存储路径）
    │
    ├── ChapterSplitterService（加载章节）
    │
    ├── ScriptOutputMapper（数据库操作）
    │
    ├── NovelUploadMapper（查询小说信息）
    │
    └── ScriptConvertProperties（转换配置）
```

### 6.4 LangChain4j自动配置流程

```
┌─────────────────────────────────────────────────────────────┐
│                LangChain4j Spring Boot Starter               │
├─────────────────────────────────────────────────────────────┤
│  1. 读取 application.yml 配置                                │
│     langchain4j.community.dashscope.chat-model.*            │
│                                                              │
│  2. 自动创建 QwenChatModel Bean                              │
│     - api-key                                               │
│     - model-name（qwen3.6-plus）                            │
│     - max-tokens（32768）                                   │
│     - temperature（0.7）                                    │
│                                                              │
│  3. 扫描 @AiService 注解的接口                               │
│     - ScriptConverter                                       │
│     - OverviewGenerator                                     │
│     - SummaryGenerator                                      │
│                                                              │
│  4. 创建 AI Service 代理实现                                 │
│     - 绑定 QwenChatModel                                    │
│     - 处理 @SystemMessage 和 @UserMessage                   │
│     - 处理返回类型解析（ChapterScript、NovelOverview等）      │
│                                                              │
│  5. 注册为 Spring Bean                                       │
│     - 可以直接 @Autowired 注入                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 七、算法设计

### 7.1 转换进度计算算法（更新）

```
输入：当前阶段 stage，当前章节索引 currentIndex，总章节数 totalChapters
输出：进度百分比 progress

1. 任务创建阶段（0-10%）
   progress = 0 + (taskCreated ? 10 : 0)

2. 全书概览阶段（10-20%）
   progress = 10 + (overviewProgress * 10)

3. 逐章转换阶段（20-90%）
   chapterProgress = (currentIndex / totalChapters) * 70
   progress = 20 + chapterProgress

4. 收尾阶段（90-100%）
   progress = 90 + (finalizeProgress * 10)

5. 限制在0-100范围内
   progress = Math.max(0, Math.min(100, progress))

返回 progress

说明：
- 0-10%：任务创建和初始化
- 10-20%：全书概览生成
- 20-90%：滑动窗口逐章转换（每章约占 70/totalChapters %）
- 90-100%：合并结果和生成YAML
```

### 7.2 结果合并算法

```
输入：List<ChapterScript> chapterScripts
输出：ScriptData scriptData

1. 初始化ScriptData
   scriptData = new ScriptData()

2. 提取元数据（从第一章）
   firstChapter = chapterScripts.get(0)
   scriptData.setTitle(firstChapter.getEpisodeTitle())
   scriptData.setTotalEpisodes(chapterScripts.size())

3. 合并角色表
   allCharacters = new LinkedHashMap<>()
   for each chapterScript in chapterScripts:
       for each character in chapterScript.getCharacters():
           if not allCharacters.containsKey(character.getName()):
               allCharacters.put(character.getName(), character)
           else:
               // 合并角色信息
               existing = allCharacters.get(character.getName())
               existing.merge(character)
   scriptData.setCharacters(new ArrayList<>(allCharacters.values()))

4. 合并场景，重新编号
   sceneNumber = 1
   for each chapterScript in chapterScripts:
       episode = new Episode()
       episode.setEpisodeNumber(chapterScript.getChapterNumber())
       episode.setTitle(chapterScript.getEpisodeTitle())
       
       for each scene in chapterScript.getScenes():
           scene.setSceneNumber(sceneNumber++)
           episode.addScene(scene)
       
       scriptData.addEpisode(episode)

5. 计算总场景数
   scriptData.setTotalScenes(sceneNumber - 1)

返回 scriptData
```

### 7.3 滑动窗口算法（新增）

```
输入：章节列表 chapters，全书概览 overview，窗口大小 windowSize
输出：各章节的ChapterScript对象列表 chapterScripts

1. 初始化结果列表 chapterScripts = []

2. 确定窗口大小（根据小说规模自适应）
   if chapters.size < 10:
       windowSize = chapters.size  // 短篇：批量转换
   else if chapters.size <= 30:
       windowSize = 5              // 中篇：5章窗口
   else:
       windowSize = 3              // 长篇：3章窗口

3. 遍历章节列表
   for i = 0 to chapters.size - 1:
       // 计算窗口范围
       start = Math.max(0, i - windowSize + 1)
       end = i + 1
       
       // 构建上下文
       contextChapters = chapters.subList(start, end)
       currentChapter = chapters.get(i)
       
       // 检查上下文token数，如果超过50K，使用摘要
       contextTokens = estimateTokens(contextChapters)
       if contextTokens > 50000:
           // 生成摘要替代前面章节
           summaries = generateSummaries(contextChapters.subList(0, contextChapters.size() - 1))
           context = buildContext(overview, summaries, currentChapter)
       else:
           context = buildContext(overview, contextChapters, currentChapter)
       
       // 调用AI转换
       chapterScript = scriptConverter.convertChapter(
           overview,
           context,
           currentChapter.getTitle(),
           currentChapter.getChapterNumber(),
           currentChapter.getContent()
       )
       
       chapterScripts.add(chapterScript)

4. 返回 chapterScripts
```

### 7.4 全书概览生成算法（新增）

```
输入：章节列表 chapters
输出：NovelOverview overview

1. 将章节列表序列化为字符串
   chaptersText = serializeChapters(chapters)

2. 调用OverviewGenerator AI Service
   overview = overviewGenerator.generateOverview(chaptersText)

3. 概览内容包括：
   - characters: 角色表（名称、类型、描述、关系）
   - plotLines: 情节线（主线、支线、伏笔）
   - locations: 地点表（主要场景地点）
   - writingStyle: 写作风格（语言特点、叙事风格）

4. 保存概览到文件（可选）
   saveOverviewToFile(overview, novelId)

5. 返回 overview
```

### 7.5 章节摘要生成算法（新增）

```
输入：章节 chapter
输出：ChapterSummary summary

1. 调用SummaryGenerator AI Service
   summary = summaryGenerator.generateSummary(chapter)

2. 摘要内容包括：
   - chapterNumber: 章节序号
   - title: 章节标题
   - characters: 主要角色列表
   - plotSummary: 主要情节
   - keyScenes: 关键场景列表
   - keyDialogues: 重要对话列表

3. 返回 summary
```

### 7.6 重试机制算法

LangChain4j内置重试机制，配置方式：

```yaml
langchain4j:
  community:
    dashscope:
      chat-model:
        max-retries: 3
```

**说明：**
- LangChain4j自动处理重试逻辑
- 支持指数退避策略
- 支持配置超时时间
- 无需手动实现重试代码

---

## 八、异常处理

### 8.1 异常类定义

ScriptConvertException 继承 RuntimeException，提供两个构造方法：
- message：异常消息
- message + cause：异常消息和原因链

### 8.2 异常场景

| 异常场景 | 异常类型 | 处理方式 |
|----------|----------|----------|
| 小说不存在 | ScriptConvertException | 抛出异常，提示小说不存在 |
| 章节数据为空 | ScriptConvertException | 抛出异常，提示章节数据为空 |
| 已有进行中的任务 | ScriptConvertException | 抛出异常，提示任务冲突 |
| 全书概览生成失败 | ScriptConvertException | 回退到简单逐章转换模式 |
| API调用失败 | dev.langchain4j.exception.* | LangChain4j自动重试，失败则记录错误 |
| AI响应格式错误 | dev.langchain4j.exception.* | 记录原始响应，标记为失败 |
| YAML生成失败 | ScriptConvertException | 记录错误，标记为失败 |
| 文件写入失败 | IOException | 捕获并转换为ScriptConvertException |
| 数据库操作失败 | DataAccessException | 捕获并转换为ScriptConvertException |

### 8.3 全局异常处理

GlobalExceptionHandler 新增处理：
- ScriptConvertException -> 返回 400 状态码
- dev.langchain4j.exception.* -> 返回 500 状态码

---

## 九、配置项

### 9.1 Maven依赖

```xml
<!-- LangChain4j DashScope集成 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-community-dashscope-spring-boot-starter</artifactId>
    <version>1.15.0-beta25</version>
</dependency>

<!-- LangChain4j核心starter（支持AI Services） -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>1.15.0-beta25</version>
</dependency>
```

### 9.2 application.yml 配置（更新）

```yaml
# LangChain4j配置
langchain4j:
  community:
    dashscope:
      chat-model:
        api-key: ${DASHSCOPE_API_KEY:sk-xxx}
        model-name: qwen3.6-plus
        max-tokens: 32768          # 从8192改为32768（32K）
        temperature: 0.7           # 从0.3改为0.7（剧本创作需要创意）
        max-retries: 3
        log-requests: true
        log-responses: true

# 异步配置
async:
  core-pool-size: 5
  max-pool-size: 10
  queue-capacity: 100
  thread-name-prefix: script-convert-

# 剧本转换配置（新增）
script-convert:
  window-size: 3                   # 滑动窗口大小
  overview-max-tokens: 10000       # 概览最大token
  summary-max-tokens: 2000         # 摘要最大token
  max-retries: 3                   # 最大重试次数
  timeout-seconds: 60              # 超时时间
  progress:
    init: 10
    overview: 20
    chapter-start: 20
    chapter-end: 90
    finalize: 100
```

### 9.3 提示词配置文件

#### src/main/resources/prompts/convert-system.txt（更新）

```
你是一位专业的剧本改编专家，擅长将小说文本转换为符合中国影视行业标准的剧本格式。

## 你的任务
将用户提供的小说章节转换为结构化的剧本格式。

## 上下文信息
你会收到以下信息：
1. 全书概览：包含角色表、情节线、地点表、写作风格
2. 前文上下文：前面章节的内容（或摘要）
3. 当前章节：需要转换的章节内容

## 输出格式要求
请严格按照以下JSON格式输出，不要包含任何其他内容：

{
  "episode_title": "幕标题（简短概括本章内容）",
  "characters": [
    {
      "name": "角色名称",
      "description": "角色简短描述"
    }
  ],
  "scenes": [
    {
      "scene_number": 场景序号,
      "scene_header": "地点 时间 内外景",
      "content": "场景内容"
    }
  ]
}

## scene_header格式规范
格式：「地点 时间 内外景」
- 地点：具体的场景位置（如：出租屋、医院走廊、公园湖边）
- 时间：日/夜/清晨/黄昏/傍晚/深夜/凌晨
- 内外景：内/外/内外（室内/室外/室内外）

示例：
- 出租屋 夜 内
- 医院走廊 日 内
- 公园湖边 黄昏 外

## 场景内容格式规范
1. 场景描写：使用连贯的叙述性文字，不要碎片化
2. 角色对话：角色名：台词内容
3. 带动作的对话：角色名（动作描述）：台词内容
4. 角色动作：使用叙述性文字描述
5. 内心活动：通过描写自然表现，不要使用"他想"等直接表述

## 注意事项
1. 保持剧本的连贯性和可读性
2. 不要使用镜头指示（如"镜头推进"、"特写"等）
3. 通过行文自然表现镜头调度
4. 角色名称要保持一致（参考全书概览中的角色表）
5. 场景划分要合理，每个场景描述一个连续的时空
6. 保持与前文的情节连贯性
7. 角色性格要与全书概览中描述的一致
```

#### src/main/resources/prompts/convert-user.txt（更新）

```
请将以下小说章节转换为剧本格式。

## 全书概览
{{overview}}

## 前文上下文
{{contextChapters}}

## 当前章节信息
- 章节标题：{{title}}
- 章节序号：{{chapterNumber}}

## 当前章节内容
{{content}}

请按照系统提示词中的JSON格式要求输出转换结果。
```

**说明：**
- 模板变量使用`{{variable}}`语法（LangChain4j标准）
- 与之前的`{variable}`语法不同
- **新增变量**：`overview`（全书概览）、`contextChapters`（前文上下文）

#### src/main/resources/prompts/overview-system.txt（新增）

```
你是一位专业的文学分析师，擅长分析小说的结构、角色和情节。

## 你的任务
分析用户提供的小说章节，生成一份全面的全书概览。

## 输出格式要求
请严格按照以下JSON格式输出，不要包含任何其他内容：

{
  "characters": [
    {
      "name": "角色名称",
      "description": "角色描述",
      "personality": "性格特点",
      "relationships": "与其他角色关系"
    }
  ],
  "plot_lines": [
    {
      "type": "情节线类型（主线/支线/伏笔）",
      "description": "情节描述",
      "chapters": [1, 2, 3]
    }
  ],
  "locations": [
    {
      "name": "地点名称",
      "description": "地点描述",
      "chapters": [1, 2, 3]
    }
  ],
  "writing_style": {
    "language_style": "语言特点",
    "narrative_style": "叙事风格",
    "dialogue_style": "对话特点"
  }
}

## 注意事项
1. 角色信息要全面，包括主要角色和重要配角
2. 情节线要清晰，区分主线、支线和伏笔
3. 地点要具体，便于后续场景设置
4. 写作风格要准确，便于保持一致性
```

#### src/main/resources/prompts/overview-user.txt（新增）

```
请分析以下小说章节，生成一份全面的全书概览。

## 章节内容
{{chapters}}

请按照系统提示词中的JSON格式要求输出分析结果。
```

#### src/main/resources/prompts/summary-system.txt（新增）

```
你是一位专业的文学摘要专家，擅长提取章节的关键信息。

## 你的任务
为用户提供的小说章节生成一份简洁的摘要，保留关键信息。

## 输出格式要求
请严格按照以下JSON格式输出，不要包含任何其他内容：

{
  "chapter_number": 章节序号,
  "title": "章节标题",
  "characters": ["角色1", "角色2"],
  "plot_summary": "主要情节概述",
  "key_scenes": ["关键场景1", "关键场景2"],
  "key_dialogues": ["重要对话1", "重要对话2"]
}

## 注意事项
1. 保留所有出现的角色名称
2. 情节概述要简洁但完整
3. 关键场景要具体，便于后续转换
4. 重要对话要保留原文风格
```

#### src/main/resources/prompts/summary-user.txt（新增）

```
请为以下小说章节生成一份简洁的摘要。

## 章节信息
- 章节序号：{{chapterNumber}}
- 章节标题：{{title}}

## 章节内容
{{content}}

请按照系统提示词中的JSON格式要求输出摘要结果。
```

---

## 十、测试用例

### 10.1 单元测试场景

| 测试场景 | 输入 | 预期结果 |
|----------|------|----------|
| 启动转换任务 | 有效的小说ID | 创建任务成功，返回任务ID |
| 启动转换任务（小说不存在） | 不存在的小说ID | 抛出ScriptConvertException |
| 启动转换任务（已有进行中任务） | 已有CONVERTING状态的任务 | 抛出ScriptConvertException |
| 获取转换状态 | 有效的任务ID | 返回转换状态和进度 |
| 取消转换任务 | 进行中的任务ID | 任务状态更新为CANCELLED |
| 重试转换任务 | 失败的任务ID | 重新启动转换流程 |
| 全书概览生成 | 章节列表 | 返回NovelOverview对象 |
| 滑动窗口转换 | 章节列表 + 窗口大小 | 返回ChapterScript对象列表 |
| AI Service转换 | 章节数据 + 上下文 | 返回ChapterScript对象 |
| 结果合并 | 多个ChapterScript | 合并为完整的ScriptData |

### 10.2 集成测试场景

| 测试场景 | 输入 | 预期结果 |
|----------|------|----------|
| 短篇批量转换 | 包含5章的小说 | 一次转换成功，生成YAML文件 |
| 中篇滑动窗口 | 包含15章的小说 | 滑动窗口转换成功 |
| 长篇概览+窗口 | 包含50章的小说 | 先生成概览，再逐章转换 |
| 超长章处理 | 包含20000字章节的小说 | 正常转换，不超token限制 |
| 角色一致性 | 包含多章节的小说 | 角色信息保持一致 |
| 情节连贯性 | 包含多章节的小说 | 情节发展自然连贯 |
| API调用失败 | 模拟API错误 | 自动重试，最终记录失败 |
| 转换取消 | 转换过程中取消 | 任务停止，状态更新 |
| 并发转换 | 同时启动多个任务 | 正常处理，不冲突 |
| 概览生成失败 | 模拟概览生成错误 | 回退到简单逐章转换模式 |

### 10.3 测试数据

| 测试文件 | 类型 | 内容描述 | 预期结果 |
|----------|------|----------|----------|
| test-3chapters.txt | TXT | 包含3个标准章节 | 转换成功 |
| test-5chapters.txt | TXT | 包含5个章节 | 转换成功 |
| test-15chapters.txt | TXT | 包含15个章节（中篇） | 滑动窗口转换成功 |
| test-50chapters.txt | TXT | 包含50个章节（长篇） | 概览+窗口转换成功 |
| test-long-chapter.txt | TXT | 单章超长内容（20000字） | 处理成功 |
| test-no-dialogue.txt | TXT | 无对话的章节 | 转换成功 |
| test-many-characters.txt | TXT | 包含大量角色的章节 | 角色信息一致 |

### 10.4 性能测试场景

| 测试场景 | 输入 | 预期结果 |
|----------|------|----------|
| 单章转换时间 | 标准章节（5000字） | <30秒 |
| 3章转换时间 | 包含3章的小说 | <2分钟 |
| 10章转换时间 | 包含10章的小说 | <5分钟 |
| 50章转换时间 | 包含50章的小说 | <20分钟 |
| 概览生成时间 | 包含50章的小说 | <2分钟 |
| 内存占用 | 处理最大文件 | <200MB |

---

## 十一、性能指标（更新）

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 单章转换时间 | <30秒 | 包含API调用和响应解析 |
| 全书概览生成时间 | <2分钟 | 一次性处理所有章节 |
| 3章转换时间 | <2分钟 | 完整转换流程 |
| 10章转换时间 | <5分钟 | 完整转换流程 |
| 50章转换时间 | <20分钟 | 完整转换流程（含概览生成） |
| API调用成功率 | >95% | 包含重试后的成功率 |
| 并发转换支持 | 5个任务 | 同时进行的转换任务数 |
| 内存占用 | <200MB | 处理最大文件时的内存峰值 |
| 角色一致性 | 100% | 同一角色在不同章节中信息一致 |
| 情节连贯性 | >95% | 情节发展自然连贯 |

---

## 十二、安全考虑

| 安全项 | 措施 |
|--------|------|
| API密钥安全 | 存储在配置文件中，使用环境变量，不硬编码在代码中 |
| 输入验证 | 验证章节内容长度，防止恶意输入 |
| 错误信息脱敏 | 不暴露API密钥和内部实现细节 |
| 并发控制 | 限制同时进行的转换任务数 |
| 资源限制 | 限制单次转换的最大章节数和内容长度 |
| Token限制 | 单章输入限制128K tokens，输出限制32K tokens |

---

## 十三、LangChain4j优势总结

| 优势 | 说明 |
|------|------|
| 声明式API | 使用`@AiService`注解，自动生成实现 |
| 结构化输出 | 直接返回POJO，自动解析JSON |
| 提示词管理 | 使用`@SystemMessage`和`@UserMessage`注解 |
| 模板变量 | 使用`{{variable}}`语法，清晰易读 |
| 资源文件加载 | 支持从文件加载提示词，便于维护 |
| 内置重试 | 自动处理重试和错误 |
| Spring Boot集成 | 自动配置，开箱即用 |
| 可观测性 | 支持日志记录和监控 |
| 多模型支持 | 轻松切换不同AI模型 |

---

## 十四、后续优化

| 优化项 | 优先级 | 说明 |
|--------|--------|------|
| 流式输出 | P1 | 使用`TokenStream`支持流式响应 |
| 多模型支持 | P1 | 支持切换不同的AI模型 |
| 批量转换 | P2 | 支持批量上传和转换 |
| 转换模板 | P2 | 支持用户自定义转换模板 |
| 转换质量评估 | P2 | 自动评估转换质量，提供优化建议 |
| 增量转换 | P3 | 支持只重新转换修改的章节 |
| 转换历史对比 | P3 | 对比不同版本的转换结果 |
| 缓存优化 | P3 | 缓存AI响应，避免重复转换 |
| RAG增强 | P3 | 使用RAG提供上下文，提升转换质量 |

---

**文档编写：** 张恒嘉  
**最后更新：** 2026-06-07  
**文档状态：** 已审核
